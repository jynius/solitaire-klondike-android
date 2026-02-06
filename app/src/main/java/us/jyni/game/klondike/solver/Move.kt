package us.jyni.game.klondike.solver

/**
 * 솔리테어 게임에서 가능한 모든 이동 타입을 정의
 */
sealed class Move {
    /**
     * Tableau 간 이동
     * @param fromCol 출발 컬럼 (0..6)
     * @param cardIndex 이동할 카드의 인덱스 (해당 카드부터 끝까지 이동)
     * @param toCol 목적지 컬럼 (0..6)
     */
    data class TableauToTableau(
        val fromCol: Int, 
        val cardIndex: Int, 
        val toCol: Int
    ) : Move() {
        override fun toString() = "T[$fromCol]@$cardIndex -> T[$toCol]"
    }
    
    /**
     * Tableau에서 Foundation으로 이동
     * @param fromCol 출발 컬럼 (0..6)
     * @param foundationIndex 목적지 Foundation (0..3)
     */
    data class TableauToFoundation(
        val fromCol: Int, 
        val foundationIndex: Int
    ) : Move() {
        override fun toString() = "T[$fromCol] -> F[$foundationIndex]"
    }
    
    /**
     * Waste에서 Tableau로 이동
     * @param toCol 목적지 컬럼 (0..6)
     */
    data class WasteToTableau(
        val toCol: Int
    ) : Move() {
        override fun toString() = "W -> T[$toCol]"
    }
    
    /**
     * Waste에서 Foundation으로 이동
     * @param foundationIndex 목적지 Foundation (0..3)
     */
    data class WasteToFoundation(
        val foundationIndex: Int
    ) : Move() {
        override fun toString() = "W -> F[$foundationIndex]"
    }
    
    /**
     * Foundation에서 Tableau로 이동
     * @param foundationIndex 출발 Foundation (0..3)
     * @param toCol 목적지 컬럼 (0..6)
     */
    data class FoundationToTableau(
        val foundationIndex: Int, 
        val toCol: Int
    ) : Move() {
        override fun toString() = "F[$foundationIndex] -> T[$toCol]"
    }
    
    /**
     * Stock에서 Waste로 카드 뽑기
     */
    object Draw : Move() {
        override fun toString() = "Draw"
    }
}
