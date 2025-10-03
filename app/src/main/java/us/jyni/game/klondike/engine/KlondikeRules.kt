package us.jyni.game.klondike.engine

import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Deck
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class KlondikeRules {

    // --- Helpers ---
    private fun isRed(suit: Suit) = suit == Suit.HEARTS || suit == Suit.DIAMONDS
    private fun isBlack(suit: Suit) = suit == Suit.SPADES || suit == Suit.CLUBS
    fun oppositeColor(a: Card, b: Card): Boolean =
        (isRed(a.suit) && isBlack(b.suit)) || (isBlack(a.suit) && isRed(b.suit))

    fun oneRankLower(a: Card, b: Card): Boolean =
        a.rank.value + 1 == b.rank.value

    // --- Tableau to Tableau ---
    fun canMoveTableauToTableau(source: List<Card>, target: List<Card>): Boolean {
        if (source.isEmpty()) return false
        val moving = source.last()
        if (!moving.isFaceUp) return false

        if (target.isEmpty()) {
            // Only King can be moved to an empty tableau
            return moving.rank == Rank.KING
        }

        val top = target.last()
        // opposite color and descending rank
        return oppositeColor(moving, top) && oneRankLower(moving, top)
    }

    // --- Tableau to Foundation ---
    fun canMoveTableauToFoundation(source: List<Card>, foundationPile: List<Card>): Boolean {
        if (source.isEmpty()) return false
        val moving = source.last()
        if (!moving.isFaceUp) return false

        if (foundationPile.isEmpty()) {
            return moving.rank == Rank.ACE
        }

        val top = foundationPile.last()
        return moving.suit == top.suit && moving.rank.value == top.rank.value + 1
    }

    // --- Foundation to Tableau ---
    fun canMoveFoundationToTableau(foundationPile: List<Card>, target: List<Card>): Boolean {
        if (foundationPile.isEmpty()) return false
        val moving = foundationPile.last()
        if (target.isEmpty()) {
            return moving.rank == Rank.KING
        }
        val top = target.last()
        return oppositeColor(moving, top) && oneRankLower(moving, top)
    }

    // Find consecutive face-up cards that can be moved together
    fun getMovableSequence(source: List<Card>): List<Card> {
        if (source.isEmpty()) return emptyList()
        
        val movable = mutableListOf<Card>()
        var i = source.size - 1
        
        // Start from the last card and work backwards
        while (i >= 0) {
            val card = source[i]
            if (!card.isFaceUp) break
            
            movable.add(0, card) // Add to front to maintain order
            
            // Check if we can continue the sequence
            if (i > 0) {
                val nextCard = source[i - 1]
                if (!nextCard.isFaceUp) break
                
                // Must be alternating colors and descending ranks
                if (!oppositeColor(nextCard, card) || nextCard.rank.value != card.rank.value + 1) {
                    break
                }
            }
            i--
        }
        
        return movable
    }
    
    // Check if a sequence can be moved to target
    fun canMoveSequenceToTableau(sequence: List<Card>, target: List<Card>): Boolean {
        if (sequence.isEmpty()) return false
        val moving = sequence.first() // The top card of the sequence
        
        if (target.isEmpty()) {
            return moving.rank == Rank.KING
        }
        
        val top = target.last()
        return oppositeColor(moving, top) && oneRankLower(moving, top)
    }

    // For engine hooks (legacy names)
    fun isMoveValid(sourcePile: List<Card>, targetPile: List<Card>): Boolean =
        canMoveTableauToTableau(sourcePile, targetPile)

    fun canDrawCard(deck: Deck): Boolean = deck.remaining() > 0

    fun isGameWon(gameState: GameState): Boolean {
        // Game is won if all foundation piles have 13 cards
        return gameState.foundation.all { it.size == 13 }
    }
}