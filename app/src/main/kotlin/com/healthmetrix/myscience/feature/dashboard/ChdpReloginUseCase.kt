package com.healthmetrix.myscience.feature.dashboard

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.sync.SyncEvent
import com.healthmetrix.myscience.feature.sync.SyncSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ChdpReloginUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val syncEventMutableSharedFlow: MutableSharedFlow<SyncEvent>,
    private val syncSettingsDataStore: DataStore<SyncSettings>,
    private val chdpClient: ChdpClient,
) {
    /**
     * Called after a successful non-first login, to compare the id x user secret pair,
     * which is only stored locally
     */
    suspend operator fun invoke() {
        val oldId = loginSettingsDataStore.data.first().chdpInfo.userId

        if (oldId != chdpClient.getClientId()) {
            chdpClient.logout()
            syncEventMutableSharedFlow.emit(SyncEvent.MismatchedAccountError)
        } else {
            syncSettingsDataStore.updateData { syncSettings ->
                syncSettings.toBuilder()
                    .setFlagAuthFailed(false)
                    .build()
            }
        }
    }
}
