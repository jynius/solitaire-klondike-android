package us.jyni.game.klondike.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import us.jyni.R
import us.jyni.game.klondike.sync.JsonlFileRepository
import us.jyni.game.klondike.sync.JsonlFileRepository.GameFilter
import us.jyni.game.klondike.sync.JsonlFileRepository.SortOrder
import us.jyni.game.klondike.util.stats.SolveStats
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    
    private lateinit var repository: JsonlFileRepository
    private lateinit var adapter: GameStatsAdapter
    
    private var currentPage = 0
    private var currentFilter = GameFilter.ALL
    private var currentSort = SortOrder.NEWEST_FIRST
    private val pageSize = 20  // 그룹(게임) 단위로 20개씩
    private var totalGroups = 0
    
    // Views
    private lateinit var totalGamesText: TextView
    private lateinit var winRateText: TextView
    private lateinit var favoritesCountText: TextView
    private lateinit var avgMovesText: TextView
    private lateinit var avgTimeText: TextView
    private lateinit var bestMovesText: TextView
    private lateinit var bestMovesDate: TextView
    private lateinit var bestTimeText: TextView
    private lateinit var bestTimeDate: TextView
    private lateinit var bestStreakText: TextView
    private lateinit var bestStreakDate: TextView
    private lateinit var btnReplayBestMoves: ImageButton
    private lateinit var btnReplayBestTime: ImageButton
    private lateinit var gamesRecycler: RecyclerView
    private lateinit var pageInfoText: TextView
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    
    // Best records
    private var bestMovesGame: SolveStats? = null
    private var bestTimeGame: SolveStats? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language before calling super.onCreate
        applyLanguage()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        repository = JsonlFileRepository(this)
        
        initViews()
        setupFilterButtons()
        setupSortSpinner()
        setupRecyclerView()
        setupPagination()
        
        // 초기 데이터 로드
        updateOverallStats()
        updateBestRecords()
        loadPage(0)
    }
    
    private fun initViews() {
        totalGamesText = findViewById(R.id.total_games_text)
        winRateText = findViewById(R.id.win_rate_text)
        favoritesCountText = findViewById(R.id.favorites_count_text)
        avgMovesText = findViewById(R.id.avg_moves_text)
        avgTimeText = findViewById(R.id.avg_time_text)
        bestMovesText = findViewById(R.id.best_moves_text)
        bestMovesDate = findViewById(R.id.best_moves_date)
        bestTimeText = findViewById(R.id.best_time_text)
        bestTimeDate = findViewById(R.id.best_time_date)
        bestStreakText = findViewById(R.id.best_streak_text)
        bestStreakDate = findViewById(R.id.best_streak_date)
        btnReplayBestMoves = findViewById(R.id.btn_replay_best_moves)
        btnReplayBestTime = findViewById(R.id.btn_replay_best_time)
        gamesRecycler = findViewById(R.id.games_recycler)
        pageInfoText = findViewById(R.id.page_info)
        btnPrevPage = findViewById(R.id.btn_prev_page)
        btnNextPage = findViewById(R.id.btn_next_page)
        
        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }
        
        // Replay best records buttons
        btnReplayBestMoves.setOnClickListener {
            android.util.Log.d("StatisticsActivity", "btnReplayBestMoves clicked, bestMovesGame=$bestMovesGame")
            bestMovesGame?.let { replayGame(it) }
        }
        
        btnReplayBestTime.setOnClickListener {
            android.util.Log.d("StatisticsActivity", "btnReplayBestTime clicked, bestTimeGame=$bestTimeGame")
            bestTimeGame?.let { replayGame(it) }
        }
    }
    
    private fun setupFilterButtons() {
        val filterSpinner = findViewById<Spinner>(R.id.filter_spinner)
        filterSpinner.setBackgroundColor(android.graphics.Color.WHITE)
        filterSpinner.setPopupBackgroundResource(android.R.color.white)
        
        val filterOptions = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_favorites),
            getString(R.string.filter_wins),
            getString(R.string.filter_losses)
        )
        
        val filterAdapter = object : ArrayAdapter<String>(this, R.layout.spinner_item, filterOptions) {
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setBackgroundColor(android.graphics.Color.WHITE)
                view.setTextColor(android.graphics.Color.BLACK)
                
                // 현재 선택된 항목 하이라이트
                if (position == filterSpinner.selectedItemPosition) {
                    view.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
                    view.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    view.setBackgroundResource(android.R.drawable.list_selector_background)
                    view.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                return view
            }
        }
        filterAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        filterSpinner.adapter = filterAdapter
        filterSpinner.dropDownVerticalOffset = 14
        filterSpinner.dropDownHorizontalOffset = -20
        
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = when (position) {
                    0 -> GameFilter.ALL
                    1 -> GameFilter.FAVORITE
                    2 -> GameFilter.WIN
                    3 -> GameFilter.LOSS
                    else -> GameFilter.ALL
                }
                filterAdapter.notifyDataSetChanged()
                currentPage = 0
                loadPage(0)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupSortSpinner() {
        val sortSpinner = findViewById<Spinner>(R.id.sort_spinner)
        sortSpinner.setBackgroundColor(android.graphics.Color.WHITE)
        sortSpinner.setPopupBackgroundResource(android.R.color.white)
        
        val sortOptions = arrayOf(
            getString(R.string.sort_newest),
            getString(R.string.sort_oldest),
            getString(R.string.sort_most_moves),
            getString(R.string.sort_least_moves),
            getString(R.string.sort_longest_time),
            getString(R.string.sort_shortest_time)
        )
        
        val arrayAdapter = object : ArrayAdapter<String>(this, R.layout.spinner_item, sortOptions) {
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setBackgroundColor(android.graphics.Color.WHITE)
                view.setTextColor(android.graphics.Color.BLACK)
                
                // 현재 선택된 항목 하이라이트
                if (position == sortSpinner.selectedItemPosition) {
                    view.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
                    view.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    view.setBackgroundResource(android.R.drawable.list_selector_background)
                    view.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                return view
            }
        }
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        sortSpinner.adapter = arrayAdapter
        sortSpinner.dropDownVerticalOffset = 14
        sortSpinner.dropDownHorizontalOffset = -20
        
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSort = when (position) {
                    0 -> SortOrder.NEWEST_FIRST
                    1 -> SortOrder.OLDEST_FIRST
                    2 -> SortOrder.MOST_MOVES
                    3 -> SortOrder.LEAST_MOVES
                    4 -> SortOrder.LONGEST_TIME
                    5 -> SortOrder.SHORTEST_TIME
                    else -> SortOrder.NEWEST_FIRST
                }
                arrayAdapter.notifyDataSetChanged()
                currentPage = 0
                loadPage(0)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupRecyclerView() {
        adapter = GameStatsAdapter(repository) { game ->
            replayGame(game)
        }
        
        gamesRecycler.layoutManager = LinearLayoutManager(this)
        gamesRecycler.adapter = adapter
        gamesRecycler.itemAnimator = null // 깜빡임 방지
    }
    
    private fun setupPagination() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                loadPage(currentPage - 1)
            }
        }
        
        btnNextPage.setOnClickListener {
            loadPage(currentPage + 1)
        }
    }
    
    private fun updateOverallStats() {
        val allGames = repository.readAllStats()
        val favorites = repository.readFavoriteStats()
        val wins = repository.readWinStats()
        
        val totalGames = allGames.size
        val totalWins = wins.size
        val totalFavorites = favorites.size
        
        val winRate = if (totalGames > 0) (totalWins * 100.0 / totalGames) else 0.0
        
        totalGamesText.text = totalGames.toString()
        winRateText.text = String.format("%.1f%%", winRate)
        favoritesCountText.text = totalFavorites.toString()
        
        if (allGames.isNotEmpty()) {
            val avgMoves = allGames.map { it.moveCount }.average()
            val avgTimeMs = allGames.map { it.durationMs }.average()
            val avgMinutes = (avgTimeMs / 60000).toInt()
            val avgSeconds = ((avgTimeMs % 60000) / 1000).toInt()
            
            avgMovesText.text = getString(R.string.stats_avg_moves, String.format("%.1f", avgMoves))
            avgTimeText.text = getString(R.string.stats_avg_time, String.format("%d:%02d", avgMinutes, avgSeconds))
        } else {
            avgMovesText.text = getString(R.string.stats_avg_moves, "0")
            avgTimeText.text = getString(R.string.stats_avg_time, "0:00")
        }
    }
    
    private fun updateBestRecords() {
        val wins = repository.readWinStats()
        
        if (wins.isEmpty()) {
            bestMovesText.text = getString(R.string.stats_min_moves, getString(R.string.stats_no_record))
            bestMovesDate.text = ""
            bestTimeText.text = getString(R.string.stats_min_time, getString(R.string.stats_no_record))
            bestTimeDate.text = ""
            bestStreakDate.text = ""
            bestStreakText.text = getString(R.string.stats_longest_streak, getString(R.string.stats_no_record))
            btnReplayBestMoves.visibility = View.GONE
            btnReplayBestTime.visibility = View.GONE
            return
        }
        
        // 최소 이동 승리
        bestMovesGame = wins.minByOrNull { it.moveCount }
        bestMovesGame?.let { game ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(game.finishedAt ?: 0L))
            bestMovesText.text = getString(R.string.stats_min_moves, "${game.moveCount}${getString(R.string.unit_moves)}")
            bestMovesDate.text = dateStr
            btnReplayBestMoves.visibility = View.VISIBLE
        }
        
        // 최단 시간 승리
        bestTimeGame = wins.minByOrNull { it.durationMs }
        bestTimeGame?.let { game ->
            val minutes = (game.durationMs / 60000).toInt()
            val seconds = ((game.durationMs % 60000) / 1000).toInt()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(game.finishedAt ?: 0L))
            bestTimeText.text = getString(R.string.stats_min_time, "$minutes:${String.format("%02d", seconds)}")
            bestTimeDate.text = dateStr
            btnReplayBestTime.visibility = View.VISIBLE
        }
        
        // 최장 연승 계산
        val allGames = repository.readAllStats()
            .sortedBy { it.finishedAt }
        
        var maxStreak = 0
        var currentStreak = 0
        var streakStartDate: Long? = null
        var streakEndDate: Long? = null
        var maxStreakStart: Long? = null
        var maxStreakEnd: Long? = null
        
        for (game in allGames) {
            if (game.outcome == "win") {
                if (currentStreak == 0) {
                    streakStartDate = game.finishedAt
                }
                currentStreak++
                streakEndDate = game.finishedAt
                
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                    maxStreakStart = streakStartDate
                    maxStreakEnd = streakEndDate
                }
            } else {
                currentStreak = 0
                streakStartDate = null
                streakEndDate = null
            }
        }
        
        if (maxStreak > 0) {
            val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            val startStr = maxStreakStart?.let { dateFormat.format(Date(it)) } ?: ""
            val endStr = maxStreakEnd?.let { dateFormat.format(Date(it)) } ?: ""
            
            val gameUnit = if (maxStreak == 1) {
                getString(R.string.unit_game_singular)
            } else {
                getString(R.string.unit_game_plural)
            }
            
            bestStreakText.text = getString(R.string.stats_longest_streak, "${maxStreak}${gameUnit}")
            bestStreakDate.text = if (maxStreak == 1) "" else "$startStr ~ $endStr"
        } else {
            bestStreakText.text = getString(R.string.stats_longest_streak, getString(R.string.stats_no_record))
            bestStreakDate.text = ""
        }
    }
    
    private fun loadPage(page: Int) {
        // 필터링된 전체 데이터 가져오기
        val allGames = when (currentFilter) {
            GameFilter.ALL -> repository.readAllStats()
            GameFilter.FAVORITE -> repository.readFavoriteStats()
            GameFilter.WIN -> repository.readWinStats()
            GameFilter.LOSS -> repository.readAllStats().filter { it.outcome != "win" }
        }
        
        // 그룹핑 먼저 (gameCode 기준)
        val groupMap = mutableMapOf<String, GameGroup>()
        for (game in allGames) {
            // gameCode가 없는 구 데이터는 seed+rules로 생성
            val actualGameCode = game.gameCode ?: us.jyni.game.klondike.util.GameCode.encode(game.seed, game.rules)
            val key = actualGameCode
            val group = groupMap.getOrPut(key) {
                GameGroup(
                    gameCode = actualGameCode,
                    dealId = game.dealId,
                    inherentStatus = game.inherentStatus,
                    plays = mutableListOf(),
                    isFavorite = repository.isFavorite(game)
                )
            }
            group.plays.add(game)
        }
        
        // 그룹 정렬 적용 (그룹 단위 기준)
        val sortedGroups = when (currentSort) {
            SortOrder.NEWEST_FIRST -> groupMap.values.sortedByDescending { it.plays.maxOf { p -> p.startedAt } }
            SortOrder.OLDEST_FIRST -> groupMap.values.sortedBy { it.plays.minOf { p -> p.startedAt } }
            SortOrder.MOST_MOVES -> groupMap.values.sortedByDescending { it.plays.minOf { p -> p.moveCount } }
            SortOrder.LEAST_MOVES -> groupMap.values.sortedBy { it.plays.minOf { p -> p.moveCount } }
            SortOrder.LONGEST_TIME -> groupMap.values.sortedByDescending { it.plays.maxOf { p -> p.durationMs } }
            SortOrder.SHORTEST_TIME -> groupMap.values.sortedBy { it.plays.minOf { p -> p.durationMs } }
        }
        
        // 그룹 내 플레이도 정렬 (최신순)
        sortedGroups.forEach { group ->
            group.plays.sortByDescending { it.startedAt }
        }
        
        totalGroups = sortedGroups.size
        
        // 페이징 적용 (그룹 단위)
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, totalGroups)
        val pagedGroups = if (startIndex < totalGroups) {
            sortedGroups.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        // 페이징된 그룹의 플레이들만 추출
        val pagedGames = pagedGroups.flatMap { it.plays }
        
        // RecyclerView 업데이트
        adapter.submitList(pagedGames)
        
        // 페이지 정보 업데이트
        val totalPages = (totalGroups + pageSize - 1) / pageSize
        val totalPlays = allGames.size
        pageInfoText.text = if (totalGroups > 0) {
            "${page + 1}/$totalPages (${totalGroups}, ${totalPlays})"
        } else {
            "게임 기록이 없습니다"
        }
        
        // 페이징 버튼 상태
        btnPrevPage.isEnabled = page > 0
        btnNextPage.isEnabled = endIndex < totalGroups
        btnPrevPage.visibility = View.VISIBLE
        btnNextPage.visibility = View.VISIBLE
        
        currentPage = page
    }
    
    private fun replayGame(game: SolveStats) {
        // 현재 진행 중인 게임이 있는지 확인 (SharedPreferences에 저장된 게임 상태)
        val prefs = getSharedPreferences("klondike_prefs", Context.MODE_PRIVATE)
        val hasOngoingGame = prefs.getString("persisted_game_sv1", null) != null
        
        if (hasOngoingGame) {
            // 확인 다이얼로그 표시
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.replay_confirm_title))
                .setMessage(getString(R.string.replay_confirm_message))
                .setPositiveButton(getString(R.string.replay_confirm_yes)) { _, _ ->
                    // 사용자가 확인하면 게임 시작
                    startReplayGame(game)
                }
                .setNegativeButton(getString(R.string.replay_confirm_no), null)
                .show()
        } else {
            // 진행 중인 게임이 없으면 바로 시작
            startReplayGame(game)
        }
    }
    
    private fun startReplayGame(game: SolveStats) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("extra_seed", game.seed.toLong())
        intent.putExtra("RULES", game.rules)
        intent.putExtra("IS_REPLAY", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 통계 화면 닫기
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

/**
 * 게임 그룹 데이터 클래스
 */
data class GameGroup(
    val gameCode: String,
    val dealId: String,
    val inherentStatus: String?,
    val plays: MutableList<SolveStats>,
    var isExpanded: Boolean = false,
    var isFavorite: Boolean = false
)

/**
 * RecyclerView 아이템 타입
 */
sealed class StatsItem {
    data class GroupHeader(val group: GameGroup) : StatsItem()
    object PlayRecordHeader : StatsItem()
    data class PlayRecord(val game: SolveStats, val groupGameCode: String) : StatsItem()
}

/**
 * RecyclerView Adapter for grouped game stats
 */
class GameStatsAdapter(
    private val repository: JsonlFileRepository,
    private val onReplayClick: (SolveStats) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var groups = listOf<GameGroup>()
    private var items = listOf<StatsItem>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    companion object {
        const val TYPE_GROUP_HEADER = 0
        const val TYPE_PLAY_RECORD_HEADER = 1
        const val TYPE_PLAY_RECORD = 2
    }
    
    fun submitList(newGames: List<SolveStats>) {
        // gameCode 기준으로 그룹핑
        val groupMap = mutableMapOf<String, GameGroup>()
        
        for (game in newGames) {
            // gameCode가 없는 구 데이터는 seed+rules로 생성
            val actualGameCode = game.gameCode ?: us.jyni.game.klondike.util.GameCode.encode(game.seed, game.rules)
            val key = actualGameCode
            val group = groupMap.getOrPut(key) {
                GameGroup(
                    gameCode = actualGameCode,
                    dealId = game.dealId,
                    inherentStatus = game.inherentStatus,
                    plays = mutableListOf(),
                    isFavorite = repository.isFavorite(game)
                )
            }
            group.plays.add(game)
        }
        
        groups = groupMap.values.toList()
        rebuildItems()
        notifyDataSetChanged()
    }
    
    private fun rebuildItems() {
        val newItems = mutableListOf<StatsItem>()
        for (group in groups) {
            newItems.add(StatsItem.GroupHeader(group))
            if (group.isExpanded) {
                newItems.add(StatsItem.PlayRecordHeader)
                group.plays.forEach { play ->
                    newItems.add(StatsItem.PlayRecord(play, group.gameCode))
                }
            }
        }
        items = newItems
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is StatsItem.GroupHeader -> TYPE_GROUP_HEADER
            is StatsItem.PlayRecordHeader -> TYPE_PLAY_RECORD_HEADER
            is StatsItem.PlayRecord -> TYPE_PLAY_RECORD
        }
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GROUP_HEADER -> {
                val view = android.view.LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_game_group_header, parent, false)
                GroupHeaderViewHolder(view)
            }
            TYPE_PLAY_RECORD_HEADER -> {
                val view = android.view.LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_play_record_header, parent, false)
                PlayRecordHeaderViewHolder(view)
            }
            TYPE_PLAY_RECORD -> {
                val view = android.view.LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_game_play_record, parent, false)
                PlayRecordViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is StatsItem.GroupHeader -> (holder as GroupHeaderViewHolder).bind(item.group)
            is StatsItem.PlayRecordHeader -> {} // No binding needed for header
            is StatsItem.PlayRecord -> (holder as PlayRecordViewHolder).bind(item.game)
        }
    }

    override fun getItemCount() = items.size
    
    /**
     * 그룹 헤더 ViewHolder
     */
    inner class GroupHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val groupHeaderLayout: LinearLayout = view.findViewById(R.id.group_header_layout)
        private val expandIcon: TextView = view.findViewById(R.id.expand_icon)
        private val gameCodeText: TextView = view.findViewById(R.id.game_code_text)
        private val playCountText: TextView = view.findViewById(R.id.play_count_text)
        private val rulesText: TextView = view.findViewById(R.id.rules_text)
        private val inherentStatusIcon: TextView = view.findViewById(R.id.inherent_status_icon)
        private val favoriteButton: ImageButton = view.findViewById(R.id.favorite_button)
        private val replayButton: ImageButton = view.findViewById(R.id.replay_button)
        
        fun bind(group: GameGroup) {
            // 확장/축소 아이콘
            expandIcon.text = if (group.isExpanded) "▼" else "▶"
            
            // Inherent Status
            when (group.inherentStatus) {
                "unsolvable" -> inherentStatusIcon.text = "❌"
                "solvable" -> inherentStatusIcon.text = "⭕"
                else -> inherentStatusIcon.text = "⭕"
            }
            
            // 규칙 (게임 속성)
            val firstPlay = group.plays.firstOrNull()
            rulesText.text = if (firstPlay != null) "D${firstPlay.rules.draw}" else ""
            
            // 게임 코드
            gameCodeText.text = group.gameCode
            
            // 플레이 횟수
            val context = itemView.context
            playCountText.text = "(${context.getString(R.string.play_count, group.plays.size)})"
            
            // 즐겨찾기
            favoriteButton.setImageResource(
                if (group.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            favoriteButton.setOnClickListener {
                group.plays.forEach { play ->
                    repository.toggleFavorite(play)
                }
                group.isFavorite = !group.isFavorite
                favoriteButton.setImageResource(
                    if (group.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                )
                val message = if (group.isFavorite) 
                    context.getString(R.string.stats_favorite_added) 
                else 
                    context.getString(R.string.stats_favorite_removed)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            
            // 재생 버튼 (게임 새로 시작)
            replayButton.setOnClickListener {
                android.util.Log.d("StatisticsActivity", "replayButton clicked, firstPlay=$firstPlay")
                firstPlay?.let { onReplayClick(it) }
            }
            
            // 그룹 헤더 클릭 → 확장/축소
            groupHeaderLayout.setOnClickListener {
                group.isExpanded = !group.isExpanded
                expandIcon.text = if (group.isExpanded) "▼" else "▶"
                rebuildItems()
                notifyDataSetChanged()
            }
            
            // 확장 아이콘도 클릭 가능
            expandIcon.setOnClickListener {
                group.isExpanded = !group.isExpanded
                expandIcon.text = if (group.isExpanded) "▼" else "▶"
                rebuildItems()
                notifyDataSetChanged()
            }
        }
    }
    
    /**
     * 플레이 기록 헤더 ViewHolder
     */
    inner class PlayRecordHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
    
    /**
     * 플레이 기록 ViewHolder
     */
    inner class PlayRecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val outcomeIcon: TextView = view.findViewById(R.id.outcome_icon)
        private val dateText: TextView = view.findViewById(R.id.date_text)
        private val scoreText: TextView = view.findViewById(R.id.score_text)
        private val movesText: TextView = view.findViewById(R.id.moves_text)
        private val timeText: TextView = view.findViewById(R.id.time_text)
        private val shareButton: ImageButton = view.findViewById(R.id.share_button)
        
        fun bind(game: SolveStats) {
            val context = itemView.context
            
            // Winnable Status
            when (game.winnableStatus) {
                "won" -> outcomeIcon.text = "🏆"
                "dead_end" -> outcomeIcon.text = "⛔"
                "state_cycle" -> outcomeIcon.text = "🔄"
                "in_progress" -> outcomeIcon.text = "🎮"
                else -> {
                    when (game.outcome) {
                        "win" -> outcomeIcon.text = "🏆"
                        "resign" -> outcomeIcon.text = "❌"
                        else -> outcomeIcon.text = "⏸️"
                    }
                }
            }
            
            // 날짜
            dateText.text = dateFormat.format(Date(game.startedAt))
            
            // 점수
            scoreText.text = String.format("%,d", game.score)
            
            // 이동 수
            movesText.text = "${game.moveCount}"
            
            // 시간
            val minutes = (game.durationMs / 60000).toInt()
            val seconds = ((game.durationMs % 60000) / 1000).toInt()
            timeText.text = String.format("%d:%02d", minutes, seconds)
            
            // 공유 버튼
            shareButton.setOnClickListener {
                Toast.makeText(context, R.string.share_not_ready, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
