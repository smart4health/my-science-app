package com.healthmetrix.myscience.feature.dataselection

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.doOnNextLayout
import com.healthmetrix.myscience.commons.ui.enforceMinTouchTargetSize
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.CardDataSelectionBinding

/**
 * Note: parent should not clipChildren or clipToPadding,
 *       and should probably have some padding
 */
class DataSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val cardMargin: Float

    init {
        orientation = VERTICAL

        context.theme.obtainStyledAttributes(attrs, R.styleable.DataSelectionView, 0, 0).apply {
            try {
                val defaultCardMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8f,
                    context.resources.displayMetrics,
                )

                cardMargin = getDimension(
                    R.styleable.DataSelectionView_dataSelectionCardMargin,
                    defaultCardMargin,
                )
            } finally {
                recycle()
            }
        }
    }

    private val entries = mutableListOf<Entry>()

    var onEntriesChangedListener: ((List<Entry>) -> Unit)? = null

    fun setEntries(newEntries: List<Entry>) {
        if (newEntries == entries) {
            return
        }

        entries.clear()
        entries.addAll(newEntries)

        removeAllViews()

        val inflater = LayoutInflater.from(context)

        entries.forEachIndexed { index, entry ->
            val card = CardDataSelectionBinding.inflate(inflater, this, false).apply {
                title.text = entry.title
                description.text = entry.description
                if (entry.required) {
                    requiredBadge.visibility = View.VISIBLE
                    switchMaterial.visibility = View.GONE
                } else {
                    requiredBadge.visibility = View.GONE
                    switchMaterial.visibility = View.VISIBLE
                    switchMaterial.isChecked = entry.checked
                    switchMaterial.setOnCheckedChangeListener { _, isChecked ->
                        val i = entries.indexOfFirst { it.key == entry.key }
                        entries[i] = entry.copy(checked = isChecked)
                        onEntriesChangedListener?.invoke(entries)
                    }

                    switchMaterial.doOnNextLayout { v ->
                        v.enforceMinTouchTargetSize(R.attr.minTouchTargetSize)
                    }
                }
            }.root

            val layoutParams = LayoutParams(
                card.layoutParams.width,
                card.layoutParams.height,
            ).apply {
                if (index < entries.size - 1) {
                    setMargins(leftMargin, topMargin, rightMargin, cardMargin.toInt())
                }
            }

            addView(card, layoutParams)
        }
    }

    data class Entry(
        val key: String,
        val title: String,
        val description: String,
        val checked: Boolean,
        val required: Boolean = false,
    )
}
