package us.jyni.game.klondike.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.solver.UnsolvableDetector
import us.jyni.game.klondike.solver.UnsolvableReason
import us.jyni.game.klondike.solver.AStarSolver
import us.jyni.game.klondike.solver.SolverResult
import us.jyni.game.klondike.solver.Move

class GameViewModel : ViewModel() {
    private val engine = GameEngine()
    private val unsolvableDetector = UnsolvableDetector(engine)
    private val solver = AStarSolver()

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    // Note: Game is started explicitly from GameActivity after checking for seeds/saves

    fun startGame(seed: ULong = 0xCAFEBABE_uL, rules: Ruleset = Ruleset()) {
        Log.d("GameViewModel", "Starting game with seed: $seed")
        engine.startGame(seed, rules)
        _state.value = engine.getGameState()
        Log.d("GameViewModel", "Game started - Stock: ${_state.value.stock.size}, Waste: ${_state.value.waste.size}")
    }

    fun draw() {
        // Make draw synchronous so Espresso waits for UI state to settle
        android.util.Log.d("GameViewModel", "draw() called")
        val drawn = engine.draw()
        android.util.Log.d("GameViewModel", "draw() returned: $drawn, waste size: ${engine.getGameState().waste.size}")
        _state.value = engine.getGameState()
    }

    fun undo() {
        if (engine.undo()) {
            _state.value = engine.getGameState()
        }
    }

    fun redo() {
        if (engine.redo()) {
            _state.value = engine.getGameState()
        }
    }

    fun reset() {
        // Generate a truly random seed using current time and random
        val randomSeed = (System.currentTimeMillis() xor kotlin.random.Random.nextLong()).toULong()
        Log.d("GameViewModel", "Starting new random game with seed: $randomSeed")
        startGame(randomSeed)
    }

    fun restartGame() {
        // 현재 게임의 시드와 규칙을 유지하면서 다시 시작
        val currentSeed = engine.getSeed() // 현재 시드 가져오기
        val currentRules = engine.getRules() // 현재 규칙 가져오기
        Log.d("GameViewModel", "Restarting game with same seed: $currentSeed")
        engine.startGame(currentSeed, currentRules)
        _state.value = engine.getGameState()
        Log.d("GameViewModel", "Game restarted - Stock: ${_state.value.stock.size}, Waste: ${_state.value.waste.size}")
    }

    fun moveTableauToTableau(fromCol: Int, toCol: Int): Boolean {
        val moved = engine.moveTableauToTableau(fromCol, toCol)
        if (moved) _state.value = engine.getGameState()
        return moved
    }
    
    fun moveTableauToTableauFromIndex(fromCol: Int, cardIndex: Int, toCol: Int): Boolean {
        val moved = engine.moveTableauToTableauFromIndex(fromCol, cardIndex, toCol)
        if (moved) _state.value = engine.getGameState()
        return moved
    }

    fun moveTableauToFoundation(fromCol: Int, foundationIndex: Int): Boolean {
        val moved = engine.moveTableauToFoundation(fromCol, foundationIndex)
        if (moved) _state.value = engine.getGameState()
        return moved
    }

    fun moveWasteToFoundation(foundationIndex: Int): Boolean {
        val moved = engine.moveWasteToFoundation(foundationIndex)
        if (moved) _state.value = engine.getGameState()
        return moved
    }

    fun moveWasteToTableau(toCol: Int): Boolean {
        val moved = engine.moveWasteToTableau(toCol)
        if (moved) _state.value = engine.getGameState()
        return moved
    }

    fun moveFoundationToTableau(foundationIndex: Int, toCol: Int): Boolean {
        val moved = engine.moveFoundationToTableau(foundationIndex, toCol)
        if (moved) _state.value = engine.getGameState()
        return moved
    }

