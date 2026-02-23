package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.sync.Ruleset
import java.util.LinkedList
import java.util.Queue

/**
 * BFS 기반 솔리테어 Solver
 * 
 * 완전 정보 게임 탐색을 통해 승리 경로를 찾습니다.
 * GameEngine 없이 독립적으로 동작합니다.
 */
class BFSSolver(private val rules: Ruleset = Ruleset()) : Solver {
    
    private val rulesEngine = KlondikeRules()
    
    /**
     * BFS 전용 탐색 노드
     */
    private data class BFSNode(
        val state: GameState,
        val path: List<Move>
    )
    
    companion object {
        private const val MAX_DEPTH = 200
        private const val MAX_STATES = 200000
        private const val TIMEOUT_MS = 30000L
    }
    
    /**
     * 현재 상태에서 승리까지의 경로 찾기
     * @return SolverResult (승리 경로 또는 실패 이유)
     */
    override fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        
        val queue: Queue<BFSNode> = LinkedList()
        val visited = mutableSetOf<String>()
        
        val initialNode = BFSNode(
            state = initialState,
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
                    
                    val newNode = BFSNode(
                        state = newState,
                        path = node.path + move
                    )
                    
                    queue.add(newNode)
                }
            }
        }
        
        return SolverResult.TooComplex("모든 경로 탐색 완료 - 승리 불가능 (${statesExplored}개 상태 탐색)")
    }
    
    /**
     * 최선의 다음 이동 찾기 (힌트용)
     */
    override fun findBestMove(state: GameState): Move? {
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
        // 각 카드는 suit에 맞는 foundation 하나로만 이동 가능
        // 모든 foundation을 시도하면 중복 상태가 생김
        for (col in 0..6) {
            val pile = state.tableau[col]
            if (pile.isEmpty()) continue
            
            val topCard = pile.last()
            if (!topCard.isFaceUp) continue
            
            // Find any foundation that accepts this card
            for (f in 0..3) {
                if (rulesEngine.canMoveTableauToFoundation(pile, state.foundation[f])) {
                    moves.add(Move.TableauToFoundation(col, f))
                    break // Only add one foundation move per card
                }
            }
        }
        
        // 2. Waste → Foundation
        if (state.waste.isNotEmpty()) {
            // Find any foundation that accepts the waste card
            for (f in 0..3) {
                if (rulesEngine.canMoveTableauToFoundation(state.waste, state.foundation[f])) {
                    moves.add(Move.WasteToFoundation(f))
                    break // Only add one foundation move
                }
            }
        }
        
        // 3. Waste → Tableau
        for (col in 0..6) {
            if (rulesEngine.canMoveTableauToTableau(state.waste, state.tableau[col])) {
                moves.add(Move.WasteToTableau(col))
            }
        }
        
        // 4. Tableau → Tableau (의미있는 분할점만 시도)
        for (fromCol in 0..6) {
            val pile = state.tableau[fromCol]
            if (pile.isEmpty()) continue
            
            // 앞면 카드부터 시작
            val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIndex == -1) continue
            
            // 의미있는 분할점: bottom + top + King
            val significantIndices = mutableSetOf<Int>()
            significantIndices.add(firstFaceUpIndex)  // 1. 맨 아래 (뒷면 노출용)
            significantIndices.add(pile.lastIndex)     // 2. 맨 위 (연결용)
            for (i in firstFaceUpIndex until pile.size) {  // 3. King (빈 칸용)
                if (pile[i].rank.value == 13) {
                    significantIndices.add(i)
                }
            }
            
            for (cardIndex in significantIndices) {
                for (toCol in 0..6) {
                    if (fromCol != toCol) {
                        if (canMoveTableauToTableauFromIndex(state, fromCol, cardIndex, toCol)) {
                            moves.add(Move.TableauToTableau(fromCol, cardIndex, toCol))
                        }
                    }
                }
            }
        }
        
        // 5. Foundation → Tableau (규칙에 따라)
        if (rules.allowFoundationToTableau) {
            for (f in 0..3) {
                for (col in 0..6) {
                    if (rulesEngine.canMoveFoundationToTableau(state.foundation[f], state.tableau[col])) {
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
        val movableSequence = rulesEngine.getMovableSequence(partialPile)
        
        if (movableSequence.isEmpty()) return false
        
        return rulesEngine.canMoveSequenceToTableau(movableSequence, dst)
    }
}
