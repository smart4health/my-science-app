package com.healthmetrix.myscience.feature.login

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.healthmetrix.s4h.myscience.R

/**
 * View to display progress in a flow
 *
 * Attributes:
 * - stepperInactiveDrawable: to display remaining and previous steps
 * - stepperActiveDrawable: to display the current step
 * - stepperOutOf: Number of total steps, greater than 0
 * - stepperCurrent: Current step, 1 indexed, (0, stepperOutOf]
 * - stepperInnerMargin: Space between drawables, defaults to 4dp
 */
class StepperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val activeDrawable: Drawable
    private val inactiveDrawable: Drawable
    private val current: Int
    private val outOf: Int

    private val innerMargin: Float

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StepperView, 0, 0).apply {
            try {
                activeDrawable = getDrawable(R.styleable.StepperView_stepperActiveDrawable)
                    ?: error("activeDrawable not set")

                inactiveDrawable = getDrawable(R.styleable.StepperView_stepperInactiveDrawable)
                    ?: error("activeDrawable not set")

                current = getInteger(R.styleable.StepperView_stepperCurrent, -1)
                outOf = getInteger(R.styleable.StepperView_stepperOutOf, -1)

                if (current < 1) {
                    error("stepperCurrent must be set and greater than 0")
                }

                if (outOf < 1) {
                    error("stepperOutOf must be set and greater than 0")
                }

                if (current > outOf) {
                    error("stepperCurrent must be less than or equal to stepperOutOf")
                }

                val defaultInnerMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4f,
                    context.resources.displayMetrics,
                )

                innerMargin =
                    getDimension(R.styleable.StepperView_stepperInnerMargin, defaultInnerMargin)
            } finally {
                recycle()
            }
        }

        (1..outOf).forEach { i ->
            val drawable = if (i == current) activeDrawable else inactiveDrawable

            ImageView(context, attrs, defStyleAttr, defStyleRes).also {
                it.setImageDrawable(drawable)
                it.layoutParams
            }.let(this::addView)

            if (i < outOf) {
                View(context, attrs, defStyleAttr, defStyleRes).also {
                    it.layoutParams = LayoutParams(innerMargin.toInt(), innerMargin.toInt())
                }.let(this::addView)
            }
        }
    }
}
