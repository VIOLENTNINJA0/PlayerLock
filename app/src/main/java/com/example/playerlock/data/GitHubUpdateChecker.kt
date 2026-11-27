package com.example.playerlock.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val version: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val isNewerThanCurrent: Boolean
)

object GitHubUpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/VIOLENTNINJA0/PlayerLock/releases/latest"

    suspend fun checkLatestRelease(currentVersion: String): Result<GitHubReleaseInfo?> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "PlayerLock-Android-App")
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("GitHub API HTTP ${connection.responseCode}"))
                }

                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()

                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", tagName)
                val body = json.optString("body", "No release notes available.")
                val htmlUrl = json.optString("html_url", "https://github.com/VIOLENTNINJA0/PlayerLock/releases")

                val latestVersion = tagName.trimStart('v', 'V')
                val isNewer = isVersionNewer(latestVersion, currentVersion)

                val info = GitHubReleaseInfo(
                    tagName = tagName,
                    version = latestVersion,
                    name = name,
                    body = body,
                    htmlUrl = htmlUrl,
                    isNewerThanCurrent = isNewer
                )

                Result.success(if (isNewer) info else null)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun isVersionNewer(latest: String, current: String): Boolean {
        fun parseParts(v: String) = v.split('.').mapNotNull { it.toIntOrNull() }
        val latestParts = parseParts(latest)
        val currentParts = parseParts(current)

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
