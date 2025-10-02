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
    private fun oppositeColor(a: Card, b: Card): Boolean =
        (isRed(a.suit) && isBlack(b.suit)) || (isBlack(a.suit) && isRed(b.suit))

    private fun oneRankLower(a: Card, b: Card): Boolean =
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

    // For engine hooks (legacy names)
    fun isMoveValid(sourcePile: List<Card>, targetPile: List<Card>): Boolean =
        canMoveTableauToTableau(sourcePile, targetPile)

    fun canDrawCard(deck: Deck): Boolean = deck.remaining() > 0

    fun isGameWon(gameState: GameState): Boolean {
        // Game is won if all foundation piles have 13 cards
        return gameState.foundation.all { it.size == 13 }
    }
}