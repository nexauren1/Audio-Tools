package com.nexauren.audiotools.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import com.nexauren.audiotools.payments.PaymentClient
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class AppPagesActivity : ComponentActivity() {
    companion object {
        const val PAGE_MENU = "menu"
        const val PAGE_TOOLS = "tools"
        const val PAGE_FAVORITES = "favorites"
        const val PAGE_HISTORY = "history"
        const val PAGE_STATS = "stats"
        const val PAGE_PROFILE = "profile"
        const val PAGE_SUPPORT = "support"

        private const val EXTRA_PAGE = "page"

        fun open(
            context: Context,
            page: String
        ) {
            context.startActivity(
                Intent(
                    context,
                    AppPagesActivity::class.java
                ).putExtra(
                    EXTRA_PAGE,
                    page
                )
            )
        }
    }

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private val uiHandler =
        Handler(Looper.getMainLooper())

    private val networkExecutor =
        Executors.newSingleThreadExecutor()

    private var avatar: ImageView? = null
    private var selectedSupportType = "SUPPORT"

    private val pickAvatar =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                updateAvatar(uri)
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openAuth()
            return
        }

        render(
            intent.getStringExtra(EXTRA_PAGE)
                ?: PAGE_MENU
        )
    }

    override fun onDestroy() {
        networkExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun render(
        page: String
    ) {
        val root = ViewKit.page(this)

        val title = when (page) {
            PAGE_MENU -> "Menu"
            PAGE_TOOLS -> "Tools"
            PAGE_FAVORITES -> "Favoritos"
            PAGE_HISTORY -> "Histórico"
            PAGE_STATS -> "Estatísticas"
            PAGE_PROFILE -> "Me"
            PAGE_SUPPORT -> "Suporte"
            else -> "Audio Tools"
        }

        root.addView(header(title))
        root.addView(ViewKit.spacer(this, 15))

        when (page) {
            PAGE_MENU -> buildMenu(root)
            PAGE_TOOLS -> buildTools(root)
            PAGE_FAVORITES -> buildFavorites(root)
            PAGE_HISTORY -> buildHistory(root)
            PAGE_STATS -> buildStats(root)
            PAGE_PROFILE -> buildProfile(root)
            PAGE_SUPPORT -> buildSupport(root)
        }

        root.addView(ViewKit.spacer(this, 18))

        if (page != PAGE_MENU) {
            root.addView(
                ViewKit.bottomNav(
                    this,
                    page
                )
            )
        }

        root.addView(ViewKit.spacer(this, 10))

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
                addView(root)
            }
        )
    }

    private fun header(
        title: String
    ): ViewGroup {
        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        row.addView(
            ViewKit.iconButton(
                this,
                "‹",
                "Voltar"
            ).apply {
                setOnClickListener {
                    finish()
                }
            }
        )

        row.addView(
            ViewKit.title(
                this,
                title,
                24f
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin =
                            ViewKit.dp(
                                this@AppPagesActivity,
                                10
                            )
                    }
            }
        )

        row.addView(
            ViewKit.iconButton(
                this,
                "●",
                "Perfil"
            ).apply {
                setOnClickListener {
                    open(
                        this@AppPagesActivity,
                        PAGE_PROFILE
                    )
                }
            }
        )

        return row
    }

    private fun buildMenu(
        root: LinearLayout
    ) {
        root.addView(
            ViewKit.hero(this).apply {
                addView(
                    LinearLayout(
                        this@AppPagesActivity
                    ).apply {
                        orientation =
                            LinearLayout.VERTICAL
                        setPadding(
                            ViewKit.dp(
                                this@AppPagesActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                18
                            )
                        )

                        addView(
                            ViewKit.eyebrow(
                                this@AppPagesActivity,
                                "NAVEGAÇÃO"
                            )
                        )

                        addView(
                            ViewKit.title(
                                this@AppPagesActivity,
                                "Tudo num só lugar.",
                                28f
                            )
                        )

                        addView(
                            ViewKit.spacer(
                                this@AppPagesActivity,
                                5
                            )
                        )

                        addView(
                            ViewKit.subtitle(
                                this@AppPagesActivity,
                                "Acede rapidamente às áreas principais do Audio Tools."
                            )
                        )
                    }
                )
            }
        )

        root.addView(
            ViewKit.spacer(this, 13)
        )

        val pages = listOf(
            Triple(
                "Tools",
                "As seis ferramentas de áudio",
                "tools"
            ),
            Triple(
                "Favoritos",
                "As ferramentas que guardaste",
                "favorites"
            ),
            Triple(
                "Histórico",
                "O que usaste recentemente",
                "history"
            ),
            Triple(
                "Estatísticas",
                "Resumo do teu uso",
                "stats"
            ),
            Triple(
                "Planos",
                "Free, Pro e Premium",
                "plans"
            ),
            Triple(
                "Me",
                "Perfil e identidade Firebase",
                "profile"
            ),
            Triple(
                "Armazenamento",
                "Escolher onde os áudios serão guardados",
                "storage"
            ),
            Triple(
                "Suporte",
                "Suporte, reclamações e sugestões",
                "support"
            ),
            Triple(
                "Definições",
                "Idioma, atualizações e preferências",
                "settings"
            ),
            Triple(
                "Conta",
                "Segurança e sessão",
                "account"
            ),
            Triple(
                "Changelog",
                "Novidades e versões",
                "changelog"
            ),
            Triple(
                "Sobre",
                "Informações sobre o app",
                "about"
            )
        )

        pages.chunked(2).forEach { pair ->
            val row =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }

            pair.forEachIndexed { index, item ->
                row.addView(
                    ViewKit.menuTile(
                        this,
                        menuIcon(item.third),
                        item.first,
                        item.second,
                        tileColor(index)
                    ) {
                        when (item.third) {
                            "tools" ->
                                open(
                                    this,
                                    PAGE_TOOLS
                                )
                            "favorites" ->
                                open(
                                    this,
                                    PAGE_FAVORITES
                                )
                            "history" ->
                                open(
                                    this,
                                    PAGE_HISTORY
                                )
                            "stats" ->
                                open(
                                    this,
                                    PAGE_STATS
                                )
                            "plans" ->
                                startActivity(
                                    Intent(
                                        this,
                                        UpgradeActivity::class.java
                                    )
                                )
                            "profile" ->
                                open(
                                    this,
                                    PAGE_PROFILE
                                )
                            "storage" ->
                                startActivity(
                                    Intent(
                                        this,
                                        StorageActivity::class.java
                                    )
                                )
                            "support" ->
                                open(
                                    this,
                                    PAGE_SUPPORT
                                )
                            "settings" ->
                                startActivity(
                                    Intent(
                                        this,
                                        SettingsActivity::class.java
                                    )
                                )
                            "account" ->
                                startActivity(
                                    Intent(
                                        this,
                                        AccountActivity::class.java
                                    )
                                )
                            "changelog" ->
                                startActivity(
                                    Intent(
                                        this,
                                        ChangelogActivity::class.java
                                    )
                                )
                            "about" ->
                                startActivity(
                                    Intent(
                                        this,
                                        AboutActivity::class.java
                                    )
                                )
                        }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        if (index == 0) {
                            rightMargin =
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    8
                                )
                        }
                    }
                )
            }

            root.addView(row)
            root.addView(
                ViewKit.spacer(this, 8)
            )
        }
    }

    private fun menuIcon(
        page: String
    ): String =
        when (page) {
            "tools" -> "♪"
            "favorites" -> "★"
            "history" -> "↺"
            "stats" -> "⌁"
            "plans" -> "◇"
            "profile" -> "●"
            "storage" -> "▣"
            "support" -> "?"
            "settings" -> "⚙"
            "account" -> "✓"
            "changelog" -> "v"
            "about" -> "i"
            else -> "•"
        }


    private fun buildTools(
        root: LinearLayout
    ) {
        root.addView(
            ViewKit.subtitle(
                this,
                "Todas as ferramentas são de áudio. Aqui não existem categorias desnecessárias."
            )
        )

        root.addView(
            ViewKit.spacer(this, 10)
        )

        ToolCatalog.tools.forEach { tool ->
            root.addView(toolRow(tool))
            root.addView(
                ViewKit.spacer(this, 9)
            )
        }
    }

    private fun buildFavorites(
        root: LinearLayout
    ) {
        val tools =
            UsageStore
                .favorites(this)
                .mapNotNull {
                    ToolCatalog.get(it)
                }

        if (tools.isEmpty()) {
            root.addView(
                emptyState(
                    "Ainda não tens favoritos.",
                    "Abre Tools e toca na estrela de qualquer ferramenta."
                )
            )
            return
        }

        tools.forEach {
            root.addView(toolRow(it))
            root.addView(
                ViewKit.spacer(this, 9)
            )
        }
    }

    private fun buildHistory(
        root: LinearLayout
    ) {
        val history =
            UsageStore.history(this)

        if (history.isEmpty()) {
            root.addView(
                emptyState(
                    "Histórico vazio.",
                    "Quando usares uma ferramenta, a atividade ficará registada aqui neste dispositivo."
                )
            )
            return
        }

        root.addView(
            ViewKit.subtitle(
                this,
                "Os últimos " +
                    minOf(
                        history.size,
                        30
                    ) +
                    " usos."
            )
        )

        root.addView(
            ViewKit.spacer(this, 9)
        )

        history.take(30).forEach { entry ->
            val tool =
                ToolCatalog.get(entry.toolId)
                    ?: return@forEach

            val time =
                SimpleDateFormat(
                    "dd/MM/yyyy  HH:mm",
                    Locale.getDefault()
                ).format(
                    Date(
                        entry.timestamp
                    )
                )

            val card =
                ViewKit.card(
                    this,
                    clickable = true,
                    accentColorRes =
                        accentFor(tool.id)
                )

            val row =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        ViewKit.dp(
                            this@AppPagesActivity,
                            13
                        ),
                        ViewKit.dp(
                            this@AppPagesActivity,
                            13
                        ),
                        ViewKit.dp(
                            this@AppPagesActivity,
                            13
                        ),
                        ViewKit.dp(
                            this@AppPagesActivity,
                            13
                        )
                    )
                }

            row.addView(
                ViewKit.iconBadge(
                    this,
                    symbolFor(tool.id),
                    accentFor(tool.id)
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewKit.dp(
                                this@AppPagesActivity,
                                44
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                44
                            )
                        ).apply {
                            rightMargin =
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    10
                                )
                        }
                }
            )

            row.addView(
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

                    addView(
                        ViewKit.title(
                            this@AppPagesActivity,
                            AppStrings.tool(
                                this@AppPagesActivity,
                                tool.id
                            ).title,
                            15f
                        )
                    )

                    addView(
                        ViewKit.spacer(
                            this@AppPagesActivity,
                            2
                        )
                    )

                    addView(
                        ViewKit.subtitle(
                            this@AppPagesActivity,
                            time
                        )
                    )
                }
            )

            row.addView(
                ViewKit.pill(
                    this,
                    if (tool.requiredPlan == "FREE") {
                        "FREE"
                    } else {
                        tool.requiredPlan
                    },
                    colorRes =
                        accentFor(tool.id)
                )
            )

            card.addView(row)

            card.setOnClickListener {
                openToolOrUpgrade(
                    tool,
                    card
                )
            }

            root.addView(card)
            root.addView(
                ViewKit.spacer(this, 8)
            )
        }
    }

    private fun buildStats(
        root: LinearLayout
    ) {
        val total =
            UsageStore.totalRuns(this)
        val unique =
            UsageStore.uniqueTools(this)
        val favorites =
            UsageStore.favorites(this).size
        val week =
            UsageStore.lastDaysRuns(
                this,
                7
            )

        root.addView(
            ViewKit.subtitle(
                this,
                "Estatísticas pessoais do uso do app neste dispositivo."
            )
        )

        root.addView(
            ViewKit.spacer(this, 12)
        )

        val grid =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        val stats = listOf(
            Triple(
                total.toString(),
                "utilizações",
                R.color.audio_blue
            ),
            Triple(
                unique.toString(),
                "ferramentas usadas",
                R.color.audio_green
            ),
            Triple(
                week.toString(),
                "últimos 7 dias",
                R.color.audio_purple
            )
        )

        stats.forEachIndexed {
            index,
            item ->
            grid.addView(
                ViewKit.stat(
                    this,
                    item.first,
                    item.second,
                    item.third
                ),
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index < stats.lastIndex) {
                        rightMargin =
                            ViewKit.dp(
                                this@AppPagesActivity,
                                7
                            )
                    }
                }
            )
        }

        root.addView(grid)
        root.addView(
            ViewKit.spacer(this, 12)
        )

        root.addView(
            ViewKit.stat(
                this,
                favorites.toString(),
                "favoritos guardados",
                R.color.audio_yellow
            )
        )

        root.addView(
            ViewKit.spacer(this, 14)
        )

        root.addView(
            ViewKit.title(
                this,
                "Ferramentas mais usadas",
                18f
            )
        )

        root.addView(
            ViewKit.spacer(this, 7)
        )

        ToolCatalog.tools
            .sortedByDescending {
                UsageStore.usageCount(
                    this,
                    it.id
                )
            }
            .forEach { tool ->
                val count =
                    UsageStore.usageCount(
                        this,
                        tool.id
                    )

                val card =
                    ViewKit.card(
                        this,
                        accentColorRes =
                            accentFor(tool.id)
                    )

                val row =
                    LinearLayout(this).apply {
                        orientation =
                            LinearLayout.HORIZONTAL
                        gravity =
                            Gravity.CENTER_VERTICAL
                        setPadding(
                            ViewKit.dp(
                                this@AppPagesActivity,
                                12
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                10
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                12
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                10
                            )
                        )
                    }

                row.addView(
                    ViewKit.iconBadge(
                        this,
                        symbolFor(tool.id),
                        accentFor(tool.id)
                    ).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    40
                                ),
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    40
                                )
                            ).apply {
                                rightMargin =
                                    ViewKit.dp(
                                        this@AppPagesActivity,
                                        9
                                    )
                            }
                    }
                )

                row.addView(
                    ViewKit.title(
                        this,
                        AppStrings.tool(
                            this@AppPagesActivity,
                            tool.id
                        ).title,
                        13.5f
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

                row.addView(
                    ViewKit.pill(
                        this,
                        count.toString(),
                        colorRes =
                            accentFor(tool.id)
                    )
                )

                card.addView(row)
                root.addView(card)
                root.addView(
                    ViewKit.spacer(this, 7)
                )
            }
    }

    private fun buildProfile(
        root: LinearLayout
    ) {
        val user =
            auth.currentUser
                ?: return

        val name =
            user.displayName
                ?.trim()
                ?.takeUnless { it.isNullOrBlank() }
                ?: "Utilizador Audio Tools"

        val email =
            user.email.orEmpty()

        val photo =
            ImageView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@AppPagesActivity,
                            88
                        ),
                        ViewKit.dp(
                            this@AppPagesActivity,
                            88
                        )
                    )
                scaleType =
                    ImageView.ScaleType.CENTER_CROP
                background =
                    ContextCompat.getDrawable(
                        this@AppPagesActivity,
                        R.drawable.avatar_bg
                    )
                setImageResource(
                    android.R.drawable.ic_menu_camera
                )
                contentDescription =
                    "Foto de perfil"
            }

        avatar = photo

        val hero =
            ViewKit.hero(this)

        val heroContent =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    ViewKit.dp(
                        this@AppPagesActivity,
                        18
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        20
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        18
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        20
                    )
                )

                addView(photo)
                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        10
                    )
                )
                addView(
                    ViewKit.title(
                        this@AppPagesActivity,
                        name,
                        25f
                    )
                )
                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        4
                    )
                )
                addView(
                    ViewKit.subtitle(
                        this@AppPagesActivity,
                        email
                    )
                )
            }

        hero.addView(heroContent)
        root.addView(hero)

        val localAvatar =
            getSharedPreferences(
                "audio_tools_personal",
                MODE_PRIVATE
            ).getString(
                "profile_avatar_file",
                ""
            )
                ?: ""

        when {
            localAvatar.isNotBlank() &&
                File(localAvatar).exists() ->
                avatar?.setImageURI(
                    Uri.fromFile(
                        File(localAvatar)
                    )
                )

            user.photoUrl != null ->
                loadAvatar(
                    user.photoUrl!!
                )
        }

        root.addView(
            ViewKit.spacer(this, 12)
        )

        val actions =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        actions.addView(
            ViewKit.button(
                this,
                "Alterar foto",
                false,
                R.color.audio_blue
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                setOnClickListener {
                    pickAvatar.launch("image/*")
                }
            }
        )

        actions.addView(
            ViewKit.spacer(this, 1).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@AppPagesActivity,
                            8
                        ),
                        1
                    )
            }
        )

        actions.addView(
            ViewKit.button(
                this,
                "Editar nome",
                false,
                R.color.audio_purple
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                setOnClickListener {
                    editName()
                }
            }
        )

        root.addView(actions)

        root.addView(
            ViewKit.spacer(this, 14)
        )

        val details =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_blue
            )

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(
                        this@AppPagesActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        15
                    )
                )

                addView(
                    ViewKit.eyebrow(
                        this@AppPagesActivity,
                        "IDENTIDADE FIREBASE"
                    )
                )

                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        7
                    )
                )

                addView(
                    infoLine(
                        "UID",
                        user.uid
                    )
                )

                addView(
                    infoLine(
                        "Email",
                        email.ifBlank { "—" }
                    )
                )

                addView(
                    infoLine(
                        "Email verificado",
                        if (user.isEmailVerified) {
                            "Sim"
                        } else {
                            "Não"
                        }
                    )
                )

                addView(
                    infoLine(
                        "Fornecedor",
                        user.providerData
                            .firstOrNull()
                            ?.providerId
                            ?: "password"
                    )
                )

                addView(
                    infoLine(
                        "Criado em",
                        formatDate(
                            user.metadata
                                ?.creationTimestamp
                        )
                    )
                )

                addView(
                    infoLine(
                        "Última sessão",
                        formatDate(
                            user.metadata
                                ?.lastSignInTimestamp
                        )
                    )
                )

                addView(
                    infoLine(
                        "Telefone",
                        user.phoneNumber
                            ?: "—"
                    )
                )
            }

        details.addView(body)
        root.addView(details)

        root.addView(
            ViewKit.spacer(this, 12)
        )

        root.addView(
            ViewKit.button(
                this,
                "Gerir segurança da conta",
                false,
                R.color.audio_green
            ).apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@AppPagesActivity,
                            AccountActivity::class.java
                        )
                    )
                }
            }
        )

        root.addView(
            ViewKit.spacer(this, 8)
        )

        root.addView(
            ViewKit.button(
                this,
                "Ver plano",
                true,
                R.color.audio_purple
            ).apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@AppPagesActivity,
                            UpgradeActivity::class.java
                        )
                    )
                }
            }
        )
    }

    private fun buildSupport(
        root: LinearLayout
    ) {
        root.addView(
            ViewKit.subtitle(
                this,
                "Envia um pedido de suporte, uma reclamação ou uma sugestão. Por enquanto, o formulário fica guardado neste dispositivo; ligaremos o destino de atendimento depois."
            )
        )

        root.addView(
            ViewKit.spacer(this, 13)
        )

        val toggle =
            MaterialButtonToggleGroup(this).apply {
                isSingleSelection = true
                isSelectionRequired = true
            }

        val options =
            listOf(
                "SUPPORT" to "Suporte",
                "COMPLAINT" to "Reclamação",
                "SUGGESTION" to "Sugestão"
            )

        options.forEach { pair ->
            val button =
                ViewKit.button(
                    this,
                    pair.second,
                    pair.first == selectedSupportType,
                    R.color.audio_blue
                )

            button.id =
                View.generateViewId()

            toggle.addView(button)

            if (
                pair.first ==
                    selectedSupportType
            ) {
                toggle.check(button.id)
            }

            button.setOnClickListener {
                selectedSupportType =
                    pair.first
            }
        }

        root.addView(toggle)

        root.addView(
            ViewKit.spacer(this, 12)
        )

        val subject =
            inputField("Assunto")

        val message =
            inputField("Mensagem")

        message.first.minimumHeight =
            ViewKit.dp(this, 150)

        message.first.gravity =
            Gravity.TOP

        message.first.setSingleLine(false)

        root.addView(subject.second)
        root.addView(ViewKit.spacer(this, 8))
        root.addView(message.second)
        root.addView(ViewKit.spacer(this, 10))

        root.addView(
            ViewKit.button(
                this,
                "Guardar pedido",
                true,
                R.color.audio_blue
            ).apply {
                setOnClickListener {
                    val s =
                        subject.first.text
                            .toString()
                            .trim()

                    val m =
                        message.first.text
                            .toString()
                            .trim()

                    if (
                        s.isBlank() ||
                        m.length < 5
                    ) {
                        Toast.makeText(
                            this@AppPagesActivity,
                            "Preenche o assunto e escreve uma mensagem um pouco mais completa.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    saveSupportRequest(
                        s,
                        m
                    )

                    subject.first.setText("")
                    message.first.setText("")

                    Toast.makeText(
                        this@AppPagesActivity,
                        "Pedido guardado. O canal de atendimento será ligado numa próxima fase.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        root.addView(
            ViewKit.spacer(this, 16)
        )

        root.addView(
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_green
            ).apply {
                addView(
                    LinearLayout(
                        this@AppPagesActivity
                    ).apply {
                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            ViewKit.dp(
                                this@AppPagesActivity,
                                14
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                14
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                14
                            ),
                            ViewKit.dp(
                                this@AppPagesActivity,
                                14
                            )
                        )

                        addView(
                            ViewKit.eyebrow(
                                this@AppPagesActivity,
                                "PRÓXIMO PASSO"
                            )
                        )

                        addView(
                            ViewKit.title(
                                this@AppPagesActivity,
                                "Centro de suporte",
                                17f
                            )
                        )

                        addView(
                            ViewKit.spacer(
                                this@AppPagesActivity,
                                4
                            )
                        )

                        addView(
                            ViewKit.subtitle(
                                this@AppPagesActivity,
                                "A estrutura já está pronta para ligar estes pedidos ao teu painel, email ou backend quando decidirmos onde os receber."
                            )
                        )
                    }
                )
            }
        )
    }

    private fun toolRow(
        tool: AudioTool
    ): View {
        val accent =
            accentFor(tool.id)

        val copy =
            AppStrings.tool(
                this,
                tool.id
            )

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

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    ViewKit.dp(
                        this@AppPagesActivity,
                        13
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        13
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        13
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        13
                    )
                )
            }

        row.addView(
            ViewKit.iconBadge(
                this,
                symbolFor(tool.id),
                accent
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewKit.dp(
                            this@AppPagesActivity,
                            48
                        ),
                        ViewKit.dp(
                            this@AppPagesActivity,
                            48
                        )
                    ).apply {
                        rightMargin =
                            ViewKit.dp(
                                this@AppPagesActivity,
                                10
                            )
                    }
            }
        )

        val copyView =
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

                addView(
                    ViewKit.title(
                        this@AppPagesActivity,
                        copy.title,
                        15f
                    )
                )

                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        3
                    )
                )

                addView(
                    ViewKit.subtitle(
                        this@AppPagesActivity,
                        copy.description
                    )
                )

                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        6
                    )
                )

                addView(
                    ViewKit.pill(
                        this@AppPagesActivity,
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
            }

        row.addView(copyView)

        row.addView(
            ViewKit.iconButton(
                this,
                if (favorite) {
                    "★"
                } else {
                    "☆"
                },
                "Favorito"
            ).apply {
                setOnClickListener {
                    UsageStore.toggleFavorite(
                        this@AppPagesActivity,
                        tool.id
                    )

                    render(
                        intent.getStringExtra(
                            EXTRA_PAGE
                        ) ?: PAGE_TOOLS
                    )
                }
            }
        )

        card.addView(row)

        card.setOnClickListener {
            openToolOrUpgrade(
                tool,
                card
            )
        }

        return card
    }

    private fun emptyState(
        title: String,
        message: String
    ): View {
        val card =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_border
            )

        card.addView(
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    ViewKit.dp(
                        this@AppPagesActivity,
                        22
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        28
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        22
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        28
                    )
                )

                addView(
                    ViewKit.iconBadge(
                        this@AppPagesActivity,
                        "·",
                        R.color.audio_muted
                    ).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    52
                                ),
                                ViewKit.dp(
                                    this@AppPagesActivity,
                                    52
                                )
                            )
                    }
                )

                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        9
                    )
                )

                addView(
                    ViewKit.title(
                        this@AppPagesActivity,
                        title,
                        18f
                    )
                )

                addView(
                    ViewKit.spacer(
                        this@AppPagesActivity,
                        4
                    )
                )

                addView(
                    ViewKit.subtitle(
                        this@AppPagesActivity,
                        message
                    )
                )
            }
        )

        return card
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

        networkExecutor.execute {
            try {
                val entitlement =
                    PaymentClient.getEntitlement()

                uiHandler.post {
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
                                this@AppPagesActivity,
                                tool.requiredPlan
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                uiHandler.post {
                    source.isEnabled = true

                    Toast.makeText(
                        this@AppPagesActivity,
                        "Não foi possível confirmar o acesso agora.",
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

    private fun inputField(
        hint: String
    ): Pair<EditText, MaterialCardView> {
        val field =
            EditText(this).apply {
                this.hint = hint
                textSize = 14f
                setPadding(
                    ViewKit.dp(
                        this@AppPagesActivity,
                        14
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        12
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        12
                    ),
                    ViewKit.dp(
                        this@AppPagesActivity,
                        12
                    )
                )
                minHeight =
                    ViewKit.dp(
                        this@AppPagesActivity,
                        54
                    )
                setTextColor(
                    ContextCompat.getColor(
                        this@AppPagesActivity,
                        R.color.audio_text
                    )
                )
                setHintTextColor(
                    ContextCompat.getColor(
                        this@AppPagesActivity,
                        R.color.audio_muted
                    )
                )
                background =
                    ContextCompat.getDrawable(
                        this@AppPagesActivity,
                        R.drawable.field_bg
                    )
            }

        val card =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_border
            )

        card.addView(field)

        return field to card
    }

    private fun saveSupportRequest(
        subject: String,
        message: String
    ) {
        val prefs =
            getSharedPreferences(
                "audio_tools_support",
                MODE_PRIVATE
            )

        val existing =
            prefs.getStringSet(
                "requests",
                emptySet()
            )?.toMutableSet()
                ?: mutableSetOf()

        val user =
            auth.currentUser

        existing.add(
            System.currentTimeMillis().toString() +
                "|" +
                selectedSupportType +
                "|" +
                (user?.uid ?: "") +
                "|" +
                (user?.email ?: "") +
                "|" +
                "v" +
                com.nexauren.audiotools.BuildConfig.VERSION_NAME +
                "|" +
                subject +
                "|" +
                message
        )

        prefs.edit()
            .putStringSet(
                "requests",
                existing
            )
            .apply()
    }

    private fun editName() {
        val field =
            EditText(this).apply {
                setText(
                    auth.currentUser
                        ?.displayName
                        .orEmpty()
                )
                selectAll()
                minHeight =
                    ViewKit.dp(
                        this@AppPagesActivity,
                        52
                    )
            }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Editar nome")
            .setView(field)
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Guardar"
            ) { _, _ ->
                val name =
                    field.text
                        .toString()
                        .trim()

                if (name.isBlank()) {
                    return@setPositiveButton
                }

                val user =
                    auth.currentUser
                        ?: return@setPositiveButton

                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        FirebaseFirestore
                            .getInstance()
                            .collection("users")
                            .document(user.uid)
                            .set(
                                mapOf(
                                    "uid" to user.uid,
                                    "email" to (
                                        user.email
                                            ?: ""
                                    ),
                                    "displayName" to name,
                                    "photoUrl" to (
                                        user.photoUrl
                                            ?.toString()
                                            ?: ""
                                    ),
                                    "updatedAt" to
                                        FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )

                        render(PAGE_PROFILE)
                    } else {
                        Toast.makeText(
                            this,
                            "Não foi possível atualizar o nome.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun updateAvatar(
        uri: Uri
    ) {
        val user =
            auth.currentUser
                ?: return

        val localFile =
            File(
                filesDir,
                "profile_avatar_" +
                    user.uid.hashCode() +
                    ".img"
            )

        runCatching {
            contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    localFile.outputStream()
                        .use { output ->
                            input.copyTo(output)
                        }
                }
        }.onFailure {
            Toast.makeText(
                this,
                "Não foi possível guardar a foto neste dispositivo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        getSharedPreferences(
            "audio_tools_personal",
            MODE_PRIVATE
        ).edit()
            .putString(
                "profile_avatar_file",
                localFile.absolutePath
            )
            .apply()

        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build()
        ).addOnCompleteListener {
            if (!it.isSuccessful) {
                Toast.makeText(
                    this,
                    "Não foi possível atualizar a foto.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnCompleteListener
            }

            FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (
                            user.email
                                ?: ""
                        ),
                        "displayName" to (
                            user.displayName
                                ?: ""
                        ),
                        "photoUrl" to (
                            user.photoUrl
                                ?.toString()
                                ?: uri.toString()
                        ),
                        "updatedAt" to
                            FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

            render(PAGE_PROFILE)
        }
    }

    private fun loadAvatar(
        uri: Uri
    ) {
        networkExecutor.execute {
            runCatching {
                val bitmap =
                    URL(uri.toString())
                        .openStream()
                        .use {
                            BitmapFactory.decodeStream(it)
                        }

                if (bitmap != null) {
                    uiHandler.post {
                        avatar?.setImageBitmap(
                            bitmap
                        )
                    }
                }
            }
        }
    }

    private fun formatDate(
        value: Long?
    ): String =
        value
            ?.takeIf { it > 0 }
            ?.let {
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).format(
                    Date(it)
                )
            }
            ?: "—"

    private fun infoLine(
        label: String,
        value: String
    ): View =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL

            addView(
                TextView(
                    this@AppPagesActivity
                ).apply {
                    text = label
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            this@AppPagesActivity,
                            R.color.audio_muted
                        )
                    )
                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewKit.dp(
                                this@AppPagesActivity,
                                118
                            ),
                            ViewGroup.LayoutParams
                                .WRAP_CONTENT
                        )
                }
            )

            addView(
                TextView(
                    this@AppPagesActivity
                ).apply {
                    text = value
                    textSize = 12.5f
                    maxLines = 4
                    setTextColor(
                        ContextCompat.getColor(
                            this@AppPagesActivity,
                            R.color.audio_text
                        )
                    )
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams
                                .WRAP_CONTENT,
                            1f
                        )
                }
            )

            setPadding(
                0,
                ViewKit.dp(
                    this@AppPagesActivity,
                    5
                ),
                0,
                ViewKit.dp(
                    this@AppPagesActivity,
                    5
                )
            )
        }

    private fun accentFor(
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

    private fun symbolFor(
        id: String
    ): String =
        when (id) {
            "cut" -> "✂"
            "convert" -> "↔"
            "extract" -> "⇲"
            "recorder" -> "●"
            "analyzer" -> "≋"
            "pro-inspector" -> "◇"
            else -> "♪"
        }

    private fun tileColor(
        index: Int
    ): Int =
        when (index % 4) {
            0 -> R.color.audio_blue
            1 -> R.color.audio_green
            2 -> R.color.audio_purple
            else -> R.color.audio_orange
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
}
