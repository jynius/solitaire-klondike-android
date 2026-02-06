# 통계 페이지 기능 명세

## ✅ 구현 완료 사항

### 1. 게임 통계 자동 저장
- ✅ `GameActivity`에서 게임 승리 시 자동으로 SolveStats 저장
- ✅ `JsonlFileRepository`에 pending.sv1 파일로 저장
- ✅ GameViewModel에 `getSolveStatsSnapshot()` 메서드 추가
- ✅ GameViewModel에 `replayGame()` 메서드 추가 (재도전 기능)

### 2. 통계 조회 기능
- ✅ `readAllStats()`: 모든 게임 기록 조회 (승리 + 패배)
- ✅ `readWinStats()`: 승리한 게임만 조회
- ✅ `readLossStats()`: 패배/포기한 게임 조회

### 3. 즐겨찾기 기능 ⭐
- ✅ `addFavorite()`: 즐겨찾기 추가
- ✅ `removeFavorite()`: 즐겨찾기 제거
- ✅ `toggleFavorite()`: 즐겨찾기 토글 (추가 ↔ 제거)
- ✅ `isFavorite()`: 즐겨찾기 여부 확인
- ✅ `readFavoriteStats()`: 즐겨찾기한 게임만 조회

### 4. 필터링 & 정렬 기능 🔍
- ✅ `GameFilter`: ALL, FAVORITE, WIN, LOSS
- ✅ `SortOrder`: 최신순, 오래된순, 이동순, 시간순 (6가지)
- ✅ `readFilteredStats()`: 필터링 + 정렬된 목록 조회

### 5. 페이징 기능 📄
- ✅ `readPagedStats()`: 페이징된 게임 목록 조회
- ✅ `PagedResult`: 페이지 정보 포함 결과 (hasNext, hasPrevious 등)
- ✅ 효율적인 대량 데이터 처리

### 6. 저장 파일 위치
```
/data/data/us.jyni/files/solves/
  ├── pending.sv1     (업로드 대기 중인 게임 기록)
  ├── uploaded.sv1    (업로드 완료된 게임 기록)
  └── favorites.txt   (즐겨찾기 dealId 목록)
```

## 📊 현재 수집 중인 데이터 (SolveStats)

프로젝트는 이미 다음 정보를 수집하고 있습니다:

- **dealId**: 게임 배치 고유 ID (예: DL1_12345...)
- **seed**: 게임 시드 (ULong) - 동일한 배치 재현 가능
- **rules**: 게임 규칙 (draw count, redeals, recycle, foundation-to-tableau)
- **startedAt**: 게임 시작 시간 (타임스탬프)
- **finishedAt**: 게임 종료 시간 (타임스탬프)
- **durationMs**: 소요 시간 (밀리초)
- **moveCount**: 이동 횟수
- **outcome**: 게임 결과 (win/resign/timeout/null)
- **layoutId**: 레이아웃 ID
- **platform**: 플랫폼 ("android")

## 🎯 통계 페이지 출력 내용

### 1. 전체 통계 (Overall Statistics)

```
┌─────────────────────────────────────┐
│ 📊 전체 통계                         │
├─────────────────────────────────────┤
│ 총 게임 수: 128                      │
│ ├─ 승리: 64 (50.0%)                 │
│ ├─ 패배/포기: 60 (46.9%)            │
│ └─ 진행 중: 4 (3.1%)                │
│                                      │
│ ⏱️  평균 플레이 시간: 4분 15초       │
│ 🎯 평균 이동 횟수: 98.5수            │
│ 🔥 최장 연승: 7게임                  │
│ 💪 총 플레이 시간: 9시간 12분        │
└─────────────────────────────────────┘
```

**계산 방법:**
- 승률 = (outcome="win" 게임 수) / (총 게임 수) × 100
- 평균 플레이 시간 = sum(durationMs) / 완료된 게임 수
- 평균 이동 횟수 = sum(moveCount) / 완료된 게임 수
- 최장 연승 = 연속된 "win" outcome의 최대 길이

### 2. 최고 기록 (Personal Best) 🏆

```
┌─────────────────────────────────────┐
│ 🏆 최고 기록                         │
├─────────────────────────────────────┤
│ ⚡ 최소 이동 승리                    │
│    52수 (2026-01-15 14:23)          │
│    [재도전] 버튼                     │
│                                      │
│ 🚀 최단 시간 승리                    │
│    1분 23초 (2026-02-01 09:15)      │
│    [재도전] 버튼                     │
│                                      │
│ 🔥 최장 연승                         │
│    7게임 (2026-01-20 ~ 2026-01-22)  │
│                                      │
│ 💎 완벽한 게임 (언두 없음)           │
│    15게임                            │
└─────────────────────────────────────┘
```

