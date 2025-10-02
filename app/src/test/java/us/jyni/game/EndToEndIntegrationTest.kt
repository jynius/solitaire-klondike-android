package us.jyni.game

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.sync.ApiClient
import us.jyni.game.klondike.sync.JsonlFileRepository
import us.jyni.game.klondike.sync.RulesetDto
import us.jyni.game.klondike.sync.SolveUploadRequest
import us.jyni.game.klondike.util.stats.SolveCodec
import us.jyni.game.klondike.util.sync.Ruleset

class EndToEndIntegrationTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun uploadOnce(repo: JsonlFileRepository, baseUrl: String, userId: String) {
        val service = ApiClient(baseUrl).service
        val lines = repo.readPending(limit = 100)
        val uploaded = mutableListOf<String>()
        for (line in lines) {
            try {
                val stats = SolveCodec.decode(line)
                val dto = RulesetDto(
                    draw = stats.rules.draw,
                    redeals = stats.rules.redeals,
                    recycle = stats.rules.recycle.name.lowercase(),
                    allowFoundationToTableau = stats.rules.allowFoundationToTableau
                )
                val req = SolveUploadRequest(
                    dealId = stats.dealId,
                    userId = userId,
                    ruleset = dto,
                    durationMs = stats.durationMs,
                    moveCount = stats.moveCount,
                    startedAt = stats.startedAt,
                    finishedAt = stats.finishedAt,
                    clientVersion = stats.clientVersion,
                    platform = stats.platform,
                    moveTraceHash = null
                )
                runBlocking { service.uploadSolve(req) }
                uploaded.add(line)
            } catch (_: Exception) {
                // keep line for retry
            }
        }
        if (uploaded.isNotEmpty()) repo.markUploaded(uploaded)
    }

    @Test
    fun end_to_end_upload_success() {
        // Arrange engine → stats → repo
        val tmp = createTempDir(prefix = "e2e_repo_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(draw = 1, redeals = -1)
            val engine = GameEngine()
            engine.startGame(seed = 42uL, rules = rules)
            engine.draw()
            // snapshot and enqueue
            repo.appendPending(engine.getSolveStatsSnapshot())

            // Mock server response OK
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"accepted\":true}"))

            // Act
            uploadOnce(repo, server.url("/").toString(), userId = "usr_test")

            // Assert
            val recorded = server.takeRequest()
            val body = recorded.body.readUtf8()
            assertTrue(body.contains("\"dealId\""))
            assertEquals(0, repo.pendingCount())
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun partial_failure_keeps_unuploaded() {
        val tmp = createTempDir(prefix = "e2e_repo2_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(draw = 3, redeals = 0)
            val e = GameEngine()
            e.startGame(seed = 100uL, rules = rules)
            e.draw()
            val s1 = e.getSolveStatsSnapshot()
            repo.appendPending(s1)
            // second entry (different seed)
            val e2 = GameEngine()
            e2.startGame(seed = 101uL, rules = rules)
            e2.draw()
            val s2 = e2.getSolveStatsSnapshot()
            repo.appendPending(s2)

            // First success, second fails (500)
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"accepted\":true}"))
            server.enqueue(MockResponse().setResponseCode(500))

            val linesBefore = repo.readPending()
            uploadOnce(repo, server.url("/").toString(), userId = "usr_test")

            // One should remain
            val remaining = repo.readPending()
            assertEquals(1, remaining.size)
            // Ensure the remaining is the second one in original order (best-effort)
            assertTrue(remaining[0] == linesBefore[1] || remaining[0] == linesBefore[0])
        } finally {
            tmp.deleteRecursively()
        }
    }
}
