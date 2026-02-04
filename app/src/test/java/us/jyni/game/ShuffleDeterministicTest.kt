package us.jyni.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jyni.game.klondike.util.sync.fisherYatesShuffle

/**
 * Fisher-Yates shuffle 결정성 테스트
 */
class ShuffleDeterministicTest {

    @Test
    fun fisherYatesShuffle_sameSeed_sameResult() {
        val seed = 12345uL
        
        val list1 = (1..10).toMutableList()
        val list2 = (1..10).toMutableList()
        
        fisherYatesShuffle(list1, seed)
        fisherYatesShuffle(list2, seed)
        
        assertEquals("Same seed should produce same shuffle", list1, list2)
    }

    @Test
    fun fisherYatesShuffle_differentSeed_differentResult() {
        val seed1 = 111uL
        val seed2 = 222uL
        
        val list1 = (1..20).toMutableList()
        val list2 = (1..20).toMutableList()
        
        fisherYatesShuffle(list1, seed1)
        fisherYatesShuffle(list2, seed2)
        
        // 매우 높은 확률로 다를 것
        assertTrue("Different seeds should produce different shuffles (probabilistic)",
            list1 != list2)
    }
}
