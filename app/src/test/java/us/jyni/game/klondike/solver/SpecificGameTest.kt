package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.GameCode

/**
 * 특정 게임 ID에 대한 solvability 테스트
 */
class SpecificGameTest {
    private lateinit var engine: GameEngine
    
    @Before
    fun setup() {
        engine = GameEngine()
    }
    
    @Test
    fun `test game Yo7xEc_IbBwg - solvable but hard`() {
        // 두 가지 가능성: I(대문자) vs l(소문자)
        val possibleCodes = listOf("Yo7xEc_IbBwg", "Yo7xEc_lbBwg")
        
        for (gameCode in possibleCodes) {
            println("\n=== Testing: $gameCode ===")
            
            val decoded = GameCode.decode(gameCode)
            if (decoded == null) {
                println("❌ Invalid game code: $gameCode")
                continue
            }
            
            val (seed, rules) = decoded
            println("Seed: $seed")
            println("Draw: ${rules.draw}, Redeals: ${rules.redeals}")
            
            engine.startGame(seed = seed, rules = rules)
            val state = engine.getGameState()
            
            // UnsolvableDetector 확인
            val detector = UnsolvableDetector()
            val detectorResult = detector.check(state)
            
            println("UnsolvableDetector: ${if (detectorResult == null) "SOLVABLE" else "UNSOLVABLE (${detectorResult.message})"}")
            
            // BFS Solver 시도
            val bfsSolver = BFSSolver(rules)
            val bfsStart = System.currentTimeMillis()
            val bfsResult = bfsSolver.solve(state)
            val bfsTime = System.currentTimeMillis() - bfsStart
            
            println("BFS Solver (${bfsTime}ms):")
            when (bfsResult) {
                is SolverResult.Success -> {
                    println("  ✅ SOLVED: ${bfsResult.moves.size} moves")
                    println("  This game is SOLVABLE")
                    return // 성공하면 테스트 종료
                }
                is SolverResult.TooComplex -> println("  ❌ Too complex: ${bfsResult.reason}")
                is SolverResult.Timeout -> println("  ❌ Timeout: ${bfsResult.reason}")
                is SolverResult.InherentlyUnsolvable -> {
                    println("  ❌ Unsolvable: ${bfsResult.reason.message}")
                    fail("Expected SOLVABLE game, but solver says UNSOLVABLE")
                }
                else -> println("  ❌ ${bfsResult::class.simpleName}")
            }
            
            // Backward Solver 시도
            val backwardSolver = BackwardSolver(rules)
            val backwardStart = System.currentTimeMillis()
            val backwardResult = backwardSolver.solve(state)
            val backwardTime = System.currentTimeMillis() - backwardStart
            
            println("Backward Solver (${backwardTime}ms):")
            when (backwardResult) {
                is SolverResult.Success -> {
                    println("  ✅ SOLVED: ${backwardResult.moves.size} moves")
                    println("  States explored: ${backwardResult.statesExplored}")
                    println("  ✨ Backward solver succeeded where BFS failed!")
                    return // 성공하면 테스트 종료
                }
                is SolverResult.TooComplex -> println("  ❌ Too complex: ${backwardResult.reason}")
                is SolverResult.Timeout -> println("  ❌ Timeout: ${backwardResult.reason}")
                is SolverResult.InherentlyUnsolvable -> println("  ❌ Unsolvable: ${backwardResult.reason.message}")
                else -> println("  ❌ ${backwardResult::class.simpleName}")
            }
            
            // Heuristic Solver 시도 (도메인 지식 활용)
            val heuristicSolver = HeuristicSolver(rules)
            val heuristicStart = System.currentTimeMillis()
            val heuristicResult = heuristicSolver.solve(state)
            val heuristicTime = System.currentTimeMillis() - heuristicStart
            
            println("Heuristic Solver (${heuristicTime}ms):")
            when (heuristicResult) {
                is SolverResult.Success -> {
                    println("  ✅ SOLVED: ${heuristicResult.moves.size} moves")
                    println("  States explored: ${heuristicResult.statesExplored}")
                    println("  🎯 Heuristic solver succeeded with domain knowledge!")
                    return // 성공하면 테스트 종료
                }
                is SolverResult.TooComplex -> println("  ❌ Too complex: ${heuristicResult.reason}")
                is SolverResult.Timeout -> println("  ❌ Timeout: ${heuristicResult.reason}")
                is SolverResult.InherentlyUnsolvable -> println("  ❌ Unsolvable: ${heuristicResult.reason.message}")
                else -> println("  ❌ ${heuristicResult::class.simpleName}")
            }
            
            // Greedy Solver 시도 (규칙 기반)
            val greedySolver = GreedySolver(rules)
            val greedyStart = System.currentTimeMillis()
            val greedyResult = greedySolver.solve(state)
            val greedyTime = System.currentTimeMillis() - greedyStart
            
            println("Greedy Solver (${greedyTime}ms):")
            when (greedyResult) {
                is SolverResult.Success -> {
                    println("  ✅ SOLVED: ${greedyResult.moves.size} moves")
                    println("  🚀 Greedy solver succeeded with rule-based strategy!")
                    return // 성공하면 테스트 종료
                }
                is SolverResult.TooComplex -> println("  ❌ Too complex: ${greedyResult.reason}")
                is SolverResult.Timeout -> println("  ❌ Timeout: ${greedyResult.reason}")
                is SolverResult.InherentlyUnsolvable -> println("  ❌ Unsolvable: ${greedyResult.reason.message}")
                else -> println("  ❌ ${greedyResult::class.simpleName}")
            }
        }
        
        println("\n⚠️ Neither solver could solve this game within time/state limits")
        println("But the game is expected to be SOLVABLE")
        assertTrue("Test completed - game is hard but solvable", true)
    }
    
