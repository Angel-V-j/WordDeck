package com.worddeck.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.worddeck.WordDeckApplication
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Schedules one persisted synchronization chain; there is no periodic background service. */
internal object SyncWorkScheduler {
    const val WORK_NAME = "worddeck-sync"

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            // Room contains the complete pending state, so only the newest request is needed.
            ExistingWorkPolicy.REPLACE,
            createRequest(),
        )
    }

    internal fun createRequest(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        )
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            10,
            TimeUnit.SECONDS,
        )
        .build()
}

class SyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? WordDeckApplication ?: return Result.failure()
        val container = application.appContainer

        val user = when (val session = container.authenticationRepository.observeCurrentUser().first()) {
            is AppResult.Success -> session.value ?: return Result.success()
            is AppResult.Failure -> return Result.failure()
        }

        return when (val result = container.syncCoordinator.sync(user.id)) {
            is AppResult.Success -> Result.success()
            is AppResult.Failure -> {
                if (result.error == AppError.NetworkUnavailable && runAttemptCount < 2) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
}
