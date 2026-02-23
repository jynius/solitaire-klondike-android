package us.jyni.game.klondike.solver

import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.GameCode

class GreedySolverTest {
    
    @Test
    fun `test greedy solver on simple game`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        val state = engine.getGameState()
        
        println("=== Initial State ===")
        println("Tableau sizes: ${state.tableau.map { it.size }}")
        println("Foundation sizes: ${state.foundation.map { it.size }}")
        println("Stock: ${state.stock.size}, Waste: ${state.waste.size}")
        
        val solver = GreedySolver()
        val start = System.currentTimeMillis()
        val result = solver.solve(state)
        val time = System.currentTimeMillis() - start
        
        println("\n=== Result (${time}ms) ===")
        when (result) {
            is SolverResult.Success -> {
                println("✅ SOLVED!")
                println("Total moves: ${result.moves.size}")
                println("First 20 moves:")
                result.moves.take(20).forEachIndexed { i, move ->
                    println("  ${i+1}. $move")
                }
            }
            is SolverResult.TooComplex -> {
                println("❌ Too complex: ${result.reason}")
            }
            is SolverResult.Timeout -> {
                println("❌ Timeout: ${result.reason}")
            }
            is SolverResult.InherentlyUnsolvable -> {
                println("❌ Unsolvable: ${result.reason.message}")
            }
            is SolverResult.UnwinnableState -> {
                println("❌ Unwinnable: ${result.reason}")
            }
        }
    }
    
    @Test
    fun `test greedy solver step by step`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        var state = engine.getGameState()
        
        val solver = GreedySolver()
        
        println("=== Step-by-Step Greedy Solver ===")
        for (step in 1..20) {
            println("\n--- Step $step ---")
            
            val move = solver.findBestMove(state)
            if (move == null) {
                println("No more moves available")
                break
            }
            
            println("Move: $move")
            
            val newState = GameStateUtils.applyMove(state, move)
            if (newState == null) {
                println("❌ Move failed!")
                break
            }
            
            state = newState
            
            println("Foundation: ${state.foundation.map { it.size }}")
            if (state.foundation.sumOf { it.size } == 52) {
                println("🎉 GAME WON!")
                break
            }
        }
    }
}
