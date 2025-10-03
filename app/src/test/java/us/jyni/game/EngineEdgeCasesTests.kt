package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class EngineEdgeCasesTests {

    @Test
    fun save_restore_preserves_redeals_remaining_and_rules() {
        val e = GameEngine()
        val rules = Ruleset(draw = 3, redeals = 2, recycle = RecycleOrder.REVERSE, allowFoundationToTableau = true)
        e.startGame(seed = 99uL, rules = rules)
        // consume one recycle
        while (e.getGameState().stock.isNotEmpty()) e.draw()
        e.draw() // triggers recycle and consumes one redeal
        assertEquals(1, e.getRedealsRemaining())

        val saved = e.saveStateString()
        val e2 = GameEngine()
        assertTrue(e2.restoreStateString(saved))
        assertEquals(1, e2.getRedealsRemaining())
        assertEquals(rules, e2.getRules())
    }

    @Test
    fun canMove_from_waste_consistent_with_execution() {
        val base = GameEngine()
        base.startGame(seed = 77uL, rules = Ruleset(draw = 1))
        // ensure some waste
        repeat(5) { base.draw() }
        val saved = base.saveStateString()

        // Check all foundation targets
        for (f in 0..3) {
            val e = GameEngine()
            assertTrue(e.restoreStateString(saved))
            val can = e.canMoveWasteToFoundation(f)
            val moved = e.moveWasteToFoundation(f)
            assertEquals("Foundation target $f consistency", can, moved)
        }
        // Check all tableau targets
        for (t in 0..6) {
            val e = GameEngine()
            assertTrue(e.restoreStateString(saved))
            val can = e.canMoveWasteToTableau(t)
            val moved = e.moveWasteToTableau(t)
            assertEquals("Tableau target $t consistency", can, moved)
        }
    }

    @Test
    fun undo_does_not_restore_consumed_redeals_count() {
        val e = GameEngine()
        e.startGame(seed = 55uL, rules = Ruleset(draw = 1, redeals = 1))
        // Exhaust to trigger recycle and consume the single redeal
        while (e.getGameState().stock.isNotEmpty()) e.draw()
        e.draw()
        assertEquals(0, e.getRedealsRemaining())
        // Undo should revert state snapshot (cards), but redealsRemaining is part of state history snapshot only via SaveCodec.
        // Our UndoManager snapshots gameState, not rules/redeals; hence undo won't reinstate redealsRemaining.
        assertTrue(e.undo())
        assertEquals(0, e.getRedealsRemaining())
    }
}
