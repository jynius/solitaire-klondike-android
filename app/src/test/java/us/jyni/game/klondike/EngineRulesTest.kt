package us.jyni.game.klondike

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class EngineRulesTest {

    @Test
    fun rules_draw_affects_waste() {
        val e = GameEngine()
        e.startGame(seed = 1uL, rules = Ruleset(draw = 1))
        e.draw()
        val w1 = e.getGameState().waste.size
        e.startGame(seed = 1uL, rules = Ruleset(draw = 3))
        e.draw()
        val w3 = e.getGameState().waste.size
        assertTrue(w3 >= 3)
        assertTrue(w1 >= 1)
        assertTrue(w3 >= w1)
    }

    @Test
    fun redeals_enforced() {
        val e = GameEngine()
        e.startGame(seed = 2uL, rules = Ruleset(draw = 3, redeals = 0))
        // exhaust stock
        repeat(30) { e.draw() }
        val before = e.getGameState()
        e.draw() // further draws shouldn't change if stock exhausted and no redeals
        val after = e.getGameState()
        assertEquals(before.stock.size, after.stock.size)
        assertEquals(before.waste.size, after.waste.size)
    }

    @Test
    fun recycle_order_keep_vs_reverse() {
        val e1 = GameEngine()
        e1.startGame(seed = 3uL, rules = Ruleset(draw = 1, redeals = 1, recycle = RecycleOrder.KEEP))
        repeat(60) { e1.draw() }
        val seqKeep = e1.getGameState().waste.take(3).map { it.rank to it.suit }

        val e2 = GameEngine()
        e2.startGame(seed = 3uL, rules = Ruleset(draw = 1, redeals = 1, recycle = RecycleOrder.REVERSE))
        repeat(60) { e2.draw() }
        val seqRev = e2.getGameState().waste.take(3).map { it.rank to it.suit }

        assertNotEquals(seqKeep, seqRev)
    }

    @Test
    fun allowFoundationToTableau_false_blocks_move() {
        val e = GameEngine()
        e.startGame(seed = 4uL, rules = Ruleset(allowFoundationToTableau = false))
        // Try an impossible F->T move (no cards yet in foundation). Should be false.
        val moved = e.moveFoundationToTableau(0, 0)
        assertFalse(moved)
    }

    @Test
    fun save_restore_roundtrip() {
        val e = GameEngine()
        e.startGame(seed = 5uL, rules = Ruleset(draw = 1))
        e.draw(); e.draw()
        val saved = e.saveStateString()

        val e2 = GameEngine()
        val ok = e2.restoreStateString(saved)
        assertTrue(ok)
        val s1 = e.getGameState()
        val s2 = e2.getGameState()
        assertEquals(s1.stock.size, s2.stock.size)
        assertEquals(s1.waste.size, s2.waste.size)
        assertEquals(s1.tableau.map { it.size }, s2.tableau.map { it.size })
    }
}
