package us.jyni.game.klondike.solver

import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.engine.KlondikeRules

class GreedyDebugTest {
    
    @Test
    fun `debug greedy solver - what moves are available`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        var state = engine.getGameState()
        
        val solver = GreedySolver()
        val rules = KlondikeRules()
        
        println("=== Initial State ===")
        state.tableau.forEachIndexed { i, pile ->
            val faceUpCards = pile.filter { it.isFaceUp }
            println("T[$i]: ${pile.size} cards, ${faceUpCards.size} face-up, top=${pile.lastOrNull()}")
        }
        println("Foundation: ${state.foundation.map { it.size }}")
        println("Stock: ${state.stock.size}, Waste: ${state.waste.size}")
        
        // 각 단계별로 가능한 이동 확인
        for (step in 1..30) {
            println("\n=== Step $step ===")
            
            // Foundation 이동 체크
            var foundationMoves = 0
            for (col in 0..6) {
                val pile = state.tableau[col]
                if (pile.isNotEmpty()) {
                    for (f in 0..3) {
                        if (rules.canMoveTableauToFoundation(pile, state.foundation[f])) {
                            println("  ✓ Foundation: T[$col] -> F[$f] (${pile.last()})")
                            foundationMoves++
                        }
                    }
                }
            }
            if (state.waste.isNotEmpty()) {
                for (f in 0..3) {
                    if (rules.canMoveTableauToFoundation(state.waste, state.foundation[f])) {
                        println("  ✓ Foundation: W -> F[$f] (${state.waste.last()})")
                        foundationMoves++
                    }
                }
            }
            
            // Tableau 이동 체크
            var tableauMoves = 0
            for (fromCol in 6 downTo 0) {
                val fromPile = state.tableau[fromCol]
                if (fromPile.isEmpty()) continue
                
                val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
                if (firstFaceUpIdx == -1) continue
                
                for (cardIdx in firstFaceUpIdx until fromPile.size) {
                    val movingCards = fromPile.subList(cardIdx, fromPile.size)
                    val isBottomCard = (cardIdx == firstFaceUpIdx)
                    
                    for (toCol in 0..6) {
                        if (fromCol != toCol) {
                            if (rules.canMoveSequenceToTableau(movingCards, state.tableau[toCol])) {
                                val blocked = isBottomCard && !fromPile.any { it.isFaceUp && it.rank.value == 13 }
                                val marker = if (blocked) "❌ BLOCKED" else "✓"
                                println("  $marker Tableau: T[$fromCol][$cardIdx] -> T[$toCol] (${movingCards.first()}, bottom=$isBottomCard)")
                                if (!blocked) tableauMoves++
                            }
                        }
                    }
                }
            }
            
            // Waste 이동 체크
            var wasteMoves = 0
            if (state.waste.isNotEmpty()) {
                val wasteCard = state.waste.last()
                for (col in 0..6) {
                    if (rules.canMoveSequenceToTableau(listOf(wasteCard), state.tableau[col])) {
                        println("  ✓ Waste: W -> T[$col] (${wasteCard})")
                        wasteMoves++
                    }
                }
            }
            
            println("Summary: Foundation=$foundationMoves, Tableau=$tableauMoves, Waste=$wasteMoves")
            
            // 실제 solver의 선택
            val move = solver.findBestMove(state)
            if (move == null) {
                println("❌ Solver found NO move!")
                break
            }
            
            println("Solver chose: $move")
            
            val newState = GameStateUtils.applyMove(state, move)
            if (newState == null) {
                println("❌ Move failed!")
                break
            }
            
            state = newState
            
            if (state.foundation.sumOf { it.size } == 52) {
                println("🎉 GAME WON!")
                break
            }
        }
    }
}
