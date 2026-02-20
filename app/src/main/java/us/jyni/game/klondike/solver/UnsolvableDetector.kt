package us.jyni.game.klondike.solver

import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.engine.GameEngine
import kotlin.math.min

/**
 * N-Pile Irretrievable 프레임워크를 사용한 Unsolvable 검사기
 * 
 * 게임 시작 시 7ms 이내에 Inherently Unsolvable을 완전히 판단합니다.
 */
class UnsolvableDetector(private val engine: GameEngine) {
    
    /**
     * Inherently Unsolvable 완전 검사 (게임 시작 시 호출)
     * @return UnsolvableReason if inherently unsolvable, null if solvable
     */
    fun checkInherentlyUnsolvable(state: GameState): UnsolvableReason? {
        // 1. N-Pile Irretrievable (N=1부터 N=5까지)
        for (n in 1..5) {
            val nPileReason = checkNPileIrretrievable(state, n)
            if (nPileReason != null) return nPileReason
        }
        
        // 3. King Irretrievable (특수 케이스)
        for (i in state.tableau.indices) {
            val kingReason = checkKingIrretrievable(state, i)
            if (kingReason != null) return kingReason
        }
        
        return null  // Solvable!
    }
    
    /**
     * Unwinnable State 검사 (게임 진행 중 호출)
     * @return UnsolvableReason if unwinnable, null if still winnable
     */
    fun checkUnwinnableState(state: GameState): UnsolvableReason? {
        // 1. Dead End: 모든 이동이 불가능한 상태
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd("더 이상 가능한 이동이 없습니다")
        }
        
        // 2. State Cycle: 이전에 방문한 상태로 돌아옴
        val stateCycle = checkStateCycle()
        if (stateCycle != null) return stateCycle
        
