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
            color =
                ContextCompat.getColor(
                    context,
                    R.color.audio_teal
                )
        }

    private val nodePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color =
                ContextCompat.getColor(
                    context,
                    R.color.audio_teal
                )
        }

    private val innerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
            color =
                ContextCompat.getColor(
                    context,
                    R.color.audio_border
                )
        }

    private var rotation = 0f
    private var animator: ValueAnimator? = null

    init {
        visibility = View.GONE
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun start() {
        visibility = View.VISIBLE
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
        visibility = View.GONE
        rotation = 0f
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

        if (visibility != View.VISIBLE) {
            return
        }

        val cx =
            width / 2f

        val cy =
            height / 2f

        val radius =
            minOf(
                width,
                height
            ) * 0.27f

        val outer =
            RectF(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
            )

        canvas.drawArc(
            outer,
            rotation,
            265f,
            false,
            ringPaint
        )

        canvas.drawCircle(
            cx,
            cy,
            radius * 0.42f,
            innerPaint
        )

        val nodes = 8

        for (i in 0 until nodes) {
            val angle =
                Math.toRadians(
                    rotation +
                        i *
                        (360.0 / nodes)
                )

            val x =
                cx +
                    cos(angle).toFloat() *
                    radius *
                    1.22f

            val y =
                cy +
                    sin(angle).toFloat() *
                    radius *
                    1.22f

            val size =
                if (i % 2 == 0) {
                    dp(3.5f)
                } else {
                    dp(2.5f)
                }

            canvas.drawCircle(
                x,
                y,
                size,
                nodePaint
            )
        }
    }

    private fun dp(
        value: Float
    ): Float =
        value *
            resources.displayMetrics.density
}
