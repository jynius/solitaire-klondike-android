# 게임 재도전 기능 가이드

## ✅ 구현 완료!

모든 게임 기록이 자동으로 저장되며, 실패한 게임이든 성공한 게임이든 언제든 재도전할 수 있습니다.

**🆕 즐겨찾기, 필터링, 페이징 기능도 추가되었습니다!**
👉 자세한 내용은 [FAVORITES_AND_FILTERING.md](FAVORITES_AND_FILTERING.md) 참조

## 🎮 핵심 기능

### 1. 자동 저장 📝
- 게임 승리 시 자동으로 통계 저장
- seed, 규칙, 소요 시간, 이동 횟수 모두 기록
- 파일 위치: `/data/data/us.jyni/files/solves/pending.sv1`

### 2. 완벽한 재현 🔄
```kotlin
// 특정 게임 재도전
val seed = 12345uL
val rules = Ruleset(draw = 3, redeals = 1)
viewModel.replayGame(seed, rules)
// ➡️ 동일한 카드 배치로 게임 시작!
```

### 3. 모든 기록 조회 📊
```kotlin
val repository = JsonlFileRepository(context)

// 모든 게임 (성공 + 실패)
val allGames = repository.readAllStats()

// 승리한 게임만
val winGames = repository.readWinStats()

// 패배/포기한 게임만
val lossGames = repository.readLossStats()
```

## 💡 사용 예시

### 예시 1: 실패한 게임 목록 보기
```kotlin
val repository = JsonlFileRepository(this)
val lossGames = repository.readLossStats()

lossGames.forEach { game ->
    println("패배한 게임 - 이동: ${game.moveCount}수, 시간: ${game.durationMs}ms")
    println("  Seed: ${game.seed}")
    println("  규칙: Draw ${game.rules.draw}, Redeals ${game.rules.redeals}")
    println("  [이 게임 재도전 가능!]")
}
```

출력 예:
```
패배한 게임 - 이동: 87수, 시간: 245000ms
  Seed: 9876543210
  규칙: Draw 3, Redeals 1
  [이 게임 재도전 가능!]
  
패배한 게임 - 이동: 134수, 시간: 412000ms
  Seed: 1234567890
  규칙: Draw 1, Redeals -1
  [이 게임 재도전 가능!]
```

### 예시 2: 최소 이동 기록 갱신하기
```kotlin
val repository = JsonlFileRepository(this)
val winGames = repository.readWinStats()

// 최소 이동으로 승리한 게임 찾기
val bestMoveGame = winGames.minByOrNull { it.moveCount }

bestMoveGame?.let { game ->
    println("현재 최고 기록: ${game.moveCount}수")
    println("이 게임을 다시 플레이해서 더 적은 이동으로 완료해보세요!")
    
    // 재도전
    val intent = Intent(context, GameActivity::class.java)
    intent.putExtra("SEED", game.seed.toLong())
    intent.putExtra("RULES", game.rules)
    startActivity(intent)
}
```

### 예시 3: 어려웠던 게임 재도전
```kotlin
val repository = JsonlFileRepository(this)
val allGames = repository.readAllStats()

// 이동 횟수가 많았던 게임 = 어려웠던 게임
val hardGames = allGames
    .filter { it.outcome == "win" }
    .sortedByDescending { it.moveCount }
    .take(5)

hardGames.forEach { game ->
    println("어려웠던 게임 - ${game.moveCount}수 소요")
    println("  더 적은 이동으로 도전해보세요!")
}
```

### 예시 4: 최근 실패한 게임들
```kotlin
val repository = JsonlFileRepository(this)
val recentLosses = repository.readLossStats()
    .sortedByDescending { it.startedAt }
    .take(10)

println("최근 실패한 게임 ${recentLosses.size}개")
recentLosses.forEach { game ->
    val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        .format(Date(game.startedAt))
    println("[$date] ${game.moveCount}수, ${game.durationMs/1000}초 - 재도전 가능")
}
```

출력 예:
```
최근 실패한 게임 5개
[02-06 14:23] 87수, 245초 - 재도전 가능
[02-06 13:45] 134수, 412초 - 재도전 가능
[02-05 21:12] 92수, 287초 - 재도전 가능
[02-05 19:08] 156수, 523초 - 재도전 가능
[02-05 15:34] 78수, 198초 - 재도전 가능
```

