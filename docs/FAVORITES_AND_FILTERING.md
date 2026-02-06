# 즐겨찾기 & 필터링 & 페이징 기능

## ✅ 구현 완료!

게임 즐겨찾기, 필터링, 페이징 기능이 모두 구현되었습니다.

## 🌟 즐겨찾기 기능

### 개념
- 마음에 드는 게임을 즐겨찾기로 표시
- 나중에 빠르게 찾아서 재도전 가능
- dealId 기반으로 즐겨찾기 관리
- 파일: `/data/data/us.jyni/files/solves/favorites.txt`

### 사용 예시

#### 즐겨찾기 추가/제거
```kotlin
val repository = JsonlFileRepository(context)
val game = repository.readAllStats().first()

// 즐겨찾기 추가
repository.addFavorite(game)

// 즐겨찾기 제거
repository.removeFavorite(game)

// 토글 (추가 ↔ 제거)
val isNowFavorite = repository.toggleFavorite(game)
if (isNowFavorite) {
    println("⭐ 즐겨찾기에 추가됨")
} else {
    println("☆ 즐겨찾기에서 제거됨")
}

// 즐겨찾기 여부 확인
if (repository.isFavorite(game)) {
    println("⭐ 이 게임은 즐겨찾기입니다")
}
```

#### 즐겨찾기 목록 조회
```kotlin
val repository = JsonlFileRepository(context)

// 즐겨찾기한 게임만 가져오기
val favorites = repository.readFavoriteStats()

println("즐겨찾기 ${favorites.size}개")
favorites.forEach { game ->
    println("⭐ ${game.moveCount}수, Seed: ${game.seed}")
}
```

## 🔍 필터링 기능

### 필터 종류

```kotlin
enum class GameFilter {
    ALL,        // 전체 게임
    FAVORITE,   // 즐겨찾기만
    WIN,        // 성공한 게임
    LOSS        // 실패한 게임
}
```

### 정렬 순서

```kotlin
enum class SortOrder {
    NEWEST_FIRST,   // 최신순 (기본)
    OLDEST_FIRST,   // 오래된순
    MOST_MOVES,     // 이동 많은순
    LEAST_MOVES,    // 이동 적은순
    LONGEST_TIME,   // 시간 긴순
    SHORTEST_TIME   // 시간 짧은순
}
```

### 사용 예시

#### 필터링 + 정렬
```kotlin
val repository = JsonlFileRepository(context)

// 즐겨찾기 게임을 최신순으로
val favoriteGames = repository.readFilteredStats(
    filter = GameFilter.FAVORITE,
    sortOrder = SortOrder.NEWEST_FIRST
)

// 성공한 게임을 이동 적은순으로 (최고 기록부터)
val bestWins = repository.readFilteredStats(
    filter = GameFilter.WIN,
    sortOrder = SortOrder.LEAST_MOVES
)

// 실패한 게임을 최신순으로
val recentLosses = repository.readFilteredStats(
    filter = GameFilter.LOSS,
    sortOrder = SortOrder.NEWEST_FIRST
)

// 전체 게임을 이동 많은순으로 (어려웠던 게임)
val hardGames = repository.readFilteredStats(
    filter = GameFilter.ALL,
    sortOrder = SortOrder.MOST_MOVES
)
```

## 📄 페이징 기능

### 개념
- 많은 게임 기록을 효율적으로 표시
- 한 페이지에 20개씩 (커스터마이징 가능)
- 이전/다음 페이지 이동
- RecyclerView 무한 스크롤에 적합

### 사용 예시

#### 기본 페이징
```kotlin
val repository = JsonlFileRepository(context)

// 첫 페이지 (0~19번째 게임)
val page1 = repository.readPagedStats(
    page = 0,
    pageSize = 20,
    filter = GameFilter.ALL,
    sortOrder = SortOrder.NEWEST_FIRST
)

println("페이지: ${page1.page + 1} / ${page1.totalPages}")
println("전체 게임: ${page1.totalItems}개")
println("현재 페이지 게임: ${page1.items.size}개")
println("다음 페이지 있음: ${page1.hasNext}")
println("이전 페이지 있음: ${page1.hasPrevious}")

// 두 번째 페이지 (20~39번째 게임)
val page2 = repository.readPagedStats(
    page = 1,
    pageSize = 20
)
```

