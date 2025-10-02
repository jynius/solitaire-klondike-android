package us.jyni.game.klondike.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import us.jyni.game.klondike.util.stats.SolveCodec

class UploadWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = JsonlFileRepository(applicationContext)
        val pending = repo.readPending(limit = 50)
        if (pending.isEmpty()) return Result.success()
        val baseUrl = inputData.getString("baseUrl") ?: return Result.retry()
        val userId = inputData.getString("userId") ?: return Result.retry()
        val client = ApiClient(baseUrl).service
        val uploaded = mutableListOf<String>()
        for (line in pending) {
            try {
                val stats = SolveCodec.decode(line)
                val rulesDto = RulesetDto(
                    draw = stats.rules.draw,
                    redeals = stats.rules.redeals,
                    recycle = stats.rules.recycle.name.lowercase(),
                    allowFoundationToTableau = stats.rules.allowFoundationToTableau
                )
                val req = SolveUploadRequest(
                    dealId = stats.dealId,
                    userId = userId,
                    ruleset = rulesDto,
                    durationMs = stats.durationMs,
                    moveCount = stats.moveCount,
                    startedAt = stats.startedAt,
                    finishedAt = stats.finishedAt,
                    clientVersion = stats.clientVersion,
                    platform = stats.platform,
                    moveTraceHash = null
                )
                client.uploadSolve(req)
                uploaded.add(line)
            } catch (e: Exception) {
                // 실패 항목은 이후 재시도
            }
        }
        if (uploaded.isNotEmpty()) {
            repo.markUploaded(uploaded)
        }
        return Result.success()
    }
}