## 🏆 재도전 시나리오

### 시나리오 A: "복수하기" 💪
```
1. 어제 아깝게 진 게임이 있음
2. 통계에서 해당 게임 찾기
3. seed와 rules 확인
4. [재도전] 클릭
5. 같은 배치로 시작
6. 이번엔 성공! 🎉
```

### 시나리오 B: "기록 경신" 🏅
```
1. 현재 최고 기록: 102수로 승리
2. "더 잘할 수 있어!"
3. 해당 게임 재도전
4. 95수로 완료
5. 새로운 최고 기록! ⭐
```

### 시나리오 C: "학습 모드" 📚
```
1. 특정 배치가 어려움
2. 같은 게임을 3-4번 반복
3. 각 플레이마다 다른 전략 시도
4. 최적 전략 발견
5. 완벽한 플레이 달성! 🎓
```

## 📊 통계 활용

### 승률 분석
```kotlin
val repository = JsonlFileRepository(this)
val allGames = repository.readAllStats()

val total = allGames.size
val wins = allGames.count { it.outcome == "win" }
val winRate = if (total > 0) wins * 100.0 / total else 0.0

println("총 $total 게임 중 $wins 승리 (${String.format("%.1f", winRate)}%)")
```

### 규칙별 비교
```kotlin
val draw1Games = allGames.filter { it.rules.draw == 1 }
val draw3Games = allGames.filter { it.rules.draw == 3 }

val draw1WinRate = draw1Games.count { it.outcome == "win" } * 100.0 / draw1Games.size
val draw3WinRate = draw3Games.count { it.outcome == "win" } * 100.0 / draw3Games.size

println("Draw 1 승률: ${String.format("%.1f", draw1WinRate)}%")
println("Draw 3 승률: ${String.format("%.1f", draw3WinRate)}%")
```

### 평균 플레이 시간
```kotlin
val avgTime = allGames
    .filter { it.finishedAt != null }
    .map { it.durationMs }
    .average()

val minutes = (avgTime / 60000).toInt()
val seconds = ((avgTime % 60000) / 1000).toInt()
println("평균 플레이 시간: ${minutes}분 ${seconds}초")
```

## 🔧 구현된 메서드

### GameViewModel
```kotlin
// 현재 게임 통계 가져오기
fun getSolveStatsSnapshot(outcomeOverride: String? = null): SolveStats

// 특정 seed와 rules로 재시작
fun replayGame(seed: ULong, rules: Ruleset)
```

### JsonlFileRepository
```kotlin
// 게임 기록 저장
fun appendPending(stats: SolveStats)

// 모든 게임 조회
fun readAllStats(): List<SolveStats>

// 승리한 게임만
fun readWinStats(): List<SolveStats>

// 패배한 게임만
fun readLossStats(): List<SolveStats>
```

### GameActivity
```kotlin
// 게임 완료 시 자동 저장
private fun saveGameStats(outcome: String)

// 승리 시 자동 호출됨
private fun showVictoryDialog() {
    // ...
    saveGameStats("win")
    // ...
}
```

## 🚀 다음 단계

### Phase 1 (구현 완료 ✅)
- ✅ 게임 통계 자동 저장
- ✅ 모든 게임 기록 조회
- ✅ seed/rules 기반 재도전 기능

### Phase 2 (구현 예정)
- ⏳ 통계 화면 UI (StatisticsActivity)
- ⏳ 게임 목록 RecyclerView
- ⏳ 각 게임에 [재도전] 버튼
- ⏳ 최고 기록 표시

### Phase 3 (향후 계획)
- 📊 그래프/차트 시각화
- 🎯 필터링/정렬 기능
- 📤 데이터 내보내기/가져오기
- 🔍 상세 검색 기능

## 💡 팁

1. **실패해도 OK!** - 모든 게임이 저장되므로 실패한 게임도 다시 도전 가능
2. **학습 도구로 활용** - 어려운 배치를 여러 번 반복해서 전략 학습
3. **기록 경신** - 성공한 게임도 더 나은 기록으로 도전 가능
4. **규칙 실험** - Draw 1 vs Draw 3, Redeals 등 다양한 규칙 비교

---

**모든 게임이 소중한 학습 기회입니다!** 🎮✨
