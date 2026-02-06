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
        solver = BFSSolver(engine)
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
        
        assertTrue("Should be solvable", result is SolverResult.Success)
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
        val state = GameState()
        
        // Stock과 Waste 비움
        state.stock.clear()
        state.waste.clear()
        
        // Tableau에 이동 불가능한 상태 만들기
        // 모든 컬럼이 같은 색상의 카드로 끝남 (이동 불가)
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true))
        state.tableau[2].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        state.tableau[3].add(Card(Suit.DIAMONDS, Rank.FOUR, isFaceUp = true))
        state.tableau[4].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        state.tableau[5].add(Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true))
        state.tableau[6].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        
        // Foundation: 비어있음 (Ace만 올릴 수 있는데 이미 Tableau에 있음)
        
        val detector = UnsolvableDetector(engine)
        val unsolvable = detector.check(state)
        
        assertNotNull("Should detect dead end", unsolvable)
        assertTrue("Should be DeadEnd type", unsolvable is UnsolvableReason.DeadEnd)
    }
    
    @Test
    fun unsolvable_king_deadlock() {
        val state = GameState()
        
        // 모든 빈 컬럼 제거
        for (i in 0..6) {
            state.tableau[i].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = false))
        }
        
        // 한 컬럼에 킹이 맨 위, 밑에 뒷면 카드
        state.tableau[0].clear()
        state.tableau[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
        
        val detector = UnsolvableDetector(engine)
        val unsolvable = detector.check(state)
        
        assertNotNull("Should detect king deadlock", unsolvable)
        assertTrue("Should be KingDeadlock type", unsolvable is UnsolvableReason.KingDeadlock)
    }
    
    @Test
    fun find_hint_for_simple_game() {
        val state = GameState()
        
        // Foundation에 A만 있음
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        
        // Tableau에 ♥2가 있음
        state.tableau[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        setEngineState(state)
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("Should find a hint", hint)
        assertEquals("Hint should be to move 2 to Foundation", 
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
        // 여러 경로가 있는 게임
        val state = GameState()
        
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.foundation[1].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        
        state.tableau[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        state.tableau[1].add(Card(Suit.SPADES, Rank.TWO, isFaceUp = true))
        state.tableau[2].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        state.tableau[3].add(Card(Suit.SPADES, Rank.THREE, isFaceUp = true))
        
        setEngineState(state)
        
        val result = solver.solve(state)
        
        assertTrue("Should find a solution", result is SolverResult.Success)
        if (result is SolverResult.Success) {
            assertTrue("Should explore multiple states", result.statesExplored > 1)
            assertTrue("Should have at least 4 moves", result.moves.size >= 4)
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
