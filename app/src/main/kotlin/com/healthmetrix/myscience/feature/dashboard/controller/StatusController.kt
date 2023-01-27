package com.healthmetrix.myscience.feature.dashboard.controller

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.Err
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.healthmetrix.myscience.commons.ui.fade
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
import com.healthmetrix.myscience.feature.dashboard.di.DashboardEntryPoint
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardStatusBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StatusController : ViewLifecycleController() {

    private val entryPoint by entryPoint<DashboardEntryPoint>()

    @SuppressLint("ShowToast")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardStatusBinding.inflate(inflater, container, false).apply {
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_dashboard_logout -> {
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            entryPoint.logOutUseCase()
                        }
                        true
                    }
                    R.id.menu_item_dashboard_oss -> {
                        Intent(container.context, OssLicensesMenuActivity::class.java)
                            .let(this@StatusController::startActivity)
                        true
                    }
                    else -> false
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                progressHorizontal.fade(out = false)

                if (entryPoint.getStatisticsUseCase() is Err) {
                    entryPoint.dashboardEventSender.send(DashboardEvent.GetStatisticsFailure)
                }

                progressHorizontal.fade(out = true)
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.statsSettingsDataStore.data.collect { statsSettings ->
                    statusCounter1.animateCounter(statsSettings.userResourcesUploaded)
                    statusCounter2.animateCounter(statsSettings.globalResourcesUploaded, 100)
                    statusCounter3.animateCounter(statsSettings.globalUsers, 200)
                }
            }
        }.root
    }

    private fun TextView.animateCounter(target: Int, startDelay: Long = 0) {
        val current = try {
            (text ?: "").toString().toInt()
        } catch (ex: NumberFormatException) {
            0
        }

        if (current == target) {
            return
        }

        ValueAnimator.ofInt(current, target).apply {
            this.startDelay = startDelay
            duration = 1500
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                text = (it.animatedValue as Int).toString()
            }
        }.start()
    }
}