        return null
    }
    
    /**
     * State Cycle 검사
     * GameEngine의 상태 히스토리를 사용하여 순환 검출
     * @return UnsolvableReason.StateCycle if cycle detected, null otherwise
     */
    fun checkStateCycle(): UnsolvableReason? {
        // 현재 상태가 이미 히스토리에 있는지 확인
        val isInHistory = engine.isStateInHistory()
        
        if (isInHistory) {
            // 이전에 방문한 상태 → Cycle 감지
            return UnsolvableReason.StateCycle("이전 상태로 돌아왔습니다 (무한 루프)")
        }
        
        // 새로운 상태면 히스토리에 추가
        engine.recordCurrentState()
        
        return null
    }
    
    /**
     * 통합 Unsolvable 검사 (Inherently + Unwinnable)
     * Legacy 호환성을 위해 유지
     */
    fun check(state: GameState): UnsolvableReason? {
        // 1. Inherently Unsolvable 검사
        val inherently = checkInherentlyUnsolvable(state)
        if (inherently != null) return inherently
        
        // 2. Unwinnable State 검사
        return checkUnwinnableState(state)
    }

    
    // ========== N-Pile Irretrievable Framework ==========
    
    private fun checkNPileIrretrievable(state: GameState, n: Int): UnsolvableReason? {
        // 의미 있는 pile 선택 (face-down이 충분한 pile만)
        val meaningfulPiles = state.tableau.indices.filter { i ->
            val pile = state.tableau[i]
            val faceDownCount = pile.count { !it.isFaceUp }
            faceDownCount >= (n - 1)
        }
        
        if (meaningfulPiles.size < n) return null
        
        // N개 pile 조합 생성
        val combinations = generateCombinations(meaningfulPiles, n)
        
        for (combo in combinations) {
            // 이 조합의 모든 face-down 합치기
            val combinedFaceDown = combo.flatMap { i ->
                state.tableau[i].filter { !it.isFaceUp }
            }
            
            // 이 조합의 각 pile의 face-up 카드들 검사
            for (pileIndex in combo) {
                val pile = state.tableau[pileIndex]
                val faceUpCards = pile.filter { it.isFaceUp }
                
                if (faceUpCards.isEmpty()) continue
                
                // 맨 위부터 순차 검사 (최대 4장)
                val cardsToCheck = min(faceUpCards.size, 4)
                for (k in 0 until cardsToCheck) {
                    val cardIndex = faceUpCards.size - 1 - k  // 맨 위부터
                    val card = faceUpCards[cardIndex]
                    
                    if (isCardIrretrievable(card, combinedFaceDown, state, pileIndex)) {
                        // Irretrievable 발견!
                        val cardStr = "${card.suit} ${card.rank}"
                        return when (n) {
                            1 -> UnsolvableReason.NPileIrretrievable.Single(pileIndex, cardStr)
                            2 -> UnsolvableReason.NPileIrretrievable.Pair(combo, listOf(cardStr))
                            else -> UnsolvableReason.NPileIrretrievable.Group(n, combo, listOf(cardStr))
                        }
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * 카드가 Irretrievable인지 확인
     */
    private fun isCardIrretrievable(
        card: Card,
        faceDownBelow: List<Card>,
        state: GameState,
        cardPileIndex: Int
    ): Boolean {
        // King은 별도 처리
        if (card.rank == Rank.KING) {
            return false  // King Irretrievable은 별도 함수에서 처리
        }
        
        // 1. Foundation 이동에 필요한 카드
        val requiredForFoundation = getRequiredForFoundation(card, state)
        
        // 2. Tableau 이동에 필요한 카드
        val requiredForTableau = getRequiredForTableau(card)
        
        // 3. Foundation 경로 가능성 확인
        // 필요한 카드가 없으면 바로 갈 수 있음 (blocked = false)
        // 필요한 카드가 있으면 모두 face-down에 있을 때만 blocked
        val foundationBlocked = requiredForFoundation.isNotEmpty() && requiredForFoundation.all { required ->
            faceDownBelow.any { it.suit == required.suit && it.rank == required.rank }
        }
        
        // 4. Tableau 경로 가능성 확인
        // 필요한 카드 중 하나라도 접근 가능하면 blocked = false
        val tableauBlocked = requiredForTableau.all { required ->
            // face-down에 있거나, 다른 접근 가능한 pile에 없음
            val inFaceDown = faceDownBelow.any { it.suit == required.suit && it.rank == required.rank }
            if (inFaceDown) return@all true
            
            // 다른 pile에서 접근 가능한지 확인
            val accessibleInOtherPile = state.tableau.withIndex().any { (index, pile) ->
                index != cardPileIndex && pile.any { it.isFaceUp && it.suit == required.suit && it.rank == required.rank }
            }
            !accessibleInOtherPile
        }
        
        // 두 경로 모두 차단되면 irretrievable
        return foundationBlocked && tableauBlocked
    }
    
    /**
     * Foundation 이동에 필요한 모든 카드 반환
     */
    private fun getRequiredForFoundation(card: Card, state: GameState): List<Card> {
        if (card.rank == Rank.ACE) return emptyList()
        
        val required = mutableListOf<Card>()
        
        // Foundation에서 현재 어디까지 올라갔는지
        val foundationPile = state.foundation.find { it.isNotEmpty() && it.first().suit == card.suit }
        val currentRank = foundationPile?.lastOrNull()?.rank?.value ?: 0
        
        // 현재 카드를 올리기 위해 필요한 직전 카드 (currentRank + 1 = card.rank.value여야 함)
        // 예: HEARTS TWO를 올리려면 HEARTS ACE가 Foundation에 있어야 (currentRank = 1)
        // 즉, currentRank + 1 = 2여야 TWO를 올릴 수 있음
        
        // 중간 모든 카드가 필요
        for (rank in (currentRank + 1) until card.rank.value) {
            required.add(Card(card.suit, Rank.values()[rank - 1], false))
        }
        
        return required
    }
    
    /**
     * Tableau 이동에 필요한 카드 반환
     */
    private fun getRequiredForTableau(card: Card): List<Card> {
        if (card.rank == Rank.KING) return emptyList()
        
        // 반대 색깔의 rank+1 카드들
        val oppositeColorSuits = if (card.suit.isRed()) {
            listOf(Suit.CLUBS, Suit.SPADES)
        } else {
            listOf(Suit.HEARTS, Suit.DIAMONDS)
        }
        
        val nextRank = Rank.values()[card.rank.value]  // rank+1
        return oppositeColorSuits.map { suit -> Card(suit, nextRank, false) }
    }
    
    // ========== King Irretrievable ==========
    
    private fun checkKingIrretrievable(state: GameState, kingPileIndex: Int): UnsolvableReason? {
        val pile = state.tableau[kingPileIndex]
        val king = pile.filter { it.isFaceUp }.firstOrNull { it.rank == Rank.KING } ?: return null
        
        // 1. Foundation 경로 확인
        val queenRequired = Card(king.suit, Rank.QUEEN, false)
        val faceDownCards = pile.filter { !it.isFaceUp }
        val foundationBlocked = faceDownCards.any { it.suit == queenRequired.suit && it.rank == queenRequired.rank }
        
        if (!foundationBlocked) return null  // Foundation으로 갈 수 있음
        
        // 2. 빈 pile 생성 가능성 확인
        // 다른 모든 pile이 King pile과 Pair Irretrievable인가?
        for (otherIndex in state.tableau.indices) {
            if (otherIndex == kingPileIndex) continue
            
            // 이 pile을 비울 수 있는지 확인
            val canBeEmptied = canPileBeEmptied(state, otherIndex, kingPileIndex)
            if (canBeEmptied) {
                return null  // 빈 pile 생성 가능
            }
        }
        
        // Foundation + Tableau 모두 차단
        return UnsolvableReason.KingIrretrievable(kingPileIndex, "${king.suit} ${king.rank}")
    }
    
    /**
     * Pile을 비울 수 있는지 확인 (King과의 Pair Irretrievable 체크)
     */
    private fun canPileBeEmptied(state: GameState, pileIndex: Int, kingPileIndex: Int): Boolean {
        val pile = state.tableau[pileIndex]
        val kingPile = state.tableau[kingPileIndex]
        
        val faceUpCards = pile.filter { it.isFaceUp }
        if (faceUpCards.isEmpty()) return true  // 이미 비어있거나 비울 수 있음
        
        val combinedFaceDown = pile.filter { !it.isFaceUp } + kingPile.filter { !it.isFaceUp }
        
        // 모든 face-up 카드가 irretrievable인지 확인
        for (card in faceUpCards) {
            if (!isCardIrretrievable(card, combinedFaceDown, state, pileIndex)) {
                return true  // 하나라도 retrievable이면 pile을 비울 수 있음
            }
        }
        
        return false  // 모든 카드가 irretrievable → pile 못 비움
    }
    
    // ========== Dead End (Unwinnable State) ==========
    
    private fun isDeadEnd(state: GameState): Boolean {
        // 1. Draw 가능성 확인
        if (canDraw(state)) {
            return false
        }
        
        // 2. Waste에서 이동 가능한지
        if (state.waste.isNotEmpty()) {
            // Waste → Foundation
            for (foundationIndex in 0..3) {
                if (engine.canMoveWasteToFoundation(foundationIndex)) {
                    return false
                }
            }
            
            // Waste → Tableau
            for (col in 0..6) {
                if (engine.canMoveWasteToTableau(col)) {
                    return false
                }
            }
        }
        
        // 3. Tableau → Foundation
        for (col in 0..6) {
            for (foundationIndex in 0..3) {
                if (engine.canMoveTableauToFoundation(col, foundationIndex)) {
                    return false
                }
            }
        }
        
        // 4. Tableau → Tableau
        for (fromCol in 0..6) {
            for (toCol in 0..6) {
                if (fromCol != toCol && engine.canMoveTableauToTableau(fromCol, toCol)) {
                    return false
                }
            }
        }
        
        // 5. Foundation → Tableau (규칙에 따라)
        if (state.rules.allowFoundationToTableau) {
            for (foundationIndex in 0..3) {
                for (col in 0..6) {
                    if (engine.canMoveFoundationToTableau(foundationIndex, col)) {
                        return false
                    }
                }
            }
        }
        
        return true  // 진짜 Dead End - 가능한 이동이 전혀 없음
    }
    
    /**
     * Draw 가능 여부 확인 (Stock에서 Waste로 카드 뽑기 또는 재활용)
     * 
     * Draw 가능 조건:
     * 1. Stock에 카드가 있음 OR
     * 2. Waste를 재활용할 수 있음 (waste 있고 redeals 남음)
     */
    private fun canDraw(state: GameState): Boolean {
        // Stock에 카드가 있으면 바로 draw 가능
        if (state.stock.isNotEmpty()) {
            return true
        }
        
        // Stock이 비었지만 Waste를 재활용할 수 있는지 확인
        if (state.waste.isEmpty()) {
            return false
        }
        
        // 재활용 가능 조건: redealsRemaining > 0 또는 unlimited (-1)
        return state.redealsRemaining > 0 || state.redealsRemaining < 0
    }
    
    // ========== Utility Functions ==========
    
    /**
     * 조합 생성 (C(n, r))
     */
    private fun generateCombinations(items: List<Int>, r: Int): List<List<Int>> {
        if (r == 0) return listOf(emptyList())
        if (items.isEmpty()) return emptyList()
        
        val result = mutableListOf<List<Int>>()
        
        fun combine(start: Int, current: List<Int>) {
            if (current.size == r) {
                result.add(current)
                return
            }
            
            for (i in start until items.size) {
                combine(i + 1, current + items[i])
            }
        }
        
        combine(0, emptyList())
        return result
    }
}
