package com.nexauren.audiotools.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

    fun dp(context: Context, value: Float): Float =
        value * context.resources.displayMetrics.density

    fun page(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.audio_bg))
            setPadding(dp(context, 18), dp(context, 12), dp(context, 18), dp(context, 34))
        }

    fun title(context: Context, text: String, size: Float = 30f): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(ContextCompat.getColor(context, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            includeFontPadding = false
        }

    fun subtitle(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
            setLineSpacing(1.16f, 1f)
        }

    fun eyebrow(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(context, R.color.audio_blue))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        }

    fun sectionLabel(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text.uppercase()
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.14f
        }

    fun card(
        context: Context,
        clickable: Boolean = false,
        accentColorRes: Int? = null
    ): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 22).toFloat()
            cardElevation = dp(context, if (clickable) 2 else 1).toFloat()
            strokeWidth = dp(context, if (accentColorRes != null) 1.5f else 1f)
            strokeColor = ContextCompat.getColor(
                context,
                accentColorRes ?: R.color.audio_border
            )
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface))
            isClickable = clickable
            isFocusable = clickable
            if (clickable) {
                rippleColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, accentColorRes ?: R.color.audio_blue_soft)
                )
            }
        }

    fun hero(context: Context): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 28).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.audio_blue)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface))
        }

    fun coloredSurface(context: Context, colorRes: Int): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 1), dp(context, 1), dp(context, 1), dp(context, 1))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 22).toFloat()
                setColor(ContextCompat.getColor(context, colorRes))
            }
        }

    fun button(
        context: Context,
        text: String,
        primary: Boolean = true,
        accentColorRes: Int = R.color.audio_blue
    ): MaterialButton =
        MaterialButton(context).apply {
            this.text = text
            minHeight = dp(context, 54)
            isAllCaps = false
            cornerRadius = dp(context, 16)
            insetTop = 0
            insetBottom = 0
            setPadding(dp(context, 16), 0, dp(context, 16), 0)
            val accent = ContextCompat.getColor(context, accentColorRes)
            if (primary) {
                backgroundTintList = ColorStateList.valueOf(accent)
                setTextColor(Color.WHITE)
                strokeWidth = 0
            } else {
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.audio_surface))
                setTextColor(accent)
                strokeWidth = dp(context, 1)
                strokeColor = ColorStateList.valueOf(accent)
            }
        }

    fun iconButton(context: Context, symbol: String, description: String): MaterialButton =
        MaterialButton(context).apply {
            text = symbol
            contentDescription = description
            minWidth = dp(context, 48)
            minHeight = dp(context, 48)
            cornerRadius = dp(context, 15)
            insetTop = 0
            insetBottom = 0
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.audio_surface))
            setTextColor(ContextCompat.getColor(context, R.color.audio_text))
            strokeWidth = dp(context, 1)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.audio_border))
        }

    fun pill(context: Context, text: String, positive: Boolean = false, colorRes: Int? = null): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val accent = ContextCompat.getColor(context, colorRes ?: if (positive) R.color.audio_green else R.color.audio_blue)
            val soft = ContextCompat.getColor(
                context,
                colorRes?.let {
                    when (it) {
                        R.color.audio_green -> R.color.audio_green_soft
                        R.color.audio_red -> R.color.audio_red_soft
                        R.color.audio_yellow -> R.color.audio_yellow_soft
                        else -> R.color.audio_blue_soft
                    }
                } ?: if (positive) R.color.audio_green_soft else R.color.audio_blue_soft
            )
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(dp(context, 11), dp(context, 7), dp(context, 11), dp(context, 7))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 50).toFloat()
                setColor(soft)
            }
        }

    fun stat(
        context: Context,
        value: String,
        label: String,
        accentColorRes: Int = R.color.audio_blue
    ): MaterialCardView {
        val card = card(context, accentColorRes = accentColorRes)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 13), dp(context, 13), dp(context, 13), dp(context, 13))
        }
        content.addView(TextView(context).apply {
            text = value
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, accentColorRes))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(spacer(context, 3))
        content.addView(TextView(context).apply {
            text = label
            textSize = 10.5f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
        })
        card.addView(content)
        return card
    }

    fun iconBadge(context: Context, text: String, colorRes: Int = R.color.audio_blue): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, colorRes))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 16).toFloat()
                setColor(
                    ContextCompat.getColor(
                        context,
                        when (colorRes) {
                            R.color.audio_green -> R.color.audio_green_soft
                            R.color.audio_red -> R.color.audio_red_soft
                            R.color.audio_yellow -> R.color.audio_yellow_soft
                            else -> R.color.audio_blue_soft
                        }
                    )
                )
            }
        }

    fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.audio_border))
            layoutParams = LinearLayout.LayoutParams(1, dp(context, 1))
        }

    fun spacer(context: Context, height: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
        }
}