**계산 방법:**
- 최소 이동 승리 = min(moveCount) where outcome="win"
- 최단 시간 승리 = min(durationMs) where outcome="win"
- 최장 연승 = 연속된 win의 최대 개수
- 완벽한 게임 = 언두 없이 승리 (향후 구현)

**🎮 재도전 기능:**
- 각 기록 옆에 "재도전" 버튼 제공
- 클릭 시 해당 게임의 seed와 rules를 사용하여 새 게임 시작
- `viewModel.startGame(seed, rules)` 호출

### 3. 규칙별 통계

```
┌─────────────────────────────────────┐
│ 📋 규칙별 통계                       │
├─────────────────────────────────────┤
│ Draw 1                               │
│   승률: 65.2% (23승 / 35게임)       │
│   평균 시간: 3분 12초                │
│   평균 이동: 76.3수                  │
│                                      │
│ Draw 3                               │
│   승률: 42.8% (40승 / 93게임)       │
│   평균 시간: 5분 45초                │
│   평균 이동: 112.8수                 │
│                                      │
│ Unlimited Redeals                    │
│   승률: 48.5%                        │
│                                      │
│ Limited Redeals (3회)                │
│   승률: 52.1%                        │
└─────────────────────────────────────┘
```

**필터링:**
- `rules.draw` 값으로 그룹화
- `rules.redeals` 값으로 그룹화

### 4. 시간대별 통계

```
┌─────────────────────────────────────┐
│ 📅 시간대별 통계                     │
├─────────────────────────────────────┤
│ 오늘      5게임  (승률 60.0%)       │
│ 이번 주   23게임 (승률 52.2%)       │
│ 이번 달   87게임 (승률 49.4%)       │
│ 전체 기간 128게임 (승률 50.0%)      │
└─────────────────────────────────────┘
```

**필터링:**
- 오늘 = startedAt이 오늘 00:00 이후
- 이번 주 = startedAt이 이번 주 월요일 00:00 이후
- 이번 달 = startedAt이 이번 달 1일 00:00 이후

### 5. 최근 플레이 기록 (클릭 가능)

```
┌──────────────────────────────────────────────────────────┐
│ 📜 최근 플레이 기록                                       │
├──────────────────────────────────────────────────────────┤
│ 2026-02-06 14:23  │ 4:23  │ 102수 │ ✅ 승리 │ Draw 3   │
│                   │       │       │ [재도전]            │
│                                                           │
│ 2026-02-06 13:45  │ 3:45  │  89수 │ ❌ 패배 │ Draw 1   │
│                   │       │       │ [재도전]            │
│                                                           │
│ 2026-02-05 21:12  │ 5:12  │ 127수 │ ✅ 승리 │ Draw 3   │
│                   │       │       │ [재도전]            │
│                                                           │
│ ...                                                       │
│                                                           │
│ [더 보기]                                                 │
└──────────────────────────────────────────────────────────┘
```

**기능:**
- 최근 10~20개 게임 표시
- 각 항목 클릭 시 상세 정보 표시 (선택사항)
- "재도전" 버튼으로 해당 seed/rules로 새 게임 시작
- 날짜/시간/소요시간/이동수/결과/규칙 표시

### 6. 효율성 지표 (Phase 2)

```
┌─────────────────────────────────────┐
│ 📈 효율성 지표                       │
├─────────────────────────────────────┤
│ 평균 이동 시간: 2.6초/수             │
│ 언두 사용률: 평균 12.3회/게임        │
│ 첫 이동까지 평균 사고 시간: 3.2초    │
│                                      │
│ 승리 게임 평균 시간: 3분 42초        │
│ 패배 게임 평균 시간: 5분 18초        │
└─────────────────────────────────────┘
```

**계산 방법:**
- 평균 이동 시간 = durationMs / moveCount
- 승리 게임 평균 시간 = avg(durationMs) where outcome="win"
- 패배 게임 평균 시간 = avg(durationMs) where outcome!="win"

**참고:** 언두 사용률, 첫 이동 시간은 현재 수집되지 않으므로 향후 추가 필요

### 7. 추이 그래프 (Phase 3)

- 일별/주별/월별 플레이 횟수 그래프
- 승률 추이 그래프
- 평균 이동 횟수 변화 그래프
- 평균 플레이 시간 변화 그래프

**구현 방법:** MPAndroidChart 또는 유사 라이브러리 사용

