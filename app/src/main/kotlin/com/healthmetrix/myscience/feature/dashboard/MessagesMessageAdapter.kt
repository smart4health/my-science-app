package com.healthmetrix.myscience.feature.dashboard

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.myscience.feature.messages.Messages
import com.healthmetrix.myscience.feature.messages.TimeAgo
import com.healthmetrix.myscience.feature.messages.toTimeAgo
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ItemMessagesMessageBinding

class MessagesMessageAdapter(
    private val listener: (String) -> Unit,
) : ListAdapter<Messages, ViewBindingViewHolder<ItemMessagesMessageBinding>>(DIFFER) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ItemMessagesMessageBinding> =
        LayoutInflater.from(parent.context)
            .let { ItemMessagesMessageBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ItemMessagesMessageBinding>,
        position: Int,
    ) {
        val message = getItem(position)

        with(holder.binding) {
            val icon = if (message.state.isUnread()) {
                R.drawable.ic_baseline_email_24
            } else {
                R.drawable.ic_outline_drafts_24
            }

            ContextCompat.getDrawable(root.context, icon)
                .let(messageIcon::setImageDrawable)

            messageTitle.text = message.title.toSpannable().apply {
                if (message.state.isUnread()) {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            messageBodyPreview.text = message.text.toSpannable().apply {
                if (message.state.isUnread()) {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            messageDate.text =
                message.created_at.toTimeAgo()
                    .toString(root.context)
                    .toSpannable().apply {
                        if (message.state.isUnread()) {
                            setSpan(
                                StyleSpan(Typeface.BOLD),
                                0,
                                length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                        }
                    }

            root.setOnClickListener {
                listener(message.id)
            }
        }
    }

    object DIFFER : DiffUtil.ItemCallback<Messages>() {
        override fun areItemsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem == newItem
        }
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
