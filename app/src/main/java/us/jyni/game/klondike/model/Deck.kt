package us.jyni.game.klondike.model

class Deck {
    private val cards: MutableList<Card> = mutableListOf()

    init {
        createDeck()
    }

    private fun createDeck() {
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(suit = suit, rank = rank))
            }
        }
    }

    fun asMutableList(): MutableList<Card> = cards

    fun drawCard(): Card? = if (cards.isNotEmpty()) cards.removeAt(cards.lastIndex) else null

    fun remaining(): Int = cards.size
}