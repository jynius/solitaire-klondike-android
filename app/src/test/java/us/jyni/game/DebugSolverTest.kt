package us.jyni.game

import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.solver.BFSSolver
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.solver.SolverResult
import us.jyni.game.klondike.util.sync.Ruleset

class DebugSolverTest {
    @Test
    fun test_solver_basic() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL, rules = Ruleset())
        
        val solver = BFSSolver()
        val state = engine.getGameState()
        
        println("Initial state - Stock: ${state.stock.size}, Waste: ${state.waste.size}")
        println("Tableau sizes: ${state.tableau.map { it.size }}")
        
        val result = solver.solve(state)
        
        println("Result type: ${result::class.simpleName}")
        when (result) {
            is SolverResult.Success -> println("Success! Moves: ${result.moves.size}")
            is SolverResult.InherentlyUnsolvable -> println("InherentlyUnsolvable: ${result.reason}")
            is SolverResult.UnwinnableState -> println("UnwinnableState: ${result.reason}")
            is SolverResult.Timeout -> println("Timeout: ${result.reason}")
            is SolverResult.TooComplex -> println("TooComplex: ${result.reason}")
        }
        
        // 최소한 결과는 반환되어야 함
        assertNotNull(result)
    }
}
