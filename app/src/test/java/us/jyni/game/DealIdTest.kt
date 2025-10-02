package us.jyni.game

import us.jyni.game.klondike.util.sync.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DealIdTest {
    private fun canonical52(): MutableList<String> {
        val suits = listOf("S","H","D","C")
        val ranks = listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K")
        val deck = mutableListOf<String>()
        for (s in suits) for (r in ranks) deck.add("$s:$r")
        return deck
    }

    @Test
    fun sameSeedSameDealId() {
        val rules = Ruleset(draw = 1, redeals = -1, recycle = RecycleOrder.REVERSE)
        val seed = 123456789uL

        val d1 = canonical52()
        fisherYatesShuffle(d1, seed)
        val id1 = DealId.generate(d1, rules, seed)

        val d2 = canonical52()
        fisherYatesShuffle(d2, seed)
        val id2 = DealId.generate(d2, rules, seed)

        assertEquals(id1, id2)
    }
}
