package com.piyush.livetranslate.data.translation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TranslationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: TranslationSyncCoordinator,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = coordinator.sync().fold(
        onSuccess = { Result.success() },
        onFailure = { if (runAttemptCount < 5) Result.retry() else Result.failure() },
    )
}
