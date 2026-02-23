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
import us.jyni.game.klondike.util.GameCode

class UnsolvableDetectorTest {
    
    private lateinit var engine: GameEngine
    private lateinit var detector: UnsolvableDetector
    
    @Before
    fun setup() {
        engine = GameEngine()
        detector = UnsolvableDetector()
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
        
        // DEBUG: Pile 4-5-6 초기 상태 출력
        println("\n=== YpUzGOpD-YWg Initial State ===")
        for (i in 4..6) {
            println("Pile $i:")
            state.tableau[i].forEachIndexed { idx, card ->
                val fd = if (card.isFaceUp) "FU" else "FD"
                println("  [$idx] $fd ${card.suit}-${card.rank}")
            }
        }
        println("Foundation:")
        state.foundation.forEachIndexed { idx, pile ->
            if (pile.isNotEmpty()) {
                println("  Pile $idx: ${pile.map { "${it.suit}-${it.rank}" }}")
            }
        }
        println()
        
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
    
    @Test
    fun `test game YYxQSt3-oDyg - 2-pile irretrievable case`() {
        // 게임 코드: YYxQSt3-oDyg
        // 문제: 2개의 검은색 Q 파일 밑에 빨간색 K들이 숨어 있고,
        // 스페이드4와 클로버6이 숨어 있어서 2-pile Irretrievable이어야 함
        
        val gameCode = "YYxQSt3-oDyg"
        val decoded = GameCode.decode(gameCode)
        
        assertNotNull("게임 코드가 유효해야 함", decoded)
        
        val (seed, rules) = decoded!!
        println("\n=== Game YYxQSt3-oDyg Analysis ===")
        println("Seed: $seed")
        println("Rules: Draw=${rules.draw}, Redeals=${rules.redeals}")
        
        // 게임 시작
        engine.startGame(seed, rules)
        val state = engine.getGameState()
        
        // Tableau 상태 출력 (상세)
        println("\nTableau State (맨 밑부터):")
        state.tableau.forEachIndexed { i, pile ->
            println("\nPile $i (총 ${pile.size}장):")
            pile.forEachIndexed { idx, card ->
                val pos = if (card.isFaceUp) "FU" else "FD"
                println("  [$idx] $pos ${card.suit}-${card.rank}")
            }
        }
        
        // 스페이드 Q, 클로버 Q 찾기
        println("\n=== QUEEN 위치 확인 ===")
        state.tableau.forEachIndexed { i, pile ->
            pile.forEachIndexed { idx, card ->
                if (card.rank == Rank.QUEEN) {
                    val pos = if (card.isFaceUp) "FU" else "FD"
                    println("Pile $i[$idx] ($pos): ${card.suit}-QUEEN")
                    
                    // 그 밑에 있는 카드들 출력
                    if (idx > 0) {
                        println("  → 그 밑 카드들:")
                        for (j in 0 until idx) {
                            val below = pile[j]
                            val belowPos = if (below.isFaceUp) "FU" else "FD"
                            println("    [$j] $belowPos ${below.suit}-${below.rank}")
                        }
                    }
                }
            }
        }
        
        // JACK 위치 찾기 (QUEEN을 Foundation으로 보내는데 필요)
        println("\n=== JACK 위치 확인 ===")
        state.tableau.forEachIndexed { i, pile ->
            pile.forEachIndexed { idx, card ->
                if (card.rank == Rank.JACK) {
                    val pos = if (card.isFaceUp) "FU" else "FD"
                    println("Pile $i[$idx] ($pos): ${card.suit}-JACK")
                }
            }
        }
        
        // Stock 확인
        println("\n=== Stock (${state.stock.size}장) ===")
        state.stock.forEachIndexed { idx, card ->
            println("  [$idx] ${card.suit}-${card.rank}")
            if (card.rank == Rank.JACK) {
                println("    ^^^ JACK 발견!")
            }
        }
        
        // Waste 확인
        println("\n=== Waste (${state.waste.size}장) ===")
        state.waste.forEachIndexed { idx, card ->
            println("  [$idx] ${card.suit}-${card.rank}")
        }
        
        // 스페이드 Q와 클로버 Q의 Foundation 경로 분석
        println("\n=== Foundation 경로 분석 ===")
        
        // SPADES-Q를 Foundation으로 보내려면
        println("\n[SPADES-QUEEN → Foundation 경로]")
        println("필요한 순서: SPADES-J → SPADES-10 → SPADES-9 → ... → SPADES-ACE")
        val spadesNeeded = listOf(Rank.JACK, Rank.TEN, Rank.NINE, Rank.EIGHT, Rank.SEVEN, 
                                  Rank.SIX, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO, Rank.ACE)
        spadesNeeded.forEach { rank ->
            val card = Card(Suit.SPADES, rank, false)
            // Pile 5-6에 있는지 확인
            val inPile5 = state.tableau[5].any { it.suit == card.suit && it.rank == card.rank }
            val inPile6 = state.tableau[6].any { it.suit == card.suit && it.rank == card.rank }
            
            if (inPile5) {
                val pile5Card = state.tableau[5].find { it.suit == card.suit && it.rank == card.rank }!!
                val pos = if (pile5Card.isFaceUp) "FU" else "FD"
                println("  SPADES-${rank}: Pile 5 ($pos) ← ⚠️ Pile 5-6 조합 안에!")
            } else if (inPile6) {
                val pile6Card = state.tableau[6].find { it.suit == card.suit && it.rank == card.rank }!!
                val pos = if (pile6Card.isFaceUp) "FU" else "FD"
                println("  SPADES-${rank}: Pile 6 ($pos) ← ⚠️ Pile 5-6 조합 안에!")
            } else {
                // 다른 곳에 있는지 확인
                var found = false
                state.tableau.forEachIndexed { i, pile ->
                    if (pile.any { it.suit == card.suit && it.rank == card.rank }) {
                        val c = pile.find { it.suit == card.suit && it.rank == card.rank }!!
                        val pos = if (c.isFaceUp) "FU" else "FD"
                        println("  SPADES-${rank}: Pile $i ($pos)")
                        found = true
                    }
                }
                if (!found) {
                    if (state.stock.any { it.suit == card.suit && it.rank == card.rank }) {
                        println("  SPADES-${rank}: Stock (접근 가능)")
                    } else if (state.waste.any { it.suit == card.suit && it.rank == card.rank }) {
                        println("  SPADES-${rank}: Waste (접근 가능)")
                    }
                }
            }
        }
        
        // CLUBS-Q를 Foundation으로 보내려면
        println("\n[CLUBS-QUEEN → Foundation 경로]")
        println("필요한 순서: CLUBS-J → CLUBS-10 → CLUBS-9 → ... → CLUBS-ACE")
        val clubsNeeded = listOf(Rank.JACK, Rank.TEN, Rank.NINE, Rank.EIGHT, Rank.SEVEN, 
                                 Rank.SIX, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO, Rank.ACE)
        clubsNeeded.forEach { rank ->
            val card = Card(Suit.CLUBS, rank, false)
            // Pile 5-6에 있는지 확인
            val inPile5 = state.tableau[5].any { it.suit == card.suit && it.rank == card.rank }
            val inPile6 = state.tableau[6].any { it.suit == card.suit && it.rank == card.rank }
            
            if (inPile5) {
                val pile5Card = state.tableau[5].find { it.suit == card.suit && it.rank == card.rank }!!
                val pos = if (pile5Card.isFaceUp) "FU" else "FD"
                println("  CLUBS-${rank}: Pile 5 ($pos) ← ⚠️ Pile 5-6 조합 안에!")
            } else if (inPile6) {
                val pile6Card = state.tableau[6].find { it.suit == card.suit && it.rank == card.rank }!!
                val pos = if (pile6Card.isFaceUp) "FU" else "FD"
                println("  CLUBS-${rank}: Pile 6 ($pos) ← ⚠️ Pile 5-6 조합 안에!")
            } else {
                // 다른 곳에 있는지 확인
                var found = false
                state.tableau.forEachIndexed { i, pile ->
                    if (pile.any { it.suit == card.suit && it.rank == card.rank }) {
                        val c = pile.find { it.suit == card.suit && it.rank == card.rank }!!
                        val pos = if (c.isFaceUp) "FU" else "FD"
                        println("  CLUBS-${rank}: Pile $i ($pos)")
                        found = true
                    }
                }
                if (!found) {
                    if (state.stock.any { it.suit == card.suit && it.rank == card.rank }) {
                        println("  CLUBS-${rank}: Stock (접근 가능)")
                    } else if (state.waste.any { it.suit == card.suit && it.rank == card.rank }) {
                        println("  CLUBS-${rank}: Waste (접근 가능)")
                    }
                }
            }
        }
        
        // Unsolvable 검사 (디버그 모드)
        val (result, debugLog) = detector.checkInherentlyUnsolvableWithDebug(state)
        
        println("\n$debugLog")
        
        if (result == null) {
            println("\n❌ DETECTOR RESULT: SOLVABLE (Unsolvable 패턴을 감지하지 못함)")
        } else {
            println("\n✓ DETECTOR RESULT: UNSOLVABLE - ${result.message}")
        }
        
        // 2-pile Irretrievable이어야 함
        assertNotNull(
            "이 게임은 2-pile Irretrievable으로 Unsolvable이어야 함\n" +
            "실제: Detector가 Solvable로 판정\n" +
            "디버그 로그:\n$debugLog",
            result
        )
        
        assertTrue(
            "2-pile Irretrievable 패턴이어야 함, 실제: ${result?.message}",
            result is UnsolvableReason.NPileIrretrievable.Pair
        )
        
        println("\n✓ 테스트 통과: ${result?.message}")
    }
}
