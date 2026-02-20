package us.jyni.game.klondike.model

import us.jyni.game.klondike.util.sync.Ruleset

data class GameState(
    var tableau: List<MutableList<Card>> = List(7) { mutableListOf() },
    var foundation: List<MutableList<Card>> = List(4) { mutableListOf() },
    var stock: MutableList<Card> = mutableListOf(),
    var waste: MutableList<Card> = mutableListOf(),
    var isGameOver: Boolean = false,
    var score: Int = 0,
    var redealsRemaining: Int = -1,  // -1 = unlimited, GameEngine과 동기화 필요
    var rules: Ruleset = Ruleset()   // 현재 게임의 규칙
) {
    fun reset() {
        tableau = List(7) { mutableListOf() }
        foundation = List(4) { mutableListOf() }
        stock = mutableListOf()
        waste = mutableListOf()
        isGameOver = false
        score = 0
        redealsRemaining = -1
        rules = Ruleset()
    }
}