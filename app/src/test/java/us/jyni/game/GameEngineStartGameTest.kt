package us.jyni.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.LayoutId

class GameEngineStartGameTest {

    @Test
    fun startGame_initialLayoutStructure_isValid() {
        val engine = GameEngine()
    val seed = 1234uL
        engine.startGame(seed)

        val state = engine.getGameState()

        // 7 tableau columns with sizes 1..7
        assertEquals(7, state.tableau.size)
        state.tableau.forEachIndexed { col, pile ->
            assertEquals(col + 1, pile.size)
            // last card face up, others face down
            pile.forEachIndexed { idx, card ->
                val expectedFaceUp = idx == pile.lastIndex
                assertEquals(expectedFaceUp, card.isFaceUp)
            }
        }

        // 4 foundation piles empty
        assertEquals(4, state.foundation.size)
        assertTrue(state.foundation.all { it.isEmpty() })

        // stock has 52 - (1+..+7) = 24, all face down
        assertEquals(24, state.stock.size)
        assertTrue(state.stock.all { !it.isFaceUp })

        // waste empty
        assertTrue(state.waste.isEmpty())

        // game not over
        assertEquals(false, state.isGameOver)
    }

    @Test
    fun startGame_sameSeed_sameLayoutId() {
    val seed = 42uL

        val id1 = layoutIdFromEngineSeed(seed)
        val id2 = layoutIdFromEngineSeed(seed)

        assertEquals(id1, id2)
    }

    private fun layoutIdFromEngineSeed(seed: ULong): String {
        val engine = GameEngine()
        engine.startGame(seed)
        val state = engine.getGameState()

        val encode = { c: Card -> "${suitCode(c.suit)}:${rankCode(c.rank)}:${if (c.isFaceUp) 'u' else 'd'}" }

        val tableau = state.tableau.map { col -> col.map(encode) }
        val foundation = state.foundation.map { pile -> pile.map(encode) }
        val stock = state.stock.map(encode)
        val waste = state.waste.map(encode)

        return LayoutId.generate(tableau, foundation, stock, waste, lv = 0)
    }

    private fun suitCode(s: Suit): String = when (s) {
        Suit.SPADES -> "S"
        Suit.HEARTS -> "H"
        Suit.DIAMONDS -> "D"
        Suit.CLUBS -> "C"
    }

    private fun rankCode(r: Rank): String = when (r) {
        Rank.ACE -> "A"
        Rank.TWO -> "2"
        Rank.THREE -> "3"
        Rank.FOUR -> "4"
        Rank.FIVE -> "5"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
    }
}
