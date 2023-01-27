package com.healthmetrix.myscience.feature.dashboard.di

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.IntentEvent
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.dashboard.ChdpReloginUseCase
import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
import com.healthmetrix.myscience.feature.login.usecase.LogOutUseCase
import com.healthmetrix.myscience.feature.login.usecase.SubmitConsentUseCase
import com.healthmetrix.myscience.feature.messages.FetchMessagesUseCase
import com.healthmetrix.myscience.feature.messages.MessagesLoadingState
import com.healthmetrix.myscience.feature.messages.MessagesSettings
import com.healthmetrix.myscience.feature.statistics.GetStatisticsUseCase
import com.healthmetrix.myscience.feature.statistics.StatsSettings
import com.healthmetrix.myscience.feature.sync.SyncEvent
import com.healthmetrix.myscience.feature.sync.SyncLoadingState
import com.healthmetrix.myscience.feature.sync.SyncSettings
import com.healthmetrix.myscience.feature.sync.usecase.FullSyncUseCase
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import com.healthmetrix.s4h.myscience.Database
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DashboardEntryPoint {
    val dataSelectionSettingsDataStore: DataStore<DataSelectionSettings>

    val logOutUseCase: LogOutUseCase

    val dashboardEventSender: SendChannel<DashboardEvent>

    val dashboardEventReceiver: ReceiveChannel<DashboardEvent>

    val syncSettingsDataStore: DataStore<SyncSettings>

    val fullSyncUseCase: FullSyncUseCase

    val applicationScope: CoroutineScope

    val syncEventSharedFlow: SharedFlow<SyncEvent>

    val syncLoadingStateReadonlyStateFlow: StateFlow<SyncLoadingState>

    val fetchMessagesUseCase: FetchMessagesUseCase

    val messagesSettingsDataStore: DataStore<MessagesSettings>

    val messagesDatabase: Database

    val messageEventReceiver: ReceiveChannel<FetchMessagesUseCase.Event>

    val messageLoadingStateReadonlyStateFlow: StateFlow<MessagesLoadingState>

    val chdpClient: ChdpClient

    val chdpReloginUseCase: ChdpReloginUseCase

    val intentEventReceiver: ReceiveChannel<IntentEvent>

    val statsSettingsDataStore: DataStore<StatsSettings>

    val getStatisticsUseCase: GetStatisticsUseCase

    val submitConsentUseCase: SubmitConsentUseCase
}
