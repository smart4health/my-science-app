package com.healthmetrix.myscience.conductor
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

class ScaleAndFadeChangeHandler(duration: Long = DEFAULT_ANIMATION_DURATION) : AnimatorChangeHandler(duration, true) {

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        // crude imitation of the default activity transition

        return (if (isPush) to else from)?.let { v ->
            AnimatorSet().also { set ->
                val (scale0, scale1) = if (isPush) .97f to 1f else 1f to .97f
                val (alpha0, alpha1) = if (isPush) .65f to 1f else 1f to .65f
                val (y0, y1) = if (isPush) 80f to 0f else 0f to 80f
                set.play(ObjectAnimator.ofFloat(v, View.SCALE_X, scale0, scale1))
                set.play(ObjectAnimator.ofFloat(v, View.SCALE_Y, scale0, scale1))
                set.play(ObjectAnimator.ofFloat(v, View.ALPHA, alpha0, alpha1))
                set.play(ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, y0, y1))
                set.interpolator = FastOutSlowInInterpolator()
            }
        } ?: AnimatorSet()
    }

    override fun resetFromView(from: View) {
        from.scaleX = 1f
        from.scaleY = 1f
        from.alpha = 1f
        from.translationX = 0f
    }
}
