package com.nexauren.audiotools.ui

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.nexauren.audiotools.payments.Entitlement

object PlanAccessStore {
    private const val PREFS =
        "audio_tools_plan_access"

    private const val KEY_UID =
        "uid"

    private const val KEY_PLAN =
        "plan"

    private const val KEY_EXPIRES_AT =
        "expires_at"

    private const val KEY_SUBSCRIPTION_ID =
        "subscription_id"

    fun current(
        context: Context
    ): Entitlement {
        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid

        if (
            uid.isNullOrBlank() ||
            prefs.getString(
                KEY_UID,
                null
            ) != uid
        ) {
            return free()
        }

        val plan =
            prefs.getString(
                KEY_PLAN,
                "FREE"
            )
                ?.uppercase()
                ?: "FREE"

        val expiresAt =
            if (
                !prefs.contains(
                    KEY_EXPIRES_AT
                )
            ) {
                null
            } else {
                prefs.getLong(
                    KEY_EXPIRES_AT,
                    0L
                ).takeIf {
                    it > 0L
                }
            }

        return Entitlement(
            plan = plan,
            isPro =
                plan == "PRO" ||
                    plan == "PREMIUM",
            isPremium =
                plan == "PREMIUM",
            expiresAt = expiresAt,
            subscriptionId =
                prefs.getString(
                    KEY_SUBSCRIPTION_ID,
                    null
                )
        )
    }

    fun save(
        context: Context,
        entitlement: Entitlement
    ) {
        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: return

        val editor =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).edit()

        editor
            .putString(
                KEY_UID,
                uid
            )
            .putString(
                KEY_PLAN,
                entitlement.plan
                    .uppercase()
            )

        if (
            entitlement.expiresAt != null
        ) {
            editor.putLong(
                KEY_EXPIRES_AT,
                entitlement.expiresAt
            )
        } else {
            editor.remove(
                KEY_EXPIRES_AT
            )
        }

        if (
            entitlement.subscriptionId
                .isNullOrBlank()
        ) {
            editor.remove(
                KEY_SUBSCRIPTION_ID
            )
        } else {
            editor.putString(
                KEY_SUBSCRIPTION_ID,
                entitlement.subscriptionId
            )
        }

        editor.apply()
    }

    fun hasAccess(
        context: Context,
        requiredPlan: String
    ): Boolean =
        current(context)
            .hasAccess(requiredPlan)

    fun clear(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }

    private fun free(): Entitlement =
        Entitlement(
            plan = "FREE",
            isPro = false,
            isPremium = false,
            expiresAt = null,
            subscriptionId = null
        )
}
