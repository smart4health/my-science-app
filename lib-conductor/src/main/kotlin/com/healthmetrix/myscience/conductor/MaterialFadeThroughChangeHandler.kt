package com.healthmetrix.myscience.conductor

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import com.google.android.material.appbar.MaterialToolbar

/**
 * Per material specs: https://material.io/design/motion/the-motion-system.html#fade-through
 *
 * Caveats:
 * - constant duration
 */
class MaterialFadeThroughChangeHandler : AnimatorChangeHandler(0, true) {
    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        val fromToolbar =
            from?.findViewWithTag<MaterialToolbar?>("toolbar")
        val fromContent =
            from?.findViewWithTag<View?>("content")

        val toToolbar =
            to?.findViewWithTag<MaterialToolbar?>("toolbar")
        val toContent =
            to?.findViewWithTag<View?>("content")

        toToolbar?.alpha = 0f
        toContent?.alpha = 0f
        toContent?.scaleX = .92f
        toContent?.scaleY = .92f

        return AnimatorSet().apply {
            playSequentially(
                AnimatorSet().apply {
                    val fromPlayTogether =
                        ((fromToolbar?.children?.toList() ?: listOf()) + listOfNotNull(fromContent))

                    fromPlayTogether.map { v ->
                        ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0f).apply {
                            duration = 90
                        }
                    }.let(this::playTogether)

                    doOnEnd {
                        toToolbar?.alpha = 1f
                        toToolbar?.children?.forEach { v ->
                            v.alpha = 0f
                        }
                    }
                },
                AnimatorSet().apply {
                    val toPlayTogether = (toToolbar?.children?.toList() ?: listOf()).map { v ->
                        ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f).apply {
                            duration = 120
                        }
                    } + listOfNotNull(
                        toContent?.let { v ->
                            ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f).apply {
                                duration = 120
                            }
                        },
                        toContent?.let { v ->
                            ObjectAnimator.ofFloat(v, View.SCALE_X, .92f, 1f).apply {
                                duration = 120
                                interpolator = FastOutSlowInInterpolator()
                            }
                        },
                        toContent?.let { v ->
                            ObjectAnimator.ofFloat(v, View.SCALE_Y, .92f, 1f).apply {
                                duration = 120
                                interpolator = FastOutSlowInInterpolator()
                            }
                        },
                    )

                    playTogether(toPlayTogether)
                },
            )
        }
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }
}
