package com.nexauren.audiotools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.nexauren.audiotools.R
import com.nexauren.audiotools.payments.Entitlement
import com.nexauren.audiotools.payments.PaymentClient
import com.nexauren.audiotools.payments.PlanInfo
import java.util.Locale
import java.util.concurrent.Executors

class UpgradeActivity : ComponentActivity() {
    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var plansColumn:
        LinearLayout? = null

    private var currentPlanView:
        TextView? = null

    private var loading:
        ProgressBar? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        if (
            FirebaseAuth
                .getInstance()
                .currentUser == null
        ) {
            startActivity(
                Intent(
                    this,
                    AuthActivity::class.java
                ).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        buildUi()
        handlePaymentReturn(intent)
        loadData()
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentReturn(intent)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val root =
            ViewKit.page(this)

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            ViewKit.button(
                this,
                "‹",
                false,
                R.color.audio_blue
            ).apply {
                minWidth =
                    ViewKit.dp(
                        this@UpgradeActivity,
                        48
                    )
                minHeight =
                    ViewKit.dp(
                        this@UpgradeActivity,
                        48
                    )
                contentDescription =
                    AppStrings.t(
                        this@UpgradeActivity,
                        "back"
                    )
                setOnClickListener {
                    finish()
                }
            }
        )

        val heading =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin =
                            ViewKit.dp(
                                this@UpgradeActivity,
                                11
                            )
                    }
            }

        heading.addView(
            ViewKit.eyebrow(
                this,
                "AUDIO TOOLS"
            )
        )

        heading.addView(
            ViewKit.title(
                this,
                "Planos",
                23f
            )
        )

        header.addView(
            heading
        )

        root.addView(header)
        root.addView(
            ViewKit.spacer(this, 20)
        )

        root.addView(
            ViewKit.title(
                this,
                "Desbloqueia mais ferramentas.",
                29f
            )
        )

        root.addView(
            ViewKit.spacer(this, 7)
        )

        root.addView(
            ViewKit.subtitle(
                this,
                "Escolhe um plano para desbloquear as ferramentas correspondentes. Pro e Premium são assinaturas mensais com renovação automática pelo PayPal."
            )
        )

        root.addView(
            ViewKit.spacer(this, 16)
        )

        val statusCard =
            ViewKit.card(
                this,
                accentColorRes =
                    R.color.audio_blue
            )

        val statusContent =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(
                        this@UpgradeActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        15
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        15
                    )
                )
            }

        statusContent.addView(
            ViewKit.eyebrow(
                this,
                "PLANO ATUAL"
            )
        )

        currentPlanView =
            TextView(this).apply {
                text =
                    "A verificar…"
                textSize = 18f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        R.color.audio_text
                    )
                )
            }

        statusContent.addView(
            currentPlanView
        )

        statusContent.addView(
            ViewKit.spacer(this, 4)
        )

        statusContent.addView(
            TextView(this).apply {
                text =
                    "O acesso é verificado antes de abrir cada ferramenta paga."
                textSize = 11.5f
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        statusCard.addView(
            statusContent
        )

        root.addView(
            statusCard
        )

        root.addView(
            ViewKit.spacer(this, 18)
        )

        loading =
            ProgressBar(this).apply {
                visibility =
                    View.VISIBLE
            }

        root.addView(loading)

        plansColumn =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                visibility =
                    View.GONE
            }

        root.addView(
            plansColumn
        )

        root.addView(
            ViewKit.spacer(this, 16)
        )

        root.addView(
            TextView(this).apply {
                text =
                    "A tua conta continua no Firebase. O Worker só controla o acesso e confirma a assinatura no PayPal."
                textSize = 11.5f
                setLineSpacing(
                    1.1f,
                    1f
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
                addView(root)
            }
        )
    }

    private fun loadData() {
        executor.execute {
            try {
                val plans =
                    PaymentClient
                        .getPlans()

                val entitlement =
                    PaymentClient
                        .getEntitlement()

                mainHandler.post {
                    render(
                        plans,
                        entitlement
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    loading?.visibility =
                        View.GONE

                    currentPlanView?.text =
                        "Não foi possível carregar os planos."

                    Toast.makeText(
                        this,
                        error.message.orEmpty(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun render(
        plans: List<PlanInfo>,
        entitlement: Entitlement
    ) {
        currentPlanView?.text =
            when (
                entitlement.plan
            ) {
                "PREMIUM" ->
                    "Premium ativo"
                "PRO" ->
                    "Pro ativo"
                else ->
                    "Free"
            }

        val column =
            plansColumn ?: return

        column.removeAllViews()

        plans.forEach { plan ->
            column.addView(
                planCard(
                    plan,
                    entitlement.plan
                )
            )

            column.addView(
                ViewKit.spacer(
                    this,
                    10
                )
            )
        }

        loading?.visibility =
            View.GONE

        column.visibility =
            View.VISIBLE
    }

    private fun planCard(
        plan: PlanInfo,
        currentPlan: String
    ): View {
        val accent =
            when (plan.id) {
                "PREMIUM" ->
                    R.color.audio_purple
                "PRO" ->
                    R.color.audio_blue
                else ->
                    R.color.audio_green
            }

        val card =
            ViewKit.card(
                this,
                accentColorRes = accent
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    ViewKit.dp(
                        this@UpgradeActivity,
                        16
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        16
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        16
                    ),
                    ViewKit.dp(
                        this@UpgradeActivity,
                        16
                    )
                )
            }

        val top =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val titleColumn =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams
                            .WRAP_CONTENT,
                        1f
                    )
            }

        titleColumn.addView(
            TextView(this).apply {
                text =
                    plan.name
                textSize = 21f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        R.color.audio_text
                    )
                )
            }
        )

        titleColumn.addView(
            ViewKit.spacer(
                this,
                3
            )
        )

        titleColumn.addView(
            TextView(this).apply {
                text =
                    priceText(plan)
                textSize = 16f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        accent
                    )
                )
            }
        )

        top.addView(
            titleColumn
        )

        if (
            plan.id ==
            currentPlan
        ) {
            top.addView(
                ViewKit.pill(
                    this,
                    "ATIVO",
                    colorRes = accent
                )
            )
        }

        content.addView(top)

        content.addView(
            ViewKit.spacer(
                this,
                9
            )
        )

        content.addView(
            TextView(this).apply {
                text =
                    plan.description
                textSize = 12.5f
                setLineSpacing(
                    1.08f,
                    1f
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@UpgradeActivity,
                        R.color.audio_muted
                    )
                )
            }
        )

        content.addView(
            ViewKit.spacer(
                this,
                10
            )
        )

        content.addView(
            ViewKit.eyebrow(
                this,
                "INCLUI"
            )
        )

        plan.includes.forEach { item ->
            content.addView(
                TextView(this).apply {
                    text =
                        "✓  " + item
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            this@UpgradeActivity,
                            R.color.audio_text
                        )
                    )
                    setPadding(
                        0,
                        ViewKit.dp(
                            this@UpgradeActivity,
                            4
                        ),
                        0,
                        0
                    )
                }
            )
        }

        content.addView(
            ViewKit.spacer(
                this,
                12
            )
        )

        val button =
            ViewKit.button(
                this,
                buttonText(
                    plan,
                    currentPlan
                ),
                false,
                accent
            )

        button.isEnabled =
            plan.id != "FREE" &&
            plan.id != currentPlan

        button.setOnClickListener {
            startSubscription(
                plan,
                button
            )
        }

        content.addView(
            button
        )

        card.addView(
            content
        )

        return card
    }

    private fun priceText(
        plan: PlanInfo
    ): String =
        if (
            plan.id == "FREE"
        ) {
            "$0  •  sem expiração"
        } else {
            "$" +
            String.format(
                Locale.US,
                "%.2f",
                plan.priceUsd
            ) +
            " / mês"
        }

    private fun buttonText(
        plan: PlanInfo,
        currentPlan: String
    ): String =
        when {
            plan.id ==
                currentPlan ->
                "Plano atual"
            plan.id == "FREE" ->
                "Free"
            else ->
                "Assinar " +
                plan.name
        }

    private fun startSubscription(
        plan: PlanInfo,
        button: MaterialButton
    ) {
        button.isEnabled =
            false
        button.text =
            "A preparar PayPal…"

        executor.execute {
            try {
                val subscription =
                    PaymentClient
                        .createSubscription(
                            plan.id
                        )

                mainHandler.post {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    subscription
                                        .approvalUrl
                                )
                            )
                        )

                        button.isEnabled =
                            true
                        button.text =
                            "Voltar ao PayPal"
                    } catch (_: Exception) {
                        button.isEnabled =
                            true
                        button.text =
                            "Tentar novamente"

                        Toast.makeText(
                            this,
                            "Não foi possível abrir o PayPal.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    button.isEnabled =
                        true
                    button.text =
                        "Tentar novamente"

                    Toast.makeText(
                        this,
                        error.message.orEmpty(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handlePaymentReturn(
        incoming: Intent
    ) {
        val data =
            incoming.data ?: return

        if (
            data.scheme != "audiotools" ||
            data.host != "paypal" ||
            data.path != "/complete"
        ) {
            return
        }

        val subscriptionId =
            data.getQueryParameter(
                "subscription_id"
            ).orEmpty()

        if (
            subscriptionId.isBlank()
        ) {
            return
        }

        activate(
            subscriptionId
        )
    }

    private fun activate(
        subscriptionId: String
    ) {
        loading?.visibility =
            View.VISIBLE

        executor.execute {
            try {
                val entitlement =
                    PaymentClient
                        .activateSubscription(
                            subscriptionId
                        )

                mainHandler.post {
                    loading?.visibility =
                        View.GONE

                    currentPlanView?.text =
                        when (
                            entitlement.plan
                        ) {
                            "PREMIUM" ->
                                "Premium ativo"
                            "PRO" ->
                                "Pro ativo"
                            else ->
                                "Free"
                        }

                    Toast.makeText(
                        this,
                        "Plano ativado com sucesso.",
                        Toast.LENGTH_LONG
                    ).show()

                    loadData()
                }
            } catch (error: Exception) {
                mainHandler.post {
                    loading?.visibility =
                        View.GONE

                    Toast.makeText(
                        this,
                        error.message.orEmpty(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