## 🚀 구현 우선순위

### Phase 1 (필수) - 기본 통계
- ✅ 총 게임 수 / 승률 / 승-패 횟수
- ✅ 평균 플레이 시간 / 평균 이동 횟수
- ✅ 최고 기록 (최소 이동, 최단 시간)
- ✅ **재도전 기능** (seed + rules로 게임 재시작)
- ✅ 최근 플레이 기록 목록

### Phase 2 (권장) - 상세 통계
- 규칙별 통계 (Draw 1 vs Draw 3)
- 시간대별 통계 (오늘/이번 주/이번 달)
- 최장 연승 기록
- 효율성 지표 (이동당 시간 등)

### Phase 3 (추가) - 시각화 & 고급 기능
- 그래프/차트 시각화
- 데이터 내보내기/가져오기
- 상세 기록 화면 (각 게임의 자세한 정보)
- 필터링/정렬 기능

## 💾 데이터 저장소 구현

### 옵션 1: SharedPreferences (간단)
- SolveStats를 JSON 또는 인코딩된 문자열로 저장
- 최근 N개 게임만 유지 (예: 100개)
- 간단하지만 대량 데이터 처리 제한적

### 옵션 2: Room Database (권장)
- SolveStats를 SQLite 테이블로 저장
- 복잡한 쿼리 가능 (집계, 필터링, 정렬)
- 무제한 기록 저장 가능

### 옵션 3: 기존 JsonlFileRepository 활용
- 이미 구현된 `JsonlFileRepository` 사용
- SV1 포맷으로 저장 중
- 읽기/쓰기 메서드 추가 필요

## 🎮 재도전 기능 구현

### GameViewModel에 메서드 추가

```kotlin
/**
 * 특정 seed와 rules로 게임 재시작
 * 통계 화면에서 과거 게임 재도전에 사용
 */
fun replayGame(seed: ULong, rules: Ruleset) {
    Log.d("GameViewModel", "Replaying game with seed: $seed, rules: $rules")
    startGame(seed, rules)
}
```

### 통계 화면에서 사용

```kotlin
// 재도전 버튼 클릭 시
replaySeedButton.setOnClickListener {
    val seed = gameRecord.seed
    val rules = gameRecord.rules
    
    // GameActivity로 돌아가면서 해당 seed로 게임 시작
    val intent = Intent(this, GameActivity::class.java).apply {
        putExtra("SEED", seed.toLong())
        putExtra("RULES", rules)
        putExtra("IS_REPLAY", true)
    }
    startActivity(intent)
    finish()
}
```

## 📱 UI 구현 제안

### StatisticsActivity 생성
- RecyclerView로 최근 게임 목록 표시
- 각 항목에 "재도전" 버튼
- 상단에 전체 통계 카드뷰들

### 레이아웃 구조
```
ScrollView
  ├─ OverallStatsCard (총 게임, 승률, 평균)
  ├─ PersonalBestCard (최고 기록들 + 재도전 버튼)
  ├─ RulesStatsCard (규칙별 통계)
  ├─ TimelineStatsCard (오늘/이번주/이번달)
  └─ RecentGamesRecyclerView (최근 게임 목록 + 재도전)
```

## 🔄 기존 코드 활용

### 이미 구현된 기능
1. ✅ `GameEngine.getSeed()` - 현재 seed 조회
2. ✅ `GameViewModel.restartGame()` - 같은 seed로 재시작
3. ✅ `SolveStats` - 게임 통계 데이터 구조
4. ✅ `SolveCodec` - SolveStats 직렬화/역직렬화
5. ✅ `JsonlFileRepository` - JSONL 파일 저장소

### 필요한 새 기능
1. ❌ 완료된 게임의 SolveStats 저장 (GameActivity에서)
2. ❌ 저장된 SolveStats 목록 조회
3. ❌ StatisticsActivity UI
4. ❌ 통계 계산 로직 (승률, 평균 등)

## 📝 구현 예시

### 통계 화면에서 모든 게임 기록 조회