#### StatisticsActivity에서 사용
```kotlin
class StatisticsActivity : AppCompatActivity() {
    private lateinit var repository: JsonlFileRepository
    private var currentPage = 0
    private var currentFilter = GameFilter.ALL
    private var currentSort = SortOrder.NEWEST_FIRST
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        repository = JsonlFileRepository(this)
        
        loadPage(0)
        setupFilterButtons()
        setupPagination()
    }
    
    private fun loadPage(page: Int) {
        val result = repository.readPagedStats(
            page = page,
            pageSize = 20,
            filter = currentFilter,
            sortOrder = currentSort
        )
        
        // UI 업데이트
        displayGames(result.items)
        updatePaginationUI(result)
        
        currentPage = page
    }
    
    private fun setupFilterButtons() {
        findViewById<Button>(R.id.btn_filter_all).setOnClickListener {
            currentFilter = GameFilter.ALL
            loadPage(0)
        }
        
        findViewById<Button>(R.id.btn_filter_favorite).setOnClickListener {
            currentFilter = GameFilter.FAVORITE
            loadPage(0)
        }
        
        findViewById<Button>(R.id.btn_filter_win).setOnClickListener {
            currentFilter = GameFilter.WIN
            loadPage(0)
        }
        
        findViewById<Button>(R.id.btn_filter_loss).setOnClickListener {
            currentFilter = GameFilter.LOSS
            loadPage(0)
        }
    }
    
    private fun setupPagination() {
        findViewById<Button>(R.id.btn_prev_page).setOnClickListener {
            if (currentPage > 0) {
                loadPage(currentPage - 1)
            }
        }
        
        findViewById<Button>(R.id.btn_next_page).setOnClickListener {
            loadPage(currentPage + 1)
        }
    }
    
    private fun displayGames(games: List<SolveStats>) {
        // RecyclerView 어댑터에 데이터 설정
        games.forEach { game ->
            val isFavorite = repository.isFavorite(game)
            addGameItem(game, isFavorite) { 
                replayGame(game) 
            }
        }
    }
    
    private fun updatePaginationUI(result: PagedResult<SolveStats>) {
        findViewById<TextView>(R.id.page_info).text = 
            "페이지 ${result.page + 1} / ${result.totalPages} (전체 ${result.totalItems}개)"
        
        findViewById<Button>(R.id.btn_prev_page).isEnabled = result.hasPrevious
        findViewById<Button>(R.id.btn_next_page).isEnabled = result.hasNext
    }
    
    private fun replayGame(game: SolveStats) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("SEED", game.seed.toLong())
        intent.putExtra("RULES", game.rules)
        startActivity(intent)
    }
}
```

## 🎯 실제 사용 시나리오

### 시나리오 1: 즐겨찾기 활용 ⭐
```
1. 게임 플레이 후 통계 화면에서 목록 확인
2. 재미있거나 어려웠던 게임에 ⭐ 클릭
3. 나중에 "즐겨찾기" 탭에서 빠르게 찾기
4. 재도전!
```

### 시나리오 2: 실패한 게임 복수하기 💪
```
1. "실패한 게임" 필터 선택
2. 최신순으로 정렬
3. "아깝게 졌던 그 게임" 찾기
4. 재도전하여 성공!
```

### 시나리오 3: 최고 기록 도전 🏆
```
1. "성공한 게임" 필터 선택
2. "이동 적은순" 정렬
3. 현재 최고 기록 확인
4. 재도전하여 기록 갱신!
```

### 시나리오 4: 어려운 게임 학습 📚
```
1. "전체" 필터 + "이동 많은순" 정렬
2. 가장 어려웠던 게임들 확인
3. 즐겨찾기 추가 ⭐
4. 여러 번 재도전하며 전략 학습
```

## 📱 UI 예시 (StatisticsActivity)

```
┌─────────────────────────────────────────┐
│ 통계                                     │
├─────────────────────────────────────────┤
│ [ 전체 ] [⭐즐겨찾기] [ 성공 ] [ 실패 ] │ <- 필터 버튼
├─────────────────────────────────────────┤
│ 정렬: [최신순 ▾]                        │ <- 정렬 드롭다운
├─────────────────────────────────────────┤
│ 2026-02-06 14:23  102수  4:23  ✅ ☆    │
│                                [재도전]  │
├─────────────────────────────────────────┤
│ 2026-02-06 13:45   89수  3:45  ❌ ⭐   │
│                                [재도전]  │
├─────────────────────────────────────────┤
│ 2026-02-05 21:12  127수  5:12  ✅ ☆    │
│                                [재도전]  │
├─────────────────────────────────────────┤
│ ...                                      │
├─────────────────────────────────────────┤
│ [ ← 이전 ]  페이지 1/5  [ 다음 → ]    │ <- 페이징
└─────────────────────────────────────────┘
```

