package com.healthmetrix.myscience.feature.login.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.commons.di.DataStoreWrapper
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.login.LoginSettingsSerializer
import com.healthmetrix.myscience.feature.login.NavEvent
import com.healthmetrix.myscience.service.consent.ConsentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.File
import java.security.SecureRandom
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoginModule {
    @Provides
    @Singleton
    fun provideLoginEventChannel(): Channel<NavEvent> = Channel(capacity = Channel.BUFFERED)

    @Provides
    fun provideLoginSender(loginEventChannel: Channel<NavEvent>): SendChannel<NavEvent> =
        loginEventChannel

    @Provides
    fun provideLoginReceiver(loginEventChannel: Channel<NavEvent>): ReceiveChannel<NavEvent> =
        loginEventChannel

    @Provides
    @Singleton
    @Named("pdf_cache_dir")
    fun providePdfCacheDirectory(
        @ApplicationContext
        applicationContext: Context,
    ): File = applicationContext.cacheDir

    @Provides
    @Singleton
    fun provideConsentService(dataProvConfig: DataProvConfig): ConsentService =
        ConsentService(baseUrl = dataProvConfig.consent.baseUrl)

    @Provides
    @Singleton
    fun provideSecureRandom() = SecureRandom()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideLoginSettingsDataStore(
        @ApplicationContext
        applicationContext: Context,
    ): DataStore<LoginSettings> {
        val dataStore = dataStore(
            fileName = "login_settings.pb",
            serializer = LoginSettingsSerializer,
        )

        return DataStoreWrapper(applicationContext, dataStore)
    }

    @Provides
    @Singleton
    fun provideLoginStateChangedChannel(): Channel<Boolean> = Channel(capacity = Channel.BUFFERED)

    @Provides
    fun provideLoginStateChangedSender(loginStateChangedChannel: Channel<Boolean>): SendChannel<Boolean> =
        loginStateChangedChannel

    @Provides
    fun provideLoginStateChangedReceiver(loginStateChangedChannel: Channel<Boolean>): ReceiveChannel<Boolean> =
        loginStateChangedChannel
}
