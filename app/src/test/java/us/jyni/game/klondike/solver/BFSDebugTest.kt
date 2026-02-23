package us.jyni.game.klondike.solver

import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * BFS 탐색 디버깅
 */
class BFSDebugTest {
    
    @Test
    fun `debug BFS search on seed 1`() {
        val engine = GameEngine()
        val rules = Ruleset(draw = 1, redeals = -1)
        engine.startGame(seed = 1uL, rules = rules)
        val state = engine.getGameState()
        
        println("=== BFS Debug Test ===")
        println("Initial state:")
        println("  Tableau: ${state.tableau.map { it.size }}")
        println("  Foundation: ${state.foundation.map { it.size }}")
        println("  Stock: ${state.stock.size}")
        println("  Waste: ${state.waste.size}")
        
        // 수동으로 몇 가지 이동 시도
        val moves = mutableListOf<Move>()
        
        // Tableau에서 가능한 이동 찾기
        for (from in state.tableau.indices) {
            val pile = state.tableau[from]
            if (pile.isEmpty()) continue
            
            val topCard = pile.last()
            if (!topCard.isFaceUp) continue
            
            // Foundation으로 이동 가능한지
            for (f in 0..3) {
                val newState = GameStateUtils.applyMove(state, Move.TableauToFoundation(from, f))
                if (newState != null) {
                    moves.add(Move.TableauToFoundation(from, f))
                    println("  Can move T[$from] -> F[$f]: ${topCard.rank} of ${topCard.suit}")
                }
            }
        }
        
        // Draw 가능한지
        if (state.stock.isNotEmpty()) {
            println("  Can draw from stock")
            moves.add(Move.Draw)
        }
        
        println("\nTotal possible moves: ${moves.size}")
        
        // 실제 BFS 실행하되, 진행상황 출력하는 버전으로
        val solver = BFSSolverWithLogging(rules)
        val result = solver.solve(state)
        
        println("\nResult: $result")
    }
}

/**
 * 로깅 기능이 있는 BFS Solver
 */
class BFSSolverWithLogging(private val rules: Ruleset = Ruleset()) {
    
    fun solve(initialState: us.jyni.game.klondike.model.GameState): SolverResult {
        val queue = java.util.LinkedList<Pair<us.jyni.game.klondike.model.GameState, List<Move>>>()
        val visited = mutableSetOf<String>()
        
        queue.add(Pair(initialState, emptyList()))
        visited.add(GameStateUtils.stateHash(initialState))
        
        var statesExplored = 0
        val maxStates = 1000 // 작게 제한
        
        while (queue.isNotEmpty()) {
            if (statesExplored >= maxStates) {
                println("\n탐색 중단: $maxStates 상태 탐색 완료")
                println("방문한 unique states: ${visited.size}")
                println("큐에 남은 노드: ${queue.size}")
                return SolverResult.TooComplex("탐색 상태 수 초과")
            }
            
            val (state, path) = queue.poll()
            statesExplored++
            
            if (statesExplored % 100 == 0) {
                println("Progress: $statesExplored states, depth ${path.size}, queue ${queue.size}")
            }
            
            // 승리 확인
            if (state.foundation.sumOf { it.size } == 52) {
                println("\n✅ 해결! ${path.size} moves, $statesExplored states explored")
                return SolverResult.Success(path, statesExplored)
            }
            
            // 가능한 이동 생성
            val moves = generateAllMoves(state)
            
            for (move in moves) {
                val nextState = GameStateUtils.applyMove(state, move)
                if (nextState == null) continue
                
                val hash = GameStateUtils.stateHash(nextState)
                if (hash !in visited) {
                    visited.add(hash)
                    queue.add(Pair(nextState, path + move))
                }
            }
        }
        
        return SolverResult.InherentlyUnsolvable(
            UnsolvableReason.DeadEnd("모든 상태 탐색 완료")
        )
    }
    
    private fun generateAllMoves(state: us.jyni.game.klondike.model.GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        // Tableau -> Foundation (only one foundation per card to avoid duplicates)
        for (t in state.tableau.indices) {
            val pile = state.tableau[t]
            if (pile.isNotEmpty() && pile.last().isFaceUp) {
                // Try all foundations but only add first valid one
                for (f in 0..3) {
                    if (GameStateUtils.applyMove(state, Move.TableauToFoundation(t, f)) != null) {
                        moves.add(Move.TableauToFoundation(t, f))
                        break // Only one foundation move per card
                    }
                }
            }
        }
        
        // Waste -> Foundation (only one foundation)
        if (state.waste.isNotEmpty()) {
            for (f in 0..3) {
                if (GameStateUtils.applyMove(state, Move.WasteToFoundation(f)) != null) {
                    moves.add(Move.WasteToFoundation(f))
                    break // Only one foundation move
                }
            }
        }
        
        // Tableau -> Tableau
        for (from in state.tableau.indices) {
            val pile = state.tableau[from]
            if (pile.isEmpty()) continue
            
            val firstFaceUpIdx = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue
            
            for (cardIdx in firstFaceUpIdx until pile.size) {
                for (to in state.tableau.indices) {
                    if (from != to) {
                        if (GameStateUtils.applyMove(state, Move.TableauToTableau(from, cardIdx, to)) != null) {
                            moves.add(Move.TableauToTableau(from, cardIdx, to))
                        }
                    }
                }
            }
        }
        
        // Waste -> Tableau
        for (t in state.tableau.indices) {
            if (GameStateUtils.applyMove(state, Move.WasteToTableau(t)) != null) {
                moves.add(Move.WasteToTableau(t))
            }
        }
        
        // Draw
        if (state.stock.isNotEmpty()) {
            moves.add(Move.Draw)
        }
        
        return moves
    }
}
