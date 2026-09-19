package com.nexauren.audiotools.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class UpdateInfo(val versionName: String, val releaseNotesUrl: String, val apkUrl: String)

object UpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/nexauren1/Audio-Tools/releases/latest"
    private val executor = Executors.newSingleThreadExecutor()

    fun check(currentVersion: String, callback: (Result<UpdateInfo?>) -> Unit) {
        executor.execute {
            try {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AudioTools-Android")
                }
                try {
                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                        callback(Result.success(null))
                        return@execute
                    }
                    if (code !in 200..299) error("HTTP $code")
                    val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
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
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }
    }

    fun download(
        context: Context,
        info: UpdateInfo,
        callback: (Result<File>) -> Unit
    ) {
        require(info.apkUrl.isNotBlank())
        executor.execute {
            val dir = File(context.filesDir, "updates").apply { mkdirs() }
            val finalFile = File(dir, "AudioTools-${info.versionName}.apk")
            val tempFile = File(dir, "AudioTools-${info.versionName}.apk.part")
            try {
                val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "AudioTools-Android")
                    setRequestProperty("Accept", "application/vnd.android.package-archive")
                }
                try {
                    if (connection.responseCode !in 200..299) error("Download HTTP ${connection.responseCode}")
                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                            }
                            output.fd.sync()
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                require(tempFile.length() > 0L)
                if (finalFile.exists()) finalFile.delete()
                check(tempFile.renameTo(finalFile)) { "Could not finalize APK" }
                callback(Result.success(finalFile))
            } catch (error: Exception) {
                tempFile.delete()
                callback(Result.failure(error))
            }
        }
    }

    fun openInstaller(context: Context, file: File) {
        require(file.exists())
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls() else true

    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ))
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
}
