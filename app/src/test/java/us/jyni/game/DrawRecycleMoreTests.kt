package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class DrawRecycleMoreTests {

    private fun exhaustStock(e: GameEngine) {
        while (e.getGameState().stock.isNotEmpty()) {
            e.draw()
            if (e.getGameState().stock.isEmpty()) break
        }
    }

    @Test
    fun redeals_one_allows_single_recycle_then_blocks() {
        val e = GameEngine()
        e.startGame(seed = 9uL, rules = Ruleset(draw = 3, redeals = 1, recycle = RecycleOrder.KEEP))
        assertEquals(1, e.getRedealsRemaining())
        // Exhaust stock
        exhaustStock(e)
        val wasteBefore = e.getGameState().waste.size
        assertTrue(wasteBefore > 0)

        // Trigger recycle + draw once
        val d1 = e.draw()
        assertTrue(d1 > 0)
        assertEquals(0, e.getRedealsRemaining())

        // Exhaust again
        exhaustStock(e)
        val sBefore = e.getGameState().stock.size
        val wBefore = e.getGameState().waste.size
        // No redeals remaining -> further draw shouldn't change
        val d2 = e.draw()
        assertEquals(0, d2)
        assertEquals(sBefore, e.getGameState().stock.size)
        assertEquals(wBefore, e.getGameState().waste.size)
    }

    @Test
    fun redeals_two_allows_two_recycles_then_blocks() {
        val e = GameEngine()
        e.startGame(seed = 10uL, rules = Ruleset(draw = 3, redeals = 2, recycle = RecycleOrder.REVERSE))
        assertEquals(2, e.getRedealsRemaining())

        // First exhaust -> recycle on next draw
        exhaustStock(e)
        val d1 = e.draw()
        assertTrue(d1 > 0)
        assertEquals(1, e.getRedealsRemaining())

        // Second exhaust -> recycle on next draw
        exhaustStock(e)
        val d2 = e.draw()
        assertTrue(d2 > 0)
        assertEquals(0, e.getRedealsRemaining())

        // Third exhaust -> should be blocked
        exhaustStock(e)
        val sb = e.getGameState().stock.size
        val wb = e.getGameState().waste.size
        val d3 = e.draw()
        assertEquals(0, d3)
        assertEquals(sb, e.getGameState().stock.size)
        assertEquals(wb, e.getGameState().waste.size)
    }

    @Test
    fun recycle_order_first_card_after_recycle_matches_expected() {
        // KEEP: after recycle, first draw should be previous waste bottom (waste.last)
        val eKeep = GameEngine()
        eKeep.startGame(seed = 11uL, rules = Ruleset(draw = 1, redeals = 1, recycle = RecycleOrder.KEEP))
        exhaustStock(eKeep)
        val keepExpected = eKeep.getGameState().waste.last()
        eKeep.draw() // triggers recycle + draws 1
        val keepActual = eKeep.getGameState().waste.last()
        assertEquals(keepExpected.rank, keepActual.rank)
        assertEquals(keepExpected.suit, keepActual.suit)

        // REVERSE: after recycle, first draw should be previous waste top (waste.first)
        val eRev = GameEngine()
        eRev.startGame(seed = 11uL, rules = Ruleset(draw = 1, redeals = 1, recycle = RecycleOrder.REVERSE))
        exhaustStock(eRev)
        val revExpected = eRev.getGameState().waste.first()
        eRev.draw()
        val revActual = eRev.getGameState().waste.last()
        assertEquals(revExpected.rank, revActual.rank)
        assertEquals(revExpected.suit, revActual.suit)

        // And ensure KEEP vs REVERSE differ
        assertFalse(keepActual.rank == revActual.rank && keepActual.suit == revActual.suit)
    }
}
