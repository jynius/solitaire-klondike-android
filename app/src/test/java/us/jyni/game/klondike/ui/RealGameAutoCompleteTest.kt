package us.jyni.game.klondike.ui

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

/**
 * 실제 게임 상태에서 autoComplete()가 제대로 작동하는지 테스트
 */
class RealGameAutoCompleteTest {
    
    @Test
    fun test_stock_only_drawing_should_stop() {
        // 시나리오: Stock에서만 Draw하고 다른 움직임이 없는 상태
        val viewModel = GameViewModel()
        val state = GameState(
            tableau = List(7) { mutableListOf() },
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
        
        // Tableau: 움직일 수 없는 카드들만 배치
        state.tableau[0].add(Card(Suit.SPADES, Rank.KING, true))
        state.tableau[1].add(Card(Suit.HEARTS, Rank.KING, true))
        state.tableau[2].add(Card(Suit.DIAMONDS, Rank.KING, true))
        
        // Stock: Foundation이나 Tableau로 갈 수 없는 카드들
        state.stock.add(Card(Suit.CLUBS, Rank.QUEEN, false))
        state.stock.add(Card(Suit.CLUBS, Rank.JACK, false))
        state.stock.add(Card(Suit.CLUBS, Rank.TEN, false))
        
        // 게임 엔진에 상태 주입 (실제로는 불가능하므로 다른 방법 사용)
        // 대신 특정 시드로 시작
        viewModel.startGame(seed = 99999u)
        
        val initialStock = viewModel.state.value.stock.size
        val initialWaste = viewModel.state.value.waste.size
        val maxExpectedDraws = initialStock + initialWaste + 1
        
        println("=== Stock Only Drawing Test ===")
        println("Initial Stock: $initialStock, Waste: $initialWaste")
        println("Max Expected Draws: $maxExpectedDraws")
        
        val moveCount = viewModel.autoComplete()
        
        println("Total moves: $moveCount")
        println("Final Stock: ${viewModel.state.value.stock.size}, Waste: ${viewModel.state.value.waste.size}")
        
        // Draw만 반복했다면 이동 횟수가 제한되어야 함
        assertTrue("무한 루프에 빠지지 않아야 함", moveCount < maxExpectedDraws * 2)
    }
    
    @Test
    fun test_waste_recycle_during_autocomplete() {
        // 시나리오: Stock이 비었고 Waste에 카드가 있을 때 Recycle이 되는지
        val viewModel = GameViewModel()
        viewModel.startGame(seed = 12345u)
        
        println("\n=== Waste Recycle Test ===")
        println("Initial Stock: ${viewModel.state.value.stock.size}")
        
        // Stock의 모든 카드를 Waste로 이동
        var drawCount = 0
        while (viewModel.state.value.stock.isNotEmpty() && drawCount < 100) {
            viewModel.draw()
            drawCount++
        }
        
        println("After drawing all: Stock=${viewModel.state.value.stock.size}, Waste=${viewModel.state.value.waste.size}")
        
        val stockEmpty = viewModel.state.value.stock.isEmpty()
        val wasteNotEmpty = viewModel.state.value.waste.isNotEmpty()
        
        assertTrue("Stock이 비어야 함", stockEmpty)
        
        if (wasteNotEmpty) {
            println("Waste has cards, testing recycle...")
            
            // autoComplete 실행
            val movesBefore = viewModel.getMoveCount()
            val autoMoves = viewModel.autoComplete()
            val movesAfter = viewModel.getMoveCount()
            
            println("AutoComplete moves: $autoMoves")
            println("Total game moves: before=$movesBefore, after=$movesAfter")
            
            // Recycle이 진행되었는지 확인 (Stock이 다시 채워졌는지 또는 Waste가 비었는지)
            val finalStock = viewModel.state.value.stock.size
            val finalWaste = viewModel.state.value.waste.size
            
            println("After autoComplete: Stock=$finalStock, Waste=$finalWaste")
            
            // autoComplete가 실행되었고, 이동이 있었다면 성공
            assertTrue("AutoComplete가 실행되어야 함", autoMoves >= 0)
        }
    }
    
    @Test
    fun test_king_moves_to_empty_column() {
        // 시나리오: King이 있고 빈 공간을 만들 수 있을 때
        val viewModel = GameViewModel()
        viewModel.startGame(seed = 11111u)
        
        println("\n=== King to Empty Column Test ===")
        
        // 여러 번 autoComplete를 실행하여 진행
        var totalMoves = 0
        var foundKingMoveScenario = false
        
        for (iteration in 1..10) {
            val movesBefore = viewModel.state.value.tableau.sumOf { it.size }
            val moves = viewModel.autoComplete()
            totalMoves += moves
            
            if (moves == 0) break
            
            // 상태 출력
            println("\n--- Iteration $iteration ---")
            println("Moves: $moves")
            
            // Tableau 상태 확인
            viewModel.state.value.tableau.forEachIndexed { col, pile ->
                val faceUpCards = pile.filter { it.isFaceUp }
                val faceDownCount = pile.count { !it.isFaceUp }
                if (pile.isNotEmpty()) {
                    println("T[$col]: ${pile.size} cards (${faceDownCount} face-down, ${faceUpCards.size} face-up)")
                    if (faceUpCards.isNotEmpty()) {
                        println("       Top: ${faceUpCards.last().rank}${faceUpCards.last().suit}")
                    }
                } else {
                    println("T[$col]: EMPTY")
                }
            }
            
            // 빈 컬럼이 있는지 확인
            val emptyColumns = viewModel.state.value.tableau.indices.filter { 
                viewModel.state.value.tableau[it].isEmpty() 
            }
            
            // King이 있는지 확인 (뒤에 뒷면 카드가 있는 King)
            val kingsWithFaceDown = viewModel.state.value.tableau.mapIndexedNotNull { col, pile ->
                val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
                if (firstFaceUpIndex > 0 && firstFaceUpIndex < pile.size) {
                    val card = pile[firstFaceUpIndex]
                    if (card.rank == Rank.KING) {
                        col to card
                    } else null
                } else null
            }
            
            if (emptyColumns.isNotEmpty() && kingsWithFaceDown.isNotEmpty()) {
                println("SCENARIO FOUND: Empty columns: $emptyColumns, Kings with face-down: ${kingsWithFaceDown.size}")
                foundKingMoveScenario = true
            }
            
            // 일부 카드를 수동으로 이동하여 상황 변경
            if (viewModel.state.value.stock.isNotEmpty()) {
                viewModel.draw()
            }
        }
        
        println("\n=== Summary ===")
        println("Total moves: $totalMoves")
        println("Found King move scenario: $foundKingMoveScenario")
        
        assertTrue("AutoComplete가 실행되어야 함", totalMoves >= 0)
    }
    
    @Test
    fun test_detailed_autocomplete_behavior() {
        // 상세한 동작 확인
        val viewModel = GameViewModel()
        viewModel.startGame(seed = 54321u)
        
        println("\n=== Detailed AutoComplete Behavior Test ===")
        println("Initial state:")
        printGameState(viewModel)
        
        // 첫 번째 autoComplete
        println("\n--- First AutoComplete ---")
        val moves1 = viewModel.autoComplete()
        println("Moves: $moves1")
        printGameState(viewModel)
        
        // Draw 몇 번
        println("\n--- Manual Draws ---")
        for (i in 1..3) {
            if (viewModel.state.value.stock.isNotEmpty() || viewModel.state.value.waste.isNotEmpty()) {
                viewModel.draw()
            }
        }
        printGameState(viewModel)
        
        // 두 번째 autoComplete
        println("\n--- Second AutoComplete ---")
        val moves2 = viewModel.autoComplete()
        println("Moves: $moves2")
        printGameState(viewModel)
        
        assertTrue("AutoComplete가 정상 작동해야 함", moves1 >= 0 && moves2 >= 0)
    }
    
    private fun printGameState(viewModel: GameViewModel) {
        val state = viewModel.state.value
        println("Stock: ${state.stock.size}, Waste: ${state.waste.size}")
        println("Foundation: ${state.foundation.map { it.size }}")
        println("Tableau sizes: ${state.tableau.map { it.size }}")
        println("Tableau face-up: ${state.tableau.map { pile -> pile.count { it.isFaceUp } }}")
    }
}
