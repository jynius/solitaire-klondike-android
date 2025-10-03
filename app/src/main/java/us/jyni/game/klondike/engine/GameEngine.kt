package us.jyni.game.klondike.engine

import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Deck
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Pile
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.util.sync.fisherYatesShuffle
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.SaveCodec
import us.jyni.game.klondike.util.stats.SolveStats
import us.jyni.game.klondike.util.stats.SolveCodec
import us.jyni.game.klondike.util.sync.DealId
import us.jyni.game.klondike.util.sync.LayoutId

class GameEngine {
    private var rules: Ruleset = Ruleset()
    private var redealsRemaining: Int = rules.redeals
    private val rulesEngine = KlondikeRules()
    private val undo = UndoManager()
    private var currentSeed: ULong = 0u
    private var currentDealId: String = ""
    private var startedAt: Long = 0L
    private var finishedAt: Long? = null
    private var moveCount: Int = 0
    private var outcome: String? = null
    private var gameState: GameState = GameState(
        tableau = List(7) { mutableListOf() },
        foundation = List(4) { mutableListOf() },
        stock = mutableListOf(),
        waste = mutableListOf(),
        isGameOver = false
    )
    private var deck: Deck = Deck()
    private var piles: List<Pile> = listOf()

    /**
     * Klondike 초기 배치:
     * - 7개의 테이블로 (0..6), 각 열은 1..7장의 카드를 받음. 각 열의 맨 위 카드만 앞면.
     * - 남은 카드는 stock (뒷면).
     * - waste는 비어 있음.
     * - 간단한 결정적 셔플 사용(기본 seed 고정). 추후 DealId/Ruleset로 확장.
     */
    fun startGame(seed: ULong = 0xCAFEBABE_uL, rules: Ruleset = Ruleset())
    {
        android.util.Log.d("GameEngine", "startGame called with seed: $seed")
        this.rules = rules
        this.redealsRemaining = rules.redeals
        this.currentSeed = seed
        this.startedAt = System.currentTimeMillis()
        this.finishedAt = null
        this.moveCount = 0
        this.outcome = null
        // 준비: 덱을 셔플
        val list: MutableList<Card> = deck.asMutableList()
        fisherYatesShuffle(list, seed)

        // DealId 계산: canonical 52 (S,H,D,C) 생성 후 같은 seed로 셔플하여 직렬화
        run {
            val suits = listOf(Suit.SPADES, Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS)
            val ranks = listOf(Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING)
            val canonical: MutableList<String> = mutableListOf()
            for (s in suits) for (r in ranks) canonical.add(suitCode(s) + ":" + rankCode(r))
            fisherYatesShuffle(canonical, seed)
            currentDealId = DealId.generate(canonical, rules, seed)
        }

        // 테이블로/파운데이션 초기화
        val tableau = List(7) { mutableListOf<Card>() }
        val foundation = List(4) { mutableListOf<Card>() }
        val stock = mutableListOf<Card>()
        val waste = mutableListOf<Card>()

        // 딜: 1~7장
        var idx = 0
        for (col in 0 until 7) {
            for (row in 0..col) {
                val card = list[idx++]
                // 각 열의 마지막 카드만 앞면
                card.isFaceUp = (row == col)
                tableau[col].add(card)
            }
        }
        // 남은 카드는 stock (뒷면)
        while (idx < list.size) {
            val c = list[idx++]
            c.isFaceUp = false
            stock.add(c)
        }

        gameState = GameState(
            tableau = tableau,
            foundation = foundation,
            stock = stock,
            waste = waste,
            isGameOver = false
        )
        // 초기 스냅샷 저장
        undo.clearHistory()
        undo.saveState(snapshot())
    }

    fun moveCard(fromPile: Pile, toPile: Pile) {
        fromPile.removeCard()?.let { toPile.addCard(it) }
    }

    fun undoLastMove() {
        // TODO: wire with UndoManager
    }

    fun getGameState(): GameState = cloneState(gameState)
    fun getRules(): Ruleset = rules
    fun getRedealsRemaining(): Int = redealsRemaining

    // --- Draw & Recycle ---
    fun draw(): Int {
        // snapshot은 상태 변화가 실제로 발생한 후 저장
        if (gameState.stock.isEmpty()) {
            if (!canRecycle()) return 0
            recycleWasteToStock()
            if (gameState.stock.isEmpty()) return 0
        }
        val n = kotlin.math.min(rules.draw, gameState.stock.size)
        repeat(n) {
            val c = gameState.stock.removeAt(gameState.stock.lastIndex)
            c.isFaceUp = true
            gameState.waste.add(c)
        }
        undo.saveState(snapshot())
        moveCount += 1
        return n
    }

