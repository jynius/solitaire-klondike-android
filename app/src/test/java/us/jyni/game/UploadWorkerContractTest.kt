package us.jyni.game

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import us.jyni.game.klondike.sync.ApiClient
import us.jyni.game.klondike.sync.RulesetDto
import us.jyni.game.klondike.sync.SolveUploadRequest

class UploadWorkerContractTest {
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

    @Test
    fun uploadSolve_contract_shape() {
        // Prepare mock response
        server.enqueue(MockResponse().setBody("{" +
            "\"accepted\":true,\"rank\":1,\"personalBest\":true}"))

        val baseUrl = server.url("/").toString()
        val client = ApiClient(baseUrl).service
        val req = SolveUploadRequest(
            dealId = "DL1_abc",
            userId = "usr_1",
            ruleset = RulesetDto(draw = 1, redeals = -1, recycle = "reverse", allowFoundationToTableau = false),
            durationMs = 1234,
            moveCount = 10,
            startedAt = 1000,
            finishedAt = 2000,
            clientVersion = "1.0.0",
            platform = "android",
            moveTraceHash = null
        )

        // Execute
    val resp = runBlocking { client.uploadSolve(req) }
        assertEquals(true, resp.accepted)

        // Inspect recorded request body
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()

        // Validate minimal schema keys exist
        listOf("dealId","userId","ruleset","durationMs","moveCount","startedAt").forEach {
            assert(body.contains("\"$it\""))
        }
    }
}
