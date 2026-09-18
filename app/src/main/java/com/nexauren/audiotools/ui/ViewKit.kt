package com.nexauren.audiotools.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.nexauren.audiotools.R

object ViewKit {
    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun page(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.audio_bg))
            setPadding(dp(context,20), dp(context,12), dp(context,20), dp(context,24))
        }

    fun title(context: Context, text: String, size: Float = 30f): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(ContextCompat.getColor(context, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    fun subtitle(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
            setLineSpacing(1.1f,1f)
        }

    fun card(context: Context, clickable: Boolean = false): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context,22).toFloat()
            cardElevation = dp(context,1).toFloat()
            strokeWidth = dp(context,1)
            strokeColor = ContextCompat.getColor(context, R.color.audio_border)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface))
            isClickable = clickable
            isFocusable = clickable
        }

    fun button(context: Context, text: String, primary: Boolean = true): MaterialButton =
        MaterialButton(context).apply {
            this.text = text
            minHeight = dp(context,52)
            isAllCaps = false
            cornerRadius = dp(context,16)
            if (primary) {
                setBackgroundColor(ContextCompat.getColor(context, R.color.audio_primary))
                setTextColor(Color.WHITE)
            } else {
                setTextColor(ContextCompat.getColor(context, R.color.audio_text))
            }
        }

    fun pill(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.audio_primary))
            gravity = Gravity.CENTER
            setPadding(dp(context,12),dp(context,6),dp(context,12),dp(context,6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(context,50).toFloat()
                setColor(Color.parseColor("#E9EFFF"))
            }
        }

    fun spacer(context: Context, height: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1,dp(context,height))
        }
}