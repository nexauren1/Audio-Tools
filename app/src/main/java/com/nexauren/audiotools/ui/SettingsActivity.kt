package com.nexauren.audiotools.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.R
import com.nexauren.audiotools.update.UpdateInfo
import com.nexauren.audiotools.update.UpdateManager
import java.io.File

class SettingsActivity : ComponentActivity() {
    private var pendingInfo: UpdateInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        pendingInfo?.let { info ->
            if (UpdateManager.canInstallPackages(this)) {
                pendingInfo = null
                beginDownload(info)
            }
        }
    }

    private fun buildUi() {
        val root = ViewKit.page(this)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ViewKit.button(this, "‹", false).apply {
            minWidth = ViewKit.dp(this@SettingsActivity, 48)
            minHeight = ViewKit.dp(this@SettingsActivity, 48)
            contentDescription = AppStrings.t(this@SettingsActivity, "back")
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = AppStrings.t(this@SettingsActivity, "settings")
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewKit.dp(this@SettingsActivity, 11)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 20))
        root.addView(section(AppStrings.t(this, "language")))
        root.addView(ViewKit.spacer(this, 7))

        val languageCard = ViewKit.card(this, accentColorRes = R.color.audio_blue)
        val languageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15))
        }
        languageContent.addView(TextView(this).apply {
            text = LanguageManager.name(LanguageManager.get(this@SettingsActivity))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
        })
        languageContent.addView(ViewKit.spacer(this, 4))
        languageContent.addView(ViewKit.subtitle(this, AppStrings.t(this, "language_desc")))
        languageContent.addView(ViewKit.spacer(this, 9))
        languageContent.addView(ViewKit.button(this, AppStrings.t(this, "choose_language"), false, R.color.audio_blue).apply {
            setOnClickListener { chooseLanguage() }
        })
        languageCard.addView(languageContent)
        root.addView(languageCard)

        root.addView(ViewKit.spacer(this, 16))
        root.addView(section(AppStrings.t(this, "update_section")))
        root.addView(ViewKit.spacer(this, 7))

        val updateCard = ViewKit.card(this, accentColorRes = R.color.audio_green)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15))
        }
        content.addView(TextView(this).apply {
            text = AppStrings.t(this@SettingsActivity, "installed")
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_muted))
        })
        content.addView(TextView(this).apply {
            text = "Audio Tools " + BuildConfig.VERSION_NAME
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
        })
        content.addView(ViewKit.spacer(this, 7))
        content.addView(ViewKit.subtitle(this, AppStrings.t(this, "download_install") + "."))
        content.addView(ViewKit.spacer(this, 10))

        val check = ViewKit.button(this, AppStrings.t(this, "check_update"), true, R.color.audio_green)
        check.setOnClickListener { checkForUpdates(check) }
        content.addView(check)
        content.addView(ViewKit.spacer(this, 7))

        content.addView(ViewKit.button(this, AppStrings.t(this, "install_permission"), false, R.color.audio_green).apply {
            setOnClickListener {
                if (UpdateManager.canInstallPackages(this@SettingsActivity)) {
                    Toast.makeText(this@SettingsActivity, AppStrings.t(this@SettingsActivity, "permission_active"), Toast.LENGTH_SHORT).show()
                } else {
                    UpdateManager.openInstallPermission(this@SettingsActivity)
                }
            }
        })

        content.addView(MaterialSwitch(this).apply {
            text = AppStrings.t(this@SettingsActivity, "auto_check")
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_text))
            isChecked = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_update_check", true)
            setOnCheckedChangeListener { _, enabled ->
                getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_update_check", enabled).apply()
            }
        })
        updateCard.addView(content)
        root.addView(updateCard)

        root.addView(ViewKit.spacer(this, 16))
        root.addView(section(AppStrings.t(this, "app_section")))
        root.addView(ViewKit.spacer(this, 7))
        root.addView(ViewKit.card(this, clickable = true).apply {
            setOnClickListener { startActivity(android.content.Intent(this@SettingsActivity, AboutActivity::class.java)) }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15), ViewKit.dp(this@SettingsActivity, 15))
                addView(ViewKit.title(this@SettingsActivity, AppStrings.t(this@SettingsActivity, "about_title"), 17f))
                addView(ViewKit.spacer(this@SettingsActivity, 4))
                addView(ViewKit.subtitle(this@SettingsActivity, AppStrings.t(this@SettingsActivity, "about_desc")))
            })
        })

        if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_update_check", true)) {
            window.decorView.postDelayed({ checkForUpdates(check) }, 450)
        }

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun chooseLanguage() {
        val current = LanguageManager.get(this)
        val options = LanguageManager.codes.map { LanguageManager.name(it) }.toTypedArray()
        val checked = LanguageManager.codes.indexOf(current)
        AlertDialog.Builder(this)
            .setTitle(AppStrings.t(this, "choose_language"))
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val code = LanguageManager.codes[which]
                if (code != current) {
                    LanguageManager.set(this, code)
                    dialog.dismiss()
                    recreate()
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(AppStrings.t(this, "back_action"), null)
            .show()
    }

    private fun checkForUpdates(button: MaterialButton) {
        button.isEnabled = false
        button.text = AppStrings.t(this, "checking")
        UpdateManager.check(BuildConfig.VERSION_NAME) { result ->
            runOnUiThread {
                button.isEnabled = true
                button.text = AppStrings.t(this, "check_update")
                result.onSuccess { info ->
                    if (info == null) {
                        Toast.makeText(this, AppStrings.t(this, "already_latest"), Toast.LENGTH_SHORT).show()
                    } else {
                        showUpdate(info)
                    }
                }.onFailure {
                    Toast.makeText(this, AppStrings.t(this, "download_error"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showUpdate(info: UpdateInfo) {
        val message = if (info.apkUrl.isBlank()) {
            AppStrings.t(this, "release_missing")
        } else {
            AppStrings.t(this, "download_started")
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(AppStrings.t(this, "update_available") + " " + info.versionName)
            .setMessage(message)
            .setNegativeButton(AppStrings.t(this, "back_action"), null)

        if (info.apkUrl.isNotBlank()) {
            builder.setPositiveButton(AppStrings.t(this, "download_install")) { _, _ ->
                if (!UpdateManager.canInstallPackages(this)) {
                    pendingInfo = info
                    Toast.makeText(this, AppStrings.t(this, "installer_permission"), Toast.LENGTH_LONG).show()
                    UpdateManager.openInstallPermission(this)
                } else {
                    beginDownload(info)
                }
            }
        } else {
            builder.setPositiveButton(AppStrings.t(this, "release_open")) { _, _ ->
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.releaseNotesUrl)))
            }
        }
        builder.show()
    }

    private fun beginDownload(info: UpdateInfo) {
        Toast.makeText(this, AppStrings.t(this, "download_started"), Toast.LENGTH_LONG).show()
        UpdateManager.download(this, info) { result ->
            runOnUiThread {
                result.onSuccess { file ->
                    try {
                        UpdateManager.openInstaller(this, file)
                    } catch (_: Exception) {
                        Toast.makeText(this, AppStrings.t(this, "download_error"), Toast.LENGTH_LONG).show()
                    }
                }.onFailure {
                    Toast.makeText(this, AppStrings.t(this, "download_error"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun section(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.audio_muted))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.12f
        }
}
