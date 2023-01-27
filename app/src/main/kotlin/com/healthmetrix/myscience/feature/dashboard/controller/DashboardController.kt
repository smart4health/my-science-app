package com.healthmetrix.myscience.feature.dashboard.controller

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.asTransaction
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.google.android.material.snackbar.Snackbar
import com.healthmetrix.myscience.IntentEvent
import com.healthmetrix.myscience.conductor.MaterialFadeThroughChangeHandler
import com.healthmetrix.myscience.conductor.MaterialSharedZAxisChangeHandler
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
import com.healthmetrix.myscience.feature.dashboard.di.DashboardEntryPoint
import com.healthmetrix.myscience.feature.messages.FetchMessagesUseCase
import com.healthmetrix.myscience.feature.sync.SyncEvent
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardBinding
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.michaelbull.result.runCatching as catch

class DashboardController : ViewLifecycleController() {

//    private val component by components(DaggerDashboardComponent.factory()::create)

    private val entryPoint by entryPoint<DashboardEntryPoint>()

    private var childRouter: Router? = null

    @SuppressLint("ShowToast")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardBinding.inflate(inflater, container, false).apply {
            // just to make sure the router is set up
            requireViewLifecycleOwner.lifecycleScope.launchWhenResumed {
                entryPoint.intentEventReceiver.receiveAsFlow().collect {
                    bottomNavigationView.selectedItemId = when (it) {
                        IntentEvent.MESSAGES -> R.id.action_messages
                        IntentEvent.SYNC -> R.id.action_my_data
                    }
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.dashboardEventReceiver.receiveAsFlow().collect {
                    when (it) {
                        DashboardEvent.AuthFailure ->
                            ReconnectChdpController()
                                .asTransaction()
                                .let(router::pushController)

                        is DashboardEvent.ViewMessage ->
                            MessageController
                                .withMessageId(it.messageId)
                                .asTransaction()
                                // would make a nice container transition in the future
                                .pushChangeHandler(MaterialSharedZAxisChangeHandler())
                                .popChangeHandler(MaterialSharedZAxisChangeHandler())
                                .let(router::pushController)

                        DashboardEvent.GetStatisticsFailure -> Snackbar.make(
                            root,
                            R.string.snackbar_dashboard_status_statistics_failed,
                            Snackbar.LENGTH_LONG,
                        ).setAnchorView(bottomNavigationView).show()

                        DashboardEvent.RevokeConsentFailure -> Snackbar.make(
                            root,
                            R.string.snackbar_dashboard_my_data_revoke_consent_failed,
                            Snackbar.LENGTH_SHORT,
                        ).setAnchorView(bottomNavigationView).show()
                    }
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.syncSettingsDataStore.data.collect { syncSettings ->
                    if (syncSettings.flagAuthFailed) {
                        bottomNavigationView.getOrCreateBadge(R.id.action_my_data)
                    } else {
                        bottomNavigationView.removeBadge(R.id.action_my_data)
                    }
                }
            }

            childRouter = getChildRouter(childContainer)
                .setPopRootControllerMode(Router.PopRootControllerMode.NEVER)
                .also { r ->
                    if (r.backstack.isEmpty()) {
                        StatusController()
                            .asTransaction()
                            .let(r::setRoot)
                    }
                }

            requireViewLifecycleOwner.lifecycleScope.launch {
                val unreadFlow = entryPoint.messagesDatabase
                    .messagesQueries
                    .countUnread()
                    .asFlow()

                val enabledFlow = entryPoint.messagesSettingsDataStore
                    .data
                    .map { it.isEnabled }

                unreadFlow.combine(enabledFlow) { query, isEnabled ->
                    if (!isEnabled) {
                        0
                    } else withContext(Dispatchers.IO) {
                        query.executeAsOne().toInt()
                    }
                }.collect { count ->
                    if (count == 0) {
                        bottomNavigationView.removeBadge(R.id.action_messages)
                    } else {
                        bottomNavigationView.getOrCreateBadge(R.id.action_messages)
                            .number = count
                    }
                }
            }

            // receive message loading error events
            // when messages are already in the channel, it will immediately try and fail
            // to launch a snackbar, so wait till resume hits
            requireViewLifecycleOwner.lifecycleScope.launchWhenResumed {
                entryPoint.messageEventReceiver
                    .receiveAsFlow()
                    .collect { event ->
                        when (event) {
                            FetchMessagesUseCase.Event.Failed -> {
                                Snackbar.make(
                                    root,
                                    container.context.getString(R.string.snackbar_dashboard_message_fetch_failed),
                                    Snackbar.LENGTH_SHORT,
                                ).setAnchorView(bottomNavigationView)
                                    .show()
                            }
                        }
                    }
            }

            requireViewLifecycleOwner.lifecycleScope.launchWhenResumed {
                entryPoint.syncEventSharedFlow.collect { event ->
                    if (event == SyncEvent.MismatchedAccountError) {
                        Snackbar.make(
                            root,
                            container.context.getString(R.string.snackbar_dashboard_account_mismatch),
                            Snackbar.LENGTH_LONG,
                        ).setAnchorView(bottomNavigationView).show()
                    }
                }
            }

            bottomNavigationView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.action_status -> {
                        childRouter?.setRoot(
                            StatusController()
                                .asTransaction()
                                .pushChangeHandler(MaterialFadeThroughChangeHandler()),
                        ) ?: Log.e(this::class.simpleName, "nav event with null child router")

                        true
                    }
                    R.id.action_my_data -> {
                        childRouter?.setRoot(
                            MyDataController()
                                .asTransaction()
                                .pushChangeHandler(MaterialFadeThroughChangeHandler()),
                        ) ?: Log.e(this::class.simpleName, "nav event with null child router")

                        true
                    }
                    R.id.action_messages -> {
                        childRouter?.setRoot(
                            MessagesController()
                                .asTransaction()
                                .pushChangeHandler(MaterialFadeThroughChangeHandler()),
                        ) ?: Log.e(this::class.simpleName, "nav event with null child router")

                        true
                    }
                    R.id.action_debug -> {
                        val controller = findDebugController()

                        if (controller == null) {
                            Toast.makeText(
                                container.context,
                                "DebugController not found",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            childRouter?.setRoot(
                                controller
                                    .asTransaction()
                                    .pushChangeHandler(MaterialFadeThroughChangeHandler()),
                            ) ?: Log.e(this::class.simpleName, "nav event with null child router")
                        }

                        true
                    }
                    else -> false
                }
            }

            bottomNavigationView.setOnItemReselectedListener { /* nothing */ }
        }.root
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)

        childRouter = null
    }

    override fun handleBack(): Boolean {
        return childRouter?.handleBack() ?: false
    }
}

private fun findDebugController(): Controller? {
    val constructors = catch {
        Class.forName("com.healthmetrix.myscience.feature.dashboard.controller.DebugController").constructors
    }.getOrElse { arrayOf() }

    var controller: Controller? = null
    for (c in constructors) {
        if (controller == null) {
            controller = c.catch {
                newInstance() as Controller
            }.get()
        }
    }

    return controller
}
