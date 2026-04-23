package io.gelio.app.data.repository

import android.content.Context
import io.gelio.app.core.util.importNormalizedImageFileToAppStorage
import io.gelio.app.core.util.localThumbnailForPath
import io.gelio.app.data.local.dao.ShowcaseDao
import io.gelio.app.data.local.entity.RemoteImageAssetEntity
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PexelsPhoto(
    val id: String,
    val title: String,
    val previewUrl: String,
    val downloadUrl: String,
    val sourcePageUrl: String,
    val photographerName: String,
    val photographerUrl: String,
)

data class PexelsSearchPage(
    val photos: List<PexelsPhoto>,
    val nextPageUrl: String?,
)

data class PexelsImportedImage(
    val localPath: String,
    val thumbnailPath: String?,
    val metadata: RemoteImageAssetEntity,
)

class PexelsRepository(
    private val context: Context,
    private val dao: ShowcaseDao,
) {
    suspend fun validateApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitized = apiKey.trim()
            require(sanitized.isNotBlank()) { "Enter a Pexels API key first." }
            val endpoint = "$baseApiUrl/search?query=architecture&per_page=1&page=1"
            requestJson(endpoint, sanitized)
            Unit
        }
    }

    suspend fun searchPhotos(
        apiKey: String,
        query: String,
        page: Int = 1,
        perPage: Int = 24,
    ): Result<PexelsSearchPage> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedKey = apiKey.trim()
            require(sanitizedKey.isNotBlank()) { "Add a Pexels API key in App Settings first." }
            val sanitizedQuery = query.trim()
            require(sanitizedQuery.isNotBlank()) { "Enter a search term first." }
            val endpoint = buildString {
                append(baseApiUrl)
                append("/search?query=")
                append(URLEncoder.encode(sanitizedQuery, Charsets.UTF_8.name()))
                append("&per_page=")
                append(perPage.coerceIn(1, 40))
                append("&page=")
                append(page.coerceAtLeast(1))
            }
            parseSearchPage(requestJson(endpoint, sanitizedKey))
        }
    }

    suspend fun loadNextPage(
        apiKey: String,
        nextPageUrl: String,
    ): Result<PexelsSearchPage> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedKey = apiKey.trim()
            require(sanitizedKey.isNotBlank()) { "Add a Pexels API key in App Settings first." }
            require(nextPageUrl.isNotBlank()) { "No more images to load." }
            parseSearchPage(requestJson(nextPageUrl, sanitizedKey))
        }
    }

    suspend fun importPhotoToAppStorage(
        photo: PexelsPhoto,
        folderName: String,
        maxLongSide: Int = 1440,
        quality: Int = 82,
    ): Result<PexelsImportedImage> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = downloadPhotoToTempFile(photo.downloadUrl)
            try {
                val localPath = importNormalizedImageFileToAppStorage(
                    context = context,
                    source = tempFile,
                    folderName = folderName,
                    maxLongSide = maxLongSide,
                    quality = quality,
                )
                val metadata = RemoteImageAssetEntity(
                    localPath = localPath,
                    provider = providerName,
                    providerAssetId = photo.id,
                    sourcePageUrl = photo.sourcePageUrl,
                    downloadUrl = photo.downloadUrl,
                    photographerName = photo.photographerName,
                    photographerUrl = photo.photographerUrl,
                    importedAt = System.currentTimeMillis(),
                )
                dao.upsertRemoteImageAsset(metadata)
                PexelsImportedImage(
                    localPath = localPath,
                    thumbnailPath = localThumbnailForPath(context, localPath),
                    metadata = metadata,
                )
            } finally {
                runCatching { tempFile.delete() }
            }
        }
    }

    private fun parseSearchPage(json: JSONObject): PexelsSearchPage {
        val photos = json.optJSONArray("photos")
            ?.let { array ->
                List(array.length()) { index -> array.getJSONObject(index) }.map { item ->
                    val src = item.getJSONObject("src")
                    PexelsPhoto(
                        id = item.optLong("id").takeIf { it > 0L }?.toString() ?: item.optString("id"),
                        title = item.optString("alt", "").trim(),
                        previewUrl = src.optString("medium").ifBlank { src.optString("small") }.ifBlank { src.optString("tiny") },
                        downloadUrl = src.optString("large").ifBlank { src.optString("large2x") }.ifBlank { src.optString("original") },
                        sourcePageUrl = item.optString("url"),
                        photographerName = item.optString("photographer"),
                        photographerUrl = item.optString("photographer_url"),
                    )
                }.filter { photo ->
                    photo.id.isNotBlank() &&
                        photo.previewUrl.isNotBlank() &&
                        photo.downloadUrl.isNotBlank()
                }
            }
            .orEmpty()
        return PexelsSearchPage(
            photos = photos,
            nextPageUrl = json.optString("next_page").takeIf { it.isNotBlank() },
        )
    }

    private fun requestJson(
        endpoint: String,
        apiKey: String,
    ): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Authorization", apiKey)
            setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Gelio/1.0")
        }
        return try {
            val responseCode = connection.responseCode
            val body = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (responseCode !in 200..299) {
                val jsonMessage = runCatching {
                    JSONObject(body).optString("error")
                }.getOrNull().orEmpty()
                val resolvedMessage = when {
                    responseCode == 401 -> "Pexels rejected the API key."
                    responseCode == 429 -> "Pexels rate limit reached. Wait and try again."
                    jsonMessage.isNotBlank() -> jsonMessage
                    body.isNotBlank() -> body
                    else -> "Pexels request failed with HTTP $responseCode."
                }
                error(resolvedMessage)
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadPhotoToTempFile(
        url: String,
    ): File {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Gelio/1.0")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error(
                    if (errorBody.isNotBlank()) errorBody else "Unable to download selected Pexels image. HTTP $responseCode.",
                )
            }
            val extension = connection.contentType
                ?.substringAfterLast('/')
                ?.substringBefore(';')
                ?.lowercase()
                ?.let { raw ->
                    when (raw) {
                        "jpeg" -> ".jpg"
                        "png" -> ".png"
                        "webp" -> ".webp"
                        else -> ".jpg"
                    }
                } ?: ".jpg"
            val tempFile = File(context.cacheDir, "pexels_${UUID.randomUUID()}$extension")
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val providerName = "pexels"
        private const val baseApiUrl = "https://api.pexels.com/v1"
    }
}
