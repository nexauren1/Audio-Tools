package com.nexauren.audiotools.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R

class ChangelogActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = ViewKit.page(this)

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
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

        header.addView(
            ViewKit.title(
                this,
                "Changelog",
                24f
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin =
                            ViewKit.dp(this@ChangelogActivity, 10)
                    }
            }
        )

        header.addView(
            ViewKit.pill(
                this,
                BuildConfig.VERSION_NAME,
                colorRes = R.color.audio_blue
            )
        )

        root.addView(header)
        root.addView(ViewKit.spacer(this, 16))

        root.addView(
            ViewKit.hero(this).apply {
                addView(
                    LinearLayout(this@ChangelogActivity).apply {
                        orientation =
                            LinearLayout.VERTICAL
                        setPadding(
                            ViewKit.dp(this@ChangelogActivity, 18),
                            ViewKit.dp(this@ChangelogActivity, 18),
                            ViewKit.dp(this@ChangelogActivity, 18),
                            ViewKit.dp(this@ChangelogActivity, 18)
                        )
                        addView(
                            ViewKit.eyebrow(
                                this@ChangelogActivity,
                                "VERSÃO ATUAL"
                            )
                        )
                        addView(
                            ViewKit.title(
                                this@ChangelogActivity,
                                "Audio Tools " +
                                    BuildConfig.VERSION_NAME,
                                26f
                            )
                        )
                        addView(ViewKit.spacer(this@ChangelogActivity, 5))
                        addView(
                            ViewKit.subtitle(
                                this@ChangelogActivity,
                                "Histórico das melhorias, novas funções e alterações importantes."
                            )
                        )
                    }
                )
            }
        )

        root.addView(ViewKit.spacer(this, 14))
        addVersion(
            root,
            "0.10.0",
            "Nova experiência",
            R.color.audio_blue,
            listOf(
                "Home renovada com pesquisa rápida e estado do plano.",
                "Menu completo com todas as áreas do app.",
                "Favoritos, histórico e estatísticas pessoais.",
                "Me com foto, nome, UID Firebase e dados da conta.",
                "Suporte com formulários para suporte, reclamações e sugestões.",
                "Upgrade com comparação Free, Pro e Premium.",
                "Partilha de resultados de áudio a partir das ferramentas.",
                "versionCode automático no GitHub Actions."
            )
        )

        root.addView(ViewKit.spacer(this, 11))

        addVersion(
            root,
            "0.9.0",
            "Planos por assinatura",
            R.color.audio_purple,
            listOf(
                "Free sem expiração.",
                "Pro por $5/mês.",
                "Premium por $10/mês.",
                "Ferramentas pagas protegidas por entitlement.",
                "PayPal por assinatura.",
                "Remoção do sistema antigo de créditos e compras únicas."
            )
        )

        root.addView(ViewKit.spacer(this, 12))
        root.addView(
            ViewKit.subtitle(
                this,
                "As notas podem crescer junto com o produto. A versão instalada é controlada pelo projeto e o sistema de atualização consulta a release mais recente."
            )
        )

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    ScrollView.OVER_SCROLL_NEVER
                addView(root)
            }
        )
    }

    private fun addVersion(
        root: LinearLayout,
        version: String,
        title: String,
        accent: Int,
        items: List<String>
    ) {
        val card =
            ViewKit.card(
                this,
                accentColorRes = accent
            )

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(this@ChangelogActivity, 15),
                    ViewKit.dp(this@ChangelogActivity, 15),
                    ViewKit.dp(this@ChangelogActivity, 15),
                    ViewKit.dp(this@ChangelogActivity, 15)
                )
            }

        body.addView(
            ViewKit.pill(
                this,
                "v$version",
                colorRes = accent
            )
        )

        body.addView(
            ViewKit.spacer(this, 8)
        )

        body.addView(
            ViewKit.title(
                this,
                title,
                20f
            )
        )

        body.addView(
            ViewKit.spacer(this, 9)
        )

        items.forEach {
            body.addView(
                TextView(this).apply {
                    text = "✓  " + it
                    textSize = 12.5f
                    setLineSpacing(1.08f, 1f)
                    setTextColor(
                        ContextCompat.getColor(
                            this@ChangelogActivity,
                            R.color.audio_text
                        )
                    )
                    setPadding(
                        0,
                        ViewKit.dp(this@ChangelogActivity, 3),
                        0,
                        ViewKit.dp(this@ChangelogActivity, 3)
                    )
                }
            )
        }

        card.addView(body)
        root.addView(card)
    }
}