    private fun canRecycle(): Boolean {
        return gameState.waste.isNotEmpty() && (redealsRemaining > 0 || rules.redeals < 0)
    }

    private fun recycleWasteToStock() {
        if (gameState.waste.isEmpty()) return
        val moving = if (rules.recycle == RecycleOrder.KEEP) {
            gameState.waste.toList()
        } else {
            gameState.waste.asReversed().toList()
        }
        // clear waste
        gameState.waste.clear()
        // move to stock with face down
        for (card in moving) {
            card.isFaceUp = false
            gameState.stock.add(card)
        }
        if (rules.redeals >= 0) {
            redealsRemaining -= 1
        }
    }

    // --- Moves ---
    fun moveTableauToTableau(fromCol: Int, toCol: Int): Boolean {
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return false
        val src = gameState.tableau[fromCol]
        val dst = gameState.tableau[toCol]
        
        // Get the movable sequence of cards
        val movableSequence = rulesEngine.getMovableSequence(src)
        if (movableSequence.isEmpty()) return false
        
        // Check if the sequence can be moved to the target
        if (!rulesEngine.canMoveSequenceToTableau(movableSequence, dst)) return false

        // Move all cards in the sequence
        val cardsToMove = movableSequence.size
        val movingCards = mutableListOf<Card>()
        
        // Remove cards from source (from the end)
        repeat(cardsToMove) {
            movingCards.add(0, src.removeAt(src.lastIndex))
        }
        
        // Add cards to destination
        dst.addAll(movingCards)
        
        // flip rule: reveal new top if facedown
        if (src.isNotEmpty()) {
            val top = src.last()
            if (!top.isFaceUp) top.isFaceUp = true
        }
        undo.saveState(snapshot())
        moveCount += 1
        return true
    }
    
    // 선택된 카드 인덱스부터 끝까지 이동하는 메서드
    fun moveTableauToTableauFromIndex(fromCol: Int, cardIndex: Int, toCol: Int): Boolean {
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return false
        val src = gameState.tableau[fromCol]
        val dst = gameState.tableau[toCol]
        
        if (cardIndex < 0 || cardIndex >= src.size) {
            android.util.Log.d("GameEngine", "moveTableauToTableauFromIndex: Invalid cardIndex=$cardIndex, src.size=${src.size}")
            return false
        }
        
        // 선택된 인덱스부터 끝까지의 카드들을 부분 리스트로 생성
        val partialPile = src.subList(cardIndex, src.size)
        android.util.Log.d("GameEngine", "moveTableauToTableauFromIndex: from=$fromCol, cardIndex=$cardIndex, to=$toCol")
        android.util.Log.d("GameEngine", "partialPile size: ${partialPile.size}, cards: ${partialPile.map { "${it.rank}${it.suit}" }}")
        
        val movableSequence = rulesEngine.getMovableSequence(partialPile)
        android.util.Log.d("GameEngine", "movableSequence size: ${movableSequence.size}, cards: ${movableSequence.map { "${it.rank}${it.suit}" }}")
        
        if (movableSequence.isEmpty()) {
            android.util.Log.d("GameEngine", "moveTableauToTableauFromIndex: No movable sequence")
            return false
        }
        
        // Check if the sequence can be moved to the target
        if (dst.isNotEmpty()) {
            android.util.Log.d("GameEngine", "Target top card: ${dst.last().rank} ${dst.last().suit}")
        } else {
            android.util.Log.d("GameEngine", "Target is empty")
        }
        
        val canMove = rulesEngine.canMoveSequenceToTableau(movableSequence, dst)
        android.util.Log.d("GameEngine", "canMoveSequenceToTableau: $canMove, dst.size=${dst.size}")
        if (!canMove) {
            android.util.Log.d("GameEngine", "moveTableauToTableauFromIndex: Move not allowed")
            return false
        }

        // Move cards from the selected index to the end
        val cardsToMove = movableSequence.size
        val movingCards = mutableListOf<Card>()
        
        // Remove cards from source (from the end, but only the movable sequence)
        repeat(cardsToMove) {
            movingCards.add(0, src.removeAt(src.lastIndex))
        }
        
        // Add cards to destination
        dst.addAll(movingCards)
        
        android.util.Log.d("GameEngine", "moveTableauToTableauFromIndex: Move successful! Moved ${cardsToMove} cards")
        
        // flip rule: reveal new top if facedown
        if (src.isNotEmpty()) {
            val top = src.last()
            if (!top.isFaceUp) top.isFaceUp = true
        }
        undo.saveState(snapshot())
        moveCount += 1
        return true
    }

