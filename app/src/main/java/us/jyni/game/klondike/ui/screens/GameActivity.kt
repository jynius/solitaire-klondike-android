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
import android.content.Intent
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View.DragShadowBuilder
import android.graphics.Color
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
import us.jyni.game.klondike.util.sync.Ruleset

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    
    // 선택 상태 관리 변수들
    private var selectedTableau: Int? = null
    private var selectedCardIndex: Int? = null  // 선택된 카드의 인덱스
    private var selectedFromWaste: Boolean = false
    private var selectedFoundation: Int? = null
    private val tableauViews = arrayOfNulls<View>(7)
    
    // 드래그 앤 드롭 관련 변수들
    private var isDragging = false
    private var dragSourceType: DragSourceType? = null
    private var dragSourceIndex: Int? = null
    private var dragCardIndex: Int? = null
    
    // 더블클릭 자동 이동 관련 변수들
    private var lastClickTime: Long = 0
    private var lastClickedCard: Pair<Int, Int>? = null // (column, cardIndex)
    private var victoryShown = false
    
    enum class DragSourceType {
        TABLEAU, WASTE, FOUNDATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide action bar for fullscreen game experience
        supportActionBar?.hide()
        
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
        val rulesText = findViewById<TextView>(R.id.rules_text)
        val layoutText = findViewById<TextView>(R.id.layout_text)
        val board = findViewById<GridLayout>(R.id.game_board)
        val debugCopyDeal = findViewById<Button>(R.id.debug_copy_deal)
        val debugCopyLayout = findViewById<Button>(R.id.debug_copy_layout)
        val debugHighlights = findViewById<android.widget.CheckBox>(R.id.debug_highlights)
        
        // Layout Message 폰트 크기를 작게 설정
        layoutText.textSize = 10f

        // Hide debug toggle button on non-debug builds
        if (!BuildConfig.DEBUG) {
            findViewById<View>(R.id.debug_toggle_button).visibility = View.GONE
        }

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

                // Copy buttons
                debugCopyDeal.setOnClickListener {
                    val deal = viewModel.dealId()
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Deal ID", deal))
                    Toast.makeText(this@GameActivity, "Deal ID 복사됨", Toast.LENGTH_SHORT).show()
                }
                
                debugCopyLayout.setOnClickListener {
                    val gameStateJson = viewModel.getGameStateJson()
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Game State JSON", gameStateJson))
                    Toast.makeText(this@GameActivity, "JSON 복사됨", Toast.LENGTH_SHORT).show()
                }

                // Observe state and render
                viewModel.state.collectLatest { s ->
                    // 게임 완료 체크 및 축하 메시지
                    if (s.isGameOver) {
                        showVictoryDialog()
                    } else {
                        // 자동 완료 가능한지 체크 (게임이 끝나지 않았을 때만)
                        autoCompleteIfPossible()
                    }
                    
                    // Update debug info (3-line format)
                    val stock = s.stock.size
                    val waste = s.waste.size
                    val fnd = s.foundation.sumOf { it.size }
                    val r = viewModel.getRules()
                    val redealsTxt = if (r.redeals < 0) "∞" else r.redeals.toString()
                    
                    // Line 1: Rules
                    val rulesStr = "Rules: D${r.draw} R:${r.recycle.name.take(3)} Redeals:$redealsTxt F→T:${if (r.allowFoundationToTableau) "on" else "off"}"
                    rulesText.text = rulesStr
                    
                    // Line 2: Card counts
                    val countsStr = "Cards: Stock:$stock  Waste:$waste  Foundation:$fnd  Tableau:${52 - stock - waste - fnd}"
                    statusText.text = countsStr
                    
                    // Line 3: Game State JSON
                    val gameStateJson = viewModel.getGameStateJson()
                    layoutText.text = gameStateJson
                    
                    // Update button states
                    findViewById<Button>(R.id.undo_button).isEnabled = viewModel.canUndo()
                    findViewById<Button>(R.id.redo_button).isEnabled = viewModel.canRedo()

                    // Render board
                    board.removeAllViews()
                    board.rowCount = 2
                    board.columnCount = 7

                    fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

                    fun makeTableau(col: Int, pileCards: List<us.jyni.game.klondike.model.Card>) : View {
                        val pileContainer = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(2), dp(2), dp(2), dp(2))  // reduced from dp(4)
                            isClickable = true
                            isFocusable = true
                            // 빈 열도 클릭 가능하도록 최소 높이 설정
                            minimumHeight = dp(80)
                            if (selectedTableau == col) {
                                background = getDrawable(R.drawable.bg_selected)
                            } else {
                                background = null
                            }
                            contentDescription = "tableau_col_$col"
                            
                            // 드롭 리스너 추가
                            setOnDragListener { view, event ->
                                handleTableauDrop(view, event, col)
                            }
                            setOnClickListener { v ->
                                val prev = selectedTableau
                                if (prev == null && !selectedFromWaste && selectedFoundation == null) {
                                    // 개별 카드가 이미 선택되지 않은 경우에만 전체 컬럼 선택
                                    if (selectedCardIndex == null) {
                                        selectedTableau = col
                                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        // explicit selected background
                                        (tableauViews[col] ?: v).background = getDrawable(R.drawable.bg_selected)
                                    }
                                } else {
                                    var moved = false
                                    if (prev != null && prev != col) {
                                        // 특정 카드가 선택된 경우 해당 인덱스부터 이동
                                        moved = if (selectedCardIndex != null) {
                                            viewModel.moveTableauToTableauFromIndex(prev, selectedCardIndex!!, col)
                                        } else {
                                            viewModel.moveTableauToTableau(prev, col)
                                        }
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
                                    // 모든 선택 상태 해제
                                    clearAllSelections()
                                }
                            }
                        }
                        tableauViews[col] = pileContainer

                        // 빈 열인 경우 최소 높이만 설정
                        if (pileCards.isEmpty()) {
                            // 빈 카드 표시 제거 - minimumHeight로 충분
                        }

                        pileCards.forEachIndexed { index, card ->
                            val cv = CardView(this@GameActivity)
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            
                            // 첫 번째 카드가 아니라면 겹치게 표시
                            if (index > 0) {
                                val prevCard = pileCards[index - 1]
                                // 이전 카드가 뒤집어져 있으면 뒤집어진 카드의 간격 사용
                                lp.topMargin = if (!prevCard.isFaceUp) -dp(64) else -dp(58) // 80dp 정사각형에 맞게 조정
                            }
                            
                            cv.layoutParams = lp
                            cv.setCard(card)
                            cv.contentDescription = "card_${card.rank}_${card.suit}_${if (card.isFaceUp) "up" else "down"}"
                            
                            // 개별 카드에 드래그 기능 추가 (face-up 카드만)
                            if (card.isFaceUp) {
                                var startTime = 0L
                                cv.setOnTouchListener { view, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            startTime = System.currentTimeMillis()
                                            true
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - startTime > 200) { // 200ms 후 드래그 시작
                                                // 드래그 시작 준비
                                                val dragData = ClipData.newPlainText("card_drag", "tableau_${col}_$index")
                                                val shadowBuilder = DragShadowBuilder(view)
                                                
                                                // 드래그 상태 설정
                                                isDragging = true
                                                dragSourceType = DragSourceType.TABLEAU
                                                dragSourceIndex = col
                                                dragCardIndex = index
                                                
                                                // 드래그 시작
                                                view.startDragAndDrop(dragData, shadowBuilder, view, 0)
                                                
                                                // 시각적 피드백
                                                view.alpha = 0.5f
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                
                                                true
                                            } else false
                                        }
                                        MotionEvent.ACTION_UP -> {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - startTime < 200) { // 빠른 탭 = 클릭
                                                handleDoubleClick(col, index)
                                            }
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            }
                            
                            pileContainer.addView(cv)
                        }
                        
                        // 클릭 가능한 빈 공간을 위해 하단에 패딩 추가
                        if (pileCards.isNotEmpty()) {
                            val spacer = View(this@GameActivity)
                            spacer.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dp(40) // 40dp 높이의 클릭 가능한 공간
                            )
                            pileContainer.addView(spacer)
                        }
                        return pileContainer
                    }

                    fun makeFoundationSlot(index: Int, cards: List<us.jyni.game.klondike.model.Card>): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(2), dp(2), dp(2), dp(2))  // reduced from dp(6)
                            isClickable = true
                            contentDescription = "foundation_$index"
                            
                            // Foundation에 드롭 리스너 추가
                            setOnDragListener { view, event ->
                                handleFoundationDrop(view, event, index)
                            }
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
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        top?.let { 
                            cv.setCard(it) 
                            cv.contentDescription = "foundation_card_${it.rank}_${it.suit}"
                            
                            // Foundation 카드에 드래그 기능 추가 (Foundation to Tableau 규칙이 활성화된 경우)
                            if (viewModel.getRules().allowFoundationToTableau) {
                                cv.setOnTouchListener { view, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            val dragData = ClipData.newPlainText("card_drag", "foundation_$index")
                                            val shadowBuilder = DragShadowBuilder(view)
                                            
                                            isDragging = true
                                            dragSourceType = DragSourceType.FOUNDATION
                                            dragSourceIndex = index
                                            dragCardIndex = null
                                            
                                            view.startDragAndDrop(dragData, shadowBuilder, view, 0)
                                            view.alpha = 0.5f
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }
                        }
                        container.addView(cv)
                        return container
                    }

                    fun makeStockSlot(): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(2), dp(2), dp(2), dp(2))  // reduced from dp(6)
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
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        if (top != null) {
                            cv.setCard(top)
                        } else {
                            // Stock이 비어있을 때도 빈 카드 배경 표시 (재분배를 위해)
                            cv.setEmpty()
                        }
                        container.addView(cv)
                        return container
                    }

                    fun makeWasteSlot(): View {
                        val container = LinearLayout(this@GameActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(2), dp(2), dp(2), dp(2))  // reduced from dp(6)
                            contentDescription = "waste"
                            isClickable = true
                            setOnClickListener { v ->
                                // 이전 선택 해제
                                clearAllSelections()
                                
                                selectedFromWaste = true
                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                background = getDrawable(R.drawable.bg_selected)
                            }
                        }
                        val top = s.waste.lastOrNull()
                        top?.let { card ->
                            val cv = CardView(this@GameActivity)
                            cv.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            cv.setCard(card)
                            cv.contentDescription = "card_${card.rank}_${card.suit}_up"
                            // subtle fade-in on new waste top
                            cv.alpha = 0f
                            cv.animate().alpha(1f).setDuration(150).setInterpolator(AccelerateDecelerateInterpolator()).start()
                            
                            // Waste 카드에 드래그 기능 추가
                            var startTime = 0L
                            cv.setOnTouchListener { view, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        startTime = System.currentTimeMillis()
                                        true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - startTime > 200) { // 200ms 후 드래그 시작
                                            val dragData = ClipData.newPlainText("card_drag", "waste")
                                            val shadowBuilder = DragShadowBuilder(view)
                                            
                                            isDragging = true
                                            dragSourceType = DragSourceType.WASTE
                                            dragSourceIndex = null
                                            dragCardIndex = null
                                            
                                            view.startDragAndDrop(dragData, shadowBuilder, view, 0)
                                            view.alpha = 0.5f
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            
                                            true
                                        } else false
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - startTime < 200) { // 빠른 탭 = 클릭
                                            handleWasteDoubleClick()
                                        }
                                        false
                                    }
                                    else -> false
                                }
                            }
                            container.addView(cv)
                        }
                        return container
                    }

                    // Row 0: foundations (0..3), spacer at 4, waste at 5, stock at 6
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
                        val p = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(6))
                        p.width = 0
                        p.columnSpec = GridLayout.spec(6, 1f)
                        board.addView(stockV, p)
                    }
                    run {
                        val wasteV = makeWasteSlot()
                        val p = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(5))
                        p.width = 0
                        p.columnSpec = GridLayout.spec(5, 1f)
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
        // 하단 컨트롤 버튼들
        findViewById<Button>(R.id.restart_button).setOnClickListener { 
            viewModel.restartGame()
            Toast.makeText(this@GameActivity, "새로시작!", Toast.LENGTH_SHORT).show()
            victoryShown = false  // 승리 상태 초기화
            persist() 
        }
        findViewById<Button>(R.id.reset_button).setOnClickListener { viewModel.reset(); persist() }
        findViewById<Button>(R.id.redo_button).setOnClickListener { viewModel.redo(); persist() }
        findViewById<Button>(R.id.undo_button).setOnClickListener { viewModel.undo(); persist() }
        
        // 디버그 토글 버튼 - floating debug panel 토글
        findViewById<Button>(R.id.debug_toggle_button).setOnClickListener {
            val floatingDebugPanel = findViewById<LinearLayout>(R.id.floating_debug_panel)
            floatingDebugPanel.visibility = if (floatingDebugPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        
        // 규칙 설정 버튼
        findViewById<Button>(R.id.rules_button).setOnClickListener {
            val intent = Intent(this, RulesActivity::class.java)
            intent.putExtra(EXTRA_RULES, viewModel.getRules())
            startActivityForResult(intent, REQUEST_CODE_RULES)
        }
    }
    
    private fun clearAllSelections() {
        // 모든 카드 선택 상태 해제
        for (i in 0..6) {
            tableauViews[i]?.let { view ->
                if (view is LinearLayout) {
                    for (j in 0 until view.childCount) {
                        val child = view.getChildAt(j)
                        if (child is CardView) {
                            child.setCardSelected(false)
                        }
                    }
                }
                view.background = null
            }
        }
        selectedTableau = null
        selectedCardIndex = null
        selectedFromWaste = false
        selectedFoundation = null
    }
    
    private fun handleTableauDrop(view: View, event: DragEvent, targetCol: Int): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // 드래그가 시작되었을 때 - 이 뷰가 드롭을 받을 수 있는지 확인
                isDragging
            }
            
            DragEvent.ACTION_DRAG_ENTERED -> {
                // 드래그가 뷰 위에 진입했을 때 - 시각적 피드백
                if (isDragging && canDropOnTableau(targetCol)) {
                    view.setBackgroundColor(Color.parseColor("#4CAF50")) // 녹색으로 하이라이트
                    view.alpha = 0.8f
                }
                true
            }
            
            DragEvent.ACTION_DRAG_EXITED -> {
                // 드래그가 뷰를 벗어났을 때 - 시각적 피드백 제거
                view.background = null
                view.alpha = 1f
                true
            }
            
            DragEvent.ACTION_DROP -> {
                // 드롭이 발생했을 때 - 실제 카드 이동 수행
                view.background = null
                view.alpha = 1f
                
                if (isDragging && canDropOnTableau(targetCol)) {
                    val moved = when (dragSourceType) {
                        DragSourceType.TABLEAU -> {
                            if (dragSourceIndex != null && dragCardIndex != null) {
                                viewModel.moveTableauToTableauFromIndex(dragSourceIndex!!, dragCardIndex!!, targetCol)
                            } else false
                        }
                        DragSourceType.WASTE -> {
                            viewModel.moveWasteToTableau(targetCol)
                        }
                        DragSourceType.FOUNDATION -> {
                            if (dragSourceIndex != null) {
                                viewModel.moveFoundationToTableau(dragSourceIndex!!, targetCol)
                            } else false
                        }
                        null -> false
                    }
                    
                    if (moved) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
                    }
                }
                
                // 드래그 상태 초기화
                resetDragState()
                true
            }
            
            DragEvent.ACTION_DRAG_ENDED -> {
                // 드래그가 끝났을 때 - 상태 초기화
                view.background = null
                view.alpha = 1f
                resetDragState()
                true
            }
            
            else -> false
        }
    }
    
    private fun handleFoundationDrop(view: View, event: DragEvent, targetFoundation: Int): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                isDragging
            }
            
            DragEvent.ACTION_DRAG_ENTERED -> {
                if (isDragging && canDropOnFoundation(targetFoundation)) {
                    view.setBackgroundColor(Color.parseColor("#2196F3")) // 파란색으로 하이라이트
                    view.alpha = 0.8f
                }
                true
            }
            
            DragEvent.ACTION_DRAG_EXITED -> {
                view.background = null
                view.alpha = 1f
                true
            }
            
            DragEvent.ACTION_DROP -> {
                view.background = null
                view.alpha = 1f
                
                if (isDragging && canDropOnFoundation(targetFoundation)) {
                    val moved = when (dragSourceType) {
                        DragSourceType.TABLEAU -> {
                            if (dragSourceIndex != null) {
                                viewModel.moveTableauToFoundation(dragSourceIndex!!, targetFoundation)
                            } else false
                        }
                        DragSourceType.WASTE -> {
                            viewModel.moveWasteToFoundation(targetFoundation)
                        }
                        DragSourceType.FOUNDATION -> false // Foundation to Foundation은 불가능
                        null -> false
                    }
                    
                    if (moved) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
                    }
                }
                
                resetDragState()
                true
            }
            
            DragEvent.ACTION_DRAG_ENDED -> {
                view.background = null
                view.alpha = 1f
                resetDragState()
                true
            }
            
            else -> false
        }
    }
    
    private fun canDropOnTableau(targetCol: Int): Boolean {
        return when (dragSourceType) {
            DragSourceType.TABLEAU -> {
                if (dragSourceIndex != null && dragCardIndex != null && dragSourceIndex != targetCol) {
                    viewModel.canMoveTableauToTableauFromIndex(dragSourceIndex!!, dragCardIndex!!, targetCol)
                } else false
            }
            DragSourceType.WASTE -> {
                viewModel.canMoveWasteToTableau(targetCol)
            }
            DragSourceType.FOUNDATION -> {
                if (dragSourceIndex != null) {
                    viewModel.canMoveFoundationToTableau(dragSourceIndex!!, targetCol)
                } else false
            }
            null -> false
        }
    }
    
    private fun canDropOnFoundation(targetFoundation: Int): Boolean {
        return when (dragSourceType) {
            DragSourceType.TABLEAU -> {
                if (dragSourceIndex != null) {
                    viewModel.canMoveTableauToFoundation(dragSourceIndex!!, targetFoundation)
                } else false
            }
            DragSourceType.WASTE -> {
                viewModel.canMoveWasteToFoundation(targetFoundation)
            }
            DragSourceType.FOUNDATION -> false // Foundation to Foundation은 불가능
            null -> false
        }
    }
    
    private fun resetDragState() {
        // 드래그 중이던 모든 뷰의 알파값을 1.0으로 복원
        restoreAllViewsAlpha()
        
        isDragging = false
        dragSourceType = null
        dragSourceIndex = null
        dragCardIndex = null
    }
    
    private fun restoreAllViewsAlpha() {
        // Tableau 카드들의 알파값 복원
        for (i in 0..6) {
            tableauViews[i]?.let { view ->
                if (view is LinearLayout) {
                    for (j in 0 until view.childCount) {
                        val child = view.getChildAt(j)
                        if (child is CardView) {
                            child.alpha = 1f
                        }
                    }
                }
            }
        }
        
        // Waste와 Foundation 카드들도 복원 (다음 UI 업데이트에서 자동으로 처리됨)
    }
    
    private fun showVictoryDialog() {
        if (victoryShown) return
        victoryShown = true
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🎉 축하합니다!")
        builder.setMessage("Klondike Solitaire를 완료했습니다!\n\n새 게임을 시작하시겠습니까?")
        builder.setPositiveButton("새 게임") { _, _ ->
            victoryShown = false
            viewModel.reset()
        }
        builder.setNegativeButton("계속 보기") { _, _ ->
            victoryShown = false
        }
        builder.setCancelable(false)
        builder.show()
    }
    
    private fun tryAutoMoveToFoundation(sourceType: DragSourceType, sourceIndex: Int? = null) {
        when (sourceType) {
            DragSourceType.TABLEAU -> {
                if (sourceIndex != null) {
                    for (foundationIndex in 0..3) {
                        if (viewModel.canMoveTableauToFoundation(sourceIndex, foundationIndex)) {
                            if (viewModel.moveTableauToFoundation(sourceIndex, foundationIndex)) {
                                return
                            }
                        }
                    }
                }
            }
            DragSourceType.WASTE -> {
                for (foundationIndex in 0..3) {
                    if (viewModel.canMoveWasteToFoundation(foundationIndex)) {
                        if (viewModel.moveWasteToFoundation(foundationIndex)) {
                            return
                        }
                    }
                }
            }
            DragSourceType.FOUNDATION -> {
                // Foundation에서는 자동 이동하지 않음
            }
        }
    }
    
    private fun handleDoubleClick(column: Int, cardIndex: Int) {
        val currentTime = System.currentTimeMillis()
        val wasRecentClick = (currentTime - lastClickTime < 500) &&
                lastClickedCard?.first == column && lastClickedCard?.second == cardIndex
        
        lastClickTime = currentTime
        lastClickedCard = Pair(column, cardIndex)
        
        if (wasRecentClick) {
            // 더블클릭 감지됨 - 자동으로 Foundation으로 이동 시도
            tryAutoMoveToFoundation(DragSourceType.TABLEAU, column)
        }
    }
    
    private fun handleWasteDoubleClick() {
        val currentTime = System.currentTimeMillis()
        val wasRecentClick = (currentTime - lastClickTime < 500) &&
                lastClickedCard == null // Waste 클릭을 나타내기 위해 null 사용
        
        lastClickTime = currentTime
        lastClickedCard = null
        
        if (wasRecentClick) {
            tryAutoMoveToFoundation(DragSourceType.WASTE)
        }
    }
    
    private fun autoCompleteIfPossible(): Boolean {
        val state = viewModel.state.value
        
        // 모든 Tableau 카드가 뒷면이 없고(face-up), Stock과 Waste가 비어있는지 확인
        val allCardsRevealed = state.tableau.all { pile -> 
            pile.all { card -> card.isFaceUp } 
        }
        val stockAndWasteEmpty = state.stock.isEmpty() && state.waste.isEmpty()
        
        if (allCardsRevealed && stockAndWasteEmpty) {
            // 자동 완료 가능 - 모든 카드를 Foundation으로 이동
            var moved = true
            while (moved) {
                moved = false
                // Tableau에서 Foundation으로 이동 가능한 카드 찾기
                for (col in 0..6) {
                    for (foundationIndex in 0..3) {
                        if (viewModel.canMoveTableauToFoundation(col, foundationIndex)) {
                            viewModel.moveTableauToFoundation(col, foundationIndex)
                            moved = true
                            break
                        }
                    }
                    if (moved) break
                }
            }
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RULES && resultCode == RESULT_OK && data != null) {
            val newRules = data.getSerializableExtra(EXTRA_RULES) as? Ruleset
            if (newRules != null) {
                Log.d("GameActivity", "New rules received: $newRules")
                // 새로운 규칙으로 게임 시작
                viewModel.startGame(viewModel.getSeed(), newRules)
                Toast.makeText(this, "규칙이 업데이트되었습니다. 새 게임이 시작됩니다.", Toast.LENGTH_SHORT).show()
            }
        }
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
        private const val REQUEST_CODE_RULES = 1001
        const val EXTRA_RULES = "extra_rules"
    }
}