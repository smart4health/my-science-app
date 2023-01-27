package com.healthmetrix.myscience.commons.di

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlin.properties.ReadOnlyProperty

/**
 * Wrap the delegate to allow Dagger to inject DataStores
 */
class DataStoreWrapper<T> constructor(
    private val applicationContext: Context,
    dataStoreDelegate: ReadOnlyProperty<Context, DataStore<T>>,
) : DataStore<T> {
    private val Context.dataStore by dataStoreDelegate

    override val data: Flow<T>
        get() = applicationContext.dataStore.data

    override suspend fun updateData(transform: suspend (t: T) -> T): T =
        applicationContext.dataStore.updateData(transform)
}
