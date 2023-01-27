package com.healthmetrix.myscience.feature.dashboard.di

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.login.usecase.GetPseudonymUseCase
import com.healthmetrix.myscience.feature.login.usecase.LogOutUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugEntryPoint {
    val logOutUseCase: LogOutUseCase

    val loginSettingsDataStore: DataStore<LoginSettings>

    val secureRandom: SecureRandom

    val getPseudonymUseCase: GetPseudonymUseCase
}
