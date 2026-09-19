package com.nexauren.audiotools.payments

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Entitlement(
    val plan: String,
    val isPro: Boolean,
    val expiresAt: Long?
)

data class PayPalOrder(
    val orderId: String,
    val approvalUrl: String
)

object PaymentClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun getEntitlement(): Entitlement {
        val json = request(
            method = "GET",
            path = "/api/entitlement"
        )

        return Entitlement(
            plan = json.optString("plan", "FREE"),
            isPro = json.optBoolean("isPro", false),
            expiresAt = if (
                json.isNull("expiresAt")
            ) null else json.optLong("expiresAt")
        )
    }

    fun createProOrder(): PayPalOrder {
        val json = request(
            method = "POST",
            path = "/api/paypal/create-order",
            body = "{}"
        )

        if (json.optBoolean("alreadyPro")) {
            throw IllegalStateException(
                "O Pro já está ativo nesta conta."
            )
        }

        val orderId = json.optString("orderId")
        val approvalUrl = json.optString("approvalUrl")

        if (orderId.isBlank() || approvalUrl.isBlank()) {
            throw IllegalStateException(
                "O PayPal não devolveu um pedido válido."
            )
        }

        return PayPalOrder(orderId, approvalUrl)
    }

    fun captureProOrder(orderId: String): Entitlement {
        val safe = orderId.trim()
        if (safe.isBlank()) {
            throw IllegalArgumentException("Pedido PayPal inválido.")
        }

        val body = JSONObject()
            .put("orderId", safe)
            .toString()

        val json = request(
            method = "POST",
            path = "/api/paypal/capture-order",
            body = body
        )

        return Entitlement(
            plan = json.optString("plan", "FREE"),
            isPro = json.optString("plan") == "PRO",
            expiresAt = if (
                json.isNull("expiresAt")
            ) null else json.optLong("expiresAt")
        )
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null
    ): JSONObject {
        val first = performRequest(
            method,
            path,
            body,
            false
        )

        if (first.first == 401) {
            return parseSuccessful(
                performRequest(
                    method,
                    path,
                    body,
                    true
                )
            )
        }

        return parseSuccessful(first)
    }

    private fun performRequest(
        method: String,
        path: String,
        body: String?,
        forceRefreshToken: Boolean
    ): Pair<Int, String> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException(
                "É necessário iniciar sessão."
            )

        val token = Tasks.await(
            user.getIdToken(forceRefreshToken)
        ).token

        if (token.isNullOrBlank()) {
            throw IllegalStateException(
                "Não foi possível validar a sessão Firebase."
            )
        }

        val connection = (
            URL(BuildConfig.PAYMENTS_BASE_URL + path)
                .openConnection() as HttpURLConnection
        ).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer " + token)

            if (body != null) {
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }
        }

        return try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val response = stream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            code to response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSuccessful(result: Pair<Int, String>): JSONObject {
        val (code, response) = result
        val json = runCatching {
            JSONObject(response.ifBlank { "{}" })
        }.getOrElse { JSONObject() }

        if (code !in 200..299) {
            val error = json.optString("error")
                .ifBlank { "Pedido recusado pelo servidor." }
            throw IllegalStateException("$error [HTTP $code]")
        }

        return json
    }
}