package com.healthmetrix.myscience.feature.login.usecase

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.service.consent.ConsentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class DownloadUnsignedPdfUseCase @Inject constructor(
    private val config: DataProvConfig,
    @Named("pdf_cache_dir")
    private val cacheDir: File,
    private val consentService: ConsentService,
) {
    /**
     * Dispatcher: due to streaming response body, the retrofit call comes back to the main thread
     *             before copying the stream, thus the NetworkOnMainThreadException
     */
    suspend operator fun invoke(
        filename: String,
        options: List<LoginSettings.ConsentOption>,
    ): Result<File, Error> = withContext(Dispatchers.IO) {
        binding {
            val outputFile = File(cacheDir, filename)

            consentService
                .runCatching { configurePdf(config.consent.getId(), options.toDto()) }
                .mapError(Error::Retrofit)
                .bind()
                .body()
                .toResultOr { Error.NoBody }
                .bind()
                .byteStream()
                .use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

            outputFile
        }
    }

    private fun List<LoginSettings.ConsentOption>.toDto(): ConsentService.ConsentOptions {
        return map { option ->
            ConsentService.ConsentOption(
                optionId = option.optionId,
                consented = option.consented,
            )
        }.let(ConsentService::ConsentOptions)
    }

    sealed class Error {
        data class Retrofit(val t: Throwable) : Error()

        object NoBody : Error()
    }
}
