package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

class UndoFlipRulesTest {

    @Test
    fun flip_on_expose_top_of_tableau() {
        val e = GameEngine()
        e.startGame(seed = 10uL, rules = Ruleset(draw = 1, redeals = 1))

        var success = false
        repeat(15) {
            val s = e.getGameState()
            // try all columns to find a move that exposes a facedown card beneath
            var moved = false
            var movedFrom = -1
            outer@ for (from in 0..6) {
                val pile = s.tableau[from]
                if (pile.size >= 2 && !pile[pile.lastIndex - 1].isFaceUp) {
                    for (to in 0..6) if (to != from) {
                        if (e.moveTableauToTableau(from, to)) { moved = true; movedFrom = from; break@outer }
                    }
                    if (!moved) {
                        for (f in 0..3) {
                            if (e.moveTableauToFoundation(from, f)) { moved = true; movedFrom = from; break@outer }
                        }
                    }
                }
            }
            if (moved) {
                val src2 = e.getGameState().tableau[movedFrom]
                if (src2.isNotEmpty()) {
                    assertTrue(src2.last().isFaceUp)
                }
                success = true
                return@repeat
            }
            // no exposing move found this iteration; draw to change surface cards
            e.draw()
        }
        assertTrue("Could not find exposing move within draw budget", success)
    }

    @Test
    fun undo_redo_restores_state_for_move_and_draw() {
        val e = GameEngine()
        e.startGame(seed = 11uL, rules = Ruleset(draw = 3, redeals = 1))
        val s0 = e.getGameState()
        val stock0 = s0.stock.size
        val waste0 = s0.waste.size

        // draw
        val n = e.draw()
        assertTrue(n > 0)
        val s1 = e.getGameState()
        assertEquals(stock0 - n, s1.stock.size)
        assertEquals(waste0 + n, s1.waste.size)

        // attempt a move (may or may not succeed based on layout); we allow no-op
        run {
            var moved = false
            outer@ for (from in 0..6) {
                for (to in 0..6) if (from != to) {
                    if (e.moveTableauToTableau(from, to)) { moved = true; break@outer }
                }
            }
        }
        val s2 = e.getGameState()

        // undo should revert to state before last action (move or draw)
        assertTrue(e.undo())
        val sUndo = e.getGameState()
        // We can't assert exact sizes if move happened; but undo then redo should be identity.
        val layoutIdUndo = layoutFingerprint(sUndo)
        assertTrue(e.redo())
        val sRedo = e.getGameState()
        val layoutIdRedo = layoutFingerprint(sRedo)
        assertEquals(layoutIdRedo, layoutFingerprint(s2))
        // redo brings back to s2 layout
        assertEquals(layoutIdRedo, layoutFingerprint(s2))
    }

    private fun layoutFingerprint(s: us.jyni.game.klondike.model.GameState): String {
        // cheap fingerprint: sizes + face flags distribution
        val t = s.tableau.joinToString("|") { col -> col.size.toString() + ":" + col.count { it.isFaceUp } }
        val f = s.foundation.joinToString(",") { it.size.toString() }
        return listOf(t, f, s.stock.size, s.waste.size, s.isGameOver).joinToString("#")
    }
}
