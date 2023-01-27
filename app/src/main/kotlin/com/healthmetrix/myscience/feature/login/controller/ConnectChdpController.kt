package com.healthmetrix.myscience.feature.login.controller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorInt
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.onFailure
import com.google.android.material.snackbar.Snackbar
import com.healthmetrix.myscience.chdp.LoginContract
import com.healthmetrix.myscience.commons.ui.fade
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.myscience.feature.login.usecase.Error
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginConnectChdpBinding
import kotlinx.coroutines.launch

/**
 * Step 6
 *
 * Launch the CHDP sign in activity
 */
class ConnectChdpController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private var contractCallback: ((Boolean) -> Unit)? = null

    override fun onContextAvailable(context: Context) {
        loginLauncher = (activity as ComponentActivity).activityResultRegistry
            .register(
                "d4l_login_launcher",
                this@ConnectChdpController,
                LoginContract(context.primaryColor),
            ) { isSuccessful ->
                contractCallback?.invoke(isSuccessful)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginConnectChdpBinding.inflate(inflater, container, false).apply {
            continueButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    continueButton.isEnabled = false
                    progressHorizontal.fade(out = false)

                    entryPoint.chdpClient
                        .loginIntent(container.context)
                        .let(loginLauncher::launch)
                }
            }

            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.BACK)
                }
            }

            contractCallback = { isSuccessful ->
                requireViewLifecycleOwner.lifecycleScope.launch {
                    if (isSuccessful) {
                        binding<Unit, Error> {
                            entryPoint.chdpFirstLoginUseCase()
                            entryPoint.submitConsentUseCase(revoke = false).bind()
                            entryPoint.continueLoginUseCase(Event.FORWARD)
                        }.onFailure {
                            entryPoint.chdpFirstLoginUseCase.undo()

                            Snackbar.make(
                                root,
                                R.string.snackbar_login_connect_chdp_upload_consent_failed,
                                Snackbar.LENGTH_SHORT,
                            ).setAnchorView(continueButton).show()
                        }
                    }

                    progressHorizontal.fade(out = true)
                    continueButton.isEnabled = true
                }
            }
        }.root
    }

    override fun onDestroyView(view: View) {
        contractCallback = null
    }

    @get:ColorInt
    private val Context.primaryColor: Int
        get() = TypedValue().let { typedValue ->
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            typedValue.data
        }
}
