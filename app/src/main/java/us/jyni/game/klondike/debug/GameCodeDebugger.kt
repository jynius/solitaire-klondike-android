package us.jyni.game.klondike.debug

import us.jyni.game.klondike.util.GameCode
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.solver.UnsolvableDetector

/**
 * 게임 코드 디버깅 헬퍼
 */
fun main() {
    val code = "YYxQSt3-oDyg"  // 2-pile Irretrievable 케이스
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
        
        // 게임 생성 및 확인
        println("\n=== Game State Analysis ===")
        val engine = GameEngine()
        engine.startGame(seed, rules)
        val state = engine.getGameState()
        
        // Tableau 출력
        println("\nTableau:")
        state.tableau.forEachIndexed { i, pile ->
            val faceDown = pile.filter { !it.isFaceUp }
            val faceUp = pile.filter { it.isFaceUp }
            println("Pile $i: FD=${faceDown.size} [${faceDown.joinToString { "${it.suit}-${it.rank}" }}] | " +
                    "FU=${faceUp.size} [${faceUp.joinToString { "${it.suit}-${it.rank}" }}]")
        }
        
        // Unsolvable 검사
        println("\n=== Unsolvable Detection ===")
        val detector = UnsolvableDetector()
        val (result, log) = detector.checkInherentlyUnsolvableWithDebug(state)
        println(log)
        
        if (result != null) {
            println("\n결과: UNSOLVABLE - $result")
        } else {
            println("\n결과: SOLVABLE (또는 감지 실패)")
        }
    } else {
        println("Failed to decode game code")
    }
}
