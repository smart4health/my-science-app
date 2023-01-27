package com.healthmetrix.myscience.feature.statistics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.healthmetrix.myscience.commons.di.DataStoreWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StatisticsModule {

    @Provides
    @Singleton
    fun provideStatsDataStore(
        @ApplicationContext
        applicationContext: Context,
    ): DataStore<StatsSettings> {
        val dataStore = dataStore(
            fileName = "stats.pb",
            serializer = StatsSettingsSerializer,
        )

        return DataStoreWrapper(applicationContext, dataStore)
    }
}
