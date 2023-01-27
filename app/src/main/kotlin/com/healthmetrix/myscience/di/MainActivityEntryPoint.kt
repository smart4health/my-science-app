package com.healthmetrix.myscience.di

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.IntentEvent
import com.healthmetrix.myscience.feature.login.LoginSettings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainActivityEntryPoint {
    val loginStateChangedReceiver: ReceiveChannel<Boolean>

    val loginSettingsDataStore: DataStore<LoginSettings>

    val intentEventSender: SendChannel<IntentEvent>
}
