package com.healthmetrix.myscience.feature.dashboard.controller

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.di.DashboardEntryPoint
import com.healthmetrix.myscience.feature.messages.MessageState
import com.healthmetrix.myscience.feature.messages.TimeAgo
import com.healthmetrix.myscience.feature.messages.toTimeAgo
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardMessageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ARG_MESSAGE_ID = "MESSAGE_ID"

class MessageController(
    bundle: Bundle,
) : ViewLifecycleController(bundle) {

    private val entryPoint by entryPoint<DashboardEntryPoint>()

    private val messageId = args.getString(ARG_MESSAGE_ID)!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardMessageBinding.inflate(inflater, container, false).apply {
            toolbar.setNavigationOnClickListener {
                router.popCurrentController()
            }

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_dashboard_message_mark_unread -> {
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                entryPoint
                                    .messagesDatabase
                                    .messagesQueries
                                    .setState(MessageState.UNREAD, messageId)
                            }

                            router.popCurrentController()
                        }
                        true
                    }
                    else -> false
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                val message = withContext(Dispatchers.IO) {
                    entryPoint.messagesDatabase
                        .messagesQueries
                        .getById(messageId)
                        .executeAsOneOrNull()
                } ?: run {
                    Toast.makeText(
                        root.context,
                        root.context.getString(R.string.toast_message_failed_read),
                        Toast.LENGTH_SHORT,
                    ).show()
                    router.popCurrentController()
                    return@launch
                }

                messageTitle.text = message.title
                messageBody.text = message.text
                messageDate.text = message.created_at.toTimeAgo().toString(container.context)

                withContext(Dispatchers.IO) {
                    if (message.state.isUnread()) {
                        entryPoint
                            .messagesDatabase
                            .messagesQueries
                            .setState(MessageState.READ, messageId)
                    }
                }
            }
        }.root
    }

    companion object {
        fun withMessageId(messageId: String) = Bundle().apply {
            putString(ARG_MESSAGE_ID, messageId)
        }.let(::MessageController)
    }
}

private fun TimeAgo.toString(context: Context) = with(context) {
    when (val t = this@toString) {
        TimeAgo.Never -> ""
        TimeAgo.Recent -> getString(R.string.item_dashboard_message_moments_ago)
        is TimeAgo.Hours -> resources.getQuantityString(R.plurals.numberOfHours, t.number, t.number)
        is TimeAgo.Days -> resources.getQuantityString(R.plurals.numberOfDays, t.number, t.number)
    }
}
