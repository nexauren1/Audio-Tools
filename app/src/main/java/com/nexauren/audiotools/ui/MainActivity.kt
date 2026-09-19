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
import android.widget.GridLayout
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
    private lateinit var toolGrid: GridLayout
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
        applyPlanBadge(
            PlanAccessStore.current(this)
        )
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
            // Access is kept locally for the active account.
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val root =
            ViewKit.page(this)

        root.addView(topBar())
        root.addView(
            ViewKit.spacer(this, 14)
        )

        val hero =
            ViewKit.hero(this)

        val heroBody =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18),
                    ViewKit.dp(this@MainActivity, 18)
                )
            }

        val heroTop =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        heroTop.addView(
            ViewKit.pill(
                this,
                "NEXAUREN AUDIO",
                colorRes =
                    R.color.audio_teal
            )
        )

        heroTop.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f
                    )
            }
        )

        planView =
            ViewKit.pill(
                this,
                "FREE",
                colorRes =
                    R.color.audio_green
            )

        heroTop.addView(planView)
        heroBody.addView(heroTop)

        heroBody.addView(
            ViewKit.spacer(this, 14)
        )

        val firstName =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.displayName
                ?.trim()
                ?.split(" ")
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }

        heroBody.addView(
            ViewKit.eyebrow(
                this,
                if (firstName != null) {
                    "OLÁ, " +
                        firstName.uppercase()
                } else {
                    "AUDIO WORKSPACE"
                }
            )
        )

        heroBody.addView(
            ViewKit.spacer(this, 4)
        )

        heroBody.addView(
            ViewKit.title(
                this,
                "Cria. Edita.\nOuve melhor.",
                30f
            )
        )

        heroBody.addView(
            ViewKit.spacer(this, 6)
        )

        heroBody.addView(
            ViewKit.subtitle(
                this,
                "Seis ferramentas para trabalhar áudio sem complicar."
            )
        )

        heroBody.addView(
            ViewKit.spacer(this, 13)
        )

        heroBody.addView(
            ViewKit.button(
                this,
                "Abrir Tools",
                true,
                R.color.audio_teal
            ).apply {
                setOnClickListener {
                    AppPagesActivity.open(
                        this@MainActivity,
                        AppPagesActivity.PAGE_TOOLS
                    )
                }
            }
        )

        hero.addView(heroBody)
        root.addView(hero)

        root.addView(
            ViewKit.spacer(this, 18)
        )

        val searchRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        searchInput =
            EditText(this).apply {
                hint = "Pesquisar uma ferramenta"
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

        searchRow.addView(
            searchInput,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        searchRow.addView(
            ViewKit.iconButton(
                this,
                "⌕",
                "Focar pesquisa"
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@MainActivity,
                            52
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            52
                        )
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

        root.addView(searchRow)

        root.addView(
            ViewKit.spacer(this, 14)
        )

        root.addView(
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL

                addView(
                    ViewKit.title(
                        this@MainActivity,
                        "Ferramentas",
                        21f
                    ).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams
                                    .WRAP_CONTENT,
                                1f
                            )
                    }
                )

                addView(
                    ViewKit.subtitle(
                        this@MainActivity,
                        "6"
                    )
                )
            }
        )

        root.addView(
            ViewKit.spacer(this, 9)
        )

        toolGrid =
            GridLayout(this).apply {
                columnCount = 3
                useDefaultMargins = false
                alignmentMode =
                    GridLayout.ALIGN_BOUNDS
            }

        root.addView(toolGrid)

        root.addView(
            ViewKit.spacer(this, 16)
        )


        root.addView(
            ViewKit.spacer(this, 10)
        )

        root.addView(
            TextView(this).apply {
                text =
                    "Audio Tools  •  v" +
                        BuildConfig.VERSION_NAME
                textSize = 10.5f
                gravity = Gravity.CENTER
                letterSpacing = 0.08f
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
        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val brand =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams
                            .WRAP_CONTENT,
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
                19f
            )
        )

        row.addView(brand)

        row.addView(
            ViewKit.iconButton(
                this,
                "☰",
                "Abrir menu"
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@MainActivity,
                            52
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            52
                        )
                    )

                setOnClickListener {
                    AppPagesActivity.open(
                        this@MainActivity,
                        AppPagesActivity.PAGE_MENU
                    )
                }
            }
        )

        return row
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
            val empty =
                ViewKit.card(
                    this,
                    accentColorRes =
                        R.color.audio_border
                )

            empty.addView(
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER_HORIZONTAL
                    setPadding(
                        ViewKit.dp(
                            this@MainActivity,
                            18
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            18
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            18
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            18
                        )
                    )
                    addView(
                        ViewKit.title(
                            this@MainActivity,
                            "Sem resultados",
                            17f
                        )
                    )
                    addView(
                        ViewKit.spacer(
                            this@MainActivity,
                            4
                        )
                    )
                    addView(
                        ViewKit.subtitle(
                            this@MainActivity,
                            "Experimenta outro nome de ferramenta."
                        )
                    )
                }
            )

            val params =
                GridLayout.LayoutParams(
                    GridLayout.spec(
                        0,
                        3
                    ),
                    GridLayout.spec(
                        0,
                        3
                    )
                )

            params.width = GridLayout.LayoutParams.MATCH_PARENT

            toolGrid.addView(
                empty,
                params
            )
            return
        }

        filtered.forEachIndexed {
            index,
            tool ->
            val card =
                toolCard(tool)

            val column =
                index % 3

            val row =
                index / 3

            val params =
                GridLayout.LayoutParams(
                    GridLayout.spec(
                        row,
                        1,
                        GridLayout.FILL,
                        1f
                    ),
                    GridLayout.spec(
                        column,
                        1,
                        GridLayout.FILL,
                        1f
                    )
                ).apply {
                    width = 0
                    height =
                        ViewKit.dp(
                            this@MainActivity,
                            190
                        )

                    val gap =
                        ViewKit.dp(
                            this@MainActivity,
                            5
                        )

                    setMargins(
                        if (column == 0) 0 else gap,
                        if (row == 0) 0 else gap,
                        if (column == 2) 0 else gap,
                        gap
                    )
                }

            toolGrid.addView(
                card,
                params
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

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    ViewKit.dp(this@MainActivity, 10),
                    ViewKit.dp(this@MainActivity, 10),
                    ViewKit.dp(this@MainActivity, 10),
                    ViewKit.dp(this@MainActivity, 10)
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
                            42
                        ),
                        ViewKit.dp(
                            this@MainActivity,
                            42
                        )
                    )
            }
        )

        top.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f
                    )
            }
        )

        top.addView(
            ViewKit.iconButton(
                this,
                if (favorite) "★" else "☆",
                if (favorite) {
                    "Remover dos favoritos"
                } else {
                    "Adicionar aos favoritos"
                }
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

        body.addView(top)
        body.addView(
            ViewKit.spacer(this, 8)
        )

        body.addView(
            TextView(this).apply {
                text =
                    copy.title
                textSize = 14f
                maxLines = 2
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

        body.addView(
            ViewKit.spacer(this, 3)
        )

        body.addView(
            TextView(this).apply {
                text =
                    copy.description
                textSize = 10.5f
                maxLines = 2
                setLineSpacing(
                    1.04f,
                    1f
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        body.addView(
            ViewKit.spacer(this, 6)
        )

        val hasAccess =
            PlanAccessStore.hasAccess(
                this,
                tool.requiredPlan
            )

        body.addView(
            ViewKit.pill(
                this,
                if (
                    tool.requiredPlan ==
                        "FREE"
                ) {
                    "FREE"
                } else if (hasAccess) {
                    tool.requiredPlan
                } else {
                    tool.requiredPlan +
                        "  🔒"
                },
                colorRes = accent
            )
        )

        body.addView(
            ViewKit.spacer(this, 6)
        )

        val actions =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        actions.addView(
            ViewKit.iconButton(
                this,
                "↗",
                "Partilhar ferramenta"
            ).apply {
                setOnClickListener {
                    shareTool(tool)
                }
            }
        )

        actions.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f
                    )
            }
        )

        actions.addView(
            ViewKit.iconButton(
                this,
                "›",
                if (
                    tool.requiredPlan ==
                        "FREE" ||
                    PlanAccessStore.hasAccess(
                        this@MainActivity,
                        tool.requiredPlan
                    )
                ) {
                    "Abrir"
                } else {
                    "Ver acesso"
                }
            ).apply {
                setOnClickListener {
                    openToolOrUpgrade(
                        tool,
                        card
                    )
                }
            }
        )

        body.addView(actions)
        card.addView(body)

        card.setOnClickListener {
            openToolOrUpgrade(
                tool,
                card
            )
        }

        return card
    }

    private fun shareTool(
        tool: AudioTool
    ) {
        val copy =
            AppStrings.tool(
                this,
                tool.id
            )

        runCatching {
            startActivity(
                Intent(
                    Intent.ACTION_SEND
                ).apply {
                    type =
                        "text/plain"

                    putExtra(
                        Intent.EXTRA_TEXT,
                        copy.title +
                            "\n\n" +
                            copy.description +
                            "\n\nNexauren Audio Tools"
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        copy.title
                    )
                }
            )
        }.onFailure {
            Toast.makeText(
                this,
                "Não foi possível abrir a partilha.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openToolOrUpgrade(
        tool: AudioTool,
        source: View
    ) {
        if (
            PlanAccessStore.hasAccess(
                this,
                tool.requiredPlan
            )
        ) {
            openTool(tool)
        } else {
            startActivity(
                UpgradeActivity.intent(
                    this,
                    tool.requiredPlan
                )
            )
        }
    }

    private fun openTool(
        tool: AudioTool
    ) {
        if (
            tool.id ==
                "pro-inspector"
        ) {
            startActivity(
                ProDemoActivity.intent(
                    this
                )
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
                PaymentClient.getEntitlement()
            }.onSuccess { entitlement ->
                PlanAccessStore.save(
                    this@MainActivity,
                    entitlement
                )

                handler.post {
                    applyPlanBadge(
                        entitlement
                    )
                    renderTools(
                        searchInput
                            ?.text
                            ?.toString()
                            .orEmpty()
                    )
                }
            }
        }
    }

    private fun applyPlanBadge(
        entitlement:
            com.nexauren.audiotools.payments.Entitlement
    ) {
        val plan =
            entitlement.plan

        val color =
            when (plan) {
                "PREMIUM" ->
                    R.color.audio_purple
                "PRO" ->
                    R.color.audio_blue
                else ->
                    R.color.audio_green
            }

        planView?.let {
            it.text = plan
            ViewKit.setPillColor(
                it,
                this@MainActivity,
                color
            )
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
            "cut" ->
                R.color.audio_blue
            "convert" ->
                R.color.audio_purple
            "extract" ->
                R.color.audio_green
            "recorder" ->
                R.color.audio_red
            "analyzer" ->
                R.color.audio_yellow
            "pro-inspector" ->
                R.color.audio_teal
            else ->
                R.color.audio_blue
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
            "pro-inspector" -> "◈"
            else -> "•"
        }
}
