package com.nexauren.audiotools.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.R
import java.util.Locale
import java.util.concurrent.Executors

class ProDemoActivity : ComponentActivity() {
    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                analyze(uri)
            }
        }

    private var status:
        TextView? = null

    private var action:
        com.google.android.material.button.MaterialButton? =
        null

    private var processing:
        CircuitProgressView? = null

    private var selectedUri:
        Uri? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        if (
            FirebaseAuth
                .getInstance()
                .currentUser == null
        ) {
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
            return
        }

        buildUi()
        prepareAccess()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
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
            ViewKit.button(
                this,
                "‹",
                false,
                R.color.audio_purple
            ).apply {
                minWidth =
                    ViewKit.dp(
                        this@ProDemoActivity,
                        48
                    )
                minHeight =
                    ViewKit.dp(
                        this@ProDemoActivity,
                        48
                    )
                contentDescription =
                    AppStrings.t(
                        this@ProDemoActivity,
                        "back"
                    )
                setOnClickListener {
                    finish()
                }
            }
        )

        val titleColumn =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin =
                            ViewKit.dp(
                                this@ProDemoActivity,
                                11
                            )
                    }
            }

        titleColumn.addView(
            ViewKit.eyebrow(
                this,
                "PRO"
            )
        )

        titleColumn.addView(
            ViewKit.title(
                this,
                "Pro Audio Inspector",
                22f
            )
        )

        header.addView(
            titleColumn
        )

        header.addView(
            ViewKit.pill(
                this,
                "PRO ✓",
                colorRes =
                    R.color.audio_purple
            )
        )

        root.addView(header)

        root.addView(
            ViewKit.spacer(
                this,
                22
            )
        )

        root.addView(
            ViewKit.title(
                this,
                "Verificações avançadas do áudio.",
                29f
            )
        )

        root.addView(
            ViewKit.spacer(
                this,
                7
            )
        )

        root.addView(
            ViewKit.subtitle(
                this,
                "Inspeciona metadados técnicos do áudio diretamente no dispositivo. Esta ferramenta exige acesso Pro."
            )
        )

        root.addView(
            ViewKit.spacer(
                this,
                15
            )
        )

        val card =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_purple
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(
                        this@ProDemoActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@ProDemoActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@ProDemoActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@ProDemoActivity,
                        15
                    )
                )
            }

        content.addView(
            ViewKit.pill(
                this,
                "ACESSO PRO",
                colorRes =
                    R.color.audio_purple
            )
        )

        content.addView(
            ViewKit.spacer(
                this,
                9
            )
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Seleciona um ficheiro de áudio para ver duração, formato e bitrate. O ficheiro não é enviado para a internet."
                textSize = 12.5f
                setLineSpacing(
                    1.12f,
                    1f
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@ProDemoActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        content.addView(
            ViewKit.spacer(
                this,
                13
            )
        )

        status =
            TextView(this).apply {
                text =
                    "A verificar o acesso Pro…"
                textSize = 13.5f
                setTextColor(
                    ContextCompat.getColor(
                        this@ProDemoActivity,
                        R.color.audio_text
                    )
                )
            }

        content.addView(status)

        processing =
            CircuitProgressView(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewKit.dp(
                            this@ProDemoActivity,
                            58
                        )
                    )
            }

        content.addView(
            processing
        )

        content.addView(
            ViewKit.spacer(
                this,
                10
            )
        )

        action =
            ViewKit.button(
                this,
                "Escolher áudio",
                true,
                R.color.audio_purple
            ).apply {
                isEnabled = false

                setOnClickListener {
                    if (
                        selectedUri == null
                    ) {
                        picker.launch(
                            arrayOf(
                                "audio/*"
                            )
                        )
                    } else {
                        analyze(
                            selectedUri!!
                        )
                    }
                }
            }

        content.addView(action)

        content.addView(
            ViewKit.spacer(
                this,
                8
            )
        )

        content.addView(
            ViewKit.button(
                this,
                "Copiar resultado",
                false,
                R.color.audio_purple
            ).apply {
                setOnClickListener {
                    val value =
                        status?.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                    if (
                        value.isBlank() ||
                        value == "A verificar o acesso Pro…"
                    ) {
                        Toast.makeText(
                            this@ProDemoActivity,
                            "Ainda não existe um resultado para copiar.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val clipboard =
                        getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as android.content.ClipboardManager

                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "Audio Tools",
                            value
                        )
                    )

                    Toast.makeText(
                        this@ProDemoActivity,
                        "Resultado copiado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        card.addView(content)
        root.addView(card)

        root.addView(
            ViewKit.spacer(
                this,
                14
            )
        )

        root.addView(
            TextView(this).apply {
                text =
                    "O acesso é conferido novamente pelo Worker antes de a ferramenta ser usada."
                textSize = 11.5f
                setLineSpacing(
                    1.08f,
                    1f
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@ProDemoActivity,
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
    }

    private fun prepareAccess() {
        val entitlement =
            PlanAccessStore.current(this)

        if (
            entitlement.hasAccess("PRO")
        ) {
            status?.text =
                "Pro ativo."
            action?.isEnabled =
                true
        } else {
            openUpgrade()
        }
    }

    private fun openUpgrade() {
        startActivity(
            UpgradeActivity.intent(
                this,
                "PRO"
            )
        )
        finish()
    }

    private fun analyze(
        uri: Uri
    ) {
        selectedUri = uri

        processing?.start()

        action?.isEnabled =
            false

        status?.text =
            "A analisar…"

        executor.execute {
            try {
                val report =
                    buildReport(uri)

                mainHandler.post {
                    processing?.stop()

                    action?.isEnabled =
                        true

                    UsageStore.record(
                        this@ProDemoActivity,
                        "pro-inspector"
                    )

                    status?.text =
                        report
                }
            } catch (error: Exception) {
                mainHandler.post {
                    processing?.stop()

                    action?.isEnabled =
                        true

                    status?.text =
                        "Não foi possível analisar este ficheiro."
                }
            }
        }
    }

    private fun buildReport(
        uri: Uri
    ): String {
        val retriever =
            MediaMetadataRetriever()

        return try {
            retriever.setDataSource(
                this,
                uri
            )

            val name =
                displayName(uri)

            val duration =
                retriever.extractMetadata(
                    MediaMetadataRetriever
                        .METADATA_KEY_DURATION
                )?.toLongOrNull()
                    ?: 0L

            val mime =
                retriever.extractMetadata(
                    MediaMetadataRetriever
                        .METADATA_KEY_MIMETYPE
                ).orEmpty()
                    .ifBlank {
                        "—"
                    }

            val bitrate =
                retriever.extractMetadata(
                    MediaMetadataRetriever
                        .METADATA_KEY_BITRATE
                )?.toLongOrNull()

            buildString {
                append(name)
                append("\n\n")

                append("Duração: ")
                append(
                    formatDuration(
                        duration
                    )
                )
                append("\n")

                append("Formato: ")
                append(mime)
                append("\n")

                append("Bitrate: ")
                append(
                    if (
                        bitrate != null &&
                        bitrate > 0
                    ) {
                        (
                            bitrate / 1000
                        ).toString() +
                        " kbps"
                    } else {
                        "—"
                    }
                )
            }
        } finally {
            retriever.release()
        }
    }

    private fun formatDuration(
        ms: Long
    ): String {
        val totalSeconds =
            (ms / 1000L)
                .coerceAtLeast(0L)

        return String.format(
            Locale.US,
            "%02d:%02d",
            totalSeconds / 60,
            totalSeconds % 60
        )
    }

    private fun displayName(
        uri: Uri
    ): String {
        contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns
                    .DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (
                cursor.moveToFirst()
            ) {
                val index =
                    cursor.getColumnIndex(
                        OpenableColumns
                            .DISPLAY_NAME
                    )

                if (index >= 0) {
                    return cursor.getString(
                        index
                    )
                }
            }
        }

        return uri.lastPathSegment
            ?: "audio"
    }

    companion object {
        private const val EXTRA_UNUSED =
            "pro_tool"

        fun intent(
            context: Context
        ): Intent =
            Intent(
                context,
                ProDemoActivity::class.java
            ).putExtra(
                EXTRA_UNUSED,
                true
            )
    }
}
