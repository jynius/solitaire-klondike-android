package us.jyni.game.klondike.solver

import us.jyni.game.klondike.model.GameState

/**
 * 솔리테어 Solver 인터페이스
 * 
 * 다양한 알고리즘(BFS, A*, DFS 등)을 동일한 방식으로 사용할 수 있도록 합니다.
 * Strategy 패턴을 적용하여 런타임에 Solver를 교체할 수 있습니다.
 */
interface Solver {
    /**
     * 현재 상태에서 승리까지의 경로 찾기
     * 
     * @param initialState 탐색 시작 상태
     * @return 탐색 결과 (성공, 실패, 타임아웃 등)
     */
    fun solve(initialState: GameState): SolverResult
    
    /**
     * 최선의 다음 이동 찾기 (힌트용)
     * 
     * @param state 현재 상태
     * @return 추천 이동 (없으면 null)
     */
    fun findBestMove(state: GameState): Move?
}

/**
 * Solver 타입
 */
enum class SolverType {
    /**
     * Breadth-First Search
     * - 최적 경로 보장
     * - 메모리 사용량 많음
     * - 안정적
     */
    BFS,
    
    /**
     * A* with Heuristic
     * - 휴리스틱으로 빠른 탐색
     * - 더 많은 상태 탐색 가능
     * - 복잡한 게임에 유리
     */
    ASTAR
}
