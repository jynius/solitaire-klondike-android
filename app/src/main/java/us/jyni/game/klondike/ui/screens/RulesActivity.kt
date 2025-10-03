package us.jyni.game.klondike.ui.screens

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import us.jyni.R

class RulesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)
        
        setupViews()
    }
    
    private fun setupViews() {
        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "게임 규칙 설정"
        
        // Find views
        val drawRadioGroup = findViewById<RadioGroup>(R.id.draw_radio_group)
        val recycleRadioGroup = findViewById<RadioGroup>(R.id.recycle_radio_group) 
        val redealsRadioGroup = findViewById<RadioGroup>(R.id.redeals_radio_group)
        val foundationToTableauSwitch = findViewById<Switch>(R.id.foundation_to_tableau_switch)
        val saveButton = findViewById<Button>(R.id.save_rules_button)
        
        // Load current preferences (placeholder - implement actual loading)
        // drawRadioGroup.check(R.id.draw_3) // example
        
        saveButton.setOnClickListener {
            // Save rules and return result
            saveRulesAndFinish()
        }
    }
    
    private fun saveRulesAndFinish() {
        // TODO: Save the selected rules to preferences or return as result
        
        // For now, just finish
        setResult(RESULT_OK)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}