package com.apexroute.audio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GitHubUpdateChecker {
    private const val REPO_RELEASES_API = "https://api.github.com/repos/mohsinapex/Apex-Route/releases/latest"
    const val REPO_WEB_URL = "https://github.com/mohsinapex/Apex-Route/releases"

    suspend fun checkForUpdates(context: Context, currentVersion: String, onResult: (Boolean, String?) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(REPO_RELEASES_API)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (connection.responseCode == 200) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseString)
                    val tagName = json.optString("tag_name", "v1.0").trimStart('v', 'V')
                    val releaseUrl = json.optString("html_url", REPO_WEB_URL)

                    withContext(Dispatchers.Main) {
                        if (isNewerVersion(tagName, currentVersion)) {
                            onResult(true, releaseUrl)
                        } else {
                            onResult(false, null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {
        }
        return false
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}
