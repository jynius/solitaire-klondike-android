package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class SimpleRulesTest {
    private val rules = KlondikeRules()

    @Test
    fun getMovableSequence_singleCard() {
        // Single face-up card should be movable
        val pile = listOf(
            Card(Suit.SPADES, Rank.ACE, isFaceUp = false),
            Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
        )
        val movable = rules.getMovableSequence(pile)
        assertEquals(1, movable.size)
        assertEquals(Rank.KING, movable[0].rank)
        assertEquals(Suit.HEARTS, movable[0].suit)
    }

    @Test
    fun getMovableSequence_validSequence() {
        // K♠ Q♥ J♠ should all be movable together
        val pile = listOf(
            Card(Suit.CLUBS, Rank.ACE, isFaceUp = false),
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
            Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
        )
        val movable = rules.getMovableSequence(pile)
        assertEquals(3, movable.size)
        assertEquals(Rank.KING, movable[0].rank)
        assertEquals(Rank.QUEEN, movable[1].rank)
        assertEquals(Rank.JACK, movable[2].rank)
    }

    @Test
    fun getMovableSequence_brokenSequence() {
        // K♠ Q♥ 10♠ - broken sequence should only return valid part
        val pile = listOf(
            Card(Suit.CLUBS, Rank.ACE, isFaceUp = false),
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
            Card(Suit.SPADES, Rank.TEN, isFaceUp = true) // Skips JACK
        )
        val movable = rules.getMovableSequence(pile)
        assertEquals(1, movable.size) // Only the last card (10♠)
        assertEquals(Rank.TEN, movable[0].rank)
    }

    @Test
    fun canMoveSequenceToTableau_kingToEmpty() {
        // King sequence should be movable to empty tableau
        val kingSequence = listOf(
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
        )
        assertTrue(rules.canMoveSequenceToTableau(kingSequence, emptyList()))
        
        // Queen sequence should NOT be movable to empty tableau
        val queenSequence = listOf(
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
            Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
        )
        assertFalse(rules.canMoveSequenceToTableau(queenSequence, emptyList()))
    }

    @Test
    fun canMoveSequenceToTableau_validMove() {
        // Q♠ J♥ sequence should be movable onto K♦
        val sequence = listOf(
            Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true),
            Card(Suit.HEARTS, Rank.JACK, isFaceUp = true)
        )
        val target = listOf(
            Card(Suit.DIAMONDS, Rank.KING, isFaceUp = true)
        )
        assertTrue(rules.canMoveSequenceToTableau(sequence, target))
    }

    @Test
    fun canMoveTableauToTableau_kingToEmpty() {
        // Single King should be able to move to empty tableau
        val kingPile = listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        assertTrue(rules.canMoveTableauToTableau(kingPile, emptyList()))
        
        // Queen should NOT be able to move to empty tableau
        val queenPile = listOf(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true))
        assertFalse(rules.canMoveTableauToTableau(queenPile, emptyList()))
    }

    @Test
    fun canMoveTableauToTableau_basicRules() {
        // Q♥ should be movable onto K♠
        val source = listOf(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true))
        val target = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
        assertTrue(rules.canMoveTableauToTableau(source, target))
        
        // Q♥ should NOT be movable onto K♥ (same color)
        val sameColorTarget = listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        assertFalse(rules.canMoveTableauToTableau(source, sameColorTarget))
        
        // Q♥ should NOT be movable onto Q♠ (same rank)
        val sameRankTarget = listOf(Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true))
        assertFalse(rules.canMoveTableauToTableau(source, sameRankTarget))
    }
}