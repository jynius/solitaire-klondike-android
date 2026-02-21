import us.jyni.game.klondike.util.GameCode
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.engine.GameEngine

fun main() {
    val gameCode = "YpUzGOpD-YWg"
    val cleanCode = gameCode.replace("-", "")
    
    val decoded = GameCode.decode(cleanCode)
    
    if (decoded != null) {
        val (seed, rules) = decoded
        println("=== Game Code: $gameCode ===")
        println("Seed: $seed")
        println("Rules: $rules")
        println()
        
        // 게임 시작해서 초기 상태 확인
        val engine = GameEngine()
        engine.startGame(seed, rules)
        
        // 초기 tableau 상태 출력
        println("=== Initial Tableau State ===")
        val state = engine.getGameState()
        state.tableau.forEachIndexed { index, pile ->
            println("\nPile $index:")
            println("  Face-down: ${pile.count { !it.isFaceUp }} cards")
            println("  Face-up: ${pile.filter { it.isFaceUp }.joinToString(", ") { "${it.suit.symbol}${it.rank.symbol}" }}")
            if (pile.any { !it.isFaceUp }) {
                println("  (Face-down blocked)")
            }
        }
        
        println("\n=== Stock & Waste ===")
        println("Stock: ${state.stock.size} cards")
        println("Waste: ${state.waste.size} cards")
        
        // 특정 카드 위치 찾기 (다이아몬드 A)
        println("\n=== Finding DIAMONDS ACE ===")
        state.tableau.forEachIndexed { index, pile ->
            pile.forEach { card ->
                if (card.suit.name == "DIAMONDS" && card.rank.name == "ACE") {
                    println("Found in Pile $index (${if (card.isFaceUp) "face-up" else "face-down"})")
                }
            }
        }
        state.stock.forEach { card ->
            if (card.suit.name == "DIAMONDS" && card.rank.name == "ACE") {
                println("Found in Stock")
            }
        }
        state.waste.forEach { card ->
            if (card.suit.name == "DIAMONDS" && card.rank.name == "ACE") {
                println("Found in Waste")
            }
        }
    } else {
        println("Failed to decode game code!")
    }
}
