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
    private val pageSize = 20
    
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
            bestMovesGame?.let { replayGame(it) }
        }
        
        btnReplayBestTime.setOnClickListener {
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
        val result = repository.readPagedStats(
            page = page,
            pageSize = pageSize,
            filter = currentFilter,
            sortOrder = currentSort
        )
        
        // RecyclerView 업데이트
        adapter.submitList(result.items)
        
        // 페이지 정보 업데이트
        pageInfoText.text = if (result.totalPages > 0) {
            "${result.page + 1} / ${result.totalPages} (${result.totalItems})"
        } else {
            "게임 기록이 없습니다"
        }
        
        // 페이징 버튼 상태
        btnPrevPage.isEnabled = result.hasPrevious
        btnNextPage.isEnabled = result.hasNext
        
        currentPage = page
    }
    
    private fun replayGame(game: SolveStats) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("extra_seed", game.seed.toLong())
        intent.putExtra("RULES", game.rules)
        intent.putExtra("IS_REPLAY", true)
        startActivity(intent)
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
 * RecyclerView Adapter for game stats
 */
class GameStatsAdapter(
    private val repository: JsonlFileRepository,
    private val onReplayClick: (SolveStats) -> Unit
) : RecyclerView.Adapter<GameStatsAdapter.ViewHolder>() {
    
    private var games = listOf<SolveStats>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    fun submitList(newGames: List<SolveStats>) {
        games = newGames
        notifyDataSetChanged()
    }
    
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.date_text)
        val movesText: TextView = view.findViewById(R.id.moves_text)
        val timeText: TextView = view.findViewById(R.id.time_text)
        val outcomeIcon: TextView = view.findViewById(R.id.outcome_icon)
        val outcomeText: TextView = view.findViewById(R.id.outcome_text)
        val rulesText: TextView = view.findViewById(R.id.rules_text)
        val seedText: TextView = view.findViewById(R.id.seed_text)
        val favoriteButton: ImageButton = view.findViewById(R.id.favorite_button)
        val replayButton: ImageButton = view.findViewById(R.id.replay_button)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_stats, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        
        // 날짜
        holder.dateText.text = dateFormat.format(Date(game.startedAt))
        
        // 이동 수
        holder.movesText.text = "${game.moveCount}"
        
        // 시간
        val minutes = (game.durationMs / 60000).toInt()
        val seconds = ((game.durationMs % 60000) / 1000).toInt()
        holder.timeText.text = String.format("%d:%02d", minutes, seconds)
        
        // 결과
        when (game.outcome) {
            "win" -> {
                holder.outcomeIcon.text = "✅"
                holder.outcomeText.text = "승리"
                holder.outcomeIcon.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
            "resign" -> {
                holder.outcomeIcon.text = "❌"
                holder.outcomeText.text = "포기"
                holder.outcomeIcon.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            else -> {
                holder.outcomeIcon.text = "⏸️"
                holder.outcomeText.text = "중단"
                holder.outcomeIcon.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            }
        }
        
        // 규칙
        holder.rulesText.text = "D${game.rules.draw}"
        
        // Seed (클릭 시 표시)
        holder.seedText.text = "Seed: ${game.seed}"
        holder.itemView.setOnClickListener {
            holder.seedText.visibility = if (holder.seedText.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        
        // 즐겨찾기 버튼
        val isFavorite = repository.isFavorite(game)
        holder.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        holder.favoriteButton.setOnClickListener {
            val nowFavorite = repository.toggleFavorite(game)
            holder.favoriteButton.setImageResource(
                if (nowFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            
            // 토스트 메시지
            val context = holder.itemView.context
            val message = if (nowFavorite) context.getString(R.string.stats_favorite_added) else context.getString(R.string.stats_favorite_removed)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        
        // 재도전 버튼
        holder.replayButton.setOnClickListener {
            onReplayClick(game)
        }
    }

    override fun getItemCount() = games.size
}
