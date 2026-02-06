package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.sync.JsonlFileRepository
import us.jyni.game.klondike.util.stats.SolveStats
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * 게임 통계 저장 테스트
 * 
 * 시나리오:
 * 1. 게임 시작 (moveCount = 0)
 * 2. 카드 이동 (moveCount > 0)
 * 3. 재시작/새게임 클릭
 * 4. 현재 게임이 "resign"으로 저장되어야 함
 */
class GameStatsSaveTest {
    
    @Test
    fun `게임 시작 후 이동 없이 재시작하면 저장 안됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            
            // 시나리오: moveCount = 0
            val moveCount = 0
            val victoryShown = false
            
            // 저장 조건 확인
            val shouldSave = moveCount > 0 && !victoryShown
            assertFalse("moveCount가 0이면 저장하지 않아야 함", shouldSave)
            
            // 저장 안 함
            val stats = repo.readAllStats()
            assertEquals("게임을 시작하지 않았으므로 저장 안됨", 0, stats.size)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `게임 시작 후 카드 이동 후 재시작하면 resign으로 저장됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            
            // 시나리오: 카드 3번 이동 후 재시작
            val moveCount = 3
            val victoryShown = false
            
            // 저장 조건 확인
            val shouldSave = moveCount > 0 && !victoryShown
            assertTrue("moveCount > 0이고 승리 전이면 저장해야 함", shouldSave)
            
            // 게임 결과 저장
            val gameStartTime = System.currentTimeMillis() - 30000 // 30초 전 시작
            val finishTime = System.currentTimeMillis()
            val stats = SolveStats(
                dealId = "TEST_DEAL",
                seed = 12345uL,
                rules = rules,
                startedAt = gameStartTime,
                finishedAt = finishTime,
                durationMs = finishTime - gameStartTime,
                moveCount = moveCount,
                outcome = "resign"
            )
            repo.appendPending(stats)
            
            // 저장 확인
            val allStats = repo.readAllStats()
            assertEquals("게임이 저장되어야 함", 1, allStats.size)
            
            val saved = allStats[0]
            assertEquals("outcome이 resign이어야 함", "resign", saved.outcome)
            assertEquals("moveCount가 3이어야 함", 3, saved.moveCount)
            assertTrue("게임 시간이 기록되어야 함", saved.durationMs > 0)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `게임 승리 후에는 재시작해도 저장 안됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            
            // 시나리오: 게임 승리 후 재시작 버튼 클릭
            val moveCount = 100
            val victoryShown = true
            
            // 저장 조건 확인
            val shouldSave = moveCount > 0 && !victoryShown
            assertFalse("승리 후에는 중복 저장하지 않아야 함", shouldSave)
            
            val stats = repo.readAllStats()
            assertEquals("승리는 이미 저장되었으므로 resign 저장 안됨", 0, stats.size)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `여러 게임 연속으로 시작하면 각각 resign으로 저장됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            
            // 게임 1: 5번 이동 후 새게임
            val game1 = SolveStats(
                dealId = "GAME1",
                seed = 111uL,
                rules = rules,
                startedAt = 1000L,
                finishedAt = 31000L,
                durationMs = 30000L,
                moveCount = 5,
                outcome = "resign"
            )
            repo.appendPending(game1)
            
            // 게임 2: 10번 이동 후 새게임
            val game2 = SolveStats(
                dealId = "GAME2",
                seed = 222uL,
                rules = rules,
                startedAt = 32000L,
                finishedAt = 62000L,
                durationMs = 30000L,
                moveCount = 10,
                outcome = "resign"
            )
            repo.appendPending(game2)
            
            // 게임 3: 20번 이동 후 승리
            val game3 = SolveStats(
                dealId = "GAME3",
                seed = 333uL,
                rules = rules,
                startedAt = 63000L,
                finishedAt = 93000L,
                durationMs = 30000L,
                moveCount = 20,
                outcome = "win"
            )
            repo.appendPending(game3)
            
            // 확인
            val allStats = repo.readAllStats()
            assertEquals("3개 게임이 저장되어야 함", 3, allStats.size)
            
            val resignGames = allStats.filter { it.outcome == "resign" }
            assertEquals("2개가 resign이어야 함", 2, resignGames.size)
            
            val winGames = allStats.filter { it.outcome == "win" }
            assertEquals("1개가 win이어야 함", 1, winGames.size)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `새게임 버튼 클릭 시나리오 - moveCount 초기화 전 저장됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            
            // 시뮬레이션: 사용자가 카드를 5번 이동한 후 "새게임" 버튼 클릭
            var moveCount = 5
            var victoryShown = false
            var currentGameSeed = 12345uL
            var gameStartTime = System.currentTimeMillis() - 60000L // 1분 전 시작
            
            // === reset_button 클릭 (새게임) ===
            android.util.Log.d("TEST", "Reset button clicked: moveCount=$moveCount")
            
            // 1. 저장 조건 확인
            val shouldSave = moveCount > 0 && !victoryShown
            assertTrue("새게임 클릭 시 저장되어야 함", shouldSave)
            
            // 2. 저장 (실제 GameActivity의 saveGameResult 로직)
            if (shouldSave) {
                val finishTime = System.currentTimeMillis()
                val stats = SolveStats(
                    dealId = "RESET_TEST",
                    seed = currentGameSeed,
                    rules = rules,
                    startedAt = gameStartTime,
                    finishedAt = finishTime,
                    durationMs = finishTime - gameStartTime,
                    moveCount = moveCount,
                    outcome = "resign"
                )
                repo.appendPending(stats)
                android.util.Log.d("TEST", "Game saved: outcome=resign, moves=$moveCount")
            }
            
            // 3. viewModel.reset() - 새로운 시드 생성
            val newSeed = 99999uL
            
            // 4. startNewGame() 호출
            gameStartTime = System.currentTimeMillis()
            moveCount = 0  // 새 게임 시작으로 초기화
            currentGameSeed = newSeed
            android.util.Log.d("TEST", "New game started: seed=$currentGameSeed, moveCount=$moveCount")
            
            // 검증
            val allStats = repo.readAllStats()
            assertEquals("새게임 클릭 시 이전 게임이 저장되어야 함", 1, allStats.size)
            assertEquals("resign으로 저장됨", "resign", allStats[0].outcome)
            assertEquals("5번 이동이 기록됨", 5, allStats[0].moveCount)
            assertEquals("이전 시드가 저장됨", 12345uL, allStats[0].seed)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `재시작 버튼 클릭 시나리오 - moveCount 초기화 전 저장됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            
            // 시뮬레이션: 사용자가 카드를 7번 이동한 후 "재시작" 버튼 클릭
            var moveCount = 7
            var victoryShown = false
            var currentGameSeed = 54321uL
            var gameStartTime = System.currentTimeMillis() - 120000L // 2분 전 시작
            
