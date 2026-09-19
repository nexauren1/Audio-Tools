package com.nexauren.audiotools.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

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
            "analyzer" -> onAnalyzerFileSelected(uri)
        }
    }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecordingInternal()
            else statusView?.text = "O microfone é necessário para gravar."
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
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(tool: AudioTool) {
        val root = ViewKit.page(this)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ViewKit.button(this, "‹", false).apply {
            minWidth = ViewKit.dp(this@ToolDetailActivity, 50)
            minHeight = ViewKit.dp(this@ToolDetailActivity, 50)
            setOnClickListener { finish() }
        })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@ToolDetailActivity, 13)
            }
            addView(ViewKit.eyebrow(this@ToolDetailActivity, tool.category))
            addView(ViewKit.title(this@ToolDetailActivity, "Ferramenta " + tool.number, 20f))
        })
        root.addView(header)
        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.title(this, tool.title, 29f))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(ViewKit.subtitle(this, tool.detail))

        root.addView(ViewKit.spacer(this, 20))
        when (tool.id) {
            "cut" -> buildCutAction(root)
            "recorder" -> buildRecorderAction(root)
            "analyzer" -> buildAnalyzerAction(root)
        }

        root.addView(ViewKit.spacer(this, 24))
        root.addView(ViewKit.sectionLabel(this, "Como funciona"))
        root.addView(ViewKit.spacer(this, 8))
        tool.steps.forEachIndexed { index, step ->
            root.addView(stepCard(index + 1, step))
            root.addView(ViewKit.spacer(this, 8))
        }

        root.addView(ViewKit.spacer(this, 12))
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
        spec.addView(ViewKit.sectionLabel(this, "Compatibilidade"))
        spec.addView(ViewKit.spacer(this, 10))
        spec.addView(specText("Entrada", tool.input))
        spec.addView(ViewKit.spacer(this, 7))
        spec.addView(specText("Saída", tool.output))
        specCard.addView(spec)
        root.addView(specCard)

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
            addView(root)
        })
    }

    private fun buildCutAction(root: LinearLayout) {
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18)
            )
        }
        content.addView(ViewKit.pill(this, "AAC / M4A", positive = true))
        content.addView(ViewKit.spacer(this, 12))

        statusView = TextView(this).apply {
            text = "Escolhe um áudio para começar."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
        }
        content.addView(statusView)

        content.addView(ViewKit.spacer(this, 12))
        content.addView(ViewKit.button(this, "Escolher áudio", true).apply {
            setOnClickListener {
                filePicker.launch(arrayOf("audio/mp4", "audio/aac", "audio/x-m4a", "audio/*", "video/mp4"))
            }
        })
        content.addView(ViewKit.spacer(this, 14))

        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val startField = timeField("Início", "0")
        val endField = timeField("Fim", "0")
        cutEndInput = endField.second
        fields.addView(startField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = ViewKit.dp(this@ToolDetailActivity, 8)
        })
        fields.addView(endField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(fields)
        content.addView(ViewKit.spacer(this, 12))

        val progress = ProgressBar(this).apply {
            visibility = ProgressBar.GONE
        }
        content.addView(progress)
        content.addView(ViewKit.spacer(this, 8))

        primaryAction = ViewKit.button(this, "Cortar e guardar", true).apply {
            isEnabled = false
            setOnClickListener {
                val uri = selectedUri ?: return@setOnClickListener
                val start = startField.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                val end = endField.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                if (start < 0 || end <= start || end * 1000 > selectedDurationMs) {
                    statusView?.text = "Escolhe um intervalo válido entre 0 e a duração do áudio."
                    return@setOnClickListener
                }
                performCut(uri, (start * 1000).toLong(), (end * 1000).toLong(), progress)
            }
        }
        content.addView(primaryAction)

        playbackAction = ViewKit.button(this, "Reproduzir resultado", false).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { lastOutputPath?.let { playFile(File(it)) } }
        }
        content.addView(ViewKit.spacer(this, 8))
        content.addView(playbackAction)

        card.addView(content)
        root.addView(card)
    }

    private fun buildRecorderAction(root: LinearLayout) {
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18)
            )
        }

        content.addView(ViewKit.pill(this, "M4A • AAC", positive = true))
        content.addView(ViewKit.spacer(this, 14))

        statusView = TextView(this).apply {
            text = "Pronto para gravar."
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            gravity = Gravity.CENTER
        }
        content.addView(statusView)

        content.addView(ViewKit.spacer(this, 14))
        primaryAction = ViewKit.button(this, "●  Começar gravação", true).apply {
            setOnClickListener {
                if (mediaRecorder == null) startRecording() else stopRecording()
            }
        }
        content.addView(primaryAction)
        content.addView(ViewKit.spacer(this, 8))

        playbackAction = ViewKit.button(this, "Reproduzir última gravação", false).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { lastOutputPath?.let { playFile(File(it)) } }
        }
        content.addView(playbackAction)

        card.addView(content)
        root.addView(card)
    }

    private fun buildAnalyzerAction(root: LinearLayout) {
        val card = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18),
                ViewKit.dp(this@ToolDetailActivity, 18)
            )
        }

        content.addView(ViewKit.pill(this, "LEITURA LOCAL", positive = true))
        content.addView(ViewKit.spacer(this, 12))
        statusView = TextView(this).apply {
            text = "Seleciona um áudio para obter o relatório."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_muted))
        }
        content.addView(statusView)
        content.addView(ViewKit.spacer(this, 12))
        content.addView(ViewKit.button(this, "Escolher áudio", true).apply {
            setOnClickListener { filePicker.launch(arrayOf("audio/*", "video/mp4")) }
        })
        content.addView(ViewKit.spacer(this, 14))

        playbackAction = ViewKit.button(this, "Reproduzir áudio", false).apply {
            visibility = MaterialButton.GONE
            setOnClickListener { selectedUri?.let { playUri(it) } }
        }
        content.addView(playbackAction)
        card.addView(content)
        root.addView(card)
    }

    private fun timeField(label: String, value: String): Pair<TextInputLayout, TextInputEditText> {
        val input = TextInputEditText(this).apply {
            setText(value)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 15f
        }
        val layout = TextInputLayout(this).apply {
            hint = label + " (s)"
            boxCornerRadiusTopStart = ViewKit.dp(this@ToolDetailActivity, 14).toFloat()
            boxCornerRadiusTopEnd = ViewKit.dp(this@ToolDetailActivity, 14).toFloat()
            boxCornerRadiusBottomStart = ViewKit.dp(this@ToolDetailActivity, 14).toFloat()
            boxCornerRadiusBottomEnd = ViewKit.dp(this@ToolDetailActivity, 14).toFloat()
            addView(input)
        }
        return layout to input
    }

    private fun stepCard(number: Int, step: String): ViewGroup {
        val card = ViewKit.card(this)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                ViewKit.dp(this@ToolDetailActivity, 15),
                ViewKit.dp(this@ToolDetailActivity, 13),
                ViewKit.dp(this@ToolDetailActivity, 15),
                ViewKit.dp(this@ToolDetailActivity, 13)
            )
        }
        row.addView(ViewKit.iconBadge(this, number.toString()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewKit.dp(this@ToolDetailActivity, 38),
                ViewKit.dp(this@ToolDetailActivity, 38)
            ).apply { rightMargin = ViewKit.dp(this@ToolDetailActivity, 12) }
        })
        row.addView(TextView(this).apply {
            text = step
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ToolDetailActivity, R.color.audio_text))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(row)
        return card
    }

    private fun onCutFileSelected(uri: Uri) {
        selectedUri = uri
        worker.execute {
            val duration = try { readDuration(uri) } catch (_: Exception) { 0L }
            mainHandler.post {
                selectedDurationMs = duration
                statusView?.text = "Selecionado: " + displayName(uri)
                primaryAction?.isEnabled = duration > 0L
                cutEndInput?.setText(String.format(Locale.US, "%.2f", duration / 1000.0))
            }
        }
    }

    private var cutEndInput: TextInputEditText? = null

    private fun performCut(uri: Uri, startMs: Long, endMs: Long, progress: ProgressBar) {
        primaryAction?.isEnabled = false
        progress.visibility = ProgressBar.VISIBLE
        statusView?.text = "A cortar o áudio…"

        worker.execute {
            try {
                val sourceName = displayName(uri).substringBeforeLast('.')
                val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioTools").apply { mkdirs() }
                val output = File(outputDir, sourceName + "_cut_" + System.currentTimeMillis() + ".m4a")
                trimAac(uri, startMs * 1000L, endMs * 1000L, output)
                lastOutputPath = output.absolutePath
                mainHandler.post {
                    progress.visibility = ProgressBar.GONE
                    primaryAction?.isEnabled = true
                    playbackAction?.visibility = MaterialButton.VISIBLE
                    statusView?.text = "Pronto. Guardado como " + output.name
                }
            } catch (_: Exception) {
                mainHandler.post {
                    progress.visibility = ProgressBar.GONE
                    primaryAction?.isEnabled = true
                    statusView?.text = "Não foi possível cortar. Na V1, use um M4A/AAC."
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

            val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                max(64 * 1024, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            } else 1024 * 1024
            val buffer = java.nio.ByteBuffer.allocate(maxInput)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime > endUs) break
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

    private fun onAnalyzerFileSelected(uri: Uri) {
        selectedUri = uri
        statusView?.text = "A analisar " + displayName(uri) + "…"
        playbackAction?.visibility = MaterialButton.GONE
        worker.execute {
            try {
                val report = analyzeAudio(uri)
                mainHandler.post {
                    statusView?.text = report
                    playbackAction?.visibility = MaterialButton.VISIBLE
                }
            } catch (_: Exception) {
                mainHandler.post { statusView?.text = "Não foi possível analisar este ficheiro." }
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
            val mime = audioFormat?.getString(MediaFormat.KEY_MIME)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: "desconhecido"
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val channels = audioFormat?.let { intValue(it, MediaFormat.KEY_CHANNEL_COUNT) }
            val sampleRate = audioFormat?.let { intValue(it, MediaFormat.KEY_SAMPLE_RATE) }

            return buildString {
                append("Ficheiro\n")
                append(displayName(uri))
                append("\n\nDuração\n")
                append(formatDuration(durationMs))
                append("\n\nFormato\n")
                append(mime)
                append("\n\nBitrate\n")
                append(if (bitrate != null && bitrate > 0) (bitrate / 1000).toString() + " kbps" else "—")
                append("\n\nCanais\n")
                append(channels?.toString() ?: "—")
                append("\n\nTaxa de amostragem\n")
                append(if (sampleRate != null && sampleRate > 0) sampleRate.toString() + " Hz" else "—")
            }
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    private fun intValue(format: MediaFormat, key: String): Int? =
        try {
            if (format.containsKey(key)) format.getInteger(key) else null
        } catch (_: Exception) {
            null
        }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

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
            primaryAction?.text = "■  Parar gravação"
            primaryAction?.setBackgroundColor(ContextCompat.getColor(this, R.color.audio_danger))
            statusView?.text = "Gravando • 00:00"
            timerRunnable = object : Runnable {
                override fun run() {
                    val seconds = ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt()
                    statusView?.text = "Gravando • %02d:%02d".format(seconds / 60, seconds % 60)
                    mainHandler.postDelayed(this, 500)
                }
            }.also { mainHandler.post(it) }
        } catch (_: Exception) {
            output.delete()
            releaseRecorder()
            statusView?.text = "Não foi possível iniciar a gravação."
        }
    }

    private fun stopRecording() {
        stopTimer()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            primaryAction?.text = "●  Começar gravação"
            primaryAction?.setBackgroundColor(ContextCompat.getColor(this, R.color.audio_primary))
            playbackAction?.visibility = MaterialButton.VISIBLE
            statusView?.text = "Gravação guardada no dispositivo."
        } catch (_: Exception) {
            releaseRecorder()
            lastOutputPath?.let { File(it).delete() }
            lastOutputPath = null
            primaryAction?.text = "●  Começar gravação"
            primaryAction?.setBackgroundColor(ContextCompat.getColor(this, R.color.audio_primary))
            statusView?.text = "A gravação foi demasiado curta. Tenta novamente."
        }
    }

    private fun playFile(file: File) {
        if (!file.exists()) {
            statusView?.text = "O ficheiro já não está disponível."
            return
        }
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@ToolDetailActivity, Uri.fromFile(file))
            setOnPreparedListener {
                it.start()
                statusView?.text = "A reproduzir • " + file.name
            }
            setOnCompletionListener {
                statusView?.text = "Reprodução concluída."
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
                statusView?.text = "A reproduzir."
            }
            setOnCompletionListener {
                statusView?.text = "Reprodução concluída."
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

    private fun specText(label: String, value: String): TextView =
        TextView(this).apply {
            text = label + "\n" + value
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