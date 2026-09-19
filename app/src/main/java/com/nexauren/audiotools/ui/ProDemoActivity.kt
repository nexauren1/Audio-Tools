package com.nexauren.audiotools.ui

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
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.payments.Entitlement
import com.nexauren.audiotools.payments.PaymentClient
import com.google.android.material.button.MaterialButton
import java.util.Locale
import java.util.concurrent.Executors

class ProDemoActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) analyze(uri)
    }

    private var status: TextView? = null
    private var action: MaterialButton? = null
    private var buyButton: MaterialButton? = null
    private var progress: ProgressBar? = null
    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        buildUi()
        handlePaymentReturn(intent)
        refreshEntitlement()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            handlePaymentReturn(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshEntitlement()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = ViewKit.page(this)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(ViewKit.button(
            this,
            "‹",
            false,
            R.color.audio_purple
        ).apply {
            minWidth = ViewKit.dp(this@ProDemoActivity, 48)
            minHeight = ViewKit.dp(this@ProDemoActivity, 48)
            contentDescription = AppStrings.t(
                this@ProDemoActivity,
                "back"
            )
            setOnClickListener { finish() }
        })

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = ViewKit.dp(this@ProDemoActivity, 11)
            }
        }
        titleColumn.addView(ViewKit.eyebrow(this, "PRO"))
        titleColumn.addView(ViewKit.title(this, proText("title"), 22f))
        header.addView(titleColumn)
        header.addView(ViewKit.pill(
            this,
            "PRO",
            colorRes = R.color.audio_purple
        ))

        root.addView(header)
        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.title(this, proText("hero"), 29f))
        root.addView(ViewKit.spacer(this, 7))
        root.addView(ViewKit.subtitle(this, proText("desc")))
        root.addView(ViewKit.spacer(this, 15))

        val card = ViewKit.card(
            this,
            accentColorRes = R.color.audio_purple
        )
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15)
            )
        }

        content.addView(ViewKit.pill(
            this,
            proText("badge"),
            colorRes = R.color.audio_purple
        ))
        content.addView(ViewKit.spacer(this, 9))
        content.addView(TextView(this).apply {
            text = proText("features")
            textSize = 12.5f
            setLineSpacing(1.12f, 1f)
            setTextColor(ContextCompat.getColor(
                this@ProDemoActivity,
                R.color.audio_muted
            ))
        })
        content.addView(ViewKit.spacer(this, 13))

        status = TextView(this).apply {
            text = proText("checking")
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(
                this@ProDemoActivity,
                R.color.audio_text
            ))
        }
        content.addView(status)

        progress = ProgressBar(this).apply {
            visibility = View.GONE
        }
        content.addView(progress)
        content.addView(ViewKit.spacer(this, 8))

        action = ViewKit.button(
            this,
            proText("choose"),
            true,
            R.color.audio_purple
        ).apply {
            isEnabled = false
            setOnClickListener {
                val uri = selectedUri
                if (uri == null) {
                    picker.launch(arrayOf("audio/*"))
                } else {
                    analyze(uri)
                }
            }
        }
        content.addView(action)
        content.addView(ViewKit.spacer(this, 8))

        buyButton = ViewKit.button(
            this,
            proText("buy"),
            false,
            R.color.audio_green
        ).apply {
            setOnClickListener { startPurchase() }
        }
        content.addView(buyButton)

        card.addView(content)
        root.addView(card)
        root.addView(ViewKit.spacer(this, 14))

        val footnote = ViewKit.card(
            this,
            accentColorRes = R.color.audio_green
        )
        footnote.addView(TextView(this).apply {
            text = proText("sandbox")
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(
                this@ProDemoActivity,
                R.color.audio_muted
            ))
            setPadding(
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15),
                ViewKit.dp(this@ProDemoActivity, 15)
            )
        })
        root.addView(footnote)
        root.addView(ViewKit.spacer(this, 22))
        root.addView(ViewKit.sectionLabel(this, proText("report")))
        root.addView(ViewKit.spacer(this, 7))

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
            addView(root)
        })
    }

    private fun refreshEntitlement() {
        executor.execute {
            try {
                val entitlement = PaymentClient.getEntitlement()
                mainHandler.post { renderEntitlement(entitlement) }
            } catch (error: Exception) {
                mainHandler.post {
                    action?.isEnabled = false
                    buyButton?.isEnabled = true
                    status?.text = proText("backendError") +
                        "\n" + error.message.orEmpty()
                }
            }
        }
    }

    private fun renderEntitlement(entitlement: Entitlement) {
        val pro = entitlement.isPro
        action?.isEnabled = pro
        buyButton?.isEnabled = !pro
        status?.text = if (pro) {
            proText("active") +
                "\n" + formatExpiry(entitlement.expiresAt)
        } else {
            proText("locked")
        }
        buyButton?.text = if (pro) {
            proText("activeButton")
        } else {
            proText("buy")
        }
    }

    private fun startPurchase() {
        buyButton?.isEnabled = false
        status?.text = proText("creating")

        executor.execute {
            try {
                val order = PaymentClient.createProOrder()
                mainHandler.post {
                    try {
                        startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(order.approvalUrl)
                        ))
                        buyButton?.isEnabled = true
                        status?.text = proText("returnToApp")
                    } catch (_: Exception) {
                        buyButton?.isEnabled = true
                        status?.text = proText("browserError")
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    buyButton?.isEnabled = true
                    status?.text = proText("purchaseError") +
                        "\n" + error.message.orEmpty()
                }
            }
        }
    }

    private fun handlePaymentReturn(incoming: Intent) {
        val data = incoming.data ?: return
        if (data.scheme != "audiotools" ||
            data.host != "paypal" ||
            data.path != "/complete"
        ) {
            return
        }

        val orderId = data.getQueryParameter("token").orEmpty()
        if (orderId.isBlank()) {
            status?.text = proText("missingOrder")
            return
        }

        capture(orderId)
    }

    private fun capture(orderId: String) {
        buyButton?.isEnabled = false
        status?.text = proText("capturing")

        executor.execute {
            try {
                val entitlement = PaymentClient.captureProOrder(orderId)
                mainHandler.post {
                    renderEntitlement(entitlement)
                    selectedUri = null
                    status?.append("\n" + proText("activated"))
                    Toast.makeText(
                        this,
                        proText("activatedToast"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (error: Exception) {
                mainHandler.post {
                    buyButton?.isEnabled = true
                    status?.text = proText("captureError") +
                        "\n" + error.message.orEmpty()
                }
            }
        }
    }

    private fun analyze(uri: Uri) {
        selectedUri = uri
        progress?.visibility = View.VISIBLE
        action?.isEnabled = false
        status?.text = proText("analyzing")

        executor.execute {
            try {
                val report = buildReport(uri)
                mainHandler.post {
                    progress?.visibility = View.GONE
                    action?.isEnabled = true
                    status?.text = report
                }
            } catch (_: Exception) {
                mainHandler.post {
                    progress?.visibility = View.GONE
                    action?.isEnabled = true
                    status?.text = proText("analysisError")
                }
            }
        }
    }

    private fun buildReport(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val name = displayName(uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val mime = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE
            ).orEmpty().ifBlank { "—" }
            val bitrate = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            )?.toLongOrNull()

            buildString {
                append(name)
                append("\n\n")
                append(proText("duration"))
                append(": ")
                append(formatDuration(duration))
                append("\n")
                append(proText("format"))
                append(": ")
                append(mime)
                append("\n")
                append(proText("bitrate"))
                append(": ")
                append(
                    if (bitrate != null && bitrate > 0) {
                        (bitrate / 1000).toString() + " kbps"
                    } else {
                        "—"
                    }
                )
                append("\n")
                append(proText("sandboxOnly"))
            }
        } finally {
            retriever.release()
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        return String.format(
            Locale.US,
            "%02d:%02d",
            totalSeconds / 60,
            totalSeconds % 60
        )
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment ?: "audio"
    }

    private fun formatExpiry(expiresAt: Long?): String {
        if (expiresAt == null) return proText("activeNoExpiry")
        return proText("expiresAt") + " " + expiresAt
    }

    private fun proText(key: String): String {
        val lang = LanguageManager.get(this)
        return when (key) {
            "title" -> when (lang) {
                "en" -> "Pro Audio Inspector"
                "fr" -> "Inspecteur audio Pro"
                "es" -> "Inspector de audio Pro"
                "de" -> "Pro-Audio-Inspektor"
                else -> "Inspetor de Áudio Pro"
            }
            "hero" -> when (lang) {
                "en" -> "Advanced audio checks."
                "fr" -> "Contrôles audio avancés."
                "es" -> "Controles de audio avanzados."
                "de" -> "Erweiterte Audio-Prüfungen."
                else -> "Verificações avançadas do áudio."
            }
            "desc" -> when (lang) {
                "en" -> "A Pro-only example showing how paid access is checked before the premium tool runs."
                "fr" -> "Un exemple Pro qui vérifie l’accès payant avant d’exécuter l’outil premium."
                "es" -> "Un ejemplo Pro que comprueba el acceso antes de ejecutar la herramienta premium."
                "de" -> "Ein Pro-Beispiel, das den kostenpflichtigen Zugriff vor der Premium-Funktion prüft."
                else -> "Um exemplo Pro que verifica o acesso pago antes de executar a ferramenta premium."
            }
            "badge" -> when (lang) {
                "en" -> "PRO ACCESS"
                "fr" -> "ACCÈS PRO"
                "es" -> "ACCESO PRO"
                "de" -> "PRO-ZUGRIFF"
                else -> "ACESSO PRO"
            }
            "features" -> when (lang) {
                "en" -> "Reads local audio metadata without uploading the file. Pro access is enforced by the Cloudflare Worker."
                "fr" -> "Lit les métadonnées audio localement sans téléverser le fichier. L’accès Pro est contrôlé par le Worker Cloudflare."
                "es" -> "Lee metadatos de audio localmente sin subir el archivo. El acceso Pro lo controla el Worker Cloudflare."
                "de" -> "Liest lokale Audio-Metadaten ohne Upload. Der Pro-Zugriff wird vom Cloudflare Worker kontrolliert."
                else -> "Lê os metadados do áudio localmente sem enviar o ficheiro. O acesso Pro é controlado pelo Worker Cloudflare."
            }
            "checking" -> when (lang) {
                "en" -> "Checking Pro access…"
                "fr" -> "Vérification de l’accès Pro…"
                "es" -> "Comprobando acceso Pro…"
                "de" -> "Pro-Zugriff wird geprüft…"
                else -> "A verificar o acesso Pro…"
            }
            "locked" -> when (lang) {
                "en" -> "Pro locked. Purchase in PayPal Sandbox to unlock this example."
                "fr" -> "Pro verrouillé. Achète dans PayPal Sandbox pour déverrouiller cet exemple."
                "es" -> "Pro bloqueado. Compra en PayPal Sandbox para desbloquear este ejemplo."
                "de" -> "Pro gesperrt. Kaufe in PayPal Sandbox, um dieses Beispiel freizuschalten."
                else -> "Pro bloqueado. Faz a compra no PayPal Sandbox para desbloquear este exemplo."
            }
            "active" -> when (lang) {
                "en" -> "Pro is active."
                "fr" -> "Pro est actif."
                "es" -> "Pro está activo."
                "de" -> "Pro ist aktiv."
                else -> "O Pro está ativo."
            }
            "choose" -> when (lang) {
                "en" -> "Choose audio and analyze"
                "fr" -> "Choisir un audio et analyser"
                "es" -> "Elegir audio y analizar"
                "de" -> "Audio auswählen und prüfen"
                else -> "Escolher áudio e analisar"
            }
            "buy" -> "Ativar Pro • 5 USD"
            "activeButton" -> when (lang) {
                "en" -> "Pro active"
                "fr" -> "Pro actif"
                "es" -> "Pro activo"
                "de" -> "Pro aktiv"
                else -> "Pro ativo"
            }
            "creating" -> when (lang) {
                "en" -> "Creating secure PayPal order…"
                "fr" -> "Création de la commande PayPal sécurisée…"
                "es" -> "Creando pedido PayPal seguro…"
                "de" -> "Sichere PayPal-Bestellung wird erstellt…"
                else -> "A criar o pedido PayPal seguro…"
            }
            "returnToApp" -> when (lang) {
                "en" -> "Approve the sandbox payment and return to Audio Tools."
                "fr" -> "Approuve le paiement sandbox puis reviens à Audio Tools."
                "es" -> "Aprueba el pago sandbox y vuelve a Audio Tools."
                "de" -> "Bestätige die Sandbox-Zahlung und kehre zu Audio Tools zurück."
                else -> "Aprova o pagamento sandbox e volta ao Audio Tools."
            }
            "browserError" -> when (lang) {
                "en" -> "Could not open PayPal in the browser."
                "fr" -> "Impossible d’ouvrir PayPal dans le navigateur."
                "es" -> "No se pudo abrir PayPal en el navegador."
                "de" -> "PayPal konnte nicht im Browser geöffnet werden."
                else -> "Não foi possível abrir o PayPal no navegador."
            }
            "purchaseError" -> when (lang) {
                "en" -> "Could not create the PayPal order."
                "fr" -> "Impossible de créer la commande PayPal."
                "es" -> "No se pudo crear el pedido PayPal."
                "de" -> "PayPal-Bestellung konnte nicht erstellt werden."
                else -> "Não foi possível criar o pedido PayPal."
            }
            "missingOrder" -> when (lang) {
                "en" -> "The PayPal return did not contain an order ID."
                "fr" -> "Le retour PayPal ne contient pas d’identifiant de commande."
                "es" -> "La vuelta de PayPal no contiene el ID del pedido."
                "de" -> "Die PayPal-Rückkehr enthält keine Bestell-ID."
                else -> "O retorno do PayPal não trouxe o ID do pedido."
            }
            "capturing" -> when (lang) {
                "en" -> "Confirming the sandbox payment…"
                "fr" -> "Confirmation du paiement sandbox…"
                "es" -> "Confirmando el pago sandbox…"
                "de" -> "Sandbox-Zahlung wird bestätigt…"
                else -> "A confirmar o pagamento sandbox…"
            }
            "activated" -> when (lang) {
                "en" -> "Pro access is now active."
                "fr" -> "L’accès Pro est maintenant actif."
                "es" -> "El acceso Pro está activo."
                "de" -> "Der Pro-Zugriff ist jetzt aktiv."
                else -> "O acesso Pro está agora ativo."
            }
            "activatedToast" -> when (lang) {
                "en" -> "Audio Tools Pro activated."
                "fr" -> "Audio Tools Pro activé."
                "es" -> "Audio Tools Pro activado."
                "de" -> "Audio Tools Pro aktiviert."
                else -> "Audio Tools Pro ativado."
            }
            "captureError" -> when (lang) {
                "en" -> "Could not confirm the PayPal payment."
                "fr" -> "Impossible de confirmer le paiement PayPal."
                "es" -> "No se pudo confirmar el pago PayPal."
                "de" -> "Die PayPal-Zahlung konnte nicht bestätigt werden."
                else -> "Não foi possível confirmar o pagamento PayPal."
            }
            "backendError" -> when (lang) {
                "en" -> "The Pro backend is not available yet."
                "fr" -> "Le backend Pro n’est pas encore disponible."
                "es" -> "El backend Pro aún no está disponible."
                "de" -> "Das Pro-Backend ist noch nicht verfügbar."
                else -> "O backend Pro ainda não está disponível."
            }
            "analyzing" -> when (lang) {
                "en" -> "Analyzing locally…"
                "fr" -> "Analyse locale…"
                "es" -> "Analizando localmente…"
                "de" -> "Lokale Analyse…"
                else -> "A analisar localmente…"
            }
            "analysisError" -> when (lang) {
                "en" -> "Could not inspect this audio file."
                "fr" -> "Impossible d’inspecter ce fichier audio."
                "es" -> "No se pudo inspeccionar este archivo de audio."
                "de" -> "Diese Audiodatei konnte nicht geprüft werden."
                else -> "Não foi possível inspecionar este áudio."
            }
            "duration" -> when (lang) {
                "en" -> "Duration"
                "fr" -> "Durée"
                "es" -> "Duración"
                "de" -> "Dauer"
                else -> "Duração"
            }
            "format" -> when (lang) {
                "en" -> "Format"
                "fr" -> "Format"
                "es" -> "Formato"
                "de" -> "Format"
                else -> "Formato"
            }
            "bitrate" -> "Bitrate"
            "sandboxOnly" -> when (lang) {
                "en" -> "Demo runs locally in the app."
                "fr" -> "La démo s’exécute localement dans l’app."
                "es" -> "La demo se ejecuta localmente en la app."
                "de" -> "Die Demo läuft lokal in der App."
                else -> "A demonstração é executada localmente na app."
            }
            "sandbox" -> when (lang) {
                "en" -> "PAYPAL SANDBOX: testing only. No production payment endpoint is used."
                "fr" -> "PAYPAL SANDBOX : tests uniquement. Aucun endpoint de production n’est utilisé."
                "es" -> "PAYPAL SANDBOX: solo pruebas. No se usa ningún endpoint de producción."
                "de" -> "PAYPAL SANDBOX: nur Tests. Kein Produktions-Endpoint wird verwendet."
                else -> "PAYPAL SANDBOX: apenas testes. Nenhum endpoint de produção é usado."
            }
            "report" -> when (lang) {
                "en" -> "LOCAL PRO TOOL"
                "fr" -> "OUTIL PRO LOCAL"
                "es" -> "HERRAMIENTA PRO LOCAL"
                "de" -> "LOKALES PRO-TOOL"
                else -> "FERRAMENTA PRO LOCAL"
            }
            "expiresAt" -> when (lang) {
                "en" -> "Entitlement expiry:"
                "fr" -> "Expiration de l’accès :"
                "es" -> "Vencimiento del acceso:"
                "de" -> "Zugriff läuft ab:"
                else -> "Expira em:"
            }
            "activeNoExpiry" -> when (lang) {
                "en" -> "Active without an expiry date."
                "fr" -> "Actif sans date d’expiration."
                "es" -> "Activo sin fecha de vencimiento."
                "de" -> "Aktiv ohne Ablaufdatum."
                else -> "Ativo sem data de expiração."
            }
            else -> key
        }
    }

    companion object {
        fun intent(context: android.content.Context): Intent =
            Intent(context, ProDemoActivity::class.java)
    }
}