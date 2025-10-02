package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class KlondikeRulesTest {
    private val rules = KlondikeRules()

    @Test
    fun tableauToTableau_basicValidAndInvalid() {
        // Move red 6 onto black 7 -> valid
        val source1 = listOf(Card(Suit.HEARTS, Rank.SIX, isFaceUp = true))
        val target1 = listOf(Card(Suit.SPADES, Rank.SEVEN, isFaceUp = true))
        assertTrue(rules.canMoveTableauToTableau(source1, target1))

        // Same color -> invalid
        val target2 = listOf(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
        assertFalse(rules.canMoveTableauToTableau(source1, target2))

        // Not descending -> invalid
        val target3 = listOf(Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true))
        assertFalse(rules.canMoveTableauToTableau(source1, target3))

        // Move King to empty column -> valid; Queen -> invalid
        val k = listOf(Card(Suit.CLUBS, Rank.KING, isFaceUp = true))
        val q = listOf(Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true))
        assertTrue(rules.canMoveTableauToTableau(k, emptyList()))
        assertFalse(rules.canMoveTableauToTableau(q, emptyList()))

        // Face-down cannot move
        val fd = listOf(Card(Suit.HEARTS, Rank.SIX, isFaceUp = false))
        assertFalse(rules.canMoveTableauToTableau(fd, target1))
    }

    @Test
    fun tableauToFoundation_basicValidAndInvalid() {
        // Empty foundation: only Ace can go
        val aH = listOf(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        val twoH = listOf(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        assertTrue(rules.canMoveTableauToFoundation(aH, emptyList()))
        assertFalse(rules.canMoveTableauToFoundation(twoH, emptyList()))

        // Same suit, ascending
        val f = listOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        val moving2 = listOf(Card(Suit.SPADES, Rank.TWO, isFaceUp = true))
        assertTrue(rules.canMoveTableauToFoundation(moving2, f))

        // Different suit -> invalid
        val moving2H = listOf(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        assertFalse(rules.canMoveTableauToFoundation(moving2H, f))

        // Not face-up -> invalid
        val hidden = listOf(Card(Suit.SPADES, Rank.THREE, isFaceUp = false))
        assertFalse(rules.canMoveTableauToFoundation(hidden, f))
    }
}
