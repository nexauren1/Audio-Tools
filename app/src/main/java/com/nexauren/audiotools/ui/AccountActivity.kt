package com.nexauren.audiotools.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.R

class AccountActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            openAuth()
            return
        }
        buildUi()
    }

    private fun buildUi() {
        val user = auth.currentUser ?: run {
            openAuth()
            return
        }

        val root = ViewKit.page(this)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(ViewKit.button(this, "‹", false).apply {
            minWidth = ViewKit.dp(this@AccountActivity, 48)
            minHeight = ViewKit.dp(this@AccountActivity, 48)
            contentDescription = AuthStrings.t(this@AccountActivity, "cancel")
            setOnClickListener { finish() }
        })

        header.addView(ViewKit.title(
            this,
            AuthStrings.t(this, "account"),
            23f
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = ViewKit.dp(this@AccountActivity, 11)
            }
        })
        root.addView(header)

        root.addView(ViewKit.spacer(this, 25))

        val displayName = user.displayName?.trim().takeUnless { it.isNullOrBlank() }
            ?: AuthStrings.t(this, "profile")
        val initials = displayName
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "AT" }

        val hero = ViewKit.hero(this)
        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                ViewKit.dp(this@AccountActivity, 20),
                ViewKit.dp(this@AccountActivity, 22),
                ViewKit.dp(this@AccountActivity, 20),
                ViewKit.dp(this@AccountActivity, 22)
            )
        }

        heroContent.addView(ViewKit.iconBadge(
            this,
            initials,
            R.color.audio_blue
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewKit.dp(this@AccountActivity, 68),
                ViewKit.dp(this@AccountActivity, 68)
            )
        })
        heroContent.addView(ViewKit.spacer(this, 12))
        heroContent.addView(ViewKit.title(this, displayName, 24f))
        heroContent.addView(ViewKit.spacer(this, 5))
        heroContent.addView(TextView(this).apply {
            text = user.email ?: ""
            textSize = 13.5f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@AccountActivity, R.color.audio_muted))
        })
        hero.addView(heroContent)
        root.addView(hero)

        root.addView(ViewKit.spacer(this, 16))

        val security = ViewKit.card(this, accentColorRes = R.color.audio_green)
        val securityContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@AccountActivity, 15),
                ViewKit.dp(this@AccountActivity, 15),
                ViewKit.dp(this@AccountActivity, 15),
                ViewKit.dp(this@AccountActivity, 15)
            )
        }
        securityContent.addView(ViewKit.eyebrow(
            this,
            AuthStrings.t(this, "password_reset")
        ))
        securityContent.addView(ViewKit.spacer(this, 5))
        securityContent.addView(ViewKit.subtitle(
            this,
            AuthStrings.t(this, "password_reset_desc")
        ))
        securityContent.addView(ViewKit.spacer(this, 10))
        securityContent.addView(ViewKit.button(
            this,
            AuthStrings.t(this, "password_reset"),
            false,
            R.color.audio_green
        ).apply {
            setOnClickListener {
                val email = auth.currentUser?.email
                if (email.isNullOrBlank()) return@setOnClickListener
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        Toast.makeText(
                            this@AccountActivity,
                            if (task.isSuccessful) {
                                AuthStrings.t(this@AccountActivity, "reset_sent")
                            } else {
                                FirebaseSupport.authError(
                                    this@AccountActivity,
                                    task.exception
                                )
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        })
        security.addView(securityContent)
        root.addView(security)

        root.addView(ViewKit.spacer(this, 18))

        root.addView(ViewKit.button(
            this,
            AuthStrings.t(this, "logout"),
            true,
            R.color.audio_red
        ).apply {
            setOnClickListener { confirmLogout() }
        })

        root.addView(ViewKit.spacer(this, 10))
        root.addView(ViewKit.button(
            this,
            AuthStrings.t(this, "cancel"),
            false,
            R.color.audio_blue
        ).apply {
            setOnClickListener { finish() }
        })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
            addView(root)
        })
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(AuthStrings.t(this, "logout_title"))
            .setMessage(AuthStrings.t(this, "logout_desc"))
            .setNegativeButton(AuthStrings.t(this, "cancel"), null)
            .setPositiveButton(AuthStrings.t(this, "logout")) { _, _ ->
                auth.signOut()
                openAuth()
            }
            .show()
    }

    private fun openAuth() {
        startActivity(
            Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
