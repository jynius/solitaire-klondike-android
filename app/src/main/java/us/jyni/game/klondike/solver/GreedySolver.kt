package us.jyni.game.klondike.solver

import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * 규칙 기반 Greedy Solver
 * 
 * 전략:
 * 1. Foundation으로 옮길 수 있는 모든 카드를 먼저 옮김
 * 2. 우측 pile부터, 다른 카드 위로 옮길 수 있는지 확인
 * 3. 맨 밑바닥 카드는 다른 pile로 옮기지 않음
 * 4. K카드가 있으면: 맨 밑바닥 카드를 옮기고 K를 맨 밑바닥으로
 * 5. Waste 카드가 tableau 카드들을 연결할 수 있으면 옮김
 * 
 * 각 무브 후 처음부터 반복, 움직일 카드 없으면 Draw
 */
class GreedySolver(private val rules: Ruleset = Ruleset()) : Solver {
    
    private val rulesEngine = KlondikeRules()
    
    // For debugging: store last state when solver stops
    var lastState: GameState? = null
        private set
    
    companion object {
        private const val MAX_MOVES = 10000
        private const val TIMEOUT_MS = 30000L
    }
    
    override fun solve(initialState: GameState): SolverResult {
        val startTime = System.currentTimeMillis()
        var state = initialState
        val moves = mutableListOf<Move>()
        var noProgressCount = 0
        val visitedStates = mutableSetOf<Int>() // 방문한 상태 추적
        visitedStates.add(state.hashCode())
        
        while (!rulesEngine.isGameWon(state)) {
            // 타임아웃 체크
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return SolverResult.Timeout("시간 초과 (${moves.size} moves)")
            }
            
            // 무한 루프 방지 - 너무 많이 이동했다면 막힌 것
            if (moves.size >= MAX_MOVES) {
                lastState = state
                return SolverResult.InherentlyUnsolvable(
                    UnsolvableReason.DeadEnd("더 이상 진행 불가 (${moves.size} moves)")
                )
            }
            
            // 1. Foundation으로 옮길 수 있는 모든 카드를 먼저 옮김
            val foundationMove = findFoundationMove(state)
            if (foundationMove != null) {
                val newState = GameStateUtils.applyMove(state, foundationMove)
                if (newState != null) {
                    state = newState
                    moves.add(foundationMove)
                    noProgressCount = 0
                    visitedStates.add(state.hashCode())
                    continue
                }
            }
            
            // 2-4. Tableau 간 이동 (우측 pile 먼저)
            val tableauMove = findTableauMove(state)
            if (tableauMove != null) {
                val newState = GameStateUtils.applyMove(state, tableauMove)
                if (newState != null && !visitedStates.contains(newState.hashCode())) {
                    state = newState
                    moves.add(tableauMove)
                    noProgressCount = 0
                    visitedStates.add(state.hashCode())
                    continue
                }
            }
            
            // 5. Waste 카드로 tableau 카드들을 연결할 수 있는지
            val wasteMove = findWasteMove(state)
            if (wasteMove != null) {
                val newState = GameStateUtils.applyMove(state, wasteMove)
                if (newState != null && !visitedStates.contains(newState.hashCode())) {
                    state = newState
                    moves.add(wasteMove)
                    noProgressCount = 0
                    visitedStates.add(state.hashCode())
                    continue
                }
            }
            
            // 움직일 카드가 없으면 Draw
            if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
                val drawMove = Move.Draw
                val newState = GameStateUtils.applyMove(state, drawMove)
                if (newState != null) {
                    state = newState
                    moves.add(drawMove)
                    noProgressCount++
                    
                    // Stock을 한 바퀴 다 돌았는데도 진전 없으면 실패
                    if (noProgressCount > state.stock.size + state.waste.size + 10) {
                        lastState = state
                        return SolverResult.InherentlyUnsolvable(
                            UnsolvableReason.DeadEnd("더 이상 진행 불가 (${moves.size} moves)")
                        )
                    }
                    continue
                }
            }
            
