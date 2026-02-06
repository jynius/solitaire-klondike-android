package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.GameState
import java.util.LinkedList
import java.util.Queue

/**
 * BFS 기반 솔리테어 Solver
 * 
 * 완전 정보 게임 탐색을 통해 승리 경로를 찾습니다.
 */
class BFSSolver(private val engine: GameEngine) {
    
    companion object {
        private const val MAX_DEPTH = 50
        private const val MAX_STATES = 10000
        private const val TIMEOUT_MS = 5000L
    }
    
    /**
     * 현재 상태에서 승리까지의 경로 찾기
     * @return SolverResult (승리 경로 또는 실패 이유)
     */
    fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        
        // Unsolvable 빠른 체크
        val unsolvableDetector = UnsolvableDetector(engine)
        val unsolvableReason = unsolvableDetector.check(initialState)
        if (unsolvableReason != null) {
            return SolverResult.Unsolvable(unsolvableReason.message)
        }
        
        val queue: Queue<SearchNode> = LinkedList()
        val visited = mutableSetOf<String>()
        
        val initialNode = SearchNode(
            state = initialState,
            gCost = 0,
            hCost = 0,
            path = emptyList()
        )
        
        queue.add(initialNode)
        visited.add(GameStateUtils.stateHash(initialState))
        
        var statesExplored = 0
        
        while (queue.isNotEmpty()) {
            // 타임아웃 체크
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return SolverResult.Timeout("탐색 시간 초과 (${statesExplored}개 상태 탐색)")
            }
            
            val node = queue.poll()
            statesExplored++
            
            // 승리 조건 체크
            if (node.state.isGameOver) {
                return SolverResult.Success(
                    moves = node.path,
                    statesExplored = statesExplored
                )
            }
            
            // 깊이 제한
            if (node.path.size >= MAX_DEPTH) {
                continue
            }
            
            // 상태 수 제한
            if (statesExplored >= MAX_STATES) {
                return SolverResult.TooComplex("탐색 상태 수 초과 (${MAX_STATES}개)")
            }
            
            // 가능한 모든 이동 생성
            val possibleMoves = getAllPossibleMoves(node.state)
            
            for (move in possibleMoves) {
                val newState = GameStateUtils.applyMove(node.state, move) ?: continue
                val stateHash = GameStateUtils.stateHash(newState)
                
                if (stateHash !in visited) {
                    visited.add(stateHash)
                    
                    val newNode = SearchNode(
                        state = newState,
                        gCost = node.gCost + 1,
                        hCost = 0,
                        path = node.path + move
                    )
                    
                    queue.add(newNode)
                }
            }
        }
        
        return SolverResult.Unsolvable("모든 경로 탐색 완료 - 승리 불가능 (${statesExplored}개 상태 탐색)")
    }
    
    /**
     * 최선의 다음 이동 찾기 (힌트용)
     */
    fun findBestMove(state: GameState): Move? {
        val result = solve(state)
        return if (result is SolverResult.Success && result.moves.isNotEmpty()) {
            result.moves.first()
        } else {
            null
        }
    }
    
    /**
     * 현재 상태에서 가능한 모든 이동 생성
     */
    private fun getAllPossibleMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        // 우선순위: Foundation 이동 > Tableau 이동 > Draw
        
        // 1. Tableau → Foundation
        for (col in 0..6) {
            for (f in 0..3) {
                if (engine.canMoveTableauToFoundation(col, f)) {
                    moves.add(Move.TableauToFoundation(col, f))
                }
            }
        }
        
        // 2. Waste → Foundation
        for (f in 0..3) {
            if (engine.canMoveWasteToFoundation(f)) {
                moves.add(Move.WasteToFoundation(f))
            }
        }
        
        // 3. Waste → Tableau
        for (col in 0..6) {
            if (engine.canMoveWasteToTableau(col)) {
                moves.add(Move.WasteToTableau(col))
            }
        }
        
        // 4. Tableau → Tableau (모든 가능한 카드 인덱스)
        for (fromCol in 0..6) {
            val pile = state.tableau[fromCol]
            if (pile.isEmpty()) continue
            
            // 앞면 카드부터 시작
            val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIndex == -1) continue
            
            for (cardIndex in firstFaceUpIndex until pile.size) {
                for (toCol in 0..6) {
                    if (fromCol != toCol) {
                        // 실제로 이동 가능한지 체크 (GameEngine 활용)
                        if (canMoveTableauToTableauFromIndex(state, fromCol, cardIndex, toCol)) {
                            moves.add(Move.TableauToTableau(fromCol, cardIndex, toCol))
                        }
                    }
                }
            }
        }
        
        // 5. Foundation → Tableau (규칙에 따라)
        if (engine.getRules().allowFoundationToTableau) {
            for (f in 0..3) {
                for (col in 0..6) {
                    if (engine.canMoveFoundationToTableau(f, col)) {
                        moves.add(Move.FoundationToTableau(f, col))
                    }
                }
            }
        }
        
        // 6. Draw (항상 가능하면 포함)
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            moves.add(Move.Draw)
        }
        
        return moves
    }
    
    /**
     * Tableau → Tableau 이동 가능 여부 (특정 카드 인덱스부터)
     */
    private fun canMoveTableauToTableauFromIndex(
        state: GameState, 
        fromCol: Int, 
        cardIndex: Int, 
        toCol: Int
    ): Boolean {
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return false
        
        val src = state.tableau[fromCol]
        val dst = state.tableau[toCol]
        
        if (cardIndex < 0 || cardIndex >= src.size) return false
        
        val partialPile = src.subList(cardIndex, src.size)
        val movableSequence = engine.rulesEngine.getMovableSequence(partialPile)
        
        if (movableSequence.isEmpty()) return false
        
        return engine.rulesEngine.canMoveSequenceToTableau(movableSequence, dst)
    }
}
