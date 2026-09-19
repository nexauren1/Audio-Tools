package com.nexauren.audiotools.support

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupportClient {
    private const val CONNECT_TIMEOUT_MS =
        15_000

    private const val READ_TIMEOUT_MS =
        20_000

    fun send(
        type: String,
        subject: String,
        message: String
    ) {
        val user =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?: throw IllegalStateException(
                    "É necessário iniciar sessão."
                )

        val token =
            Tasks.await(
                user.getIdToken(false)
            ).token

        if (
            token.isNullOrBlank()
        ) {
            throw IllegalStateException(
                "Não foi possível validar a sessão Firebase."
            )
        }

        val body =
            JSONObject()
                .put(
                    "type",
                    type
                )
                .put(
                    "subject",
                    subject
                )
                .put(
                    "message",
                    message
                )
                .put(
                    "name",
                    user.displayName
                        ?.trim()
                        .orEmpty()
                )
                .put(
                    "appVersion",
                    BuildConfig.VERSION_NAME
                )
                .toString()

        val connection =
            (
                URL(
                    BuildConfig
                        .PAYMENTS_BASE_URL +
                        "/api/support/contact"
                ).openConnection()
                    as HttpURLConnection
            ).apply {
                requestMethod =
                    "POST"
                connectTimeout =
                    CONNECT_TIMEOUT_MS
                readTimeout =
                    READ_TIMEOUT_MS
                useCaches = false
                doInput = true
                doOutput = true

                setRequestProperty(
                    "Accept",
                    "application/json"
                )

                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                setRequestProperty(
                    "Authorization",
                    "Bearer " + token
                )
            }

        try {
            connection.outputStream
                .use { output ->
                    output.write(
                        body.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }

            val code =
                connection.responseCode

            val stream =
                if (
                    code in 200..299
                ) {
                    connection.inputStream
                } else {
                    connection.errorStream
                        ?: connection.inputStream
                }

            val response =
                stream
                    .bufferedReader(
                        Charsets.UTF_8
                    )
                    .use {
                        it.readText()
                    }

            val json =
                runCatching {
                    JSONObject(
                        response.ifBlank {
                            "{}"
                        }
                    )
                }.getOrDefault(
                    JSONObject()
                )

            if (
                code !in 200..299
            ) {
                val error =
                    json.optString(
                        "error"
                    ).ifBlank {
                        "Não foi possível enviar o pedido."
                    }

                throw IllegalStateException(
                    "$error [HTTP $code]"
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
