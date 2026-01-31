package us.jyni.game.klondike.util

import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * Compact, deterministic save/restore codec for GameState + Rules + Timer/Score without external JSON libs.
 * Format (single line):
 * GS2;draw=1;redeals=-1;recycle=reverse;f2t=true;rr=1;go=0;score=45;moves=28;started=123456;paused=0;tab=S:A:u,S:2:d/...;fnd=...;sto=...;was=...
 * Version GS2 includes score, moves, timer data
 */
object SaveCodec {
    private const val PREFIX = "GS2;"
    private const val LEGACY_PREFIX = "GS1;"

    data class SaveData(
        val state: GameState,
        val rules: Ruleset,
        val redealsRemaining: Int,
        val dealId: String?,
        val score: Int = 0,
        val moveCount: Int = 0,
        val startedAt: Long = 0L,
        val totalPausedMs: Long = 0L,
        val seed: ULong = 0xCAFEBABE_uL
    )

    fun encode(
        state: GameState,
        rules: Ruleset,
        redealsRemaining: Int,
        dealId: String? = null,
        score: Int = 0,
        moveCount: Int = 0,
        startedAt: Long = 0L,
        totalPausedMs: Long = 0L,
        seed: ULong = 0xCAFEBABE_uL
    ): String {
        val sb = StringBuilder()
        sb.append(PREFIX)
        if (dealId != null) {
            sb.append("did=").append(dealId).append(';')
        }
        sb.append("seed=").append(seed.toString()).append(';')
        sb.append("draw=").append(rules.draw).append(';')
        sb.append("redeals=").append(rules.redeals).append(';')
        sb.append("recycle=").append(if (rules.recycle == RecycleOrder.REVERSE) "reverse" else "keep").append(';')
        sb.append("f2t=").append(rules.allowFoundationToTableau).append(';')
        sb.append("rr=").append(redealsRemaining).append(';')
        sb.append("go=").append(if (state.isGameOver) 1 else 0).append(';')
        sb.append("score=").append(score).append(';')
        sb.append("moves=").append(moveCount).append(';')
        sb.append("started=").append(startedAt).append(';')
        sb.append("paused=").append(totalPausedMs).append(';')
        sb.append("tab=").append(encodeColumns(state.tableau)).append(';')
        sb.append("fnd=").append(encodeColumns(state.foundation)).append(';')
        sb.append("sto=").append(encodePile(state.stock)).append(';')
        sb.append("was=").append(encodePile(state.waste))
        return sb.toString()
    }

    data class Decoded(val state: GameState, val rules: Ruleset, val redealsRemaining: Int, val dealId: String?)

    fun decode(data: String): SaveData {
        val isLegacy = data.startsWith(LEGACY_PREFIX)
        require(data.startsWith(PREFIX) || isLegacy) { "Unsupported save format" }
        
        val parts = data.removePrefix(if (isLegacy) LEGACY_PREFIX else PREFIX).split(';')
        val map = mutableMapOf<String, String>()
        for (p in parts) {
            val idx = p.indexOf('=')
            if (idx > 0) map[p.substring(0, idx)] = p.substring(idx + 1)
        }
        
        val dealId = map["did"]
        val seed = map["seed"]?.toULongOrNull() ?: 0xCAFEBABE_uL
        val draw = map["draw"]!!.toInt()
        val redeals = map["redeals"]!!.toInt()
        val recycle = when (map["recycle"]) { "keep" -> RecycleOrder.KEEP else -> RecycleOrder.REVERSE }
        val f2t = map["f2t"]!!.toBoolean()
        val rr = map["rr"]!!.toInt()
        val go = map["go"] == "1"
        
        // New fields in GS2 (with defaults for GS1 compatibility)
        val score = map["score"]?.toIntOrNull() ?: 0
        val moveCount = map["moves"]?.toIntOrNull() ?: 0
        val startedAt = map["started"]?.toLongOrNull() ?: System.currentTimeMillis()
        val totalPausedMs = map["paused"]?.toLongOrNull() ?: 0L
        
        val tableau = decodeColumns(map["tab"].orEmpty())
        val foundation = decodeColumns(map["fnd"].orEmpty())
        val stock = decodePile(map["sto"].orEmpty())
        val waste = decodePile(map["was"].orEmpty())
        
        val rules = Ruleset(draw, redeals, recycle, f2t)
        val state = GameState(tableau, foundation, stock, waste, go, score)
        
        return SaveData(state, rules, rr, dealId, score, moveCount, startedAt, totalPausedMs, seed)
    }

    private fun encodeColumns(cols: List<MutableList<Card>>): String =
        cols.joinToString(separator = "/") { encodePile(it) }

    private fun encodePile(pile: List<Card>): String =
        pile.joinToString(separator = ",") { encodeCard(it) }

    private fun decodeColumns(s: String): List<MutableList<Card>> =
        if (s.isBlank()) List(7) { mutableListOf() } else s.split('/').map { decodePile(it) }

    private fun decodePile(s: String): MutableList<Card> =
        if (s.isBlank()) mutableListOf() else s.split(',').map { decodeCard(it) }.toMutableList()

    private fun encodeCard(c: Card): String = suitCode(c.suit) + ":" + rankCode(c.rank) + ":" + if (c.isFaceUp) "u" else "d"

    private fun decodeCard(s: String): Card {
        val parts = s.split(':')
        val suit = when (parts[0]) {
            "S" -> Suit.SPADES
            "H" -> Suit.HEARTS
            "D" -> Suit.DIAMONDS
            "C" -> Suit.CLUBS
            else -> error("bad suit")
        }
        val rank = when (parts[1]) {
            "A" -> Rank.ACE
            "2" -> Rank.TWO
            "3" -> Rank.THREE
            "4" -> Rank.FOUR
            "5" -> Rank.FIVE
            "6" -> Rank.SIX
            "7" -> Rank.SEVEN
            "8" -> Rank.EIGHT
            "9" -> Rank.NINE
            "10" -> Rank.TEN
            "J" -> Rank.JACK
            "Q" -> Rank.QUEEN
            "K" -> Rank.KING
            else -> error("bad rank")
        }
        val faceUp = parts.getOrNull(2) == "u"
        return Card(suit, rank, faceUp)
    }

    private fun suitCode(s: Suit) = when (s) {
        Suit.SPADES -> "S"
        Suit.HEARTS -> "H"
        Suit.DIAMONDS -> "D"
        Suit.CLUBS -> "C"
    }

    private fun rankCode(r: Rank) = when (r) {
        Rank.ACE -> "A"
        Rank.TWO -> "2"
        Rank.THREE -> "3"
        Rank.FOUR -> "4"
        Rank.FIVE -> "5"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
    }
}
