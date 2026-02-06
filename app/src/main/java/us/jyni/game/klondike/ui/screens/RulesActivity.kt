package us.jyni.game.klondike.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import us.jyni.R
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.util.sync.RecycleOrder

class RulesActivity : AppCompatActivity() {
    
    private lateinit var currentRules: Ruleset
    private lateinit var drawRadioGroup: RadioGroup
    private lateinit var recycleRadioGroup: RadioGroup 
    private lateinit var redealsRadioGroup: RadioGroup
    private lateinit var foundationToTableauSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)
        
        // Get current rules from intent
        currentRules = intent.getSerializableExtra(GameActivity.EXTRA_RULES) as? Ruleset ?: Ruleset()
        
        setupViews()
        loadCurrentRules()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }
        
        // Find views
        drawRadioGroup = findViewById(R.id.draw_radio_group)
        recycleRadioGroup = findViewById(R.id.recycle_radio_group) 
        redealsRadioGroup = findViewById(R.id.redeals_radio_group)
        foundationToTableauSwitch = findViewById(R.id.foundation_to_tableau_switch)
        val saveButton = findViewById<Button>(R.id.save_rules_button)
        
        saveButton.setOnClickListener {
            saveRulesAndFinish()
        }
    }
    
    private fun loadCurrentRules() {
        // Set draw mode
        when (currentRules.draw) {
            1 -> drawRadioGroup.check(R.id.draw_1)
            3 -> drawRadioGroup.check(R.id.draw_3)
        }
        
        // Set recycle order
        when (currentRules.recycle) {
            RecycleOrder.KEEP -> recycleRadioGroup.check(R.id.recycle_keep)
            RecycleOrder.REVERSE -> recycleRadioGroup.check(R.id.recycle_reverse)
        }
        
        // Set redeals
        when (currentRules.redeals) {
            -1 -> redealsRadioGroup.check(R.id.redeals_unlimited)
            0 -> redealsRadioGroup.check(R.id.redeals_0)
            1 -> redealsRadioGroup.check(R.id.redeals_1)
            2 -> redealsRadioGroup.check(R.id.redeals_2)
            else -> redealsRadioGroup.check(R.id.redeals_unlimited) // 3 이상은 무제한으로 처리
        }
        
        // Set foundation to tableau
        foundationToTableauSwitch.isChecked = currentRules.allowFoundationToTableau
    }
    
    private fun saveRulesAndFinish() {
        // Get selected values from UI
        val selectedDraw = when (drawRadioGroup.checkedRadioButtonId) {
            R.id.draw_1 -> 1
            R.id.draw_3 -> 3
            else -> 1
        }
        
        val selectedRecycle = when (recycleRadioGroup.checkedRadioButtonId) {
            R.id.recycle_keep -> RecycleOrder.KEEP
            R.id.recycle_reverse -> RecycleOrder.REVERSE
            else -> RecycleOrder.REVERSE
        }
        
        val selectedRedeals = when (redealsRadioGroup.checkedRadioButtonId) {
            R.id.redeals_unlimited -> -1
            R.id.redeals_0 -> 0
            R.id.redeals_1 -> 1
            R.id.redeals_2 -> 2
            else -> -1
        }
        
        val selectedFoundationToTableau = foundationToTableauSwitch.isChecked
        
        // Create new ruleset
        val newRules = Ruleset(
            draw = selectedDraw,
            recycle = selectedRecycle,
            redeals = selectedRedeals,
            allowFoundationToTableau = selectedFoundationToTableau
        )
        
        // Return the new rules
        val resultIntent = Intent()
        resultIntent.putExtra(GameActivity.EXTRA_RULES, newRules)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}