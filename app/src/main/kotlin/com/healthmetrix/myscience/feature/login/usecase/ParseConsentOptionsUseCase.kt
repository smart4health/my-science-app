package com.healthmetrix.myscience.feature.login.usecase

import android.net.Uri
import android.util.Base64
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.myscience.feature.login.LoginSettings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.github.michaelbull.result.runCatching as catch

class ParseConsentOptionsUseCase @Inject constructor() {
    @OptIn(ExperimentalSerializationApi::class)
    operator fun invoke(uri: Uri): Result<List<LoginSettings.ConsentOption>, Error> = binding {
        uri.getQueryParameter("options")
            .toResultOr { Error.NoQueryParam }
            .bind()
            .catch { Base64.decode(this, Base64.DEFAULT) }
            .mapError(Error::InvalidBase64Encoding)
            .bind()
            .toString(Charsets.UTF_8)
            .catch { Json.decodeFromString<List<JsonConsentOptions>>(this) }
            .mapError(Error::InvalidJson)
            .bind()
            .map(JsonConsentOptions::toConsentOption)
    }

    @Serializable
    private class JsonConsentOptions(
        val optionId: Int,
        val consented: Boolean,
    ) {
        fun toConsentOption(): LoginSettings.ConsentOption =
            LoginSettings.ConsentOption.newBuilder()
                .setOptionId(optionId)
                .setConsented(consented)
                .build()
    }

    sealed class Error {
        object NoQueryParam : Error()

        data class InvalidBase64Encoding(val t: Throwable) : Error()

        data class InvalidJson(val t: Throwable) : Error()
    }
}
