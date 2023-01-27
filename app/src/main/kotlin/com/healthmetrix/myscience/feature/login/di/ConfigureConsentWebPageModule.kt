package com.healthmetrix.myscience.feature.login.di

import com.healthmetrix.myscience.feature.login.ConfigureConsentProgress
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConfigureConsentWebPageModule {
    @Provides
    @Singleton
    fun provideConfigureConsentProgressChannel(): Channel<ConfigureConsentProgress> =
        Channel(capacity = Channel.BUFFERED)

    @Provides
    fun provideConfigureConsentProgressSender(
        loginEventChannel: Channel<ConfigureConsentProgress>,
    ): SendChannel<ConfigureConsentProgress> = loginEventChannel

    @Provides
    fun provideConfigureConsentProgressReceiver(
        loginEventChannel: Channel<ConfigureConsentProgress>,
    ): ReceiveChannel<ConfigureConsentProgress> = loginEventChannel
}
