package us.jyni.game.klondike.solver

import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.engine.GameEngine

/**
 * 게임이 unsolvable 상태인지 빠르게 판단하는 검사기
 * 
 * 전체 탐색 없이 특정 패턴을 감지하여 불가능한 게임을 조기에 걸러냅니다.
 */
class UnsolvableDetector(private val engine: GameEngine) {
    
    /**
     * 게임이 unsolvable인지 판단
     * @return UnsolvableReason if unsolvable, null if solvable or unknown
     */
    fun check(state: GameState): UnsolvableReason? {
        // 빠른 순서대로 체크 (가장 쉬운 것부터)
        
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd("더 이상 가능한 이동이 없습니다")
        }
        
        if (hasKingDeadlock(state)) {
            return UnsolvableReason.KingDeadlock("킹이 필요한 카드를 막고 있습니다")
        }
        
        val suitBlock = findSameSuitBlock(state)
        if (suitBlock != null) {
            return suitBlock
        }
        
        return null  // Solvable 또는 판단 불가
    }
    
    /**
     * 1. 즉시 막힘 (Dead End)
     * Stock과 Waste가 비었고, 가능한 이동이 전혀 없음
     */
    private fun isDeadEnd(state: GameState): Boolean {
        // Stock이나 Waste에 카드가 있으면 아직 draw 가능
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            return false
        }
        
        // Tableau에서 Foundation으로 이동 가능한 카드가 있는지
        for (col in 0..6) {
            for (foundationIndex in 0..3) {
                if (engine.canMoveTableauToFoundation(col, foundationIndex)) {
                    return false  // 가능한 이동이 있음
                }
            }
        }
        
        // Tableau 간 이동이 가능한지
        for (fromCol in 0..6) {
            for (toCol in 0..6) {
                if (fromCol != toCol && engine.canMoveTableauToTableau(fromCol, toCol)) {
                    return false  // 가능한 이동이 있음
                }
            }
        }
        
        // 아무 이동도 불가능
        return true
    }
    
    /**
     * 2. 킹 데드락 (King Deadlock)
     * 모든 빈 공간이 없고, 킹 밑에 뒷면 카드가 있음
     */
    private fun hasKingDeadlock(state: GameState): Boolean {
        val emptyColumns = state.tableau.count { it.isEmpty() }
        if (emptyColumns > 0) return false  // 킹을 옮길 곳이 있음
        
        // 킹이 맨 위에 있고 밑에 뒷면 카드가 있는 경우를 찾기
        for (pile in state.tableau) {
            if (pile.isEmpty()) continue
            
            val topCard = pile.last()
            if (topCard.rank.value == 13) {  // 킹
                // 킹 밑에 뒷면 카드가 있는지
                val hasFaceDownBelow = pile.dropLast(1).any { !it.isFaceUp }
                if (hasFaceDownBelow) {
                    // 킹을 옮길 수 있는지 확인
                    var canMoveKing = false
                    for (toCol in 0..6) {
                        val toIndex = state.tableau.indexOf(pile)
                        if (toCol != toIndex && state.tableau[toCol].isEmpty()) {
                            canMoveKing = true
                            break
                        }
                    }
                    if (!canMoveKing) {
                        return true  // 킹 데드락
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * 3. 무늬 블록 (Same Suit Block)
     * Foundation에 올라가야 할 카드가 같은 무늬의 더 높은 카드 밑에 깔림
     */
    private fun findSameSuitBlock(state: GameState): UnsolvableReason.SameSuitBlock? {
        for ((foundationIndex, foundation) in state.foundation.withIndex()) {
            if (foundation.isEmpty()) continue
            
            val topFoundation = foundation.last()
            val nextRankNeeded = topFoundation.rank.value + 1  // 다음에 필요한 랭크
            
            if (nextRankNeeded > 13) continue  // 이미 K까지 완성
            
            // 같은 무늬의 다음 카드를 Tableau에서 찾기
            for ((pileIndex, pile) in state.tableau.withIndex()) {
                for ((cardIndex, card) in pile.withIndex()) {
                    if (card.suit == topFoundation.suit && card.rank.value == nextRankNeeded) {
                        // 필요한 카드 발견! 
                        // 이 카드 위에 같은 무늬의 더 높은 카드가 있는지 확인
                        val cardsAbove = pile.subList(cardIndex + 1, pile.size)
                        
                        for (aboveCard in cardsAbove) {
                            if (aboveCard.suit == topFoundation.suit && 
                                aboveCard.rank.value > nextRankNeeded) {
                                // 같은 무늬의 더 높은 카드가 위에 있음
                                // 이것은 잠재적 블록이지만, 항상 unsolvable은 아님
                                // (다른 경로로 카드를 꺼낼 수 있을 수도 있음)
                                
                                // 더 엄격한 조건: 카드가 뒷면이거나, 꺼낼 방법이 없는 경우
                                if (!card.isFaceUp) {
                                    return UnsolvableReason.SameSuitBlock(
                                        "필요한 카드가 같은 무늬 카드 밑에 뒷면으로 깔려있습니다: " +
                                        "${topFoundation.suit} ${nextRankNeeded}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null
    }
}

/**
 * Unsolvable 이유를 나타내는 sealed class
 */
sealed class UnsolvableReason(val message: String) {
    /**
     * 더 이상 가능한 이동이 없음
     */
    data class DeadEnd(val reason: String) : UnsolvableReason(reason)
    
    /**
     * 킹이 중요한 카드를 막고 있음
     */
    data class KingDeadlock(val reason: String) : UnsolvableReason(reason)
    
    /**
     * 같은 무늬의 카드가 블록을 형성
     */
    data class SameSuitBlock(val reason: String) : UnsolvableReason(reason)
    
    /**
     * 순환 의존성 (구현 예정)
     */
    data class CircularDependency(val reason: String) : UnsolvableReason(reason)
    
    /**
     * 필수 카드에 접근 불가 (구현 예정)
     */
    data class UnreachableCard(val reason: String) : UnsolvableReason(reason)
}
