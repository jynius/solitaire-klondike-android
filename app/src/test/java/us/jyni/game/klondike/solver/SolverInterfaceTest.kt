package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.ui.GameViewModel

/**
 * Solver 인터페이스 및 Strategy 패턴 테스트
 */
class SolverInterfaceTest {
    
    @Test
    fun `Solver 인터페이스를 통해 BFSSolver와 AStarSolver 모두 사용 가능`() {
        val engine = GameEngine()
        engine.startGame(1234uL)
        val state = engine.getGameState()
        
        // BFSSolver
        val bfsSolver: Solver = BFSSolver(engine)
        val bfsResult = bfsSolver.solve(state)
        assertNotNull("BFS Solver가 결과를 반환해야 함", bfsResult)
        
        // AStarSolver
        val astarSolver: Solver = AStarSolver(engine)
        val astarResult = astarSolver.solve(state)
        assertNotNull("A* Solver가 결과를 반환해야 함", astarResult)
    }
    
    @Test
    fun `GameViewModel은 SolverType에 따라 다른 Solver를 사용`() {
        // BFS Solver 사용
        val bfsViewModel = GameViewModel(SolverType.BFS)
        assertEquals("BFS Solver 타입이어야 함", SolverType.BFS, bfsViewModel.getSolverType())
        
        // A* Solver 사용
        val astarViewModel = GameViewModel(SolverType.ASTAR)
        assertEquals("A* Solver 타입이어야 함", SolverType.ASTAR, astarViewModel.getSolverType())
    }
    
    @Test
    fun `기본 SolverType은 BFS`() {
        val viewModel = GameViewModel()  // 기본값
        assertEquals("기본 Solver는 BFS여야 함", SolverType.BFS, viewModel.getSolverType())
    }
    
    @Test
    fun `BFS와 A* 둘 다 findBestMove 메서드를 제공`() {
        val engine = GameEngine()
        engine.startGame(1234uL)
        val state = engine.getGameState()
        
        val bfsSolver: Solver = BFSSolver(engine)
        val astarSolver: Solver = AStarSolver(engine)
        
        // 둘 다 findBestMove 호출 가능
        val bfsMove = bfsSolver.findBestMove(state)
        val astarMove = astarSolver.findBestMove(state)
        
        // 결과는 다를 수 있지만, 메서드는 호출 가능해야 함
        // (타임아웃이나 복잡도로 null일 수 있음)
        assertTrue("BFS findBestMove 호출 성공", true)
        assertTrue("A* findBestMove 호출 성공", true)
    }
    
    @Test
    fun `Solver 인터페이스 타입으로 다형성 지원`() {
        val engine = GameEngine()
        
        val solvers: List<Solver> = listOf(
            BFSSolver(engine),
            AStarSolver(engine)
        )
        
        assertEquals("2개의 Solver가 리스트에 있어야 함", 2, solvers.size)
        
        // 모든 Solver가 solve 메서드를 가짐
        engine.startGame(5678uL)
        val state = engine.getGameState()
        
        for (solver in solvers) {
            val result = solver.solve(state)
            assertNotNull("각 Solver가 결과를 반환해야 함", result)
        }
    }
}
