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
     * 승리 불가능
     */
    data class Unsolvable(val reason: String) : SolverResult()
    
    /**
     * 탐색 시간 초과
     */
    data class Timeout(val reason: String) : SolverResult()
    
    /**
     * 너무 복잡함 (상태 수 초과)
     */
    data class TooComplex(val reason: String) : SolverResult()
}
