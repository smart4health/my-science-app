package com.healthmetrix.myscience.feature.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthmetrix.myscience.commons.ui.ViewBindingViewHolder
import com.healthmetrix.s4h.myscience.databinding.ViewHolderSpacerBinding

class SpacerAdapter : RecyclerView.Adapter<ViewBindingViewHolder<ViewHolderSpacerBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewBindingViewHolder<ViewHolderSpacerBinding> {
        return LayoutInflater.from(parent.context)
            .let { ViewHolderSpacerBinding.inflate(it, parent, false) }
            .let(::ViewBindingViewHolder)
    }

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder<ViewHolderSpacerBinding>,
        position: Int,
    ) {
        // nothing to do
    }

    override fun getItemCount(): Int = 1
}