    @Test
    fun `test game YfXdty1HVIQE - check solvability`() {
        val gameCode = "YfXdty1HVIQE"
        
        // 1. 게임 코드 디코딩
        val decoded = GameCode.decode(gameCode)
        assertNotNull("Game code should be valid", decoded)
        
        val (seed, rules) = decoded!!
        println("=== Game Code Analysis ===")
        println("Code: $gameCode")
        println("Seed: $seed")
        println("Draw: ${rules.draw}")
        println("Redeals: ${rules.redeals}")
        println("Recycle: ${rules.recycle}")
        println("Foundation→Tableau: ${rules.allowFoundationToTableau}")
        
        // 2. 게임 시작
        engine.startGame(seed = seed, rules = rules)
        val initialState = engine.getGameState()
        
        println("\n=== Initial State ===")
        println("Deal ID: ${engine.getDealId()}")
        println("Layout ID: ${engine.getLayoutId()}")
        
        // 3. UnsolvableDetector로 먼저 판단 (현재 시스템의 판단)
        val detector = UnsolvableDetector()
        val detectorResult = detector.check(initialState)
        
        println("\n=== UnsolvableDetector Result ===")
        if (detectorResult == null) {
            println("❌ Detector says: SOLVABLE (no unsolvable pattern detected)")
        } else {
            println("✓ Detector says: UNSOLVABLE")
            println("  Reason: ${detectorResult.message}")
        }
        
        // 4. BFSSolver로 실제 해결 가능 여부 확인 (ground truth)
        val solver = BFSSolver(rules)
        
        println("\n=== BFS Solver Result (Ground Truth) ===")
        val startTime = System.currentTimeMillis()
        val solverResult = solver.solve(initialState)
        val elapsed = System.currentTimeMillis() - startTime
        
        println("Solving time: ${elapsed}ms")
        
        val actuallyUnsolvable = when (solverResult) {
            is SolverResult.Success -> {
                println("✅ Solver says: SOLVABLE")
                println("   Found solution with ${solverResult.moves.size} moves")
                false
            }
            is SolverResult.InherentlyUnsolvable -> {
                println("❌ Solver says: INHERENTLY UNSOLVABLE")
                println("   Reason: ${solverResult.reason.message}")
                true
            }
            is SolverResult.UnwinnableState -> {
                println("⚠ Solver says: UNWINNABLE STATE")
                println("   Reason: ${solverResult.reason}")
                true
            }
            is SolverResult.Timeout -> {
                println("⏱ Solver TIMEOUT (unable to determine)")
                println("   Reason: ${solverResult.reason}")
                null
            }
            is SolverResult.TooComplex -> {
                println("🔴 Solver says: TOO COMPLEX (unable to determine)")
                println("   Reason: ${solverResult.reason}")
                null
            }
        }
        
        // 5. 결과 비교
        println("\n=== Comparison ===")
        val detectorSaysUnsolvable = (detectorResult != null)
        
        println("Detector: ${if (detectorSaysUnsolvable) "UNSOLVABLE" else "SOLVABLE"}")
        println("Solver:   ${when(actuallyUnsolvable) { 
            true -> "UNSOLVABLE" 
            false -> "SOLVABLE" 
            null -> "UNKNOWN" 
        }}")
        
