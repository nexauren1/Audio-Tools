package com.nexauren.audiotools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog

class ToolDetailActivity : ComponentActivity() {
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            Toast.makeText(this, "Selecionado: " + displayName(uri), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tool = ToolCatalog.get(intent.getStringExtra(EXTRA_TOOL_ID).orEmpty())
        if (tool == null) {
            finish()
            return
        }
        buildUi(tool)
    }

    private fun buildUi(tool: AudioTool) {
        val root = ViewKit.page(this)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(MaterialButton(this).apply {
            text = "‹"
            contentDescription = "Voltar"
            minWidth = ViewKit.dp(this@ToolDetailActivity, 48)
            minHeight = ViewKit.dp(this@ToolDetailActivity, 48)
            cornerRadius = ViewKit.dp(this@ToolDetailActivity, 15)
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            setBackgroundColor(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = tool.category.uppercase()
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.1f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@ToolDetailActivity, 14)
            }
        })
        root.addView(header)
        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.pill(this, "Ferramenta " + tool.number))
        root.addView(ViewKit.spacer(this, 12))
        root.addView(ViewKit.title(this, tool.title, 30f))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(ViewKit.subtitle(this, tool.description))

        root.addView(ViewKit.spacer(this, 20))
        val action = ViewKit.card(this)
        val actionContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18)
            )
        }
        actionContent.addView(ViewKit.title(this, "Começar", 18f))
        actionContent.addView(ViewKit.spacer(this, 6))
        actionContent.addView(ViewKit.subtitle(this, "A página já está isolada do catálogo. O motor específico pode ser ligado aqui sem refazer a navegação."))
        actionContent.addView(ViewKit.spacer(this, 14))
        actionContent.addView(ViewKit.button(this, "Selecionar ficheiro", true).apply {
            setOnClickListener { picker.launch(arrayOf("audio/*", "video/*")) }
        })
        actionContent.addView(ViewKit.spacer(this, 8))
        actionContent.addView(TextView(this).apply {
            text = "Base de processamento preparada para a implementação do motor desta ferramenta."
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        })
        action.addView(actionContent)
        root.addView(action)

        root.addView(ViewKit.spacer(this, 20))
        root.addView(sectionTitle("Como funciona"))
        root.addView(ViewKit.spacer(this, 8))
        tool.steps.forEachIndexed { index, step ->
            val stepCard = ViewKit.card(this)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    ViewKit.dp(this@ToolDetailActivity, 16),
                    ViewKit.dp(this@ToolDetailActivity, 14),
                    ViewKit.dp(this@ToolDetailActivity, 16),
                    ViewKit.dp(this@ToolDetailActivity, 14)
                )
            }
            row.addView(TextView(this).apply {
                text = (index + 1).toString()
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_primary))
                setBackgroundColor(android.graphics.Color.parseColor("#E9EFFF"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewKit.dp(this@ToolDetailActivity, 36),
                    ViewKit.dp(this@ToolDetailActivity, 36)
                ).apply { rightMargin = ViewKit.dp(this@ToolDetailActivity, 12) }
            })
            row.addView(TextView(this).apply {
                text = step
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            stepCard.addView(row)
            root.addView(stepCard)
            root.addView(ViewKit.spacer(this, 8))
        }

        val specCard = ViewKit.card(this)
        val spec = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18)
            )
        }
        spec.addView(sectionTitle("Especificações"))
        spec.addView(ViewKit.spacer(this, 10))
        spec.addView(specText("Entrada", tool.input))
        spec.addView(ViewKit.spacer(this, 8))
        spec.addView(specText("Saída", tool.output))
        specCard.addView(spec)
        root.addView(specCard)

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun specText(label: String, value: String): TextView =
        TextView(this).apply {
            text = "$label  •  $value"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment ?: "ficheiro"
    }

    companion object {
        private const val EXTRA_TOOL_ID = "tool_id"

        fun intent(context: Context, toolId: String): Intent =
            Intent(context, ToolDetailActivity::class.java).putExtra(EXTRA_TOOL_ID, toolId)
    }
}