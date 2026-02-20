package us.jyni.game.klondike.solver

/**
 * Solver 결과
 */
sealed class SolverResult {
    /**
     * 승리 경로 발견
     */
    data class Success(
        val moves: List<Move>,
        val statesExplored: Int
    ) : SolverResult()
    
    /**
     * Inherently Unsolvable: 초기 배치 자체가 해결 불가능
     * 어떤 이동 순서로도 승리할 수 없음
     */
    data class InherentlyUnsolvable(val reason: UnsolvableReason) : SolverResult()
    
    /**
     * Unwinnable State: 플레이어의 선택으로 인한 막힌 상태
     * 다른 경로로는 가능했을 수 있음
     */
    data class UnwinnableState(val reason: String) : SolverResult()
    
    /**
     * 탐색 시간 초과
     */
    data class Timeout(val reason: String) : SolverResult()
    
    /**
     * 너무 복잡함 (상태 수 초과)
     */
    data class TooComplex(val reason: String) : SolverResult()
}

/**
 * Unsolvable 이유를 나타내는 sealed class
 */
sealed class UnsolvableReason(val message: String) {
    /**
     * Dead End: 더 이상 가능한 이동이 없음 (Unwinnable State)
     */
    data class DeadEnd(val reason: String) : UnsolvableReason(reason)
    
    /**
     * State Cycle: 이전에 방문한 상태로 돌아옴 (Unwinnable State)
     */
    data class StateCycle(val reason: String) : UnsolvableReason(reason)
    
    /**
     * N-Pile Irretrievable: N개 pile이 서로의 필요 카드를 모두 막음
     * (Inherently Unsolvable)
     */
    sealed class NPileIrretrievable(reason: String) : UnsolvableReason(reason) {
        data class Single(val pileIndex: Int, val card: String) : 
            NPileIrretrievable("Single Irretrievable at pile $pileIndex: $card")
        
        data class Pair(val piles: List<Int>, val cards: List<String>) : 
            NPileIrretrievable("Pair Irretrievable at piles ${piles.joinToString()}: ${cards.joinToString()}")
        
        data class Group(val n: Int, val piles: List<Int>, val cards: List<String>) : 
            NPileIrretrievable("$n-Pile Group Irretrievable at piles ${piles.joinToString()}: ${cards.joinToString()}")
    }
    
    /**
     * King Irretrievable: King의 Foundation + Tableau 경로 모두 차단
     * (Inherently Unsolvable)
     */
    data class KingIrretrievable(val pileIndex: Int, val king: String) : 
        UnsolvableReason("King Irretrievable at pile $pileIndex: $king")
    
    /**
     * Circular Dependency: 일반적인 순환 의존성
     * (Inherently Unsolvable)
     */
    data class CircularDependency(val reason: String) : UnsolvableReason(reason)
}
