package com.healthmetrix.myscience.feature.dashboard.controller

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.healthmetrix.myscience.commons.ui.MarginItemDecoration
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
import com.healthmetrix.myscience.feature.dashboard.MessagesHeaderAdapter
import com.healthmetrix.myscience.feature.dashboard.MessagesMessageAdapter
import com.healthmetrix.myscience.feature.dashboard.MessagesOptionsAdapter
import com.healthmetrix.myscience.feature.dashboard.di.DashboardEntryPoint
import com.healthmetrix.myscience.feature.messages.MessagesLoadingState
import com.healthmetrix.myscience.feature.messages.NEW_MESSAGES_NOTIFICATION_ID
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardMessagesBinding
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesController : ViewLifecycleController() {

    private val entryPoint by entryPoint<DashboardEntryPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardMessagesBinding.inflate(inflater, container, false).apply {
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
                            .let(this@MessagesController::startActivity)
                        true
                    }
                    else -> false
                }
            }

            swipeRefresh.setOnRefreshListener {
                entryPoint.applicationScope.launch {
                    entryPoint.fetchMessagesUseCase()
                }
            }

            messagesRecyclerView.apply {
                layoutManager = LinearLayoutManager(root.context)
                adapter = ConcatAdapter(
                    MessagesOptionsAdapter { isChecked ->
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            entryPoint.messagesSettingsDataStore.updateData { messagesSettings ->
                                messagesSettings.toBuilder()
                                    .setIsEnabled(isChecked)
                                    .build()
                            }
                        }
                    },
                    MessagesHeaderAdapter(),
                    MessagesMessageAdapter { id ->
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            entryPoint.dashboardEventSender.send(DashboardEvent.ViewMessage(id))
                        }
                    },
                )

                root.context
                    .resources
                    .displayMetrics
                    .let { TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, it) }
                    .toInt()
                    .let(::MarginItemDecoration)
                    .let(this::addItemDecoration)
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.messagesSettingsDataStore
                    .data
                    .combine(entryPoint.messageLoadingStateReadonlyStateFlow, ::Pair)
                    .collect { (settings, loadingState) ->

                        swipeRefresh.isEnabled =
                            settings.isEnabled
                        swipeRefresh.isRefreshing =
                            settings.isEnabled && (loadingState == MessagesLoadingState.IN_PROGRESS)

                        messagesRecyclerView
                            .findAdapter<MessagesOptionsAdapter>()
                            ?.setIsEnabled(settings.isEnabled)
                    }
            }

            // observe messages and submit to messages adapter
            requireViewLifecycleOwner.lifecycleScope.launch {
                val allMessagesFlow = entryPoint.messagesDatabase
                    .messagesQueries
                    .getAllOrderByCreatedAt()
                    .asFlow()

                val enabledFlow = entryPoint.messagesSettingsDataStore
                    .data
                    .map { it.isEnabled }

                allMessagesFlow
                    .combine(enabledFlow) { query, isEnabled ->
                        if (!isEnabled) {
                            emptyList()
                        } else withContext(Dispatchers.IO) {
                            query.executeAsList()
                        }
                    }
                    .collect { messages ->
                        messagesRecyclerView
                            .findAdapter<MessagesMessageAdapter>()
                            ?.submitList(messages)
                    }
            }

            // observe messages and submit state to header
            requireViewLifecycleOwner.lifecycleScope.launch {
                val countAllFlow = entryPoint.messagesDatabase
                    .messagesQueries
                    .countAll()
                    .asFlow()

                countAllFlow.combine(entryPoint.messagesSettingsDataStore.data) { query, data ->

                    val count = withContext(Dispatchers.IO) {
                        query.executeAsOne()
                    }

                    when {
                        !data.isEnabled -> MessagesHeaderAdapter.State.Disabled
                        count == 0L -> MessagesHeaderAdapter.State.Empty(data.lastFetchedAt)
                        else -> MessagesHeaderAdapter.State.Enabled(data.lastFetchedAt)
                    }
                }.collect { state ->
                    messagesRecyclerView
                        .findAdapter<MessagesHeaderAdapter>()
                        ?.setState(state)
                }
            }
        }.root
    }

    override fun onAttach(view: View) {
        NotificationManagerCompat.from(view.context)
            .cancel(NEW_MESSAGES_NOTIFICATION_ID)
    }
}

private inline fun <reified T> RecyclerView.findAdapter(): T? {
    return (adapter as? ConcatAdapter)
        ?.adapters
        ?.filterIsInstance(T::class.java)
        ?.firstOrNull()
}
