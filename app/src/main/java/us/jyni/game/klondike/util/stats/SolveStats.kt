package us.jyni.game.klondike.util.stats

import us.jyni.game.klondike.util.sync.Ruleset

data class SolveStats(
    val dealId: String,
    val seed: ULong,
    val rules: Ruleset,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationMs: Long,
    val moveCount: Int,
    val outcome: String?, // win|resign|timeout|null
    val score: Int = 0, // 게임 점수
    val layoutId: String? = null,
    val clientVersion: String? = null,
    val platform: String? = null,
    val inherentStatus: String? = null, // "solvable" | "unsolvable"
    val winnableStatus: String? = null, // "won" | "dead_end" | "state_cycle" | "in_progress"
    val gameCode: String? = null  // Base64 공유 코드 (12자)
)

object SolveCodec {
    private const val PREFIX = "SV1;"

    fun encode(s: SolveStats): String = buildString {
        append(PREFIX)
        append("dealId=").append(s.dealId).append(';')
        append("seed=").append(s.seed.toString()).append(';')
        append("draw=").append(s.rules.draw).append(';')
        append("redeals=").append(s.rules.redeals).append(';')
        append("recycle=").append(if (s.rules.recycle.name.lowercase()=="reverse") "reverse" else "keep").append(';')
        append("f2t=").append(s.rules.allowFoundationToTableau).append(';')
        append("started=").append(s.startedAt).append(';')
        append("finished=").append(s.finishedAt ?: 0L).append(';')
        append("dur=").append(s.durationMs).append(';')
        append("moves=").append(s.moveCount).append(';')
        append("outcome=").append(s.outcome ?: "").append(';')
        append("score=").append(s.score).append(';')
        append("layoutId=").append(s.layoutId ?: "").append(';')
        append("client=").append(s.clientVersion ?: "").append(';')
        append("plat=").append(s.platform ?: "")
    }

    fun decode(str: String): SolveStats {
        require(str.startsWith(PREFIX)) { "bad solve format" }
        val parts = str.removePrefix(PREFIX).split(';')
        val map = mutableMapOf<String,String>()
        for (p in parts) {
            val i = p.indexOf('=')
            if (i>0) map[p.substring(0,i)] = p.substring(i+1)
        }
        val dealId = map["dealId"] ?: ""
        val seed = map["seed"]!!.toULong()
        val draw = map["draw"]!!.toInt()
        val redeals = map["redeals"]!!.toInt()
        val recycle = if ((map["recycle"] ?: "reverse").lowercase()=="keep") us.jyni.game.klondike.util.sync.RecycleOrder.KEEP else us.jyni.game.klondike.util.sync.RecycleOrder.REVERSE
        val f2t = map["f2t"]!!.toBoolean()
        val started = map["started"]!!.toLong()
        val finished = map["finished"]!!.toLong().let { if (it==0L) null else it }
        val dur = map["dur"]!!.toLong()
        val moves = map["moves"]!!.toInt()
        val outcome = map["outcome"].takeUnless { it.isNullOrEmpty() }
        val score = map["score"]?.toIntOrNull() ?: 0
        val layoutId = map["layoutId"].takeUnless { it.isNullOrEmpty() }
        val client = map["client"].takeUnless { it.isNullOrEmpty() }
        val plat = map["plat"].takeUnless { it.isNullOrEmpty() }
        val rules = Ruleset(draw, redeals, recycle, f2t)
        return SolveStats(dealId, seed, rules, started, finished, dur, moves, outcome, score, layoutId, client, plat)
    }
}
