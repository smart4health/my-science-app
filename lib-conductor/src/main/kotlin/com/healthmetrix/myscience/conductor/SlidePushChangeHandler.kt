package com.healthmetrix.myscience.conductor

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

private const val KEY = "HMX_SLIDE_PUSH_CHANGE_HANDLER_IS_FORWARD"

/**
 * Handles forward and backwards transitions
 *
 * Does not handle popping, because right now we are always
 * replacing Top, which does not pop the controller being replaced
 */
class SlidePushChangeHandler(
    private var isForward: Boolean = true,
) : AnimatorChangeHandler(DEFAULT_ANIMATION_DURATION, true) {

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        if (!isPush) throw IllegalArgumentException("Not supported")

        return AnimatorSet().also { set ->
            from?.let { v ->
                val end = if (isForward) -v.width else v.width
                set.play(ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0f, end.toFloat()))
            }

            to?.let { v ->
                val start = if (isForward) v.width else -v.width
                set.play(ObjectAnimator.ofFloat(v, View.TRANSLATION_X, start.toFloat(), 0f))
            }
        }
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putBoolean(KEY, isForward)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        isForward = bundle.getBoolean(KEY, true)
    }
}
