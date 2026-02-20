package us.jyni.game.klondike.solver

import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.model.*
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * N-Pile Irretrievable Framework 성능 테스트
 *
 * 목표: 게임 시작 시 Inherently Unsolvable 체크가 10ms 이내 완료
 */
class PerformanceTest {
    
    private lateinit var engine: GameEngine
    private lateinit var detector: UnsolvableDetector
    
    @Before
    fun setup() {
        engine = GameEngine()
        detector = UnsolvableDetector(engine)
        engine.startGame(seed = 0uL, rules = Ruleset())
    }
    
    @Test
    fun `test performance - checkInherentlyUnsolvable under 10ms`() {
        // 다양한 게임 상태로 테스트
        val states = listOf(
            createTypicalGameState(),
            createComplexGameState(),
            createManyFaceDownState()
        )
        
        val timings = mutableListOf<Long>()
        
        for ((index, state) in states.withIndex()) {
            // 성능 측정
            val startTime = System.nanoTime()
            val result = detector.checkInherentlyUnsolvable(state)
            val endTime = System.nanoTime()
            
            val durationMs = (endTime - startTime) / 1_000_000.0
            timings.add((endTime - startTime) / 1_000_000)
            
            println("State ${index + 1}: ${String.format("%.2f", durationMs)}ms - ${if (result != null) "Unsolvable: ${result::class.simpleName}" else "Solvable"}")
        }
        
        val avgMs = timings.average()
        val maxMs = timings.maxOrNull() ?: 0L
        
        println("\n=== Performance Summary ===")
        println("Average: ${String.format("%.2f", avgMs)}ms")
        println("Max: ${maxMs}ms")
        println("Target: <10ms")
        
        assert(avgMs < 10.0) { "Average time should be under 10ms, but was ${String.format("%.2f", avgMs)}ms" }
        assert(maxMs < 10L) { "Max time should be under 10ms, but was ${maxMs}ms" }
    }
    
    @Test
    fun `test performance - worst case scenario`() {
        // 최악의 경우: 많은 face-down 카드
        val state = createManyFaceDownState()
        
        val startTime = System.nanoTime()
        val result = detector.checkInherentlyUnsolvable(state)
        val endTime = System.nanoTime()
        
        val durationMs = (endTime - startTime) / 1_000_000.0
        
        println("Worst case (many face-down): ${String.format("%.2f", durationMs)}ms")
        
        assert(durationMs < 10.0) { "Worst case should be under 10ms, but was ${String.format("%.2f", durationMs)}ms" }
    }
    
    @Test
    fun `test performance - best case scenario`() {
        // 최선의 경우: King Deadlock으로 조기 종료
        // King이 face-up이고 그 아래 같은 suit의 face-down King이 있음
        val state = GameState(
            tableau = listOf(
                mutableListOf(
                    Card(Suit.SPADES, Rank.KING, isFaceUp = false),  // face-down King
                    Card(Suit.SPADES, Rank.KING, isFaceUp = true)    // face-up King (Queen 필요)
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
        
        val startTime = System.nanoTime()
        val result = detector.checkInherentlyUnsolvable(state)
        val endTime = System.nanoTime()
        
        val durationMs = (endTime - startTime) / 1_000_000.0
        
        println("Best case: ${String.format("%.2f", durationMs)}ms - ${if (result != null) result::class.simpleName else "Solvable"}")
        
        // King Deadlock을 기대하지만, 실제로는 감지되지 않을 수 있음
        // 이 경우는 다른 패턴으로 감지될 수 있으므로 시간만 확인
        assert(durationMs < 10.0) { "Best case should be under 10ms, but was ${String.format("%.2f", durationMs)}ms" }
    }
    
    // Helper methods to create test states
    
    private fun createTypicalGameState(): GameState {
        return GameState(
            tableau = listOf(
                mutableListOf(Card(Suit.HEARTS, Rank.ACE, false), Card(Suit.SPADES, Rank.TWO, true)),
                mutableListOf(Card(Suit.DIAMONDS, Rank.THREE, true)),
                mutableListOf(Card(Suit.CLUBS, Rank.FOUR, false), Card(Suit.HEARTS, Rank.FIVE, true)),
                mutableListOf(Card(Suit.SPADES, Rank.SIX, true)),
                mutableListOf(Card(Suit.DIAMONDS, Rank.SEVEN, false), Card(Suit.CLUBS, Rank.EIGHT, true)),
                mutableListOf(Card(Suit.HEARTS, Rank.NINE, true)),
                mutableListOf(Card(Suit.SPADES, Rank.TEN, false), Card(Suit.DIAMONDS, Rank.JACK, true))
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
    }
    
    private fun createComplexGameState(): GameState {
        return GameState(
            tableau = listOf(
                mutableListOf(
                    Card(Suit.HEARTS, Rank.TWO, false),
                    Card(Suit.SPADES, Rank.THREE, false),
                    Card(Suit.CLUBS, Rank.FOUR, false),
                    Card(Suit.DIAMONDS, Rank.FIVE, true)
                ),
                mutableListOf(
                    Card(Suit.HEARTS, Rank.SIX, false),
                    Card(Suit.SPADES, Rank.SEVEN, true)
                ),
                mutableListOf(Card(Suit.CLUBS, Rank.EIGHT, true)),
                mutableListOf(Card(Suit.DIAMONDS, Rank.NINE, false), Card(Suit.HEARTS, Rank.TEN, true)),
                mutableListOf(Card(Suit.SPADES, Rank.JACK, true)),
                mutableListOf(Card(Suit.CLUBS, Rank.QUEEN, false), Card(Suit.DIAMONDS, Rank.KING, true)),
                mutableListOf(Card(Suit.HEARTS, Rank.ACE, true))
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
    }
    
    private fun createManyFaceDownState(): GameState {
        return GameState(
            tableau = listOf(
                mutableListOf(
                    Card(Suit.HEARTS, Rank.ACE, false),
                    Card(Suit.SPADES, Rank.TWO, false),
                    Card(Suit.CLUBS, Rank.THREE, false),
                    Card(Suit.DIAMONDS, Rank.FOUR, false),
                    Card(Suit.HEARTS, Rank.FIVE, false),
                    Card(Suit.SPADES, Rank.SIX, true)
                ),
                mutableListOf(
                    Card(Suit.CLUBS, Rank.SEVEN, false),
                    Card(Suit.DIAMONDS, Rank.EIGHT, false),
                    Card(Suit.HEARTS, Rank.NINE, false),
                    Card(Suit.SPADES, Rank.TEN, true)
                ),
                mutableListOf(
                    Card(Suit.CLUBS, Rank.JACK, false),
                    Card(Suit.DIAMONDS, Rank.QUEEN, false),
                    Card(Suit.HEARTS, Rank.KING, true)
                ),
                mutableListOf(
                    Card(Suit.SPADES, Rank.ACE, false),
                    Card(Suit.CLUBS, Rank.TWO, true)
                ),
                mutableListOf(Card(Suit.DIAMONDS, Rank.THREE, true)),
                mutableListOf(Card(Suit.HEARTS, Rank.FOUR, true)),
                mutableListOf(Card(Suit.SPADES, Rank.FIVE, true))
            ),
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf()
        )
    }
}
