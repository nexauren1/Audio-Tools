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
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.update.UpdateScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
        requestNotificationsIfNeeded()
        buildUi()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        brand.addView(TextView(this).apply {
            text = "AUDIO TOOLS"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        })
        brand.addView(ViewKit.title(this, "O teu áudio.", 28f))
        header.addView(brand)

        header.addView(MaterialButton(this).apply {
            text = "☰"
            contentDescription = "Menu"
            minWidth = ViewKit.dp(this@MainActivity, 52)
            minHeight = ViewKit.dp(this@MainActivity, 52)
            cornerRadius = ViewKit.dp(this@MainActivity, 16)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
            setBackgroundColor(Color.WHITE)
            setOnClickListener { showMenu() }
        })
        header.addView(MaterialButton(this).apply {
            text = "⚙"
            contentDescription = "Definições"
            minWidth = ViewKit.dp(this@MainActivity, 52)
            minHeight = ViewKit.dp(this@MainActivity, 52)
            cornerRadius = ViewKit.dp(this@MainActivity, 16)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
            setBackgroundColor(Color.WHITE)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 14))
        val hero = ViewKit.card(this)
        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@MainActivity, 20), ViewKit.dp(this@MainActivity, 20),
                ViewKit.dp(this@MainActivity, 20), ViewKit.dp(this@MainActivity, 20))
        }
        heroContent.addView(ViewKit.pill(this, "14 ferramentas • base V1"))
        heroContent.addView(ViewKit.spacer(this, 12))
        heroContent.addView(ViewKit.title(this, "Tudo para trabalhar com áudio", 23f))
        heroContent.addView(ViewKit.spacer(this, 6))
        heroContent.addView(ViewKit.subtitle(this, "Corta, junta, converte, grava, edita, analisa e prepara o teu áudio. Cada ferramenta tem a sua própria área."))
        hero.addView(heroContent)
        root.addView(hero)

        root.addView(ViewKit.spacer(this, 22))
        root.addView(TextView(this).apply {
            text = "FERRAMENTAS"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        })
        root.addView(ViewKit.spacer(this, 8))

        ToolCatalog.tools.forEach { tool ->
            root.addView(toolCard(tool))
            root.addView(ViewKit.spacer(this, 10))
        }

        root.addView(TextView(this).apply {
            text = "Audio Tools " + BuildConfig.VERSION_NAME + " • preparado para crescer"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            gravity = Gravity.CENTER
        })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun showMenu() {
        val items = arrayOf("Ferramentas", "Atualizações", "Sobre o Audio Tools", "Definições")
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> window.decorView.post { }
                    1 -> startActivity(Intent(this, SettingsActivity::class.java))
                    2 -> startActivity(Intent(this, AboutActivity::class.java))
                    3 -> startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            .show()
    }

    private fun toolCard(tool: AudioTool): ViewGroup {
        val card = ViewKit.card(this, clickable = true)
        card.setOnClickListener {
            startActivity(ToolDetailActivity.intent(this, tool.id))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ViewKit.dp(this@MainActivity, 16), ViewKit.dp(this@MainActivity, 14),
                ViewKit.dp(this@MainActivity, 16), ViewKit.dp(this@MainActivity, 14))
        }

        row.addView(TextView(this).apply {
            text = tool.number
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_primary))
            setBackgroundColor(Color.parseColor("#E9EFFF"))
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 48),
                ViewKit.dp(this@MainActivity, 48)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 14)
            }
        })

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = tool.title
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        texts.addView(ViewKit.spacer(this@MainActivity, 4))
        texts.addView(TextView(this).apply {
            text = tool.description
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        row.addView(texts)
        row.addView(TextView(this).apply {
            text = "›"
            textSize = 26f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        })
        card.addView(row)
        return card
    }
}