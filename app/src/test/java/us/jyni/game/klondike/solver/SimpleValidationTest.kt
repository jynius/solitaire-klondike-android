package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

/**
 * 가장 간단한 시나리오로 Solver 검증
 */
class SimpleValidationTest {
    
    @Test
    fun simple_move_validation() {
        // ♦7을 ♠8에 놓을 수 있는지 직접 검증
        val card = Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true)
        val tableau = listOf(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        // 색상 체크
        val cardColor = card.suit.ordinal % 2  // DIAMONDS = 1, 1%2 = 1 (RED)
        val targetColor = tableau.last().suit.ordinal % 2  // SPADES = 0, 0%2 = 0 (BLACK)
        val oppositeColor = cardColor != targetColor
        
        println("카드 색상: $cardColor (${card.suit})")
        println("목표 색상: $targetColor (${tableau.last().suit})")
        println("반대 색상: $oppositeColor")
        
        // 랭크 체크
        val oneRankLower = card.rank.value == tableau.last().rank.value - 1  // 7 == 8-1
        println("랭크 검사: ${card.rank.value} == ${tableau.last().rank.value - 1} = $oneRankLower")
        
        assertTrue("반대 색상이어야 함", oppositeColor)
        assertTrue("1 낮은 랭크여야 함", oneRankLower)
    }
    
    @Test
    fun solver_can_find_simple_move() {
        val solver = AStarSolver()
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♠8
        state.tableau[0].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        // T[1]: ♦7
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        println("찾은 힌트: $hint")
        
        if (hint == null) {
            println("\n=== 디버깅 정보 ===")
            println("T[0]: ${state.tableau[0]}")
            println("T[1]: ${state.tableau[1]}")
            println("Stock: ${state.stock.size}장")
            println("Waste: ${state.waste.size}장")
        }
        
        assertNotNull("힌트를 찾아야 함", hint)
    }
}
