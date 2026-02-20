package us.jyni.game.klondike.util

import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class GameCodeTest {
    
    @Test
    fun testEncodeDecodeBasic() {
        val seed = 6340476947374324331uL
        val rules = Ruleset(
            draw = 3,
            redeals = -1,
            recycle = RecycleOrder.REVERSE,
            allowFoundationToTableau = true
        )
        
        val code = GameCode.encode(seed, rules)
        println("Game Code (Base64): $code (${code.length} chars)")
        
        // 길이 확인 (Base64는 약 12자)
        assertTrue("Code should be around 12-13 chars", code.length in 11..14)
        
        // 디코딩 확인
        val decoded = GameCode.decode(code)
        assertNotNull("Should decode successfully", decoded)
        
        val (decodedSeed, decodedRules) = decoded!!
        assertEquals("Seed should match", seed, decodedSeed)
        assertEquals("Draw should match", rules.draw, decodedRules.draw)
        assertEquals("Redeals should match", rules.redeals, decodedRules.redeals)
        assertEquals("Recycle should match", rules.recycle, decodedRules.recycle)
        assertEquals("Foundation to Tableau should match", 
            rules.allowFoundationToTableau, decodedRules.allowFoundationToTableau)
    }
    
    @Test
    fun testDifferentRules() {
        val seed = 12345uL
        
        // D1, 0 redeals, KEEP, no foundation to tableau
        val rules1 = Ruleset(1, 0, RecycleOrder.KEEP, false)
        val code1 = GameCode.encode(seed, rules1)
        val decoded1 = GameCode.decode(code1)
        assertNotNull(decoded1)
        assertEquals(rules1, decoded1!!.second)
        
        // D3, 2 redeals, REVERSE, allow foundation to tableau
        val rules2 = Ruleset(3, 2, RecycleOrder.REVERSE, true)
        val code2 = GameCode.encode(seed, rules2)
        val decoded2 = GameCode.decode(code2)
        assertNotNull(decoded2)
        assertEquals(rules2, decoded2!!.second)
        
        // 다른 규칙은 다른 코드여야 함
        assertNotEquals("Different rules should have different codes", code1, code2)
    }
    
    @Test
    fun testZeroSeed() {
        val seed = 0uL
        val rules = Ruleset()
        
        val code = GameCode.encode(seed, rules)
        val decoded = GameCode.decode(code)
        
        assertNotNull(decoded)
        assertEquals(seed, decoded!!.first)
    }
    
    @Test
    fun testMaxSeed() {
        val seed = ULong.MAX_VALUE
        val rules = Ruleset()
        
        val code = GameCode.encode(seed, rules)
        println("Max seed code: $code (${code.length} chars)")
        
        val decoded = GameCode.decode(code)
        assertNotNull(decoded)
        assertEquals(seed, decoded!!.first)
    }
    
    @Test
    fun testInvalidCodes() {
        assertNull("Empty code should be invalid", GameCode.decode(""))
        assertNull("Single char should be invalid", GameCode.decode("A"))
        assertFalse("Empty code is not valid", GameCode.isValid(""))
        assertFalse("Single char is not valid", GameCode.isValid("A"))
    }
    
    @Test
    fun testFormat() {
        val code = "zYWFhYWFhYWF"
        val formatted = GameCode.format(code)
        
        println("Original: $code")
        println("Formatted: $formatted")
        
        assertTrue("Should contain hyphens", formatted.contains('-'))
    }
    
    @Test
    fun testAllRuleCombinations() {
        val seed = 123456uL
        val draws = listOf(1, 3)
        val redeals = listOf(-1, 0, 1, 2)
        val recycles = RecycleOrder.values()
        val foundations = listOf(false, true)
        
        var count = 0
        for (draw in draws) {
            for (redeal in redeals) {
                for (recycle in recycles) {
                    for (foundation in foundations) {
                        val rules = Ruleset(draw, redeal, recycle, foundation)
                        val code = GameCode.encode(seed, rules)
                        val decoded = GameCode.decode(code)
                        
                        assertNotNull("Should decode: $code", decoded)
                        assertEquals("Rules should match for $code", rules, decoded!!.second)
                        count++
                    }
                }
            }
        }
        
        println("Tested $count rule combinations successfully")
        assertEquals("Should test all combinations", 2 * 4 * 2 * 2, count)
    }
}
