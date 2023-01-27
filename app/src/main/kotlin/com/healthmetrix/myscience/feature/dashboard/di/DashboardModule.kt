package com.healthmetrix.myscience.feature.dashboard.di

import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
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
object DashboardModule {

    @Provides
    @Singleton
    fun provideDashboardEventChannel(): Channel<DashboardEvent> =
        Channel(capacity = Channel.BUFFERED)

    @Provides
    @Singleton
    fun provideDashboardEventSender(dashboardEventChannel: Channel<DashboardEvent>): SendChannel<DashboardEvent> =
        dashboardEventChannel

    @Provides
    @Singleton
    fun provideDashboardEventReceiver(dashboardEventChannel: Channel<DashboardEvent>): ReceiveChannel<DashboardEvent> =
        dashboardEventChannel
}
