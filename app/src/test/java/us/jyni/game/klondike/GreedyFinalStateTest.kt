package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.util.GameCode

class GreedyFinalStateTest {

    private lateinit var engine: GameEngine
    
    @Before
    fun setup() {
        engine = GameEngine()
    }

    @Test
    fun testYo7xEc_lbBwg_finalState() {
        val gameCode = "Yo7xEc_lbBwg"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val initialState = engine.getGameState()
        
        val output = StringBuilder()
        output.appendLine("\n========== GreedySolver Final State Test ==========")
        output.appendLine("Game Code: $gameCode")
        output.appendLine("\nInitial State:")
        output.append(getGameStateString(initialState))
        
        val solver = GreedySolver()
        val result = solver.solve(initialState)
        
        output.appendLine("\n========== Solver Result ==========")
        when (result) {
            is SolverResult.Success -> {
                output.appendLine("✓ SOLVED in ${result.moves.size} moves")
            }
            is SolverResult.InherentlyUnsolvable -> {
                output.appendLine("✗ Unsolvable: ${result.reason.message}")
                
                // Show final state from solver
                val finalState = solver.lastState
                if (finalState != null) {
                    output.appendLine("\n========== Final State ==========")
                    output.append(getGameStateString(finalState))
                    
                    val foundationCards = finalState.foundation.sumOf { it.size }
                    output.appendLine("\n========== Summary ==========")
                    output.appendLine("Foundation: $foundationCards / 52 cards")
                    output.appendLine("Remaining: ${52 - foundationCards} cards")
                }
            }
            is SolverResult.Timeout -> {
                output.appendLine("⏱ Timeout: ${result.reason}")
            }
            is SolverResult.TooComplex -> {
                output.appendLine("💥 Too Complex: ${result.reason}")
            }
            is SolverResult.UnwinnableState -> {
                output.appendLine("✗ Unwinnable: ${result.reason}")
            }
        }
        
        // Write to file
        val outputFile = java.io.File("/tmp/greedy_state_${gameCode}.txt")
        outputFile.writeText(output.toString())
        println("Output written to: ${outputFile.absolutePath}")
    }
    
    @Test
    fun testYo7xEc_IbBwg_finalState() {
        val gameCode = "Yo7xEc_IbBwg"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val initialState = engine.getGameState()
        
        val output = StringBuilder()
        output.appendLine("\n========== GreedySolver Final State Test ==========")
        output.appendLine("Game Code: $gameCode")
        output.appendLine("\nInitial State:")
        output.append(getGameStateString(initialState))
        
        val solver = GreedySolver()
        val result = solver.solve(initialState)
        
        output.appendLine("\n========== Solver Result ==========")
        when (result) {
            is SolverResult.Success -> {
                output.appendLine("✓ SOLVED in ${result.moves.size} moves")
            }
            is SolverResult.InherentlyUnsolvable -> {
                output.appendLine("✗ Unsolvable: ${result.reason.message}")
                
                // Show final state from solver
                val finalState = solver.lastState
                if (finalState != null) {
                    output.appendLine("\n========== Final State ==========")
                    output.append(getGameStateString(finalState))
                    
                    val foundationCards = finalState.foundation.sumOf { it.size }
                    output.appendLine("\n========== Summary ==========")
                    output.appendLine("Foundation: $foundationCards / 52 cards")
                    output.appendLine("Remaining: ${52 - foundationCards} cards")
                }
            }
            is SolverResult.Timeout -> {
                output.appendLine("⏱ Timeout: ${result.reason}")
            }
            is SolverResult.TooComplex -> {
                output.appendLine("💥 Too Complex: ${result.reason}")
            }
            is SolverResult.UnwinnableState -> {
                output.appendLine("✗ Unwinnable: ${result.reason}")
            }
        }
        
        // Write to file
        val outputFile = java.io.File("/tmp/greedy_state_${gameCode}.txt")
        outputFile.writeText(output.toString())
        println("Output written to: ${outputFile.absolutePath}")
    }
    
    @Test
    fun testYTIuVH1NfzxQ_finalState() {
        val gameCode = "YTIuVH1NfzxQ"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val initialState = engine.getGameState()
        
        val output = StringBuilder()
        output.appendLine("\n========== GreedySolver Final State Test ==========")
        output.appendLine("Game Code: $gameCode")
        output.appendLine("\nInitial State:")
        output.append(getGameStateString(initialState))
        
        val solver = GreedySolver()
        val result = solver.solve(initialState)
        
        output.appendLine("\n========== Solver Result ==========")
        when (result) {
            is SolverResult.Success -> {
                output.appendLine("✓ SOLVED in ${result.moves.size} moves")
            }
            is SolverResult.InherentlyUnsolvable -> {
                output.appendLine("✗ Unsolvable: ${result.reason.message}")
                
                // Show final state from solver
                val finalState = solver.lastState
                if (finalState != null) {
                    output.appendLine("\n========== Final State ==========")
                    output.append(getGameStateString(finalState))
                    
                    val foundationCards = finalState.foundation.sumOf { it.size }
                    output.appendLine("\n========== Summary ==========")
                    output.appendLine("Foundation: $foundationCards / 52 cards")
                    output.appendLine("Remaining: ${52 - foundationCards} cards")
                }
            }
            is SolverResult.Timeout -> {
                output.appendLine("⏱ Timeout: ${result.reason}")
            }
            is SolverResult.TooComplex -> {
                output.appendLine("💥 Too Complex: ${result.reason}")
            }
            is SolverResult.UnwinnableState -> {
                output.appendLine("✗ Unwinnable: ${result.reason}")
            }
        }
        
        // Write to file
        val outputFile = java.io.File("/tmp/greedy_state_${gameCode}.txt")
        outputFile.writeText(output.toString())
        println("Output written to: ${outputFile.absolutePath}")
    }
    
    private fun getGameStateString(state: GameState): String {
        val sb = StringBuilder()
        // Foundation
        sb.appendLine("Foundation:")
        state.foundation.forEachIndexed { index, pile ->
            val topCard = pile.lastOrNull()
            sb.appendLine("  F[$index]: ${topCard?.toString() ?: "empty"} (${pile.size} cards)")
        }
        
        // Tableau
        sb.appendLine("\nTableau:")
        state.tableau.forEachIndexed { index, pile ->
            val faceUpCount = pile.count { it.isFaceUp }
            val faceDownCount = pile.size - faceUpCount
            val topCard = pile.lastOrNull()
            sb.appendLine("  T[$index]: ${topCard?.toString() ?: "empty"} ($faceDownCount down, $faceUpCount up, total ${pile.size})")
        }
        
        // Stock and Waste
        sb.appendLine("\nStock: ${state.stock.size} cards")
        sb.appendLine("Waste: ${state.waste.lastOrNull()?.toString() ?: "empty"} (${state.waste.size} cards)")
        
        return sb.toString()
    }
}
