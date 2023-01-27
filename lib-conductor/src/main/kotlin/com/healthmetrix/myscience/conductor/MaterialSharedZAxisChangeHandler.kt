package com.healthmetrix.myscience.conductor

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import com.google.android.material.appbar.MaterialToolbar

private const val MULT = 1L

/**
 * Per material spec https://material.io/design/motion/the-motion-system.html#shared-axis
 */
class MaterialSharedZAxisChangeHandler : AnimatorChangeHandler(0, true) {

    @SuppressLint("Recycle")
    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        val fromToolbar = from?.findViewWithTag<MaterialToolbar?>("toolbar")
        val fromContent = from?.findViewWithTag<View?>("content")

        val toToolbar = to?.findViewWithTag<MaterialToolbar?>("toolbar")
        val toContent = to?.findViewWithTag<View?>("content")

        if (fromContent == null || toContent == null) {
            return AnimatorSet()
        }

        val scaleAnimator = if (isPush) AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(toContent, View.SCALE_X, .8f, 1f).scaleModifiers(),
                ObjectAnimator.ofFloat(toContent, View.SCALE_Y, .8f, 1f).scaleModifiers(),
                ObjectAnimator.ofFloat(fromContent, View.SCALE_X, 1f, 1.1f).scaleModifiers(),
                ObjectAnimator.ofFloat(fromContent, View.SCALE_Y, 1f, 1.1f).scaleModifiers(),
            )
        } else AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(toContent, View.SCALE_X, 1.1f, 1f).scaleModifiers(),
                ObjectAnimator.ofFloat(toContent, View.SCALE_Y, 1.1f, 1f).scaleModifiers(),
                ObjectAnimator.ofFloat(fromContent, View.SCALE_X, 1f, .8f).scaleModifiers(),
                ObjectAnimator.ofFloat(fromContent, View.SCALE_Y, 1f, .8f).scaleModifiers(),
            )
        }

        val fadeOutAnimator = AnimatorSet().apply {
            doOnStart {
                // keep the target transparent while the from views fade out
                to.alpha = 0f
            }

            fromToolbar.childrenOrEmpty.plus(fromContent).map { v ->
                ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0f).apply {
                    duration = 90 * MULT
                    interpolator = FastOutSlowInInterpolator()
                }
            }.toList().let(this::playTogether)

            doOnEnd {
                to.alpha = 1f
                if (!isPush) from.alpha = 0f
            }
        }

        val fadeInAnimator = AnimatorSet().apply {
            toToolbar.childrenOrEmpty.plus(toContent).map { v ->
                ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f).apply {
                    duration = 210 * MULT
                    interpolator = FastOutSlowInInterpolator()
                }
            }.toList().let(this::playTogether)
        }

        return AnimatorSet().apply {
            play(scaleAnimator).with(
                AnimatorSet().apply {
                    playSequentially(fadeOutAnimator, fadeInAnimator)
                },
            )
        }
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
        from.scaleX = 1f
        from.scaleY = 1f
    }

    private fun ObjectAnimator.scaleModifiers(): ObjectAnimator = apply {
        duration = 300 * MULT
        interpolator = FastOutSlowInInterpolator()
    }

    private val MaterialToolbar?.childrenOrEmpty: Sequence<View>
        get() = this?.children ?: sequenceOf()
}
