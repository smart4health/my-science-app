package com.healthmetrix.myscience.feature.login.usecase

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.commons.urlEncode
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.service.consent.ConsentService
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject

class SignConsentUseCase @Inject constructor(
    private val config: DataProvConfig,
    private val firebaseLogInUseCase: FirebaseLogInUseCase,
    private val downloadUnsignedPdfUseCase: DownloadUnsignedPdfUseCase,
    private val consentService: ConsentService,
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val continueLoginUseCase: ContinueLoginUseCase,
) {
    /**
     * - login with firebase to get an id and token (persist)
     * - load the consent options
     * - fetch pdf to file
     * - submitPdf to consent -> document id (persist) and token
     * -> return url with document id x token
     *
     * overall function analysis:
     * Input:
     * - consent options
     * Output:
     * - firebase id x firebase token
     * - document id x signing token (not persisted)
     * - url with doc id x signing token
     *
     * So, this step is not done if
     * - no firebase info and document id and not success from web page
     */
    suspend fun initiateSigning(): Result<String, Error> = binding<String, Error> {
        val consentOptions = loginSettingsDataStore.data
            .first()
            .consentOptionsList

        if (consentOptions.isEmpty()) {
            Error.NoConsentOptions.let(::Err).bind<Unit>()
        }

        val firebaseInfo = firebaseLogInUseCase()
            .mapError(Error::FirebaseError)
            .bind()

        val pdfFile = downloadUnsignedPdfUseCase(firebaseInfo.userId, consentOptions)
            .mapError(Error::PdfDownloadFailure)
            .bind()

        val submitPdfResponse = consentService.runCatching {
            submitPdf(
                bearerToken = firebaseInfo.bearerToken,
                successRedirectUrl = config.consent.signingSuccessUri.toString(),
                consentId = config.consent.getId(),
                pdf = pdfFile.asRequestBody("application/pdf".toMediaType()),
            )
        }.mapError(Error::PdfSubmissionFailure).bind()

        loginSettingsDataStore.updateData { loginSettings ->
            loginSettings.toBuilder()
                .setDocumentId(submitPdfResponse.documentId)
                .setFirebaseInfo(firebaseInfo)
                .build()
        }

        "${config.consent.host}/signatures/${submitPdfResponse.documentId}/sign?token=${submitPdfResponse.token.urlEncode()}"
    }.onFailure { e ->
        Log.e(this::class.simpleName, "Failed to launch signing: $e")
    }

    suspend fun handleResponse(uri: Uri) {
        if (uri.scheme == config.consent.signingSuccessUri.scheme &&
            uri.host == config.consent.signingSuccessUri.host &&
            uri.path == config.consent.signingSuccessUri.path
        ) {
            continueLoginUseCase(Event.FORWARD)
        }
    }

    /**
     * For use when navigating back
     */
    suspend fun unsign() {
        firebaseLogInUseCase.signOut()
        loginSettingsDataStore.updateData { loginSettings ->
            loginSettings.toBuilder()
                .clearDocumentId()
                .clearFirebaseInfo()
                .build()
        }
    }

    sealed class Error {
        object NoConsentOptions : Error()

        data class FirebaseError(val t: Throwable) : Error()

        data class PdfDownloadFailure(val e: DownloadUnsignedPdfUseCase.Error) : Error()

        data class PdfSubmissionFailure(val t: Throwable) : Error()
    }
}
