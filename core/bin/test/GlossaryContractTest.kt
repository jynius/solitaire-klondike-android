import org.junit.Assert.assertEquals
import org.junit.Test

class GlossaryContractTest {
    enum class Suit { S, H, D, C }
    enum class Rank { A, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, J, Q, K }

    @Test
    fun suitAndRankCounts() {
        assertEquals(4, Suit.values().size)
        assertEquals(13, Rank.values().size)
    }
}
