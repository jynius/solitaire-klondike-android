package com.example.klondike

import us.jyni.game.klondike.util.sync.LayoutId
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutIdTest {
    @Test
    fun sameLayoutSameId() {
        val t1 = listOf(
            listOf("S:A:u"),
            listOf("H:2:d","H:3:u"),
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
        val f1 = listOf(emptyList<String>(), emptyList(), emptyList(), emptyList())
        val s1 = listOf("D:K:d","C:Q:d")
        val w1 = emptyList<String>()

        val t2 = listOf(
            listOf("S:A:u"),
            listOf("H:2:d","H:3:u"),
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
        val f2 = listOf(emptyList<String>(), emptyList(), emptyList(), emptyList())
        val s2 = listOf("D:K:d","C:Q:d")
        val w2 = emptyList<String>()

        val id1 = LayoutId.generate(t1, f1, s1, w1)
        val id2 = LayoutId.generate(t2, f2, s2, w2)

        assertEquals(id1, id2)
    }
}
