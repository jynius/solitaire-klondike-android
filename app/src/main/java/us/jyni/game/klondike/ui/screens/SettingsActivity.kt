package us.jyni.game.klondike.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import us.jyni.R
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.solver.SolverType
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var currentRules: Ruleset
    private lateinit var drawRadioGroup: RadioGroup
    private lateinit var recycleRadioGroup: RadioGroup 
    private lateinit var redealsRadioGroup: RadioGroup
    private lateinit var foundationToTableauSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var solverSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language before calling super.onCreate
        applyLanguage()
        
        super.onCreate(savedInstanceState)
        
        // Hide action bar for fullscreen experience
        supportActionBar?.hide()
        
        // Hide system navigation bar
        window.insetsController?.let { controller ->
            controller.hide(android.view.WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContentView(R.layout.activity_settings)
        
        // Get current rules from intent
        currentRules = intent.getSerializableExtra(GameActivity.EXTRA_RULES) as? Ruleset ?: Ruleset()
        
        setupViews()
        loadCurrentRules()
        loadCurrentLanguage()
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
        languageRadioGroup = findViewById(R.id.language_radio_group)
        solverSpinner = findViewById(R.id.solver_spinner)
        val saveButton = findViewById<Button>(R.id.save_rules_button)
        
        // Setup solver spinner
        setupSolverSpinner()
        
        // Solver info button tooltip
        findViewById<ImageButton>(R.id.solver_info_button).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.solver_algorithm_title))
                .setMessage(getString(R.string.solver_algorithm_desc))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        
        // Load current settings
        loadCurrentLanguage()
        loadCurrentRules()
        loadCurrentSolver()
        
        // Language change listener
        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLanguage = when (checkedId) {
                R.id.language_korean -> "ko"
                R.id.language_english -> "en"
                else -> "ko"
            }
            
            val currentLanguage = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("language", "ko")
            
            if (newLanguage != currentLanguage) {
                changeLanguage(newLanguage)
            }
        }
        
        saveButton.setOnClickListener {
            saveSolver()
            saveRulesAndFinish()
        }
    }
    
    private fun setupSolverSpinner() {
        val solverNames = arrayOf(
            getString(R.string.solver_bfs),
            getString(R.string.solver_backward),
            getString(R.string.solver_heuristic),
            getString(R.string.solver_astar),
            getString(R.string.solver_greedy)
        )
        
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, solverNames) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.textSize = 14f
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.textSize = 14f
                view.setBackgroundColor(android.graphics.Color.WHITE)
                view.setTextColor(android.graphics.Color.BLACK)
                view.setPadding(16, 16, 16, 16)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        solverSpinner.adapter = adapter
    }
    
    private fun loadCurrentSolver() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val solverType = prefs.getString("solver_type", SolverType.ASTAR.name) ?: SolverType.ASTAR.name
        
        val position = when (solverType) {
            SolverType.BFS.name -> 0
            SolverType.BACKWARD.name -> 1
            SolverType.HEURISTIC.name -> 2
            SolverType.ASTAR.name -> 3
            SolverType.GREEDY.name -> 4
            else -> 3 // Default to A*
        }
        
        solverSpinner.setSelection(position)
    }
    
    private fun saveSolver() {
        val selectedSolver = when (solverSpinner.selectedItemPosition) {
            0 -> SolverType.BFS.name
            1 -> SolverType.BACKWARD.name
            2 -> SolverType.HEURISTIC.name
            3 -> SolverType.ASTAR.name
            4 -> SolverType.GREEDY.name
            else -> SolverType.ASTAR.name
        }
        
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("solver_type", selectedSolver)
            .apply()
    }
    
    private fun loadCurrentLanguage() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentLanguage = prefs.getString("language", "ko") ?: "ko"
        
        when (currentLanguage) {
            "ko" -> languageRadioGroup.check(R.id.language_korean)
            "en" -> languageRadioGroup.check(R.id.language_english)
            else -> languageRadioGroup.check(R.id.language_korean)
        }
    }
    
    private fun changeLanguage(languageCode: String) {
        // Save language preference
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language", languageCode)
            .apply()
        
        // Update locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Show toast and restart app
        Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
        
        // Restart the entire app
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
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

    private fun applyLanguage() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "ko") ?: "ko"
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}