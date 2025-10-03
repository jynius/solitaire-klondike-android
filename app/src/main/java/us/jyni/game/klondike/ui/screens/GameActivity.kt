package us.jyni.game.klondike.ui.screens

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import us.jyni.BuildConfig
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import us.jyni.R
import us.jyni.game.klondike.ui.GameViewModel
import us.jyni.game.klondike.ui.components.CardView

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components and observe ViewModel data
        setupUi()
        setupObservers()

        // Restore saved state or start with explicit seed if provided
        var restored = savedInstanceState?.getString(KEY_SAVED_GAME)?.let { data ->
            viewModel.restoreStateString(data)
        } ?: false

        if (!restored) {
            // Try persistent save (process death restoration)
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val persisted = prefs.getString(KEY_PERSISTED_GAME, null)
            if (persisted != null) {
                restored = viewModel.restoreStateString(persisted)
            }
        }

        if (!restored) {
            intent?.getLongExtra(EXTRA_SEED, Long.MIN_VALUE)?.let { raw ->
                if (raw != Long.MIN_VALUE) {
                    viewModel.startGame(raw.toULong())
                }
            }
        }
    }

    private fun setupObservers() {
        val statusText = findViewById<TextView>(R.id.status_text)
        val board = findViewById<GridLayout>(R.id.game_board)
        val debugShowDeal = findViewById<android.widget.CheckBox>(R.id.debug_show_deal)
        val debugCopyDeal = findViewById<Button>(R.id.debug_copy_deal)
        val debugHighlights = findViewById<android.widget.CheckBox>(R.id.debug_highlights)
        val rulesBar = findViewById<View>(R.id.rules_bar)
        val drawSpinner = findViewById<android.widget.Spinner>(R.id.rules_draw_spinner)
        val recycleSpinner = findViewById<android.widget.Spinner>(R.id.rules_recycle_spinner)
        val redealsSpinner = findViewById<android.widget.Spinner>(R.id.rules_redeals_spinner)
        val allowF2T = findViewById<android.widget.CheckBox>(R.id.rules_allow_f2t)

        // Hide debug bar on non-debug builds
        if (!BuildConfig.DEBUG) {
            findViewById<View>(R.id.debug_bar).visibility = View.GONE
            rulesBar.visibility = View.GONE
        }

        // Populate rule spinners (debug only)
        if (BuildConfig.DEBUG) {
            var rulesInit = true
            drawSpinner.adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.rules_draw_1), getString(R.string.rules_draw_3))
            )
            recycleSpinner.adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.rules_recycle_keep), getString(R.string.rules_recycle_reverse))
            )
            redealsSpinner.adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    getString(R.string.rules_redeals_unlimited),
                    getString(R.string.rules_redeals_0),
                    getString(R.string.rules_redeals_1),
                    getString(R.string.rules_redeals_2)
                )
            )

            // Initialize selection from current rules
            val current = viewModel.getRules()
            drawSpinner.setSelection(if (current.draw == 1) 0 else 1)
            recycleSpinner.setSelection(if (current.recycle.name == "KEEP") 0 else 1)
            redealsSpinner.setSelection(
                when (current.redeals) {
                    -1 -> 0
                    0 -> 1
                    1 -> 2
                    else -> 3
                }
            )
            allowF2T.isChecked = current.allowFoundationToTableau

            fun applyRules() {
                if (rulesInit) return
                val draw = if (drawSpinner.selectedItemPosition == 0) 1 else 3
                val recycle = if (recycleSpinner.selectedItemPosition == 0)
                    us.jyni.game.klondike.util.sync.RecycleOrder.KEEP
                else us.jyni.game.klondike.util.sync.RecycleOrder.REVERSE

                val seed = intent?.getLongExtra(EXTRA_SEED, Long.MIN_VALUE)
                val useSeed = seed?.takeIf { it != Long.MIN_VALUE }?.toULong() ?: 0xCAFEBABE_uL

                // Redeals and allow F->T from UI
                val redeals = when (redealsSpinner.selectedItemPosition) {
                    0 -> -1
                    1 -> 0
                    2 -> 1
                    else -> 2
                }
                val allow = allowF2T.isChecked

                // Restart game with same seed and updated rules
                viewModel.startGame(
                    useSeed,
                    us.jyni.game.klondike.util.sync.Ruleset(
                        draw = draw,
                        recycle = recycle,
                        redeals = redeals,
                        allowFoundationToTableau = allow
                    )
                )
                // Persist new state
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_PERSISTED_GAME, viewModel.saveStateString()).apply()
            }

            drawSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long
                ) { applyRules() }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
            recycleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long
                ) { applyRules() }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
            redealsSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long
                ) { applyRules() }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
            allowF2T.setOnCheckedChangeListener { _, _ -> applyRules() }
            // End init guard after setting selections and listeners
            rulesInit = false
        }
    var selectedTableau: Int? = null
    var selectedFromWaste: Boolean = false
    var selectedFoundation: Int? = null
    val tableauViews = arrayOfNulls<View>(7)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Enable long-press to copy Deal ID
                statusText.setOnLongClickListener {
                    val deal = viewModel.dealId()
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Deal ID", deal))
                    Toast.makeText(this@GameActivity, getString(R.string.deal_copied), Toast.LENGTH_SHORT).show()
                    true
                }

                // Copy button mirrors long-press
                debugCopyDeal.setOnClickListener {
                    val deal = viewModel.dealId()
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Deal ID", deal))
                    Toast.makeText(this@GameActivity, getString(R.string.deal_copied), Toast.LENGTH_SHORT).show()
                }

                // Observe state and render
                viewModel.state.collectLatest { s ->
                    // Update status
                    val stock = s.stock.size
                    val waste = s.waste.size
                    val fnd = s.foundation.sumOf { it.size }
                    val dl = viewModel.dealId()
                    val r = viewModel.getRules()
                    val redealsTxt = if (r.redeals < 0) "∞" else r.redeals.toString()
                    val rulesStr = "D${r.draw} R:${r.recycle.name.take(3)} $redealsTxt ${if (r.allowFoundationToTableau) "F→T:on" else "F→T:off"}"
                    val base = "Stock:$stock  Waste:$waste  Foundation:$fnd  $rulesStr"
                    statusText.text = if (debugShowDeal.isChecked) "$base  Deal:$dl" else base

                    // Render board
                    board.removeAllViews()
                    board.rowCount = 2
                    board.columnCount = 7

                    fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

                    fun makeTableau(col: Int, pileCards: List<us.jyni.game.klondike.model.Card>) : View {
                        val pileContainer = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(4), dp(4), dp(4), dp(4))
                            isClickable = true
                            isFocusable = true
                            if (selectedTableau == col) {
                                background = getDrawable(R.drawable.bg_selected)
                            } else {
                                background = null
                            }
                            contentDescription = "tableau_col_$col"
                            setOnClickListener { v ->
                                val prev = selectedTableau
                                if (prev == null && !selectedFromWaste && selectedFoundation == null) {
                                    selectedTableau = col
                                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    // explicit selected background
                                    (tableauViews[col] ?: v).background = getDrawable(R.drawable.bg_selected)
                                } else {
                                    var moved = false
                                    if (prev != null && prev != col) {
                                        moved = viewModel.moveTableauToTableau(prev, col)
                                    } else if (selectedFromWaste) {
                                        moved = viewModel.moveWasteToTableau(col)
                                    } else if (selectedFoundation != null) {
                                        moved = viewModel.moveFoundationToTableau(selectedFoundation!!, col)
                                    }
                                    if (!moved) {
                                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                        Toast.makeText(this@GameActivity, "Invalid move", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // success pulse + subtle scale animation
                                        val targetView = (tableauViews[col] ?: v)
                                        targetView.animate()
                                            .alpha(0.6f)
                                            .scaleX(1.03f)
                                            .scaleY(1.03f)
                                            .setInterpolator(AccelerateDecelerateInterpolator())
                                            .setDuration(140)
                                            .withEndAction {
                                                targetView.animate()
                                                    .alpha(1f)
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .setDuration(140)
                                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                                    .start()
                                            }
                                            .start()
                                    }
                                    // clear previous highlight
                                    if (prev != null) tableauViews[prev]?.background = null
                                    // ensure current is not highlighted if no selection remains
                                    (tableauViews[col] ?: v).background = null
                                    selectedTableau = null
                                    selectedFromWaste = false
                                    selectedFoundation = null
                                }
                            }
                        }
                        tableauViews[col] = pileContainer

                        pileCards.forEach { card ->
                            val cv = CardView(this@GameActivity)
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                if (card.isFaceUp) dp(36) else dp(24)
                            )
                            lp.bottomMargin = if (card.isFaceUp) dp(12) else dp(4)
                            cv.layoutParams = lp
                            cv.setCard(card)
                            cv.contentDescription = "card_${card.rank}_${card.suit}_${if (card.isFaceUp) "up" else "down"}"
                            pileContainer.addView(cv)
                        }
                        return pileContainer
                    }

                    fun makeFoundationSlot(index: Int, cards: List<us.jyni.game.klondike.model.Card>): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(6), dp(6), dp(6), dp(6))
                            isClickable = true
                            contentDescription = "foundation_$index"
                            setOnClickListener { v ->
                                val from = selectedTableau
                                if (from != null) {
                                    val moved = viewModel.moveTableauToFoundation(from, index)
                                    if (!moved) {
                                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                        Toast.makeText(this@GameActivity, "Invalid move", Toast.LENGTH_SHORT).show()
                                    } else {
                                        v.animate()
                                            .alpha(0.6f)
                                            .scaleX(1.04f)
                                            .scaleY(1.04f)
                                            .setDuration(140)
                                            .setInterpolator(AccelerateDecelerateInterpolator())
                                            .withEndAction {
                                                v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                                    .setDuration(140)
                                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                                    .start()
                                            }.start()
                                    }
                                    selectedTableau = null
                                } else if (selectedFoundation == null) {
                                    // select foundation for F->T
                                    selectedFoundation = index
                                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                } else {
                                    // Try waste -> foundation
                                    val moved = viewModel.moveWasteToFoundation(index)
                                    if (!moved) {
                                        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                        Toast.makeText(this@GameActivity, "Invalid move", Toast.LENGTH_SHORT).show()
                                    } else {
                                        v.animate()
                                            .alpha(0.6f)
                                            .scaleX(1.04f)
                                            .scaleY(1.04f)
                                            .setDuration(140)
                                            .setInterpolator(AccelerateDecelerateInterpolator())
                                            .withEndAction {
                                                v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                                    .setDuration(140)
                                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                                    .start()
                                            }.start()
                                    }
                                }
                            }
                        }
                        // show top card if any
                        val top = cards.lastOrNull()
                        val cv = CardView(this@GameActivity)
                        cv.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(28)
                        )
                        top?.let { cv.setCard(it) }
                        container.addView(cv)
                        return container
                    }

                    fun makeStockSlot(): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(6), dp(6), dp(6), dp(6))
                            isClickable = true
                            contentDescription = "stock"
                            setOnClickListener {
                                viewModel.draw()
                            }
                        }
                        val top = s.stock.lastOrNull()
                        val cv = CardView(this@GameActivity)
                        cv.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(28)
                        )
                        top?.let { cv.setCard(it) }
                        container.addView(cv)
                        return container
                    }

                    fun makeWasteSlot(): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(6), dp(6), dp(6), dp(6))
                            contentDescription = "waste"
                            isClickable = true
                            setOnClickListener { v ->
                                selectedFromWaste = true
                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                background = getDrawable(R.drawable.bg_selected)
                            }
                        }
                        val top = s.waste.lastOrNull()
                        val cv = CardView(this@GameActivity)
                        cv.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(28)
                        )
                        top?.let { 
                            cv.setCard(it)
                            cv.contentDescription = "card_${it.rank}_${it.suit}_up"
                            // subtle fade-in on new waste top
                            cv.alpha = 0f
                            cv.animate().alpha(1f).setDuration(150).setInterpolator(AccelerateDecelerateInterpolator()).start()
                        }
                        container.addView(cv)
                        return container
                    }

                    // Row 0: foundations (0..3), spacer at 4, stock at 5, waste at 6
                    repeat(4) { idx ->
                        val v = makeFoundationSlot(idx, s.foundation[idx])
                        // rules-based highlight for foundation targets
                        if (selectedFoundation == idx) {
                            v.background = getDrawable(R.drawable.bg_selected)
                            ViewCompat.setStateDescription(v, "selected")
                            v.alpha = 1f
                        } else if (debugHighlights.isChecked) {
                            val target = when {
                                selectedTableau != null -> viewModel.canMoveTableauToFoundation(selectedTableau!!, idx)
                                selectedFromWaste -> viewModel.canMoveWasteToFoundation(idx)
                                else -> null
                            }
                            v.background = when (target) {
                                true -> getDrawable(R.drawable.bg_highlight_target)
                                false -> getDrawable(R.drawable.bg_highlight_dim)
                                null -> null
                            }
                            ViewCompat.setStateDescription(v, when (target) {
                                true -> "target"
                                false -> "not target"
                                null -> null
                            })
                            v.alpha = 1f
                        } else {
                            v.background = null
                            ViewCompat.setStateDescription(v, null)
                            v.alpha = 1f
                        }
                        val p = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(idx))
                        p.width = 0
                        p.columnSpec = GridLayout.spec(idx, 1f)
                        board.addView(v, p)
                    }
                    run {
                        val stockV = makeStockSlot()
                        val p = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(5))
                        p.width = 0
                        p.columnSpec = GridLayout.spec(5, 1f)
                        board.addView(stockV, p)
                    }
                    run {
                        val wasteV = makeWasteSlot()
                        val p = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(6))
                        p.width = 0
                        p.columnSpec = GridLayout.spec(6, 1f)
                        board.addView(wasteV, p)
                    }

                    // Row 1: tableau 0..6
                    s.tableau.forEachIndexed { col, pile ->
                        val pileContainer = makeTableau(col, pile)
                        // rules-based target highlight for tableau
                        if (selectedTableau == col) {
                            pileContainer.background = getDrawable(R.drawable.bg_selected)
                            ViewCompat.setStateDescription(pileContainer, "selected")
                            pileContainer.alpha = 1f
                        } else if (debugHighlights.isChecked) {
                            val target = when {
                                selectedTableau != null -> viewModel.canMoveTableauToTableau(selectedTableau!!, col)
                                selectedFromWaste -> viewModel.canMoveWasteToTableau(col)
                                selectedFoundation != null -> viewModel.canMoveFoundationToTableau(selectedFoundation!!, col)
                                else -> null
                            }
                            pileContainer.background = when (target) {
                                true -> getDrawable(R.drawable.bg_highlight_target)
                                false -> getDrawable(R.drawable.bg_highlight_dim)
                                null -> null
                            }
                            ViewCompat.setStateDescription(pileContainer, when (target) {
                                true -> "target"
                                false -> "not target"
                                null -> null
                            })
                            pileContainer.alpha = 1f
                        } else {
                            pileContainer.background = null
                            ViewCompat.setStateDescription(pileContainer, null)
                            pileContainer.alpha = 1f
                        }
                        val params = GridLayout.LayoutParams(
                            GridLayout.spec(1),
                            GridLayout.spec(col)
                        )
                        params.width = 0
                        params.height = GridLayout.LayoutParams.WRAP_CONTENT
                        params.columnSpec = GridLayout.spec(col, 1f)
                        board.addView(pileContainer, params)
                    }
                }
            }
        }
    }

    private fun setupUi() {
        fun persist() {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PERSISTED_GAME, viewModel.saveStateString()).apply()
        }
        findViewById<Button>(R.id.draw_button).setOnClickListener { viewModel.draw(); persist() }
        findViewById<Button>(R.id.undo_button).setOnClickListener { viewModel.undo(); persist() }
        findViewById<Button>(R.id.redo_button).setOnClickListener { viewModel.redo(); persist() }
        findViewById<Button>(R.id.reset_button).setOnClickListener { viewModel.reset(); persist() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val saved = viewModel.saveStateString()
        outState.putString(KEY_SAVED_GAME, saved)
        // Also persist to SharedPreferences for process-death restore
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PERSISTED_GAME, saved).apply()
    }

    companion object {
        private const val KEY_SAVED_GAME = "saved_game_sv1"
        private const val PREFS_NAME = "klondike_prefs"
        private const val KEY_PERSISTED_GAME = "persisted_game_sv1"
        const val EXTRA_SEED = "extra_seed"
    }
}