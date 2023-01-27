package com.healthmetrix.myscience.feature.login.usecase

import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.onFailure
import com.google.protobuf.ByteString
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.LoginSettings
import java.security.SecureRandom
import javax.inject.Inject

class ChdpFirstLoginUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val secureRandom: SecureRandom,
    private val chdpClient: ChdpClient,
) {
    /*
     * Call after successful *first* login to store the id x secret pair
     */
    suspend operator fun invoke() {
        val chdpId = chdpClient.getClientId()
        val userSecret = ByteArray(32).apply {
            secureRandom.nextBytes(this)
        }.let(ByteString::copyFrom)

        loginSettingsDataStore.updateData { loginSettings ->
            val chdpInfo = loginSettings.chdpInfo.toBuilder()
                .setUserId(chdpId)
                .setUserSecret(userSecret)
                .build()

            loginSettings.toBuilder()
                .setChdpInfo(chdpInfo)
                .build()
        }
    }

    suspend fun undo() {
        chdpClient.logout().onFailure { e ->
            Log.e(this::class.simpleName, "Failed to logout", e)
        }
        loginSettingsDataStore.updateData { loginSettings ->
            loginSettings.toBuilder()
                .clearChdpInfo()
                .build()
        }
    }
}
