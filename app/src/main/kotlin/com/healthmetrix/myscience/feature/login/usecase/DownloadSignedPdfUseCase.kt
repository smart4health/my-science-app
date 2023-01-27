package com.healthmetrix.myscience.feature.login.usecase

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.service.consent.ConsentService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadSignedPdfUseCase @Inject constructor(
    @ApplicationContext
    private val applicationContext: Context,
    private val consentService: ConsentService,
    private val loginSettingsDataStore: DataStore<LoginSettings>,
) {
    /**
     * Dispatchers note: copyTo requires the IO dispatcher to be set
     */
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        binding<Pair<ParcelFileDescriptor, Uri>, Error> {
            val loginSettings = loginSettingsDataStore.data.first()

            if (loginSettings.optionDocumentIdCase == LoginSettings.OptionDocumentIdCase.OPTIONDOCUMENTID_NOT_SET) {
                Error.NoDocumentId.let(::Err).bind<Unit>()
            }

            if (loginSettings.optionInfoCase == LoginSettings.OptionInfoCase.OPTIONINFO_NOT_SET) {
                Error.NoBearerToken.let(::Err).bind<Unit>()
            }

            val pdfFile = File(applicationContext.filesDir, "pdfs/${loginSettings.documentId}.pdf")

            pdfFile.parentFile?.mkdirs()

            if (!pdfFile.exists()) {
                consentService
                    .runCatching {
                        fetchPdf(
                            loginSettings.firebaseInfo.bearerToken,
                            loginSettings.documentId,
                        )
                    }
                    .mapError(Error::RetrofitError)
                    .bind()
                    .body()
                    .toResultOr { Error.NoBody }
                    .bind()
                    .byteStream()
                    .use { inputStream ->
                        pdfFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)

            val contentUri = FileProvider.getUriForFile(
                applicationContext,
                applicationContext.packageName,
                pdfFile,
            )

            parcelFileDescriptor to contentUri
        }
    }.onFailure { e ->
        Log.e(this::class.simpleName, "Failed to download signed pdf: $e")
    }

    sealed class Error {
        object NoDocumentId : Error()

        object NoBearerToken : Error()

        data class RetrofitError(val t: Throwable) : Error()

        object NoBody : Error()
    }
}
