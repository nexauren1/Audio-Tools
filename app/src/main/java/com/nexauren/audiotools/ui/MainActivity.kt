package com.nexauren.audiotools.ui

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.update.UpdateScheduler

class MainActivity : ComponentActivity() {

    private lateinit var scrollView: ScrollView
    private var heroPreview: SignalPreviewView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
        requestNotificationsIfNeeded()
        buildUi()
    }

    override fun onDestroy() {
        heroPreview?.stop()
        super.onDestroy()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            window.decorView.postDelayed({
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 700)
            }, 900)
        }
    }

    private fun buildUi() {
        val root = ViewKit.page(this)

        root.addView(topBar())
        root.addView(ViewKit.spacer(this, 18))
        root.addView(heroSection())
        root.addView(ViewKit.spacer(this, 20))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val headingCopy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headingCopy.addView(ViewKit.eyebrow(this, "WORKSPACE"))
        headingCopy.addView(ViewKit.title(this, "Escolhe o que precisas", 23f))
        headingCopy.addView(ViewKit.spacer(this, 3))
        headingCopy.addView(ViewKit.subtitle(this, "Três ferramentas focadas. Cada uma faz uma coisa e faz essa coisa bem."))
        heading.addView(headingCopy)
        heading.addView(ViewKit.pill(this, "03 ATIVAS", colorRes = R.color.audio_green))
        root.addView(heading)

        root.addView(ViewKit.spacer(this, 12))
        ToolCatalog.tools.forEach { tool ->
            root.addView(toolCard(tool))
            root.addView(ViewKit.spacer(this, 12))
        }

        root.addView(quickFlow())
        root.addView(ViewKit.spacer(this, 12))
        root.addView(localPrivacyCard())

        root.addView(ViewKit.spacer(this, 22))
        root.addView(TextView(this).apply {
            text = "AUDIO TOOLS  •  " + BuildConfig.VERSION_NAME
            textSize = 10.5f
            letterSpacing = 0.12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            gravity = Gravity.CENTER
        })

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(root)
        }
        setContentView(scrollView)
    }

    private fun topBar(): ViewGroup {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "AT"
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ViewKit.dp(this@MainActivity, 14).toFloat()
                setColor(ContextCompat.getColor(this@MainActivity, R.color.audio_blue))
            }
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 44), ViewKit.dp(this@MainActivity, 44)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 11)
            }
        }
        header.addView(logo)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        brand.addView(ViewKit.eyebrow(this, "NEXAUREN"))
        brand.addView(TextView(this).apply {
            text = "AUDIO TOOLS"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        header.addView(brand)

        header.addView(ViewKit.iconButton(this, "☰", "Menu").apply {
            setOnClickListener { showMenu() }
        })
        header.addView(ViewKit.spacer(this, 2))
        header.addView(ViewKit.iconButton(this, "⚙", "Definições").apply {
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, SettingsActivity::class.java))
            }
        })
        return header
    }

    private fun heroSection(): MaterialCardView {
        val card = ViewKit.hero(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 18)
            )
        }

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusRow.addView(ViewKit.pill(this, "AUDIO WORKSPACE", colorRes = R.color.audio_blue))
        statusRow.addView(ViewKit.spacer(this, 1).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        statusRow.addView(ViewKit.pill(this, "LOCAL", positive = true))
        content.addView(statusRow)

        heroPreview = SignalPreviewView(this, R.color.audio_blue, live = false).also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewKit.dp(this, 112)
            ).apply {
                topMargin = ViewKit.dp(this@MainActivity, 14)
                bottomMargin = ViewKit.dp(this@MainActivity, 14)
            }
            it.start()
        }
        content.addView(heroPreview)

        content.addView(ViewKit.title(this, "O teu áudio,\nno teu controlo.", 28f))
        content.addView(ViewKit.spacer(this, 8))
        content.addView(ViewKit.subtitle(this, "Corta, grava e analisa diretamente no telemóvel, com feedback visual em cada etapa."))

        content.addView(ViewKit.spacer(this, 14))
        val colorLegend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        colorLegend.addView(colorDot(R.color.audio_blue))
        colorLegend.addView(legendText("Editar"))
        colorLegend.addView(ViewKit.spacer(this, 10))
        colorLegend.addView(colorDot(R.color.audio_red))
        colorLegend.addView(legendText("Gravar"))
        colorLegend.addView(ViewKit.spacer(this, 10))
        colorLegend.addView(colorDot(R.color.audio_yellow))
        colorLegend.addView(legendText("Analisar"))
        colorLegend.addView(ViewKit.spacer(this, 10))
        colorLegend.addView(colorDot(R.color.audio_green))
        colorLegend.addView(legendText("Pronto"))
        content.addView(colorLegend)

        content.addView(ViewKit.spacer(this, 14))
        content.addView(ViewKit.button(this, "Abrir ferramentas", true, R.color.audio_blue).apply {
            setOnClickListener { scrollToTools() }
        })

        card.addView(content)
        return card
    }

    private fun legendText(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        }

    private fun colorDot(colorRes: Int): View =
        View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, colorRes))
            }
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 8), ViewKit.dp(this@MainActivity, 8)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 4)
            }
        }

    private fun toolCard(tool: AudioTool): MaterialCardView {
        val colorRes = toolAccent(tool.id)
        val card = ViewKit.card(this, clickable = true, accentColorRes = colorRes)
        card.setOnClickListener {
            startActivity(ToolDetailActivity.intent(this, tool.id))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 14),
                ViewKit.dp(this@MainActivity, 14),
                ViewKit.dp(this@MainActivity, 14),
                ViewKit.dp(this@MainActivity, 14)
            )
        }

        val preview = SignalPreviewView(this, colorRes, live = false).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewKit.dp(this@MainActivity, 76)
            )
            alpha = 0.94f
        }
        content.addView(preview)
        content.addView(ViewKit.spacer(this, 13))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(ViewKit.iconBadge(this, toolSymbol(tool.id), colorRes).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 46), ViewKit.dp(this@MainActivity, 46)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 11)
            }
        })

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        copy.addView(TextView(this).apply {
            text = tool.title
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        copy.addView(ViewKit.spacer(this, 4))
        copy.addView(TextView(this).apply {
            text = tool.description
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            setLineSpacing(1.08f, 1f)
        })
        header.addView(copy)
        content.addView(header)

        content.addView(ViewKit.spacer(this, 12))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        footer.addView(ViewKit.pill(this, tool.category, colorRes = colorRes))
        footer.addView(ViewKit.spacer(this, 1).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        footer.addView(ViewKit.button(this, "Abrir  ›", false, colorRes).apply {
            minHeight = ViewKit.dp(this@MainActivity, 44)
            minWidth = 0
            setOnClickListener { startActivity(ToolDetailActivity.intent(this@MainActivity, tool.id)) }
        })
        content.addView(footer)

        card.addView(content)
        return card
    }

    private fun quickFlow(): View {
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 16)
            )
        }
        content.addView(ViewKit.eyebrow(this, "FLUXO RÁPIDO"))
        content.addView(ViewKit.title(this, "Escolher  →  trabalhar  →  guardar", 18f))
        content.addView(ViewKit.spacer(this, 12))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val items = listOf(
            Triple("01", "Abrir", R.color.audio_blue),
            Triple("02", "Processar", R.color.audio_yellow),
            Triple("03", "Guardar", R.color.audio_green)
        )
        items.forEachIndexed { index, item ->
            val step = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            step.addView(ViewKit.iconBadge(this, item.first, item.third).apply {
                layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 38), ViewKit.dp(this@MainActivity, 38))
            })
            step.addView(ViewKit.spacer(this, 6))
            step.addView(TextView(this).apply {
                text = item.second
                textSize = 10.5f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
                gravity = Gravity.CENTER
            })
            row.addView(step)
            if (index < items.lastIndex) {
                row.addView(TextView(this).apply {
                    text = "→"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_border))
                })
            }
        }
        content.addView(row)
        card.addView(content)
        return card
    }

    private fun localPrivacyCard(): MaterialCardView {
        val card = ViewKit.card(this, accentColorRes = R.color.audio_green)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 15),
                ViewKit.dp(this@MainActivity, 14),
                ViewKit.dp(this@MainActivity, 15),
                ViewKit.dp(this@MainActivity, 14)
            )
        }
        row.addView(ViewKit.iconBadge(this, "✓", R.color.audio_green).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 43), ViewKit.dp(this@MainActivity, 43)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 11)
            }
        })
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        copy.addView(TextView(this).apply {
            text = "Processamento local"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        copy.addView(ViewKit.spacer(this, 3))
        copy.addView(TextView(this).apply {
            text = "Os ficheiros são trabalhados no dispositivo nesta versão."
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        })
        row.addView(copy)
        card.addView(row)
        return card
    }

    private fun toolAccent(toolId: String): Int = when (toolId) {
        "cut" -> R.color.audio_blue
        "recorder" -> R.color.audio_red
        "analyzer" -> R.color.audio_yellow
        else -> R.color.audio_blue
    }

    private fun toolSymbol(toolId: String): String = when (toolId) {
        "cut" -> "✂"
        "recorder" -> "●"
        "analyzer" -> "⌁"
        else -> "•"
    }

    private fun scrollToTools() {
        scrollView.post { scrollView.smoothScrollTo(0, ViewKit.dp(this, 455)) }
    }

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setTitle("AUDIO TOOLS")
            .setItems(arrayOf("Ferramentas", "Atualizações", "Sobre", "Definições")) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> scrollToTools()
                    1, 3 -> startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    2 -> startActivity(android.content.Intent(this, AboutActivity::class.java))
                }
            }
            .show()
    }
}
