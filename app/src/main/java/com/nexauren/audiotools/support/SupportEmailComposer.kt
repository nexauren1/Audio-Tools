package com.nexauren.audiotools.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.ui.PlanAccessStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SupportEmailComposer {
    private const val SUPPORT_EMAIL =
        "nexaurenstore@gmail.com"

    fun open(
        context: Context,
        type: String,
        subject: String,
        message: String
    ): Boolean {
        val user =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?: throw IllegalStateException(
                    "É necessário iniciar sessão."
                )

        val requestType =
            typeLabel(type)

        val plan =
            PlanAccessStore.current(
                context
            ).plan

        val body =
            buildBody(
                user = user,
                plan = plan,
                requestType = requestType,
                subject = subject,
                message = message
            )

        val emailIntent =
            Intent(
                Intent.ACTION_SENDTO
            ).apply {
                data =
                    Uri.parse(
                        "mailto:" +
                            SUPPORT_EMAIL
                    )

                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(
                        SUPPORT_EMAIL
                    )
                )

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "[Audio Tools · " +
                        requestType +
                        "] " +
                        subject
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    body
                )
            }

        if (
            emailIntent.resolveActivity(
                context.packageManager
            ) != null
        ) {
            context.startActivity(
                Intent.createChooser(
                    emailIntent,
                    "Escolher aplicativo de email"
                )
            )

            return true
        }

        val fallback =
            Intent(
                Intent.ACTION_SEND
            ).apply {
                type = "text/plain"

                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(
                        SUPPORT_EMAIL
                    )
                )

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "[Audio Tools · " +
                        requestType +
                        "] " +
                        subject
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    body
                )
            }

        if (
            fallback.resolveActivity(
                context.packageManager
            ) == null
        ) {
            return false
        }

        context.startActivity(
            Intent.createChooser(
                fallback,
                "Escolher aplicativo de email"
            )
        )

        return true
    }

    private fun buildBody(
        user: com.google.firebase.auth.FirebaseUser,
        plan: String,
        requestType: String,
        subject: String,
        message: String
    ): String {
        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            )

        fun format(
            timestamp: Long?
        ): String =
            timestamp
                ?.takeIf { it > 0L }
                ?.let {
                    dateFormat.format(
                        Date(it)
                    )
                }
                ?: "Não informado"

        val name =
            user.displayName
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Não informado"

        val email =
            user.email
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Não informado"

        val phone =
            user.phoneNumber
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Não informado"

        val provider =
            user.providerData
                .firstOrNull()
                ?.providerId
                ?: "password"

        return buildString {
            append(
                "Olá Nexauren Store,\n\n"
            )
            append(
                "Recebeste uma nova mensagem " +
                    "do Audio Tools.\n\n"
            )
            append(
                "=== PEDIDO ===\n"
            )
            append(
                "Tipo: " +
                    requestType +
                    "\n"
            )
            append(
                "Assunto: " +
                    subject +
                    "\n\n"
            )
            append(
                "=== DADOS DA CONTA ===\n"
            )
            append(
                "Nome: " +
                    name +
                    "\n"
            )
            append(
                "Email: " +
                    email +
                    "\n"
            )
            append(
                "Firebase UID: " +
                    user.uid +
                    "\n"
            )
            append(
                "Telefone: " +
                    phone +
                    "\n"
            )
            append(
                "Fornecedor de login: " +
                    provider +
                    "\n"
            )
            append(
                "Plano: " +
                    plan +
                    "\n"
            )
            append(
                "Conta criada em: " +
                    format(
                        user.metadata
                            ?.creationTimestamp
                    ) +
                    "\n"
            )
            append(
                "Última sessão: " +
                    format(
                        user.metadata
                            ?.lastSignInTimestamp
                    ) +
                    "\n\n"
            )
            append(
                "=== APP E DISPOSITIVO ===\n"
            )
            append(
                "Versão Audio Tools: " +
                    BuildConfig.VERSION_NAME +
                    "\n"
            )
            append(
                "Fabricante: " +
                    Build.MANUFACTURER +
                    "\n"
            )
            append(
                "Modelo: " +
                    Build.MODEL +
                    "\n"
            )
            append(
                "Android: " +
                    Build.VERSION.RELEASE +
                    " (API " +
                    Build.VERSION.SDK_INT +
                    ")\n"
            )
            append(
                "Idioma: " +
                    Locale.getDefault()
                        .toLanguageTag() +
                    "\n\n"
            )
            append(
                "=== MENSAGEM ===\n"
            )
            append(message)
            append("\n")
        }
    }

    private fun typeLabel(
        type: String
    ): String =
        when (
            type
                .trim()
                .uppercase()
        ) {
            "COMPLAINT" ->
                "Reclamação"

            "SUGGESTION" ->
                "Sugestão"

            else ->
                "Suporte"
        }
}
