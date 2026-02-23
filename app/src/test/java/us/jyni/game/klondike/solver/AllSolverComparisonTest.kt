package us.jyni.game.klondike.solver

import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.GameCode
import java.io.File

class AllSolverComparisonTest {

    private lateinit var engine: GameEngine
    
    @Before
    fun setup() {
        engine = GameEngine()
    }

    @Test
    fun compareAllSolvers_Yo7xEc_lbBwg() {
        val gameCode = "Yo7xEc_lbBwg"
        compareAllSolvers(gameCode)
    }

    @Test
    fun compareAllSolvers_Yo7xEc_IbBwg() {
        val gameCode = "Yo7xEc_IbBwg"
        compareAllSolvers(gameCode)
    }

    @Test
    fun compareAllSolvers_YTIuVH1NfzxQ() {
        val gameCode = "YTIuVH1NfzxQ"
        compareAllSolvers(gameCode)
    }

    private fun compareAllSolvers(gameCode: String) {
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val initialState = engine.getGameState()
        
        val output = StringBuilder()
        output.appendLine("\n" + "=".repeat(80))
        output.appendLine("Game Code: $gameCode")
        output.appendLine("=".repeat(80))
        
        val solvers = listOf(
            "BFS" to BFSSolver(),
            "Backward" to BackwardSolver(),
            "Heuristic" to HeuristicSolver(),
            "A*" to AStarSolver(),
            "Greedy" to GreedySolver()
        )
        
        for ((name, solver) in solvers) {
            output.appendLine("\n--- $name Solver ---")
            
            val startTime = System.currentTimeMillis()
            val result = solver.solve(initialState)
            val elapsed = System.currentTimeMillis() - startTime
            
            when (result) {
                is SolverResult.Success -> {
                    output.appendLine("✓ SOLVED")
                    output.appendLine("  Moves: ${result.moves.size}")
                    output.appendLine("  States: ${result.statesExplored}")
                    output.appendLine("  Time: ${elapsed}ms")
                }
                is SolverResult.InherentlyUnsolvable -> {
                    output.appendLine("✗ Unsolvable: ${result.reason.message}")
                    output.appendLine("  Time: ${elapsed}ms")
                }
                is SolverResult.Timeout -> {
                    output.appendLine("⏱ Timeout: ${result.reason}")
                    output.appendLine("  Time: ${elapsed}ms")
                }
                is SolverResult.TooComplex -> {
                    output.appendLine("💥 Too Complex: ${result.reason}")
                    output.appendLine("  Time: ${elapsed}ms")
                }
                is SolverResult.UnwinnableState -> {
                    output.appendLine("✗ Unwinnable: ${result.reason}")
                    output.appendLine("  Time: ${elapsed}ms")
                }
            }
        }
        
        output.appendLine("\n" + "=".repeat(80))
        
        // Write to file
        val outputFile = File("/tmp/solver_comparison_${gameCode}.txt")
        outputFile.writeText(output.toString())
        println(output.toString())
        println("\nOutput written to: ${outputFile.absolutePath}")
    }
}
