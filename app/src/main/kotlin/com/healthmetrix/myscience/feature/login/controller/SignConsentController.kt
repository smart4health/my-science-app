package com.healthmetrix.myscience.feature.login.controller

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bluelinelabs.conductor.ControllerChangeType
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.healthmetrix.myscience.commons.ui.fade
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.conductor.doOnNextChangeEnd
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.myscience.feature.webpage.Redirectable
import com.healthmetrix.myscience.feature.webpage.showPageTransaction
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginSignConsentBinding
import kotlinx.coroutines.launch

/**
 * Step 4
 *
 * launch the dynamic consent web app to sign the consent
 */
class SignConsentController : ViewLifecycleController(), Redirectable {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginSignConsentBinding.inflate(inflater, container, false).apply {
            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.BACK)
                }
            }

            signConsentContinue.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    signConsentContinue.disable {
                        progressHorizontal.fade(out = false)

                        entryPoint.signConsentUseCase
                            .initiateSigning()
                            .onSuccess { url ->
                                showPageTransaction(url)
                                    .let(router::pushController)
                            }
                            .onFailure {
                                container
                                    .context
                                    .getString(R.string.snackbar_login_sign_consent_launch_error)
                                    .let { Snackbar.make(root, it, Snackbar.LENGTH_SHORT) }
                                    .setAnchorView(signConsentContinue)
                                    .show()
                            }

                        progressHorizontal.fade(out = true)
                    }
                }
            }
        }.root
    }

    override fun onRedirect(uri: Uri): Redirectable.Action {
        doOnNextChangeEnd { _, _, controllerChangeType ->
            if (controllerChangeType == ControllerChangeType.POP_ENTER) {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.signConsentUseCase.handleResponse(uri)
                }
            }
        }

        return Redirectable.Action.POP
    }

    private suspend fun View.disable(block: suspend () -> Unit) {
        val before = isEnabled
        isEnabled = false
        block()
        isEnabled = before
    }
}
