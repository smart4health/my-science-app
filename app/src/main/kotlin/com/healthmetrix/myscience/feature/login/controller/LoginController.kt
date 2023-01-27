package com.healthmetrix.myscience.feature.login.controller

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import androidx.lifecycle.lifecycleScope
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.asTransaction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.healthmetrix.myscience.conductor.SlidePushChangeHandler
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.LoginSettings
import com.healthmetrix.myscience.feature.login.SelfCollapsingBottomSheetBehavior
import com.healthmetrix.myscience.feature.login.State
import com.healthmetrix.myscience.feature.login.calculateProgress
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.myscience.feature.login.getProgress
import com.healthmetrix.myscience.feature.login.toDomain
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LoginController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    private var childRouter: Router? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginBinding.inflate(inflater, container, false).apply {
            childRouter = getChildRouter(childContainer)
                .setPopRootControllerMode(Router.PopRootControllerMode.NEVER)
                .also { r ->
                    if (r.backstack.isEmpty()) requireViewLifecycleOwner.lifecycleScope.launch {
                        entryPoint
                            .loginSettingsDataStore
                            .data
                            .first()
                            .loginState
                            .toDomain()
                            .toController()
                            ?.asTransaction()
                            ?.let(r::setRoot)
                    }
                }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.loginReceiver.receiveAsFlow().collect { navEvent ->
                    navEvent.newState
                        .toController()
                        ?.asTransaction(pushChangeHandler = SlidePushChangeHandler(isForward = navEvent.event == Event.FORWARD))
                        .let { routerTransaction ->
                            if (routerTransaction == null) {
                                entryPoint.loginStateChangedSender.send(true)
                            } else {
                                childRouter?.replaceTopController(routerTransaction)
                            }
                        }
                }
            }

            val milestoneHeaders = listOf(
                MilestoneHeader(milestone1Pointer, milestone1Header, milestone1ReachedIndicator),
                MilestoneHeader(milestone2Pointer, milestone2Header, milestone2ReachedIndicator),
                MilestoneHeader(milestone3Pointer, milestone3Header, milestone3ReachedIndicator),
                MilestoneHeader(milestone4Pointer, milestone4Header, milestone4ReachedIndicator),
            )

            val bottomSheetBehavior = SelfCollapsingBottomSheetBehavior.from(bottomSheet)

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint
                    .loginSettingsDataStore
                    .data
                    .map(LoginSettings::getLoginState)
                    .map(LoginSettings.LoginState::toDomain)
                    .collect { loginState ->
                        loginState.toViewMilestone().let { (milestone, hidden) ->
                            with(bottomSheetBehavior) {
                                if (hidden) {
                                    isHideable = true
                                    state = BottomSheetBehavior.STATE_HIDDEN
                                } else {
                                    isHideable = false
                                }

                                milestone?.let { (milestone, subMilestone) ->
                                    setProgress(milestone, subMilestone)

                                    milestoneHeaders.forEachIndexed { index, milestoneHeader ->
                                        when {
                                            milestone > index -> MilestoneHeader.State.REACHED
                                            milestone == index -> MilestoneHeader.State.CURRENT
                                            else -> MilestoneHeader.State.FUTURE
                                        }.let(milestoneHeader::setState)
                                    }
                                }
                            }
                        }
                    }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint
                    .configureConsentProgressReceiver
                    .receiveAsFlow()
                    .map { it.calculateProgress() }
                    .collect { subMilestone ->
                        setProgress(milestoneProgressView.currentMilestone, subMilestone)
                    }
            }
        }.root
    }

    override fun handleBack(): Boolean {
        return childRouter?.handleBack() ?: false
    }
}

private fun State.toController(): Controller? = when (this) {
    State.WELCOME -> WelcomeController()
    State.CONFIGURE -> ConfigureConsentController()
    State.SIGN -> SignConsentController()
    State.REVIEW -> ReviewConsentController()
    State.CONNECT_CHDP -> ConnectChdpController()
    State.SELECT_DATA -> DataSelectionController()
    State.FINISHED -> LoginFinishedController()
    State.DASHBOARD -> null
}

private fun State.toViewMilestone() = when (this) {
    State.WELCOME -> ViewMilestone(ViewMilestone.Milestone(0, 0f), true)
    State.CONFIGURE -> ViewMilestone(ViewMilestone.Milestone(1, 0f), false)
    State.SIGN -> ViewMilestone(ViewMilestone.Milestone(2, 0f), false)
    State.REVIEW -> ViewMilestone(ViewMilestone.Milestone(3, 0f), false)
    State.CONNECT_CHDP -> ViewMilestone(ViewMilestone.Milestone(4, 0f), false)
    State.SELECT_DATA -> ViewMilestone(null, true)
    State.FINISHED -> ViewMilestone(null, true)
    State.DASHBOARD -> ViewMilestone(null, true)
}

private data class ViewMilestone(
    val milestone: Milestone?,
    val hidden: Boolean,
) {
    data class Milestone(
        val milestone: Int,
        val subMilestone: Float,
    )
}

private fun ControllerLoginBinding.setProgress(milestone: Int, subMilestone: Float) {
    milestoneProgressView.animateTo(milestone, subMilestone)

    percentageTextView.text = milestoneProgressView
        .milestoneWeights
        .getProgress(milestone, subMilestone)
        .roundedPercent()
        .let { p ->
            root.context.getString(
                R.string.milestone_progress_percentage_text,
                p,
            )
        }
}

private fun Float.roundedPercent(): Int {
    val p = (this * 100).toInt()

    return 5 * (p / 5f).roundToInt()
}

private data class MilestoneHeader(
    val pointer: View,
    val header: TextView,
    val completedIndicator: View,
) {
    fun setState(state: State) {
        pointer.visibility = if (state == State.CURRENT) View.VISIBLE else View.INVISIBLE
        // technically is a fake bold...  https://saket.me/android-fake-vs-true-bold-and-italic/
        header.text = header.text.toSpannable().apply {
            getSpans<StyleSpan>(0, length).forEach(this::removeSpan)
            if (state == State.CURRENT) {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }

        completedIndicator.visibility = if (state == State.REACHED) View.VISIBLE else View.GONE
    }

    enum class State {
        REACHED,
        CURRENT,
        FUTURE,
    }
}
