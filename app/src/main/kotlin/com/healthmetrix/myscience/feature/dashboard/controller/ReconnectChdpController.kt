package com.healthmetrix.myscience.feature.dashboard.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.healthmetrix.myscience.conductor.ViewLifecycleController

class ReconnectChdpController : ViewLifecycleController() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return TextView(container.context).also {
            it.text = "reconnect chdp"
        }
    }
}
