package com.nexauren.audiotools.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.nexauren.audiotools.R
import com.nexauren.audiotools.ui.AboutActivity
import com.nexauren.audiotools.ui.MainActivity
import com.nexauren.audiotools.ui.SettingsActivity
import com.nexauren.audiotools.ui.ToolDetailActivity
import kotlin.math.absoluteValue

object NotificationCenter {
    const val CHANNEL_TOOLS = "tools"
    const val CHANNEL_NEWS = "news"
    const val CHANNEL_UPDATES = "updates"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_TOOLS, "Novas ferramentas", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Novas ferramentas disponíveis no Audio Tools." },
                NotificationChannel(CHANNEL_NEWS, "Novidades", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Novidades e avisos do Audio Tools." },
                NotificationChannel(CHANNEL_UPDATES, "Atualizações do app", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Avisos de novas versões do Audio Tools." }
            )
        )
    }

    fun notifyTool(context: Context, id: String, title: String, description: String) {
        post(
            context = context,
            channelId = CHANNEL_TOOLS,
            notificationId = 1000 + id.hashCode().absoluteValue % 8000,
            title = title,
            body = description,
            pendingIntent = PendingIntent.getActivity(
                context,
                id.hashCode(),
                ToolDetailActivity.intent(context, id),
                pendingFlags()
            )
        )
    }

    fun notifyNews(context: Context, id: String, title: String, description: String) {
        post(
            context = context,
            channelId = CHANNEL_NEWS,
            notificationId = 9000 + id.hashCode().absoluteValue % 800,
            title = title,
            body = description,
            pendingIntent = PendingIntent.getActivity(
                context,
                id.hashCode(),
                Intent(context, MainActivity::class.java),
                pendingFlags()
            )
        )
    }

    fun notifyUpdate(context: Context, version: String) {
        post(
            context = context,
            channelId = CHANNEL_UPDATES,
            notificationId = 9800,
            title = "Nova versão disponível",
            body = "Audio Tools $version está disponível. Abre as definições para atualizar.",
            pendingIntent = PendingIntent.getActivity(
                context,
                9800,
                Intent(context, SettingsActivity::class.java),
                pendingFlags()
            )
        )
    }

    private fun post(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
