package com.healthmetrix.myscience.feature.messages

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.commons.di.DataStoreWrapper
import com.healthmetrix.myscience.service.recontact.RecontactService
import com.healthmetrix.s4h.myscience.Database
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Credentials
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessagesModule {
    @Provides
    @Singleton
    fun provideMessageEventChannel(): Channel<FetchMessagesUseCase.Event> =
        Channel(capacity = Channel.BUFFERED)

    @Provides
    @Singleton
    fun provideMessageEventSender(channel: Channel<FetchMessagesUseCase.Event>): SendChannel<FetchMessagesUseCase.Event> =
        channel

    @Provides
    @Singleton
    fun provideMessageEventReceiver(channel: Channel<FetchMessagesUseCase.Event>): ReceiveChannel<FetchMessagesUseCase.Event> =
        channel

    @Provides
    @Singleton
    fun provideMessagesSettingsDataStore(
        @ApplicationContext
        applicationContext: Context,
    ): DataStore<MessagesSettings> {
        val dataStore = dataStore(
            fileName = "messages.pb",
            serializer = MessageSettingsSerializer,
        )

        return DataStoreWrapper(applicationContext, dataStore)
    }

    @Provides
    @Singleton
    fun provideMessageLoadingStateMutableStateFlow(): MutableStateFlow<MessagesLoadingState> =
        MutableStateFlow(MessagesLoadingState.READY)

    @Provides
    @Singleton
    fun provideMessageLoadingStateReadonlyStateFlow(
        messageLoadingStateMutableStateFlow: MutableStateFlow<MessagesLoadingState>,
    ): StateFlow<MessagesLoadingState> = messageLoadingStateMutableStateFlow.asStateFlow()

    @Provides
    @Singleton
    @Named("recontactOkHttpClient")
    fun provideRecontactServiceOkHttpClient(
        dataProvConfig: DataProvConfig,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        Credentials.basic(
                            dataProvConfig.recontact.user,
                            dataProvConfig.recontact.pass,
                        ),
                    )
                    .build().let(chain::proceed)
            }
            .build()

    @Provides
    @Singleton
    fun provideRecontactService(
        dataProvConfig: DataProvConfig,
        @Named("recontactOkHttpClient")
        okHttpClient: OkHttpClient,
    ): RecontactService =
        RecontactService(
            baseUrl = dataProvConfig.recontact.baseUrl,
            okHttpClient = okHttpClient,
        )

    @Provides
    @Singleton
    fun provideDriver(
        @ApplicationContext
        applicationContext: Context,
    ): SqlDriver = AndroidSqliteDriver(Database.Schema, applicationContext, "messages.db")

    @Provides
    @Singleton
    fun provideDatabase(driver: SqlDriver) = Database(
        driver,
        messagesAdapter = Messages.Adapter(stateAdapter = EnumColumnAdapter()),
    )
}
