package com.nexauren.audiotools.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexauren.audiotools.BuildConfig
import com.nexauren.audiotools.notifications.NotificationCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val prefs = applicationContext.getSharedPreferences("network_updates", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            checkManifest()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            UpdateScheduler.schedule(applicationContext, delaySeconds = 6 * 60 * 60L)
        }
    }

    private fun checkManifest() {
        val url = URL("https://raw.githubusercontent.com/nexauren1/Audio-Tools/main/updates.json")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AudioTools-Android")
        }
        val body = try {
            if (connection.responseCode !in 200..299) return
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val root = JSONObject(body)

        val remoteVersion = root.optString("version").removePrefix("v")
        if (remoteVersion.isNotBlank() &&
            UpdateManager.isNewer(remoteVersion, BuildConfig.VERSION_NAME) &&
            prefs.getString("version_notified", "") != remoteVersion
        ) {
            NotificationCenter.notifyUpdate(applicationContext, remoteVersion)
            prefs.edit().putString("version_notified", remoteVersion).apply()
        }

        val tools = root.optJSONArray("tools")
        if (tools != null) {
            for (i in 0 until tools.length()) {
                val item = tools.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank() || prefs.getBoolean("tool_$id", false)) continue
                NotificationCenter.notifyTool(
                    applicationContext,
                    id,
                    item.optString("title", "Nova ferramenta"),
                    item.optString("description", "Nova ferramenta disponível.")
                )
                prefs.edit().putBoolean("tool_$id", true).apply()
            }
        }

        val news = root.optJSONArray("news")
        if (news != null) {
            for (i in 0 until news.length()) {
                val item = news.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank() || prefs.getBoolean("news_$id", false)) continue
                NotificationCenter.notifyNews(
                    applicationContext,
                    id,
                    item.optString("title", "Novidade"),
                    item.optString("description", "Há uma novidade no Audio Tools.")
                )
                prefs.edit().putBoolean("news_$id", true).apply()
            }
        }
    }
}
