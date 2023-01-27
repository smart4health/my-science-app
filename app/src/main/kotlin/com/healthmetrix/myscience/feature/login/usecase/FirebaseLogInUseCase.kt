package com.healthmetrix.myscience.feature.login.usecase

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.google.firebase.auth.FirebaseAuth
import com.healthmetrix.myscience.feature.login.LoginSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Log in with firebase and save the credentials
 */
class FirebaseLogInUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {

    @Suppress("ThrowableNotThrown")
    suspend operator fun invoke(): Result<LoginSettings.FirebaseInfo, Throwable> = binding {
        val user = firebaseAuth.signInAnonymously()
            .runCatching { await() }
            .bind()
            .user
            .toResultOr { IllegalStateException("Failed to get user") }
            .bind()

        val token = user.getIdToken(true)
            .runCatching { await() }
            .bind()
            .token
            .toResultOr { IllegalStateException("Failed to get token") }
            .bind()

        val info = LoginSettings.FirebaseInfo.newBuilder()
            .setUserId(user.uid)
            .setBearerToken(token)
            .build()

        info
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
