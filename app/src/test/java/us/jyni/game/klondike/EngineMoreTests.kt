package us.jyni.game.klondike

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

class EngineMoreTests {

    @Test
    fun dealId_stable_across_save_restore() {
        val e = GameEngine()
        e.startGame(seed = 42uL, rules = Ruleset(draw = 3))
        val id = e.getDealId()
        val saved = e.saveStateString()
        val e2 = GameEngine()
        assertTrue(e2.restoreStateString(saved))
        assertEquals(id, e2.getDealId())
    }

    @Test
    fun canMove_consistent_with_move_execution() {
        val e = GameEngine()
        e.startGame(seed = 7uL, rules = Ruleset(draw = 1))
        val s = e.getGameState()
        // Try a few pairs and foundations for consistency
        for (from in 0..6) {
            for (to in 0..6) if (from != to) {
                val can = e.canMoveTableauToTableau(from, to)
                if (can) {
                    val moved = e.moveTableauToTableau(from, to)
                    assertTrue(moved)
                    return
                }
            }
            for (f in 0..3) {
                val canF = e.canMoveTableauToFoundation(from, f)
                if (canF) {
                    val moved = e.moveTableauToFoundation(from, f)
                    assertTrue(moved)
                    return
                }
            }
        }
        // If we didn't find any canMove=true, draw and try again
        e.draw()
        var found = false
        loop@ for (from in 0..6) {
            for (to in 0..6) if (from != to) {
                if (e.canMoveTableauToTableau(from, to)) { assertTrue(e.moveTableauToTableau(from, to)); found = true; break@loop }
            }
        }
        if (!found) {
            // As a fallback assert that canMove to foundation implies move success if any
            for (from in 0..6) for (f in 0..3) {
                if (e.canMoveTableauToFoundation(from, f)) { assertTrue(e.moveTableauToFoundation(from, f)); return }
            }
        }
    }
}
