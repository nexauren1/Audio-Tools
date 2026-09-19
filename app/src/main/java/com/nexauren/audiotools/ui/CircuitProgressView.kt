package com.nexauren.audiotools.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.R
import kotlin.math.cos
import kotlin.math.sin

class CircuitProgressView(
    context: Context
) : View(context) {
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3f)
            strokeCap = Paint.Cap.ROUND
        }

    private val dotPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val trackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
        }

    private var rotation = 0f
    private var animator: ValueAnimator? = null

    init {
        isVisible = false
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val tint =
            ContextCompat.getColor(
                context,
                R.color.audio_teal
            )

        ringPaint.color = tint
        dotPaint.color = tint
        trackPaint.color =
            ContextCompat.getColor(
                context,
                R.color.audio_border
            )
    }

    fun start() {
        isVisible = true

        animator?.cancel()

        animator =
            ValueAnimator.ofFloat(
                0f,
                360f
            ).apply {
                duration = 920L
                repeatCount =
                    ValueAnimator.INFINITE
                interpolator =
                    LinearInterpolator()

                addUpdateListener {
                    rotation =
                        it.animatedValue as Float
                    invalidate()
                }

                start()
            }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        isVisible = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(
        canvas: Canvas
    ) {
        super.onDraw(canvas)

        if (!isVisible) {
            return
        }

        val cx =
            width / 2f
        val cy =
            height / 2f

        val radius =
            minOf(width, height) * 0.28f

        val rect =
            RectF(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
            )

        canvas.drawArc(
            rect,
            rotation,
            260f,
            false,
            ringPaint
        )

        canvas.drawCircle(
            cx,
            cy,
            radius * 0.38f,
            trackPaint
        )

        val nodes = 8

        for (i in 0 until nodes) {
            val angle =
                Math.toRadians(
                    rotation +
                        i *
                        (360.0 / nodes)
                )

            val nx =
                cx +
                    cos(angle).toFloat() *
                    radius * 1.22f

            val ny =
                cy +
                    sin(angle).toFloat() *
                    radius * 1.22f

            val size =
                if (i % 2 == 0) {
                    dp(3.5f)
                } else {
                    dp(2.5f)
                }

            canvas.drawCircle(
                nx,
                ny,
                size,
                dotPaint
            )
        }
    }

    private fun dp(
        value: Float
    ): Float =
        value *
            resources.displayMetrics.density

    private val isVisible: Boolean
        get() = visibility == View.VISIBLE

    private var isVisibleProperty: Boolean = false

    private fun setVisible(
        value: Boolean
    ) {
        visibility =
            if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }

        isVisibleProperty = value
    }

    private val visibleProperty
        get() = isVisibleProperty

    private var visibilityState: Boolean
        get() = isVisibleProperty
        set(value) {
            setVisible(value)
        }

    private var View.isVisible: Boolean
        get() = visibilityState
        set(value) {
            visibilityState = value
        }
}