## 🔧 RecyclerView 어댑터 예시

```kotlin
class GameStatsAdapter(
    private val repository: JsonlFileRepository,
    private val onReplayClick: (SolveStats) -> Unit
) : RecyclerView.Adapter<GameStatsAdapter.ViewHolder>() {
    
    private var games = listOf<SolveStats>()
    
    fun submitList(newGames: List<SolveStats>) {
        games = newGames
        notifyDataSetChanged()
    }
    
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.date_text)
        val movesText: TextView = view.findViewById(R.id.moves_text)
        val timeText: TextView = view.findViewById(R.id.time_text)
        val outcomeIcon: TextView = view.findViewById(R.id.outcome_icon)
        val favoriteButton: ImageButton = view.findViewById(R.id.favorite_button)
        val replayButton: Button = view.findViewById(R.id.replay_button)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_stats, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        
        // 날짜
        val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            .format(Date(game.startedAt))
        holder.dateText.text = date
        
        // 이동 수
        holder.movesText.text = "${game.moveCount}수"
        
        // 시간
        val minutes = (game.durationMs / 60000).toInt()
        val seconds = ((game.durationMs % 60000) / 1000).toInt()
        holder.timeText.text = String.format("%d:%02d", minutes, seconds)
        
        // 결과 아이콘
        holder.outcomeIcon.text = if (game.outcome == "win") "✅" else "❌"
        
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
        }
        
        // 재도전 버튼
        holder.replayButton.setOnClickListener {
            onReplayClick(game)
        }
    }
    
    override fun getItemCount() = games.size
}
```

## 📊 통계 대시보드 예시

```kotlin
class StatisticsDashboard(
    private val repository: JsonlFileRepository
) {
    fun calculateOverallStats(): OverallStats {
        val allGames = repository.readAllStats()
        val favorites = repository.readFavoriteStats()
        val wins = repository.readWinStats()
        val losses = repository.readLossStats()
        
        return OverallStats(
            totalGames = allGames.size,
            totalWins = wins.size,
            totalLosses = losses.size,
            totalFavorites = favorites.size,
            winRate = if (allGames.isNotEmpty()) wins.size * 100.0 / allGames.size else 0.0,
            avgMoves = if (allGames.isNotEmpty()) allGames.map { it.moveCount }.average() else 0.0,
            avgTime = if (allGames.isNotEmpty()) allGames.map { it.durationMs }.average() else 0.0
        )
    }
    
    data class OverallStats(
        val totalGames: Int,
        val totalWins: Int,
        val totalLosses: Int,
        val totalFavorites: Int,
        val winRate: Double,
        val avgMoves: Double,
        val avgTime: Double
    )
}
```

## 🎨 레이아웃 예시 (activity_statistics.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <!-- 전체 통계 카드 -->
    <androidx.cardview.widget.CardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp">
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">
            
            <TextView
                android:id="@+id/total_games_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="총 게임: 128개"
                android:textSize="16sp"/>
            
            <TextView
                android:id="@+id/win_rate_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="승률: 50.0%"
                android:textSize="16sp"/>
            
            <TextView
                android:id="@+id/favorites_count_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="⭐ 즐겨찾기: 12개"
                android:textSize="16sp"/>
        </LinearLayout>
    </androidx.cardview.widget.CardView>
    
    <!-- 필터 버튼들 -->
    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp">
        
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btn_filter_all"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="전체"/>
            
            <Button
                android:id="@+id/btn_filter_favorite"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="⭐즐겨찾기"/>
            
            <Button
                android:id="@+id/btn_filter_win"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="✅성공"/>
            
            <Button
                android:id="@+id/btn_filter_loss"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="❌실패"/>
        </LinearLayout>
    </HorizontalScrollView>
    
    <!-- 정렬 스피너 -->
    <Spinner
        android:id="@+id/sort_spinner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"/>
    
    <!-- 게임 목록 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/games_recycler"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>
    
    <!-- 페이징 컨트롤 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:padding="8dp">
        
        <Button
            android:id="@+id/btn_prev_page"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="← 이전"/>
        
        <TextView
            android:id="@+id/page_info"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="페이지 1 / 5"
            android:gravity="center"
            android:textSize="14sp"/>
        
        <Button
            android:id="@+id/btn_next_page"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="다음 →"/>
    </LinearLayout>
