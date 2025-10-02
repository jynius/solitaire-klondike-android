package us.jyni.game.klondike.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import us.jyni.game.klondike.model.GameState

class GameViewModel : ViewModel() {
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> get() = _gameState

    init {
        // 초기 게임 상태 설정
        _gameState.value = GameState()
    }

    fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }

    // 추가적인 UI 관련 메서드 구현 가능
}