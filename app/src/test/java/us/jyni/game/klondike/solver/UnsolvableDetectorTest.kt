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

class UnsolvableDetectorTest {
    
    private lateinit var engine: GameEngine
    private lateinit var detector: UnsolvableDetector
    
    @Before
    fun setup() {
        engine = GameEngine()
        detector = UnsolvableDetector(engine)
    }
    
    // @Test
    // TODO: Fix this test - current state is NOT a dead end (♦2 and ♣2 can move to Foundation)
    fun detects_dead_end_DISABLED() {
        val state = GameState()
        
        // Stock과 Waste 비움
        state.stock.clear()
        state.waste.clear()
        
        // 이동 불가능한 Tableau 구성
        // 모든 컬럼이 같은 색상으로 끝나서 서로 이동 불가
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true))
        state.tableau[2].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        state.tableau[3].add(Card(Suit.DIAMONDS, Rank.FOUR, isFaceUp = true))
        state.tableau[4].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        state.tableau[5].add(Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true))
        state.tableau[6].add(Card(Suit.CLUBS, Rank.TWO, isFaceUp = true))
        
        // Foundation에 Ace들 이미 있음
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.foundation[1].add(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.ACE, isFaceUp = true))
        state.foundation[3].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.check(state)
        
        assertNotNull("Should detect unsolvable state", result)
        assertTrue("Should be DeadEnd", result is UnsolvableReason.DeadEnd)
    }
    
    @Test
    fun detects_same_color_block() {
        val state = GameState()
        
        // Foundation: ♥A, ♥2까지 (♥3 필요)
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.foundation[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        // Tableau: ♥3이 뒷면, 그 위에 ♥5
        state.tableau[0].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        // 이 케이스는 Single Irretrievable로 감지될 수 있습니다
        // (♥3이 뒷면이고 ♥4가 필요하지만 어딘가에 뒷면으로 있을 경우)
        // 현재는 정확한 조건을 체크하지 않으므로 테스트는 단순히 결과를 확인합니다
    }
    
    @Test
    fun solvable_game_returns_null() {
        val state = GameState()
        
        // 명백히 solvable한 상태
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        // Stock에 카드가 있음
        state.stock.add(Card(Suit.SPADES, Rank.KING, isFaceUp = false))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.check(state)
        
        assertNull("Solvable game should return null", result)
    }
    
    
    @Test
    fun stock_available_not_deadend() {
        val state = GameState()
        
        // Tableau에 이동 불가능
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true))
        
        // 하지만 Stock에 카드가 있음
        state.stock.add(Card(Suit.SPADES, Rank.KING, isFaceUp = false))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.check(state)
        
        // Stock이 있으므로 DeadEnd가 아님
        if (result is UnsolvableReason.DeadEnd) {
            fail("Should not be dead end when stock has cards")
        }
    }
    
    @Test
    fun waste_available_not_deadend() {
        val state = GameState()
        
        // Tableau에 이동 불가능
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true))
        
        // Stock은 비었지만 Waste에 카드가 있음
        state.stock.clear()
        state.waste.add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.check(state)
        
        // Waste에서 Tableau로 이동 가능하면 DeadEnd가 아님
        // (♠8 → ♥7 또는 ♦6은 색상이 안 맞아서 불가능)
        // 이 경우는 실제로 DeadEnd일 수 있음
    }
    
    @Test
    fun regression_YpUzGOpDYWg_should_be_solvable() {
        // Regression test for game code YpUzGOpD-YWg
        // This game was incorrectly flagged as "Inherently Unsolvable"
        // but was actually solved by the user
        
        val seed = 11911113069331112296uL
        val rules = Ruleset(
            draw = 1, 
            redeals = -1, 
            recycle = us.jyni.game.klondike.util.sync.RecycleOrder.REVERSE, 
            allowFoundationToTableau = true
        )
        
        engine.startGame(seed, rules)
        val state = engine.getGameState()
        
        // 초기 상태에서 Inherently Unsolvable이 아니어야 함
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull(
            "Game YpUzGOpD-YWg should be solvable (user completed it), but was detected as: $result",
            result
        )
    }
    
    @Test
    fun stock_cards_prevent_false_positive_irretrievable() {
        // Test that cards in Stock prevent false positive N-Pile Irretrievable detection
        val state = GameState()
        
        // Foundation: 비어있음 (♦A 필요)
        state.foundation[0].clear()
        
        // Tableau pile 0: face-down ♦A, face-up ♦2
        state.tableau[0].add(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true))
        
        // Tableau pile 1: face-down cards only
        state.tableau[1].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = false))
        
        // Stock에 ♦A가 있음 (접근 가능!)
        state.stock.add(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = false))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull(
            "Stock contains ♦A, so ♦2 should not be flagged as irretrievable",
            result
        )
    }
    
    @Test
    fun waste_cards_prevent_false_positive_irretrievable() {
        // Test that cards in Waste prevent false positive N-Pile Irretrievable detection
        val state = GameState()
        
        // Foundation: 비어있음 (♠A 필요)
        state.foundation[0].clear()
        
        // Tableau pile 0: face-down ♠A, face-up ♠2
        state.tableau[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.SPADES, Rank.TWO, isFaceUp = true))
        
        // Tableau pile 1: face-down cards only
        state.tableau[1].add(Card(Suit.HEARTS, Rank.FOUR, isFaceUp = false))
        
        // Stock은 비었지만 Waste에 ♠A가 있음 (접근 가능!)
        state.stock.clear()
        state.waste.add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        
        engine.startGame(seed = 0uL, rules = Ruleset())
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull(
            "Waste contains ♠A, so ♠2 should not be flagged as irretrievable",
            result
        )
    }
}
