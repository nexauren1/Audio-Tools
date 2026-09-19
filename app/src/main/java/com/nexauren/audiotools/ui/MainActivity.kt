package com.nexauren.audiotools.ui

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.update.UpdateScheduler

class MainActivity : ComponentActivity() {
    private lateinit var scrollView: ScrollView
    private lateinit var toolGrid: LinearLayout
    private var searchInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
        requestNotificationsIfNeeded()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::toolGrid.isInitialized) renderTools(searchInput?.text?.toString().orEmpty())
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
        root.addView(ViewKit.spacer(this, 16))

        val hero = ViewKit.hero(this)
        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@MainActivity, 17), ViewKit.dp(this@MainActivity, 17), ViewKit.dp(this@MainActivity, 17), ViewKit.dp(this@MainActivity, 18))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(ViewKit.pill(this, "NEXAUREN AUDIO", colorRes = R.color.audio_blue))
        row.addView(ViewKit.spacer(this, 1).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        row.addView(ViewKit.pill(this, BuildConfig.VERSION_NAME, colorRes = R.color.audio_green))
        heroContent.addView(row)
        heroContent.addView(ViewKit.spacer(this, 12))
        heroContent.addView(ViewKit.title(this, "O teu áudio,\nno teu controlo.", 27f))
        heroContent.addView(ViewKit.spacer(this, 7))
        heroContent.addView(ViewKit.subtitle(this, AppStrings.t(this, "workspace_desc")))
        heroContent.addView(ViewKit.spacer(this, 12))

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        status.addView(metric("5", AppStrings.t(this, "active"), R.color.audio_blue))
        status.addView(ViewKit.spacer(this, 12))
        status.addView(metric("100%", AppStrings.t(this, "local_processing"), R.color.audio_green))
        heroContent.addView(status)
        hero.addView(heroContent)
        root.addView(hero)

        root.addView(ViewKit.spacer(this, 20))
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        heading.addView(ViewKit.eyebrow(this, AppStrings.t(this, "workspace")))
        heading.addView(ViewKit.title(this, AppStrings.t(this, "choose"), 22f))
        heading.addView(ViewKit.spacer(this, 4))
        heading.addView(ViewKit.subtitle(this, AppStrings.t(this, "workspace_desc")))
        root.addView(heading)
        root.addView(ViewKit.spacer(this, 12))

        searchInput = EditText(this).apply {
            hint = if (LanguageManager.get(this@MainActivity) == "pt") "Pesquisar ferramentas…" else "Search tools…"
            isSingleLine = true
            textSize = 14f
            setPadding(ViewKit.dp(this@MainActivity, 14), ViewKit.dp(this@MainActivity, 12), ViewKit.dp(this@MainActivity, 14), ViewKit.dp(this@MainActivity, 12))
            background = GradientDrawable().apply {
                cornerRadius = ViewKit.dp(this@MainActivity, 15).toFloat()
                setColor(ContextCompat.getColor(this@MainActivity, R.color.audio_surface))
                setStroke(ViewKit.dp(this@MainActivity, 1), ContextCompat.getColor(this@MainActivity, R.color.audio_border))
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderTools(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        root.addView(searchInput)

        root.addView(ViewKit.spacer(this, 12))
        toolGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(toolGrid)

        root.addView(ViewKit.spacer(this, 16))
        root.addView(quickFlow())
        root.addView(ViewKit.spacer(this, 12))
        root.addView(localPrivacyCard())

        root.addView(ViewKit.spacer(this, 20))
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
        renderTools()
    }

    private fun topBar(): ViewGroup {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ViewKit.iconBadge(this, "AT", R.color.audio_blue).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 44), ViewKit.dp(this@MainActivity, 44)).apply {
                rightMargin = ViewKit.dp(this@MainActivity, 11)
            }
        })
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
        header.addView(ViewKit.iconButton(this, "☰", AppStrings.t(this, "menu")).apply { setOnClickListener { showMenu() } })
        header.addView(ViewKit.spacer(this, 2))
        header.addView(ViewKit.iconButton(this, "⚙", AppStrings.t(this, "settings")).apply {
            setOnClickListener { startActivity(android.content.Intent(this@MainActivity, SettingsActivity::class.java)) }
        })
        return header
    }

    private fun metric(value: String, label: String, colorRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))
            })
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 10.5f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            })
        }

    private fun renderTools(query: String = "") {
        if (!::toolGrid.isInitialized) return
        toolGrid.removeAllViews()
        val q = query.trim().lowercase()
        val tools = ToolCatalog.tools.mapNotNull { tool ->
            val copy = AppStrings.tool(this, tool.id)
            if (q.isBlank() || (copy.title + " " + copy.description + " " + copy.category).lowercase().contains(q)) {
                tool
            } else null
        }

        if (tools.isEmpty()) {
            toolGrid.addView(ViewKit.card(this).apply {
                addView(TextView(this@MainActivity).apply {
                    text = "—"
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
                    setPadding(ViewKit.dp(this@MainActivity, 20), ViewKit.dp(this@MainActivity, 20), ViewKit.dp(this@MainActivity, 20), ViewKit.dp(this@MainActivity, 20))
                })
            })
            return
        }

        tools.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            pair.forEachIndexed { index, tool ->
                row.addView(compactToolCard(tool), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index == 0) rightMargin = ViewKit.dp(this@MainActivity, 8)
                })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { leftMargin = ViewKit.dp(this@MainActivity, 8) })
            toolGrid.addView(row)
            toolGrid.addView(ViewKit.spacer(this, 8))
        }
    }

    private fun compactToolCard(tool: AudioTool): MaterialCardView {
        val accent = toolAccent(tool.id)
        val copy = AppStrings.tool(this, tool.id)
        val card = ViewKit.card(this, clickable = true, accentColorRes = accent).apply {
            setOnClickListener { startActivity(ToolDetailActivity.intent(this@MainActivity, tool.id)) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@MainActivity, 12), ViewKit.dp(this@MainActivity, 12), ViewKit.dp(this@MainActivity, 12), ViewKit.dp(this@MainActivity, 13))
        }
        val iconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        iconRow.addView(ViewKit.iconBadge(this, toolSymbol(tool.id), accent).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 40), ViewKit.dp(this@MainActivity, 40)).apply { rightMargin = ViewKit.dp(this@MainActivity, 8) }
        })
        iconRow.addView(TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, accent))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        content.addView(iconRow)
        content.addView(ViewKit.spacer(this, 9))
        content.addView(TextView(this).apply {
            text = copy.title
            textSize = 15f
            maxLines = 2
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        content.addView(ViewKit.spacer(this, 4))
        content.addView(TextView(this).apply {
            text = copy.description
            textSize = 11f
            maxLines = 3
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            setLineSpacing(1.05f, 1f)
        })
        content.addView(ViewKit.spacer(this, 8))
        content.addView(ViewKit.pill(this, copy.category, colorRes = accent))
        card.addView(content)
        return card
    }

    private fun quickFlow(): View {
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@MainActivity, 15), ViewKit.dp(this@MainActivity, 15), ViewKit.dp(this@MainActivity, 15), ViewKit.dp(this@MainActivity, 15))
        }
        content.addView(ViewKit.eyebrow(this, AppStrings.t(this, "quick_flow")))
        content.addView(ViewKit.title(this, AppStrings.t(this, "flow_title"), 17f))
        content.addView(ViewKit.spacer(this, 10))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val items = listOf(
            Triple("01", AppStrings.t(this, "open"), R.color.audio_blue),
            Triple("02", AppStrings.t(this, "process"), R.color.audio_yellow),
            Triple("03", AppStrings.t(this, "save"), R.color.audio_green)
        )
        items.forEachIndexed { index, item ->
            val step = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            step.addView(ViewKit.iconBadge(this, item.first, item.third).apply {
                layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 36), ViewKit.dp(this@MainActivity, 36))
            })
            step.addView(ViewKit.spacer(this, 5))
            step.addView(TextView(this).apply {
                text = item.second
                textSize = 10.5f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
            })
            row.addView(step)
            if (index < items.lastIndex) row.addView(TextView(this).apply {
                text = "→"
                textSize = 17f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_border))
            })
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
            setPadding(ViewKit.dp(this@MainActivity, 14), ViewKit.dp(this@MainActivity, 12), ViewKit.dp(this@MainActivity, 14), ViewKit.dp(this@MainActivity, 12))
        }
        row.addView(ViewKit.iconBadge(this, "✓", R.color.audio_green).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@MainActivity, 40), ViewKit.dp(this@MainActivity, 40)).apply { rightMargin = ViewKit.dp(this@MainActivity, 10) }
        })
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        copy.addView(TextView(this).apply {
            text = AppStrings.t(this@MainActivity, "local_processing")
            textSize = 13.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_text))
        })
        copy.addView(TextView(this).apply {
            text = AppStrings.t(this@MainActivity, "local_desc")
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.audio_muted))
        })
        row.addView(copy)
        card.addView(row)
        return card
    }

    private fun toolAccent(toolId: String): Int = when (toolId) {
        "cut" -> R.color.audio_blue
        "convert" -> R.color.audio_purple
        "extract" -> R.color.audio_green
        "recorder" -> R.color.audio_red
        "analyzer" -> R.color.audio_yellow
        else -> R.color.audio_blue
    }

    private fun toolSymbol(toolId: String): String = when (toolId) {
        "cut" -> "✂"
        "convert" -> "⇄"
        "extract" -> "↥"
        "recorder" -> "●"
        "analyzer" -> "⌁"
        else -> "•"
    }

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setTitle("AUDIO TOOLS")
            .setItems(arrayOf(
                AppStrings.t(this, "tools"),
                AppStrings.t(this, "updates"),
                AppStrings.t(this, "about"),
                AppStrings.t(this, "settings")
            )) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> scrollView.smoothScrollTo(0, ViewKit.dp(this, 360))
                    1, 3 -> startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    2 -> startActivity(android.content.Intent(this, AboutActivity::class.java))
                }
            }
            .show()
    }
\n}
