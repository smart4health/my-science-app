package com.healthmetrix.myscience.feature.login.di

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.ConfigureConsentProgress
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.login.NavEvent
import com.healthmetrix.myscience.feature.login.usecase.ChdpFirstLoginUseCase
import com.healthmetrix.myscience.feature.login.usecase.ConfigureConsentUseCase
import com.healthmetrix.myscience.feature.login.usecase.ContinueLoginUseCase
import com.healthmetrix.myscience.feature.login.usecase.DownloadSignedPdfUseCase
import com.healthmetrix.myscience.feature.login.usecase.LogOutUseCase
import com.healthmetrix.myscience.feature.login.usecase.ParseConsentOptionsUseCase
import com.healthmetrix.myscience.feature.login.usecase.SignConsentUseCase
import com.healthmetrix.myscience.feature.login.usecase.SubmitConsentUseCase
import com.healthmetrix.myscience.feature.sync.SyncSettings
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoginEntryPoint {
    val loginSettingsDataStore: DataStore<LoginSettings>

    val dataSelectionSettingsDataStore: DataStore<DataSelectionSettings>

    val logOutUseCase: LogOutUseCase

    val configureConsentUseCase: ConfigureConsentUseCase

    val signConsentUseCase: SignConsentUseCase

    val parseConsentOptionsUseCase: ParseConsentOptionsUseCase

    val downloadSignedPdfUseCase: DownloadSignedPdfUseCase

    val config: DataProvConfig

    val continueLoginUseCase: ContinueLoginUseCase

    val chdpClient: ChdpClient

    val syncSettingsDataStore: DataStore<SyncSettings>

    val chdpFirstLoginUseCase: ChdpFirstLoginUseCase

    val submitConsentUseCase: SubmitConsentUseCase

    val loginReceiver: ReceiveChannel<NavEvent>

    val loginStateChangedSender: SendChannel<Boolean>

    val configureConsentProgressReceiver: ReceiveChannel<ConfigureConsentProgress>
}
