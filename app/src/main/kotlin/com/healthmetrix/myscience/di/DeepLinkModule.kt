package com.healthmetrix.myscience.di

import com.healthmetrix.myscience.IntentEvent
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
object DeepLinkModule {
    @Provides
    @Singleton
    fun provideIntentEventChannel(): Channel<IntentEvent> = Channel(capacity = Channel.BUFFERED)

    @Provides
    fun provideIntentEventSender(
        intentEventChannel: Channel<IntentEvent>,
    ): SendChannel<IntentEvent> = intentEventChannel

    @Provides
    fun provideIntentEventReceiver(
        intentEventChannel: Channel<IntentEvent>,
    ): ReceiveChannel<IntentEvent> = intentEventChannel
}
