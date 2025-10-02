package us.jyni.game.klondike.util.sync

import java.security.MessageDigest

/**
 * 초기 레이아웃(테이블/파운데이션/스톡/웨이스트)을 계약된 직렬화 규칙으로 고정 문자열로 만든 뒤,
 * SHA-256 → Base58 로 인코딩해 Layout ID(L1_ 접두사)를 생성합니다.
 * 이 ID는 셔플/시드/규칙의 입력과 무관하게, 동일한 레이아웃이면 동일하게 생성되어 중복 제거에 사용할 수 있습니다.
 */
object LayoutId {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /** Encoded card format: "S:A:u" (suit:rank:face), face = 'u' or 'd' */
    fun canonicalJson(
        lv: Int = 0,
        tableau: List<List<String>>,
        foundation: List<List<String>>,
        stock: List<String>,
        waste: List<String>
    ): String {
        fun arr(list: List<String>) = list.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        fun arr2(list2: List<List<String>>) = list2.joinToString(prefix = "[", postfix = "]") { arr(it) }
        // 공백 없음, 필드 순서 고정
        return "{" +
            "\"lv\":" + lv + "," +
            "\"tableau\":" + arr2(tableau) + "," +
            "\"foundation\":" + arr2(foundation) + "," +
            "\"stock\":" + arr(stock) + "," +
            "\"waste\":" + arr(waste) +
            "}"
    }

    private fun sha256(data: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))

    private fun base58(input: ByteArray): String {
        var bi = java.math.BigInteger(1, input)
        val sb = StringBuilder()
        while (bi > java.math.BigInteger.ZERO) {
            val divRem = bi.divideAndRemainder(java.math.BigInteger.valueOf(58))
            bi = divRem[0]
            val idx = divRem[1].toInt()
            sb.append(ALPHABET[idx])
        }
        for (b in input) { if (b.toInt() == 0) sb.append(ALPHABET[0]) else break }
        return sb.reverse().toString()
    }

    fun generate(
        tableau: List<List<String>>,
        foundation: List<List<String>>,
        stock: List<String>,
        waste: List<String>,
        lv: Int = 0
    ): String {
        val json = canonicalJson(lv, tableau, foundation, stock, waste)
        return "L1_" + base58(sha256(json))
    }

    // 헬퍼: 카드 코드 "S:A" + faceUp -> "S:A:u" or "S:A:d"
    fun encodeCard(suitRank: String, faceUp: Boolean): String = "$suitRank:" + if (faceUp) "u" else "d"
}
