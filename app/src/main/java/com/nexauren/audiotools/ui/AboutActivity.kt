package com.nexauren.audiotools.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ViewKit.page(this)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(MaterialButton(this).apply {
            text = "‹"
            minWidth = ViewKit.dp(this@AboutActivity, 48)
            minHeight = ViewKit.dp(this@AboutActivity, 48)
            cornerRadius = ViewKit.dp(this@AboutActivity, 15)
            setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.audio_text))
            setBackgroundColor(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
        })
        header.addView(ViewKit.title(this, "Sobre", 24f).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@AboutActivity, 12)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 26))
        root.addView(ViewKit.pill(this, "AUDIO TOOLS"))
        root.addView(ViewKit.spacer(this, 12))
        root.addView(ViewKit.title(this, "Uma caixa de ferramentas para áudio", 28f))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(ViewKit.subtitle(this, "A arquitetura separa catálogo, interface e serviços. Cada ferramenta pode receber o seu próprio motor sem reestruturar a aplicação."))

        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.card(this).apply {
            addView(LinearLayout(this@AboutActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(this@AboutActivity, 18),
                    ViewKit.dp(this@AboutActivity, 18),
                    ViewKit.dp(this@AboutActivity, 18),
                    ViewKit.dp(this@AboutActivity, 18)
                )
                addView(ViewKit.title(this@AboutActivity, "Base atual", 17f))
                addView(ViewKit.spacer(this@AboutActivity, 8))
                addView(ViewKit.subtitle(this@AboutActivity, "14 ferramentas catalogadas, páginas individuais, seletor de ficheiros, definições e verificação de atualizações por release."))
                addView(ViewKit.spacer(this@AboutActivity, 10))
                addView(TextView(this@AboutActivity).apply {
                    text = "Versão " + BuildConfig.VERSION_NAME
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.audio_muted))
                })
            })
        })

        root.addView(ViewKit.spacer(this, 18))
        root.addView(ViewKit.button(this, "Voltar", false).apply { setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}