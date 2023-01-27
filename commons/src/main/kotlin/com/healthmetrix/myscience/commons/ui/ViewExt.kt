package com.healthmetrix.myscience.commons.ui

import android.content.res.TypedArray
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import androidx.annotation.AttrRes

fun View.enforceMinTouchTargetSize(@AttrRes minTouchTargetSizeAttr: Int) {
    lateinit var typedArray: TypedArray
    val minTouchTargetSize = try {
        typedArray = context.theme.obtainStyledAttributes(intArrayOf(minTouchTargetSizeAttr))
        typedArray.getDimension(0, -1f)
    } finally {
        typedArray.recycle()
    }

    val hitRect = Rect().apply {
        getHitRect(this)
    }

    if (minTouchTargetSize > 0 && (hitRect.width() < minTouchTargetSize || hitRect.height() < minTouchTargetSize)) {
        if (hitRect.width() < minTouchTargetSize) {
            val expandHorizontal = ((minTouchTargetSize - hitRect.width()) / 2).toInt()
            hitRect.left -= expandHorizontal
            hitRect.right += expandHorizontal
        }

        if (hitRect.height() < minTouchTargetSize) {
            val expandVertical = ((minTouchTargetSize - hitRect.height()) / 2).toInt()
            hitRect.top -= expandVertical
            hitRect.bottom += expandVertical
        }

        (parent as? View)?.touchDelegate = TouchDelegate(hitRect, this)
    }
}

/**
 * re-entrant fading animation extension function
 * make sure views are ready by starting as GONE and alpha of 0
 */
fun View.fade(out: Boolean) {
    if (!out) {
        visibility = View.VISIBLE
    }

    animate().apply {
        cancel()
        duration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        alpha(if (out) 0f else 1f)

        setListener(null)

        if (out) withEndAction {
            visibility = View.GONE
        }
    }
}

fun View.fadeOut() = fade(out = true)

fun View.fadeIn() = fade(out = false)
