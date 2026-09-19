package com.nexauren.audiotools.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nexauren.audiotools.R
import com.nexauren.audiotools.catalog.AudioTool
import com.nexauren.audiotools.catalog.ToolCatalog
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ToolDetailActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        when (activeToolId) {
            "cut" -> onCutFileSelected(uri)
            "convert" -> onConverterFileSelected(uri)
            "analyzer" -> onAnalyzerFileSelected(uri)
        }
    }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecordingInternal()
            else statusView?.text = AppStrings.t(this, "permission_mic")
        }

    private var activeToolId: String = ""
    private var selectedUri: Uri? = null
    private var selectedDurationMs: Long = 0L
    private var lastOutputPath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordingStartedAt = 0L
    private var timerRunnable: Runnable? = null

    private var statusView: TextView? = null
    private var primaryAction: MaterialButton? = null
    private var playbackAction: MaterialButton? = null
    private var signalPreview: SignalPreviewView? = null
    private var trimView: WaveformTrimView? = null
    private var trimStartInput: TextInputEditText? = null
    private var trimEndInput: TextInputEditText? = null
    private var convertTarget = "m4a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeToolId = intent.getStringExtra(EXTRA_TOOL_ID).orEmpty()
        val tool = ToolCatalog.get(activeToolId)
        if (tool == null) {
            finish()
            return
        }
        buildUi(tool)
    }

    override fun onDestroy() {
        stopTimer()
        releasePlayer()
        releaseRecorder()
        signalPreview?.stop()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(tool: AudioTool) {
        val copy = AppStrings.tool(this, tool.id)
        val root = ViewKit.page(this)
        val accent = accentFor(tool.id)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ViewKit.button(this, "‹", false, accent).apply {
            minWidth = ViewKit.dp(this@ToolDetailActivity, 48)
            minHeight = ViewKit.dp(this@ToolDetailActivity, 48)
            contentDescription = AppStrings.t(this@ToolDetailActivity, "back")
            setOnClickListener { finish() }
        })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@ToolDetailActivity, 11)
            }
            addView(ViewKit.eyebrow(this@ToolDetailActivity, copy.category))
            addView(TextView(this@ToolDetailActivity).apply {
                text = "N° " + tool.number
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@ToolDetailActivity, accent))
            })
        })
        header.addView(ViewKit.pill(this, when (tool.id) {
            "cut" -> "EDIT"
            "convert" -> "↔"
            "recorder" -> AppStrings.t(this, "live")
            else -> "SCAN"
        }, colorRes = accent))
        root.addView(header)
        root.addView(ViewKit.spacer(this, 20))

        val title = TextView(this).apply {
            text = copy.title
            textSize = 29f
            maxLines = 2
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
        }
        root.addView(title)
        root.addView(ViewKit.spacer(this, 6))
        root.addView(ViewKit.subtitle(this, copy.detail))
        root.addView(ViewKit.spacer(this, 14))

        signalPreview = SignalPreviewView(this, accent, live = tool.id == "recorder").apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewKit.dp(this@ToolDetailActivity, 104))
            if (tool.id != "recorder") start()
        }
        root.addView(signalPreview)

        root.addView(ViewKit.spacer(this, 15))
        when (tool.id) {
            "cut" -> buildCutAction(root)
            "convert" -> buildConverterAction(root)
            "recorder" -> buildRecorderAction(root)
            "analyzer" -> buildAnalyzerAction(root)
        }

        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.sectionLabel(this, AppStrings.t(this, "how")))
        root.addView(ViewKit.spacer(this, 7))
        copy.steps.forEachIndexed { index, step ->
            root.addView(stepCard(index + 1, step, accent))
            root.addView(ViewKit.spacer(this, 7))
        }

        val specCard = ViewKit.card(this, accentColorRes = accent)
        val spec = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15))
        }
        spec.addView(ViewKit.sectionLabel(this, AppStrings.t(this, "compatibility")))
        spec.addView(ViewKit.spacer(this, 8))
        spec.addView(specText(AppStrings.t(this, "input"), copy.input))
        spec.addView(ViewKit.spacer(this, 7))
        spec.addView(specText(AppStrings.t(this, "output"), copy.output))
        specCard.addView(spec)
        root.addView(ViewKit.spacer(this, 10))
        root.addView(specCard)

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
            addView(root)
        })
    }

    private fun buildCutAction(root: LinearLayout) {
        val card = ViewKit.card(this, accentColorRes = R.color.audio_blue)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15))
        }
        content.addView(ViewKit.pill(this, AppStrings.t(this, "cut_precise"), colorRes = R.color.audio_blue))
        content.addView(ViewKit.spacer(this, 8))
        content.addView(TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "cut_info")
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        })
        content.addView(ViewKit.spacer(this, 10))

        statusView = TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "choose_audio")
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
        }
        content.addView(statusView)
        content.addView(ViewKit.spacer(this, 8))

        content.addView(ViewKit.button(this, AppStrings.t(this, "choose_audio"), true, R.color.audio_blue).apply {
            setOnClickListener { filePicker.launch(arrayOf("audio/mp4", "audio/aac", "audio/x-m4a", "audio/*")) }
        })
        content.addView(ViewKit.spacer(this, 10))

        trimView = WaveformTrimView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewKit.dp(this@ToolDetailActivity, 122))
            onRangeChanged = { start, end ->
                trimStartInput?.setTextWithoutMovingCursor(formatSeconds(start), null)
                trimEndInput?.setTextWithoutMovingCursor(formatSeconds(end), null)
            }
        }
        content.addView(trimView)

        val fields = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val startField = timeField(if (LanguageManager.get(this@ToolDetailActivity) == "pt") "Início (s)" else "Start (s)", "0")
        val endField = timeField(if (LanguageManager.get(this@ToolDetailActivity) == "pt") "Fim (s)" else "End (s)", "0")
        trimStartInput = startField.second
        trimEndInput = endField.second
        fields.addView(startField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = ViewKit.dp(this@ToolDetailActivity, 7) })
        fields.addView(endField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(ViewKit.spacer(this, 9))
        content.addView(fields)

        val progress = ProgressBar(this).apply { visibility = View.GONE }
        content.addView(ViewKit.spacer(this, 5))
        content.addView(progress)

        primaryAction = ViewKit.button(this, AppStrings.t(this, "cut_action"), true, R.color.audio_blue).apply {
            isEnabled = false
            setOnClickListener {
                val uri = selectedUri ?: return@setOnClickListener
                val start = trimStartInput?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                val end = trimEndInput?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                if (start < 0.0 || end <= start || end * 1000.0 > selectedDurationMs) {
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "cut_bad")
                    return@setOnClickListener
                }
                performCut(uri, (start * 1000).toLong(), (end * 1000).toLong(), progress)
            }
        }
        content.addView(ViewKit.spacer(this, 8))
        content.addView(primaryAction)

        playbackAction = ViewKit.button(this, AppStrings.t(this, "original"), false, R.color.audio_blue).apply {
            visibility = MaterialButton.GONE
            setOnClickListener {
                lastOutputPath?.let { playFile(File(it)) } ?: selectedUri?.let { playUri(it) }
            }
        }
        content.addView(ViewKit.spacer(this, 7))
        content.addView(playbackAction)
        card.addView(content)
        root.addView(card)

        trimStartInput?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) syncTrimFromFields()
        }
        trimEndInput?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) syncTrimFromFields()
        }
    }

    private fun syncTrimFromFields() {
        val start = trimStartInput?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        val end = trimEndInput?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        if (selectedDurationMs > 0L && end > start) {
            trimView?.setRange((start * 1000).toLong().coerceAtLeast(0L), (end * 1000).toLong().coerceAtMost(selectedDurationMs))
        }
    }

    private fun buildConverterAction(root: LinearLayout) {
        val card = ViewKit.card(this, accentColorRes = R.color.audio_purple)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15))
        }
        content.addView(ViewKit.pill(this, AppStrings.t(this, "converter_pill"), colorRes = R.color.audio_purple))
        content.addView(ViewKit.spacer(this, 8))
        content.addView(TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "supported")
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        })
        content.addView(ViewKit.spacer(this, 9))
        statusView = TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "select_source")
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
        }
        content.addView(statusView)
        content.addView(ViewKit.spacer(this, 9))
        content.addView(ViewKit.button(this, AppStrings.t(this, "select_source"), true, R.color.audio_purple).apply {
            setOnClickListener { filePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "audio/mp4", "audio/aac", "audio/x-m4a")) }
        })
        content.addView(ViewKit.spacer(this, 11))

        val targetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val wavButton = ViewKit.button(this, "WAV", false, R.color.audio_purple).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = ViewKit.dp(this@ToolDetailActivity, 7) }
            setOnClickListener { setConvertTarget("wav") }
        }
        val m4aButton = ViewKit.button(this, "M4A / AAC", true, R.color.audio_purple).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { setConvertTarget("m4a") }
        }
        targetRow.addView(wavButton)
        targetRow.addView(m4aButton)
        content.addView(targetRow)

        content.addView(ViewKit.spacer(this, 6))
        content.addView(TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "converter_tip")
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        })

        val progress = ProgressBar(this).apply { visibility = View.GONE }
        content.addView(ViewKit.spacer(this, 6))
        content.addView(progress)
        primaryAction = ViewKit.button(this, AppStrings.t(this, "convert_action"), true, R.color.audio_purple).apply {
            isEnabled = false
            setOnClickListener {
                val uri = selectedUri ?: return@setOnClickListener
                performConversion(uri, convertTarget, progress)
            }
        }
        content.addView(ViewKit.spacer(this, 7))
        content.addView(primaryAction)

        playbackAction = ViewKit.button(this, "▶", false, R.color.audio_purple).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { lastOutputPath?.let { playFile(File(it)) } }
        }
        content.addView(ViewKit.spacer(this, 7))
        content.addView(playbackAction)

        card.addView(content)
        root.addView(card)

        setConvertTarget("m4a")
        // Keep references indirectly by updating button state from the target selector.
        wavButton.tag = "wav"
        m4aButton.tag = "m4a"
        content.tag = targetRow
    }

    private fun setConvertTarget(target: String) {
        convertTarget = target
        val row = primaryAction?.parent?.parent?.parent as? LinearLayout ?: return
        val targetRow = row.tag as? LinearLayout ?: return
        val wav = targetRow.getChildAt(0) as? MaterialButton
        val m4a = targetRow.getChildAt(1) as? MaterialButton
        if (target == "wav") {
            wav?.apply { backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_purple)); setTextColor(android.graphics.Color.WHITE) }
            m4a?.apply { backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_surface)); setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_purple)) }
        } else {
            m4a?.apply { backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_purple)); setTextColor(android.graphics.Color.WHITE) }
            wav?.apply { backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_surface)); setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_purple)) }
        }
    }

    private fun buildRecorderAction(root: LinearLayout) {
        val card = ViewKit.card(this, accentColorRes = R.color.audio_red)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15))
        }
        content.addView(ViewKit.pill(this, AppStrings.t(this, "monitoring"), colorRes = R.color.audio_red))
        content.addView(ViewKit.spacer(this, 10))
        statusView = TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "record_ready")
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            gravity = Gravity.CENTER
        }
        content.addView(statusView)
        content.addView(ViewKit.spacer(this, 11))
        primaryAction = ViewKit.button(this, AppStrings.t(this, "record_start"), true, R.color.audio_red).apply {
            setOnClickListener { if (mediaRecorder == null) startRecording() else stopRecording() }
        }
        content.addView(primaryAction)
        playbackAction = ViewKit.button(this, AppStrings.t(this, "record_play"), false, R.color.audio_red).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { lastOutputPath?.let { playFile(File(it)) } }
        }
        content.addView(ViewKit.spacer(this, 7))
        content.addView(playbackAction)
        card.addView(content)
        root.addView(card)
    }

    private fun buildAnalyzerAction(root: LinearLayout) {
        val card = ViewKit.card(this, accentColorRes = R.color.audio_yellow)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15), ViewKit.dp(this@ToolDetailActivity, 15))
        }
        content.addView(ViewKit.pill(this, AppStrings.t(this, "report"), colorRes = R.color.audio_yellow))
        content.addView(ViewKit.spacer(this, 9))
        statusView = TextView(this).apply {
            text = AppStrings.t(this@ToolDetailActivity, "select_for_report")
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        }
        content.addView(statusView)
        content.addView(ViewKit.spacer(this, 9))
        content.addView(ViewKit.button(this, AppStrings.t(this, "analyze_action"), true, R.color.audio_yellow).apply {
            setOnClickListener { filePicker.launch(arrayOf("audio/*", "video/mp4")) }
        })
        playbackAction = ViewKit.button(this, AppStrings.t(this, "playing"), false, R.color.audio_yellow).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { selectedUri?.let { playUri(it) } }
        }
        content.addView(ViewKit.spacer(this, 7))
        content.addView(playbackAction)
        card.addView(content)
        root.addView(card)
    }

    private fun timeField(hint: String, value: String): Pair<TextInputLayout, TextInputEditText> {
        val input = TextInputEditText(this).apply {
            setText(value)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 14f
        }
        val layout = TextInputLayout(this).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(input)
        }
        return layout to input
    }

    private fun stepCard(number: Int, step: String, accent: Int): ViewGroup {
        val card = ViewKit.card(this, accentColorRes = accent)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ViewKit.dp(this@ToolDetailActivity, 12), ViewKit.dp(this@ToolDetailActivity, 10), ViewKit.dp(this@ToolDetailActivity, 12), ViewKit.dp(this@ToolDetailActivity, 10))
        }
        row.addView(ViewKit.iconBadge(this, "%02d".format(number), accent).apply {
            layoutParams = LinearLayout.LayoutParams(ViewKit.dp(this@ToolDetailActivity, 38), ViewKit.dp(this@ToolDetailActivity, 38)).apply { rightMargin = ViewKit.dp(this@ToolDetailActivity, 10) }
        })
        row.addView(TextView(this).apply {
            text = step
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(row)
        return card
    }

    private fun onCutFileSelected(uri: Uri) {
        selectedUri = uri
        lastOutputPath = null
        playbackAction?.text = AppStrings.t(this, "original")
        worker.execute {
            val duration = try { readDuration(uri) } catch (_: Exception) { 0L }
            mainHandler.post {
                selectedDurationMs = duration
                trimView?.durationMs = duration
                trimView?.resetRange()
                statusView?.text = AppStrings.t(this@ToolDetailActivity, "selected") + ": " + displayName(uri)
                primaryAction?.isEnabled = duration > 0L
                trimStartInput?.setText("0.00")
                trimEndInput?.setText(String.format(Locale.US, "%.2f", duration / 1000.0))
            }
        }
    }

    private fun performCut(uri: Uri, startMs: Long, endMs: Long, progress: ProgressBar) {
        primaryAction?.isEnabled = false
        progress.visibility = View.VISIBLE
        statusView?.text = AppStrings.t(this, "cutting")
        worker.execute {
            try {
                val sourceName = displayName(uri).substringBeforeLast('.')
                val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioTools").apply { mkdirs() }
                val output = File(outputDir, sourceName + "_cut_" + System.currentTimeMillis() + ".m4a")
                trimAac(uri, startMs * 1000L, endMs * 1000L, output)
                lastOutputPath = output.absolutePath
                mainHandler.post {
                    progress.visibility = View.GONE
                    primaryAction?.isEnabled = true
                    playbackAction?.visibility = View.VISIBLE
                    playbackAction?.text = AppStrings.t(this@ToolDetailActivity, "result")
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "cut_ok") + " • " + formatDuration(endMs - startMs) + " • " + output.name
                }
            } catch (_: Exception) {
                mainHandler.post {
                    progress.visibility = View.GONE
                    primaryAction?.isEnabled = true
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "cut_error")
                }
            }
        }
    }

    private fun trimAac(uri: Uri, startUs: Long, endUs: Long, output: File) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(this, uri, null)
            var trackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    trackIndex = i
                    break
                }
            }
            require(trackIndex >= 0)
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            require(format.getString(MediaFormat.KEY_MIME).orEmpty() == "audio/mp4a-latm")
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(format)
            muxer.start()
            muxerStarted = true
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) max(64 * 1024, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)) else 1024 * 1024
            val buffer = java.nio.ByteBuffer.allocate(maxInput)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0L || sampleTime > endUs) break
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size <= 0) break
                info.offset = 0
                info.size = size
                info.flags = extractor.sampleFlags
                info.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                muxer.writeSampleData(outTrack, buffer, info)
                extractor.advance()
            }
        } finally {
            if (muxerStarted) try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            extractor.release()
        }
    }

    private fun onConverterFileSelected(uri: Uri) {
        selectedUri = uri
        lastOutputPath = null
        val name = displayName(uri)
        val lower = name.lowercase(Locale.US)
        val isWav = lower.endsWith(".wav") || lower.endsWith(".wave")
        convertTarget = if (isWav) "m4a" else "wav"
        setConvertTarget(convertTarget)
        statusView?.text = AppStrings.t(this, "selected") + ": " + name
        primaryAction?.isEnabled = true
        playbackAction?.visibility = View.GONE
    }

    private fun performConversion(uri: Uri, target: String, progress: ProgressBar) {
        primaryAction?.isEnabled = false
        progress.visibility = View.VISIBLE
        statusView?.text = AppStrings.t(this, "conversion_progress")
        worker.execute {
            try {
                val sourceName = displayName(uri).substringBeforeLast('.').ifBlank { "audio" }
                val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioTools").apply { mkdirs() }
                val output = File(outputDir, sourceName + "_converted_" + System.currentTimeMillis() + "." + target)
                if (target == "m4a") wavToM4a(uri, output) else m4aToWav(uri, output)
                lastOutputPath = output.absolutePath
                mainHandler.post {
                    progress.visibility = View.GONE
                    primaryAction?.isEnabled = true
                    playbackAction?.visibility = View.VISIBLE
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "converted") + " • " + output.name
                    playbackAction?.text = if (LanguageManager.get(this@ToolDetailActivity) == "pt") "▶ Reproduzir resultado" else "▶ Play result"
                }
            } catch (_: Exception) {
                mainHandler.post {
                    progress.visibility = View.GONE
                    primaryAction?.isEnabled = true
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "conversion_error")
                }
            }
        }
    }

    private data class WavInfo(val sampleRate: Int, val channels: Int, val bits: Int, val dataOffset: Long, val dataSize: Long)

    private fun parseWav(input: DataInputStream): WavInfo {
        val riff = ByteArray(4)
        input.readFully(riff)
        require(String(riff, Charsets.US_ASCII) == "RIFF")
        readIntLE(input)
        input.readFully(riff)
        require(String(riff, Charsets.US_ASCII) == "WAVE")
        var sampleRate = 0
        var channels = 0
        var bits = 0
        var pcm = false
        var dataOffset = -1L
        var dataSize = -1L
        var bytesSeen = 12L
        while (dataOffset < 0L && bytesSeen < 64L * 1024L) {
            input.readFully(riff)
            val size = readIntLE(input)
            bytesSeen += 8
            val tag = String(riff, Charsets.US_ASCII)
            when (tag) {
                "fmt " -> {
                    val fmt = ByteArray(size)
                    input.readFully(fmt)
                    bytesSeen += size.toLong()
                    val format = littleShort(fmt, 0)
                    channels = littleShort(fmt, 2)
                    sampleRate = littleInt(fmt, 4)
                    bits = littleShort(fmt, 14)
                    pcm = format == 1
                }
                "data" -> {
                    dataOffset = bytesSeen
                    dataSize = size.toLong()
                    break
                }
                else -> {
                    input.skipBytes(size)
                    bytesSeen += size.toLong()
                }
            }
            if (size % 2 != 0) {
                input.skipBytes(1)
                bytesSeen++
            }
        }
        require(pcm && channels in 1..2 && sampleRate > 0 && bits == 16 && dataOffset >= 0L)
        return WavInfo(sampleRate, channels, bits, dataOffset, dataSize)
    }

    private fun wavToM4a(uri: Uri, output: File) {
        val stream = BufferedInputStream(contentResolver.openInputStream(uri) ?: error("open failed"))
        val input = DataInputStream(stream)
        val info = parseWav(input)
        val encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", info.sampleRate, info.channels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, (info.sampleRate * info.channels * 2 * 4).coerceIn(64000, 256000))
            setInteger(MediaFormat.KEY_AAC_PROFILE, 2)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var track = -1
        var inputDone = false
        var outputDone = false
        var ptsUs = 0L
        val buffer = ByteArray(64 * 1024)
        var bytesRemaining = info.dataSize

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val index = encoder.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val inBuffer = encoder.getInputBuffer(index) ?: error("no input")
                        inBuffer.clear()
                        val want = min(min(inBuffer.remaining(), buffer.size.toLong()), bytesRemaining).toInt()
                        val read = if (want > 0) input.read(buffer, 0, want) else 0
                        if (read > 0) {
                            inBuffer.put(buffer, 0, read)
                            encoder.queueInputBuffer(index, 0, read, ptsUs, 0)
                            ptsUs += (read.toLong() * 1_000_000L) / (info.sampleRate * info.channels * 2L)
                            bytesRemaining -= read
                        } else {
                            encoder.queueInputBuffer(index, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }
                val outInfo = MediaCodec.BufferInfo()
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(outInfo, 0)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            require(track < 0)
                            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                            track = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val outBuffer = encoder.getOutputBuffer(outIndex)
                            if (outBuffer != null && outInfo.size > 0 && outInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                outBuffer.position(outInfo.offset)
                                outBuffer.limit(outInfo.offset + outInfo.size)
                                muxer?.writeSampleData(track, outBuffer, outInfo)
                            }
                            outputDone = outInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            encoder.releaseOutputBuffer(outIndex, false)
                            if (outputDone) break
                        }
                    }
                }
            }
        } finally {
            input.close()
            try { encoder.stop() } catch (_: Exception) {}
            encoder.release()
            if (muxerStarted) try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
        require(output.exists() && output.length() > 0L)
    }

    private fun m4aToWav(uri: Uri, output: File) {
        val extractor = MediaExtractor()
        val decoder: MediaCodec
        try {
            extractor.setDataSource(this, uri, null)
            var trackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    trackIndex = i
                    trackFormat = f
                    break
                }
            }
            require(trackIndex >= 0 && trackFormat != null)
            val mime = trackFormat!!.getString(MediaFormat.KEY_MIME) ?: error("no mime")
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()
            extractor.selectTrack(trackIndex)

            RandomAccessFile(output, "rw").use { raf ->
                raf.setLength(0)
                repeat(44) { raf.writeByte(0) }
                var inputDone = false
                var outputDone = false
                var dataBytes = 0L
                var sampleRate = trackFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = trackFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex = decoder.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            val inBuffer = decoder.getInputBuffer(inputIndex) ?: error("no decoder input")
                            val sampleTime = extractor.sampleTime
                            if (sampleTime < 0L) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val size = extractor.readSampleData(inBuffer, 0)
                                decoder.queueInputBuffer(inputIndex, 0, size.coerceAtLeast(0), sampleTime, extractor.sampleFlags)
                                extractor.advance()
                            }
                        }
                    }

                    val info = MediaCodec.BufferInfo()
                    val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val f = decoder.outputFormat
                            sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        outputIndex >= 0 -> {
                            val out = decoder.getOutputBuffer(outputIndex)
                            if (out != null && info.size > 0) {
                                out.position(info.offset)
                                out.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size)
                                out.get(bytes)
                                raf.write(bytes)
                                dataBytes += bytes.size
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            decoder.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
                writeWavHeader(raf, dataBytes, sampleRate, channels)
            }
        } finally {
            try { decoder.stop() } catch (_: Exception) {}
            try { decoder.release() } catch (_: Exception) {}
            extractor.release()
        }
    }

    private fun writeWavHeader(raf: RandomAccessFile, dataSize: Long, sampleRate: Int, channels: Int) {
        val byteRate = sampleRate * channels * 2
        val blockAlign = channels * 2
        raf.seek(0)
        raf.writeBytes("RIFF")
        writeIntLE(raf, (36L + dataSize).toInt())
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        writeIntLE(raf, 16)
        writeShortLE(raf, 1)
        writeShortLE(raf, channels)
        writeIntLE(raf, sampleRate)
        writeIntLE(raf, byteRate)
        writeShortLE(raf, blockAlign)
        writeShortLE(raf, 16)
        raf.writeBytes("data")
        writeIntLE(raf, dataSize.toInt())
    }

    private fun readIntLE(input: DataInputStream): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        val b2 = input.readUnsignedByte()
        val b3 = input.readUnsignedByte()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun littleInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun littleShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun writeIntLE(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value ushr 8) and 0xFF)
        raf.writeByte((value ushr 16) and 0xFF)
        raf.writeByte((value ushr 24) and 0xFF)
    }

    private fun writeShortLE(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value ushr 8) and 0xFF)
    }

    private fun onAnalyzerFileSelected(uri: Uri) {
        selectedUri = uri
        statusView?.text = AppStrings.t(this, "analyzing") + " " + displayName(uri) + "…"
        playbackAction?.visibility = View.GONE
        worker.execute {
            try {
                val report = analyzeAudio(uri)
                mainHandler.post {
                    statusView?.text = report
                    playbackAction?.visibility = View.VISIBLE
                    playbackAction?.text = "▶"
                }
            } catch (_: Exception) {
                mainHandler.post { statusView?.text = AppStrings.t(this@ToolDetailActivity, "analysis_error") }
            }
        }
    }

    private fun analyzeAudio(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            retriever.setDataSource(this, uri)
            extractor.setDataSource(this, uri, null)
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    audioFormat = format
                    break
                }
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val mime = audioFormat?.getString(MediaFormat.KEY_MIME) ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "—"
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val channels = audioFormat?.let { intValue(it, MediaFormat.KEY_CHANNEL_COUNT) }
            val sampleRate = audioFormat?.let { intValue(it, MediaFormat.KEY_SAMPLE_RATE) }
            return buildString {
                append(displayName(uri)); append("\n")
                append(AppStrings.t(this@ToolDetailActivity, "duration")); append("  •  "); append(formatDuration(durationMs)); append("\n")
                append(AppStrings.t(this@ToolDetailActivity, "format")); append("  •  "); append(mime); append("\n")
                append(AppStrings.t(this@ToolDetailActivity, "bitrate")); append("  •  "); append(if (bitrate != null && bitrate > 0) bitrate / 1000 else "—"); append(" kbps\n")
                append(AppStrings.t(this@ToolDetailActivity, "channels")); append("  •  "); append(channels ?: "—"); append("\n")
                append(AppStrings.t(this@ToolDetailActivity, "sample_rate")); append("  •  "); append(if (sampleRate != null && sampleRate > 0) sampleRate else "—"); append(" Hz")
            }
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    private fun intValue(format: MediaFormat, key: String): Int? =
        try { if (format.containsKey(key)) format.getInteger(key) else null } catch (_: Exception) { null }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0L)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatSeconds(ms: Long): String = String.format(Locale.US, "%.2f", ms / 1000.0)

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startRecordingInternal()
    }

    private fun startRecordingInternal() {
        releasePlayer()
        stopTimer()
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings").apply { mkdirs() }
        val name = "AudioTools_Record_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"
        val output = File(dir, name)
        try {
            mediaRecorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            lastOutputPath = output.absolutePath
            recordingStartedAt = System.currentTimeMillis()
            primaryAction?.text = AppStrings.t(this, "record_stop")
            primaryAction?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.audio_red_dark))
            statusView?.text = AppStrings.t(this, "record_start") + " • 00:00"
            signalPreview?.start()
            timerRunnable = object : Runnable {
                override fun run() {
                    val seconds = ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt()
                    val amplitude = try { mediaRecorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                    signalPreview?.setLevel((amplitude / 32767f).coerceIn(0f, 1f))
                    statusView?.text = AppStrings.t(this@ToolDetailActivity, "record_start") + " • %02d:%02d".format(seconds / 60, seconds % 60)
                    mainHandler.postDelayed(this, 160)
                }
            }.also { mainHandler.post(it) }
        } catch (_: Exception) {
            output.delete()
            releaseRecorder()
            statusView?.text = AppStrings.t(this, "recording_error")
        }
    }

    private fun stopRecording() {
        stopTimer()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            primaryAction?.text = AppStrings.t(this, "record_start")
            primaryAction?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.audio_red))
            signalPreview?.stop()
            playbackAction?.visibility = View.VISIBLE
            statusView?.text = AppStrings.t(this, "recording_saved")
        } catch (_: Exception) {
            releaseRecorder()
            lastOutputPath?.let { File(it).delete() }
            lastOutputPath = null
            primaryAction?.text = AppStrings.t(this, "record_start")
            primaryAction?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.audio_red))
            signalPreview?.stop()
            statusView?.text = AppStrings.t(this, "record_short")
        }
    }

    private fun playFile(file: File) {
        if (!file.exists()) {
            statusView?.text = AppStrings.t(this, "not_available")
            return
        }
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@ToolDetailActivity, Uri.fromFile(file))
            setOnPreparedListener {
                it.start()
                statusView?.text = if (file.name.isBlank()) AppStrings.t(this@ToolDetailActivity, "playing") else AppStrings.t(this@ToolDetailActivity, "playing") + " • " + file.name
            }
            setOnCompletionListener {
                statusView?.text = AppStrings.t(this@ToolDetailActivity, "completed")
                releasePlayer()
            }
            prepareAsync()
        }
    }

    private fun playUri(uri: Uri) {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@ToolDetailActivity, uri)
            setOnPreparedListener {
                it.start()
                statusView?.text = AppStrings.t(this@ToolDetailActivity, "playing")
            }
            setOnCompletionListener {
                statusView?.text = AppStrings.t(this@ToolDetailActivity, "completed")
                releasePlayer()
            }
            prepareAsync()
        }
    }

    private fun releasePlayer() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun releaseRecorder() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
    }

    private fun stopTimer() {
        timerRunnable?.let { mainHandler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun readDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    private fun accentFor(toolId: String): Int = when (toolId) {
        "cut" -> R.color.audio_blue
        "convert" -> R.color.audio_purple
        "recorder" -> R.color.audio_red
        "analyzer" -> R.color.audio_yellow
        else -> R.color.audio_blue
    }

    private fun specText(label: String, value: String): TextView =
        TextView(this).apply {
            text = label.uppercase() + "\n" + value
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment ?: "file"
    }

    companion object {
        private const val EXTRA_TOOL_ID = "tool_id"

        fun intent(context: Context, toolId: String): Intent =
            Intent(context, ToolDetailActivity::class.java).putExtra(EXTRA_TOOL_ID, toolId)
    }
}

private fun TextInputEditText.setTextWithoutMovingCursor(value: String, unused: Nothing?) {
    if (text?.toString() != value) setText(value)
}
