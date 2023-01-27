package com.healthmetrix.myscience.feature.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import ca.uhn.fhir.context.FhirContext
import care.data4life.fhir.r4.FhirR4Parser
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.commons.di.DataStoreWrapper
import com.healthmetrix.myscience.service.qomop.QomopService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Credentials
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncEventMutableSharedFlow(): MutableSharedFlow<SyncEvent> = MutableSharedFlow()

    @Provides
    @Singleton
    fun provideSyncEventSharedFlow(syncEventMutableSharedFlow: MutableSharedFlow<SyncEvent>): SharedFlow<SyncEvent> =
        syncEventMutableSharedFlow

    @Provides
    @Singleton
    fun provideSyncSettingsDataStore(
        @ApplicationContext
        applicationContext: Context,
    ): DataStore<SyncSettings> {
        val dataStore = dataStore(
            fileName = "sync_settings.pb",
            serializer = SyncSettingsSerializer,
        )

        return DataStoreWrapper(applicationContext, dataStore)
    }

    @Provides
    @Singleton
    fun provideSyncLoadingStateMutableStateFlow(): MutableStateFlow<SyncLoadingState> =
        MutableStateFlow(SyncLoadingState.READY)

    @Provides
    @Singleton
    fun provideSyncLoadingStateReadonlyStateFlow(
        syncLoadingStateMutableStateFlow: MutableStateFlow<SyncLoadingState>,
    ): StateFlow<SyncLoadingState> = syncLoadingStateMutableStateFlow.asStateFlow()

    @Provides
    @Singleton
    @Named("qomopOkHttpClient")
    fun provideQomopServiceOkHttpClient(
        dataProvConfig: DataProvConfig,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        Credentials.basic(
                            dataProvConfig.qomop.user,
                            dataProvConfig.qomop.pass,
                        ),
                    )
                    .build().let(chain::proceed)
            }
            .build()

    @Provides
    @Singleton
    fun provideQomopService(
        dataProvConfig: DataProvConfig,
        @Named("qomopOkHttpClient")
        okHttpClient: OkHttpClient,
    ): QomopService =
        QomopService(
            baseUrl = dataProvConfig.qomop.baseUrl,
            okHttpClient = okHttpClient,
        )

    @Provides
    @Singleton
    fun provideD4LParser() = FhirR4Parser()

    @Provides
    @Singleton
    fun provideHapiFhirContext(): FhirContext = FhirContext.forR4()
}
