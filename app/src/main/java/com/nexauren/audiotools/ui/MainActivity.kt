package com.nexauren.audiotools.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.payments.PaymentClient
import com.nexauren.audiotools.update.UpdateScheduler
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var toolGrid: LinearLayout
    private var searchInput: EditText? = null
    private var planView: TextView? = null

    private val executor =
        Executors.newSingleThreadExecutor()

    private val handler =
        Handler(Looper.getMainLooper())

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        if (
            FirebaseAuth
                .getInstance()
                .currentUser == null
        ) {
            openAuth()
            return
        }

        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
        requestNotificationsIfNeeded()
        buildUi()
        refreshPlan()
    }

    override fun onResume() {
        super.onResume()

        if (::toolGrid.isInitialized) {
            renderTools(
                searchInput
                    ?.text
                    ?.toString()
                    .orEmpty()
            )
            refreshPlan()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = ViewKit.page(this)

        root.addView(topBar())
        root.addView(ViewKit.spacer(this, 12))

        val hero = ViewKit.hero(this)

        val heroContent =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18)
                )
            }

        val heroTop =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        heroTop.addView(
            ViewKit.pill(
                this,
                "AUDIO WORKSPACE",
                colorRes = R.color.audio_blue
            )
        )

        heroTop.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(0, 1, 1f)
            }
        )

        planView =
            ViewKit.pill(
                this,
                "FREE",
                colorRes = R.color.audio_green
            )

        heroTop.addView(planView)
        heroContent.addView(heroTop)

        heroContent.addView(ViewKit.spacer(this, 13))

        val firstName =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.displayName
                ?.trim()
                ?.split(" ")
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }

        heroContent.addView(
            ViewKit.title(
                this,
                if (firstName != null) {
                    "Olá, " + firstName + "."
                } else {
                    "Olá."
                },
                18f
            )
        )

        heroContent.addView(ViewKit.spacer(this, 3))

        heroContent.addView(
            ViewKit.title(
                this,
                "O teu áudio,\nno teu controlo.",
                30f
            )
        )

        heroContent.addView(ViewKit.spacer(this, 7))

        heroContent.addView(
            ViewKit.subtitle(
                this,
                "Uma área simples para editar, converter, extrair, gravar e analisar áudio."
            )
        )

        heroContent.addView(ViewKit.spacer(this, 14))

        val quick =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        quick.addView(
            ViewKit.button(
                this,
                "⚡ Abrir ferramentas",
                true,
                R.color.audio_blue
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                setOnClickListener {
                    AppPagesActivity.open(
                        this@MainActivity,
                        AppPagesActivity.PAGE_TOOLS
                    )
                }
            }
        )

        quick.addView(
            ViewKit.iconButton(
                this,
                "⌕",
                "Pesquisar ferramentas"
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(this@MainActivity, 52),
                        ViewKit.dp(this@MainActivity, 52)
                    ).apply {
                        leftMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                8
                            )
                    }

                setOnClickListener {
                    searchInput?.requestFocus()

                    (
                        getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as? InputMethodManager
                    )?.showSoftInput(
                        searchInput,
                        InputMethodManager.SHOW_IMPLICIT
                    )
                }
            }
        )

        heroContent.addView(quick)
        hero.addView(heroContent)
        root.addView(hero)

        root.addView(ViewKit.spacer(this, 18))
        root.addView(sectionHeader("Ferramentas"))
        root.addView(ViewKit.spacer(this, 8))

        searchInput =
            EditText(this).apply {
                hint = "Pesquisar ferramenta…"
                isSingleLine = true
                textSize = 14f
                minHeight =
                    ViewKit.dp(
                        this@MainActivity,
                        52
                    )

                setPadding(
                    ViewKit.dp(this@MainActivity, 15),
                    0,
                    ViewKit.dp(this@MainActivity, 15),
                    0
                )

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_text
                    )
                )

                setHintTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )

                background =
                    GradientDrawable().apply {
                        cornerRadius =
                            ViewKit.dp(
                                this@MainActivity,
                                17
                            ).toFloat()

                        setColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.audio_surface
                            )
                        )

                        setStroke(
                            ViewKit.dp(
                                this@MainActivity,
                                1
                            ),
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.audio_border
                            )
                        )
                    }

                addTextChangedListener(
                    object :
                        android.text.TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            renderTools(
                                s?.toString()
                                    .orEmpty()
                            )
                        }

                        override fun afterTextChanged(
                            s: android.text.Editable?
                        ) = Unit
                    }
                )
            }

        root.addView(searchInput)
        root.addView(ViewKit.spacer(this, 12))

        toolGrid =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        root.addView(toolGrid)

        root.addView(ViewKit.spacer(this, 16))
        root.addView(sectionHeader("Recentes"))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(recentSection())

        root.addView(ViewKit.spacer(this, 16))
        root.addView(sectionHeader("Favoritos"))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(favoriteSection())

        root.addView(ViewKit.spacer(this, 16))
        root.addView(proStatusCard())

        root.addView(ViewKit.spacer(this, 12))
        root.addView(localCard())

        root.addView(ViewKit.spacer(this, 20))
        root.addView(ViewKit.bottomNav(this, "home"))

        root.addView(ViewKit.spacer(this, 12))
        root.addView(
            TextView(this).apply {
                text =
                    "Audio Tools  •  " +
                        BuildConfig.VERSION_NAME
                textSize = 10.5f
                gravity = Gravity.CENTER
                letterSpacing = 0.1f
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
                addView(root)
            }
        )

        renderTools()
    }

    private fun topBar(): ViewGroup {
        val user =
            FirebaseAuth
                .getInstance()
                .currentUser

        val displayName =
            user
                ?.displayName
                ?.trim()
                .orEmpty()

        val initials =
            displayName
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") {
                    it.take(1).uppercase()
                }
                .ifBlank { "AT" }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            ViewKit.iconBadge(
                this,
                initials,
                R.color.audio_blue
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(this@MainActivity, 46),
                        ViewKit.dp(this@MainActivity, 46)
                    ).apply {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                10
                            )
                    }

                contentDescription =
                    "Abrir perfil"

                setOnClickListener {
                    AppPagesActivity.open(
                        this@MainActivity,
                        AppPagesActivity.PAGE_PROFILE
                    )
                }
            }
        )

        val brand =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        brand.addView(
            ViewKit.eyebrow(
                this,
                "NEXAUREN"
            )
        )

        brand.addView(
            ViewKit.title(
                this,
                "AUDIO TOOLS",
                17f
            )
        )

        header.addView(brand)

        header.addView(
            ViewKit.iconButton(
                this,
                "☰",
                "Menu completo"
            ).apply {
                setOnClickListener {
                    AppPagesActivity.open(
                        this@MainActivity,
                        AppPagesActivity.PAGE_MENU
                    )
                }
            }
        )

        header.addView(
            ViewKit.spacer(this, 4)
        )

        header.addView(
            ViewKit.iconButton(
                this,
                "⚙",
                "Definições"
            ).apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            SettingsActivity::class.java
                        )
                    )
                }
            }
        )

        return header
    }

    private fun sectionHeader(
        title: String
    ): ViewGroup =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                ViewKit.title(
                    this@MainActivity,
                    title,
                    20f
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }
            )
        }

    private fun renderTools(
        query: String = ""
    ) {
        if (!::toolGrid.isInitialized) {
            return
        }

        toolGrid.removeAllViews()

        val q =
            query.trim().lowercase()

        val filtered =
            ToolCatalog.tools.filter { tool ->
                val copy =
                    AppStrings.tool(
                        this,
                        tool.id
                    )

                q.isBlank() ||
                    (
                        copy.title +
                            " " +
                            copy.description +
                            " " +
                            copy.detail
                    )
                        .lowercase()
                        .contains(q)
            }

        if (filtered.isEmpty()) {
            toolGrid.addView(
                emptyCard(
                    "Não encontrámos nenhuma ferramenta com essa pesquisa."
                )
            )
            return
        }

        filtered.forEach { tool ->
            toolGrid.addView(
                toolCard(tool)
            )

            toolGrid.addView(
                ViewKit.spacer(this, 9)
            )
        }
    }

    private fun toolCard(
        tool: AudioTool
    ): MaterialCardView {
        val copy =
            AppStrings.tool(
                this,
                tool.id
            )

        val accent =
            toolAccent(tool.id)

        val favorite =
            UsageStore.isFavorite(
                this,
                tool.id
            )

        val card =
            ViewKit.card(
                this,
                clickable = true,
                accentColorRes = accent
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    ViewKit.dp(
                        this@MainActivity,
                        14
                    ),
                    ViewKit.dp(
                        this@MainActivity,
                        14
                    ),
                    ViewKit.dp(
                        this@MainActivity,
                        14
                    ),
                    ViewKit.dp(
                        this@MainActivity,
                        14
                    )
                )
            }

        val top =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        top.addView(
            ViewKit.iconBadge(
                this,
                toolSymbol(tool.id),
                accent
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@MainActivity,
                            48
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            48
                        )
                    ).apply {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                11
                            )
                    }

                contentDescription =
                    copy.title
            }
        )

        val text =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        text.addView(
            TextView(this).apply {
                this.text = copy.title
                textSize = 16f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_text
                    )
                )
            }
        )

        text.addView(ViewKit.spacer(this, 3))

        text.addView(
            TextView(this).apply {
                this.text = copy.description
                textSize = 11.5f
                maxLines = 2
                setLineSpacing(1.05f, 1f)
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        top.addView(text)

        top.addView(
            ViewKit.iconButton(
                this,
                if (favorite) "★" else "☆",
                "Alternar favorito"
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@MainActivity,
                            48
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            48
                        )
                    )

                setOnClickListener {
                    UsageStore.toggleFavorite(
                        this@MainActivity,
                        tool.id
                    )

                    renderTools(
                        searchInput
                            ?.text
                            ?.toString()
                            .orEmpty()
                    )
                }
            }
        )

        content.addView(top)
        content.addView(ViewKit.spacer(this, 10))

        val meta =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        meta.addView(
            ViewKit.pill(
                this,
                "ÁUDIO",
                colorRes = accent
            )
        )

        meta.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f
                    )
            }
        )

        meta.addView(
            ViewKit.pill(
                this,
                if (
                    tool.requiredPlan ==
                        "FREE"
                ) {
                    "FREE"
                } else {
                    tool.requiredPlan +
                        "  🔒"
                },
                colorRes = accent
            )
        )

        content.addView(meta)
        card.addView(content)

        card.setOnClickListener {
            openToolOrUpgrade(tool, card)
        }

        return card
    }

    private fun recentSection(): View {
        val ids =
            UsageStore
                .history(this)
                .map { it.toolId }
                .distinct()
                .take(3)

        if (ids.isEmpty()) {
            return emptyCard(
                "As ferramentas que usares vão aparecer aqui."
            )
        }

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        ids.mapNotNull {
            ToolCatalog.get(it)
        }.forEachIndexed { index, tool ->
            row.addView(
                ViewKit.quickChip(
                    this,
                    toolSymbol(tool.id),
                    AppStrings
                        .tool(this, tool.id)
                        .title,
                    toolAccent(tool.id)
                ) {
openToolOrUpgrade(
                        tool,
                        row
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index < ids.lastIndex) {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                7
                            )
                    }
                }
            )
        }

        return row
    }

    private fun favoriteSection(): View {
        val ids =
            UsageStore
                .favorites(this)
                .toList()
                .take(3)

        if (ids.isEmpty()) {
            return emptyCard(
                "Marca uma ferramenta com ☆ para a encontrares rapidamente."
            )
        }

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        ids.mapNotNull {
            ToolCatalog.get(it)
        }.forEachIndexed { index, tool ->
            row.addView(
                ViewKit.quickChip(
                    this,
                    toolSymbol(tool.id),
                    AppStrings
                        .tool(this, tool.id)
                        .title,
                    toolAccent(tool.id)
                ) {
openToolOrUpgrade(
                        tool,
                        row
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index < ids.lastIndex) {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                7
                            )
                    }
                }
            )
        }

        return row
    }

    private fun proStatusCard():
        MaterialCardView {
        val card =
            ViewKit.card(
                this,
                clickable = true,
                accentColorRes =
                    R.color.audio_purple
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(this@MainActivity, 15),
                    ViewKit.dp(this@MainActivity, 15),
                    ViewKit.dp(this@MainActivity, 15),
                    ViewKit.dp(this@MainActivity, 15)
                )
            }

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        row.addView(
            ViewKit.iconBadge(
                this,
                "PRO",
                R.color.audio_purple
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(this@MainActivity, 50),
                        ViewKit.dp(this@MainActivity, 50)
                    ).apply {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                11
                            )
                    }
            }
        )

        val copy =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        copy.addView(
            ViewKit.eyebrow(
                this,
                "ACESSO"
            )
        )

        copy.addView(
            TextView(this).apply {
                text =
                    "Plano e ferramentas desbloqueadas"
                textSize = 14.5f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_text
                    )
                )
            }
        )

        copy.addView(
            TextView(this).apply {
                text =
                    "Consulta o estado da tua assinatura e os planos disponíveis."
                textSize = 11.5f
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        row.addView(copy)
        content.addView(row)
        content.addView(ViewKit.spacer(this, 10))

        content.addView(
            ViewKit.button(
                this,
                "Ver planos",
                false,
                R.color.audio_purple
            ).apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            UpgradeActivity::class.java
                        )
                    )
                }
            }
        )

        card.addView(content)

        card.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    UpgradeActivity::class.java
                )
            )
        }

        return card
    }

    private fun localCard():
        MaterialCardView {
        val card =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_green
            )

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    ViewKit.dp(this@MainActivity, 14),
                    ViewKit.dp(this@MainActivity, 12),
                    ViewKit.dp(this@MainActivity, 14),
                    ViewKit.dp(this@MainActivity, 12)
                )
            }

        row.addView(
            ViewKit.iconBadge(
                this,
                "✓",
                R.color.audio_green
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(this@MainActivity, 42),
                        ViewKit.dp(this@MainActivity, 42)
                    ).apply {
                        rightMargin =
                            ViewKit.dp(
                                this@MainActivity,
                                10
                            )
                    }
            }
        )

        val copy =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        copy.addView(
            TextView(this).apply {
                text = "Processamento local"
                textSize = 13.5f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_text
                    )
                )
            }
        )

        copy.addView(
            TextView(this).apply {
                text =
                    "As ferramentas locais trabalham no próprio dispositivo sempre que possível."
                textSize = 11f
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        row.addView(copy)
        card.addView(row)

        return card
    }

    private fun emptyCard(
        message: String
    ): MaterialCardView =
        ViewKit.card(this).apply {
            addView(
                TextView(this@MainActivity).apply {
                    text = message
                    textSize = 12.5f
                    setLineSpacing(1.1f, 1f)
                    setTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.audio_muted
                        )
                    )
                    setPadding(
                        ViewKit.dp(
                            this@MainActivity,
                            16
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            16
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            16
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            16
                        )
                    )
                }
            )
        }

    private fun openToolOrUpgrade(
        tool: AudioTool,
        source: View
    ) {
        if (tool.requiredPlan == "FREE") {
            openTool(tool)
            return
        }

        source.isEnabled = false

        executor.execute {
            try {
                val entitlement =
                    PaymentClient.getEntitlement()

                handler.post {
                    source.isEnabled = true

                    if (
                        entitlement.hasAccess(
                            tool.requiredPlan
                        )
                    ) {
                        openTool(tool)
                    } else {
                        startActivity(
                            UpgradeActivity.intent(
                                this@MainActivity,
                                tool.requiredPlan
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                handler.post {
                    source.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "Não foi possível confirmar o teu acesso agora.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openTool(
        tool: AudioTool
    ) {
        if (tool.id == "pro-inspector") {
            startActivity(
                ProDemoActivity.intent(this)
            )
        } else {
            startActivity(
                ToolDetailActivity.intent(
                    this,
                    tool.id
                )
            )
        }
    }

    private fun refreshPlan() {
        executor.execute {
            runCatching {
                PaymentClient
                    .getEntitlement()
            }.onSuccess { entitlement ->
                handler.post {
                    val plan =
                        entitlement.plan

                    planView?.text =
                        plan

                    val color =
                        when (plan) {
                            "PREMIUM" ->
                                R.color.audio_purple
                            "PRO" ->
                                R.color.audio_blue
                            else ->
                                R.color.audio_green
                        }

                    planView?.setTextColor(
                        ContextCompat.getColor(
                            this,
                            color
                        )
                    )
                }
            }
        }
    }

    private fun requestNotificationsIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            window.decorView.postDelayed(
                {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS
                        ),
                        700
                    )
                },
                900
            )
        }
    }

    private fun openAuth() {
        startActivity(
            Intent(
                this,
                AuthActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun toolAccent(
        id: String
    ): Int =
        when (id) {
            "cut" -> R.color.audio_blue
            "convert" -> R.color.audio_purple
            "extract" -> R.color.audio_green
            "recorder" -> R.color.audio_red
            "analyzer" -> R.color.audio_yellow
            "pro-inspector" ->
                R.color.audio_purple
            else -> R.color.audio_blue
        }

    private fun toolSymbol(
        id: String
    ): String =
        when (id) {
            "cut" -> "✂"
            "convert" -> "⇄"
            "extract" -> "↥"
            "recorder" -> "●"
            "analyzer" -> "⌁"
            "pro-inspector" -> "★"
            else -> "•"
        }
}
