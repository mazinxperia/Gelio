package io.gelio.app.features.map

object MapAssetPaths {
    const val ROOT_DIR = "maps"

    // Keep the current world asset path stable so existing saved data and backups continue to work.
    const val WORLD = "maps/world.svg"
    const val WORLD_METADATA = "maps/world/world.svg"

    // Canonical folders for future map assets.
    const val WORLD_DIR = "maps/world"
    const val INDIA_DIR = "maps/india"
    const val UAE_DIR = "maps/uae"

    // Reserved paths for upcoming region maps.
    const val INDIA = "maps/india/india.svg"
    const val DUBAI = "maps/uae/dubai.svg"
    const val SHARJAH = "maps/uae/sharjah.svg"
    const val PIN = "maps/pin.svg"
}
