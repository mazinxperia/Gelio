package io.gelio.app.data.model

import androidx.compose.runtime.Immutable

enum class ReviewSource(
    val storageKey: String,
    val label: String,
) {
    GOOGLE("google", "Google"),
    TRIPADVISOR("tripadvisor", "Tripadvisor"),
    FACEBOOK("facebook", "Facebook"),
    GENERIC("generic", "Generic Review");

    companion object {
        fun fromStorageKey(value: String): ReviewSource =
            entries.firstOrNull { it.storageKey == value } ?: GENERIC
    }
}

@Immutable
data class ReviewCard(
    val id: String,
    val sectionId: String,
    val reviewerName: String,
    val sourceType: ReviewSource,
    val subHeading: String,
    val comment: String,
    val rating: Int,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
