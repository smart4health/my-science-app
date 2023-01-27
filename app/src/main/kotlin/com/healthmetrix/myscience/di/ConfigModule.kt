package com.healthmetrix.myscience.di

import com.healthmetrix.myscience.DataProvConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Provides
    @Singleton
    fun provideDataProvConfig(): DataProvConfig = DataProvConfig
}
