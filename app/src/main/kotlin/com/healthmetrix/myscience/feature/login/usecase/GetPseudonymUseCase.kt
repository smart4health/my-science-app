package com.healthmetrix.myscience.feature.login.usecase

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.LoginSettings
import kotlinx.coroutines.flow.map
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class GetPseudonymUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val chdpClient: ChdpClient,
) {

    operator fun invoke() = loginSettingsDataStore.data.map { loginSettings ->
        if (loginSettings.chdpInfo.userSecret.isEmpty) {
            null
        } else {
            chdpClient.getClientId().hmacSha256(loginSettings.chdpInfo.userSecret.toByteArray())
        }
    }
}

// seems like Mac::getInstance returns a new object each time, meaning
// this method is thread safe
private fun String.hmacSha256(secret: ByteArray): String = with(Mac.getInstance("HmacSHA256")) {
    init(SecretKeySpec(secret, algorithm))
    val cipherText = doFinal(toByteArray())
    reset()
    cipherText.joinToString("") { b -> "%02X".format(b) }
}
