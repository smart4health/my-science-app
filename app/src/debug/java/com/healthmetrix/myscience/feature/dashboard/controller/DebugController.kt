package com.healthmetrix.myscience.feature.dashboard.controller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.protobuf.ByteString
import com.healthmetrix.myscience.commons.base64
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.di.DebugEntryPoint
import com.healthmetrix.myscience.feature.messages.MessagesWorker
import com.healthmetrix.myscience.feature.messages.NEW_MESSAGES_NOTIFICATION_ID
import com.healthmetrix.myscience.feature.sync.SYNC_ERROR_NOTIFICATION_ID
import com.healthmetrix.myscience.feature.sync.SyncWorker
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardDebugBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DebugController : ViewLifecycleController() {

    private val entryPoint by entryPoint<DebugEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardDebugBinding.inflate(inflater, container, false).apply {
            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.loginSettingsDataStore.data.collect { loginSettings ->
                    debugUserSecretBody.text = loginSettings
                        .chdpInfo
                        .userSecret
                        .toByteArray()
                        .base64()
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.getPseudonymUseCase().collect { pseudonym ->
                    debugPseudonymBody.text = pseudonym ?: "None!"
                }
            }

            debugDeleteUserSecretButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.loginSettingsDataStore.updateData { loginSettings ->
                        val newSecret = ByteArray(32).apply {
                            entryPoint.secureRandom.nextBytes(this)
                        }.let(ByteString::copyFrom)

                        val chdpInfo = loginSettings.chdpInfo.toBuilder()
                            .setUserSecret(newSecret)
                            .build()

                        loginSettings.toBuilder()
                            .setChdpInfo(chdpInfo)
                            .build()
                    }
                }
            }

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_dashboard_logout -> {
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            entryPoint.logOutUseCase()
                        }
                        true
                    }
                    R.id.menu_item_dashboard_oss -> {
                        Intent(container.context, OssLicensesMenuActivity::class.java)
                            .let(this@DebugController::startActivity)
                        true
                    }
                    else -> false
                }
            }

            debugExampleMessagesNotificationButton.setOnClickListener {
                val notification =
                    MessagesWorker.buildNotification(root.context, (1 until 5).random())

                NotificationManagerCompat.from(root.context)
                    .notify(NEW_MESSAGES_NOTIFICATION_ID, notification)
            }

            debugExampleSyncNotificationButton.setOnClickListener {
                val notification = SyncWorker.buildNotification(root.context)

                NotificationManagerCompat.from(root.context)
                    .notify(SYNC_ERROR_NOTIFICATION_ID, notification)
            }

            debugCrash.setOnClickListener {
                error("This is a test crash")
            }
        }.root
    }
}
