package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.stats.SolveCodec
import us.jyni.game.klondike.util.sync.Ruleset

class SolveStatsTest {
    @Test
    fun move_and_draw_increment_and_dealid_stable() {
        val rules = Ruleset(draw = 3, redeals = 1)
        val e1 = GameEngine()
        val seed = 999uL
        e1.startGame(seed = seed, rules = rules)

        // perform some actions
        val d1 = e1.draw(); assertTrue(d1 > 0)
        // try one tableau move; it's okay if none available
        run {
            for (from in 0..6) for (to in 0..6) if (from!=to) {
                if (e1.moveTableauToTableau(from,to)) return@run
            }
        }
        val stats1 = e1.getSolveStatsSnapshot()
        val encoded1 = SolveCodec.encode(stats1)

        val e2 = GameEngine()
        e2.startGame(seed = seed, rules = rules)
        val stats2 = e2.getSolveStatsSnapshot()
        val encoded2 = SolveCodec.encode(stats2)

        // Same seed+rules should produce same dealId, hence same prefix in encoded (dealId field equal)
        assertTrue(encoded1.contains("dealId="))
        val part1 = encoded1.substringAfter("dealId=").substringBefore(';')
        val part2 = encoded2.substringAfter("dealId=").substringBefore(';')
        assertEquals(part2, part1)

        // moves increased at least by 1 due to draw
        assertTrue(stats1.moveCount >= 1)
    }
}
