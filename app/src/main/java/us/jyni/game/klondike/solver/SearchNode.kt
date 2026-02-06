package us.jyni.game.klondike.solver

import us.jyni.game.klondike.model.GameState

/**
 * A* 탐색 노드
 */
data class SearchNode(
    val state: GameState,
    val gCost: Int,              // 시작부터 현재까지의 실제 비용
    val hCost: Int,              // 현재부터 목표까지의 추정 비용 (휴리스틱)
    val path: List<Move>
) {
    val fCost: Int get() = gCost + hCost  // 총 예상 비용
}
