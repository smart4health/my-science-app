package com.healthmetrix.myscience

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.asTransaction
import com.healthmetrix.myscience.conductor.SlidePushChangeHandler
import com.healthmetrix.myscience.di.MainActivityEntryPoint
import com.healthmetrix.myscience.feature.dashboard.controller.DashboardController
import com.healthmetrix.myscience.feature.login.State
import com.healthmetrix.myscience.feature.login.controller.LoginController
import com.healthmetrix.myscience.feature.login.toDomain
import com.healthmetrix.s4h.myscience.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    private lateinit var router: Router

    private val entryPoint by entryPoint<MainActivityEntryPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        processIntent(intent)

        binding = ActivityMainBinding.inflate(layoutInflater).also { binding ->
            setContentView(binding.root)

            router = Conductor.attachRouter(this, binding.root, savedInstanceState)
                .setPopRootControllerMode(Router.PopRootControllerMode.NEVER)

            // set initial controller based on whether user is logged in or not
            if (router.backstack.isEmpty()) {
                lifecycleScope.launch {
                    entryPoint
                        .loginSettingsDataStore
                        .data
                        .first()
                        .loginState
                        .toDomain()
                        .let { if (it == State.DASHBOARD) DashboardController() else LoginController() }
                        .asTransaction()
                        .let(router::setRoot)
                }
            }
        }

        // switched between login and dashboard controllers when log in state changes
        lifecycleScope.launch {
            entryPoint.loginStateChangedReceiver.receiveAsFlow().collect { isLoggedIn ->
                (if (isLoggedIn) DashboardController() else LoginController())
                    .asTransaction(pushChangeHandler = SlidePushChangeHandler(isForward = isLoggedIn))
                    .let(router::replaceTopController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    private fun processIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "my-science" && intent.data?.host == "dashboard") {
            lifecycleScope.launch {
                when (intent.data?.path) {
                    "/messages" -> IntentEvent.MESSAGES
                    "/sync" -> IntentEvent.SYNC
                    else -> null
                }?.let { entryPoint.intentEventSender.send(it) }
            }
        }
    }
}

enum class IntentEvent {
    MESSAGES,
    SYNC,
}
