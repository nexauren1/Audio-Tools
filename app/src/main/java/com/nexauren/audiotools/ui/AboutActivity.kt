package com.nexauren.audiotools.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ViewKit.page(this)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(ViewKit.button(this, "‹", false).apply {
            minWidth = ViewKit.dp(this@AboutActivity, 48)
            minHeight = ViewKit.dp(this@AboutActivity, 48)
            setOnClickListener { finish() }
        })
        header.addView(ViewKit.title(this, AppStrings.t(this, "about"), 23f).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@AboutActivity, 11)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 24))
        root.addView(ViewKit.pill(this, "AUDIO TOOLS", colorRes = R.color.audio_blue))
        root.addView(ViewKit.spacer(this, 10))
        root.addView(ViewKit.title(this, AppStrings.t(this, "about_head"), 28f))
        root.addView(ViewKit.spacer(this, 7))
        root.addView(ViewKit.subtitle(this, AppStrings.t(this, "about_body")))

        root.addView(ViewKit.spacer(this, 18))
        val card = ViewKit.card(this, accentColorRes = R.color.audio_green)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@AboutActivity, 15), ViewKit.dp(this@AboutActivity, 15), ViewKit.dp(this@AboutActivity, 15), ViewKit.dp(this@AboutActivity, 15))
        }
        content.addView(ViewKit.sectionLabel(this, "AUDIO TOOLS"))
        content.addView(ViewKit.spacer(this, 7))
        content.addView(ViewKit.title(this, AppStrings.t(this, "four_tools"), 18f))
        content.addView(ViewKit.spacer(this, 5))
        content.addView(ViewKit.subtitle(this, AppStrings.t(this, "workspace_desc")))
        content.addView(ViewKit.spacer(this, 8))
        content.addView(TextView(this).apply {
            text = AppStrings.t(this@AboutActivity, "version") + " " + BuildConfig.VERSION_NAME
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.audio_muted))
        })
        card.addView(content)
        root.addView(card)

        root.addView(ViewKit.spacer(this, 14))
        root.addView(ViewKit.button(this, AppStrings.t(this, "back_action"), false).apply { setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }
}
