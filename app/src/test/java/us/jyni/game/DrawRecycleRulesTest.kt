package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class DrawRecycleRulesTest {

    @Test
    fun draw1_and_draw3_counts_and_faces() {
        val e1 = GameEngine()
        e1.startGame(seed = 1uL, rules = Ruleset(draw = 1))
        val s1 = e1.getGameState()
        val beforeStock = s1.stock.size
        val moved1 = e1.draw()
        assertEquals(1, moved1)
        val s1After = e1.getGameState()
        assertEquals(beforeStock - 1, s1After.stock.size)
        assertEquals(1, s1After.waste.size)
        assertTrue(s1After.waste.last().isFaceUp)

        val e3 = GameEngine()
        e3.startGame(seed = 2uL, rules = Ruleset(draw = 3))
        val s3 = e3.getGameState()
        val beforeStock3 = s3.stock.size
        val moved3 = e3.draw()
        assertEquals(3, moved3)
        val s3After = e3.getGameState()
        assertEquals(beforeStock3 - 3, s3After.stock.size)
        assertEquals(3, s3After.waste.size)
        assertTrue(s3After.waste.all { it.isFaceUp })
    }

    @Test
    fun recycle_keep_preserves_waste_top_as_next_stock_top() {
        val rules = Ruleset(draw = 3, redeals = 1, recycle = RecycleOrder.KEEP)
        val e = GameEngine()
        e.startGame(seed = 3uL, rules = rules)
    // Exhaust stock without triggering recycle
    while (e.getGameState().stock.isNotEmpty()) { e.draw() }
    val s = e.getGameState()
    assertTrue(s.stock.isEmpty())
    assertTrue(s.waste.isNotEmpty())
    val wasteTop = s.waste.last()
    // Next draw triggers recycle and then draws
    val moved = e.draw()
    assertTrue(moved > 0)
    // After recycle(KEEP), waste was cleared; the first newly drawn card equals previous waste top
    val s2 = e.getGameState()
    assertTrue(s2.waste.isNotEmpty())
    val firstNew = s2.waste.first()
    assertEquals(wasteTop, firstNew)
    assertTrue(firstNew.isFaceUp)
    }

    @Test
    fun recycle_reverse_uses_bottom_of_waste_as_next_stock_top() {
        val rules = Ruleset(draw = 3, redeals = 1, recycle = RecycleOrder.REVERSE)
        val e = GameEngine()
        e.startGame(seed = 4uL, rules = rules)
    while (e.getGameState().stock.isNotEmpty()) { e.draw() }
    val s = e.getGameState()
    assertTrue(s.stock.isEmpty())
    assertTrue(s.waste.isNotEmpty())
    val wasteBottom = s.waste.first()
    val moved = e.draw()
    assertTrue(moved > 0)
    // After recycle(REVERSE), waste was cleared; the first newly drawn card equals previous waste bottom
    val s2 = e.getGameState()
    assertTrue(s2.waste.isNotEmpty())
    val firstNew = s2.waste.first()
    assertEquals(wasteBottom, firstNew)
    assertTrue(firstNew.isFaceUp)
    }
}
