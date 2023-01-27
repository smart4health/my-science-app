package com.healthmetrix.myscience.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.healthmetrix.myscience.feature.messages.MessagesWorker
import com.healthmetrix.myscience.feature.sync.SyncWorker
import javax.inject.Inject

class DaggerWorkerFactory @Inject constructor(
    private val syncWorkerFactory: SyncWorker.Factory,
    private val messagesWorkerFactory: MessagesWorker.Factory,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        SyncWorker::class.qualifiedName -> syncWorkerFactory(appContext, workerParameters)
        MessagesWorker::class.qualifiedName -> messagesWorkerFactory(appContext, workerParameters)
        else -> null
    }
}
