package us.jyni.game.klondike.util.sync

import kotlin.math.abs

/**
 * 간단한 64-bit Xorshift* PRNG (플랫폼 독립 시퀀스 보장 목적)
 */
class XorShift64Star(seed: ULong) {
    private var state: ULong = if (seed == 0uL) 0x9E3779B97F4A7C15uL else seed

    fun nextULong(): ULong {
        var x = state
        x = x xor (x shr 12)
        x = x xor (x shl 25)
        x = x xor (x shr 27)
        state = x
        return x * 0x2545F4914F6CDD1DuL
    }

    fun nextInt(bound: Int): Int {
        val b = if (bound <= 0) 1 else bound
        val v = (nextULong() and ULong.MAX_VALUE).toLong()
        val r = (v ushr 1) % b
        return r.toInt()
    }
}

/** Fisher–Yates 셔플 (in-place) */
fun <T> fisherYatesShuffle(list: MutableList<T>, seed: ULong) {
    val rng = XorShift64Star(seed)
    for (i in list.lastIndex downTo 1) {
        val j = rng.nextInt(i + 1)
        if (i != j) {
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}
