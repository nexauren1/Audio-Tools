package com.nexauren.audiotools.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
        requestNotificationsIfNeeded()
        buildUi()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            window.decorView.postDelayed({
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 700)
            }, 900)
        }
    }

    private fun buildUi() {
        val root = ViewKit.page(this)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        brand.addView(ViewKit.eyebrow(this, "AUDIO TOOLS"))
        brand.addView(ViewKit.title(this, "O teu áudio.", 29f))
        header.addView(brand)
        header.addView(iconButton("☰", "Menu") { showMenu() })
        header.addView(ViewKit.spacer(this, 1))
        header.addView(iconButton("⚙", "Definições") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 18))

        val hero = ViewKit.hero(this)
        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 21),
                ViewKit.dp(this@MainActivity, 22),
                ViewKit.dp(this@MainActivity, 21),
                ViewKit.dp(this@MainActivity, 22)
            )
        }
        heroContent.addView(ViewKit.pill(this, "V1 • 3 ferramentas funcionais", positive = true))
        heroContent.addView(ViewKit.spacer(this, 12))
        heroContent.addView(ViewKit.title(this, "Menos ferramentas.\nMais qualidade.", 25f))
        heroContent.addView(ViewKit.spacer(this, 8))
        heroContent.addView(ViewKit.subtitle(this, "Uma caixa de ferramentas simples, bonita e útil. O processamento acontece no próprio dispositivo."))
        heroContent.addView(ViewKit.spacer(this, 16))
        heroContent.addView(ViewKit.button(this, "Ver as ferramentas", true).apply {
            setOnClickListener { scrollToTools() }
        })
        hero.addView(heroContent)
        root.addView(hero)

        root.addView(ViewKit.spacer(this, 14))
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        stats.addView(ViewKit.stat(this, "03", "ferramentas"), weightParams())
        stats.addView(ViewKit.spacer(this, 8))
        stats.addView(ViewKit.stat(this, "100%", "local"), weightParams())
        stats.addView(ViewKit.spacer(this, 8))
        stats.addView(ViewKit.stat(this, "V1", "base"), weightParams())
        root.addView(stats)

        root.addView(ViewKit.spacer(this, 26))
        root.addView(ViewKit.sectionLabel(this, "Ferramentas"))
        root.addView(ViewKit.spacer(this, 9))

        ToolCatalog.tools.forEach { tool ->
            root.addView(toolCard(tool))
            root.addView(ViewKit.spacer(this, 11))
        }

        val note = ViewKit.card(this)
        val noteContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 15),
                ViewKit.dp(this@MainActivity, 16),
                ViewKit.dp(this@MainActivity, 15)
            )
        }
        noteContent.addView(ViewKit.iconBadge(this, "✓").apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 44), ViewKit.dp(this@MainActivity, 44)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 12)
            }
        })
        val noteText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        noteText.addView(TextView(this).apply {
            text = "Base limpa e pronta para crescer"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        noteText.addView(ViewKit.spacer(this, 3))
        noteText.addView(TextView(this).apply {
            text = "Novas ferramentas podem ser adicionadas sem alterar esta estrutura."
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        })
        noteContent.addView(noteText)
        note.addView(noteContent)
        root.addView(note)

        root.addView(ViewKit.spacer(this, 18))
        root.addView(TextView(this).apply {
            text = "Audio Tools " + BuildConfig.VERSION_NAME + " • feito para evoluir"
            textSize = 12f
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

    private fun iconButton(symbol: String, description: String, action: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = symbol
            contentDescription = description
            minWidth = ViewKit.dp(this@MainActivity, 50)
            minHeight = ViewKit.dp(this@MainActivity, 50)
            cornerRadius = ViewKit.dp(this@MainActivity, 16)
            insetTop = 0
            insetBottom = 0
            setBackgroundColor(Color.WHITE)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
            setOnClickListener { action() }
        }

    private fun weightParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun scrollToTools() {
        scrollView.post { scrollView.smoothScrollTo(0, ViewKit.dp(this, 390)) }
    }

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setTitle("Audio Tools")
            .setItems(arrayOf("Ferramentas", "Atualizações", "Sobre", "Definições")) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> scrollToTools()
                    1, 3 -> startActivity(Intent(this, SettingsActivity::class.java))
                    2 -> startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            .show()
    }

    private fun toolCard(tool: AudioTool): MaterialCardView {
        val card = ViewKit.card(this, clickable = true)
        card.setOnClickListener {
            startActivity(ToolDetailActivity.intent(this, tool.id))
        }

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

        val badgeText = when (tool.id) {
            "cut" -> "✂"
            "recorder" -> "●"
            else -> "⌁"
        }
        row.addView(ViewKit.iconBadge(this, badgeText).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewKit.dp(this@MainActivity, 48),
                ViewKit.dp(this@MainActivity, 48)
            ).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 13)
            }
        })

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textBox.addView(TextView(this).apply {
            text = tool.title
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        textBox.addView(ViewKit.spacer(this, 4))
        textBox.addView(TextView(this).apply {
            text = tool.description
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        row.addView(textBox)
        row.addView(TextView(this).apply {
            text = "›"
            textSize = 25f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        })
        card.addView(row)
        return card
    }
}