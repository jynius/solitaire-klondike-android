package us.jyni.game.klondike.util.sync

import java.security.MessageDigest

object DealId {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" // Base58

    data class Context(
        val dealContractVersion: Int = 1,
        val shuffleVersion: Int = 1
    )

    fun canonicalDeckString(deck: List<String>): String = deck.joinToString(",")

    fun canonicalJson(
        v: Int,
        sv: Int,
        rules: Ruleset,
        seed: ULong,
        deck: List<String>
    ): String {
        val recycle = when (rules.recycle) {
            RecycleOrder.KEEP -> "keep"
            RecycleOrder.REVERSE -> "reverse"
        }
        // 필드 순서/공백 고정
        return "{" +
            "\"v\":" + v + "," +
            "\"sv\":" + sv + "," +
            "\"rules\":{\"draw\":" + rules.draw + ",\"redeals\":" + rules.redeals + ",\"recycle\":\"" + recycle + "\"}," +
            "\"seed\":\"" + seed.toString() + "\"," +
            "\"deck\":\"" + canonicalDeckString(deck) + "\"}"
    }

    fun sha256Hex(data: String): ByteArray {
        val dg = MessageDigest.getInstance("SHA-256")
        return dg.digest(data.toByteArray(Charsets.UTF_8))
    }

    fun base58(input: ByteArray): String {
        var bi = java.math.BigInteger(1, input)
        val sb = StringBuilder()
        while (bi > java.math.BigInteger.ZERO) {
            val divRem = bi.divideAndRemainder(java.math.BigInteger.valueOf(58))
            bi = divRem[0]
            val idx = divRem[1].toInt()
            sb.append(ALPHABET[idx])
        }
        // leading zeros
        for (b in input) {
            if (b.toInt() == 0) sb.append(ALPHABET[0]) else break
        }
        return sb.reverse().toString()
    }

    fun generate(
        deckCanonical: List<String>,
        rules: Ruleset,
        seed: ULong,
        ctx: Context = Context()
    ): String {
        val json = canonicalJson(ctx.dealContractVersion, ctx.shuffleVersion, rules, seed, deckCanonical)
        val hash = sha256Hex(json)
        return "DL1_" + base58(hash)
    }
}
