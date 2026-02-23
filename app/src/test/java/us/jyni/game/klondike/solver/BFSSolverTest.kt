package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.Ruleset

class BFSSolverTest {
    
    private lateinit var engine: GameEngine
    private lateinit var solver: BFSSolver
    
    @Before
    fun setup() {
        engine = GameEngine()
        solver = BFSSolver()  // 이제 engine 없이 생성 가능
    }
    
    @Test
    fun simple_winning_game_3_moves() {
        // 거의 다 완성된 게임 (3장만 남음)
        val state = GameState()
        
        // Foundation: 각 무늬별로 K까지 (마지막 3장 제외)
        state.foundation[0].addAll(createFoundationPile(Suit.HEARTS, 13)) // ♥K까지
        state.foundation[1].addAll(createFoundationPile(Suit.DIAMONDS, 13)) // ♦K까지
        state.foundation[2].addAll(createFoundationPile(Suit.CLUBS, 11))   // ♣J까지 (Q, K 남음)
        state.foundation[3].addAll(createFoundationPile(Suit.SPADES, 13))  // ♠K까지
        
        // Tableau: ♣Q, ♣K 만 남음
        state.tableau[0].add(Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.CLUBS, Rank.KING, isFaceUp = true))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        // 상태 강제 설정 (테스트용)
        setEngineState(state)
        
        val result = solver.solve(state)
        
        // 디버깅: result 타입을 assert 메시지에 포함
        val resultMsg = when (result) {
            is SolverResult.Success -> "Success with ${result.moves.size} moves"
            is SolverResult.InherentlyUnsolvable -> "InherentlyUnsolvable: ${result.reason}"
            is SolverResult.UnwinnableState -> "UnwinnableState: ${result.reason}"
            is SolverResult.Timeout -> "Timeout: ${result.reason}"
            is SolverResult.TooComplex -> "TooComplex: ${result.reason}"
        }
        
        assertTrue("Should be solvable but got: $resultMsg", result is SolverResult.Success)
        if (result is SolverResult.Success) {
            assertTrue("Should have 2 moves", result.moves.size == 2)
            assertEquals("First move should be Q to Foundation", 
                Move.TableauToFoundation(0, 2), result.moves[0])
            assertEquals("Second move should be K to Foundation", 
                Move.TableauToFoundation(1, 2), result.moves[1])
        }
    }
    
    @Test
    fun unsolvable_deadend() {
        // Dead end 검사는 실제 게임에서 발생하는 상황을 사용
        // 특정 seed로 dead end 상태를 만들거나, UnsolvableDetector 단위 테스트에서 확인
        // 이 테스트는 제거하고 DeadEndDetectionTest에서 더 정확히 테스트
        
        // 간단한 dead end: Foundation에 거의 완성, Tableau에서 이동 불가
        val state = GameState()
        
        // Foundation: 모든 카드가 거의 완성 (각 무늬 K까지)
        state.foundation[0].addAll(createFoundationPile(Suit.HEARTS, 13))
        state.foundation[1].addAll(createFoundationPile(Suit.DIAMONDS, 13))
        state.foundation[2].addAll(createFoundationPile(Suit.CLUBS, 13))
        state.foundation[3].addAll(createFoundationPile(Suit.SPADES, 13))
        
        // 이미 게임이 끝난 상태
        state.isGameOver = true
        
        val detector = UnsolvableDetector()
        val unsolvable = detector.check(state)
        
        // 게임이 이미 끝났으므로 dead end는 아님 (게임 완료)
        // 실제 dead end 테스트는 DeadEndDetectionTest에서 수행
        assertNull("Completed game is not unsolvable", unsolvable)
    }
    
    @Test
    fun find_hint_for_simple_game() {
        // 거의 완성된 게임 - 힌트 테스트
        val state = GameState()
        
        // Foundation: 각 무늬별로 거의 완성 (마지막 몇 장 제외)
        state.foundation[0].addAll(createFoundationPile(Suit.HEARTS, 12)) // ♥Q까지
        state.foundation[1].addAll(createFoundationPile(Suit.DIAMONDS, 13)) // ♦K까지
        state.foundation[2].addAll(createFoundationPile(Suit.CLUBS, 13))   // ♣K까지
        state.foundation[3].addAll(createFoundationPile(Suit.SPADES, 13))  // ♠K까지
        
        // Tableau: ♥K만 남음 (힌트는 이것을 Foundation으로)
        state.tableau[0].add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("Should find a hint", hint)
        assertEquals("Hint should be to move K to Foundation", 
            Move.TableauToFoundation(0, 0), hint)
    }
    
    @Test
    fun timeout_on_complex_game() {
        // 복잡한 게임으로 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        val state = engine.getGameState()
        
        val startTime = System.currentTimeMillis()
        val result = solver.solve(state)
        val duration = System.currentTimeMillis() - startTime
        
        // 타임아웃 또는 너무 복잡함으로 종료되어야 함
        assertTrue("Should timeout or be too complex", 
            result is SolverResult.Timeout || result is SolverResult.TooComplex)
        
        // 5초 이내에 종료되어야 함
        assertTrue("Should finish within timeout", duration < 6000)
    }
    
    @Test
    fun solver_explores_multiple_paths() {
        // 여러 경로가 있는 게임 - 거의 완성된 상태에서 여러 선택지
        val state = GameState()
        
        // Foundation: 각 무늬별로 거의 완성
        state.foundation[0].addAll(createFoundationPile(Suit.HEARTS, 11))   // ♥J까지 (Q, K 남음)
        state.foundation[1].addAll(createFoundationPile(Suit.DIAMONDS, 11)) // ♦J까지 (Q, K 남음)
        state.foundation[2].addAll(createFoundationPile(Suit.CLUBS, 13))    // ♣K까지
        state.foundation[3].addAll(createFoundationPile(Suit.SPADES, 13))   // ♠K까지
        
        // Tableau: 여러 컬럼에 분산 (여러 경로 가능)
        state.tableau[0].add(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.QUEEN, isFaceUp = true))
        state.tableau[2].add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        state.tableau[3].add(Card(Suit.DIAMONDS, Rank.KING, isFaceUp = true))
        
        val result = solver.solve(state)
        
        assertTrue("Should find a solution", result is SolverResult.Success)
        if (result is SolverResult.Success) {
            assertTrue("Should explore multiple states", result.statesExplored > 1)
            assertTrue("Should have 4 moves", result.moves.size == 4)
        }
    }
    
    // Helper functions
    
    private fun createFoundationPile(suit: Suit, upTo: Int): MutableList<Card> {
        val pile = mutableListOf<Card>()
        val ranks = Rank.values()
        for (i in 0 until upTo) {
            pile.add(Card(suit, ranks[i], isFaceUp = true))
        }
        return pile
    }
    
    private fun setEngineState(state: GameState) {
        // GameEngine의 상태를 직접 설정하는 헬퍼
        // 실제 구현에서는 GameEngine에 setState 메소드가 필요할 수 있음
        // 현재는 solve()가 전달받은 state를 사용하므로 필요 없을 수도 있음
    }
}
