package com.nexauren.audiotools.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    private const val UNIQUE_WORK = "audio_tools_network_updates"

    fun schedule(context: Context, delaySeconds: Long = 30L) {
        val enabled =
            context.getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            ).getBoolean(
                "auto_update_check",
                true
            )

        if (!enabled) {
            cancel(context)
            return
        }

        val request = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(
        context: Context
    ) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(UNIQUE_WORK)
    }
}
