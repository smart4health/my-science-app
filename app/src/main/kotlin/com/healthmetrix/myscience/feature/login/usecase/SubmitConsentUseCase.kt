package com.healthmetrix.myscience.feature.login.usecase

import android.util.Log
import androidx.datastore.core.DataStore
import care.data4life.sdk.lang.D4LException
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.sync.usecase.ConvertFhirUseCase
import kotlinx.coroutines.flow.first
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import javax.inject.Inject

const val THIRD_PARTY_ACCESS_CONSENT_OPTION_ID = 1

class SubmitConsentUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val generateConsentUseCase: GenerateConsentUseCase,
    private val chdpClient: ChdpClient,
    private val convertFhirUseCase: ConvertFhirUseCase,
) {
    suspend operator fun invoke(revoke: Boolean): Result<Unit, Error> = binding<Unit, Error> {
        val loginSettings = loginSettingsDataStore.data.first()
        val thirdPartyAccessAllowed =
            loginSettings.consentOptionsList
                .firstOrNull { it.optionId == THIRD_PARTY_ACCESS_CONSENT_OPTION_ID }
                ?.consented
                ?: false

        val d4LConsent = convertFhirUseCase
            .fromHapi(
                generateConsentUseCase(
                    patientReference = Reference().apply {
                        identifier = Identifier().apply {
                            value = chdpClient.getClientId()
                            system = "http://fhir.data4life.care/CodeSystem/user-id"
                        }
                    },
                    thirdPartyAccessAllowed = thirdPartyAccessAllowed,
                    timeOfConsent = DateTimeType.now(),
                    revoke = revoke,
                ),
            )
            .mapError(Error::HapiConversion)
            .bind()
            .toResultOr { Error.HapiConversionNull }
            .bind()

        chdpClient
            .createRecord(d4LConsent)
            .onFailure { t ->
                Log.e(
                    this@SubmitConsentUseCase::class.simpleName,
                    "Failed to upload consent[${d4LConsent.id}]: $t",
                )
            }
            .mapError(Error::ChdpUpload)
            .bind()
    }.onFailure { error ->
        if (error is Error.HasThrowable) {
            Log.e(
                this@SubmitConsentUseCase::class.simpleName,
                "Failed to submit consent: ${error::class.simpleName}",
                error.inner,
            )
        } else {
            Log.e(
                this@SubmitConsentUseCase::class.simpleName,
                "Failed to submit consent: $error",
            )
        }
    }
}

sealed class Error {
    data class ChdpUpload(override val inner: D4LException) : Error(), HasThrowable
    data class HapiConversion(override val inner: Throwable) : Error(), HasThrowable
    object HapiConversionNull : Error()
    interface HasThrowable {
        val inner: Throwable
    }
}
