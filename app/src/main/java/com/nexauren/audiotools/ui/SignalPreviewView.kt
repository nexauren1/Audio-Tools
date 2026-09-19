package com.nexauren.audiotools.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.R
import kotlin.math.abs
import kotlin.math.sin

class SignalPreviewView(
    context: Context,
    private val accentColorRes: Int = R.color.audio_blue,
    private val live: Boolean = false
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val waveform = FloatArray(64) { index ->
        val base = abs(sin(index * 0.57f)) * 0.72f
        val variation = 0.55f + abs(sin(index * 2.31f)) * 0.35f
        (0.16f + base * variation).coerceIn(0.08f, 0.94f)
    }
    private var phase = 0f
    private var level = 0.28f
    private var running = false

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        contentDescription = if (live) "Medidor de áudio em tempo real" else "Preview de waveform"
        if (live) start()
    }

    fun start() {
        running = true
        postInvalidateOnAnimation()
    }

    fun stop() {
        running = false
        invalidate()
    }

    fun setLevel(value: Float) {
        level = value.coerceIn(0f, 1f)
        if (!running) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val accent = ContextCompat.getColor(context, accentColorRes)
        val soft = ContextCompat.getColor(context, R.color.audio_surface_alt)
        canvas.drawColor(ContextCompat.getColor(context, R.color.audio_surface))

        gridPaint.color = soft
        gridPaint.alpha = 170
        val gridY = h / 4f
        for (i in 1..3) {
            canvas.drawLine(0f, gridY * i, w, gridY * i, gridPaint)
        }
        for (i in 1..7) {
            val x = w * i / 8f
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dp(2.5f)
        paint.color = accent
        paint.alpha = 55
        val glowPath = Path()
        val glow = Path()
        val center = h / 2f
        val slot = w / waveform.size.coerceAtLeast(1)
        for (i in waveform.indices) {
            val x = i * slot + slot / 2f
            val dynamic = if (live) {
                (waveform[i] * (0.35f + level * 0.95f) + sin(i * 0.42f + phase) * 0.08f).coerceIn(0.06f, 0.96f)
            } else {
                waveform[i]
            }
            val y = center - dynamic * (h * 0.40f)
            if (i == 0) {
                glowPath.moveTo(x, y)
            } else {
                glowPath.lineTo(x, y)
            }
        }
        paint.alpha = 42
        paint.strokeWidth = dp(7f)
        canvas.drawPath(glowPath, paint)

        paint.alpha = 235
        paint.strokeWidth = dp(2.5f)
        val mainPath = Path()
        for (i in waveform.indices) {
            val x = i * slot + slot / 2f
            val dynamic = if (live) {
                (waveform[i] * (0.35f + level * 0.95f) + sin(i * 0.42f + phase) * 0.08f).coerceIn(0.06f, 0.96f)
            } else waveform[i]
            val y = center - dynamic * (h * 0.40f)
            val mirror = center + dynamic * (h * 0.40f) * 0.72f
            if (i == 0) {
                mainPath.moveTo(x, y)
                glow.moveTo(x, mirror)
            } else {
                mainPath.lineTo(x, y)
                glow.lineTo(x, mirror)
            }
        }
        canvas.drawPath(mainPath, paint)
        paint.alpha = 95
        paint.strokeWidth = dp(1.6f)
        canvas.drawPath(glow, paint)

        val sweepX = if (live) ((phase / 11f) % 1f) * w else w * 0.64f
        paint.style = Paint.Style.FILL
        paint.color = accent
        paint.alpha = 26
        canvas.drawRect(RectF(sweepX - dp(20f), 0f, sweepX + dp(20f), h), paint)
        paint.alpha = 210
        canvas.drawRect(RectF(sweepX - dp(0.8f), h * 0.16f, sweepX + dp(0.8f), h * 0.84f), paint)

        if (running) {
            phase += 0.32f
            postInvalidateOnAnimation()
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
