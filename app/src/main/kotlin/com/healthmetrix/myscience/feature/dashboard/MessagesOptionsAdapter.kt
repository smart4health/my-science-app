package com.healthmetrix.myscience.feature.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.RecyclerView
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.myscience.commons.ui.enforceMinTouchTargetSize
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ItemMessagesOptionBinding

class MessagesOptionsAdapter(
    private val listener: (Boolean) -> Unit,
) : RecyclerView.Adapter<ViewBindingViewHolder<ItemMessagesOptionBinding>>() {

    private var isEnabled: Boolean = false

    private var changeListener: ((Boolean) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ItemMessagesOptionBinding> =
        LayoutInflater.from(parent.context)
            .let { ItemMessagesOptionBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ItemMessagesOptionBinding>,
        position: Int,
    ) {
        holder.binding.messagesOptionSwitch.doOnNextLayout { v ->
            v.enforceMinTouchTargetSize(R.attr.minTouchTargetSize)
        }

        holder.binding.messagesOptionSwitch.isChecked = isEnabled

        changeListener = {
            holder.binding.messagesOptionSwitch.isChecked = isEnabled
        }

        holder.binding.messagesOptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }

    override fun getItemCount(): Int = 1

    fun setIsEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        changeListener?.invoke(isEnabled)
    }
}