            // 더 이상 할 수 있는 것이 없음
            lastState = state
            return SolverResult.InherentlyUnsolvable(
                UnsolvableReason.DeadEnd("막다른 길 (${moves.size} moves)")
            )
        }
        
        lastState = state
        return SolverResult.Success(moves, moves.size)
    }
    
    override fun findBestMove(state: GameState): Move? {
        // Foundation 이동이 최우선
        findFoundationMove(state)?.let { return it }
        
        // Tableau 이동
        findTableauMove(state)?.let { return it }
        
        // Waste 이동
        findWasteMove(state)?.let { return it }
        
        // Draw
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            return Move.Draw
        }
        
        return null
    }
    
    /**
     * 1. Foundation으로 옮길 수 있는 모든 카드 찾기
     */
    private fun findFoundationMove(state: GameState): Move? {
        // Tableau에서 Foundation으로
        for (col in 0..6) {
            val pile = state.tableau[col]
            if (pile.isNotEmpty()) {
                // canMoveTableauToFoundation이 이미 isFaceUp 체크함
                for (f in 0..3) {
                    if (rulesEngine.canMoveTableauToFoundation(pile, state.foundation[f])) {
                        return Move.TableauToFoundation(col, f)
                    }
                }
            }
        }
        
        // Waste에서 Foundation으로
        if (state.waste.isNotEmpty()) {
            for (f in 0..3) {
                if (rulesEngine.canMoveTableauToFoundation(state.waste, state.foundation[f])) {
                    return Move.WasteToFoundation(f)
                }
            }
        }
        
        return null
    }
    
    /**
     * 2-4. Tableau 간 이동
     * - 우측 pile 먼저
     * - 맨 밑바닥 카드는 다른 pile로 옮기지 않음
     * - 예외: 다른 pile에 K카드가 있으면 가능 (빈 칸에 K를 놓을 수 있음)
     */
    private fun findTableauMove(state: GameState): Move? {
        // 다른 pile들에 이동 가능한 K카드가 있는지 확인
        fun hasMovableKingInOtherPiles(excludeCol: Int): Boolean {
            return state.tableau.withIndex().any { (col, pile) ->
                col != excludeCol &&
                pile.isNotEmpty() &&
                pile.first().isFaceUp && pile.first().rank.value == 13
            }
        }
        
        // 우측(6)부터 좌측(0)으로
        for (fromCol in 6 downTo 0) {
            val fromPile = state.tableau[fromCol]
            if (fromPile.isEmpty()) continue
            
            val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue
            
            // 맨 밑 face-up 카드만 시도 (중간 카드는 시도하지 않음)
            val cardIdx = firstFaceUpIdx
            val movingCards = fromPile.subList(cardIdx, fromPile.size)
            
            // 규칙: 맨 밑바닥 카드는 다른 pile로 옮기지 않음
            // 예외: 다른 pile에 K카드가 있으면 가능 (빈 칸을 채울 수 있음)
            if (firstFaceUpIdx == 0) { // 맨 밑바닥인 경우
                val hasOtherKing = hasMovableKingInOtherPiles(fromCol)
                if (!hasOtherKing) {
                    continue // 다른 pile에 K가 없으면 맨 밑바닥은 못 옮김
                }
            }
            
            // 이동 가능한 pile 찾기
            for (toCol in 0..6) {
                if (fromCol == toCol) continue
                
                if (canMoveSequence(movingCards, state.tableau[toCol])) {
                    return Move.TableauToTableau(fromCol, cardIdx, toCol)
                }
            }
        }
        
        return null
    }
    
    /**
     * 5. Waste 카드가 tableau 카드들을 연결할 수 있는지
     */
    private fun findWasteMove(state: GameState): Move? {
        if (state.waste.isEmpty()) return null
        
        val wasteCard = state.waste.last()
        
        // Waste 카드를 각 tableau로 옮겼을 때
        // 다른 tableau 카드를 옮길 수 있게 되는지 확인
        for (targetCol in 0..6) {
            if (!canMoveCard(wasteCard, state.tableau[targetCol])) {
                continue
            }
            
            // Waste 카드를 targetCol에 놓으면
            // 다른 tableau 카드를 옮길 수 있게 되는가?
            for (fromCol in 0..6) {
                if (fromCol == targetCol) continue
                
                val fromPile = state.tableau[fromCol]
                if (fromPile.isEmpty()) continue
                
                val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
                if (firstFaceUpIdx == -1) continue
                
                for (cardIdx in firstFaceUpIdx until fromPile.size) {
                    val movingCards = fromPile.subList(cardIdx, fromPile.size)
                    val topCard = movingCards.first()
                    
                    // 현재는 옮길 수 없지만
                    if (canMoveCard(topCard, state.tableau[targetCol])) {
                        continue // 이미 옮길 수 있음
                    }
                    
                    // Waste 카드 위로 옮길 수 있는가?
                    if (canMoveCard(topCard, listOf(wasteCard))) {
                        // Waste 카드를 옮기면 이 카드를 옮길 수 있게 됨
                        return Move.WasteToTableau(targetCol)
                    }
                }
            }
        }
        
        // 연결 효과가 없어도 옮길 수 있으면 옮김 (기본 동작)
        for (col in 0..6) {
            if (canMoveCard(wasteCard, state.tableau[col])) {
                return Move.WasteToTableau(col)
            }
        }
        
        return null
    }
    
    private fun canMoveSequence(movingCards: List<Card>, targetPile: List<Card>): Boolean {
        if (movingCards.isEmpty()) return false
        return rulesEngine.canMoveSequenceToTableau(movingCards, targetPile)
    }
    
    private fun canMoveCard(card: Card, targetPile: List<Card>): Boolean {
        return rulesEngine.canMoveSequenceToTableau(listOf(card), targetPile)
    }
}
