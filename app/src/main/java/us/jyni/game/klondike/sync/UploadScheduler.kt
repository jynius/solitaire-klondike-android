package us.jyni.game.klondike.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UploadScheduler {
    private const val UNIQUE_WORK = "upload_solves_periodic"

    private fun inputData(context: Context): Data = Data.Builder()
        .putString("baseUrl", SyncSettings.getBaseUrl(context))
        .putString("userId", SyncSettings.getUserId(context))
        .build()

    private fun constraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic(context: Context, repeatHours: Long = 12) {
        if (!SyncSettings.isUploadEnabled(context)) return
        val req = PeriodicWorkRequestBuilder<UploadWorker>(repeatHours, TimeUnit.HOURS)
            .setConstraints(constraints())
            .setInputData(inputData(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun triggerOnce(context: Context) {
        if (!SyncSettings.isUploadEnabled(context)) return
        val req = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints())
            .setInputData(inputData(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}
