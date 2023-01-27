package com.healthmetrix.myscience.feature.login.usecase

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.feature.login.ConfigureConsentProgress
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.LoginSettings
import kotlinx.coroutines.channels.SendChannel
import javax.inject.Inject

/**
 * This is sort of taking on the view model role strangely enough
 */
class ConfigureConsentUseCase @Inject constructor(
    private val config: DataProvConfig,
    private val parseConsentOptionsUseCase: ParseConsentOptionsUseCase,
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val continueLoginUseCase: ContinueLoginUseCase,
    private val configureConsentProgressSendChannel: @JvmSuppressWildcards SendChannel<ConfigureConsentProgress>,
) {

    fun generateUrl(): String =
        "${config.consent.host}/consents/${config.consent.getId()}?successRedirectUrl=${config.consent.configureSuccessUri}&platform=android&cancelRedirectUrl=${config.consent.configureCancelUri}"

    fun shouldOpenExternally(uri: Uri) = !uri.isSuccessUri() && !uri.isCancelUri()

    suspend fun handleResponse(uri: Uri) {
        if (uri.isSuccessUri()) {
            val consentOptions = when (val options = parseConsentOptionsUseCase(uri)) {
                is Ok -> options.value
                is Err -> {
                    Log.e(
                        this::class.simpleName,
                        "Failed to parse consent options from '$uri': ${options.error}",
                    )
                    return
                }
            }

            loginSettingsDataStore.updateData { loginSettings ->
                loginSettings.toBuilder()
                    .clearConsentOptions()
                    .addAllConsentOptions(consentOptions)
                    .build()
            }

            continueLoginUseCase(Event.FORWARD)
        } else {
            configureConsentProgressSendChannel.send(ConfigureConsentProgress.NONE)
        }
    }

    private fun Uri.isSuccessUri(): Boolean =
        scheme == config.consent.configureSuccessUri.scheme &&
            host == config.consent.configureSuccessUri.host &&
            path == config.consent.configureSuccessUri.path

    private fun Uri.isCancelUri(): Boolean =
        scheme == config.consent.configureCancelUri.scheme &&
            host == config.consent.configureCancelUri.host &&
            path == config.consent.configureCancelUri.path

    suspend fun cheat() {
        """
            [
                { "optionId": 0, "consented": true },
                { "optionId": 1, "consented": false },
                { "optionId": 2, "consented": false },
                { "optionId": 3, "consented": false },
                { "optionId": 4, "consented": false },
                { "optionId": 5, "consented": true }
            ]
        """.trimIndent()
            .toByteArray()
            .let { Base64.encodeToString(it, Base64.DEFAULT) }
            .let { "${config.consent.configureSuccessUri}?options=$it" }
            .let(Uri::parse)
            .let { handleResponse(it) }
    }
}
