package com.healthmetrix.myscience.feature.login.usecase

import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.onFailure
import com.google.firebase.auth.FirebaseAuth
import com.healthmetrix.myscience.chdp.ChdpClient
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.messages.MessagesSettings
import com.healthmetrix.myscience.feature.statistics.StatsSettings
import com.healthmetrix.myscience.feature.sync.SyncSettings
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import com.healthmetrix.s4h.myscience.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clear local data and return to start
 */
@Singleton
class LogOutUseCase @Inject constructor(
    private val loginStateChangeSender: @JvmSuppressWildcards SendChannel<Boolean>,
    private val loginSettingsDataStore: DataStore<LoginSettings>,
    private val dataSelectionSettingsDataStore: DataStore<DataSelectionSettings>,
    private val firebaseAuth: FirebaseAuth,
    private val chdpClient: ChdpClient,
    private val applicationScope: CoroutineScope,
    private val syncSettingsDataStore: DataStore<SyncSettings>,
    private val messagesSettingsDataStore: DataStore<MessagesSettings>,
    private val statsSettingsDataStore: DataStore<StatsSettings>,
    private val messagesDatabase: Database,
) {
    /**
     * How this works:
     *
     * We want to update all the state no matter what, but at time of writing mostly
     * view lifecycles are used.  That means, when logging out and moving the ui manipulation
     * (the event sending) to be the first operation, the view lifecycle will cancel work,
     * and with no animations, nothing gets run at all.  There are many solutions to this,
     * such as using activity lifecycle, or application lifecycle to run this method, but
     * I'd rather enforce the atomic-ness here.  To that end, I inject a coroutine scope
     * created by the application and never cancelled, launch work on that scope, and join
     * in the normal parent scope.  This way invoke() still looks like a normal suspend function,
     * that can be cancelled (the join, which is just waiting, is cancellable), but the work
     * inside the applicationScope is not cancelled
     *
     * Other solutions:
     * - change all usages to use a longer-lived scope
     *   Works, but not enforceable, and is a silent error. Although I could turn the errors
     *   into runtime exceptions, it can be done better
     *
     * - withContext(NonCancellable) {}
     *   Not considered because it is really not flexible code inside runs and can easily
     *   get out of control, especially if there is a library bug or such
     *
     * - GlobalScope
     *   no
     */
    suspend operator fun invoke(): Unit = applicationScope.launch {
        loginStateChangeSender.send(false)

        loginSettingsDataStore.updateData {
            LoginSettings.getDefaultInstance()
        }

        dataSelectionSettingsDataStore.updateData {
            DataSelectionSettings.getDefaultInstance()
        }

        syncSettingsDataStore.updateData {
            SyncSettings.getDefaultInstance()
        }

        messagesSettingsDataStore.updateData {
            MessagesSettings.getDefaultInstance()
        }

        statsSettingsDataStore.updateData {
            StatsSettings.getDefaultInstance()
        }

        withContext(Dispatchers.IO) {
            messagesDatabase.messagesQueries.clear()
        }

        firebaseAuth.signOut()

        chdpClient.logout().onFailure { ex ->
            Log.e(this::class.simpleName, "Failed to log out", ex)
        }
    }.join()
}
