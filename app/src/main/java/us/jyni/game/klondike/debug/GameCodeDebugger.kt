package us.jyni.game.klondike.debug

import us.jyni.game.klondike.util.GameCode

/**
 * 게임 코드 디버깅 헬퍼
 */
fun main() {
    val code = "YpUzGOpD-YWg"  // 하이픈 제거 필요
    val cleanCode = code.replace("-", "")
    
    println("Original Code: $code")
    println("Clean Code: $cleanCode")
    
    val decoded = GameCode.decode(cleanCode)
    
    if (decoded != null) {
        val (seed, rules) = decoded
        println("\n=== Decoded Game ===")
        println("Seed: $seed")
        println("Draw: ${rules.draw}")
        println("Redeals: ${rules.redeals}")
        println("Recycle: ${rules.recycle}")
        println("Foundation→Tableau: ${rules.allowFoundationToTableau}")
    } else {
        println("Failed to decode game code")
    }
}
