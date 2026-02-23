package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.engine.GameEngine

/**
 * 가장 간단한 시나리오로 Solver 검증
 */
class SimpleValidationTest {
    
    @Test
    fun simple_move_validation() {
        // ♦7을 ♠8에 놓을 수 있는지 직접 검증
        val card = Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true)
        val tableau = listOf(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        // 색상 체크 (Suit.isRed() 사용)
        val cardIsRed = card.suit.isRed()
        val targetIsRed = tableau.last().suit.isRed()
        val oppositeColor = cardIsRed != targetIsRed
        
        println("카드 색상: ${if (cardIsRed) "RED" else "BLACK"} (${card.suit})")
        println("목표 색상: ${if (targetIsRed) "RED" else "BLACK"} (${tableau.last().suit})")
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
        
        // 거의 완성된 게임에서 간단한 이동
        val state = GameState()
        
        // Foundation: 대부분 완성
        state.foundation[0].addAll(createFoundationPile(Suit.HEARTS, 13))
        state.foundation[1].addAll(createFoundationPile(Suit.DIAMONDS, 13))
        state.foundation[2].addAll(createFoundationPile(Suit.CLUBS, 12))  // ♣Q까지
        state.foundation[3].addAll(createFoundationPile(Suit.SPADES, 13))
        
        // Tableau: ♣K만 남음
        state.tableau[0].add(Card(Suit.CLUBS, Rank.KING, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        println("찾은 힌트: $hint")
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertEquals("♣K을 Foundation으로 이동", Move.TableauToFoundation(0, 2), hint)
    }
    
    private fun createFoundationPile(suit: Suit, upTo: Int): MutableList<Card> {
        val pile = mutableListOf<Card>()
        val ranks = Rank.values()
        for (i in 0 until upTo) {
            pile.add(Card(suit, ranks[i], isFaceUp = true))
        }
        return pile
    }
}
