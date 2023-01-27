package com.healthmetrix.myscience.feature.login

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.healthmetrix.s4h.myscience.R
import kotlin.math.abs
import kotlin.math.min

class MilestoneProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    val milestoneWeights: List<Int>

    var currentMilestone = 0
        private set

    private val trackPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }
    private val indicatorPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private val reachedMilestonePaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private val unreachedMilestonePaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private val risingTransitionPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private val fallingTransitionPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private var milestonePaints: List<Paint>

    private var progressAnimator: ValueAnimator? = null
    private val progressInterpolator = DecelerateInterpolator()
    private val argbEvaluator = ArgbEvaluator()

    private var currentProgress = 0f

    private val trackRect = RectF(0f, 0f, 0f, 0f)
    private val indicatorRect = RectF(0f, 0f, 0f, 0f)

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MilestoneProgressView, 0, 0).apply {
            try {
                trackPaint.color =
                    getColor(R.styleable.MilestoneProgressView_trackColor, Color.GREEN)
                indicatorPaint.color =
                    getColor(R.styleable.MilestoneProgressView_indicatorColor, Color.RED)
                reachedMilestonePaint.color =
                    getColor(R.styleable.MilestoneProgressView_milestoneReachedColor, Color.BLUE)
                unreachedMilestonePaint.color =
                    getColor(R.styleable.MilestoneProgressView_milestoneUnreachedColor, Color.WHITE)

                milestoneWeights = getString(R.styleable.MilestoneProgressView_milestoneWeights)
                    ?.split(",")
                    ?.map(String::toInt)
                    ?: error("must set app:milestoneWeights on MilestoneProgressViews")

                milestonePaints = milestoneWeights.map { unreachedMilestonePaint }

                risingTransitionPaint.color = unreachedMilestonePaint.color
                fallingTransitionPaint.color = reachedMilestonePaint.color
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        trackRect.apply {
            left = 0f
            right = width.toFloat()
            top = 0f
            bottom = height.toFloat()
        }

        val extraWidth = height

        indicatorRect.apply {
            left = 0f
            right = ((width - extraWidth) * currentProgress) + extraWidth
            top = 0f
            bottom = height.toFloat()
        }

        val trackRadius = (height / 2).toFloat()

        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)
        canvas.drawRoundRect(indicatorRect, trackRadius, trackRadius, indicatorPaint)

        var acc = 0
        val total = milestoneWeights.sum()
        val radius = height.toFloat() / 4f
        milestoneWeights.forEachIndexed { index, weight ->
            acc += weight
            val percentage = acc.toFloat() / total.toFloat()
            canvas.drawCircle(
                ((width - extraWidth) * percentage) + extraWidth - radius * 2,
                (height / 2).toFloat(),
                radius,
                milestonePaints[index],
            )
        }
    }

    fun animateTo(milestone: Int, nextMilestoneProgress: Float = 0f) {
        progressAnimator?.cancel()

        milestonePaints = milestoneWeights.getChanges(currentMilestone, milestone).map {
            when (it) {
                MilestoneChanges.REACHED -> reachedMilestonePaint
                MilestoneChanges.REACHED_TO_UNREACHED -> fallingTransitionPaint
                MilestoneChanges.UNREACHED_TO_REACHED -> risingTransitionPaint
                MilestoneChanges.UNREACHED -> unreachedMilestonePaint
            }
        }

        currentMilestone = milestone

        val targetPercent = milestoneWeights.getProgress(currentMilestone, nextMilestoneProgress)

        progressAnimator = ValueAnimator.ofFloat(currentProgress, targetPercent).apply {
            // android.widget.ProgressBar#PROGRESS_ANIM_DURATION is 80 but that is way too quick
            duration = 300L
            // android.widget.ProgressBar#PROGRESS_ANIM_INTERPOLATOR
            interpolator = progressInterpolator
            addUpdateListener {
                currentProgress = it.animatedValue as Float
                risingTransitionPaint.color = argbEvaluator.evaluate(
                    it.animatedFraction,
                    unreachedMilestonePaint.color,
                    reachedMilestonePaint.color,
                ) as Int
                fallingTransitionPaint.color = argbEvaluator.evaluate(
                    it.animatedFraction,
                    reachedMilestonePaint.color,
                    unreachedMilestonePaint.color,
                ) as Int
                invalidate()
            }
            start()
        }
    }

    @Suppress("unused")
    fun jumpTo(milestone: Int, nextMilestoneProgress: Float = 0f) {
        progressAnimator?.cancel()

        milestonePaints = milestoneWeights.getChanges(currentMilestone, milestone).map {
            when (it) {
                MilestoneChanges.REACHED -> reachedMilestonePaint
                // no transitioning necessary when jumping
                MilestoneChanges.REACHED_TO_UNREACHED -> unreachedMilestonePaint
                // no transitioning necessary when jumping
                MilestoneChanges.UNREACHED_TO_REACHED -> reachedMilestonePaint
                MilestoneChanges.UNREACHED -> unreachedMilestonePaint
            }
        }

        currentMilestone = milestone

        currentProgress = milestoneWeights.getProgress(currentMilestone, nextMilestoneProgress)

        invalidate()
    }

    @Suppress("unused")
    fun cancel() {
        progressAnimator?.cancel()
    }
}

fun List<Int>.getProgress(milestone: Int, nextMilestonePercentage: Float): Float {
    require(milestone >= 0)
    require(milestone <= size)
    require(nextMilestonePercentage < 1f)
    require(nextMilestonePercentage >= 0f)

    val weightIndex = milestone - 1
    val total = sum()
    val reachedTotal = subList(0, milestone).sum()
    val baseProgress = reachedTotal.toFloat() / total.toFloat()
    val nextMilestoneProgress = getOrElse(weightIndex + 1) { 0 }.toFloat() /
        total.toFloat() * nextMilestonePercentage

    return baseProgress + nextMilestoneProgress
}

private fun List<Int>.getChanges(
    currentMilestone: Int,
    nextMilestone: Int,
): List<MilestoneChanges> {
    val numReached = min(currentMilestone, nextMilestone)
    val numTransitioning = abs(nextMilestone - currentMilestone)
    val numUnreached = size - numTransitioning - numReached

    val transitionKind = if (nextMilestone > currentMilestone) {
        MilestoneChanges.UNREACHED_TO_REACHED
    } else {
        MilestoneChanges.REACHED_TO_UNREACHED
    }

    return List(numReached) { MilestoneChanges.REACHED } +
        List(numTransitioning) { transitionKind } +
        List(numUnreached) { MilestoneChanges.UNREACHED }
}

private enum class MilestoneChanges {
    REACHED,
    REACHED_TO_UNREACHED,
    UNREACHED_TO_REACHED,
    UNREACHED,
}
