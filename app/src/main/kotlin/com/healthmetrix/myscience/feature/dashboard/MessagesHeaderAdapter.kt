package com.healthmetrix.myscience.feature.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.myscience.feature.messages.TimeAgo
import com.healthmetrix.myscience.feature.messages.toTimeAgo
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ItemMessagesHeaderBinding

class MessagesHeaderAdapter :
    RecyclerView.Adapter<ViewBindingViewHolder<ItemMessagesHeaderBinding>>() {

    private var state: State = State.Gone

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ItemMessagesHeaderBinding> =
        LayoutInflater.from(parent.context)
            .let { ItemMessagesHeaderBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ItemMessagesHeaderBinding>,
        position: Int,
    ) {
        with(holder.binding.root.context) {
            holder.binding.stateTextView.text = when (val s = state) {
                is State.Enabled ->
                    s.lastFetchedAtMillis
                        .toTimeAgo()
                        .toString(this)
                is State.Empty ->
                    s.lastFetchedAtMillis
                        .toTimeAgo()
                        .toString(this)
                        .let { getString(R.string.item_messages_state_empty, it) }
                State.Disabled -> getString(R.string.item_messages_state_disabled)
                State.Gone -> ""
            }
        }
    }

    override fun getItemCount(): Int {
        return if (state == State.Gone) 0 else 1
    }

    fun setState(newState: State) {
        if (state == State.Gone && newState != State.Gone) {
            state = newState
            notifyItemInserted(0)
        } else if (state != State.Gone && newState == State.Gone) {
            state = newState
            notifyItemRemoved(0)
        } else {
            state = newState
            notifyItemChanged(0)
        }
    }

    sealed class State {
        object Gone : State()
        data class Enabled(val lastFetchedAtMillis: Long) : State()
        data class Empty(val lastFetchedAtMillis: Long) : State()
        object Disabled : State()
    }
}

private fun TimeAgo.toString(context: Context) = with(context) {
    when (val t = this@toString) {
        TimeAgo.Never -> getString(R.string.item_dashboard_messages_never_fetched)
        TimeAgo.Recent -> getString(R.string.item_dashboard_messages_last_fetched_at_recently)
        is TimeAgo.Hours -> {
            val hoursAgo = resources.getQuantityString(
                R.plurals.numberOfHours,
                t.number,
                t.number,
            )

            getString(R.string.item_dashboard_messages_last_fetched_at, hoursAgo)
        }
        is TimeAgo.Days -> {
            val daysAgo = resources.getQuantityString(
                R.plurals.numberOfDays,
                t.number,
                t.number,
            )

            getString(R.string.item_dashboard_messages_last_fetched_at, daysAgo)
        }
    }
}
