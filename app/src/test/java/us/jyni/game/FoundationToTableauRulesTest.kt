package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.Ruleset

class FoundationToTableauRulesTest {
    private val rules = KlondikeRules()

    @Test
    fun pure_rules_validation() {
        // Move black 6 from foundation onto red 7 in tableau: valid
        val fnd = listOf(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        val tab = listOf(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        assertTrue(rules.canMoveFoundationToTableau(fnd, tab))

        // Same color -> invalid
        val tabSameColor = listOf(Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true))
        assertFalse(rules.canMoveFoundationToTableau(fnd, tabSameColor))

        // Not descending -> invalid
        val tabHigher = listOf(Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true))
        assertFalse(rules.canMoveFoundationToTableau(fnd, tabHigher))

        // Empty tableau: only King allowed
        val fKing = listOf(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        assertTrue(rules.canMoveFoundationToTableau(fKing, emptyList()))
        assertFalse(rules.canMoveFoundationToTableau(fnd, emptyList()))
    }

    @Test
    fun engine_disables_when_option_false() {
        val e = GameEngine()
        e.startGame(seed = 7uL, rules = Ruleset(allowFoundationToTableau = false))
        // No matter indices, engine should refuse when option is false
        assertFalse(e.moveFoundationToTableau(0, 0))
    }
}
