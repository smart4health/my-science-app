package com.healthmetrix.myscience.feature.login

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SelfCollapsingBottomSheetBehavior constructor(
    context: Context,
    attributeSet: AttributeSet?,
) : BottomSheetBehavior<View>(context, attributeSet) {

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: View,
        ev: MotionEvent,
    ): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (!parent.isPointInChildBounds(child, ev.x.toInt(), ev.y.toInt()) && state != STATE_HIDDEN) {
                state = STATE_COLLAPSED
            }
        }
        return super.onInterceptTouchEvent(parent, child, ev)
    }

    companion object {
        /**
         * Wildly fragile due to lots of casting, be careful what you call this on
         */
        fun from(view: View): SelfCollapsingBottomSheetBehavior {
            return (view.layoutParams as CoordinatorLayout.LayoutParams).behavior as SelfCollapsingBottomSheetBehavior
        }
    }
}
