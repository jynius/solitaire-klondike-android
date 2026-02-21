package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import java.util.PriorityQueue

/**
 * A* 알고리즘 기반 솔리테어 Solver
 * 제약 기반 휴리스틱을 사용하여 효율적으로 승리 경로 탐색
 */
class AStarSolver(private val engine: GameEngine) : Solver {
    
    companion object {
        private const val MAX_DEPTH = 150
        private const val MAX_STATES = 100000
        private const val TIMEOUT_MS = 5000L
    }
    
    /**
     * 현재 상태에서 승리까지의 경로 찾기
     */
    override fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        
        // Inherently Unsolvable 체크 (게임 시작 시)
        val unsolvableDetector = UnsolvableDetector(engine)
        val inherentlyUnsolvable = unsolvableDetector.checkInherentlyUnsolvable(initialState)
        if (inherentlyUnsolvable != null) {
            return SolverResult.InherentlyUnsolvable(inherentlyUnsolvable)
        }
        
        // Priority Queue: f(n) = g(n) + h(n) 기준 정렬
        val openSet = PriorityQueue<SearchNode>(compareBy { it.fCost })
        val closedSet = mutableSetOf<String>()
        
        val initialNode = SearchNode(
            state = initialState,
            gCost = 0,
            hCost = heuristic(initialState),
            path = emptyList()
        )
        
        openSet.add(initialNode)
        var statesExplored = 0
        
        while (openSet.isNotEmpty()) {
            // 타임아웃 체크
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return SolverResult.Timeout("탐색 시간 초과 (${statesExplored}개 상태 탐색)")
            }
            
            val node = openSet.poll()
            statesExplored++
            
            // 승리 조건 체크
            if (node.state.isGameOver) {
                return SolverResult.Success(
                    moves = node.path,
                    statesExplored = statesExplored
                )
            }
            
            // 이미 방문한 상태면 스킵
            val hash = GameStateUtils.stateHash(node.state)
            if (hash in closedSet) continue
            closedSet.add(hash)
            
            // 깊이 제한
            if (node.path.size >= MAX_DEPTH) continue
            
            // 상태 수 제한
            if (statesExplored >= MAX_STATES) {
                return SolverResult.TooComplex("탐색 상태 수 초과 (${MAX_STATES}개)")
            }
            
            // 우선순위 기반 이동 생성
            val moves = getPrioritizedMoves(node.state)
            