    fun moveTableauToFoundation(fromCol: Int, foundationIndex: Int): Boolean {
        if (fromCol !in 0..6 || foundationIndex !in 0..3) return false
        val src = gameState.tableau[fromCol]
        val fnd = gameState.foundation[foundationIndex]
        if (!rulesEngine.canMoveTableauToFoundation(src, fnd)) return false

        val moving = src.removeAt(src.lastIndex)
        fnd.add(moving)
        // flip rule
        if (src.isNotEmpty()) {
            val top = src.last()
            if (!top.isFaceUp) top.isFaceUp = true
        }
        // check win condition
        gameState.isGameOver = rulesEngine.isGameWon(gameState)
        undo.saveState(snapshot())
        moveCount += 1
        if (gameState.isGameOver) {
            outcome = "win"
            finishedAt = System.currentTimeMillis()
        }
        return true
    }

    fun moveFoundationToTableau(foundationIndex: Int, toCol: Int): Boolean {
        if (!rules.allowFoundationToTableau) return false
        if (foundationIndex !in 0..3 || toCol !in 0..6) return false
        val fnd = gameState.foundation[foundationIndex]
        val dst = gameState.tableau[toCol]
        if (!rulesEngine.canMoveFoundationToTableau(fnd, dst)) return false

        val moving = fnd.removeAt(fnd.lastIndex)
        dst.add(moving)
        undo.saveState(snapshot())
        moveCount += 1
        return true
    }

    fun moveWasteToTableau(toCol: Int): Boolean {
        if (toCol !in 0..6) return false
        val src = gameState.waste
        if (src.isEmpty()) {
            android.util.Log.d("GameEngine", "moveWasteToTableau: waste is empty")
            return false
        }
        val dst = gameState.tableau[toCol]
        
        android.util.Log.d("GameEngine", "moveWasteToTableau: to=$toCol, waste card: ${src.last().rank} ${src.last().suit}, dst.size=${dst.size}")
        if (dst.isNotEmpty()) {
            android.util.Log.d("GameEngine", "Target top card: ${dst.last().rank} ${dst.last().suit}")
        } else {
            android.util.Log.d("GameEngine", "Target is empty")
        }
        
        val canMove = rulesEngine.canMoveTableauToTableau(src, dst)
        android.util.Log.d("GameEngine", "moveWasteToTableau canMove: $canMove")
        if (!canMove) return false

        val moving = src.removeAt(src.lastIndex)
        dst.add(moving)
        undo.saveState(snapshot())
        moveCount += 1
        return true
    }

    fun moveWasteToFoundation(foundationIndex: Int): Boolean {
        if (foundationIndex !in 0..3) return false
        val src = gameState.waste
        if (src.isEmpty()) return false
        val fnd = gameState.foundation[foundationIndex]
        if (!rulesEngine.canMoveTableauToFoundation(src, fnd)) return false

        val moving = src.removeAt(src.lastIndex)
        fnd.add(moving)
        // check win condition
        gameState.isGameOver = rulesEngine.isGameWon(gameState)
        undo.saveState(snapshot())
        moveCount += 1
        if (gameState.isGameOver) {
            outcome = "win"
            finishedAt = System.currentTimeMillis()
        }
        return true
    }

    fun undo(): Boolean {
        val prev = undo.undo() ?: return false
        gameState = cloneState(prev)
        return true
    }

    fun redo(): Boolean {
        val next = undo.redo() ?: return false
        gameState = cloneState(next)
        return true
    }

    fun canUndo(): Boolean = undo.hasUndo()
    fun canRedo(): Boolean = undo.hasRedo()

    fun getDealId(): String = currentDealId

    // --- Preview helpers for UI (no state change) ---
    fun canMoveTableauToTableau(fromCol: Int, toCol: Int): Boolean {
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return false
        val src = gameState.tableau[fromCol]
        val dst = gameState.tableau[toCol]
        
        // Get the movable sequence and check if it can be moved
        val movableSequence = rulesEngine.getMovableSequence(src)
        val canMove = movableSequence.isNotEmpty() && rulesEngine.canMoveSequenceToTableau(movableSequence, dst)
        
        android.util.Log.d("GameEngine", "canMoveTableauToTableau: from=$fromCol to=$toCol, src.size=${src.size}, dst.size=${dst.size}, movableSequence.size=${movableSequence.size}, canMove=$canMove")
        
        if (movableSequence.isNotEmpty()) {
            android.util.Log.d("GameEngine", "First movable card: ${movableSequence.first().rank} ${movableSequence.first().suit}")
            
            // 자세한 규칙 검사 로그
            if (dst.isEmpty()) {
                android.util.Log.d("GameEngine", "Target is empty, need KING: ${movableSequence.first().rank == Rank.KING}")
            } else {
                val targetCard = dst.last()
                android.util.Log.d("GameEngine", "Target top card: ${targetCard.rank} ${targetCard.suit}")
                val movingCard = movableSequence.first()
                val oppositeColor = rulesEngine.oppositeColor(movingCard, targetCard)
                val oneRankLower = rulesEngine.oneRankLower(movingCard, targetCard)
                android.util.Log.d("GameEngine", "Rule check - oppositeColor: $oppositeColor, oneRankLower: $oneRankLower")
            }
        } else {
            android.util.Log.d("GameEngine", "No movable sequence found")
        }
        
        return canMove
    }

