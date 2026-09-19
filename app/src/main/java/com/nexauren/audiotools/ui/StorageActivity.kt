package com.nexauren.audiotools.ui

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.R
import com.nexauren.audiotools.storage.NexaurenStorage

class StorageActivity : ComponentActivity() {
    private lateinit var locationView: android.widget.TextView

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            runCatching {
                NexaurenStorage.rememberTree(
                    this,
                    uri
                )
            }.onSuccess {
                refreshLocation()
                android.widget.Toast.makeText(
                    this,
                    "Pasta principal guardada.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }.onFailure {
                android.widget.Toast.makeText(
                    this,
                    "Não foi possível guardar este local. Escolhe uma pasta do armazenamento partilhado.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::locationView.isInitialized) {
            refreshLocation()
        }
    }

    private fun buildUi() {
        val root =
            ViewKit.page(this)

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
                "Armazenamento",
                23f
            ).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin =
                            ViewKit.dp(
                                this@StorageActivity,
                                10
                            )
                    }
            }
        )

        root.addView(header)
        root.addView(
            ViewKit.spacer(this, 18)
        )

        root.addView(
            ViewKit.hero(this).apply {
                addView(
                    LinearLayout(
                        this@StorageActivity
                    ).apply {
                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            ViewKit.dp(
                                this@StorageActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                18
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                18
                            )
                        )

                        addView(
                            ViewKit.eyebrow(
                                this@StorageActivity,
                                "ÁUDIOS DO NEXAUREN"
                            )
                        )

                        addView(
                            ViewKit.spacer(
                                this@StorageActivity,
                                5
                            )
                        )

                        addView(
                            ViewKit.title(
                                this@StorageActivity,
                                "Uma pasta para tudo.",
                                27f
                            )
                        )

                        addView(
                            ViewKit.spacer(
                                this@StorageActivity,
                                5
                            )
                        )

                        addView(
                            ViewKit.subtitle(
                                this@StorageActivity,
                                "Escolhe uma pasta no armazenamento partilhado. O Audio Tools cria dentro dela uma pasta própria para cada ferramenta."
                            )
                        )
                    }
                )
            }
        )

        root.addView(
            ViewKit.spacer(this, 14)
        )

        val card =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_teal
            )

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    ViewKit.dp(this@StorageActivity, 15),
                    ViewKit.dp(this@StorageActivity, 15),
                    ViewKit.dp(this@StorageActivity, 15),
                    ViewKit.dp(this@StorageActivity, 15)
                )
            }

        body.addView(
            ViewKit.eyebrow(
                this,
                "LOCAL ATUAL"
            )
        )

        body.addView(
            ViewKit.spacer(
                this,
                5
            )
        )

        locationView =
            ViewKit.title(
                this,
                "Ainda não escolhida",
                18f
            )

        body.addView(locationView)

        body.addView(
            ViewKit.spacer(
                this,
                6
            )
        )

        body.addView(
            ViewKit.subtitle(
                this,
                "Recomendado: uma pasta como Música/Nexauren. O sistema do Android mantém essa localização fora da área Android/data."
            )
        )

        body.addView(
            ViewKit.spacer(
                this,
                11
            )
        )

        body.addView(
            ViewKit.button(
                this,
                "Escolher pasta",
                true,
                R.color.audio_teal
            ).apply {
                setOnClickListener {
                    launchPicker()
                }
            }
        )

        card.addView(body)
        root.addView(card)

        root.addView(
            ViewKit.spacer(this, 12)
        )

        root.addView(
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_blue
            ).apply {
                addView(
                    LinearLayout(
                        this@StorageActivity
                    ).apply {
                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            ViewKit.dp(
                                this@StorageActivity,
                                15
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                15
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                15
                            ),
                            ViewKit.dp(
                                this@StorageActivity,
                                15
                            )
                        )

                        addView(
                            ViewKit.eyebrow(
                                this@StorageActivity,
                                "ESTRUTURA"
                            )
                        )

                        addView(
                            ViewKit.spacer(
                                this@StorageActivity,
                                7
                            )
                        )

                        val tools =
                            ToolCatalog.tools

                        tools.forEach { tool ->
                            addView(
                                ViewKit.subtitle(
                                    this@StorageActivity,
                                    "• " +
                                        AppStrings.tool(
                                            this@StorageActivity,
                                            tool.id
                                        ).title +
                                        " - Nexauren"
                                )
                            )

                            addView(
                                ViewKit.spacer(
                                    this@StorageActivity,
                                    3
                                )
                            )
                        }
                    }
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

        refreshLocation()
    }

    private fun launchPicker() {
        val intent =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE
            ).apply {
                val current =
                    NexaurenStorage.getTreeUri(
                        this@StorageActivity
                    )

                if (current != null) {
                    putExtra(
                        DocumentsContract.EXTRA_INITIAL_URI,
                        current
                    )
                }
            }

        picker.launch(intent)
    }

    private fun refreshLocation() {
        locationView.text =
            NexaurenStorage.rootLabel(this)
    }
}
