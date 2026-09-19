package com.nexauren.audiotools.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.material.button.MaterialButton
import com.nexauren.audiotools.R

class AuthActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var submitButton: MaterialButton
    private lateinit var switchButton: MaterialButton
    private var forgotButton: MaterialButton? = null
    private var messageView: TextView? = null
    private var registerMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val firebaseApp = try {
                FirebaseApp.getInstance()
            } catch (_: IllegalStateException) {
                FirebaseApp.initializeApp(this)
            }

            if (firebaseApp == null) {
                Log.e(
                    "AuthActivity",
                    "Firebase initialization returned null. " +
                        "Default Firebase options are unavailable."
                )
                buildUi()
                showMessage(
                    AuthStrings.t(this, "auth_config") +
                        " [Firebase: INIT_NO_DEFAULT_APP]"
                )
                return
            }

            Log.d(
                "AuthActivity",
                "Firebase initialized. projectId=" +
                    firebaseApp.options.projectId +
                    " applicationId=" +
                    firebaseApp.options.applicationId +
                    " package=" + packageName
            )

            auth = FirebaseAuth.getInstance(firebaseApp)
        } catch (exception: Exception) {
            Log.e(
                "AuthActivity",
                "Firebase initialization failed.",
                exception
            )
            buildUi()
            showMessage(firebaseInitError(exception))
            return
        }

        if (auth.currentUser != null) {
            openMain()
            return
        }

        buildUi()
    }

    private fun firebaseInitError(error: Throwable?): String {
        var current: Throwable? = error
        var code = ""
        var message = ""

        while (current != null) {
            if (current is com.google.firebase.auth.FirebaseAuthException) {
                code = current.errorCode.orEmpty()
                message = current.message.orEmpty()
                break
            }

            if (message.isBlank()) {
                message = current.message.orEmpty()
            }

            current = current.cause
        }

        val safeCode = code.ifBlank { "INIT_ERROR" }
        val safeMessage = message
            .replace("\\s+".toRegex(), " ")
            .take(220)

        Log.e(
            "AuthActivity",
            "Firebase init diagnostic. " +
                "type=" + error?.javaClass?.name +
                " code=" + safeCode +
                " message=" + safeMessage,
            error
        )

        return buildString {
            append(AuthStrings.t(this@AuthActivity, "auth_config"))
            append(" [Firebase: ")
            append(safeCode)
            append("]")

            if (safeMessage.isNotBlank()) {
                append(" ")
                append(safeMessage)
            }
        }
    }

    private fun buildUi() {
        val root = ViewKit.page(this)
        root.setPadding(
            ViewKit.dp(this, 18),
            ViewKit.dp(this, 24),
            ViewKit.dp(this, 18),
            ViewKit.dp(this, 34)
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ViewKit.iconBadge(this, "AT", R.color.audio_blue).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewKit.dp(this@AuthActivity, 48),
                ViewKit.dp(this@AuthActivity, 48)
            ).apply {
                rightMargin = ViewKit.dp(this@AuthActivity, 12)
            }
        })

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        brand.addView(ViewKit.eyebrow(this, "NEXAUREN"))
        brand.addView(ViewKit.title(this, "AUDIO TOOLS", 17f))
        header.addView(brand)
        root.addView(header)

        root.addView(ViewKit.spacer(this, 30))
        root.addView(ViewKit.pill(this, AuthStrings.t(this, "account"), colorRes = R.color.audio_blue))
        root.addView(ViewKit.spacer(this, 12))
        root.addView(
            ViewKit.title(
                this,
                if (registerMode) AuthStrings.t(this, "create_account")
                else AuthStrings.t(this, "welcome_back"),
                30f
            )
        )
        root.addView(ViewKit.spacer(this, 7))
        root.addView(
            ViewKit.subtitle(
                this,
                if (registerMode) AuthStrings.t(this, "create_desc")
                else AuthStrings.t(this, "sign_in_desc")
            )
        )
        root.addView(ViewKit.spacer(this, 18))

        val card = ViewKit.card(this, accentColorRes = R.color.audio_blue)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ViewKit.dp(this@AuthActivity, 16),
                ViewKit.dp(this@AuthActivity, 17),
                ViewKit.dp(this@AuthActivity, 16),
                ViewKit.dp(this@AuthActivity, 17)
            )
        }

        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val nameField = if (registerMode) {
            inputField(
                AuthStrings.t(this, "name"),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            ).also { fields.addView(it.first) }
        } else null

        val email = inputField(
            AuthStrings.t(this, "email"),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        fields.addView(email.first)

        val password = inputField(
            AuthStrings.t(this, "password"),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        fields.addView(password.first)

        val confirm = if (registerMode) {
            inputField(
                AuthStrings.t(this, "confirm_password"),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ).also { fields.addView(it.first) }
        } else null

        nameField?.second?.setSingleLine(true)
        email.second.setSingleLine(true)
        password.second.setSingleLine(true)
        confirm?.second?.setSingleLine(true)

        content.addView(fields)
        content.addView(ViewKit.spacer(this, 4))

        messageView = TextView(this).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@AuthActivity, R.color.audio_danger))
            setLineSpacing(1.1f, 1f)
            visibility = View.GONE
        }
        content.addView(messageView)
        content.addView(ViewKit.spacer(this, 7))

        submitButton = ViewKit.button(
            this,
            if (registerMode) AuthStrings.t(this, "create_account")
            else AuthStrings.t(this, "sign_in"),
            true,
            R.color.audio_blue
        )
        submitButton.setOnClickListener {
            submit(
                nameField?.second,
                email.second,
                password.second,
                confirm?.second
            )
        }
        content.addView(submitButton)
        content.addView(ViewKit.spacer(this, 8))

        switchButton = ViewKit.button(
            this,
            if (registerMode) AuthStrings.t(this, "switch_login")
            else AuthStrings.t(this, "switch_register"),
            false,
            R.color.audio_blue
        )
        switchButton.setOnClickListener {
            registerMode = !registerMode
            buildUi()
        }
        content.addView(switchButton)

        if (!registerMode) {
            forgotButton = ViewKit.button(
                this,
                AuthStrings.t(this, "forgot_password"),
                false,
                R.color.audio_green
            )
            forgotButton!!.setOnClickListener {
                sendReset(email.second)
            }
            content.addView(ViewKit.spacer(this, 8))
            content.addView(forgotButton)
        } else {
            forgotButton = null
        }

        card.addView(content)
        root.addView(card)

        root.addView(ViewKit.spacer(this, 14))
        root.addView(ViewKit.subtitle(this, AuthStrings.t(this, "security_note")))

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(root)
            }
        )
    }

    private fun inputField(label: String, inputType: Int): Pair<LinearLayout, EditText> {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ViewKit.dp(this@AuthActivity, 9)
            }
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@AuthActivity, R.color.audio_muted))
        }
        wrapper.addView(labelView)
        wrapper.addView(ViewKit.spacer(this, 5))

        val field = EditText(this).apply {
            this.inputType = inputType
            minHeight = ViewKit.dp(this@AuthActivity, 52)
            setPadding(
                ViewKit.dp(this@AuthActivity, 14),
                ViewKit.dp(this@AuthActivity, 8),
                ViewKit.dp(this@AuthActivity, 14),
                ViewKit.dp(this@AuthActivity, 8)
            )
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@AuthActivity, R.color.audio_text))
            setHintTextColor(ContextCompat.getColor(this@AuthActivity, R.color.audio_muted))
            background = GradientDrawable().apply {
                cornerRadius = ViewKit.dp(this@AuthActivity, 15).toFloat()
                setColor(ContextCompat.getColor(this@AuthActivity, R.color.audio_surface_alt))
                setStroke(
                    ViewKit.dp(this@AuthActivity, 1),
                    ContextCompat.getColor(this@AuthActivity, R.color.audio_border)
                )
            }
        }
        wrapper.addView(field)
        return wrapper to field
    }

    private fun submit(
        name: EditText?,
        email: EditText,
        password: EditText,
        confirm: EditText?
    ) {
        hideMessage()

        val emailValue = email.text.toString().trim()
        val passwordValue = password.text.toString()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            showMessage(AuthStrings.t(this, "invalid_email"))
            return
        }

        if (passwordValue.length < 6) {
            showMessage(AuthStrings.t(this, "password_short"))
            return
        }

        if (registerMode) {
            val nameValue = name?.text?.toString()?.trim().orEmpty()
            if (nameValue.isBlank()) {
                showMessage(AuthStrings.t(this, "name_required"))
                return
            }
            if (passwordValue != confirm?.text?.toString()) {
                showMessage(AuthStrings.t(this, "password_mismatch"))
                return
            }
            setLoading(true)
            auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        setLoading(false)
                        showMessage(authError(task.exception))
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser
                    if (user == null) {
                        setLoading(false)
                        showMessage(AuthStrings.t(this, "auth_generic"))
                        return@addOnCompleteListener
                    }

                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(nameValue)
                        .build()

                    user.updateProfile(profile).addOnCompleteListener {
                        saveProfile(user, nameValue)
                        openMain()
                    }
                }
        } else {
            setLoading(true)
            auth.signInWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        setLoading(false)
                        showMessage(authError(task.exception))
                        return@addOnCompleteListener
                    }

                    auth.currentUser?.let { saveProfile(it, it.displayName.orEmpty()) }
                    openMain()
                }
        }
    }

    private fun saveProfile(user: FirebaseUser, name: String) {
        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to name,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(
                data,
                SetOptions.merge()
            )
    }

    private fun sendReset(email: EditText) {
        hideMessage()
        val emailValue = email.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            showMessage(AuthStrings.t(this, "reset_invalid"))
            return
        }

        forgotButton?.isEnabled = false
        auth.sendPasswordResetEmail(emailValue)
            .addOnCompleteListener {
                forgotButton?.isEnabled = true
                if (it.isSuccessful) {
                    Toast.makeText(
                        this,
                        AuthStrings.t(this, "reset_sent"),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    showMessage(authError(it.exception))
                }
            }
    }

    private fun authError(error: Exception?): String {
        var current: Throwable? = error
        var code = ""
        var message = ""

        while (current != null) {
            if (current is com.google.firebase.auth.FirebaseAuthException) {
                code = current.errorCode.orEmpty()
                message = current.message.orEmpty()
                break
            }
            if (message.isBlank()) {
                message = current.message.orEmpty()
            }
            current = current.cause
        }

        Log.e(
            "AuthActivity",
            "Firebase Auth failed. type=" +
                error?.javaClass?.name + " code=" + code +
                " message=" + message,
            error
        )

        return when {
            code == "ERROR_EMAIL_ALREADY_IN_USE" ->
                AuthStrings.t(this, "auth_exists")
            code == "ERROR_INVALID_CREDENTIAL" ||
                code == "ERROR_INVALID_LOGIN_CREDENTIALS" ||
                code == "ERROR_WRONG_PASSWORD" ||
                code == "ERROR_USER_NOT_FOUND" ->
                AuthStrings.t(this, "auth_invalid")
            code == "ERROR_INVALID_EMAIL" ->
                AuthStrings.t(this, "invalid_email")
            code == "ERROR_WEAK_PASSWORD" ->
                AuthStrings.t(this, "auth_weak")
            code == "ERROR_NETWORK_REQUEST_FAILED" ||
                error is com.google.firebase.FirebaseNetworkException ->
                AuthStrings.t(this, "auth_network")
            code == "ERROR_OPERATION_NOT_ALLOWED" ->
                AuthStrings.t(this, "auth_provider_disabled")
            code == "ERROR_INVALID_API_KEY" ||
                code == "ERROR_API_KEY_INVALID" ||
                code == "ERROR_API_KEY_SERVICE_BLOCKED" ||
                message.contains("API key", ignoreCase = true) ->
                AuthStrings.t(this, "auth_invalid_config")
            code == "ERROR_APP_NOT_AUTHORIZED" ||
                code == "ERROR_INVALID_APP_CREDENTIAL" ||
                message.contains("authorized", ignoreCase = true) ->
                AuthStrings.t(this, "auth_app_not_authorized")
            code == "ERROR_TOO_MANY_REQUESTS" ||
                message.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                AuthStrings.t(this, "auth_too_many")
            code == "ERROR_USER_DISABLED" ->
                AuthStrings.t(this, "auth_disabled")
            code == "ERROR_API_NOT_AVAILABLE" ||
                code == "ERROR_INTERNAL_ERROR" ->
                AuthStrings.t(this, "auth_internal")
            else -> {
                val safeCode = code.ifBlank { "UNKNOWN" }
                val safeMessage = message
                    .replace("\\s+".toRegex(), " ")
                    .take(180)
                if (safeMessage.isNotBlank()) {
                    AuthStrings.t(this, "auth_generic") +
                        " [Firebase: " + safeCode + "] " + safeMessage
                } else {
                    AuthStrings.t(this, "auth_generic") +
                        " [Firebase: " + safeCode + "]"
                }
            }
        }
    }
    private fun setLoading(loading: Boolean) {
        submitButton.isEnabled = !loading
        switchButton.isEnabled = !loading
        forgotButton?.isEnabled = !loading
        submitButton.text =
            if (loading) {
                AuthStrings.t(this, "loading")
            } else if (registerMode) {
                AuthStrings.t(this, "create_account")
            } else {
                AuthStrings.t(this, "sign_in")
            }
    }

    private fun showMessage(message: String) {
        messageView?.text = message
        messageView?.visibility = View.VISIBLE
    }

    private fun hideMessage() {
        messageView?.text = ""
        messageView?.visibility = View.GONE
    }

    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
