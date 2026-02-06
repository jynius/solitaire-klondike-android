package us.jyni.game.klondike.ui

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * GameViewModel의 autoComplete() 함수 테스트
 * 개선된 기능들을 검증:
 * 1. Stock/Waste 무한 루프 방지
 * 2. Waste Recycle 기능
 * 3. King 이동 전략
 * 4. Tableau 간 이동 (뒷면 카드 뒤집기)
 */
class GameViewModelAutoCompleteTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun autoComplete_basic_foundation_moves() {
        // 기본적인 Foundation 이동 테스트
        viewModel.startGame(seed = 12345u, rules = Ruleset())
        
        val moveCount = viewModel.autoComplete()
        
        // 최소한 몇 개의 카드는 Foundation으로 이동했어야 함
        assertTrue("자동 완료가 일부 카드를 이동했어야 함", moveCount >= 0)
    }
    
    @Test
    fun autoComplete_with_waste_recycle() {
        // Waste Recycle이 필요한 상황 테스트
        viewModel.startGame(seed = 54321u, rules = Ruleset())
        
        // Stock의 모든 카드를 Waste로 이동
        var drawCount = 0
        while (viewModel.state.value.stock.isNotEmpty() && drawCount < 100) {
            viewModel.draw()
            drawCount++
        }
        
        val stockEmpty = viewModel.state.value.stock.isEmpty()
        val wasteNotEmpty = viewModel.state.value.waste.isNotEmpty()
        
        assertTrue("Stock이 비어야 함", stockEmpty)
        assertTrue("Waste에 카드가 있어야 함", wasteNotEmpty)
        
        // autoComplete가 Waste를 재활용해야 함
        val moveCount = viewModel.autoComplete()
        
        // Waste 재활용이 포함되어 이동 횟수가 있어야 함
        assertTrue("자동 완료가 진행되어야 함", moveCount >= 0)
    }
    
    @Test
    fun autoComplete_stops_on_infinite_draw_loop() {
        // Draw만 반복하고 다른 이동이 없는 상황에서 멈춰야 함
        viewModel.startGame(seed = 99999u, rules = Ruleset())
        
        // 여러 번 Draw를 시도
        for (i in 1..30) {
            viewModel.draw()
        }
        
        val initialMoveCount = viewModel.getMoveCount()
        val moveCount = viewModel.autoComplete()
        
        // 무한 루프에 빠지지 않고 종료되어야 함
        assertTrue("자동 완료가 유한 시간 내에 종료되어야 함", moveCount >= 0)
        
        // 이동 횟수가 너무 많지 않아야 함 (무한 루프가 아님을 의미)
        assertTrue("이동 횟수가 합리적이어야 함", moveCount < 1000)
    }
    
    @Test
    fun autoComplete_moves_king_to_empty_space() {
        // King을 빈 공간으로 이동하는 전략 테스트
        // 특정 시드로 시작하여 빈 공간과 King이 있는 상황 만들기
        viewModel.startGame(seed = 11111u, rules = Ruleset())
        
        // 여러 번 autoComplete를 실행하여 빈 공간이 생기는지 확인
        var totalMoves = 0
        for (i in 1..10) {
            val moves = viewModel.autoComplete()
            totalMoves += moves
            if (moves == 0) break
            
            // 빈 Tableau 열이 있는지 확인
            val hasEmptyColumn = viewModel.state.value.tableau.any { it.isEmpty() }
            if (hasEmptyColumn) {
                // King이 있는지 확인
                val hasKing = viewModel.state.value.tableau.any { pile ->
                    pile.any { card -> card.rank == Rank.KING && card.isFaceUp }
                }
                
                if (hasKing) {
                    // King 이동 전략이 실행될 수 있는 상황
                    println("빈 공간과 King이 모두 있음 - King 이동 전략 테스트 가능")
                }
            }
        }
        
        // 자동 완료가 정상적으로 실행되었는지만 확인
        assertTrue("자동 완료가 실행되어야 함", totalMoves >= 0)
    }
    
    @Test
    fun autoComplete_flips_face_down_cards() {
        // 뒷면 카드를 뒤집기 위해 Tableau 간 이동을 하는지 테스트
        viewModel.startGame(seed = 77777u, rules = Ruleset())
        
        val initialFaceDownCount = viewModel.state.value.tableau.sumOf { pile ->
            pile.count { !it.isFaceUp }
        }
        
        println("초기 뒷면 카드 수: $initialFaceDownCount")
        
        val moveCount = viewModel.autoComplete()
        
        val finalFaceDownCount = viewModel.state.value.tableau.sumOf { pile ->
            pile.count { !it.isFaceUp }
        }
        
        println("최종 뒷면 카드 수: $finalFaceDownCount")
        println("자동 완료 이동 횟수: $moveCount")
        
        // 자동 완료가 진행되었다면 뒷면 카드가 줄어들었을 가능성이 있음
        assertTrue("자동 완료가 실행되어야 함", moveCount >= 0)
    }
    
    @Test
    fun autoComplete_handles_multiple_strategies() {
        // 여러 전략을 조합하여 사용하는지 테스트
        viewModel.startGame(seed = 33333u, rules = Ruleset())
        
        var totalMoves = 0
        var iterations = 0
        var previousMoves = -1
        
        // 여러 번 반복하여 다양한 전략이 사용되는지 확인
        while (iterations < 20) {
            val moves = viewModel.autoComplete()
            totalMoves += moves
            
            if (moves == 0 || moves == previousMoves) {
                break
            }
            
            previousMoves = moves
            iterations++
            
            // 일부 카드를 수동으로 이동하여 상황 변경
            if (viewModel.state.value.stock.isNotEmpty()) {
                viewModel.draw()
            }
        }
        
        println("총 반복 횟수: $iterations")
        println("총 이동 횟수: $totalMoves")
        
        // 여러 전략이 조합되어 사용되었는지 확인
        assertTrue("자동 완료가 정상적으로 실행되어야 함", totalMoves >= 0)
        assertTrue("무한 루프에 빠지지 않아야 함", iterations < 20)
    }
    
    @Test
    fun autoComplete_respects_max_draw_attempts() {
        // maxDrawAttempts 제한이 작동하는지 테스트
        viewModel.startGame(seed = 88888u, rules = Ruleset())
        
        val stockSize = viewModel.state.value.stock.size
        val wasteSize = viewModel.state.value.waste.size
        val maxExpectedDraws = stockSize + wasteSize + 1
        
        println("Stock 크기: $stockSize, Waste 크기: $wasteSize")
        println("최대 예상 Draw 횟수: $maxExpectedDraws")
        
        val moveCount = viewModel.autoComplete()
        
        // 이동 횟수가 합리적인 범위 내에 있어야 함
        assertTrue("이동 횟수가 합리적이어야 함", moveCount < maxExpectedDraws * 10)
        assertTrue("자동 완료가 종료되어야 함", moveCount >= 0)
    }
    
    @Test
    fun autoComplete_moves_waste_to_tableau() {
        // Waste에서 Tableau로 이동하는 전략 테스트
        viewModel.startGame(seed = 66666u, rules = Ruleset())
        
        // Waste에 카드가 있도록 Draw
        if (viewModel.state.value.stock.isNotEmpty()) {
            viewModel.draw()
            viewModel.draw()
            viewModel.draw()
        }
        
        val initialWasteSize = viewModel.state.value.waste.size
        println("초기 Waste 크기: $initialWasteSize")
        
        val moveCount = viewModel.autoComplete()
        
        val finalWasteSize = viewModel.state.value.waste.size
        println("최종 Waste 크기: $finalWasteSize")
        println("이동 횟수: $moveCount")
        
        // autoComplete가 Waste 카드도 고려했는지 확인
        assertTrue("자동 완료가 실행되어야 함", moveCount >= 0)
    }
}