    fun autoComplete(): Int {
        println("[AutoComplete] ===== START =====")
        var moveCount = 0
        var moved = true
        var consecutiveDraws = 0
        val totalCards = _state.value.stock.size + _state.value.waste.size
        val maxConsecutiveDraws = if (totalCards > 0) totalCards else 1
        var movesSinceLastNonDraw = 0  // Draw 이후 다른 이동이 없었던 횟수
        var recycleCount = 0  // Recycle 발생 횟수
        var wasteUsedAfterRecycle = false  // Recycle 후 Waste에서 카드를 실제로 사용했는지
        var drawsAfterRecycle = 0  // Recycle 후 Draw 횟수
        var recycleSize = totalCards  // 초기 카드 수로 설정
        
        android.util.Log.d("GameViewModel", "=== AutoComplete Start ===")
        android.util.Log.d("GameViewModel", "Stock: ${_state.value.stock.size}, Waste: ${_state.value.waste.size}")
        
        while (moved) {
            moved = false
            var madeNonDrawMove = false  // 이번 루프에서 Draw 외의 이동을 했는지
            
            android.util.Log.d("GameViewModel", "--- Loop Start (moveCount=$moveCount) ---")
            
            // 1. Waste에서 Foundation으로 이동 시도
            if (_state.value.waste.isNotEmpty()) {
                android.util.Log.d("GameViewModel", "Checking Waste→Foundation (Waste size: ${_state.value.waste.size})")
                for (foundationIndex in 0..3) {
                    if (engine.canMoveWasteToFoundation(foundationIndex)) {
                        android.util.Log.d("GameViewModel", "Can move Waste→F[$foundationIndex]")
                        if (engine.moveWasteToFoundation(foundationIndex)) {
                            _state.value = engine.getGameState()
                            moveCount++
                            moved = true
                            madeNonDrawMove = true
                            consecutiveDraws = 0
                            movesSinceLastNonDraw = 0
                            wasteUsedAfterRecycle = true
                            drawsAfterRecycle = 0  // Waste 사용 시 Draw 카운터 리셋
                            // recycleCount는 리셋하지 않음 (Waste 사용 후에도 recycle 카운트 유지)
                            android.util.Log.d("GameViewModel", "Moved Waste → F[$foundationIndex]")
                            break
                        }
                    }
                }
            }
            
            // 2. Tableau에서 Foundation으로 이동 시도
            if (!moved) {
                for (col in 0..6) {
                    for (foundationIndex in 0..3) {
                        if (engine.canMoveTableauToFoundation(col, foundationIndex)) {
                            if (engine.moveTableauToFoundation(col, foundationIndex)) {
                                _state.value = engine.getGameState()
                                moveCount++
                                moved = true
                                madeNonDrawMove = true
                                consecutiveDraws = 0
                                // movesSinceLastNonDraw는 리셋하지 않음 (Waste 사용 아님)
                                android.util.Log.d("GameViewModel", "Moved T[$col] → F[$foundationIndex]")
                                break
                            }
                        }
                    }
                    if (moved) break
                }
            }
            
            // 3. Tableau 간 이동 시도 (뒷면 카드를 뒤집기 위해)
            // 우선순위: 뒷면 카드가 있는 경우 먼저, 그 다음 모든 이동
            if (!moved) {
                // 3a. 뒷면 카드를 뒤집을 수 있는 이동 우선
                for (fromCol in 0..6) {
                    val pile = _state.value.tableau[fromCol]
                    if (pile.isEmpty()) continue
                    
                    val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
                    if (firstFaceUpIndex > 0) {
                        // 앞면 카드 뒤에 뒷면 카드가 있음 → 이동하면 뒤집힘
                        for (toCol in 0..6) {
                            if (fromCol != toCol && engine.canMoveTableauToTableau(fromCol, toCol)) {
                                if (engine.moveTableauToTableau(fromCol, toCol)) {
                                    _state.value = engine.getGameState()
                                    moveCount++
                                    moved = true
                                    madeNonDrawMove = true
                                    consecutiveDraws = 0
                                    android.util.Log.d("GameViewModel", "Moved T[$fromCol] → T[$toCol] (to flip)")
                                    break
                                }
                            }
                        }
                        if (moved) break
                    }
                }
                
                // 3b. 뒷면 카드가 없어도, 진행을 위해 다른 Tableau 이동 시도
                // (단, Waste에 카드가 있고 아무 것도 놓을 수 없을 때만)
                if (!moved && _state.value.waste.isNotEmpty()) {
                    // Waste를 놓을 곳이 있는지 먼저 확인
                    var wasteCanBePlaced = false
                    for (col in 0..6) {
                        if (engine.canMoveWasteToTableau(col)) {
                            wasteCanBePlaced = true
                            break
                        }
                    }
                    
                    println("[Step 3b] wasteCanBePlaced=$wasteCanBePlaced, waste.size=${_state.value.waste.size}")
                    
                    // Waste를 놓을 곳이 없으면 Tableau 재배치 시도
                    if (!wasteCanBePlaced) {
                        println("[Step 3b] Trying Tableau rearrangements...")
                        for (fromCol in 0..6) {
                            val pile = _state.value.tableau[fromCol]
                            if (pile.isEmpty()) continue
                            
                            val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
                            if (firstFaceUpIndex >= 0) {
                                for (toCol in 0..6) {
                                    if (fromCol != toCol) {
                                        val canMove = engine.canMoveTableauToTableau(fromCol, toCol)
                                        if (canMove) {
                                            println("[Step 3b] Can move T[$fromCol] → T[$toCol]")
                                        }
                                        if (canMove && engine.moveTableauToTableau(fromCol, toCol)) {
                                            _state.value = engine.getGameState()
                                            moveCount++
                                            moved = true
                                            madeNonDrawMove = true
                                            consecutiveDraws = 0
                                            android.util.Log.d("GameViewModel", "Moved T[$fromCol] → T[$toCol] (rearrange for Waste)")
                                            println("[Step 3b] Moved T[$fromCol] → T[$toCol]!")
                                            break
                                        }
                                    }
                                }
                                if (moved) break
                            }
                        }
                        if (!moved) {
                            println("[Step 3b] No Tableau rearrangements found")
                        }
                    }
                }
            }
            
            // 4. King을 빈 공간으로 이동 (뒷면 카드 노출을 위해)
            if (!moved) {
                val emptyCol = _state.value.tableau.indexOfFirst { it.isEmpty() }
                if (emptyCol != -1) {
                    for (fromCol in 0..6) {
                        val pile = _state.value.tableau[fromCol]
                        if (pile.isEmpty()) continue
                        
                        val firstFaceUpIndex = pile.indexOfFirst { it.isFaceUp }
                        if (firstFaceUpIndex > 0 && firstFaceUpIndex < pile.size) {
                            val card = pile[firstFaceUpIndex]
                            if (card.rank == Rank.KING) {
                                if (engine.moveTableauToTableau(fromCol, emptyCol)) {
                                    _state.value = engine.getGameState()
                                    moveCount++
                                    moved = true
                                    madeNonDrawMove = true
                                    consecutiveDraws = 0
                                    // movesSinceLastNonDraw는 리셋하지 않음 (Waste 사용 아님)
                                    android.util.Log.d("GameViewModel", "Moved King from T[$fromCol] → T[$emptyCol] (empty)")
                                    break
                                }
                            }
                        }
                    }
                }
            }
            
            // 5. Waste에서 Tableau로 이동 시도
            if (!moved && _state.value.waste.isNotEmpty()) {
                val wasteCard = _state.value.waste.last()
                android.util.Log.d("GameViewModel", "Checking Waste→Tableau (Waste size: ${_state.value.waste.size}, top: ${wasteCard.suit} ${wasteCard.rank})")
                // Tableau 상태도 출력
                _state.value.tableau.forEachIndexed { i, pile ->
                    val topCard = pile.lastOrNull { it.isFaceUp }
                    android.util.Log.d("GameViewModel", "  T[$i]: ${pile.size} cards, top=${topCard?.let { "${it.suit} ${it.rank}" } ?: "empty/hidden"}")
                }
                for (toCol in 0..6) {
                    val canMove = engine.canMoveWasteToTableau(toCol)
                    if (canMove) {
                        android.util.Log.d("GameViewModel", "Can move Waste→T[$toCol]")
                    }
                    if (canMove) {
                        if (engine.moveWasteToTableau(toCol)) {
                            _state.value = engine.getGameState()
                            moveCount++
                            moved = true
                            madeNonDrawMove = true
                            consecutiveDraws = 0
                            movesSinceLastNonDraw = 0
                            wasteUsedAfterRecycle = true
                            drawsAfterRecycle = 0  // Waste 사용 시 Draw 카운터 리셋
                            // recycleCount는 리셋하지 않음 (Waste 사용 후에도 recycle 카운트 유지)
                            android.util.Log.d("GameViewModel", "Moved Waste → T[$toCol]")
                            break
                        }
                    }
                }
            }
            
            // 6. Stock에서 Draw (Stock이 비었으면 Waste를 재활용)
            if (!moved && (_state.value.stock.isNotEmpty() || _state.value.waste.isNotEmpty())) {
                val stockWasEmpty = _state.value.stock.isEmpty()
                
                // Recycle 전 체크: Stock이 비어있고, Waste를 사용하지 않았으면 중단
                // 1) 거의 한 사이클을 돌았거나 (draws >= size-1)
                // 2) 아직 Draw를 하지 않았는데 (draws=0) wasteUsed=false이면 (처음부터 Recycle)
                if (stockWasEmpty && !wasteUsedAfterRecycle && recycleSize > 0 &&
                    (drawsAfterRecycle >= recycleSize - 1 || (drawsAfterRecycle == 0 && recycleCount >= 0))) {
                    android.util.Log.d("GameViewModel", "Stock 비었지만 Waste 사용 없음 - Recycle 취소")
                    moved = false
                } else {
                    val drawn = engine.draw()
                    if (drawn > 0) {
                    _state.value = engine.getGameState()
                    moveCount++
                    moved = true
                    consecutiveDraws++
                    if (!madeNonDrawMove) {
                        movesSinceLastNonDraw++
                    }
                    
                    // Recycle 발생 감지
                    if (stockWasEmpty && drawn > 0) {
                        android.util.Log.d("GameViewModel", "Recycle 발생 (count: $recycleCount, wasteUsed: $wasteUsedAfterRecycle, draws: $drawsAfterRecycle)")
                        
                        // Recycle이 발생했는데 Waste를 사용하지 않았으면 중단
                        if (!wasteUsedAfterRecycle && (recycleCount > 0 || drawsAfterRecycle > 0)) {
                            android.util.Log.d("GameViewModel", "Recycle 무한 루프 - Waste 사용 없이 ${recycleCount + 1}번째 recycle")
                            // 이번 Draw는 취소 (무한 루프 방지용)
                            moveCount--
                            moved = false
                        } else {
                            // 새로운 Recycle 사이클 시작
                            recycleCount++
                            wasteUsedAfterRecycle = false
                            drawsAfterRecycle = 0
                            recycleSize = _state.value.stock.size + _state.value.waste.size
                        }
                    } else {
                        // Recycle 아닌 일반 Draw
                        drawsAfterRecycle++
                        
                        // 한 사이클을 돌았는데 Waste를 사용하지 않았으면 중단
                        if (drawsAfterRecycle >= recycleSize && !wasteUsedAfterRecycle) {
                            android.util.Log.d("GameViewModel", "${drawsAfterRecycle}번 Draw했지만 Waste 사용 없음 - 중단")
                            moved = false
                        }
                    }
                    
                    android.util.Log.d("GameViewModel", "Draw (consecutive: $consecutiveDraws/$maxConsecutiveDraws, nonDrawMoves: $movesSinceLastNonDraw)")
                    
                    // Draw만 계속 반복하면 중단 (Stock+Waste를 한 바퀴 돌았는데도 진행이 없으면)
                    if (movesSinceLastNonDraw >= maxConsecutiveDraws) {
                        android.util.Log.d("GameViewModel", "Draw 무한 루프 감지 - 자동 완료 중단 (연속 Draw: $movesSinceLastNonDraw/$maxConsecutiveDraws)")
                        moved = false
                    }
                    } else {
                        // Draw 실패 (Stock도 비었고 Waste도 비었거나 recycle 불가)
                        android.util.Log.d("GameViewModel", "Draw 실패 - 더 이상 뽑을 카드 없음")
                        moved = false
                    }
                }
            }
        }
        
        android.util.Log.d("GameViewModel", "=== AutoComplete End: $moveCount moves ===")
        return moveCount
    }

