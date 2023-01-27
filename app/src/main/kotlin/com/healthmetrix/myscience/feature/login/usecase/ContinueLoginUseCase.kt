package com.healthmetrix.myscience.feature.login.usecase

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.login.NavEvent
import com.healthmetrix.myscience.feature.login.submit
import com.healthmetrix.myscience.feature.login.toDataStore
import com.healthmetrix.myscience.feature.login.toDomain
import kotlinx.coroutines.channels.SendChannel
import javax.inject.Inject

class ContinueLoginUseCase @Inject constructor(
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val loginSender: @JvmSuppressWildcards SendChannel<NavEvent>,
) {

    suspend operator fun invoke(loginEvent: Event) {
        val loginSettings = loginSettingsDataStore.updateData { loginSettings ->
            val newState = loginSettings.loginState
                .toDomain()
                .submit(loginEvent)
                .toDataStore()

            loginSettings.toBuilder()
                .setLoginState(newState)
                .build()
        }

        loginSettings
            .loginState
            .toDomain()
            .let { NavEvent(it, loginEvent) }
            .let { loginSender.send(it) }
    }
}