</LinearLayout>
```

## 🧪 테스트 예시

```kotlin
class FavoritesTest {
    @Test
    fun favorite_addAndRemove_works() {
        val tmpDir = createTempDir()
        val repo = JsonlFileRepository(tmpDir)
        
        // 게임 생성 및 저장
        val engine = GameEngine()
        engine.startGame(seed = 123uL)
        val stats = engine.getSolveStatsSnapshot("win")
        repo.appendPending(stats)
        
        // 즐겨찾기 추가
        assertFalse(repo.isFavorite(stats))
        repo.addFavorite(stats)
        assertTrue(repo.isFavorite(stats))
        
        // 즐겨찾기 제거
        repo.removeFavorite(stats)
        assertFalse(repo.isFavorite(stats))
        
        tmpDir.deleteRecursively()
    }
    
    @Test
    fun filtering_works() {
        val tmpDir = createTempDir()
        val repo = JsonlFileRepository(tmpDir)
        
        // 여러 게임 추가
        for (i in 1..10) {
            val engine = GameEngine()
            engine.startGame(seed = i.toULong())
            val outcome = if (i % 2 == 0) "win" else null
            val stats = engine.getSolveStatsSnapshot(outcome)
            repo.appendPending(stats)
            
            // 일부만 즐겨찾기
            if (i <= 3) {
                repo.addFavorite(stats)
            }
        }
        
        // 필터 테스트
        val allGames = repo.readFilteredStats(GameFilter.ALL)
        assertEquals(10, allGames.size)
        
        val wins = repo.readFilteredStats(GameFilter.WIN)
        assertEquals(5, wins.size)
        
        val favorites = repo.readFilteredStats(GameFilter.FAVORITE)
        assertEquals(3, favorites.size)
        
        tmpDir.deleteRecursively()
    }
    
    @Test
    fun paging_works() {
        val tmpDir = createTempDir()
        val repo = JsonlFileRepository(tmpDir)
        
        // 25개 게임 추가
        for (i in 1..25) {
            val engine = GameEngine()
            engine.startGame(seed = i.toULong())
            val stats = engine.getSolveStatsSnapshot("win")
            repo.appendPending(stats)
        }
        
        // 첫 페이지
        val page1 = repo.readPagedStats(page = 0, pageSize = 10)
        assertEquals(10, page1.items.size)
        assertEquals(0, page1.page)
        assertEquals(25, page1.totalItems)
        assertEquals(3, page1.totalPages)
        assertTrue(page1.hasNext)
        assertFalse(page1.hasPrevious)
        
        // 두 번째 페이지
        val page2 = repo.readPagedStats(page = 1, pageSize = 10)
        assertEquals(10, page2.items.size)
        assertTrue(page2.hasNext)
        assertTrue(page2.hasPrevious)
        
        // 마지막 페이지
        val page3 = repo.readPagedStats(page = 2, pageSize = 10)
        assertEquals(5, page3.items.size)
        assertFalse(page3.hasNext)
        assertTrue(page3.hasPrevious)
        
        tmpDir.deleteRecursively()
    }
}
```

## 🚀 다음 단계

### Phase 1 ✅ (완료)
- ✅ 게임 통계 자동 저장
- ✅ 즐겨찾기 추가/제거/토글
- ✅ 필터링 (전체/즐겨찾기/성공/실패)
- ✅ 정렬 (6가지 옵션)
- ✅ 페이징 (효율적인 목록 표시)

### Phase 2 (다음)
- ⏳ StatisticsActivity UI 구현
- ⏳ RecyclerView 어댑터 구현
- ⏳ 필터/정렬 UI
- ⏳ 페이징 컨트롤

### Phase 3 (향후)
- 📊 통계 차트/그래프
- 🔍 검색 기능
- 📤 데이터 백업/복원
- ☁️ 클라우드 동기화

## 💡 사용 팁

1. **즐겨찾기 활용**
   - 재미있는 배치는 즐겨찾기 ⭐
   - 어려운 배치도 즐겨찾기해서 나중에 도전
   - 최고 기록 게임도 즐겨찾기

2. **필터 조합**
   - "즐겨찾기 + 이동 적은순" = 나의 베스트 게임들
   - "실패 + 최신순" = 복수할 게임 찾기
   - "성공 + 이동 많은순" = 어려웠지만 성공한 게임

3. **페이징 활용**
   - 한 번에 20개씩 보면서 천천히 탐색
   - 무한 스크롤로 부드러운 UX

---

**이제 수백 개의 게임도 쉽게 관리할 수 있습니다!** 🎮⭐