    fun canMoveTableauToFoundation(fromCol: Int, foundationIndex: Int): Boolean {
        if (fromCol !in 0..6 || foundationIndex !in 0..3) return false
        return rulesEngine.canMoveTableauToFoundation(gameState.tableau[fromCol], gameState.foundation[foundationIndex])
    }

    fun canMoveWasteToFoundation(foundationIndex: Int): Boolean {
        if (foundationIndex !in 0..3) return false
        return rulesEngine.canMoveTableauToFoundation(gameState.waste, gameState.foundation[foundationIndex])
    }

    fun canMoveWasteToTableau(toCol: Int): Boolean {
        if (toCol !in 0..6) return false
        return rulesEngine.canMoveTableauToTableau(gameState.waste, gameState.tableau[toCol])
    }

    fun canMoveFoundationToTableau(foundationIndex: Int, toCol: Int): Boolean {
        if (foundationIndex !in 0..3 || toCol !in 0..6) return false
        return rulesEngine.canMoveFoundationToTableau(gameState.foundation[foundationIndex], gameState.tableau[toCol])
    }

    // --- Snapshot helpers ---
    private fun snapshot(): GameState = cloneState(gameState)

    private fun cloneState(src: GameState): GameState {
        fun cloneCards(list: List<Card>) = list.map { Card(it.suit, it.rank, it.isFaceUp) }.toMutableList()
        val tableau = src.tableau.map { cloneCards(it) }
        val foundation = src.foundation.map { cloneCards(it) }
        val stock = cloneCards(src.stock)
        val waste = cloneCards(src.waste)
        return GameState(tableau, foundation, stock, waste, src.isGameOver)
    }

    // --- Save/Restore ---
    fun saveStateString(): String = SaveCodec.encode(gameState, rules, redealsRemaining, dealId = currentDealId)

    fun restoreStateString(data: String): Boolean {
        return try {
            val decoded = SaveCodec.decode(data)
            this.gameState = decoded.state
            this.rules = decoded.rules
            this.redealsRemaining = decoded.redealsRemaining
            this.currentDealId = decoded.dealId ?: computeLayoutIdForState(gameState)
            this.currentSeed = 0u
            this.startedAt = System.currentTimeMillis()
            this.finishedAt = null
            this.moveCount = 0
            this.outcome = null
            undo.clearHistory()
            undo.saveState(snapshot())
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun computeLayoutIdForState(s: GameState): String {
        val enc = { c: Card -> suitCode(c.suit) + ":" + rankCode(c.rank) + ":" + (if (c.isFaceUp) "u" else "d") }
        val t = s.tableau.map { it.map(enc) }
        val f = s.foundation.map { it.map(enc) }
        val st = s.stock.map(enc)
        val w = s.waste.map(enc)
        return LayoutId.generate(t, f, st, w, lv = 0)
    }

    private fun suitCode(s: Suit) = when (s) { Suit.SPADES->"S"; Suit.HEARTS->"H"; Suit.DIAMONDS->"D"; Suit.CLUBS->"C" }
    private fun rankCode(r: Rank) = when (r) {
        Rank.ACE->"A"; Rank.TWO->"2"; Rank.THREE->"3"; Rank.FOUR->"4"; Rank.FIVE->"5"; Rank.SIX->"6"; Rank.SEVEN->"7";
        Rank.EIGHT->"8"; Rank.NINE->"9"; Rank.TEN->"10"; Rank.JACK->"J"; Rank.QUEEN->"Q"; Rank.KING->"K"
    }

    fun getSolveStatsSnapshot(outcomeOverride: String? = null): SolveStats {
        val now = System.currentTimeMillis()
        val finished = outcome?.let { finishedAt } ?: outcomeOverride?.let { now }
        val dur = (finished ?: now) - startedAt
        val layoutId = computeLayoutIdForState(gameState)
        return SolveStats(
            dealId = currentDealId,
            seed = currentSeed,
            rules = rules,
            startedAt = startedAt,
            finishedAt = finished,
            durationMs = dur,
            moveCount = moveCount,
            outcome = outcomeOverride ?: outcome,
            layoutId = layoutId,
            clientVersion = null,
            platform = "android"
        )
    }

    fun encodeSolveStats(outcomeOverride: String? = null): String = SolveCodec.encode(getSolveStatsSnapshot(outcomeOverride))
}