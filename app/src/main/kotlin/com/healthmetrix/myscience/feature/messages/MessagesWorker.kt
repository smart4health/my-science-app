package com.healthmetrix.myscience.feature.messages

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
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.myscience.MESSAGES_NOTIFICATION_CHANNEL_ID
import com.healthmetrix.myscience.MainActivity
import com.healthmetrix.s4h.myscience.R
import kotlinx.coroutines.flow.first
import javax.inject.Inject

const val NEW_MESSAGES_NOTIFICATION_ID = 2021_08_16_1
const val NEW_MESSAGES_REQUEST_CODE = 2021_08_16_2

class MessagesWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val fetchMessagesUseCase: FetchMessagesUseCase,
    private val messagesSettingsDataStore: DataStore<MessagesSettings>,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!messagesSettingsDataStore.data.first().isEnabled) {
            return Result.success()
        }

        return fetchMessagesUseCase().onSuccess { numberOfNewMessages ->
            if (numberOfNewMessages > 0) {
                val notification = buildNotification(context, numberOfNewMessages)

                NotificationManagerCompat.from(context)
                    .notify(NEW_MESSAGES_NOTIFICATION_ID, notification)
            }
        }.mapBoth(success = { Result.success() }, failure = { Result.failure() })
    }

    companion object {
        fun buildNotification(context: Context, numberOfMessages: Int): Notification {
            val contentTitle = context.resources.getQuantityString(
                R.plurals.numberOfNewMessages,
                numberOfMessages,
                numberOfMessages,
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("my-science://dashboard/messages")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NEW_MESSAGES_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

            return NotificationCompat.Builder(context, MESSAGES_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_outline_email_24)
                .setContentTitle(contentTitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL)
                .build()
        }
    }

    class Factory @Inject constructor(
        private val fetchMessagesUseCase: FetchMessagesUseCase,
        private val messagesSettingsDataStore: DataStore<MessagesSettings>,
    ) {
        operator fun invoke(appContext: Context, workerParams: WorkerParameters) =
            MessagesWorker(
                appContext,
                workerParams,
                fetchMessagesUseCase,
                messagesSettingsDataStore,
            )
    }
}
