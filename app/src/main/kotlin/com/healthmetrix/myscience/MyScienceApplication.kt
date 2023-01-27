package com.healthmetrix.myscience

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.healthmetrix.myscience.di.DaggerWorkerFactory
import com.healthmetrix.myscience.feature.messages.MessagesWorker
import com.healthmetrix.myscience.feature.sync.SyncWorker
import com.healthmetrix.s4h.myscience.BuildConfig
import com.healthmetrix.s4h.myscience.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

const val MESSAGES_NOTIFICATION_CHANNEL_ID = "messages"
const val SYNC_NOTIFICATION_CHANNEL_ID = "sync"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationEntryPoint {
    val daggerWorkerFactory: DaggerWorkerFactory
}

@HiltAndroidApp
class MyScienceApplication : Application(), Configuration.Provider {

    private val entryPoint by entryPoint<ApplicationEntryPoint>()

    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        addSyncWorker()
        addMessagesWorker()
        createNotificationChannels()
    }

    private fun addSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // wifi
            .setRequiresCharging(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("chdp_sync")
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun addMessagesWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // wifi
            .setRequiresCharging(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MessagesWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("fetch_messages")
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("messages", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                MESSAGES_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name_messages),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_description_messages)
            }

            val syncChannel = NotificationChannel(
                SYNC_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name_sync),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description_sync)
            }

            getSystemService<NotificationManager>()
                ?.createNotificationChannels(listOf(messageChannel, syncChannel))
        }
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(entryPoint.daggerWorkerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
