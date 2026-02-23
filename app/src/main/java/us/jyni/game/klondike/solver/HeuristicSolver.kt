package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.util.sync.Ruleset
import java.util.PriorityQueue

/**
 * 휴리스틱 기반 Solver (도메인 지식 활용)
 * 
 * 핵심 전략:
 * 1. Face-down 카드를 뒤집는 이동 우선 (가장 중요!)
 * 2. Foundation으로 올릴 수 있으면 바로 올리기
 * 3. 깊은 pile(카드 많은) 우선 정리
 * 4. 빈 공간은 King이 있을 때만 만들기
 * 5. 우측 pile 우선 (더 많은 face-down 카드)
 */
class HeuristicSolver(private val rules: Ruleset = Ruleset()) : Solver {
    
    private val rulesEngine = KlondikeRules()
    
    override fun findBestMove(state: GameState): Move? {
        val moves = generatePrioritizedMoves(state)
        return moves.firstOrNull()?.move
    }
    
    private data class ScoredMove(
        val move: Move,
        val score: Int
    ) : Comparable<ScoredMove> {
        override fun compareTo(other: ScoredMove): Int = other.score.compareTo(this.score)
    }
    
    private data class SearchNode(
        val state: GameState,
        val path: List<Move>,
        val score: Int
    ) : Comparable<SearchNode> {
        override fun compareTo(other: SearchNode): Int = other.score.compareTo(this.score)
    }
    
    companion object {
        private const val MAX_DEPTH = 100
        private const val MAX_STATES = 200000 // BFS보다 많이
        private const val TIMEOUT_MS = 20000L
        
        // 점수 가중치
        private const val FLIP_CARD_BONUS = 100        // Face-down 뒤집기
        private const val TO_FOUNDATION_BONUS = 50     // Foundation으로
        private const val DEEP_PILE_BONUS = 10         // 깊은 pile 정리
        private const val RIGHT_PILE_BONUS = 5         // 우측 pile 우선
        private const val EMPTY_COLUMN_PENALTY = -30   // 빈 공간 만들기 (King 없으면)
        private const val CARDS_IN_FOUNDATION = 20     // Foundation의 카드 수
    }
    
    override fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        
        // 우선순위 큐 (점수가 높은 노드 우선)
        val queue = PriorityQueue<SearchNode>()
        val visited = mutableSetOf<String>()
        
        val initialScore = evaluateState(initialState)
        queue.add(SearchNode(initialState, emptyList(), initialScore))
        visited.add(GameStateUtils.stateHash(initialState))
        
        var statesExplored = 0
        var bestScore = initialScore
        
        while (queue.isNotEmpty()) {
            // 타임아웃 체크
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return SolverResult.Timeout("탐색 시간 초과 (${TIMEOUT_MS}ms)")
            }
            
            // 상태 수 제한
            if (statesExplored >= MAX_STATES) {
                return SolverResult.TooComplex("탐색 상태 수 초과 (" + MAX_STATES + "개)")
            }
            
            val node = queue.poll()
            statesExplored++
            
            // 진행 상황 추적
            if (node.score > bestScore) {
                bestScore = node.score
            }
            
            // 승리 확인
            if (rulesEngine.isGameWon(node.state)) {
                return SolverResult.Success(node.path, statesExplored)
            }
            
            // 깊이 제한
            if (node.path.size >= MAX_DEPTH) {
                continue
            }
            
            // Unsolvable 체크
            val detector = UnsolvableDetector()
            val unsolvableReason = detector.check(node.state)
            if (unsolvableReason != null) {
                continue // 이 경로는 막힘
            }
            
            // 우선순위가 높은 이동 생성
            val moves = generatePrioritizedMoves(node.state)
            
