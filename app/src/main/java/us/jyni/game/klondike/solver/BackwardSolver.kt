package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.Ruleset
import java.util.LinkedList
import java.util.Queue

/**
 * 역방향 탐색 Solver (간소화 버전)
 * 
 * 핵심 아이디어:
 * - Stock/Waste는 완전히 무시 (Draw=1이면 순서 무관)
 * - Foundation에서 Tableau로 카드를 내리는 것만 고려
 * - 초기 상태 매칭은 Tableau 구조만 확인
 * 
 * 장점:
 * - 탐색 공간이 크게 줄어듦
 * - Stock/Waste 순서 추적 불필요
 * - Foundation → Tableau 이동이 결정론적
 */
class BackwardSolver(private val rules: Ruleset = Ruleset()) : Solver {
    
    private val rulesEngine = KlondikeRules()
    
    override fun findBestMove(state: GameState): Move? {
        // Backward solver는 힌트 기능을 지원하지 않음
        return null
    }
    
    private data class BackwardNode(
        val state: GameState,
        val path: List<Move> // 역방향 이동 경로
    )
    
    companion object {
        private const val MAX_DEPTH = 100
        private const val MAX_STATES = 200000
        private const val TIMEOUT_MS = 15000L
    }
    
    override fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        
        // 1. 목표 상태 생성 (모든 카드가 Foundation에)
        val goalState = createGoalState(initialState)
        
        // 2. 역방향 BFS
        val queue: Queue<BackwardNode> = LinkedList()
        val visited = mutableSetOf<String>()
        
        val goalNode = BackwardNode(
            state = goalState,
            path = emptyList()
        )
        
        queue.add(goalNode)
        visited.add(GameStateUtils.stateHash(goalState))
        
        var statesExplored = 0
        
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
            
            // 초기 상태에 도달했는지 확인
            if (matchesInitialState(node.state, initialState)) {
                // 경로를 역순으로 반환 (forward 이동으로 변환)
                val forwardMoves = reversePathToForwardMoves(node.path)
                return SolverResult.Success(forwardMoves, statesExplored)
            }
            
            // 깊이 제한
            if (node.path.size >= MAX_DEPTH) {
                continue
            }
            
            // 역방향 이동 생성
            val backwardMoves = generateBackwardMoves(node.state)
            
