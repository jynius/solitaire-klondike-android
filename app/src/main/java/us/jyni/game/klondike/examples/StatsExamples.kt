// 즐겨찾기, 필터링, 페이징 사용 예제
// 이 파일은 StatisticsActivity 구현 시 참고용입니다.

package us.jyni.game.klondike.examples

import us.jyni.game.klondike.sync.JsonlFileRepository
import us.jyni.game.klondike.sync.JsonlFileRepository.GameFilter
import us.jyni.game.klondike.sync.JsonlFileRepository.SortOrder
import us.jyni.game.klondike.util.stats.SolveStats

/**
 * 즐겨찾기 사용 예제
 */
fun exampleFavorites(repository: JsonlFileRepository) {
    // 모든 게임 가져오기
    val allGames = repository.readAllStats()
    
    if (allGames.isNotEmpty()) {
        val firstGame = allGames.first()
        
        // 즐겨찾기 추가
        repository.addFavorite(firstGame)
        println("⭐ 즐겨찾기 추가됨")
        
        // 즐겨찾기 여부 확인
        if (repository.isFavorite(firstGame)) {
            println("✅ 즐겨찾기입니다")
        }
        
        // 즐겨찾기 토글 (제거)
        val isNowFavorite = repository.toggleFavorite(firstGame)
        println(if (isNowFavorite) "⭐ 추가됨" else "☆ 제거됨")
        
        // 즐겨찾기 목록 조회
        val favorites = repository.readFavoriteStats()
        println("즐겨찾기 게임 ${favorites.size}개")
    }
}

/**
 * 필터링 사용 예제
 */
fun exampleFiltering(repository: JsonlFileRepository) {
    // 즐겨찾기 게임을 최신순으로
    val favoriteGames = repository.readFilteredStats(
        filter = GameFilter.FAVORITE,
        sortOrder = SortOrder.NEWEST_FIRST
    )
    println("⭐ 즐겨찾기: ${favoriteGames.size}개")
    
    // 성공한 게임을 이동 적은순으로 (최고 기록부터)
    val bestWins = repository.readFilteredStats(
        filter = GameFilter.WIN,
        sortOrder = SortOrder.LEAST_MOVES
    )
    if (bestWins.isNotEmpty()) {
        val best = bestWins.first()
        println("🏆 최고 기록: ${best.moveCount}수")
    }
    
    // 실패한 게임을 최신순으로
    val recentLosses = repository.readFilteredStats(
        filter = GameFilter.LOSS,
        sortOrder = SortOrder.NEWEST_FIRST
    )
    println("❌ 최근 실패: ${recentLosses.size}개")
    
    // 전체 게임을 이동 많은순으로 (어려웠던 게임)
    val hardGames = repository.readFilteredStats(
        filter = GameFilter.ALL,
        sortOrder = SortOrder.MOST_MOVES
    )
    if (hardGames.isNotEmpty()) {
        val hardest = hardGames.first()
        println("💪 가장 어려웠던 게임: ${hardest.moveCount}수")
    }
}

/**
 * 페이징 사용 예제
 */
fun examplePaging(repository: JsonlFileRepository) {
    // 첫 페이지 (0~19번째 게임)
    val page1 = repository.readPagedStats(
        page = 0,
        pageSize = 20,
        filter = GameFilter.ALL,
        sortOrder = SortOrder.NEWEST_FIRST
    )
    
    println("📄 페이지: ${page1.page + 1} / ${page1.totalPages}")
    println("   전체: ${page1.totalItems}개")
    println("   현재 페이지: ${page1.items.size}개")
    println("   다음 페이지: ${if (page1.hasNext) "있음" else "없음"}")
    println("   이전 페이지: ${if (page1.hasPrevious) "있음" else "없음"}")
    
    // 다음 페이지가 있으면 가져오기
    if (page1.hasNext) {
        val page2 = repository.readPagedStats(
            page = 1,
            pageSize = 20
        )
        println("📄 두 번째 페이지: ${page2.items.size}개")
    }
}

/**
 * 복합 사용 예제: 즐겨찾기한 성공 게임을 페이징으로
 */
fun exampleCombined(repository: JsonlFileRepository) {
    // 즐겨찾기한 게임 중 성공한 것만 필터링
    val favoriteWins = repository.readFavoriteStats()
        .filter { it.outcome == "win" }
        .sortedBy { it.moveCount }  // 이동 적은순
    
    println("⭐✅ 즐겨찾기 + 성공: ${favoriteWins.size}개")
    
    if (favoriteWins.isNotEmpty()) {
        val best = favoriteWins.first()
        println("   최고 기록: ${best.moveCount}수 (Seed: ${best.seed})")
    }
}

/**
 * 통계 계산 예제
 */
fun exampleStats(repository: JsonlFileRepository) {
    val allGames = repository.readAllStats()
    val favorites = repository.readFavoriteStats()
    val wins = repository.readWinStats()
    val losses = repository.readLossStats()
    
    println("📊 전체 통계")
    println("   총 게임: ${allGames.size}개")
    println("   승리: ${wins.size}개")
    println("   패배: ${losses.size}개")
    println("   즐겨찾기: ${favorites.size}개")
    
    if (allGames.isNotEmpty()) {
        val winRate = wins.size * 100.0 / allGames.size
        val avgMoves = allGames.map { it.moveCount }.average()
        val avgTime = allGames.map { it.durationMs }.average() / 1000
        
        println("   승률: ${String.format("%.1f", winRate)}%")
        println("   평균 이동: ${String.format("%.1f", avgMoves)}수")
        println("   평균 시간: ${String.format("%.1f", avgTime)}초")
    }
}

/**
 * 게임 재도전 예제
 */
fun exampleReplay(repository: JsonlFileRepository, game: SolveStats) {
    println("🎮 재도전 준비")
    println("   Seed: ${game.seed}")
    println("   규칙: Draw ${game.rules.draw}, Redeals ${game.rules.redeals}")
    println("   이전 기록: ${game.moveCount}수, ${game.durationMs/1000}초")
    println("   결과: ${game.outcome ?: "진행중"}")
    
    if (repository.isFavorite(game)) {
        println("   ⭐ 즐겨찾기 게임입니다")
    }
    
    // 실제 게임 시작은 Activity에서:
    // val intent = Intent(context, GameActivity::class.java)
    // intent.putExtra("SEED", game.seed.toLong())
    // intent.putExtra("RULES", game.rules)
    // startActivity(intent)
}

/**
 * 전체 예제 실행
 */
fun runAllExamples(repository: JsonlFileRepository) {
    println("=== 즐겨찾기 예제 ===")
    exampleFavorites(repository)
    println()
    
    println("=== 필터링 예제 ===")
    exampleFiltering(repository)
    println()
    
    println("=== 페이징 예제 ===")
    examplePaging(repository)
    println()
    
    println("=== 복합 사용 예제 ===")
    exampleCombined(repository)
    println()
    
    println("=== 통계 계산 예제 ===")
    exampleStats(repository)
    println()
    
    val allGames = repository.readAllStats()
    if (allGames.isNotEmpty()) {
        println("=== 게임 재도전 예제 ===")
        exampleReplay(repository, allGames.first())
    }
}
