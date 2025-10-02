package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class SaveRestoreTest {
    @Test
    fun save_and_restore_preserves_state_and_rules() {
        val rules = Ruleset(draw = 3, redeals = 1, recycle = RecycleOrder.REVERSE, allowFoundationToTableau = false)
        val e1 = GameEngine()
        e1.startGame(seed = 123uL, rules = rules)

        // some draws to mutate state
        repeat(5) { e1.draw() }
        val s1 = e1.getGameState()
        val sig1 = sig(s1)
        val rr1 = e1.getRedealsRemaining()

        val saved = e1.saveStateString()

        val e2 = GameEngine()
        val ok = e2.restoreStateString(saved)
        assertTrue(ok)

        assertEquals(rules, e2.getRules())
        assertEquals(rr1, e2.getRedealsRemaining())
        val s2 = e2.getGameState()
        assertEquals(sig1, sig(s2))

        // continue gameplay should be consistent (at least first draw effect)
        val moved1 = e1.draw()
        val moved2 = e2.draw()
        assertEquals(moved1, moved2)
    }

    private fun sig(s: us.jyni.game.klondike.model.GameState): String {
        val t = s.tableau.joinToString("|") { col -> col.size.toString() + ":" + col.count { it.isFaceUp } }
        val f = s.foundation.joinToString(",") { it.size.toString() }
        return listOf(t, f, s.stock.size, s.waste.size, s.isGameOver).joinToString("#")
    }
}