            for (scoredMove in moves) {
                val nextState = GameStateUtils.applyMove(node.state, scoredMove.move)
                if (nextState == null) continue
                
                val hash = GameStateUtils.stateHash(nextState)
                if (hash in visited) continue
                
                visited.add(hash)
                val newScore = evaluateState(nextState)
                
                queue.add(SearchNode(
                    state = nextState,
                    path = node.path + scoredMove.move,
                    score = newScore
                ))
            }
        }
        
        return SolverResult.InherentlyUnsolvable(
            UnsolvableReason.DeadEnd("모든 경로 탐색 완료 - 해결 불가")
        )
    }
    
    /**
     * 상태 평가 (점수가 높을수록 좋은 상태)
     */
    private fun evaluateState(state: GameState): Int {
        var score = 0
        
        // 1. Foundation의 카드 수 (가장 중요)
        score += state.foundation.sumOf { it.size } * CARDS_IN_FOUNDATION
        
        // 2. Face-up 카드 수 (많을수록 좋음)
        val faceUpCount = state.tableau.sumOf { pile -> 
            pile.count { it.isFaceUp } 
        }
        score += faceUpCount * 5
        
        // 3. 빈 Tableau 열 (King이 있으면 보너스)
        val emptyColumns = state.tableau.count { it.isEmpty() }
        val hasKing = state.tableau.any { pile -> 
            pile.any { it.rank == Rank.KING && it.isFaceUp }
        }
        if (emptyColumns > 0 && hasKing) {
            score += emptyColumns * 20
        }
        
        // 4. Tableau 정렬도 (순서대로 쌓여있으면 보너스)
        state.tableau.forEach { pile ->
            if (pile.size >= 2) {
                var orderedCount = 0
                for (i in 1 until pile.size) {
                    if (pile[i].isFaceUp && pile[i-1].isFaceUp) {
                        if (pile[i].rank.ordinal == pile[i-1].rank.ordinal - 1) {
                            orderedCount++
                        }
                    }
                }
                score += orderedCount * 3
            }
        }
        
        // 5. Stock/Waste 페널티 (많이 남아있으면 감점)
        score -= state.stock.size * 2
        score -= state.waste.size * 1
        
        return score
    }
    
    /**
     * 도메인 지식 기반 우선순위 이동 생성
     */
    private fun generatePrioritizedMoves(state: GameState): List<ScoredMove> {
        val moves = mutableListOf<ScoredMove>()
        
        // 1. Tableau → Foundation (항상 최우선!)
        for (t in state.tableau.indices) {
            val pile = state.tableau[t]
            if (pile.isEmpty()) continue
            
            for (f in 0..3) {
                if (rulesEngine.canMoveTableauToFoundation(pile, state.foundation[f])) {
                    moves.add(ScoredMove(
                        Move.TableauToFoundation(t, f),
                        TO_FOUNDATION_BONUS
                    ))
                }
            }
        }
        
        // 2. Waste → Foundation
        if (state.waste.isNotEmpty()) {
            for (f in 0..3) {
                if (rulesEngine.canMoveTableauToFoundation(listOf(state.waste.last()), state.foundation[f])) {
                    moves.add(ScoredMove(
                        Move.WasteToFoundation(f),
                        TO_FOUNDATION_BONUS
                    ))
                }
            }
        }
        
        // 3. Tableau → Tableau (Face-down 카드 뒤집기 우선!)
        for (from in state.tableau.indices) {
            if (state.tableau[from].isEmpty()) continue
            
            val fromPile = state.tableau[from]
            val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue
            
            // 의미있는 분할점: bottom + top + King
            val significantIndices = mutableSetOf<Int>()
            significantIndices.add(firstFaceUpIdx)  // 1. 맨 아래 (뒷면 노출용)
            significantIndices.add(fromPile.lastIndex)  // 2. 맨 위 (연결용)
            for (i in firstFaceUpIdx until fromPile.size) {  // 3. King (빈 칸용)
                if (fromPile[i].rank.value == 13) {
                    significantIndices.add(i)
                }
            }
            
            for (cardIdx in significantIndices) {
                for (to in state.tableau.indices) {
                    if (from == to) continue
                    
                    val movingCards = fromPile.drop(cardIdx)
                    if (rulesEngine.canMoveTableauToTableau(movingCards, state.tableau[to])) {
                        var score = 0
                        
                        // Face-down 카드를 뒤집을 수 있으면 큰 보너스!
                        val willFlipCard = cardIdx > 0 && !fromPile[cardIdx - 1].isFaceUp
                        if (willFlipCard) {
                            score += FLIP_CARD_BONUS
                        }
                        
                        // 깊은 pile 정리 보너스
                        score += fromPile.size * DEEP_PILE_BONUS
                        
                        // 우측 pile 우선 (보통 더 많은 face-down 카드)
                        score += from * RIGHT_PILE_BONUS
                        
                        // 빈 공간 만들기는 King이 있을 때만
                        if (cardIdx == 0 && state.tableau[to].isEmpty()) {
                            val hasKingInOtherPiles = state.tableau.indices
                                .filter { it != from }
                                .any { pile ->
                                    state.tableau[pile].any { it.rank == Rank.KING && it.isFaceUp }
                                }
                            if (!hasKingInOtherPiles) {
                                score += EMPTY_COLUMN_PENALTY
                            }
                        }
                        
                        moves.add(ScoredMove(
                            Move.TableauToTableau(from, cardIdx, to),
                            score
                        ))
                    }
                }
            }
        }
        
        // 4. Waste → Tableau
        if (state.waste.isNotEmpty()) {
            val wasteCard = state.waste.last()
            for (t in state.tableau.indices) {
                if (rulesEngine.canMoveTableauToTableau(listOf(wasteCard), state.tableau[t])) {
                    moves.add(ScoredMove(
                        Move.WasteToTableau(t),
                        30 // 중간 우선순위
                    ))
                }
            }
        }
        
        // 5. Foundation → Tableau (필요한 경우만)
        if (rules.allowFoundationToTableau) {
            for (f in 0..3) {
                if (state.foundation[f].isEmpty()) continue
                
                for (t in state.tableau.indices) {
                    if (rulesEngine.canMoveFoundationToTableau(state.foundation[f], state.tableau[t])) {
                        moves.add(ScoredMove(
                            Move.FoundationToTableau(f, t),
                            -10 // 낮은 우선순위 (일반적으로 좋지 않음)
                        ))
                    }
                }
            }
        }
        
        // 6. Draw (다른 이동이 없을 때)
        if (state.stock.isNotEmpty()) {
            moves.add(ScoredMove(
                Move.Draw,
                10 // 낮은 우선순위
            ))
        }
        
        // 점수 순으로 정렬
        return moves.sortedByDescending { it.score }
    }
}
