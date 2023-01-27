package com.healthmetrix.myscience.feature.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onFailure
import com.healthmetrix.myscience.MainActivity
import com.healthmetrix.myscience.SYNC_NOTIFICATION_CHANNEL_ID
import com.healthmetrix.myscience.feature.sync.usecase.FullSyncUseCase
import com.healthmetrix.s4h.myscience.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val SYNC_ERROR_NOTIFICATION_ID = 2021_08_16_3
const val SYNC_ERROR_REQUEST_CODE = 2021_08_16_4

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val fullSyncUseCase: FullSyncUseCase,
    private val syncSettingsDataStore: DataStore<SyncSettings>,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!syncSettingsDataStore.data.first().isBackgroundSharingEnabled) {
            return@withContext Result.success()
        }

        fullSyncUseCase()
            .onFailure { e ->
                if (e is FullSyncUseCase.Error.Chdp) {
                    val notification = buildNotification(context)

                    NotificationManagerCompat.from(context)
                        .notify(SYNC_ERROR_NOTIFICATION_ID, notification)
                }
            }
            .mapBoth(
                success = { Result.success() },
                failure = { Result.failure() },
            )
    }

    companion object {
        fun buildNotification(context: Context): Notification {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("my-science://dashboard/sync")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                SYNC_ERROR_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

            return NotificationCompat.Builder(context, SYNC_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_outline_error_outline_24)
                .setContentTitle(context.getString(R.string.notification_sync_error_title))
                .setContentText(context.getString(R.string.notification_sync_error_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .build()
        }
    }

    class Factory @Inject constructor(
        private val fullSyncUseCase: FullSyncUseCase,
        private val syncSettingsDataStore: DataStore<SyncSettings>,
    ) {
        operator fun invoke(appContext: Context, workerParameters: WorkerParameters) =
            SyncWorker(appContext, workerParameters, fullSyncUseCase, syncSettingsDataStore)
    }
}
