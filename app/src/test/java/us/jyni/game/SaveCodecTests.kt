package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.SaveCodec
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class SaveCodecTests {

    @Test
    fun encode_includes_dealId_when_provided_and_decodes_back() {
        val state = GameState(
            tableau = List(7) { mutableListOf() },
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false
        )
        val rules = Ruleset(draw = 3, redeals = 1, recycle = RecycleOrder.KEEP, allowFoundationToTableau = true)
        val encoded = SaveCodec.encode(state, rules, redealsRemaining = 1, dealId = "DL1_ABC")
        assertTrue(encoded.contains("did=DL1_ABC"))

        val decoded = SaveCodec.decode(encoded)
        assertEquals("DL1_ABC", decoded.dealId)
        assertEquals(1, decoded.redealsRemaining)
        assertEquals(rules, decoded.rules)
    }

    @Test
    fun decode_handles_missing_dealId_for_backward_compat() {
        val state = GameState(
            tableau = List(7) { mutableListOf() },
            foundation = List(4) { mutableListOf() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false
        )
        val rules = Ruleset(draw = 1, redeals = -1, recycle = RecycleOrder.REVERSE, allowFoundationToTableau = false)
        // encode without dealId (defaults to null)
        val encoded = SaveCodec.encode(state, rules, redealsRemaining = -1)
        assertFalse(encoded.contains("did="))

        val decoded = SaveCodec.decode(encoded)
        assertNull(decoded.dealId)
        assertEquals(rules, decoded.rules)
    }
}
