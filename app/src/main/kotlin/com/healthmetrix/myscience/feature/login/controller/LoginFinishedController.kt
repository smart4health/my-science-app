package com.healthmetrix.myscience.feature.login.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginFinishedBinding
import kotlinx.coroutines.launch

/**
 * Step 8
 *
 * Display an "all done" message before going to dashboard
 */
class LoginFinishedController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginFinishedBinding.inflate(inflater, container, false).apply {
            continueButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.syncSettingsDataStore.updateData { syncSettings ->
                        syncSettings.toBuilder()
                            .setIsBackgroundSharingEnabled(true)
                            .build()
                    }

                    entryPoint.continueLoginUseCase(Event.FORWARD)
                }
            }

            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.BACK)
                }
            }
        }.root
    }
}
