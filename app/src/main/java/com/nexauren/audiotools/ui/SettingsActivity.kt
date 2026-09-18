package com.nexauren.audiotools.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.update.UpdateInfo
import com.nexauren.audiotools.update.UpdateManager

class SettingsActivity : ComponentActivity() {
    private var pendingDownloadId: Long? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id <= 0L || id != pendingDownloadId) return
            val uri = getSystemService(DownloadManager::class.java).getUriForDownloadedFile(id)
            if (uri == null) {
                Toast.makeText(this@SettingsActivity, "Não foi possível abrir a atualização.", Toast.LENGTH_LONG).show()
                return
            }
            if (!UpdateManager.canInstallPackages(this@SettingsActivity)) {
                Toast.makeText(this@SettingsActivity, "Ative a permissão para instalar atualizações.", Toast.LENGTH_LONG).show()
                UpdateManager.openInstallPermission(this@SettingsActivity)
                return
            }
            UpdateManager.openInstaller(this@SettingsActivity, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(downloadReceiver)
        super.onStop()
    }

    private fun buildUi() {
        val root = ViewKit.page(this)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(MaterialButton(this).apply {
            text = "‹"
            minWidth = ViewKit.dp(this@SettingsActivity, 48)
            minHeight = ViewKit.dp(this@SettingsActivity, 48)
            cornerRadius = ViewKit.dp(this@SettingsActivity, 15)
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            setBackgroundColor(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Definições"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@SettingsActivity, 12)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 22))
        root.addView(section("ATUALIZAÇÕES"))
        root.addView(ViewKit.spacer(this, 8))

        val updateCard = ViewKit.card(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@SettingsActivity, 18),
                ViewKit.dp(this@SettingsActivity, 18),
                ViewKit.dp(this@SettingsActivity, 18),
                ViewKit.dp(this@SettingsActivity, 18)
            )
        }
        content.addView(TextView(this).apply {
            text = "Versão instalada"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_muted))
        })
        content.addView(ViewKit.spacer(this, 4))
        content.addView(TextView(this).apply {
            text = "Audio Tools " + BuildConfig.VERSION_NAME
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(ViewKit.spacer(this, 8))
        content.addView(ViewKit.subtitle(this, "Consulta o último release do projeto. Quando houver um APK novo, podes descarregar e abrir o instalador oficial do Android."))
        content.addView(ViewKit.spacer(this, 14))

        val check = ViewKit.button(this, "Verificar atualizações", true)
        check.setOnClickListener { checkForUpdates(check) }
        content.addView(check)
        content.addView(ViewKit.spacer(this, 8))

        content.addView(ViewKit.button(this, "Permissão para instalar atualizações", false).apply {
            setOnClickListener {
                if (UpdateManager.canInstallPackages(this@SettingsActivity)) {
                    Toast.makeText(this@SettingsActivity, "A permissão já está ativa.", Toast.LENGTH_SHORT).show()
                } else {
                    UpdateManager.openInstallPermission(this@SettingsActivity)
                }
            }
        })

        content.addView(MaterialSwitch(this).apply {
            text = "Verificar ao abrir as definições"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            isChecked = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_update_check", true)
            setOnCheckedChangeListener { _, enabled ->
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putBoolean("auto_update_check", enabled).apply()
            }
        })
        updateCard.addView(content)
        root.addView(updateCard)

        root.addView(ViewKit.spacer(this, 18))
        root.addView(section("APP"))
        root.addView(ViewKit.spacer(this, 8))
        root.addView(ViewKit.card(this, clickable = true).apply {
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, AboutActivity::class.java))
            }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(this@SettingsActivity, 18),
                    ViewKit.dp(this@SettingsActivity, 18),
                    ViewKit.dp(this@SettingsActivity, 18),
                    ViewKit.dp(this@SettingsActivity, 18)
                )
                addView(ViewKit.title(this@SettingsActivity, "Sobre o Audio Tools", 17f))
                addView(ViewKit.spacer(this@SettingsActivity, 5))
                addView(ViewKit.subtitle(this@SettingsActivity, "Arquitetura modular para crescer sem transformar a aplicação numa única tela gigante."))
            })
        })

        if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_update_check", true)) {
            window.decorView.postDelayed({ checkForUpdates(check) }, 400)
        }

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun checkForUpdates(button: MaterialButton) {
        button.isEnabled = false
        button.text = "A verificar…"
        UpdateManager.check(BuildConfig.VERSION_NAME) { result ->
            runOnUiThread {
                button.isEnabled = true
                button.text = "Verificar atualizações"
                result.onSuccess { info ->
                    if (info == null) {
                        Toast.makeText(this, "Já tens a versão mais recente.", Toast.LENGTH_SHORT).show()
                    } else {
                        showUpdate(info)
                    }
                }.onFailure {
                    Toast.makeText(this, "Não foi possível verificar agora.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showUpdate(info: UpdateInfo) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Atualização " + info.versionName + " disponível")
            .setMessage(
                if (info.apkUrl.isBlank()) {
                    "Há um novo release, mas ainda não existe um APK anexado."
                } else {
                    "Existe uma nova versão. O download usa o APK publicado no release."
                }
            )
            .setNegativeButton("Fechar", null)

        if (info.apkUrl.isNotBlank()) {
            builder.setPositiveButton("Baixar e instalar") { _, _ ->
                if (!UpdateManager.canInstallPackages(this)) {
                    Toast.makeText(this, "Primeiro permite instalações desta aplicação.", Toast.LENGTH_LONG).show()
                    UpdateManager.openInstallPermission(this)
                } else {
                    pendingDownloadId = UpdateManager.download(this, info)
                    Toast.makeText(this, "Download iniciado.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            builder.setPositiveButton("Abrir release") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseNotesUrl)))
            }
        }
        builder.show()
    }

    private fun section(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_muted))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        }
}