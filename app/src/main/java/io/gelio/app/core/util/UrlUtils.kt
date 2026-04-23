package io.gelio.app.core.util

import android.net.Uri
import android.text.Html
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun extractYouTubeId(url: String): String? {
    val normalized = url.trim()
    val shortId = Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(normalized)?.groupValues?.getOrNull(1)
    val watchId = Regex("[?&]v=([A-Za-z0-9_-]{6,})").find(normalized)?.groupValues?.getOrNull(1)
    val embedId = Regex("embed/([A-Za-z0-9_-]{6,})").find(normalized)?.groupValues?.getOrNull(1)
    return shortId ?: watchId ?: embedId
}

fun youtubeThumbnail(url: String): String? =
    extractYouTubeId(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

fun youtubeEmbedUrl(url: String): String {
    val id = extractYouTubeId(url) ?: return url
    return "https://www.youtube.com/embed/$id?autoplay=0&rel=0&modestbranding=1&playsinline=1"
}

fun isRemoteUrl(value: String): Boolean =
    value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

fun encodeNavArg(value: String): String = Uri.encode(value)

fun decodeNavArg(value: String?): String = Uri.decode(value.orEmpty())

fun cleanVirtualTourThumbnailUrl(value: String): String {
    val normalized = value.trim()
    val kuulaShareImage = Regex("""https?://kuula\.co/shareimg/([^/]+)/[^/]+/([^/?#]+)""", RegexOption.IGNORE_CASE)
        .find(normalized)
    if (kuulaShareImage != null) {
        val panoramaId = kuulaShareImage.groupValues[1]
        val fileName = kuulaShareImage.groupValues[2]
        return "https://files.kuula.io/$panoramaId/$fileName"
    }

    return normalized
}

fun extractIframeSrc(value: String): String? {
    val normalized = value.trim()
    if (normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true)) {
        return normalized.htmlDecoded()
    }

    return Regex("""(?is)<iframe\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""")
        .find(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?.htmlDecoded()
}

suspend fun resolveVirtualTourThumbnailUrl(embedHtmlOrUrl: String): String? =
    withContext(Dispatchers.IO) {
        val pageUrl = extractIframeSrc(embedHtmlOrUrl)?.takeIf { isRemoteUrl(it) } ?: return@withContext null
        if (pageUrl in virtualTourThumbnailMisses) return@withContext null
        virtualTourThumbnailCache[pageUrl]?.let { return@withContext it }

        runCatching {
            val connection = URL(pageUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Gelio/1.0")
            connection.inputStream.bufferedReader().use { reader ->
                val html = reader.readText()
                val image = html.metaContent("og:image") ?: html.metaContent("twitter:image")
                image
                    ?.htmlDecoded()
                    ?.let { URI(pageUrl).resolve(it).toString() }
                    ?.let(::cleanVirtualTourThumbnailUrl)
            }
        }.getOrNull()?.also {
            virtualTourThumbnailCache[pageUrl] = it
        } ?: run {
            virtualTourThumbnailMisses += pageUrl
            null
        }
    }

private fun String.metaContent(name: String): String? =
    Regex("""(?is)<meta\b[^>]*>""")
        .findAll(this)
        .map { it.value }
        .firstOrNull { tag ->
            tag.contains("property=\"$name\"", ignoreCase = true) ||
                tag.contains("property='$name'", ignoreCase = true) ||
                tag.contains("name=\"$name\"", ignoreCase = true) ||
                tag.contains("name='$name'", ignoreCase = true)
        }
        ?.let { tag ->
            Regex("""(?is)\bcontent\s*=\s*["']([^"']+)["']""")
                .find(tag)
                ?.groupValues
                ?.getOrNull(1)
        }

private fun String.htmlDecoded(): String =
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

private val virtualTourThumbnailCache = ConcurrentHashMap<String, String>()
private val virtualTourThumbnailMisses = ConcurrentHashMap.newKeySet<String>()
