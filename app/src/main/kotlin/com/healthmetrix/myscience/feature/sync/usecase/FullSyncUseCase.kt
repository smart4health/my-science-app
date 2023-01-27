package com.healthmetrix.myscience.feature.sync.usecase

import android.util.Log
import androidx.datastore.core.DataStore
import ca.uhn.fhir.context.FhirContext
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.commons.base64
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.statistics.StatsSettings
import com.healthmetrix.myscience.feature.statistics.toUserCredentials
import com.healthmetrix.myscience.feature.sync.SyncEvent
import com.healthmetrix.myscience.feature.sync.SyncLoadingState
import com.healthmetrix.myscience.feature.sync.SyncSettings
import com.healthmetrix.myscience.feature.sync.toBundle
import com.healthmetrix.myscience.service.deident.DeidentService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Resource
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.github.michaelbull.result.coroutines.binding.binding as coBinding

@Singleton
class FullSyncUseCase @Inject constructor(
    private val syncSettingsDataStore: DataStore<SyncSettings>,
    private val syncEventSharedFlow: MutableSharedFlow<SyncEvent>,
    private val syncLoadingStateMutableStateFlow: MutableStateFlow<SyncLoadingState>,
    private val chdpClient: ChdpClient,
    private val deidentService: DeidentService,
    private val fhirContext: FhirContext,
    private val convertFhirUseCase: ConvertFhirUseCase,
    private val writeToFileUseCase: WriteToFileUseCase,
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val dataSyncPermissionCheckUseCase: DataSyncPermissionCheckUseCase,
    private val statsSettingsDataStore: DataStore<StatsSettings>,
) {

    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        syncLoadingStateMutableStateFlow.value = SyncLoadingState.IN_PROGRESS

        coBinding<Unit, Error> {
            val (userId, userSecret) = loginSettingsDataStore.data.first().toUserCredentials()

            if (userSecret.isEmpty()) {
                Error.NoUserSecret.let(::Err).bind<Unit>()
            }

            val fetchedAt = ZonedDateTime.now()

            // having all resources in memory isn't great but for now we only send one bundle
            // so there isn't an alternative
            val allResources = chdpClient.downloadAllWithErr()
                .toList()
                .combine()
                .mapError(Error::Chdp)
                .bind()
                .flatten()
                .map(convertFhirUseCase::toHapi)
                .combine()
                .mapError(Error::HapiConversion)
                .bind()
                .onEach {
                    writeToFileUseCase(it.filename, it.serialize())
                }

            val bundle = allResources
                .filter {
                    dataSyncPermissionCheckUseCase(resource = it).onFailure { t ->
                        Log.e(
                            this@FullSyncUseCase::class.simpleName,
                            "Failed to check upload consent for [${it.idElement.idPart}]: $t",
                        )
                    }.get() == DataSyncPermission.ALLOWED
                }
                .toBundle()

            Log.i(
                this@FullSyncUseCase::class.simpleName,
                "Uploading ${bundle.entry.size} consented resources of total of ${allResources.size}.",
            )

            writeToFileUseCase("Bundle-${fetchedAt.toLocalDate()}.json", bundle.serialize())

            val d4lId = chdpClient
                .getClientId()
                .toByteArray(Charsets.UTF_8)
                .base64()

            deidentService
                .runCatching {
                    uploadBundle(
                        userId = userId,
                        d4lId = d4lId,
                        fetchedAt = fetchedAt,
                        userSecret = userSecret.base64(),
                        bundle = bundle,
                    )
                }
                .mapError(Error::DeidentUpload)
                .bind()
        }.onSuccess {
            syncLoadingStateMutableStateFlow.value = SyncLoadingState.READY
            // Resetting Stats.lastFetchedAt to hard reload those numbers for the StatusController
            statsSettingsDataStore.updateData { statsSettings ->
                statsSettings.toBuilder()
                    .setLastFetchedAt(0)
                    .build()
            }
        }.onFailure { error ->
            if (error is Error.HasThrowable) {
                Log.e(
                    this@FullSyncUseCase::class.simpleName,
                    "Failed to sync: ${error::class.simpleName}",
                    error.inner,
                )
            } else {
                Log.e(this@FullSyncUseCase::class.simpleName, "Failed to sync: $error")
            }

            syncLoadingStateMutableStateFlow.value = SyncLoadingState.FAILED

            if (error is Error.Chdp) {
                syncEventSharedFlow.emit(SyncEvent.AuthException)
                syncSettingsDataStore.updateData { syncSettings ->
                    syncSettings.toBuilder()
                        .setFlagAuthFailed(true)
                        .build()
                }
            }
        }
    }

    private fun Resource.serialize(): String = fhirContext
        .newJsonParser()
        .encodeResourceToString(this)

    private val DomainResource.filename: String
        get() = "$resourceType-${idElement.idPart}.json"

    sealed class Error {
        object NoUserSecret : Error()

        data class HapiConversion(override val inner: Throwable) : Error(), HasThrowable

        data class DeidentUpload(override val inner: Throwable) : Error(), HasThrowable

        data class Chdp(override val inner: Throwable) : Error(), HasThrowable

        interface HasThrowable {
            val inner: Throwable
        }
    }
}