    fun saveStateString(): String = engine.saveStateString()

    fun restoreStateString(data: String): Boolean {
        val ok = engine.restoreStateString(data)
        if (ok) _state.value = engine.getGameState()
        return ok
    }

    // Preview helpers for UI highlighting
    fun canMoveTableauToTableau(fromCol: Int, toCol: Int) = engine.canMoveTableauToTableau(fromCol, toCol)
    fun canMoveTableauToTableauFromIndex(fromCol: Int, cardIndex: Int, toCol: Int): Boolean {
        // 개별 카드 인덱스부터 이동 가능성 체크를 위한 임시 메서드
        android.util.Log.d("GameEngine", "canMoveTableauToTableauFromIndex: from=$fromCol, cardIndex=$cardIndex, to=$toCol")
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return false
        val src = engine.getGameState().tableau[fromCol]
        val dst = engine.getGameState().tableau[toCol]
        
        if (cardIndex < 0 || cardIndex >= src.size) return false
        
        // 선택된 인덱스부터 끝까지의 카드들을 부분 리스트로 생성
        val partialPile = src.subList(cardIndex, src.size)
        android.util.Log.d("GameEngine", "partialPile size: ${partialPile.size}, cards: ${partialPile.map { "${it.rank}${it.suit}" }}")
        val movableSequence = engine.rulesEngine.getMovableSequence(partialPile)
        android.util.Log.d("GameEngine", "movableSequence size: ${movableSequence.size}, cards: ${movableSequence.map { "${it.rank}${it.suit}" }}")
        
        if (movableSequence.isEmpty()) return false
        
        val canMove = engine.rulesEngine.canMoveSequenceToTableau(movableSequence, dst)
        android.util.Log.d("GameEngine", "canMoveSequenceToTableau result: $canMove")
        return canMove
    }
    fun canMoveTableauToFoundation(fromCol: Int, f: Int) = engine.canMoveTableauToFoundation(fromCol, f)
    fun canMoveWasteToFoundation(f: Int) = engine.canMoveWasteToFoundation(f)
    fun canMoveWasteToTableau(toCol: Int) = engine.canMoveWasteToTableau(toCol)
    fun canMoveFoundationToTableau(f: Int, toCol: Int) = engine.canMoveFoundationToTableau(f, toCol)

