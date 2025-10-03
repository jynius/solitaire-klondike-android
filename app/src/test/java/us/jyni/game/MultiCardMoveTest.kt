package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class MultiCardMoveTest {
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
    fun getMovableSequence_sameColorBreaks() {
        // K♠ Q♠ - same color should break the sequence
        val pile = listOf(
            Card(Suit.CLUBS, Rank.ACE, isFaceUp = false),
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true)
        )
        val movable = rules.getMovableSequence(pile)
        assertEquals(1, movable.size) // Only the last card (Q♠)
        assertEquals(Rank.QUEEN, movable[0].rank)
    }

    @Test
    fun getMovableSequence_faceDownBreaks() {
        // Face-down card should break the sequence
        val pile = listOf(
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = false), // Face down
            Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
        )
        val movable = rules.getMovableSequence(pile)
        assertEquals(1, movable.size) // Only the last card (J♠)
        assertEquals(Rank.JACK, movable[0].rank)
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
    fun canMoveSequenceToTableau_invalidColor() {
        // Q♠ J♥ sequence should NOT be movable onto K♠ (same color as Q)
        val sequence = listOf(
            Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true),
            Card(Suit.HEARTS, Rank.JACK, isFaceUp = true)
        )
        val target = listOf(
            Card(Suit.SPADES, Rank.KING, isFaceUp = true)
        )
        assertFalse(rules.canMoveSequenceToTableau(sequence, target))
    }

    @Test
    fun canMoveSequenceToTableau_invalidRank() {
        // Q♠ J♥ sequence should NOT be movable onto J♦ (wrong rank)
        val sequence = listOf(
            Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true),
            Card(Suit.HEARTS, Rank.JACK, isFaceUp = true)
        )
        val target = listOf(
            Card(Suit.DIAMONDS, Rank.JACK, isFaceUp = true)
        )
        assertFalse(rules.canMoveSequenceToTableau(sequence, target))
    }

    // GameEngine tests are disabled due to Android Log mocking issues
    // @Test
    // fun gameEngine_multiCardMove_kingToEmpty() {
    //     // This test requires Android mocking setup
    // }

    /*
    // GameEngine tests disabled due to Android Log mocking requirements
    // These tests verify the complete game engine integration but require
    // additional setup to mock Android dependencies
    
    @Test
    fun gameEngine_multiCardMove_sequenceOntoCard() {
        // Test moving Q♥ J♠ sequence onto K♦
    }

    @Test
    fun gameEngine_multiCardMove_partialSequence() {
        // Test moving partial sequence with face-down cards remaining
    }

    @Test
    fun gameEngine_multiCardMove_autoFlip() {
        // Test that face-down cards are flipped after sequence moves
    }

    @Test
    fun gameEngine_singleCardMove_stillWorks() {
        // Test that single card moves still work with new logic
    }
    */
}