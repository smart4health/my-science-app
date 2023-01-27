package com.healthmetrix.myscience.feature.statistics

import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.healthmetrix.myscience.commons.base64
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.service.deident.DeidentService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val statsSettingsDataStore: DataStore<StatsSettings>,
    private val deidentService: DeidentService,
) {

    suspend operator fun invoke(): Result<Unit, Error> = binding<Unit, Error> {
        val (userId, userSecret) = loginSettingsDataStore.data.first()
            .toUserCredentials()

        if (userSecret.isEmpty()) {
            Error.NoUserSecret.let(::Err).bind<Unit>()
        }

        val response = deidentService
            .runCatching {
                getStatistics(userId, userSecret.base64())
            }
            .mapError(Error::DeidentStatistics)
            .bind()

        statsSettingsDataStore.updateData { statsSettings ->
            statsSettings
                .toBuilder()
                .setLastFetchedAt(System.currentTimeMillis())
                .setUserResourcesUploaded(response.user.resourcesUploadedCount)
                .setGlobalResourcesUploaded(response.global.resourcesUploadedCount)
                .setGlobalUsers(response.global.usersCount)
                .build()
        }
    }.onFailure { error ->
        if (error is Error.HasThrowable) {
            Log.e(
                this::class.simpleName,
                "Failed to fetch stats: ${error::class.simpleName}",
                error.inner,
            )
        } else {
            Log.e(
                this::class.simpleName,
                "Failed to fetch stats: $error",
            )
        }
    }

    sealed class Error {
        object NoUserSecret : Error()
        data class DeidentStatistics(override val inner: Throwable) : Error(), HasThrowable
        interface HasThrowable {
            val inner: Throwable
        }
    }
}

internal fun LoginSettings.toUserCredentials(): Pair<String, ByteArray> =
    firebaseInfo.userId to chdpInfo.userSecret.toByteArray()
