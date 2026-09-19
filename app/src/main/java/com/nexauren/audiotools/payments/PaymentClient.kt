package com.nexauren.audiotools.payments

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PlanInfo(
    val id: String,
    val name: String,
    val priceUsd: Double,
    val durationDays: Int?,
    val billingInterval: String,
    val description: String,
    val includes: List<String>
)

data class Entitlement(
    val plan: String,
    val isPro: Boolean,
    val isPremium: Boolean,
    val expiresAt: Long?,
    val subscriptionId: String?
) {
    fun hasAccess(
        requiredPlan: String
    ): Boolean {
        val required =
            requiredPlan
                .uppercase()

        if (required == "FREE") {
            return true
        }

        val active =
            expiresAt == null ||
                expiresAt >
                System.currentTimeMillis()
                    .div(1000L)

        return when (required) {
            "PREMIUM" ->
                plan == "PREMIUM" &&
                    active
            "PRO" ->
                (plan == "PRO" ||
                    plan == "PREMIUM") &&
                    active
            else ->
                true
        }
    }
}

data class PayPalSubscription(
    val subscriptionId: String,
    val approvalUrl: String
)

object PaymentClient {
    private const val CONNECT_TIMEOUT_MS =
        15_000

    private const val READ_TIMEOUT_MS =
        20_000

    fun getPlans(): List<PlanInfo> {
        val json = request(
            method = "GET",
            path = "/api/plans"
        )

        val array =
            json.optJSONArray(
                "plans"
            ) ?: JSONArray()

        return buildList {
            for (
                index in 0 until
                    array.length()
            ) {
                val item =
                    array.optJSONObject(
                        index
                    ) ?: continue

                val values =
                    item.optJSONArray(
                        "includes"
                    )

                val includes =
                    buildList {
                        if (values != null) {
                            for (
                                i in 0 until
                                    values.length()
                            ) {
                                add(
                                    values.optString(
                                        i
                                    )
                                )
                            }
                        }
                    }

                add(
                    PlanInfo(
                        id =
                            item.optString(
                                "id"
                            ),
                        name =
                            item.optString(
                                "name"
                            ),
                        priceUsd =
                            item.optDouble(
                                "priceUsd",
                                0.0
                            ),
                        durationDays =
                            if (
                                item.isNull(
                                    "durationDays"
                                )
                            ) {
                                null
                            } else {
                                item.optInt(
                                    "durationDays"
                                )
                            },
                        billingInterval =
                            item.optString(
                                "billingInterval",
                                "NONE"
                            ),
                        description =
                            item.optString(
                                "description"
                            ),
                        includes =
                            includes
                    )
                )
            }
        }
    }

    fun getEntitlement():
        Entitlement {
        val json = request(
            method = "GET",
            path = "/api/entitlement"
        )

        val plan =
            json.optString(
                "plan",
                "FREE"
            )

        return Entitlement(
            plan = plan,
            isPro =
                json.optBoolean(
                    "isPro",
                    plan == "PRO" ||
                        plan == "PREMIUM"
                ),
            isPremium =
                json.optBoolean(
                    "isPremium",
                    plan == "PREMIUM"
                ),
            expiresAt =
                if (
                    json.isNull(
                        "expiresAt"
                    )
                ) {
                    null
                } else {
                    json.optLong(
                        "expiresAt"
                    )
                },
            subscriptionId =
                json.optString(
                    "subscriptionId",
                    ""
                ).ifBlank {
                    null
                }
        )
    }

    fun createSubscription(
        planId: String
    ): PayPalSubscription {
        val body =
            JSONObject()
                .put(
                    "planId",
                    planId.uppercase()
                )
                .toString()

        val json = request(
            method = "POST",
            path =
                "/api/paypal/create-subscription",
            body = body
        )

        val id =
            json.optString(
                "subscriptionId"
            )

        val approval =
            json.optString(
                "approvalUrl"
            )

        if (
            id.isBlank() ||
            approval.isBlank()
        ) {
            throw IllegalStateException(
                "O PayPal não devolveu uma assinatura válida."
            )
        }

        return PayPalSubscription(
            id,
            approval
        )
    }

    fun activateSubscription(
        subscriptionId: String
    ): Entitlement {
        val safe =
            subscriptionId.trim()

        if (safe.isBlank()) {
            throw IllegalArgumentException(
                "Assinatura PayPal inválida."
            )
        }

        val body =
            JSONObject()
                .put(
                    "subscriptionId",
                    safe
                )
                .toString()

        val json = request(
            method = "POST",
            path =
                "/api/paypal/activate-subscription",
            body = body
        )

        val plan =
            json.optString(
                "plan",
                "FREE"
            )

        return Entitlement(
            plan = plan,
            isPro =
                plan == "PRO" ||
                    plan == "PREMIUM",
            isPremium =
                plan == "PREMIUM",
            expiresAt =
                if (
                    json.isNull(
                        "expiresAt"
                    )
                ) {
                    null
                } else {
                    json.optLong(
                        "expiresAt"
                    )
                },
            subscriptionId =
                json.optString(
                    "subscriptionId",
                    ""
                ).ifBlank {
                    null
                }
        )
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null
    ): JSONObject {
        val first =
            performRequest(
                method,
                path,
                body,
                false
            )

        if (
            first.first == 401
        ) {
            return parseSuccessful(
                performRequest(
                    method,
                    path,
                    body,
                    true
                )
            )
        }

        return parseSuccessful(
            first
        )
    }

    private fun performRequest(
        method: String,
        path: String,
        body: String?,
        forceRefreshToken: Boolean
    ): Pair<Int, String> {
        val user =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?: throw IllegalStateException(
                    "É necessário iniciar sessão."
                )

        val token =
            Tasks.await(
                user.getIdToken(
                    forceRefreshToken
                )
            ).token

        if (token.isNullOrBlank()) {
            throw IllegalStateException(
                "Não foi possível validar a sessão Firebase."
            )
        }

        val connection = (
            URL(
                BuildConfig
                    .PAYMENTS_BASE_URL +
                    path
            ).openConnection()
                as HttpURLConnection
        ).apply {
            requestMethod =
                method
            connectTimeout =
                CONNECT_TIMEOUT_MS
            readTimeout =
                READ_TIMEOUT_MS
            useCaches = false
            doInput = true

            setRequestProperty(
                "Accept",
                "application/json"
            )

            setRequestProperty(
                "Authorization",
                "Bearer " + token
            )

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
                connection.outputStream
                    .use { output ->
                        output.write(
                            body.toByteArray(
                                Charsets.UTF_8
                            )
                        )
                    }
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
                stream.bufferedReader(
                    Charsets.UTF_8
                ).use {
                    it.readText()
                }

            code to response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSuccessful(
        result: Pair<Int, String>
    ): JSONObject {
        val (
            code,
            response
        ) = result

        val json =
            runCatching {
                JSONObject(
                    response.ifBlank {
                        "{}"
                    }
                )
            }.getOrElse {
                JSONObject()
            }

        if (code !in 200..299) {
            val error =
                json.optString(
                    "error"
                ).ifBlank {
                    "Pedido recusado pelo servidor."
                }

            val details =
                json.optString(
                    "details"
                ).ifBlank {
                    ""
                }

            throw IllegalStateException(
                if (
                    details.isBlank()
                ) {
                    "$error [HTTP $code]"
                } else {
                    "$error: $details [HTTP $code]"
                }
            )
        }

        return json
    }
}
