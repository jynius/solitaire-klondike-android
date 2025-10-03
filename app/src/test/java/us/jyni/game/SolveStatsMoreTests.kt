package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

class SolveStatsMoreTests {
    @Test
    fun solveStats_contains_dealId_and_layoutId_changes_with_state() {
        val e = GameEngine()
        e.startGame(seed = 123uL, rules = Ruleset(draw = 1))
        val s1 = e.getSolveStatsSnapshot()
        assertTrue(s1.dealId.startsWith("DL1_"))
        val layout1 = s1.layoutId

        // Change state (draw or simple move if possible)
        e.draw()
        val s2 = e.getSolveStatsSnapshot()
        assertEquals(s1.dealId, s2.dealId)
        assertNotEquals(layout1, s2.layoutId)
    }

    @Test
    fun solveStats_outcome_override_applies() {
        val e = GameEngine()
        e.startGame(seed = 1uL)
        val s1 = e.getSolveStatsSnapshot(outcomeOverride = "abandoned")
        assertEquals("abandoned", s1.outcome)
    }
}
