package us.jyni.game.klondike.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.sync.Ruleset

class GameViewModel : ViewModel() {
    private val engine = GameEngine()

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
}
