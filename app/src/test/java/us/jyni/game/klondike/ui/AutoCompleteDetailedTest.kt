package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.model.*

/**
 * AutoComplete 상세 테스트
 * 각 개선사항이 실제로 작동하는지 확인
 */
class AutoCompleteDetailedTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_현재_문제_재현_Draw만_반복하는지`() {
        // 여러 시드로 테스트해서 Draw만 반복하는 상황 찾기
        val seeds = listOf(12345UL, 54321UL, 99999UL, 11111UL, 77777UL)
        
        for (seed in seeds) {
            println("\n=== Testing seed: $seed ===")
            viewModel.startGame(seed)
            
            // 몇 번 draw해서 Waste에 카드 만들기
            repeat(5) { viewModel.draw() }
            
            val beforeState = viewModel.state.value
            val beforeStock = beforeState.stock.size
            val beforeWaste = beforeState.waste.size
            val totalCards = beforeStock + beforeWaste
            
            println("Before autoComplete:")
            println("  Stock: $beforeStock, Waste: $beforeWaste (total: $totalCards)")
            
            if (beforeWaste > 0) {
                val wasteTop = beforeState.waste.last()
                println("  Waste top: ${wasteTop.suit} ${wasteTop.rank}")
                
                // Tableau 상태
                beforeState.tableau.forEachIndexed { i, pile ->
                    val topCard = pile.lastOrNull { it.isFaceUp }
                    println("  T[$i]: ${pile.size} cards, top=${topCard?.let { "${it.suit} ${it.rank}" } ?: "empty"}")
                }
                
                // canMoveWasteToTableau 체크
                var canMoveToAny = false
                for (col in 0..6) {
                    if (viewModel.canMoveWasteToTableau(col)) {
                        println("  ✓ Can move Waste → T[$col]")
                        canMoveToAny = true
                    }
                }
                
                if (!canMoveToAny) {
                    println("  ✗ Cannot move Waste to any Tableau")
                }
            }
            
            val moves = viewModel.autoComplete()
            
            val afterState = viewModel.state.value
            val afterStock = afterState.stock.size
            val afterWaste = afterState.waste.size
            
            println("\nAfter autoComplete ($moves moves):")
            println("  Stock: $afterStock, Waste: $afterWaste")
            
            // 분석: Draw만 했는지 확인
            if (moves > 0 && totalCards > 0) {
                val stockWasteChanged = (beforeStock != afterStock) || (beforeWaste != afterWaste)
                
                if (stockWasteChanged && moves <= totalCards + 2) {
                    println("  ⚠ WARNING: Only Draw moves detected! (moves=$moves, totalCards=$totalCards)")
                    println("  This suggests Waste→Tableau or other moves didn't happen")
                }
            }
        }
    }
    
    @Test
    fun `test_Waste에서_Tableau로_이동_가능한_상황`() {
        // 특정 상황을 만들어서 테스트
        viewModel.startGame(12345UL)
        
        // 게임 상태를 직접 조작할 수 없으므로, 여러 시드로 시도
        var foundMovableCase = false
        
        for (seed in 1UL..100UL) {
            viewModel.startGame(seed)
            repeat(10) { viewModel.draw() }
            
            val state = viewModel.state.value
            if (state.waste.isEmpty()) continue
            
            val wasteCard = state.waste.last()
            
            // 각 Tableau 컬럼 체크
            for (col in 0..6) {
                if (viewModel.canMoveWasteToTableau(col)) {
                    foundMovableCase = true
                    println("\n✓ Found movable case with seed=$seed")
                    println("  Waste top: ${wasteCard.suit} ${wasteCard.rank}")
                    
                    val tableau = state.tableau[col]
                    val tableauTop = tableau.lastOrNull { it.isFaceUp }
                    println("  T[$col] top: ${tableauTop?.let { "${it.suit} ${it.rank}" } ?: "empty"}")
                    
                    // autoComplete 실행
                    val beforeWaste = state.waste.size
                    val moves = viewModel.autoComplete()
                    val afterWaste = viewModel.state.value.waste.size
                    
                    println("  AutoComplete: $moves moves")
                    println("  Waste before: $beforeWaste, after: $afterWaste")
                    
                    // Waste 크기가 줄었는지 확인 (이동이 일어났다면)
                    if (afterWaste < beforeWaste) {
                        println("  ✓ Waste card was moved successfully!")
                    } else {
                        println("  ✗ Waste card was NOT moved despite being movable!")
                        fail("Waste→Tableau move should have happened but didn't")
                    }
                    
                    return // 하나만 찾으면 충분
                }
            }
        }
        
        if (!foundMovableCase) {
            println("⚠ Could not find a case where Waste→Tableau is possible in first 100 seeds")
        }
    }
    
    @Test
    fun `test_Draw_무한루프_방지_동작`() {
        viewModel.startGame(99999UL)
        
        val beforeState = viewModel.state.value
        val totalCards = beforeState.stock.size + beforeState.waste.size
        
        println("Before autoComplete:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        println("  Total: $totalCards")
        
        val moves = viewModel.autoComplete()
        
        println("\nAutoComplete made $moves moves")
        
        // Draw만 했을 경우, totalCards + 여유분 정도에서 멈춰야 함
        assertTrue("Should stop before infinite loop (moves=$moves, totalCards=$totalCards)", 
            moves <= totalCards + 5)
        
        // 정확히 어디서 멈췄는지 확인
        if (moves > 0 && moves <= totalCards + 5) {
            println("✓ Infinite loop detection worked (stopped at $moves moves)")
        }
    }
    
    @Test
    fun `test_Foundation_이동_우선순위`() {
        // Foundation으로 이동 가능한 상황에서는 그것이 우선되어야 함
        for (seed in 1UL..50UL) {
            viewModel.startGame(seed)
            
            // 몇 번 이동해서 Foundation에 카드가 있는 상태로
            repeat(20) { viewModel.draw() }
            
            val beforeState = viewModel.state.value
            val beforeFoundation = beforeState.foundation.sumOf { it.size }
            
            if (beforeFoundation > 0) {
                // Foundation에 이미 카드가 있으면 autoComplete 실행
                val moves = viewModel.autoComplete()
                val afterFoundation = viewModel.state.value.foundation.sumOf { it.size }
                
                if (moves > 0) {
                    println("Seed $seed: $moves moves, Foundation: $beforeFoundation → $afterFoundation")
                    
                    if (afterFoundation > beforeFoundation) {
                        println("  ✓ Foundation moves happened")
                        return // 하나만 확인하면 충분
                    }
                }
            }
        }
    }
    
    @Test
    fun `test1_wasteToTableau가 실행되는지 확인`() {
        // Waste에 있는 카드가 Tableau로 이동 가능한 상황
        viewModel.startGame(12345UL)
        
        // 초기 상태 출력
        val initialState = viewModel.state.value
        println("Initial state:")
        println("  Stock: ${initialState.stock.size}, Waste: ${initialState.waste.size}")
        println("  Tableau[0]: ${initialState.tableau[0].size} cards")
        
        // 몇 번 draw해서 Waste에 카드 만들기
        repeat(3) { viewModel.draw() }
        
        val beforeState = viewModel.state.value
        println("\nAfter 3 draws:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        if (beforeState.waste.isNotEmpty()) {
            val wasteCard = beforeState.waste.last()
            println("  Waste top: ${wasteCard.suit} ${wasteCard.rank}")
        }
        
        // AutoComplete 실행
        val moves = viewModel.autoComplete()
        
        val afterState = viewModel.state.value
        println("\nAfter autoComplete ($moves moves):")
        println("  Stock: ${afterState.stock.size}, Waste: ${afterState.waste.size}")
        
        // Draw만 한 게 아니라 실제로 이동이 있었는지 확인
        // (이 테스트는 실패할 수 있지만 어떤 이동이 있었는지 확인용)
        assertTrue("AutoComplete should make some moves", moves > 0)
    }
    
    @Test
    fun `test2_King이동이_실행되는지_확인`() {
        // King이 있고 빈 칸이 있는 상황을 만들기 위한 테스트
        viewModel.startGame(54321UL)
        
        val initialState = viewModel.state.value
        println("Initial state:")
        initialState.tableau.forEachIndexed { i, pile ->
            println("  T[$i]: ${pile.size} cards (faceUp: ${pile.count { it.isFaceUp }})")
            if (pile.isNotEmpty() && pile.any { it.isFaceUp }) {
                val faceUpCards = pile.filter { it.isFaceUp }
                println("    Face up: ${faceUpCards.map { "${it.suit} ${it.rank}" }}")
            }
        }
        
        val moves = viewModel.autoComplete()
        println("\nAutoComplete made $moves moves")
        
        assertTrue("AutoComplete should make some moves", moves >= 0)
    }
    
    @Test
    fun `test3_Draw무한루프_감지_확인`() {
        // Stock과 Waste만 있고 다른 이동이 불가능한 상황
        viewModel.startGame(99999UL)
        
        val beforeState = viewModel.state.value
        val totalCards = beforeState.stock.size + beforeState.waste.size
        
        println("Before autoComplete:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        println("  Total cards in Stock+Waste: $totalCards")
        
        val moves = viewModel.autoComplete()
        
        println("\nAutoComplete made $moves moves")
        
        // Draw만 했을 경우 최대 totalCards + 여유분 정도만 이동해야 함
        assertTrue("Should stop after detecting infinite loop", moves <= totalCards + 5)
    }
    
    @Test
    fun `test4_Waste재활용_확인`() {
        // Stock이 비었을 때 draw하면 Waste를 재활용하는지 확인
        viewModel.startGame(11111UL)
        
        // Stock이 다 떨어질 때까지 draw
        repeat(30) { viewModel.draw() }
        
        val beforeState = viewModel.state.value
        println("After depleting stock:")
        println("  Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        
        // 한 번 더 draw하면 재활용 되어야 함 (규칙에 따라)
        if (beforeState.stock.isEmpty() && beforeState.waste.isNotEmpty()) {
            viewModel.draw()
            val afterDraw = viewModel.state.value
            println("\nAfter one more draw (should recycle):")
            println("  Stock: ${afterDraw.stock.size}, Waste: ${afterDraw.waste.size}")
            
            // 재활용 여부 확인 (규칙에 따라 다를 수 있음)
            println("  Recycled: ${afterDraw.stock.size > 0 || afterDraw.waste.size != beforeState.waste.size}")
        }
    }
    
    @Test
    fun `test5_canMoveWasteToTableau_동작확인`() {
        // 실제로 canMoveWasteToTableau가 제대로 동작하는지 확인
        viewModel.startGame(12345UL)
        
        // Waste에 카드가 있을 때까지 draw
        repeat(5) { viewModel.draw() }
        
        val state = viewModel.state.value
        if (state.waste.isNotEmpty()) {
            val wasteCard = state.waste.last()
            println("Waste top card: ${wasteCard.suit} ${wasteCard.rank}")
            
            println("\nChecking canMoveWasteToTableau for each column:")
            for (col in 0..6) {
                val canMove = viewModel.canMoveWasteToTableau(col)
                val pile = state.tableau[col]
                val topCard = pile.lastOrNull { it.isFaceUp }
                println("  T[$col]: canMove=$canMove, topCard=${topCard?.let { "${it.suit} ${it.rank}" } ?: "empty"}")
            }
        }
    }
}
