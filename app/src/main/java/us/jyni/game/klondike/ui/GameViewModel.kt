package us.jyni.game.klondike.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.sync.Ruleset

class GameViewModel : ViewModel() {
    private val engine = GameEngine()

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        startGame()
    }

    fun startGame(seed: ULong = 0xCAFEBABE_uL, rules: Ruleset = Ruleset()) {
        engine.startGame(seed, rules)
        _state.value = engine.getGameState()
    }

    fun draw() {
        viewModelScope.launch {
            engine.draw()
            _state.value = engine.getGameState()
        }
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
        startGame()
    }

    fun moveTableauToTableau(fromCol: Int, toCol: Int): Boolean {
        val moved = engine.moveTableauToTableau(fromCol, toCol)
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
    fun canMoveTableauToFoundation(fromCol: Int, f: Int) = engine.canMoveTableauToFoundation(fromCol, f)
    fun canMoveWasteToFoundation(f: Int) = engine.canMoveWasteToFoundation(f)
    fun canMoveWasteToTableau(toCol: Int) = engine.canMoveWasteToTableau(toCol)
    fun canMoveFoundationToTableau(f: Int, toCol: Int) = engine.canMoveFoundationToTableau(f, toCol)

    fun dealId(): String = engine.getDealId()

    fun getRules(): Ruleset = engine.getRules()
}