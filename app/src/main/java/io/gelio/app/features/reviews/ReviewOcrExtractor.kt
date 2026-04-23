package io.gelio.app.features.reviews

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.gelio.app.data.model.ReviewSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class ReviewOcrResult(
    val reviewerName: String = "",
    val subHeading: String = "",
    val comment: String = "",
    val rating: Int? = null,
    val sourceType: ReviewSource = ReviewSource.GENERIC,
    val rawText: String = "",
)

suspend fun extractReviewFromScreenshot(
    context: Context,
    uri: Uri,
): ReviewOcrResult {
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val result = recognizer.process(image).awaitResult()
    recognizer.close()

    val lines = result.textBlocks
        .flatMap { block -> block.lines }
        .map { it.text.trim() }
        .filter { it.isNotBlank() }

    val rawText = lines.joinToString("\n")
    val source = detectSource(lines)
    val rating = detectRating(lines)
    val reviewerName = detectReviewerName(lines, source)
    val subHeading = detectSubHeading(lines, reviewerName, source)
    val comment = detectComment(lines, reviewerName, subHeading, source)

    return ReviewOcrResult(
        reviewerName = reviewerName,
        subHeading = subHeading,
        comment = comment,
        rating = rating,
        sourceType = source,
        rawText = rawText,
    )
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

private fun detectSource(lines: List<String>): ReviewSource {
    val joined = lines.joinToString(" ").lowercase()
    return when {
        "tripadvisor" in joined || "trip advisor" in joined -> ReviewSource.TRIPADVISOR
        Regex("""\bfacebook\b""").containsMatchIn(joined) -> ReviewSource.FACEBOOK
        "google" in joined || "local guide" in joined || "google review" in joined -> ReviewSource.GOOGLE
        else -> ReviewSource.GENERIC
    }
}

private fun detectRating(lines: List<String>): Int? {
    val joined = lines.joinToString(" ")
    Regex("""([1-5])(?:\.0)?\s*(?:out of 5|/5|stars?)""", RegexOption.IGNORE_CASE)
        .find(joined)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it.coerceIn(1, 5) }

    Regex("""[★⭐]{1,5}""").find(joined)?.value?.length?.let { return it.coerceIn(1, 5) }

    return null
}

private fun detectReviewerName(
    lines: List<String>,
    source: ReviewSource,
): String {
    val bannedTerms = setOf(
        "google",
        "tripadvisor",
        "facebook",
        "reviews",
        "review",
        "local guide",
        "ago",
        "stars",
    )
    return lines.firstOrNull { line ->
        val lower = line.lowercase()
        line.length in 2..48 &&
            line.count { it.isLetter() } >= 3 &&
            bannedTerms.none { it in lower } &&
            !Regex("""^\d""").containsMatchIn(line) &&
            detectSource(listOf(line)) == ReviewSource.GENERIC
    }.orEmpty()
}

private fun detectSubHeading(
    lines: List<String>,
    reviewerName: String,
    source: ReviewSource,
): String {
    return lines.firstOrNull { line ->
        val lower = line.lowercase()
        line != reviewerName &&
            line.length <= 48 &&
            (
                "local guide" in lower ||
                    "reviews" in lower ||
                    "photos" in lower ||
                    "ago" in lower ||
                    source.label.lowercase() in lower
                )
    }.orEmpty()
}

private fun detectComment(
    lines: List<String>,
    reviewerName: String,
    subHeading: String,
    source: ReviewSource,
): String {
    val filtered = lines.filterNot { line ->
        val lower = line.lowercase()
        line == reviewerName ||
            line == subHeading ||
            line.length < 8 ||
            source.label.lowercase() == lower ||
            Regex("""^([1-5])(?:\.0)?\s*(?:out of 5|/5|stars?)$""", RegexOption.IGNORE_CASE).matches(line) ||
            Regex("""^[★⭐]{1,5}$""").matches(line)
    }

    val sentenceLike = filtered.filter { line ->
        line.split(Regex("""\s+""")).size >= 4 && line.count { it.isLetter() } >= 12
    }

    return when {
        sentenceLike.isNotEmpty() -> sentenceLike.joinToString(" ").trim()
        filtered.isNotEmpty() -> filtered.joinToString(" ").trim()
        else -> ""
    }
}