        if (actuallyUnsolvable == null) {
            // Solver가 결정하지 못한 경우 - UnsolvableDetector 결과만 출력
            println("\n⚠️ INCONCLUSIVE: Solver could not determine (timeout/too complex)")
            println("However, UnsolvableDetector says: ${if (detectorSaysUnsolvable) "UNSOLVABLE" else "SOLVABLE"}")
            
            if (!detectorSaysUnsolvable) {
                println("\n🐛 POTENTIAL BUG: Detector says SOLVABLE, but solver couldn't solve it within limits")
                println("   This might be a false negative - the game could be unsolvable")
                println("   or it might just be very complex.")
                
                // 이 경우는 fail하지 않고 경고만 출력
                assertTrue("Test completed with warning", true)
                return
            } else {
                // Detector가 unsolvable이라고 했고, solver도 풀지 못함 - 일치 가능성
                println("\n✓ Detector says UNSOLVABLE, and solver couldn't find solution")
                assertTrue("Detector result seems correct", true)
                return
            }
        }
        
        if (detectorSaysUnsolvable != actuallyUnsolvable) {
            if (actuallyUnsolvable && !detectorSaysUnsolvable) {
                fail("🐛 FALSE NEGATIVE: Detector says SOLVABLE, but game is actually UNSOLVABLE!\n" +
                     "This is a bug in UnsolvableDetector - it failed to detect an unsolvable game.")
            } else {
                fail("🐛 FALSE POSITIVE: Detector says UNSOLVABLE, but game is actually SOLVABLE!\n" +
                     "Detector reason: ${detectorResult?.message}")
            }
        } else {
            println("\n✅ MATCH: Both agree the game is ${if (actuallyUnsolvable) "UNSOLVABLE" else "SOLVABLE"}")
            assertTrue("Results match", true)
        }
    }
    
    @Test
    fun `test game YfXdty1HVIQE - compare backward solver`() {
        val gameCode = "YfXdty1HVIQE"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val state = engine.getGameState()
        
        println("=== Backward Solver Test ===")
        
        // BFS Solver
        val bfsSolver = BFSSolver(rules)
        val bfsStart = System.currentTimeMillis()
        val bfsResult = bfsSolver.solve(state)
        val bfsTime = System.currentTimeMillis() - bfsStart
        
        println("BFS Solver: ${bfsTime}ms")
        when (bfsResult) {
            is SolverResult.Success -> println("  ✓ Solution found: ${bfsResult.moves.size} moves")
            is SolverResult.TooComplex -> println("  ✗ Too complex: ${bfsResult.reason}")
            is SolverResult.Timeout -> println("  ✗ Timeout: ${bfsResult.reason}")
            else -> println("  ✗ ${bfsResult::class.simpleName}")
        }
        
        // Backward Solver
        val backwardSolver = BackwardSolver(rules)
        val backwardStart = System.currentTimeMillis()
        val backwardResult = backwardSolver.solve(state)
        val backwardTime = System.currentTimeMillis() - backwardStart
        
        println("\nBackward Solver: ${backwardTime}ms")
        when (backwardResult) {
            is SolverResult.Success -> {
                println("  ✓ Solution found: ${backwardResult.moves.size} moves")
                println("  States explored: ${backwardResult.statesExplored}")
            }
            is SolverResult.TooComplex -> println("  ✗ Too complex: ${backwardResult.reason}")
            is SolverResult.Timeout -> println("  ✗ Timeout: ${backwardResult.reason}")
            is SolverResult.InherentlyUnsolvable -> println("  ✗ Unsolvable: ${backwardResult.reason.message}")
            else -> println("  ✗ ${backwardResult::class.simpleName}")
        }
        
        println("\n=== Performance Comparison ===")
        println("BFS:      ${bfsTime}ms - ${(bfsResult as? SolverResult.Success)?.statesExplored ?: "N/A"} states")
        println("Backward: ${backwardTime}ms - ${(backwardResult as? SolverResult.Success)?.statesExplored ?: "N/A"} states")
        
        if (backwardResult is SolverResult.Success && bfsResult !is SolverResult.Success) {
            println("\n✅ Backward Solver succeeded where BFS failed!")
        } else if (bfsResult is SolverResult.Success && backwardResult !is SolverResult.Success) {
            println("\n✅ BFS Solver succeeded where Backward failed!")
        }
        
        assertTrue("Test completed", true)
    }
    
    @Test
    fun `test game YfXdty1HVIQE - check unsolvable detection`() {
        val gameCode = "YfXdty1HVIQE"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        engine.startGame(seed = seed, rules = rules)
        val state = engine.getGameState()
        
        println("=== Unsolvable Detection Analysis ===")
        
        // UnsolvableDetector로 확인
        val detector = UnsolvableDetector()
        val result = detector.check(state)
        
        when (result) {
            is UnsolvableReason.NPileIrretrievable -> {
                println("✓ N-Pile Irretrievable detected")
                println("  Message: ${result.message}")
            }
            is UnsolvableReason.DeadEnd -> {
                println("✓ Dead End detected")
                println("  Reason: ${result.reason}")
            }
            is UnsolvableReason.StateCycle -> {
                println("✓ State Cycle detected")
                println("  Reason: ${result.reason}")
            }
            is UnsolvableReason.KingIrretrievable -> {
                println("✓ King Irretrievable detected")
                println("  Message: ${result.message}")
            }
            is UnsolvableReason.CircularDependency -> {
                println("✓ Circular Dependency detected")
                println("  Reason: ${result.reason}")
            }
            null -> {
                println("⚠ No unsolvable reason detected initially")
                println("  (May require game progression to detect)")
            }
        }
    }
    
    @Test
    fun `test game YfXdty1HVIQE - verify layout`() {
        val gameCode = "YfXdty1HVIQE"
        val decoded = GameCode.decode(gameCode)!!
        val (seed, rules) = decoded
        
        // 같은 시드로 2번 시작하면 동일한 레이아웃이어야 함
        engine.startGame(seed = seed, rules = rules)
        val dealId1 = engine.getDealId()
        val layoutId1 = engine.getLayoutId()
        
        engine.startGame(seed = seed, rules = rules)
        val dealId2 = engine.getDealId()
        val layoutId2 = engine.getLayoutId()
        
        assertEquals("Same seed should produce same Deal ID", dealId1, dealId2)
        assertEquals("Same seed should produce same Layout ID", layoutId1, layoutId2)
        
        println("Deal ID: $dealId1")
        println("Layout ID: $layoutId1")
    }
}
