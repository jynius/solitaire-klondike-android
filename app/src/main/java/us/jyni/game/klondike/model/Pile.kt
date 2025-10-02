package us.jyni.game.klondike.model

class Pile {
    private val cards: MutableList<Card> = mutableListOf()

    fun addCard(card: Card) {
        cards.add(card)
    }

    fun removeCard(): Card? {
        return if (cards.isNotEmpty()) {
            cards.removeAt(cards.size - 1)
        } else {
            null
        }
    }

    fun peekCard(): Card? {
        return if (cards.isNotEmpty()) {
            cards[cards.size - 1]
        } else {
            null
        }
    }

    fun getCards(): List<Card> {
        return cards.toList()
    }

    fun isEmpty(): Boolean {
        return cards.isEmpty()
    }
}