            for (move in moves) {
                val newState = GameStateUtils.applyMove(node.state, move) ?: continue
                val newHash = GameStateUtils.stateHash(newState)
                
                if (newHash !in closedSet) {
                    val newNode = SearchNode(
                        state = newState,
                        gCost = node.gCost + 1,
                        hCost = heuristic(newState),
                        path = node.path + move
                    )
                    openSet.add(newNode)
                }
            }
        }
        
        // openSet이 비었음 = 더 이상 탐색할 상태가 없음
        // 하지만 이것이 정말 "불가능"한지 확인 필요
        // 현재 상태에서 가능한 이동이 있으면 TooComplex, 없으면 Unsolvable
        val currentState = if (closedSet.isNotEmpty()) {
            // 마지막으로 탐색한 상태 확인 (실제로는 초기 상태에서 확인)
            initialState
        } else {
            initialState
        }
        
        val possibleMoves = getPrioritizedMoves(currentState)
        return if (possibleMoves.isEmpty()) {
            SolverResult.TooComplex("모든 경로 탐색 완료 - 승리 불가능 (${statesExplored}개 상태 탐색)")
        } else {
            SolverResult.TooComplex("경로를 찾을 수 없음 - 게임은 계속 가능 (${statesExplored}개 상태 탐색)")
        }
    }
    
    /**
     * 최선의 다음 이동 찾기 (힌트용)
     */
    override fun findBestMove(state: GameState): Move? {
        // 간단한 이동부터 시도
        val simpleMoves = getPrioritizedMoves(state)
        
        if (simpleMoves.isNotEmpty()) {
            // 첫 번째 유효한 이동 반환 (Solver 실행 없이)
            for (move in simpleMoves) {
                val newState = GameStateUtils.applyMove(state, move)
                if (newState != null) {
                    return move
                }
            }
        }
        
        return null
    }
    
    /**
     * 제약 기반 휴리스틱 함수
     * 낮을수록 좋은 상태 (승리에 가까움)
     */
    private fun heuristic(state: GameState): Int {
        var cost = 0
        
        // 1. Foundation에 올라가지 않은 카드 수 (핵심 목표)
        val inFoundation = state.foundation.sumOf { it.size }
        cost += (52 - inFoundation) * 10
        
        // 2. Tableau의 블로킹 비용 (위에 카드가 많을수록 비용 증가)
        for (pile in state.tableau) {
            for (i in pile.indices) {
                val card = pile[i]
                // Foundation에 올라갈 수 있는 카드가 막혀있으면 비용 증가
                if (canGoToFoundation(card, state.foundation)) {
                    val blockingCount = pile.size - i - 1
                    cost += blockingCount * 5
                }
            }
        }
        
        // 3. Stock/Waste에 남은 카드
        cost += (state.stock.size + state.waste.size) * 2
        
        // 4. 빈 Tableau 컬럼 평가
        val emptyCount = state.tableau.count { it.isEmpty() }
        cost += when {
            emptyCount == 0 && hasKingInTableau(state) -> 15  // King이 있는데 빈 공간 없음
            emptyCount > 3 -> 10  // 너무 많은 빈 공간
            else -> 0
        }
        
        // 5. 뒤집히지 않은 카드 (앞면으로 만들어야 함)
        val faceDownCount = state.tableau.sumOf { pile ->
            pile.count { !it.isFaceUp }
        }
        cost += faceDownCount * 3
        
        return cost
    }
    
    /**
     * 카드가 현재 Foundation에 올라갈 수 있는지 확인
     */
    private fun canGoToFoundation(card: Card, foundation: List<List<Card>>): Boolean {
        for (pile in foundation) {
            if (pile.isEmpty() && card.rank == Rank.ACE) return true
            if (pile.isNotEmpty()) {
                val top = pile.last()
                if (card.suit == top.suit && card.rank.value == top.rank.value + 1) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Tableau에 King이 있는지 확인
     */
    private fun hasKingInTableau(state: GameState): Boolean {
        return state.tableau.any { pile ->
            pile.any { it.rank == Rank.KING }
        }
    }
    
    /**
     * 우선순위 기반 이동 생성
     */
    private fun getPrioritizedMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        // 우선순위 1: 안전한 Foundation 이동
        val foundationMoves = getSafeFoundationMoves(state)
        moves.addAll(foundationMoves)
        
        // 우선순위 2: 카드를 뒤집는 이동
        val flipMoves = getFlipMoves(state)
        moves.addAll(flipMoves)
        
        // 우선순위 3: King을 빈 공간으로
        val kingMoves = getKingToEmptyMoves(state)
        moves.addAll(kingMoves)
        
        // 우선순위 4: 유용한 Tableau 이동
        val tableauMoves = getProductiveTableauMoves(state)
        moves.addAll(tableauMoves)
        
        // 우선순위 5: Waste에서 이동
        val wasteMoves = getWasteMoves(state)
        moves.addAll(wasteMoves)
        
        // 우선순위 6: Draw (Stock이 비었으면 Waste 재활용 포함)
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            moves.add(Move.Draw)
        }
        
        return moves
    }
    
    /**
     * 안전한 Foundation 이동
     * (두 랭크 이상 낮은 반대 색상 카드가 이미 올라갔으면 안전)
     */
    private fun getSafeFoundationMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        // Tableau → Foundation
        for (col in 0..6) {
            val pile = state.tableau[col]
            if (pile.isEmpty()) continue
            
            val card = pile.last()
            for (f in 0..3) {
                val fnd = state.foundation[f]
                if (canPlaceOnFoundation(card, fnd) && isSafeToMoveToFoundation(card, state.foundation)) {
                    moves.add(Move.TableauToFoundation(col, f))
                }
            }
        }
        
        // Waste → Foundation
        if (state.waste.isNotEmpty()) {
            val card = state.waste.last()
            for (f in 0..3) {
                val fnd = state.foundation[f]
                if (canPlaceOnFoundation(card, fnd) && isSafeToMoveToFoundation(card, state.foundation)) {
                    moves.add(Move.WasteToFoundation(f))
                }
            }
        }
        
        return moves
    }
    
    /**
     * Foundation으로 이동이 안전한지 확인
     */
    private fun isSafeToMoveToFoundation(card: Card, foundation: List<List<Card>>): Boolean {
        // Ace, 2, 3은 항상 안전 (게임 초반이므로)
        if (card.rank.value <= 3) return true
        
        // 반대 색상의 낮은 랭크 카드들이 이미 올라갔는지 확인
        val cardIsRed = card.suit.isRed()
        
        for (pile in foundation) {
            if (pile.isEmpty()) continue
            val top = pile.last()
            if (top.suit.isRed() != cardIsRed) {
                // 반대 색상이 2 이상 낮으면 안전
                if (top.rank.value >= card.rank.value - 2) {
                    return true
                }
            }
        }
        
        // 반대 색상 카드가 충분히 올라가지 않았으면 불안전
        return false
    }
    
    /**
     * 뒤집기 이동 (뒷면 카드를 노출)
     */
    private fun getFlipMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        for (fromCol in 0..6) {
            val pile = state.tableau[fromCol]
            if (pile.isEmpty()) continue
            
            // 뒷면 카드가 있고, 그 위에 앞면 카드가 있는 경우
            val faceDownIndex = pile.indexOfFirst { !it.isFaceUp }
            if (faceDownIndex != -1 && faceDownIndex < pile.lastIndex) {
                // 위의 카드를 이동시키면 뒷면 카드 노출
                val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
                if (firstFaceUpIndex != -1) {
                    for (toCol in 0..6) {
                        if (fromCol != toCol) {
                            val dst = state.tableau[toCol]
                            val card = pile[firstFaceUpIndex]
                            if (canPlaceOnTableau(card, dst)) {
                                moves.add(Move.TableauToTableau(fromCol, firstFaceUpIndex, toCol))
                            }
                        }
                    }
                }
            }
        }
        
        return moves
    }
    
    /**
     * King을 빈 공간으로 이동
     */
    private fun getKingToEmptyMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        val emptyCol = state.tableau.indexOfFirst { it.isEmpty() }
        if (emptyCol == -1) return moves
        
        // Tableau에서 King 찾기
        for (fromCol in 0..6) {
            val pile = state.tableau[fromCol]
            if (pile.isEmpty()) continue
            
            val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIndex != -1) {
                val card = pile[firstFaceUpIndex]
                if (card.rank == Rank.KING && firstFaceUpIndex > 0) {
                    // King이 첫 카드가 아니면 빈 공간으로 이동 (뒷면 카드 노출)
                    moves.add(Move.TableauToTableau(fromCol, firstFaceUpIndex, emptyCol))
                }
            }
        }
        
        return moves
    }
    
    /**
     * 유용한 Tableau 이동
     */
    private fun getProductiveTableauMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        for (fromCol in 0..6) {
            val pile = state.tableau[fromCol]
            
            if (pile.isEmpty()) continue
            
            val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIndex == -1) {
                continue
            }
            
            for (cardIndex in firstFaceUpIndex until pile.size) {
                val card = pile[cardIndex]
                for (toCol in 0..6) {
                    if (fromCol != toCol) {
                        val dst = state.tableau[toCol]
                        
                        if (canPlaceOnTableau(card, dst)) {
                            moves.add(Move.TableauToTableau(fromCol, cardIndex, toCol))
                        }
                    }
                }
            }
        }
        
        return moves
    }
    
    /**
     * Waste에서 이동
     */
    private fun getWasteMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        if (state.waste.isEmpty()) return moves
        
        val card = state.waste.last()
        
        // Waste → Tableau
        for (toCol in 0..6) {
            val dst = state.tableau[toCol]
            if (canPlaceOnTableau(card, dst)) {
                moves.add(Move.WasteToTableau(toCol))
            }
        }
        
        return moves
    }
    
    private fun canPlaceOnTableau(card: Card, tableau: List<Card>): Boolean {
        if (tableau.isEmpty()) {
            return card.rank == Rank.KING
        }
        
        // 마지막 앞면 카드 찾기
        val target = tableau.lastOrNull { it.isFaceUp }
        if (target == null) {
            // 모두 뒷면이면 놓을 수 없음
            return false
        }
        
        val oppositeColor = card.suit.isRed() != target.suit.isRed()
        val oneRankLower = card.rank.value == target.rank.value - 1
        
        return oppositeColor && oneRankLower
    }
    
    private fun canPlaceOnFoundation(card: Card, foundation: List<Card>): Boolean {
        if (foundation.isEmpty()) {
            return card.rank == Rank.ACE
        }
        
        val top = foundation.last()
        return card.suit == top.suit && card.rank.value == top.rank.value + 1
    }
}
