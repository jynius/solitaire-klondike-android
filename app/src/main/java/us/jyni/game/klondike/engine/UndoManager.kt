package us.jyni.game.klondike.engine

import us.jyni.game.klondike.model.GameState
class UndoManager {
    private val history: MutableList<GameState> = mutableListOf()
    private var currentStateIndex: Int = -1

    fun saveState(state: GameState) {
        // Remove any states that are ahead of the current index
        if (currentStateIndex < history.size - 1) {
            history.subList(currentStateIndex + 1, history.size).clear()
        }
        history.add(state)
        currentStateIndex++
    }

    fun undo(): GameState? {
        return if (currentStateIndex > 0) {
            currentStateIndex--
            history[currentStateIndex]
        } else {
            null
        }
    }

    fun redo(): GameState? {
        return if (currentStateIndex < history.size - 1) {
            currentStateIndex++
            history[currentStateIndex]
        } else {
            null
        }
    }

    fun clearHistory() {
        history.clear()
        currentStateIndex = -1
    }

    fun hasUndo(): Boolean = currentStateIndex > 0
    fun hasRedo(): Boolean = currentStateIndex in 0 until (history.size - 1)
}