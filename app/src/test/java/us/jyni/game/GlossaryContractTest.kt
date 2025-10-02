package us.jyni.game

import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryContractTest {

    @Test
    fun suitAndRankCounts() {
        assertEquals(4, Suit.values().size)
        assertEquals(13, Rank.values().size)
    }

    @Test
    fun deckHas52UniqueCards() {
        val suits = Suit.values()
        val ranks = Rank.values()
        val set = mutableSetOf<String>()
        for (s in suits) for (r in ranks) set.add("$s:$r")
        assertEquals(52, set.size)
    }

    @Test
    fun tableauHasSevenColumns() {
        val tableau = List(7) { emptyList<String>() }
        assertEquals(7, tableau.size)
    }

    @Test
    fun foundationHasFourPiles() {
        val foundation = List(4) { emptyList<String>() }
        assertEquals(4, foundation.size)
    }
}