```kotlin
class StatisticsActivity : AppCompatActivity() {
    private lateinit var repository: JsonlFileRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        repository = JsonlFileRepository(this)
        
        // 모든 게임 기록 가져오기
        val allGames = repository.readAllStats()
        val winGames = repository.readWinStats()
        val lossGames = repository.readLossStats()
        
        // 전체 통계 계산
        val totalGames = allGames.size
        val totalWins = winGames.size
        val winRate = if (totalGames > 0) (totalWins * 100.0 / totalGames) else 0.0
        
        // 평균 시간 및 이동 수
        val avgTime = allGames.map { it.durationMs }.average()
        val avgMoves = allGames.map { it.moveCount }.average()
        
        // 최고 기록
        val bestMoveGame = winGames.minByOrNull { it.moveCount }
        val bestTimeGame = winGames.minByOrNull { it.durationMs }
        
        // UI 업데이트
        displayStats(totalGames, winRate, avgTime, avgMoves)
        displayBestRecords(bestMoveGame, bestTimeGame)
        displayRecentGames(allGames.takeLast(10).reversed())
    }
    
    private fun displayRecentGames(games: List<SolveStats>) {
        // RecyclerView로 게임 목록 표시
        games.forEach { game ->
            // 각 게임에 "재도전" 버튼 추가
            addGameItem(game) { replayGame(game) }
        }
    }
    
    private fun replayGame(game: SolveStats) {
        // 해당 게임의 seed와 rules로 새 게임 시작
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("SEED", game.seed.toLong())
        intent.putExtra("RULES", game.rules)
        intent.putExtra("IS_REPLAY", true)
        startActivity(intent)
    }
}
```

### GameActivity에서 재도전 게임 시작

```kotlin
class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ... (기존 코드)
        
        // 재도전 모드로 시작하는 경우
        val isReplay = intent.getBooleanExtra("IS_REPLAY", false)
        if (isReplay) {
            val seed = intent.getLongExtra("SEED", 0L).toULong()
            val rules = intent.getSerializableExtra("RULES") as? Ruleset
            if (seed > 0uL && rules != null) {
                viewModel.replayGame(seed, rules)
                Toast.makeText(this, "재도전 모드: 같은 배치로 시작합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### 통계 계산 예제

```kotlin
// 승률 계산
fun calculateWinRate(stats: List<SolveStats>): Double {
    val total = stats.size
    val wins = stats.count { it.outcome == "win" }
    return if (total > 0) wins * 100.0 / total else 0.0
}

// 평균 플레이 시간
fun calculateAverageTime(stats: List<SolveStats>): Long {
    val completed = stats.filter { it.finishedAt != null }
    return if (completed.isNotEmpty()) {
        completed.map { it.durationMs }.average().toLong()
    } else 0L
}

// 평균 이동 횟수
fun calculateAverageMoves(stats: List<SolveStats>): Double {
    return if (stats.isNotEmpty()) {
        stats.map { it.moveCount }.average()
    } else 0.0
}

// 최장 연승 기록
fun calculateLongestWinStreak(stats: List<SolveStats>): Int {
    var maxStreak = 0
    var currentStreak = 0
    
    stats.sortedBy { it.startedAt }.forEach { game ->
        if (game.outcome == "win") {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else {
            currentStreak = 0
        }
    }
    
    return maxStreak
}

// 규칙별 통계
fun getStatsByRule(stats: List<SolveStats>, draw: Int): Map<String, Any> {
    val filtered = stats.filter { it.rules.draw == draw }
    val total = filtered.size
    val wins = filtered.count { it.outcome == "win" }
    val winRate = if (total > 0) wins * 100.0 / total else 0.0
    val avgTime = if (filtered.isNotEmpty()) filtered.map { it.durationMs }.average() else 0.0
    val avgMoves = if (filtered.isNotEmpty()) filtered.map { it.moveCount }.average() else 0.0
    
    return mapOf(
        "total" to total,
        "wins" to wins,
        "winRate" to winRate,
        "avgTime" to avgTime,
        "avgMoves" to avgMoves
    )
}

// 시간대별 필터링 (오늘/이번 주/이번 달)
fun filterByTimeRange(stats: List<SolveStats>, range: TimeRange): List<SolveStats> {
    val now = System.currentTimeMillis()
    val cutoff = when (range) {
        TimeRange.TODAY -> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.timeInMillis
        }
        TimeRange.THIS_WEEK -> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.timeInMillis
        }
        TimeRange.THIS_MONTH -> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.timeInMillis
        }
        TimeRange.ALL -> 0L
    }
    
    return stats.filter { it.startedAt >= cutoff }
}

enum class TimeRange {
    TODAY, THIS_WEEK, THIS_MONTH, ALL
}
```

## 🎮 사용 시나리오

사용자가 통계 화면에서:
1. 전체 플레이 기록 확인
2. 최고 기록 조회
3. **실패한 게임을 다시 도전하여 성공 시도**
4. **성공한 게임을 더 좋은 기록으로 갱신 시도**
5. 다양한 규칙별 성과 비교
