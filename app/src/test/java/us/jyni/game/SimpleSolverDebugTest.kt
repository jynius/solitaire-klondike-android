package us.jyni.game

import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.solver.BFSSolver
import us.jyni.game.klondike.solver.GameStateUtils
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class SimpleSolverDebugTest {
    @Test
    fun test_getAllPossibleMoves() {
        val engine = GameEngine()
        val solver = BFSSolver()
        
        val state = GameState()
        
        // Foundation: ♣J까지
        state.foundation[2].add(Card(Suit.CLUBS, Rank.ACE, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.TWO, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.THREE, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.SIX, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.NINE, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.TEN, isFaceUp = true))
        state.foundation[2].add(Card(Suit.CLUBS, Rank.JACK, isFaceUp = true))
        
        // Tableau: ♣Q
        state.tableau[0].add(Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true))
        
        // getAllPossibleMoves는 private이므로 reflection이나 다른 방법 필요
        // 대신 applyMove를 직접 테스트
        val move = us.jyni.game.klondike.solver.Move.TableauToFoundation(0, 2)
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        if (newState != null) {
            assertEquals("Foundation should have 12 cards", 12, newState.foundation[2].size)
            assertEquals("Tableau should be empty", 0, newState.tableau[0].size)
        }
    }
}
