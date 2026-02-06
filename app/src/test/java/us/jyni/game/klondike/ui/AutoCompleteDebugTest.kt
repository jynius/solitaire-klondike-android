package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * AutoComplete 디버깅 테스트
 * 정확히 어떤 이동이 일어나는지 추적
 */
class AutoCompleteDebugTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_54321_seed_상세분석`() {
        println("\n=== Seed 54321 Detailed Analysis ===")
        viewModel.startGame(54321UL)
        
        // 5번 draw
        repeat(5) { viewModel.draw() }
        
        val beforeState = viewModel.state.value
        println("Before autoComplete:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        println("  Foundation: ${beforeState.foundation.map { it.size }}")
        
        if (beforeState.waste.isNotEmpty()) {
            val wasteTop = beforeState.waste.last()
            println("  Waste top: ${wasteTop.suit} ${wasteTop.rank}")
        }
        
        beforeState.tableau.forEachIndexed { i, pile ->
            val faceUpCount = pile.count { it.isFaceUp }
            val topCard = pile.lastOrNull { it.isFaceUp }
            println("  T[$i]: ${pile.size} cards (${faceUpCount} face-up), top=${topCard?.let { "${it.suit} ${it.rank}" } ?: "none"}")
        }
        
        println("\nChecking possible moves:")
        for (col in 0..6) {
            if (viewModel.canMoveWasteToTableau(col)) {
                println("  ✓ Waste → T[$col] is possible")
            }
        }
        
        // autoComplete 실행
        println("\nRunning autoComplete...")
        val moves = viewModel.autoComplete()
        
        val afterState = viewModel.state.value
        println("\nAfter autoComplete ($moves moves):")
        println("  Stock: ${afterState.stock.size}, Waste: ${afterState.waste.size}")
        println("  Foundation: ${afterState.foundation.map { it.size }}")
        
        afterState.tableau.forEachIndexed { i, pile ->
            val faceUpCount = pile.count { it.isFaceUp }
            val topCard = pile.lastOrNull { it.isFaceUp }
            println("  T[$i]: ${pile.size} cards (${faceUpCount} face-up), top=${topCard?.let { "${it.suit} ${it.rank}" } ?: "none"}")
        }
        
        // 분석
        val foundationIncrease = afterState.foundation.sumOf { it.size } - beforeState.foundation.sumOf { it.size }
        println("\nAnalysis:")
        println("  Total moves: $moves")
        println("  Foundation increase: $foundationIncrease cards")
        println("  Stock change: ${beforeState.stock.size} → ${afterState.stock.size}")
        println("  Waste change: ${beforeState.waste.size} → ${afterState.waste.size}")
        
        if (foundationIncrease > 0) {
            println("  ✓ Cards moved to Foundation")
        }
        
        // moves가 너무 많으면 문제
        assertTrue("Too many moves suggests something wrong", moves < 200)
    }
    
    @Test
    fun `test_첫_11번_이동_추적`() {
        println("\n=== Tracking First 11 Moves ===")
        viewModel.startGame(12345UL)
        
        // Stock이 비어있지 않은 상태로 시작
        repeat(5) { viewModel.draw() }
        
        val beforeState = viewModel.state.value
        val totalCards = beforeState.stock.size + beforeState.waste.size
        
        println("Initial state:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        println("  Total cards: $totalCards")
        
        // 한 번에 하나씩 이동 추적
        var moveCount = 0
        var drawCount = 0
        var nonDrawCount = 0
        
        for (i in 1..15) {
            val stateBefore = viewModel.state.value
            val stockBefore = stateBefore.stock.size
            val wasteBefore = stateBefore.waste.size
            val foundationBefore = stateBefore.foundation.sumOf { it.size }
            
            // 수동으로 한 번의 이동 시도
            var moved = false
            
            // 1. Waste → Foundation
            if (!moved && stateBefore.waste.isNotEmpty()) {
                for (f in 0..3) {
                    if (viewModel.canMoveWasteToFoundation(f)) {
                        viewModel.moveWasteToFoundation(f)
                        println("Move $i: Waste → F[$f]")
                        moved = true
                        nonDrawCount++
                        break
                    }
                }
            }
            
            // 2. Tableau → Foundation
            if (!moved) {
                outer@ for (col in 0..6) {
                    for (f in 0..3) {
                        if (viewModel.canMoveTableauToFoundation(col, f)) {
                            viewModel.moveTableauToFoundation(col, f)
                            println("Move $i: T[$col] → F[$f]")
                            moved = true
                            nonDrawCount++
                            break@outer
                        }
                    }
                }
            }
            
            // 3. Waste → Tableau
            if (!moved && stateBefore.waste.isNotEmpty()) {
                for (col in 0..6) {
                    if (viewModel.canMoveWasteToTableau(col)) {
                        viewModel.moveWasteToTableau(col)
                        println("Move $i: Waste → T[$col]")
                        moved = true
                        nonDrawCount++
                        break
                    }
                }
            }
            
            // 4. Draw
            if (!moved && (stateBefore.stock.isNotEmpty() || stateBefore.waste.isNotEmpty())) {
                viewModel.draw()
                println("Move $i: Draw")
                moved = true
                drawCount++
            }
            
            if (!moved) {
                println("No more moves available after $i attempts")
                break
            }
            
            moveCount++
            
            // 11번 이동 후 상태 확인
            if (moveCount == 11) {
                val stateAfter = viewModel.state.value
                println("\nAfter 11 moves:")
                println("  Stock: $stockBefore → ${stateAfter.stock.size}")
                println("  Waste: $wasteBefore → ${stateAfter.waste.size}")
                println("  Foundation: $foundationBefore → ${stateAfter.foundation.sumOf { it.size }}")
                println("  Draw moves: $drawCount, Non-draw moves: $nonDrawCount")
                
                if (drawCount == 11) {
                    println("  ⚠ ALL 11 moves were Draw!")
                }
                break
            }
        }
    }
}
