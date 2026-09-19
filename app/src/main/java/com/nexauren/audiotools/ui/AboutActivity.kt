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

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(ViewKit.button(this, "‹", false).apply {
            minWidth = ViewKit.dp(this@AboutActivity, 50)
            minHeight = ViewKit.dp(this@AboutActivity, 50)
            setOnClickListener { finish() }
        })
        header.addView(ViewKit.title(this, "Sobre", 24f).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@AboutActivity, 12)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 28))
        root.addView(ViewKit.pill(this, "AUDIO TOOLS"))
        root.addView(ViewKit.spacer(this, 13))
        root.addView(ViewKit.title(this, "Feito para ser simples.", 29f))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(ViewKit.subtitle(this, "O Audio Tools começa pequeno de propósito: três ferramentas bem definidas, uma interface limpa e uma base preparada para crescer."))

        root.addView(ViewKit.spacer(this, 22))
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@AboutActivity, 18),
                ViewKit.dp(this@AboutActivity, 18),
                ViewKit.dp(this@AboutActivity, 18),
                ViewKit.dp(this@AboutActivity, 18)
            )
        }
        content.addView(ViewKit.sectionLabel(this@AboutActivity, "V1"))
        content.addView(ViewKit.spacer(this@AboutActivity, 10))
        content.addView(ViewKit.title(this@AboutActivity, "3 ferramentas funcionais", 19f))
        content.addView(ViewKit.spacer(this@AboutActivity, 7))
        content.addView(ViewKit.subtitle(this@AboutActivity, "Cortar áudio, gravar e analisar. O processamento é feito localmente, sem precisar enviar o ficheiro para um serviço externo."))
        content.addView(ViewKit.spacer(this@AboutActivity, 12))
        content.addView(TextView(this@AboutActivity).apply {
            text = "Versão " + BuildConfig.VERSION_NAME
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.audio_muted))
        })
        card.addView(content)
        root.addView(card)

        root.addView(ViewKit.spacer(this, 18))
        root.addView(ViewKit.button(this, "Voltar", false).apply { setOnClickListener { finish() } })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }
}