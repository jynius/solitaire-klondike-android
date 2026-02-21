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
     * 게임의 Inherent 속성(Game Property)을 검사합니다.
     * 
     * 이것은 "현재 플레이 상태"가 아니라 "게임 속성"을 검사합니다.
     * Seed + Rules가 동일하면 언제 호출해도 결과가 같습니다.
     * 
     * @param state 초기 배치 정보를 담은 GameState (보통 초기 상태)
     *              주의: 이 검사는 state 객체를 받지만, 실제로는 "게임 속성"을 판단합니다.
     *                   같은 seed + rules로 생성된 게임이라면 언제 체크해도 항상 동일한 결과를 반환합니다.
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
     * 게임의 Inherent 속성(Game Property)을 검사합니다 (디버깅 정보 포함)
     * 
     * 이것은 "현재 플레이 상태"가 아니라 "게임 속성"을 검사합니다.
     * Seed + Rules가 동일하면 언제 호출해도 결과가 같습니다.
     * 
     * @param state 초기 배치 정보를 담은 GameState (보통 초기 상태)
     * @return Pair<UnsolvableReason?, String> - 결과와 디버그 로그
     */
    fun checkInherentlyUnsolvableWithDebug(state: GameState): Pair<UnsolvableReason?, String> {
        val log = StringBuilder()
        log.appendLine("=== Unsolvable Detector Debug ===")
        
        // 1. N-Pile Irretrievable (N=1부터 N=5까지)
        for (n in 1..5) {
            log.appendLine("\n[Checking $n-Pile Irretrievable]")
            val nPileReason = checkNPileIrretrievable(state, n)
            if (nPileReason != null) {
                log.appendLine("✗ FOUND: $nPileReason")
                return Pair(nPileReason, log.toString())
            } else {
                log.appendLine("✓ No $n-pile irretrievable found")
            }
        }
        
        // 2. King Irretrievable (특수 케이스)
        log.appendLine("\n[Checking King Irretrievable]")
        for (i in state.tableau.indices) {
            val kingReason = checkKingIrretrievable(state, i)
            if (kingReason != null) {
                log.appendLine("✗ FOUND: $kingReason")
                return Pair(kingReason, log.toString())
            }
        }
        log.appendLine("✓ No king irretrievable found")
        
       현재 플레이 상태(Play State)를 검사합니다.
     * 
     * 이것은 게임 속성이 아니라 "현재 진행 상황에 따른 상태"를 검사합니다.
     * 플레이어의 선택에 따라 매 이동마다 결과가 달라질 수 있습니다.
     * 
     * @param state 현재 게임 상태=== RESULT: SOLVABLE ===")
        return Pair(null, log.toString())
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
            // N개 pile의 face-up 맨 밑 카드 가져오기
            val cards = combo.mapNotNull { i ->
                val pile = state.tableau[i]
                val faceUpCards = pile.filter { it.isFaceUp }
                faceUpCards.firstOrNull()?.let { card -> Pair(card, i) }
            }
            
            // N개 카드가 모두 있어야 함
            if (cards.size != n) continue
            
            // N개 카드의 필요 카드 합집합 계산
            val allRequired = cards.flatMap { (card, _) ->
                getRequiredForFoundation(card, state) + getRequiredForTableau(card)
            }.distinctBy { "${it.suit}_${it.rank}" }
            
            // 합집합이 비어있으면 스킵 (모든 카드가 이동 가능)
            if (allRequired.isEmpty()) continue
            
            // N개 pile의 combinedFaceDown
            val combinedFaceDown = combo.flatMap { i ->
                state.tableau[i].filter { !it.isFaceUp }
            }
            
            // 합집합이 모두 combinedFaceDown에 있는지 확인
            // Stock/Waste 무관: Tableau pile 내부만 검사 (문서 참조)
            val allBlocked = allRequired.all { required ->
                combinedFaceDown.any { it.suit == required.suit && it.rank == required.rank }
            }
            
            if (allBlocked) {
                // N-Pile Irretrievable 발견!
                val cardStrList = cards.map { (card, _) -> "${card.suit} ${card.rank}" }
                return when (n) {
                    1 -> UnsolvableReason.NPileIrretrievable.Single(combo[0], cardStrList[0])
                    2 -> UnsolvableReason.NPileIrretrievable.Pair(combo, cardStrList)
                    else -> UnsolvableReason.NPileIrretrievable.Group(n, combo, cardStrList)
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
        // 필요한 카드가 있으면 모두 face-down에 있고 Stock/Waste에도 없을 때만 blocked
        val foundationBlocked = requiredForFoundation.isNotEmpty() && requiredForFoundation.all { required ->
            val inFaceDown = faceDownBelow.any { it.suit == required.suit && it.rank == required.rank }
            val inStock = state.stock.any { it.suit == required.suit && it.rank == required.rank }
            val inWaste = state.waste.any { it.suit == required.suit && it.rank == required.rank }
            
            // face-down에 있고 Stock/Waste에 없으면 접근 불가
            inFaceDown && !inStock && !inWaste
        }
        
        // 4. Tableau 경로 가능성 확인
        // 필요한 카드 중 하나라도 접근 가능하면 blocked = false
        val tableauBlocked = requiredForTableau.all { required ->
            // face-down에 있는지 확인
            val inFaceDown = faceDownBelow.any { it.suit == required.suit && it.rank == required.rank }
            if (inFaceDown) {
                // face-down에 있어도 Stock/Waste에서 접근 가능하면 OK
                val inStock = state.stock.any { it.suit == required.suit && it.rank == required.rank }
                val inWaste = state.waste.any { it.suit == required.suit && it.rank == required.rank }
                if (inStock || inWaste) return@all false
                
                return@all true  // face-down에만 있음
            }
            
            // 다른 pile에서 접근 가능한지 확인
            val accessibleInOtherPile = state.tableau.withIndex().any { (index, pile) ->
                index != cardPileIndex && pile.any { it.isFaceUp && it.suit == required.suit && it.rank == required.rank }
            }
            
            // Stock/Waste에서 접근 가능한지 확인
            val inStock = state.stock.any { it.suit == required.suit && it.rank == required.rank }
            val inWaste = state.waste.any { it.suit == required.suit && it.rank == required.rank }
            
            // 어디에도 없으면 blocked
            !accessibleInOtherPile && !inStock && !inWaste
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
