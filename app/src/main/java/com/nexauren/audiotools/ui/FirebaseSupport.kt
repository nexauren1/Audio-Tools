package com.nexauren.audiotools.ui

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

object FirebaseSupport {
    fun authError(context: Context, error: Exception?): String {
        var current: Throwable? = error
        var code = ""
        var message = ""

        while (current != null) {
            if (current is FirebaseAuthException) {
                code = current.errorCode.orEmpty()
                message = current.message.orEmpty()
                break
            }

            if (message.isBlank()) {
                message = current.message.orEmpty()
            }

            current = current.cause
        }

        val normalizedMessage = message
            .replace("\\s+".toRegex(), " ")
            .trim()

        Log.e(
            "FirebaseSupport",
            "Firebase Auth failed. type=" +
                error?.javaClass?.name +
                " code=" + code +
                " message=" + normalizedMessage,
            error
        )

        return when {
            code == "ERROR_EMAIL_ALREADY_IN_USE" ->
                AuthStrings.t(context, "auth_exists")

            code == "ERROR_INVALID_CREDENTIAL" ||
                code == "ERROR_INVALID_LOGIN_CREDENTIALS" ||
                code == "ERROR_WRONG_PASSWORD" ||
                code == "ERROR_USER_NOT_FOUND" ->
                AuthStrings.t(context, "auth_invalid")

            code == "ERROR_INVALID_EMAIL" ->
                AuthStrings.t(context, "invalid_email")

            code == "ERROR_WEAK_PASSWORD" ->
                AuthStrings.t(context, "auth_weak")

            code == "ERROR_NETWORK_REQUEST_FAILED" ||
                error is FirebaseNetworkException ->
                AuthStrings.t(context, "auth_network")

            code == "ERROR_OPERATION_NOT_ALLOWED" ->
                AuthStrings.t(context, "auth_provider_disabled")

            code == "ERROR_INVALID_API_KEY" ||
                code == "ERROR_API_KEY_INVALID" ||
                code == "ERROR_API_KEY_SERVICE_BLOCKED" ||
                normalizedMessage.contains("API key", ignoreCase = true) ->
                configurationError(code, normalizedMessage)

            code == "ERROR_APP_NOT_AUTHORIZED" ||
                code == "ERROR_INVALID_APP_CREDENTIAL" ||
                normalizedMessage.contains("not authorized", ignoreCase = true) ||
                normalizedMessage.contains("authorized", ignoreCase = true) ->
                configurationError(code, normalizedMessage)

            code == "ERROR_TOO_MANY_REQUESTS" ||
                normalizedMessage.contains(
                    "TOO_MANY_ATTEMPTS",
                    ignoreCase = true
                ) ->
                AuthStrings.t(context, "auth_too_many")

            code == "ERROR_USER_DISABLED" ->
                AuthStrings.t(context, "auth_disabled")

            code == "ERROR_API_NOT_AVAILABLE" ||
                code == "ERROR_INTERNAL_ERROR" ->
                AuthStrings.t(context, "auth_internal")

            else -> {
                val safeCode = code.ifBlank { "UNKNOWN" }
                val safeMessage = normalizedMessage.take(220)

                if (safeMessage.isNotBlank()) {
                    AuthStrings.t(context, "auth_generic") +
                        " [Firebase: " + safeCode + "] " +
                        safeMessage
                } else {
                    AuthStrings.t(context, "auth_generic") +
                        " [Firebase: " + safeCode + "]"
                }
            }
        }
    }

    private fun configurationError(code: String, message: String): String {
        val safeCode = code.ifBlank { "AUTH_CONFIG_ERROR" }

        Log.e(
            "FirebaseSupport",
            "Firebase configuration problem. code=" +
                safeCode + " message=" + message
        )

        return buildString {
            append("Firebase não aceitou a configuração da app.")
            append("\nCódigo: ")
            append(safeCode)

            if (message.isNotBlank()) {
                append("\nDetalhe: ")
                append(message)
            }

            append(
                "\n\nAtualize o google-services.json " +
                    "com uma chave Firebase válida do mesmo projeto."
            )
        }
    }
}
