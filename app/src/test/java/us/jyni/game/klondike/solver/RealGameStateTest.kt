package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

/**
 * 실제 게임에서 발생한 상황 테스트
 */
class RealGameStateTest {
    
    private val solver = AStarSolver(GameEngine())
    
    @Test
    fun real_game_state_with_stock_and_hidden_ace() {
        // 실제 게임 상태: Stock 24장, ♠A가 T[5]에 숨겨져 있음
        val stateString = "GS2;did=DL1_3Wix6VwVjVfYdnc2cKG1opNjRh4xegrWLKsnBgrUhRRC;seed=10420697478978593767;draw=1;redeals=-1;" +
                "recycle=reverse;f2t=true;rr=-1;go=0;score=10;moves=1;started=1770338924287;paused=0;" +
                "tab=H:5:u/H:K:u/S:K:d,S:10:d,C:10:u/S:6:d,S:3:d,C:4:d,D:K:u/S:2:d,S:Q:d,C:2:d,D:Q:d,H:10:u/" +
                "S:A:d,D:8:d,H:J:d,C:9:d,C:5:d,H:4:u/D:6:d,C:7:d,C:Q:d,C:8:d,D:A:d,S:8:d,D:3:u;" +
                "fnd=C:A:u///;" +
                "sto=C:6:d,D:10:d,H:8:d,H:2:d,S:9:d,H:6:d,C:J:d,D:4:d,D:9:d,H:3:d,S:4:d,H:7:d,D:7:d,H:A:d,C:3:d,D:2:d,D:5:d,H:Q:d,C:K:d,H:9:d,S:7:d,S:5:d,S:J:d,D:J:d;" +
                "was="
        
        val engine = GameEngine()
        assertTrue("상태 복원 실패", engine.restoreStateString(stateString))
        
        val state = engine.getGameState()
        
        // 상태 검증
        assertEquals("Stock은 24장", 24, state.stock.size)
        assertEquals("Waste는 비어있음", 0, state.waste.size)
        assertEquals("F[0]은 ♣A", 1, state.foundation[0].size)
        assertEquals("T[5]는 6장", 6, state.tableau[5].size)
        
        // T[5]의 첫 번째 카드가 ♠A (뒷면)
        val hiddenAce = state.tableau[5][0]
        assertEquals("T[5][0]은 ♠A", Suit.SPADES, hiddenAce.suit)
        assertEquals("T[5][0]은 Ace", Rank.ACE, hiddenAce.rank)
        assertFalse("T[5][0]은 뒷면", hiddenAce.isFaceUp)
        
        println("=== 현재 상태 ===")
        println("Stock: ${state.stock.size}장")
        for (i in 0..6) {
            val pile = state.tableau[i]
            val faceUpCount = pile.count { it.isFaceUp }
            val topCard = pile.lastOrNull()
            println("T[$i]: ${pile.size}장 (앞면=$faceUpCount) - Top: ${topCard?.let { "${it.suit}${it.rank}" }}")
        }
        
        println("\n앞면 카드 목록:")
        for (i in 0..6) {
            val pile = state.tableau[i]
            val faceUpCards = pile.filter { it.isFaceUp }
            if (faceUpCards.isNotEmpty()) {
                println("T[$i]: ${faceUpCards.joinToString { "${it.suit}${it.rank}" }}")
            }
        }
        
        // Solver 실행
        val result = solver.solve(state)
        
        println("\n=== Solver 결과 ===")
        when (result) {
            is SolverResult.Success -> {
                println("✅ 성공: ${result.moves.size}개 이동")
                result.moves.take(10).forEach { println("  - $it") }
            }
            is SolverResult.Timeout -> println("⏱️ 시간 초과: ${result.reason}")
            is SolverResult.TooComplex -> println("🔥 너무 복잡: ${result.reason}")
            is SolverResult.InherentlyUnsolvable -> println("❌ 구조적으로 불가능: ${result.reason}")
            is SolverResult.UnwinnableState -> println("❌ 막다른 길: ${result.reason}")
        }
        
        // 최소한 Draw나 Tableau 이동을 찾아야 함
        val bestMove = solver.findBestMove(state)
        assertNotNull("힌트를 찾아야 함", bestMove)
        println("\n=== 최선의 이동 ===")
        println("힌트: $bestMove")
        
        // Draw가 아닌 다른 이동을 찾는 게 더 좋음 (하지만 현재는 Draw가 유일한 선택일 수 있음)
        // Tableau 간 이동 가능성 확인
        var tableauMoveFound = false
        for (from in 0..6) {
            for (to in 0..6) {
                if (from != to && engine.canMoveTableauToTableau(from, to)) {
                    println("가능한 Tableau 이동: T[$from] → T[$to]")
                    tableauMoveFound = true
                }
            }
        }
        
        if (!tableauMoveFound) {
            println("Tableau 간 이동 불가능 - Draw가 유일한 선택")
        }
    }
}
