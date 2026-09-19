package com.nexauren.audiotools.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class WaveformTrimView(context: Context) : View(context) {
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handle = Paint(Paint.ANTI_ALIAS_FLAG)
    var durationMs: Long = 0L
        set(value) {
            field = max(0L, value)
            if (field == 0L) {
                startMs = 0L
                endMs = 0L
            } else {
                if (endMs <= 0L || endMs > field) endMs = field
                if (startMs < 0L || startMs >= endMs) startMs = 0L
            }
            invalidate()
        }
    var startMs: Long = 0L
        private set(value) {
            field = value.coerceIn(0L, max(0L, endMs - 1L))
        }
    var endMs: Long = 0L
        private set(value) {
            field = value.coerceIn(min(durationMs, max(startMs + 1L, 1L)), max(durationMs, 1L))
        }

    var onRangeChanged: ((Long, Long) -> Unit)? = null
    private var activeHandle = 0
    private val bars = FloatArray(72) { i ->
        (0.20f + abs(sin(i * 0.61f)) * 0.55f + abs(sin(i * 1.93f)) * 0.18f).coerceIn(0.12f, 0.92f)
    }

    init {
        isClickable = true
        contentDescription = "Audio trim waveform"
    }

    fun resetRange() {
        startMs = 0L
        endMs = durationMs
        invalidate()
        onRangeChanged?.invoke(startMs, endMs)
    }

    fun setRange(start: Long, end: Long) {
        if (durationMs <= 0L) return
        endMs = end.coerceIn(1L, durationMs)
        startMs = start.coerceIn(0L, endMs - 1L)
        invalidate()
        onRangeChanged?.invoke(startMs, endMs)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat()
        val bg = ContextCompat.getColor(context, R.color.audio_surface_alt)
        val accent = ContextCompat.getColor(context, R.color.audio_blue)
        canvas.drawColor(ContextCompat.getColor(context, R.color.audio_surface))

        grid.color = bg
        grid.alpha = 180
        grid.strokeWidth = dp(1f)
        for (i in 1..7) {
            val x = w * i / 8f
            canvas.drawLine(x, 0f, x, h, grid)
        }

        wave.color = accent
        wave.alpha = 95
        wave.strokeWidth = dp(2.2f)
        val center = h / 2f
        val slot = w / bars.size
        bars.forEachIndexed { i, amp ->
            val x = i * slot + slot / 2f
            val y1 = center - amp * h * 0.38f
            val y2 = center + amp * h * 0.38f
            canvas.drawLine(x, y1, x, y2, wave)
        }

        val sx = pos(startMs, w)
        val ex = pos(endMs, w)
        fill.style = Paint.Style.FILL
        fill.color = accent
        fill.alpha = 32
        canvas.drawRect(RectF(sx, 0f, ex, h), fill)

        handle.color = accent
        handle.alpha = 255
        canvas.drawRoundRect(RectF(sx - dp(6f), 0f, sx + dp(6f), h), dp(5f), dp(5f), handle)
        handle.alpha = 230
        canvas.drawRoundRect(RectF(ex - dp(6f), 0f, ex + dp(6f), h), dp(5f), dp(5f), handle)

        handle.color = ContextCompat.getColor(context, R.color.audio_surface)
        handle.alpha = 255
        canvas.drawCircle(sx, dp(15f), dp(4f), handle)
        canvas.drawCircle(ex, dp(15f), dp(4f), handle)
        canvas.drawCircle(sx, h - dp(15f), dp(4f), handle)
        canvas.drawCircle(ex, h - dp(15f), dp(4f), handle)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (durationMs <= 0L) return true
        val w = width.toFloat().coerceAtLeast(1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val sx = pos(startMs, w)
                val ex = pos(endMs, w)
                activeHandle = when {
                    abs(x - sx) < dp(32f) -> 1
                    abs(x - ex) < dp(32f) -> 2
                    x < (sx + ex) / 2f -> 1
                    else -> 2
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromX(x, w)
                return true
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                updateFromX(event.x, w)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }
        return true
    }

    private fun updateFromX(x: Float, w: Float) {
        val value = (x.coerceIn(0f, w) / w * durationMs).toLong()
        if (activeHandle == 1) startMs = min(value, endMs - 1L).coerceAtLeast(0L)
        if (activeHandle == 2) endMs = max(value, startMs + 1L).coerceAtMost(durationMs)
        invalidate()
        onRangeChanged?.invoke(startMs, endMs)
    }

    private fun pos(ms: Long, w: Float): Float =
        if (durationMs <= 0L) 0f else (ms.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
