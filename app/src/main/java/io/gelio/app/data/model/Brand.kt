package io.gelio.app.data.model

enum class Brand(
    val displayName: String,
    val shortLabel: String,
) {
    GENERIC(
        displayName = "Gelio",
        shortLabel = "Library",
    ),
    DESIGN(
        displayName = "Legacy Design",
        shortLabel = "Design",
    ),
    TOURISM(
        displayName = "Legacy Tourism",
        shortLabel = "Tourism",
    ),
}
