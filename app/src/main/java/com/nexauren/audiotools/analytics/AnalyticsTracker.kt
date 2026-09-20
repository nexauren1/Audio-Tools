package com.nexauren.audiotools.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsTracker {
    private fun event(
        context: Context,
        name: String,
        fill: Bundle.() -> Unit = {}
    ) {
        runCatching {
            FirebaseAnalytics
                .getInstance(context)
                .logEvent(name, Bundle().apply(fill))
        }
    }

    private fun errorType(error: Throwable?): String =
        error?.javaClass?.simpleName
            ?.take(60)
            .orEmpty()
            .ifBlank { "unknown" }

    fun appStarted(context: Context) =
        event(context, "app_started")

    fun authScreenViewed(context: Context) =
        event(context, "auth_screen_viewed")

    fun authAttempt(context: Context, method: String, mode: String) =
        event(context, "auth_attempt") {
            putString("method", method)
            putString("mode", mode)
        }

    fun authSuccess(
        context: Context,
        method: String,
        mode: String,
        userId: String?
    ) {
        runCatching {
            FirebaseAnalytics
                .getInstance(context)
                .setUserId(userId)
        }
        event(context, "auth_success") {
            putString("method", method)
            putString("mode", mode)
        }
    }

    fun authFailure(
        context: Context,
        method: String,
        mode: String,
        error: Throwable?
    ) = event(context, "auth_failure") {
        putString("method", method)
        putString("mode", mode)
        putString("error_type", errorType(error))
    }

    fun passwordResetRequested(context: Context) =
        event(context, "password_reset_requested")

    fun passwordResetResult(
        context: Context,
        success: Boolean,
        error: Throwable? = null
    ) = event(context, "password_reset_result") {
        putString("status", if (success) "success" else "failure")
        if (!success) {
            putString("error_type", errorType(error))
        }
    }

    fun logout(context: Context) =
        event(context, "logout")

    fun screenViewed(context: Context, screen: String) =
        event(context, "screen_viewed") {
            putString("screen_name", screen)
        }

    fun toolOpened(
        context: Context,
        toolId: String,
        requiredPlan: String
    ) = event(context, "tool_opened") {
        putString("tool_id", toolId)
        putString("required_plan", requiredPlan)
    }

    fun toolFileSelected(context: Context, toolId: String) =
        event(context, "tool_file_selected") {
            putString("tool_id", toolId)
        }

    fun toolProcessStarted(
        context: Context,
        toolId: String,
        action: String
    ) = event(context, "tool_process_started") {
        putString("tool_id", toolId)
        putString("action", action)
    }

    fun toolCompleted(
        context: Context,
        toolId: String,
        action: String
    ) = event(context, "tool_completed") {
        putString("tool_id", toolId)
        putString("action", action)
    }

    fun toolFailed(
        context: Context,
        toolId: String,
        action: String,
        error: Throwable? = null
    ) = event(context, "tool_failed") {
        putString("tool_id", toolId)
        putString("action", action)
        putString("error_type", errorType(error))
    }

    fun resultPlayed(context: Context, toolId: String) =
        event(context, "result_played") {
            putString("tool_id", toolId)
        }

    fun resultSaved(context: Context, toolId: String) =
        event(context, "result_saved") {
            putString("tool_id", toolId)
        }

    fun resultShareOpened(context: Context, toolId: String) =
        event(context, "result_share_opened") {
            putString("tool_id", toolId)
        }

    fun resultCopied(context: Context, toolId: String) =
        event(context, "result_copied") {
            putString("tool_id", toolId)
        }

    fun favoriteToggled(
        context: Context,
        toolId: String,
        favorite: Boolean
    ) = event(context, "favorite_toggled") {
        putString("tool_id", toolId)
        putString("status", if (favorite) "added" else "removed")
    }

    fun searchUsed(context: Context, resultCount: Int) =
        event(context, "tool_search_used") {
            putInt("result_count", resultCount)
        }

    fun plansViewed(context: Context) =
        event(context, "plans_viewed")

    fun plansLoaded(context: Context, count: Int) =
        event(context, "plans_loaded") {
            putInt("plan_count", count)
        }

    fun plansLoadFailed(
        context: Context,
        error: Throwable?
    ) = event(context, "plans_load_failed") {
        putString("error_type", errorType(error))
    }

    fun toolAccessCheckFailed(
        context: Context,
        toolId: String,
        error: Throwable?
    ) = event(context, "tool_access_check_failed") {
        putString("tool_id", toolId)
        putString("error_type", errorType(error))
    }

    fun paymentCancelled(context: Context) =
        event(context, "payment_cancelled")

    fun entitlementLoaded(
        context: Context,
        plan: String,
        source: String
    ) {
        runCatching {
            FirebaseAnalytics
                .getInstance(context)
                .setUserProperty(
                    "audio_plan",
                    plan.lowercase()
                )
        }
        event(context, "entitlement_loaded") {
            putString("plan", plan)
            putString("source", source)
        }
    }

    fun planSelected(context: Context, planId: String) =
        event(context, "plan_selected") {
            putString("plan_id", planId)
        }

    fun checkoutStarted(
        context: Context,
        planId: String,
        priceUsd: Double
    ) = event(context, "begin_checkout") {
        putString("plan_id", planId)
        putDouble("price_usd", priceUsd)
        putString("currency", "USD")
    }

    fun checkoutCreated(context: Context, planId: String) =
        event(context, "checkout_created") {
            putString("plan_id", planId)
        }

    fun checkoutOpened(context: Context, planId: String) =
        event(context, "checkout_opened") {
            putString("plan_id", planId)
        }

    fun checkoutFailed(
        context: Context,
        planId: String,
        stage: String,
        error: Throwable? = null
    ) = event(context, "checkout_failed") {
        putString("plan_id", planId)
        putString("stage", stage)
        putString("error_type", errorType(error))
    }

    fun paymentReturnReceived(
        context: Context,
        hasSubscriptionId: Boolean
    ) = event(context, "payment_return_received") {
        putString(
            "status",
            if (hasSubscriptionId) "identified" else "unidentified"
        )
    }

    fun paymentActivationStarted(context: Context) =
        event(context, "payment_activation_started")

    fun paymentCompleted(
        context: Context,
        planId: String,
        subscriptionId: String?,
        priceUsd: Double? = null
    ) {
        event(context, "payment_completed") {
            putString("plan_id", planId)
            putString("status", "success")
        }

        if (priceUsd != null && priceUsd > 0.0) {
            event(context, "purchase") {
                putString("transaction_id", subscriptionId.orEmpty())
                putString("currency", "USD")
                putDouble("value", priceUsd)
                putString("item_id", planId)
                putString("item_name", planId)
            }
        }
    }

    fun paymentActivationFailed(
        context: Context,
        error: Throwable?
    ) = event(context, "payment_activation_failed") {
        putString("error_type", errorType(error))
    }
}
