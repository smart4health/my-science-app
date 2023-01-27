package com.healthmetrix.myscience.feature.login.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dataselection.toDataSelectionSettings
import com.healthmetrix.myscience.feature.dataselection.toEntries
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginDataSelectionBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DataSelectionController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginDataSelectionBinding.inflate(inflater, container, false).apply {
            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    backButton.isEnabled = false // logging out takes a moment...

                    // use case candidate?
                    entryPoint.dataSelectionSettingsDataStore.updateData {
                        DataSelectionSettings.getDefaultInstance()
                    }
                    entryPoint.chdpFirstLoginUseCase.undo()
                    entryPoint.continueLoginUseCase(Event.BACK)

                    backButton.isEnabled = true
                }
            }

            continueButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.FORWARD)
                }
            }

            dataSelectionView.onEntriesChangedListener = { entries ->
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.dataSelectionSettingsDataStore.updateData {
                        entries.toDataSelectionSettings()
                    }
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.dataSelectionSettingsDataStore.data.collect { dataSelectionSettings ->
                    dataSelectionSettings
                        .toEntries(container.context.resources)
                        .let(dataSelectionView::setEntries)
                }
            }
        }.root
    }
}