    fun dealId(): String = engine.getDealId()

    fun getRules(): Ruleset = engine.getRules()
    
    fun getSeed(): ULong = engine.getSeed()
    
    fun layoutId(): String = engine.getLayoutId()
    
    fun getGameStateJson(): String = engine.getGameStateJson()
    
    /**
     * 현재 게임 상태를 사람이 읽기 쉬운 형태로 출력
     * 디버깅과 테스트 케이스 작성에 사용
     */
    fun getReadableState(): String {
        val state = _state.value
        val sb = StringBuilder()
        
        sb.appendLine("=== 현재 게임 상태 ===")
        sb.appendLine("Deal ID: ${dealId()}")
        sb.appendLine("Layout ID: ${layoutId()}")
        sb.appendLine()
        
        // Foundation
        sb.appendLine("Foundation:")
        state.foundation.forEachIndexed { i, pile ->
            val suitName = when(i) {
                0 -> "♣"
                1 -> "♦"
                2 -> "♥"
                3 -> "♠"
                else -> "?"
            }
            val topCard = pile.lastOrNull()
            val rankName = topCard?.rank?.name ?: "Empty"
            sb.appendLine("  F[$i] $suitName: $rankName (${pile.size}장)")
        }
        sb.appendLine()
        
        // Tableau
        sb.appendLine("Tableau:")
        state.tableau.forEachIndexed { i, pile ->
            val faceUpCount = pile.count { it.isFaceUp }
            val faceDownCount = pile.size - faceUpCount
            sb.append("  T[$i]: ${pile.size}장 (뒷면=$faceDownCount, 앞면=$faceUpCount)")
            
            // 앞면 카드들 표시
            if (faceUpCount > 0) {
                sb.append(" | ")
                pile.filter { it.isFaceUp }.forEach { card ->
                    val suitSymbol = when(card.suit) {
                        Suit.CLUBS -> "♣"
                        Suit.DIAMONDS -> "♦"
                        Suit.HEARTS -> "♥"
                        Suit.SPADES -> "♠"
                    }
                    sb.append("$suitSymbol${card.rank.name} ")
                }
            }
            sb.appendLine()
        }
        sb.appendLine()
        
        // Stock & Waste
        sb.appendLine("Stock: ${state.stock.size}장")
        sb.append("Waste: ${state.waste.size}장")
        if (state.waste.isNotEmpty()) {
            sb.append(" | Top: ")
            val topCard = state.waste.last()
            val suitSymbol = when(topCard.suit) {
                Suit.CLUBS -> "♣"
                Suit.DIAMONDS -> "♦"
                Suit.HEARTS -> "♥"
                Suit.SPADES -> "♠"
            }
            sb.append("$suitSymbol${topCard.rank.name}")
        }
        sb.appendLine()
        sb.appendLine()
        
        // Stats
        sb.appendLine("Stats:")
        sb.appendLine("  Score: ${getScore()}")
        sb.appendLine("  Moves: ${getMoveCount()}")
        sb.appendLine("  Time: ${getElapsedTimeMs() / 1000}s")
        
        return sb.toString()
    }

    // Undo/Redo availability for UI enablement
    fun canUndo(): Boolean = engine.canUndo()
    fun canRedo(): Boolean = engine.canRedo()
    
    // Timer and Score
    fun getElapsedTimeMs(): Long = engine.getElapsedTimeMs()
    fun getScore(): Int = engine.getScore()
    fun getMoveCount(): Int = engine.getMoveCount()
    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun isPaused(): Boolean = engine.isPaused()
    
    // Unsolvable detection
    fun checkUnsolvable(): UnsolvableReason? {
        return unsolvableDetector.check(_state.value)
    }
    
    // Solver
    fun solve(): SolverResult {
        return solver.solve(_state.value)
    }
    
    fun findHint(): Move? {
        val currentState = _state.value
        android.util.Log.d("GameViewModel", "findHint() - Tableau 상태:")
        currentState.tableau.forEachIndexed { i, pile ->
            android.util.Log.d("GameViewModel", "  T[$i]: ${pile.size}장 (앞면=${pile.count { it.isFaceUp }})")
        }
        android.util.Log.d("GameViewModel", "  Stock: ${currentState.stock.size}장, Waste: ${currentState.waste.size}장")
        
        return solver.findBestMove(currentState)
    }
}
