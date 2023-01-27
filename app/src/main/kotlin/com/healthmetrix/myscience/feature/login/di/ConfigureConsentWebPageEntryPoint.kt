package com.healthmetrix.myscience.feature.login.di

import com.healthmetrix.myscience.feature.login.ConfigureConsentProgress
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.SendChannel

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfigureConsentWebPageEntryPoint {
    val configureConsentProgressSender: SendChannel<ConfigureConsentProgress>
}
