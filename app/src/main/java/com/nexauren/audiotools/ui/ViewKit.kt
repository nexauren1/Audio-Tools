package com.nexauren.audiotools.ui

import android.content.Context
import android.content.Intent
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
    fun dp(
        context: Context,
        value: Int
    ): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun dp(
        context: Context,
        value: Float
    ): Float =
        value * context.resources.displayMetrics.density

    fun page(
        context: Context
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation =
                LinearLayout.VERTICAL

            setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_bg
                )
            )

            val widthDp =
                resources.displayMetrics.widthPixels /
                    resources.displayMetrics.density

            val horizontal =
                if (widthDp < 360f) 15 else 18

            setPadding(
                dp(context, horizontal),
                dp(context, 12),
                dp(context, horizontal),
                dp(context, 30)
            )
        }

    fun title(
        context: Context,
        text: String,
        size: Float = 30f
    ): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_text
                )
            )
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
            includeFontPadding = false
        }

    fun subtitle(
        context: Context,
        text: String
    ): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 13.5f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_muted
                )
            )
            setLineSpacing(1.16f, 1f)
            includeFontPadding = false
        }

    fun eyebrow(
        context: Context,
        text: String
    ): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 10.5f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_blue
                )
            )
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
            letterSpacing = 0.12f
        }

    fun sectionLabel(
        context: Context,
        text: String
    ): TextView =
        TextView(context).apply {
            this.text = text.uppercase()
            textSize = 11f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_muted
                )
            )
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
            letterSpacing = 0.13f
        }

    fun card(
        context: Context,
        clickable: Boolean = false,
        accentColorRes: Int? = null
    ): MaterialCardView =
        MaterialCardView(context).apply {
            radius =
                dp(context, 21).toFloat()

            cardElevation =
                dp(
                    context,
                    if (clickable) 2 else 1
                ).toFloat()

            strokeWidth =
                dp(
                    context,
                    if (accentColorRes != null) {
                        1.3f
                    } else {
                        1f
                    }
                ).toInt()

            strokeColor =
                ContextCompat.getColor(
                    context,
                    accentColorRes
                        ?: R.color.audio_border
                )

            setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_surface
                )
            )

            isClickable =
                clickable

            isFocusable =
                clickable

            if (clickable) {
                rippleColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            when (accentColorRes) {
                                R.color.audio_green ->
                                    R.color.audio_green_soft
                                R.color.audio_red ->
                                    R.color.audio_red_soft
                                R.color.audio_yellow ->
                                    R.color.audio_yellow_soft
                                R.color.audio_purple ->
                                    R.color.audio_purple_soft
                                R.color.audio_orange ->
                                    R.color.audio_orange_soft
                                R.color.audio_teal ->
                                    R.color.audio_teal_soft
                                R.color.audio_pink ->
                                    R.color.audio_pink_soft
                                else ->
                                    R.color.audio_blue_soft
                            }
                        )
                    )

                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            view.animate()
                                .scaleX(0.985f)
                                .scaleY(0.985f)
                                .setDuration(70L)
                                .start()
                        }

                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(90L)
                                .start()
                        }
                    }
                    false
                }
            }
        }

    fun hero(
        context: Context
    ): MaterialCardView =
        MaterialCardView(context).apply {
            radius =
                dp(context, 26).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1)
            strokeColor =
                ContextCompat.getColor(
                    context,
                    R.color.audio_blue
                )
            setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_surface
                )
            )
        }

    fun button(
        context: Context,
        text: String,
        primary: Boolean = true,
        accentColorRes: Int = R.color.audio_blue
    ): MaterialButton =
        MaterialButton(context).apply {
            this.text = text
            minHeight = dp(context, 52)
            minimumHeight = dp(context, 52)
            minWidth = dp(context, 48)
            isAllCaps = false
            cornerRadius = dp(context, 16)
            insetTop = 0
            insetBottom = 0
            setPadding(
                dp(context, 15),
                0,
                dp(context, 15),
                0
            )
            textSize = 14f
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
            contentDescription = text

            val accent =
                ContextCompat.getColor(
                    context,
                    accentColorRes
                )

            if (primary) {
                backgroundTintList =
                    ColorStateList.valueOf(
                        accent
                    )
                setTextColor(Color.WHITE)
                strokeWidth = 0
            } else {
                backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.audio_surface
                        )
                    )
                setTextColor(accent)
                strokeWidth = dp(context, 1)
                strokeColor =
                    ColorStateList.valueOf(
                        accent
                    )
            }
        }

    fun setPillColor(
        view: TextView,
        context: Context,
        colorRes: Int
    ) {
        val softRes =
            when (colorRes) {
                R.color.audio_green ->
                    R.color.audio_green_soft
                R.color.audio_red ->
                    R.color.audio_red_soft
                R.color.audio_yellow ->
                    R.color.audio_yellow_soft
                R.color.audio_purple ->
                    R.color.audio_purple_soft
                R.color.audio_orange ->
                    R.color.audio_orange_soft
                R.color.audio_teal ->
                    R.color.audio_teal_soft
                R.color.audio_pink ->
                    R.color.audio_pink_soft
                else ->
                    R.color.audio_blue_soft
            }

        view.setTextColor(
            ContextCompat.getColor(
                context,
                colorRes
            )
        )

        view.background =
            GradientDrawable().apply {
                cornerRadius =
                    dp(context, 50).toFloat()
                setColor(
                    ContextCompat.getColor(
                        context,
                        softRes
                    )
                )
            }
    }

    fun iconButton(
        context: Context,
        symbol: String,
        description: String
    ): MaterialButton =
        MaterialButton(context).apply {
            text = symbol
            contentDescription = description
            minWidth = dp(context, 48)
            minHeight = dp(context, 48)
            cornerRadius = dp(context, 15)
            insetTop = 0
            insetBottom = 0
            backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.audio_surface
                    )
                )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_text
                )
            )
            strokeWidth = dp(context, 1)
            strokeColor =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.audio_border
                    )
                )
            textSize = 18f
        }

    fun pill(
        context: Context,
        text: String,
        positive: Boolean = false,
        colorRes: Int? = null
    ): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 10.5f
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )

            val accentRes =
                colorRes
                    ?: if (positive) {
                        R.color.audio_green
                    } else {
                        R.color.audio_blue
                    }

            val accent =
                ContextCompat.getColor(
                    context,
                    accentRes
                )

            val softRes =
                when (accentRes) {
                    R.color.audio_green ->
                        R.color.audio_green_soft
                    R.color.audio_red ->
                        R.color.audio_red_soft
                    R.color.audio_yellow ->
                        R.color.audio_yellow_soft
                    R.color.audio_purple ->
                        R.color.audio_purple_soft
                    R.color.audio_orange ->
                        R.color.audio_orange_soft
                    R.color.audio_teal ->
                        R.color.audio_teal_soft
                    R.color.audio_pink ->
                        R.color.audio_pink_soft
                    else ->
                        R.color.audio_blue_soft
                }

            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(
                dp(context, 10),
                dp(context, 7),
                dp(context, 10),
                dp(context, 7)
            )

            background =
                GradientDrawable().apply {
                    cornerRadius =
                        dp(context, 50).toFloat()
                    setColor(
                        ContextCompat.getColor(
                            context,
                            softRes
                        )
                    )
                }
        }

    fun stat(
        context: Context,
        value: String,
        label: String,
        accentColorRes: Int = R.color.audio_blue
    ): MaterialCardView {
        val card =
            card(
                context,
                accentColorRes =
                    accentColorRes
            )

        val content =
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(context, 12),
                    dp(context, 12),
                    dp(context, 12),
                    dp(context, 12)
                )
            }

        content.addView(
            TextView(context).apply {
                text = value
                textSize = 20f
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        accentColorRes
                    )
                )
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }
        )

        content.addView(
            spacer(context, 2)
        )

        content.addView(
            TextView(context).apply {
                text = label
                textSize = 10.5f
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.audio_muted
                    )
                )
            }
        )

        card.addView(content)
        return card
    }

    fun quickChip(
        context: Context,
        symbol: String,
        label: String,
        colorRes: Int,
        onClick: () -> Unit
    ): MaterialCardView {
        val card =
            card(
                context,
                clickable = true,
                accentColorRes = colorRes
            )

        card.addView(
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(context, 9),
                    dp(context, 10),
                    dp(context, 9),
                    dp(context, 10)
                )

                addView(
                    iconBadge(
                        context,
                        symbol,
                        colorRes
                    ).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                dp(context, 40),
                                dp(context, 40)
                            )
                    }
                )

                addView(
                    spacer(context, 5)
                )

                addView(
                    TextView(context).apply {
                        text = label
                        textSize = 10.5f
                        gravity = Gravity.CENTER
                        maxLines = 2
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.audio_text
                            )
                        )
                    }
                )
            }
        )

        card.setOnClickListener {
            onClick()
        }

        return card
    }

    fun menuTile(
        context: Context,
        symbol: String,
        label: String,
        description: String,
        colorRes: Int,
        onClick: () -> Unit
    ): MaterialCardView {
        val card =
            card(
                context,
                clickable = true,
                accentColorRes = colorRes
            )

        card.addView(
            LinearLayout(context).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(context, 13),
                    dp(context, 13),
                    dp(context, 13),
                    dp(context, 13)
                )

                addView(
                    iconBadge(
                        context,
                        symbol,
                        colorRes
                    ).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                dp(context, 40),
                                dp(context, 40)
                            )
                    }
                )

                addView(
                    spacer(context, 8)
                )

                addView(
                    title(
                        context,
                        label,
                        15f
                    )
                )

                addView(
                    spacer(context, 3)
                )

                addView(
                    subtitle(
                        context,
                        description
                    )
                )
            }
        )

        card.setOnClickListener {
            onClick()
        }

        return card
    }

    fun iconBadge(
        context: Context,
        text: String,
        colorRes: Int = R.color.audio_blue
    ): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    colorRes
                )
            )

            val soft =
                when (colorRes) {
                    R.color.audio_green ->
                        R.color.audio_green_soft
                    R.color.audio_red ->
                        R.color.audio_red_soft
                    R.color.audio_yellow ->
                        R.color.audio_yellow_soft
                    R.color.audio_purple ->
                        R.color.audio_purple_soft
                    R.color.audio_orange ->
                        R.color.audio_orange_soft
                    R.color.audio_teal ->
                        R.color.audio_teal_soft
                    R.color.audio_pink ->
                        R.color.audio_pink_soft
                    else ->
                        R.color.audio_blue_soft
                }

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE
                    cornerRadius =
                        dp(context, 16).toFloat()
                    setColor(
                        ContextCompat.getColor(
                            context,
                            soft
                        )
                    )
                }
        }

    fun divider(
        context: Context
    ): View =
        View(context).apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.audio_border
                )
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(context, 1)
                )
        }

    fun bottomNav(
        context: Context,
        current: String
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(context, 6),
                dp(context, 6),
                dp(context, 6),
                dp(context, 6)
            )

            background =
                GradientDrawable().apply {
                    cornerRadius =
                        dp(context, 22).toFloat()
                    setColor(
                        ContextCompat.getColor(
                            context,
                            R.color.audio_surface
                        )
                    )
                    setStroke(
                        dp(context, 1),
                        ContextCompat.getColor(
                            context,
                            R.color.audio_border
                        )
                    )
                }

            val items =
                listOf(
                    "home" to "⌂",
                    "tools" to "◫",
                    "favorites" to "★",
                    "plans" to "◇",
                    "profile" to "●"
                )

            items.forEachIndexed { index, pair ->
                val id = pair.first
                val symbol = pair.second

                val button =
                    iconButton(
                        context,
                        symbol,
                        when (id) {
                            "home" -> "Home"
                            "tools" -> "Tools"
                            "favorites" ->
                                "Favoritos"
                            "plans" -> "Planos"
                            else -> "Me"
                        }
                    ).apply {
                        val active =
                            id == current

                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (active) {
                                    R.color.audio_blue
                                } else {
                                    R.color.audio_text
                                }
                            )
                        )

                        if (active) {
                            backgroundTintList =
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.audio_blue_soft
                                    )
                                )

                            strokeColor =
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.audio_blue
                                    )
                                )
                        }

                        setOnClickListener {
                            when (id) {
                                "home" ->
                                    context.startActivity(
                                        Intent(
                                            context,
                                            MainActivity::class.java
                                        )
                                    )

                                "tools" ->
                                    AppPagesActivity.open(
                                        context,
                                        AppPagesActivity.PAGE_TOOLS
                                    )

                                "favorites" ->
                                    AppPagesActivity.open(
                                        context,
                                        AppPagesActivity.PAGE_FAVORITES
                                    )

                                "plans" ->
                                    context.startActivity(
                                        Intent(
                                            context,
                                            UpgradeActivity::class.java
                                        )
                                    )

                                "profile" ->
                                    AppPagesActivity.open(
                                        context,
                                        AppPagesActivity.PAGE_PROFILE
                                    )
                            }
                        }

                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                dp(context, 48),
                                1f
                            ).apply {
                                if (index <
                                    items.lastIndex
                                ) {
                                    rightMargin =
                                        dp(context, 3)
                                }
                            }
                    }

                addView(button)
            }
        }

    fun spacer(
        context: Context,
        height: Int
    ): View =
        View(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(context, height)
                )
        }
}
