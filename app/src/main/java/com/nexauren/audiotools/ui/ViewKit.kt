package com.nexauren.audiotools.ui

import android.content.Context
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

    fun page(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.audio_bg))
            setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 30))
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
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
            setLineSpacing(1.12f, 1f)
        }

    fun eyebrow(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.audio_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.1f
        }

    fun sectionLabel(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text.uppercase()
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        }

    fun card(context: Context, clickable: Boolean = false): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 24).toFloat()
            cardElevation = dp(context, 1).toFloat()
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.audio_border)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface))
            isClickable = clickable
            isFocusable = clickable
            if (clickable) {
                foreground = GradientDrawable().apply {
                    cornerRadius = dp(context, 24).toFloat()
                    setColor(Color.TRANSPARENT)
                }
            }
        }

    fun hero(context: Context): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 28).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_hero))
        }

    fun button(context: Context, text: String, primary: Boolean = true): MaterialButton =
        MaterialButton(context).apply {
            this.text = text
            minHeight = dp(context, 54)
            isAllCaps = false
            cornerRadius = dp(context, 16)
            insetTop = 0
            insetBottom = 0
            if (primary) {
                setBackgroundColor(ContextCompat.getColor(context, R.color.audio_primary))
                setTextColor(Color.WHITE)
            } else {
                setBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface_alt))
                setTextColor(ContextCompat.getColor(context, R.color.audio_text))
                strokeWidth = dp(context, 1)
                strokeColor = ContextCompat.getColorStateList(context, R.color.audio_border)
            }
        }

    fun pill(context: Context, text: String, positive: Boolean = false): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (positive) R.color.audio_success else R.color.audio_primary
                )
            )
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 50).toFloat()
                setColor(
                    ContextCompat.getColor(
                        context,
                        if (positive) R.color.audio_success_soft else R.color.audio_primary_soft
                    )
                )
            }
        }

    fun stat(context: Context, value: String, label: String): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = dp(context, 20).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.audio_border)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.audio_surface))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
        }
        content.addView(TextView(context).apply {
            text = value
            textSize = 21f
            setTextColor(ContextCompat.getColor(context, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(ViewKit.spacer(context, 3))
        content.addView(TextView(context).apply {
            text = label
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.audio_muted))
        })
        card.addView(content)
        return card
    }

    fun iconBadge(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.audio_primary))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 16).toFloat()
                setColor(ContextCompat.getColor(context, R.color.audio_primary_soft))
            }
        }

    fun spacer(context: Context, height: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
        }
}