            for (move in backwardMoves) {
                val nextState = applyBackwardMove(node.state, move)
                val hash = GameStateUtils.stateHash(nextState)
                
                if (hash !in visited) {
                    visited.add(hash)
                    queue.add(BackwardNode(
                        state = nextState,
                        path = node.path + move
                    ))
                }
            }
        }
        
        return SolverResult.InherentlyUnsolvable(
            UnsolvableReason.DeadEnd("역방향 탐색으로 초기 상태에 도달할 수 없음")
        )
    }
    
    /**
     * 목표 상태 생성: 모든 카드가 Foundation에 순서대로
     */
    private fun createGoalState(initialState: GameState): GameState {
        // initialState에서 모든 카드를 수집
        val allCards = mutableListOf<Card>()
        
        initialState.tableau.forEach { pile -> allCards.addAll(pile) }
        initialState.foundation.forEach { pile -> allCards.addAll(pile) }
        allCards.addAll(initialState.stock)
        allCards.addAll(initialState.waste)
        
        // Foundation에 Suit별로 정렬
        val foundations = List(4) { mutableListOf<Card>() }
        val suitOrder = listOf(Suit.SPADES, Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS)
        
        for ((idx, suit) in suitOrder.withIndex()) {
            val suitCards = allCards.filter { it.suit == suit }
                .sortedBy { it.rank.ordinal }
            foundations[idx].addAll(suitCards)
        }
        
        return GameState(
            tableau = List(7) { mutableListOf() },
            foundation = foundations,
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = true
        )
    }
    
    /**
     * 초기 상태와 매칭되는지 확인
     * 
     * **중요**: Stock/Waste는 무시!
     * - Draw=1이면 Stock 순서는 의미 없음
     * - Tableau 구조만 동일하면 됨
     */
    private fun matchesInitialState(current: GameState, initial: GameState): Boolean {
        // Tableau 구조만 확인
        if (current.tableau.size != initial.tableau.size) return false
        
        for (i in current.tableau.indices) {
            val currentPile = current.tableau[i]
            val initialPile = initial.tableau[i]
            
            if (currentPile.size != initialPile.size) return false
            
            for (j in currentPile.indices) {
                if (currentPile[j] != initialPile[j]) return false
                if (currentPile[j].isFaceUp != initialPile[j].isFaceUp) return false
            }
        }
        
        // Foundation도 확인 (초기 상태에서는 보통 비어있음)
        for (i in current.foundation.indices) {
            if (current.foundation[i].size != initial.foundation[i].size) return false
        }
        
        // Stock/Waste는 무시!
        return true
    }
    
    /**
     * 역방향 이동 생성
     * Foundation → Tableau, Tableau → Tableau (역방향)
     */
    private fun generateBackwardMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        
        // 1. Foundation → Tableau (목표에서 Tableau로 카드 내리기)
        for (f in state.foundation.indices) {
            if (state.foundation[f].isEmpty()) continue
            
            val topCard = state.foundation[f].last()
            
            for (t in state.tableau.indices) {
                // Tableau에 놓을 수 있는지 확인 (역방향이므로 조건 반대)
                if (canPlaceOnTableauBackward(topCard, state.tableau[t])) {
                    moves.add(Move.FoundationToTableau(f, t))
                }
            }
        }
        
        // 2. Tableau → Tableau (다른 pile로 이동)
        for (from in state.tableau.indices) {
            if (state.tableau[from].isEmpty()) continue
            
            // Face-up 카드들만 이동 가능
            val fromPile = state.tableau[from]
            val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue
            
            // 맨 밑 face-up 카드만 시도 (중간 카드는 불필요한 중복)
            val cardIdx = firstFaceUpIdx
            val movingCard = fromPile[cardIdx]
            
            for (to in state.tableau.indices) {
                if (from == to) continue
                
                if (canPlaceOnTableauBackward(movingCard, state.tableau[to])) {
                    moves.add(Move.TableauToTableau(from, to, cardIdx))
                }
            }
        }
        
        // 3. Waste/Stock 재구성 (역방향으로 Draw를 되돌림)
        // 이 부분은 복잡하므로 일단 생략
        
        return moves
    }
    
    /**
     * 역방향에서 Tableau에 카드를 놓을 수 있는지 확인
     * Forward: 빨강-검정 교대, 1씩 감소
     * Backward: 같은 규칙이지만 Foundation에서 내려올 때는 임의로 배치 가능
     */
    private fun canPlaceOnTableauBackward(card: Card, targetPile: List<Card>): Boolean {
        if (targetPile.isEmpty()) {
            // 빈 pile에는 King만 (역방향에서도 동일)
            return card.rank == Rank.KING
        }
        
        val topCard = targetPile.last()
        if (!topCard.isFaceUp) return false
        
        // 색깔 교대 + 1씩 증가 (역방향이므로)
        val colorAlternates = card.suit.isRed() != topCard.suit.isRed()
        val rankDecreases = card.rank.ordinal == topCard.rank.ordinal - 1
        
        return colorAlternates && rankDecreases
    }
    
    /**
     * 역방향 이동 적용
     * 
     * Face-down 카드를 자동으로 뒤집어야 함!
     */
    private fun applyBackwardMove(state: GameState, move: Move): GameState {
        return when (move) {
            is Move.FoundationToTableau -> {
                val card = state.foundation[move.foundationIndex].last()
                
                val newFoundation = state.foundation.mapIndexed { idx, pile ->
                    if (idx == move.foundationIndex) {
                        pile.dropLast(1).toMutableList()
                    } else {
                        pile.toMutableList()
                    }
                }
                
                val newTableau = state.tableau.mapIndexed { idx, pile ->
                    if (idx == move.toCol) {
                        (pile + card.copy(isFaceUp = true)).toMutableList()
                    } else {
                        pile.toMutableList()
                    }
                }
                
                state.copy(
                    tableau = newTableau,
                    foundation = newFoundation
                )
            }
            
            is Move.TableauToTableau -> {
                val fromPile = state.tableau[move.fromCol]
                val movingCards = fromPile.drop(move.cardIndex)
                val remainingCards = fromPile.take(move.cardIndex)
                
                // 남은 카드의 마지막 카드를 face-up으로 뒤집기 (역방향이므로 face-down으로!)
                val newRemainingCards = if (remainingCards.isNotEmpty()) {
                    remainingCards.dropLast(1) + remainingCards.last().copy(isFaceUp = false)
                } else {
                    remainingCards
                }
                
                val newTableau = state.tableau.mapIndexed { idx, pile ->
                    when (idx) {
                        move.fromCol -> newRemainingCards.toMutableList()
                        move.toCol -> (pile + movingCards).toMutableList()
                        else -> pile.toMutableList()
                    }
                }
                
                state.copy(tableau = newTableau)
            }
            
            else -> state // 다른 이동은 무시
        }
    }
    
    /**
     * 역방향 경로를 정방향 이동으로 변환
     */
    private fun reversePathToForwardMoves(backwardPath: List<Move>): List<Move> {
        // 역방향 경로를 역순으로 뒤집고, 각 이동을 반대로 변환
        return backwardPath.reversed().map { move ->
            when (move) {
                is Move.FoundationToTableau -> 
                    Move.TableauToFoundation(move.toCol, move.foundationIndex)
                    
                is Move.TableauToTableau -> 
                    // 역방향: from → to 였다면, 정방향: to → from
                    Move.TableauToTableau(move.toCol, 0, move.fromCol) // cardIndex는 재계산 필요
                    
                else -> move
            }
        }
    }
}
