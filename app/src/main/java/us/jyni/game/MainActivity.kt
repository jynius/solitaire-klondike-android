package us.jyni

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import us.jyni.game.klondike.ui.screens.GameActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the game activity
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish()
    }
}