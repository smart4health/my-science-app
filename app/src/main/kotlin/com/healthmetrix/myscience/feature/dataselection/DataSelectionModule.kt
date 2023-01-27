package com.healthmetrix.myscience.feature.dataselection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.healthmetrix.myscience.commons.di.DataStoreWrapper
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSelectionModule {
    @Provides
    @Singleton
    fun provideDataSelectionDataStore(
        @ApplicationContext
        applicationContext: Context,
    ): DataStore<DataSelectionSettings> {
        val dataStore = dataStore(
            fileName = "data_selection.pb",
            serializer = DataSelectionSettingsSerializer,
        )

        return DataStoreWrapper(applicationContext, dataStore)
    }
}
