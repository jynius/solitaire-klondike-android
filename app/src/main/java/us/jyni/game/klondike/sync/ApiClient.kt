package us.jyni.game.klondike.sync

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class RulesetDto(
    val draw: Int,
    val redeals: Int,
    val recycle: String,
    @Json(name = "allowFoundationToTableau") val allowFoundationToTableau: Boolean? = null
)

data class SolveUploadRequest(
    val dealId: String,
    val userId: String,
    val ruleset: RulesetDto,
    val durationMs: Long,
    val moveCount: Int,
    val startedAt: Long,
    val finishedAt: Long?,
    val clientVersion: String?,
    val platform: String?,
    val moveTraceHash: String? = null
)

data class SolveUploadResponse(val accepted: Boolean, val rank: Int?, val personalBest: Boolean?)

data class DailyResponse(val dealId: String, val seed: String, val ruleset: RulesetDto, val validFrom: String?, val validTo: String?)

data class LeaderboardEntry(val userId: String, val durationMs: Long, val moveCount: Int)
data class LeaderboardResponse(val dealId: String, val top: List<LeaderboardEntry>)

interface ApiService {
    @POST("/v1/solves")
    suspend fun uploadSolve(@Body req: SolveUploadRequest): SolveUploadResponse

    @GET("/v1/daily")
    suspend fun getDaily(): DailyResponse

    @GET("/v1/leaderboards/{dealId}")
    suspend fun getLeaderboard(@Path("dealId") dealId: String): LeaderboardResponse
}

class ApiClient(baseUrl: String) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    val service: ApiService = retrofit.create(ApiService::class.java)
}
