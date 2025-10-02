package us.jyni.game.klondike.ui.screens

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import us.jyni.R
import us.jyni.game.klondike.ui.GameViewModel

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components and observe ViewModel data
        setupObservers()
    }

    private fun setupObservers() {
        // Observe ViewModel data and update UI accordingly
    }
}