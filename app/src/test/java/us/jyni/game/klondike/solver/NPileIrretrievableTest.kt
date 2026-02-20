package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.model.*
import us.jyni.game.klondike.engine.GameEngine

/**
 * N-Pile Irretrievable 프레임워크 테스트
 */
class NPileIrretrievableTest {
    
    private lateinit var engine: GameEngine
    private lateinit var detector: UnsolvableDetector
    
    @Before
    fun setup() {
        engine = GameEngine()
        detector = UnsolvableDetector(engine)
    }
    
    // ========== 1. Single Irretrievable (N=1) Tests ==========
    
    @Test
    fun `test single irretrievable - basic case`() {
        val state = GameState(
            tableau = listOf(
                // Pile 0: 하트2가 Single Irretrievable
                mutableListOf(
                    Card(Suit.HEARTS, Rank.ACE, isFaceUp = false),      // Foundation 필요
                    Card(Suit.SPADES, Rank.THREE, isFaceUp = false),    // Tableau 필요
                    Card(Suit.CLUBS, Rank.THREE, isFaceUp = false),     // Tableau 필요
                    Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
                ),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNotNull("하트2는 Single Irretrievable이어야 함", result)
        assertTrue(result is UnsolvableReason.NPileIrretrievable.Single)
    }
    
    @Test
    fun `test single retrievable - foundation path available`() {
        val state = GameState(
            tableau = listOf(
                mutableListOf(
                    Card(Suit.SPADES, Rank.THREE, isFaceUp = false),
                    Card(Suit.CLUBS, Rank.THREE, isFaceUp = false),
                    Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
                    // 하트A는 없음!
                ),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = listOf(
                mutableListOf(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)),  // Foundation 경로 가능!
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            stock = mutableListOf(),
            waste = mutableListOf()
        )
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull("하트2는 Foundation으로 갈 수 있음", result)
    }
    
    @Test
    fun `test single retrievable - tableau path available`() {
        val state = GameState(
            tableau = listOf(
                // Pile 0
                mutableListOf(
                    Card(Suit.HEARTS, Rank.ACE, isFaceUp = false),
                    Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
                ),
                // Pile 1: Tableau 경로 제공
                mutableListOf(
                    Card(Suit.SPADES, Rank.THREE, isFaceUp = true)
                ),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull("하트2는 스페이드3 위에 갈 수 있음", result)
    }
    
    // ========== 2. Pair Irretrievable (N=2) Tests ==========
    
    @Test
    fun `test pair irretrievable - mutual blocking`() {
        val state = GameState(
            tableau = listOf(
                // Pile 0: 하트3
                mutableListOf(
                    Card(Suit.HEARTS, Rank.ACE, isFaceUp = false),
                    Card(Suit.HEARTS, Rank.TWO, isFaceUp = false),
                    Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
                ),
                // Pile 1: 다이아3 (서로 블로킹)
                mutableListOf(
                    Card(Suit.SPADES, Rank.FOUR, isFaceUp = false),    // 하트3이 필요
                    Card(Suit.CLUBS, Rank.FOUR, isFaceUp = false),     // 하트3이 필요
                    Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = true)
                ),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNotNull("Pile 0과 1이 서로 블로킹", result)
        // Note: Single이 먼저 감지될 수 있음 (하트3도 single irretrievable)
        assertTrue(result is UnsolvableReason.NPileIrretrievable)
    }
    
    // ========== 3. Integration Tests ==========
    
    @Test
    fun `test solvable game - no irretrievable cards`() {
        val state = createSimpleSolvableState()
        
        val result = detector.checkInherentlyUnsolvable(state)
        
        assertNull("Solvable 게임", result)
    }
    
    @Test
    fun `test dead end detection`() {
        val state = GameState(
            tableau = listOf(
                mutableListOf(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)),
                mutableListOf(Card(Suit.DIAMONDS, Rank.FOUR, isFaceUp = true)),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = listOf(
                mutableListOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            stock = mutableListOf(),  // Stock 비어있음
            waste = mutableListOf()   // Waste 비어있음
        )
        
        // Dead End는 check() 메서드로 감지
        val result = detector.check(state)
        
        assertNotNull("Dead End 발생", result)
        assertTrue(result is UnsolvableReason.DeadEnd)
    }
    
    // ========== Helper Functions ==========
    
    private fun createSimpleSolvableState(): GameState {
        return GameState(
            tableau = listOf(
                mutableListOf(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)),
                mutableListOf(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)),
                mutableListOf(Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)),
                mutableListOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
    }
}
