package us.jyni.game.klondike.util

import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset
import java.util.Base64

/**
 * 게임 공유 코드 인코더/디코더
 * 형식: [Rules 1자][Seed 11자] = 총 12자 (Base64 URL-safe)
 * 
 * 예시: zYWFhYWFhYWF
 * - z: 규칙 (D3, Redeals=무제한, Recycle=REVERSE, F→T=true)
 * - YWFhYWFhYWF: Seed (Base64 인코딩)
 */
object GameCode {
    // Base64 URL-safe 알파벳
    private const val BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    
    /**
     * 게임 코드 생성
     */
    fun encode(seed: ULong, rules: Ruleset): String {
        val rulesChar = encodeRules(rules)
        val seedCode = encodeBase64(seed)
        return "$rulesChar$seedCode"
    }
    
    /**
     * 게임 코드 파싱
     * @return Pair(seed, rules) or null if invalid
     */
    fun decode(code: String): Pair<ULong, Ruleset>? {
        if (code.length < 2) return null
        
        return try {
            val rulesChar = code[0]
            val seedCode = code.substring(1)
            
            val rules = decodeRules(rulesChar) ?: return null
            val seed = decodeBase64(seedCode) ?: return null
            
            Pair(seed, rules)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 규칙을 1자로 인코딩
     * 비트 구조:
     * - bit 0: draw (0=D1, 1=D3)
     * - bit 1-2: redeals (0=무제한, 1=0회, 2=1회, 3=2회)
     * - bit 3: recycle (0=KEEP, 1=REVERSE)
     * - bit 4: allowFoundationToTableau (0=false, 1=true)
     */
    private fun encodeRules(r: Ruleset): Char {
        var code = 0
        
        // bit 0: draw
        code = code or (if (r.draw == 3) 1 else 0)
        
        // bit 1-2: redeals
        val redealsBits = when (r.redeals) {
            -1 -> 0  // 무제한
            0 -> 1
            1 -> 2
            else -> 3  // 2 이상
        }
        code = code or (redealsBits shl 1)
        
        // bit 3: recycle
        code = code or ((if (r.recycle == RecycleOrder.REVERSE) 1 else 0) shl 3)
        
        // bit 4: allowFoundationToTableau
        code = code or ((if (r.allowFoundationToTableau) 1 else 0) shl 4)
        
        return BASE64[code]
    }
    
    /**
     * 1자에서 규칙 디코딩
     */
    private fun decodeRules(c: Char): Ruleset? {
        val idx = BASE64.indexOf(c)
        if (idx < 0) return null
        
        val draw = if ((idx and 1) == 1) 3 else 1
        
        val redeals = when ((idx shr 1) and 0b11) {
            0 -> -1  // 무제한
            1 -> 0
            2 -> 1
            else -> 2
        }
        
        val recycle = if (((idx shr 3) and 1) == 1) 
            RecycleOrder.REVERSE else RecycleOrder.KEEP
        
        val allowFoundationToTableau = ((idx shr 4) and 1) == 1
        
        return Ruleset(draw, redeals, recycle, allowFoundationToTableau)
    }
    
    /**
     * ULong을 Base64로 인코딩
     */
    private fun encodeBase64(value: ULong): String {
        // ULong을 8바이트 배열로 변환
        val bytes = ByteArray(8)
        var v = value
        for (i in 7 downTo 0) {
            bytes[i] = (v and 0xFFuL).toByte()
            v = v shr 8
        }
        
        // Base64 URL-safe 인코딩 (패딩 없음)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Base64 문자열을 ULong으로 디코딩
     */
    private fun decodeBase64(str: String): ULong? {
        return try {
            val bytes = Base64.getUrlDecoder().decode(str)
            if (bytes.size != 8) return null
            
            var result = 0uL
            for (b in bytes) {
                result = (result shl 8) or (b.toUByte().toULong())
            }
            result
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 게임 코드 유효성 검사
     */
    fun isValid(code: String): Boolean {
        return decode(code) != null
    }
    
    /**
     * 사람이 읽기 쉬운 형식으로 포맷 (4-4-4 그룹화)
     * 예: zYWFhYWFhYWF → z-YWFh-YWFhYWF
     */
    fun format(code: String): String {
        if (code.length < 5) return code
        
        return buildString {
            append(code[0])
            append('-')
            val seed = code.substring(1)
            val chunks = seed.chunked(4)
            append(chunks.joinToString("-"))
        }
    }
}

