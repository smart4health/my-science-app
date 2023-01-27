package com.healthmetrix.myscience.feature.login.controller

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bluelinelabs.conductor.ControllerChangeType
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.conductor.doOnNextChangeEnd
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.myscience.feature.webpage.Redirectable
import com.healthmetrix.s4h.myscience.BuildConfig
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginConfigureConsentBinding
import kotlinx.coroutines.launch

/**
 * Step 3
 *
 * Launch the dynamic consent controller
 */
class ConfigureConsentController : ViewLifecycleController(), Redirectable {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginConfigureConsentBinding.inflate(inflater, container, false).apply {
            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.BACK)
                }
            }

            configureConsentContinue.setOnClickListener {
                entryPoint
                    .configureConsentUseCase
                    .generateUrl()
                    .let(this@ConfigureConsentController::showConfigureConsentWebPageTransaction)
                    .let(router::pushController)
            }

            if (BuildConfig.BUILD_TYPE == "debug") {
                configureConsentContinue.setOnLongClickListener {
                    requireViewLifecycleOwner.lifecycleScope.launch {
                        Toast.makeText(container.context, "Cheating!", Toast.LENGTH_SHORT).show()
                        entryPoint.configureConsentUseCase.cheat()
                    }
                    true
                }
            }
        }.root
    }

    override fun onRedirect(uri: Uri) =
        if (entryPoint.configureConsentUseCase.shouldOpenExternally(uri)) {
            Redirectable.Action.OPEN_EXTERNAL
        } else {
            doOnNextChangeEnd { _, _, controllerChangeType ->
                if (controllerChangeType == ControllerChangeType.POP_ENTER) {
                    requireViewLifecycleOwner.lifecycleScope.launch {
                        entryPoint.configureConsentUseCase.handleResponse(uri)
                    }
                }
            }

            Redirectable.Action.POP
        }
}