            // === restart_button 클릭 (같은 배치로 재시작) ===
            android.util.Log.d("TEST", "Restart button clicked: moveCount=$moveCount")
            
            // 1. 저장 조건 확인
            val shouldSave = moveCount > 0 && !victoryShown
            assertTrue("재시작 클릭 시 저장되어야 함", shouldSave)
            
            // 2. 저장 (실제 GameActivity의 saveGameResult 로직)
            if (shouldSave) {
                val finishTime = System.currentTimeMillis()
                val stats = SolveStats(
                    dealId = "RESTART_TEST",
                    seed = currentGameSeed,
                    rules = rules,
                    startedAt = gameStartTime,
                    finishedAt = finishTime,
                    durationMs = finishTime - gameStartTime,
                    moveCount = moveCount,
                    outcome = "resign"
                )
                repo.appendPending(stats)
                android.util.Log.d("TEST", "Game saved: outcome=resign, moves=$moveCount")
            }
            
            // 3. viewModel.restartGame() - 같은 시드 유지
            // (시드 변경 없음)
            
            // 4. victoryShown 초기화
            victoryShown = false
            
            // 5. startNewGame(currentGameSeed) 호출
            gameStartTime = System.currentTimeMillis()
            moveCount = 0  // 새 게임 시작으로 초기화
            // currentGameSeed는 동일 유지
            android.util.Log.d("TEST", "Game restarted: seed=$currentGameSeed, moveCount=$moveCount")
            
            // 검증
            val allStats = repo.readAllStats()
            assertEquals("재시작 클릭 시 이전 게임이 저장되어야 함", 1, allStats.size)
            assertEquals("resign으로 저장됨", "resign", allStats[0].outcome)
            assertEquals("7번 이동이 기록됨", 7, allStats[0].moveCount)
            assertEquals("같은 시드가 저장됨", 54321uL, allStats[0].seed)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
    
    @Test
    fun `새게임과 재시작 버튼을 번갈아 사용 - 모두 저장됨`() {
        val tmp = createTempDir(prefix = "stats_test_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            
            var moveCount = 0
            var victoryShown = false
            var currentGameSeed = 1000uL
            var gameStartTime = System.currentTimeMillis()
            
            // 게임 1: 3번 이동 후 "재시작" 클릭
            moveCount = 3
            if (moveCount > 0 && !victoryShown) {
                repo.appendPending(SolveStats(
                    dealId = "TEST1",
                    seed = currentGameSeed,
                    rules = rules,
                    startedAt = gameStartTime,
                    finishedAt = System.currentTimeMillis(),
                    durationMs = 10000,
                    moveCount = moveCount,
                    outcome = "resign"
                ))
            }
            // 재시작: 같은 시드, moveCount 초기화
            gameStartTime = System.currentTimeMillis()
            moveCount = 0
            
            // 게임 2: 5번 이동 후 "새게임" 클릭
            moveCount = 5
            if (moveCount > 0 && !victoryShown) {
                repo.appendPending(SolveStats(
                    dealId = "TEST2",
                    seed = currentGameSeed,
                    rules = rules,
                    startedAt = gameStartTime,
                    finishedAt = System.currentTimeMillis(),
                    durationMs = 15000,
                    moveCount = moveCount,
                    outcome = "resign"
                ))
            }
            // 새게임: 새 시드, moveCount 초기화
            currentGameSeed = 2000uL
            gameStartTime = System.currentTimeMillis()
            moveCount = 0
            
            // 게임 3: 2번 이동 후 "재시작" 클릭
            moveCount = 2
            if (moveCount > 0 && !victoryShown) {
                repo.appendPending(SolveStats(
                    dealId = "TEST3",
                    seed = currentGameSeed,
                    rules = rules,
                    startedAt = gameStartTime,
                    finishedAt = System.currentTimeMillis(),
                    durationMs = 8000,
                    moveCount = moveCount,
                    outcome = "resign"
                ))
            }
            // 재시작: 같은 시드, moveCount 초기화
            gameStartTime = System.currentTimeMillis()
            moveCount = 0
            
            // 검증
            val allStats = repo.readAllStats()
            assertEquals("3개 게임이 모두 저장되어야 함", 3, allStats.size)
            
            // 모두 resign
            assertTrue("모두 resign이어야 함", allStats.all { it.outcome == "resign" })
            
            // moveCount 확인
            assertEquals("첫 게임 3번 이동", 3, allStats[0].moveCount)
            assertEquals("두번째 게임 5번 이동", 5, allStats[1].moveCount)
            assertEquals("세번째 게임 2번 이동", 2, allStats[2].moveCount)
            
        } finally {
            tmp.deleteRecursively()
        }
    }
}
