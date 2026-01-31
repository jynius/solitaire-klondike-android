package us.jyni.game.klondike.model

data class GameState(
    var tableau: List<MutableList<Card>> = List(7) { mutableListOf() },
    var foundation: List<MutableList<Card>> = List(4) { mutableListOf() },
    var stock: MutableList<Card> = mutableListOf(),
    var waste: MutableList<Card> = mutableListOf(),
    var isGameOver: Boolean = false,
    var score: Int = 0
) {
    fun reset() {
        tableau = List(7) { mutableListOf() }
        foundation = List(4) { mutableListOf() }
        stock = mutableListOf()
        waste = mutableListOf()
        isGameOver = false
        score = 0
    }
}