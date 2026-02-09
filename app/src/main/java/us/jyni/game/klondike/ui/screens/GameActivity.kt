package us.jyni.game.klondike.ui.screens

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import android.widget.Button
import android.widget.ImageButton
import android.widget.GridLayout
import android.widget.ImageView
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
import android.view.ViewGroup
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
import us.jyni.game.klondike.solver.SolverResult
import us.jyni.game.klondike.solver.Move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import us.jyni.game.klondike.sync.JsonlFileRepository

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    private val solverScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repository: JsonlFileRepository
    
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
    
    // 게임 기록 관련 변수들
    private var gameStartTime: Long = 0
    private var currentGameSeed: ULong = 0u
    
    enum class DragSourceType {
        TABLEAU, WASTE, FOUNDATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repository
        repository = JsonlFileRepository(this)
        
        // Hide action bar for fullscreen game experience
        supportActionBar?.hide()
        
        // Hide system navigation bar
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
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
            val seed = intent?.getLongExtra(EXTRA_SEED, Long.MIN_VALUE)
            if (seed != null && seed != Long.MIN_VALUE) {
                viewModel.startGame(seed.toULong())
                startNewGame(seed.toULong())
            } else {
                // 저장된 상태도 없고 시드도 없으면 새 랜덤 게임 시작
                viewModel.reset()
                startNewGame(viewModel.getSeed())
            }
        } else {
            // 복원된 게임
            startNewGame(viewModel.getSeed())
        }
    }

    private fun setupObservers() {
        val statusText = findViewById<TextView>(R.id.status_text)
        val rulesText = findViewById<TextView>(R.id.rules_text)
        val layoutText = findViewById<TextView>(R.id.layout_text)
        val board = findViewById<GridLayout>(R.id.game_board)
        val debugCopyDeal = findViewById<Button>(R.id.debug_copy_deal)
        val debugCopyLayout = findViewById<Button>(R.id.debug_copy_layout)
        
        // Layout Message 폰트 크기를 작게 설정
        layoutText.textSize = 10f
        
        // Timer coroutine - updates every second
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    if (!viewModel.isPaused()) {
                        // Force state refresh to update timer display
                        updateTimerAndScore()
                    }
                }
            }
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
                
                // Check stats button
                findViewById<Button>(R.id.debug_check_stats).setOnClickListener {
                    val allStats = repository.readAllStats()
                    android.util.Log.d("GameActivity", "=== Stats Check ===")
                    android.util.Log.d("GameActivity", "Total games: ${allStats.size}")
                    android.util.Log.d("GameActivity", "Current moveCount: ${viewModel.getMoveCount()}")
                    android.util.Log.d("GameActivity", "Current seed: $currentGameSeed")
                    android.util.Log.d("GameActivity", "Victory shown: $victoryShown")
                    
                    allStats.takeLast(5).forEach { stat ->
                        android.util.Log.d("GameActivity", "  - ${stat.outcome}: seed=${stat.seed}, moves=${stat.moveCount}, time=${stat.durationMs}ms")
                    }
                    
                    Toast.makeText(this@GameActivity, "기록: 총 ${allStats.size}개 (로그 확인)", Toast.LENGTH_LONG).show()
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
                    
                    // Update Timer and Score in header (if available)
                    val elapsed = viewModel.getElapsedTimeMs()
                    val minutes = (elapsed / 60000).toInt()
                    val seconds = ((elapsed % 60000) / 1000).toInt()
                    val score = viewModel.getScore()
                    val moves = viewModel.getMoveCount()
                    val timeStr = String.format("%02d:%02d", minutes, seconds)
                    
                    findViewById<TextView>(R.id.timer_text)?.text = timeStr
                    findViewById<TextView>(R.id.score_text)?.text = String.format("%,d", score)
                    findViewById<TextView>(R.id.moves_text)?.text = moves.toString()
                    
                    // Line 2: Card counts (simplified)
                    val countsStr = "Stock:$stock  Waste:$waste  Foundation:$fnd  Tableau:${52 - stock - waste - fnd}"
                    statusText.text = countsStr
                    
                    // Line 3: Game State JSON
                    val gameStateJson = viewModel.getGameStateJson()
                    layoutText.text = gameStateJson
                    
                    // Update button states
                    findViewById<ImageButton>(R.id.undo_button).isEnabled = viewModel.canUndo()

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
                                    }
                                    clearAllSelections()
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
                                    }
                                    selectedFromWaste = false
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
        
        // 즐겨찾기 상태 표시 업데이트
        updateFavoriteIndicator()
        
        findViewById<ImageButton>(R.id.restart_button).setOnClickListener {
            val currentMoveCount = viewModel.getMoveCount()
            android.util.Log.d("GameActivity", "Restart button clicked: moveCount=$currentMoveCount, victoryShown=$victoryShown")
            // 현재 게임을 포기로 기록
            if (currentMoveCount > 0 && !victoryShown) {
                android.util.Log.d("GameActivity", "Saving current game as resign")
                saveGameResult("resign")
            } else {
                android.util.Log.d("GameActivity", "Not saving: moveCount=$currentMoveCount, victoryShown=$victoryShown")
            }
            
            viewModel.restartGame()
            Toast.makeText(this@GameActivity, "같은 배치로 다시 시작합니다", Toast.LENGTH_SHORT).show()
            victoryShown = false  // 승리 상태 초기화
            persist()
            startNewGame(currentGameSeed)
            updateFavoriteIndicator()
        }
        findViewById<ImageButton>(R.id.reset_button).setOnClickListener {
            val currentMoveCount = viewModel.getMoveCount()
            android.util.Log.d("GameActivity", "Reset button clicked: moveCount=$currentMoveCount, victoryShown=$victoryShown")
            // 현재 게임을 포기로 기록
            if (currentMoveCount > 0 && !victoryShown) {
                android.util.Log.d("GameActivity", "Saving current game as resign")
                saveGameResult("resign")
            } else {
                android.util.Log.d("GameActivity", "Not saving: moveCount=$currentMoveCount, victoryShown=$victoryShown")
            }
            
            viewModel.reset()
            persist()
            startNewGame(viewModel.getSeed())
            updateFavoriteIndicator()
        }
        findViewById<ImageButton>(R.id.undo_button).setOnClickListener {
            viewModel.undo()
            persist()
        }
        
        // Hint button - Solver 기반 힌트
        findViewById<ImageButton>(R.id.hint_button).setOnClickListener {
            // 백그라운드에서 힌트 찾기
            solverScope.launch {
                val hint = viewModel.findHint()
                
                withContext(Dispatchers.Main) {
                    if (hint != null) {
                        Toast.makeText(
                            this@GameActivity, 
                            "힌트: ${hint}", 
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Unsolvable 체크
                        val unsolvableReason = viewModel.checkUnsolvable()
                        if (unsolvableReason != null) {
                            Toast.makeText(
                                this@GameActivity, 
                                "게임 막힘: ${unsolvableReason.message}", 
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@GameActivity, 
                                "힌트를 찾을 수 없습니다", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        
        // Auto button - 자동 완료
        findViewById<ImageButton>(R.id.auto_button).setOnClickListener {
            val moveCount = viewModel.autoComplete()
            if (moveCount > 0) {
                Toast.makeText(this, "$moveCount 장의 카드를 자동으로 이동했습니다", Toast.LENGTH_SHORT).show()
                persist()
            } else {
                Toast.makeText(this, "더 이상 자동으로 이동할 카드가 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Share button - 공유하기
        findViewById<ImageButton>(R.id.share_button).setOnClickListener {
            Toast.makeText(this, "공유 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
        }
        
        // 일시정지 버튼 추가 예정 (현재는 주석 처리)
        // TODO: pause_button을 레이아웃에 추가하고 활성화
        /*
        findViewById<Button>(R.id.pause_button)?.setOnClickListener {
            if (viewModel.isPaused()) {
                viewModel.resume()
                (it as Button).text = "Pause"
                Toast.makeText(this@GameActivity, "게임 재개", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.pause()
                (it as Button).text = "Resume"
                Toast.makeText(this@GameActivity, "일시정지", Toast.LENGTH_SHORT).show()
            }
        }
        */
        
        // Back button
        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener {
            finish()
        }
        
        // Statistics button
        findViewById<ImageButton>(R.id.statistics_button)?.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
        
        // 규칙 설정 버튼
        findViewById<ImageButton>(R.id.rules_button).setOnClickListener {
            val intent = Intent(this, RulesActivity::class.java)
            intent.putExtra(EXTRA_RULES, viewModel.getRules())
            startActivityForResult(intent, REQUEST_CODE_RULES)
        }
    }
    
    private fun clearAllSelections() {
        // 모든 카드 선택 상태 해제
        for (i in 0..6) {
            tableauViews[i]?.background = null
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
                // 드래그가 뷰 위에 진입했을 때 - 배경색 제거
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
                } else {
                    view.alpha = 1f
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
                // 드래그가 뷰 위에 진입했을 때 - 배경색 제거
                true
            }
            
            DragEvent.ACTION_DRAG_EXITED -> {
                view.background = null
                view.alpha = 1f
                true
            }
            
            DragEvent.ACTION_DROP -> {
                view.background = null
                
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
                        Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    view.alpha = 1f
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
        
        // 승리 기록 저장
        saveGameResult("win")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🎉 축하합니다!")
        builder.setMessage("Klondike Solitaire를 완료했습니다!\n\n새 게임을 시작하시겠습니까?")
        builder.setPositiveButton("새 게임") { _, _ ->
            victoryShown = false
            viewModel.reset()
            startNewGame(viewModel.getSeed())
        }
        builder.setNegativeButton("다시 시작") { _, _ ->
            victoryShown = false
            // 같은 시드로 재시작
            viewModel.restartGame()
            startNewGame(currentGameSeed)
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
    
    private fun animateCardToFoundation(sourceIndex: Int, foundationIndex: Int, sourceType: DragSourceType) {
        // 소스와 목적지 뷰의 위치 계산
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val boardView = findViewById<GridLayout>(R.id.game_board)
        val state = viewModel.state.value
        
        // 소스 카드와 뷰 찾기
        var sourceViewLocal: View? = null
        var sourceCard: us.jyni.game.klondike.model.Card? = null
        
        when (sourceType) {
            DragSourceType.TABLEAU -> {
                tableauViews[sourceIndex]?.let { pileView ->
                    if (pileView is LinearLayout && pileView.childCount > 0) {
                        sourceViewLocal = pileView.getChildAt(pileView.childCount - 1)
                        sourceCard = state.tableau[sourceIndex].lastOrNull()
                    }
                }
            }
            DragSourceType.WASTE -> {
                // Waste 영역은 game_board의 5번째 자식 (stock 다음)
                if (boardView != null && boardView.childCount > 4) {
                    val wasteContainer = boardView.getChildAt(4)
                    if (wasteContainer is LinearLayout && wasteContainer.childCount > 0) {
                        sourceViewLocal = wasteContainer.getChildAt(wasteContainer.childCount - 1)
                        sourceCard = state.waste.lastOrNull()
                    }
                }
            }
            else -> {}
        }
        
        // Foundation 뷰 찾기 (boardView의 첫 4개 자식)
        val foundationView = if (boardView != null && foundationIndex < 4) {
            boardView.getChildAt(foundationIndex)
        } else null
        
        val sourceView = sourceViewLocal ?: return
        val card = sourceCard ?: return
        if (foundationView == null) {
            return
        }
        
        // 소스와 목적지의 화면 좌표 얻기
        val sourceLocation = IntArray(2)
        val destLocation = IntArray(2)
        val sourceWidth = sourceView.width
        val sourceHeight = sourceView.height
        sourceView.getLocationOnScreen(sourceLocation)
        foundationView.getLocationOnScreen(destLocation)
        
        // 트레일 효과를 위한 임시 카드 뷰 생성
        val trailView = CardView(this).apply {
            setCard(card)
            layoutParams = ViewGroup.LayoutParams(sourceWidth, sourceHeight)
        }
        
        // 루트 뷰에 추가
        (rootView as? android.widget.FrameLayout)?.addView(trailView)
        
        // 시작 위치로 이동
        trailView.x = sourceLocation[0].toFloat()
        trailView.y = sourceLocation[1].toFloat()
        
        // 목적지로 애니메이션
        trailView.animate()
            .x(destLocation[0].toFloat())
            .y(destLocation[1].toFloat())
            .alpha(0.7f)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // 애니메이션 종료 후 트레일 뷰 제거
                (rootView as? android.widget.FrameLayout)?.removeView(trailView)
            }
            .start()
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
                            if (viewModel.moveTableauToFoundation(col, foundationIndex)) {
                                moved = true
                                break
                            }
                        }
                    }
                    if (moved) break
                }
            }
            return true
        }
        return false
    }
    
    private fun updateTimerAndScore() {
        val statusText = findViewById<TextView>(R.id.status_text)
        val s = viewModel.state.value
        val stock = s.stock.size
        val waste = s.waste.size
        val fnd = s.foundation.sumOf { it.size }
        val elapsed = viewModel.getElapsedTimeMs()
        val minutes = (elapsed / 60000).toInt()
        val seconds = ((elapsed % 60000) / 1000).toInt()
        val score = viewModel.getScore()
        val moves = viewModel.getMoveCount()
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        
        // Update header timer, score, and moves
        findViewById<TextView>(R.id.timer_text)?.text = timeStr
        findViewById<TextView>(R.id.score_text)?.text = String.format("%,d", score)
        findViewById<TextView>(R.id.moves_text)?.text = moves.toString()
        
        // Update status text (card counts only)
        val countsStr = "Stock:$stock  Waste:$waste  Foundation:$fnd  Tableau:${52 - stock - waste - fnd}"
        statusText.text = countsStr
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

    override fun onPause() {
        super.onPause()
        // 타이머 일시정지
        viewModel.pause()
        // 앱이 백그라운드로 가거나 종료될 때 게임 상태를 저장
        saveGameState()
    }

    override fun onResume() {
        super.onResume()
        // 타이머 재개
        viewModel.resume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val saved = viewModel.saveStateString()
        outState.putString(KEY_SAVED_GAME, saved)
        // Also persist to SharedPreferences for process-death restore
        saveGameState()
    }

    private fun saveGameState() {
        try {
            val saved = viewModel.saveStateString()
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PERSISTED_GAME, saved).apply()
        } catch (e: Exception) {
            android.util.Log.e("GameActivity", "Failed to save game state", e)
        }
    }
    
    private fun getCurrentGameStats(): us.jyni.game.klondike.util.stats.SolveStats? {
        return try {
            us.jyni.game.klondike.util.stats.SolveStats(
                dealId = viewModel.dealId(),
                seed = currentGameSeed,
                rules = viewModel.getRules(),
                startedAt = gameStartTime,
                finishedAt = null,
                durationMs = 0,
                moveCount = viewModel.getMoveCount(),
                outcome = null
            )
        } catch (e: Exception) {
            android.util.Log.e("GameActivity", "Failed to get current game stats", e)
            null
        }
    }
    
    private fun updateFavoriteIndicator() {
        val indicator = findViewById<ImageView>(R.id.favorite_indicator)
        
        // 통계 목록에서 현재 seed와 동일한 게임들 확인
        val allStats = repository.readAllStats()
        val gamesWithSameSeed = allStats.filter { it.seed == currentGameSeed }
        val hasFavorite = gamesWithSameSeed.any { repository.isFavorite(it) }
        
        indicator.setImageResource(
            if (hasFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
    }
    
    private fun startNewGame(seed: ULong) {
        gameStartTime = System.currentTimeMillis()
        currentGameSeed = seed
    }
    
    private fun saveGameResult(outcome: String) {
        try {
            val finishTime = System.currentTimeMillis()
            val currentMoveCount = viewModel.getMoveCount()
            val stats = us.jyni.game.klondike.util.stats.SolveStats(
                dealId = viewModel.dealId(),
                seed = currentGameSeed,
                rules = viewModel.getRules(),
                startedAt = gameStartTime,
                finishedAt = finishTime,
                durationMs = finishTime - gameStartTime,
                moveCount = currentMoveCount,
                outcome = outcome
            )
            repository.appendPending(stats)
            android.util.Log.d("GameActivity", "Game result saved: outcome=$outcome, moves=$currentMoveCount, duration=${finishTime - gameStartTime}ms")
        } catch (e: Exception) {
            android.util.Log.e("GameActivity", "Failed to save game result", e)
        }
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