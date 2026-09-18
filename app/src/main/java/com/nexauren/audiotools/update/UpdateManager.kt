package com.nexauren.audiotools.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class UpdateInfo(val versionName: String, val releaseNotesUrl: String, val apkUrl: String)

object UpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/nexauren1/Audio-Tools/releases/latest"

    fun check(currentVersion: String, callback: (Result<UpdateInfo?>) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AudioTools-Android")
                }
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    connection.disconnect()
                    callback(Result.failure(IllegalStateException("HTTP error")))
                    return@execute
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val root = JSONObject(body)
                val latest = root.optString("tag_name").removePrefix("v")
                if (latest.isBlank() || !isNewer(latest, currentVersion.removePrefix("v"))) {
                    callback(Result.success(null))
                    return@execute
                }

                val assets = root.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                callback(Result.success(UpdateInfo(latest, root.optString("html_url"), apkUrl)))
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }
    }

    fun isNewer(remote: String, local: String): Boolean {
        fun parts(value: String) = value.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
        val r = parts(remote)
        val l = parts(local)
        val size = maxOf(r.size, l.size)
        for (i in 0 until size) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    fun download(context: Context, info: UpdateInfo): Long {
        require(info.apkUrl.isNotBlank())
        val request = DownloadManager.Request(Uri.parse(info.apkUrl)).apply {
            setTitle("Audio Tools " + info.versionName)
            setDescription("A transferir a atualização")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "AudioTools-" + info.versionName + ".apk"
            )
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }

    fun openInstaller(context: Context, uri: Uri) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true

    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName)
                )
            )
        }
    }
}