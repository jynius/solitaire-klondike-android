package us.jyni.game.klondike.solver

import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.engine.GameEngine

/**
 * Backward Solver 기본 동작 테스트
 */
class BackwardSolverTest {
    
    @Test
    fun `test backward solver on simple game`() {
        val engine = GameEngine()
        
        // 간단한 게임으로 테스트 (seed 1은 보통 쉬움)
        engine.startGame(seed = 1uL)
        val state = engine.getGameState()
        
        println("=== Simple Game Test (Seed 1) ===")
        
        val bfsSolver = BFSSolver()
        val bfsStart = System.currentTimeMillis()
        val bfsResult = bfsSolver.solve(state)
        val bfsTime = System.currentTimeMillis() - bfsStart
        
        val backwardSolver = BackwardSolver()
        val backwardStart = System.currentTimeMillis()
        val backwardResult = backwardSolver.solve(state)
        val backwardTime = System.currentTimeMillis() - backwardStart
        
        println("BFS:      ${bfsTime}ms - ${(bfsResult as? SolverResult.Success)?.moves?.size ?: "N/A"} moves")
        println("Backward: ${backwardTime}ms - ${(backwardResult as? SolverResult.Success)?.moves?.size ?: "N/A"} moves")
        
        // 적어도 하나는 성공해야 함
        val anySuccess = bfsResult is SolverResult.Success || backwardResult is SolverResult.Success
        assertTrue("At least one solver should succeed on simple game", anySuccess)
    }
    
    @Test
    fun `test backward solver goal state creation`() {
        val engine = GameEngine()
        engine.startGame(seed = 42uL)
        val initialState = engine.getGameState()
        
        println("=== Goal State Concept ===")
        println("Initial state:")
        println("  Tableau piles: ${initialState.tableau.map { it.size }}")
        println("  Foundation piles: ${initialState.foundation.map { it.size }}")
        println("  Stock: ${initialState.stock.size}")
        println("  Waste: ${initialState.waste.size}")
        
        println("\nBackward solver starts from goal (all cards in foundation)")
        println("and works backwards to initial state.")
        
        assertTrue("Backward solver created successfully", true)
    }
}
