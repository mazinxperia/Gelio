package io.gelio.app.features.map

enum class MapRendererMode {
    VECTOR_WORLD,
    SVG_POSTER,
}

data class MapAssetDefinition(
    val assetName: String,
    val displayName: String,
    val rendererMode: MapRendererMode,
    val supportsCountrySelection: Boolean,
    val defaultViewport: WorldMapViewportState = WorldMapViewportState(0.5f, 0.5f, 1f),
)

object MapAssetRegistry {
    val currentWorld =
        MapAssetDefinition(
            assetName = MapAssetPaths.WORLD,
            displayName = "Current World Map",
            rendererMode = MapRendererMode.VECTOR_WORLD,
            supportsCountrySelection = true,
            defaultViewport = WorldMapViewportState(0.5f, 0.5f, 1f),
        )

    val india =
        MapAssetDefinition(
            assetName = MapAssetPaths.INDIA,
            displayName = "India",
            rendererMode = MapRendererMode.SVG_POSTER,
            supportsCountrySelection = false,
            defaultViewport = WorldMapViewportState(0.5f, 0.5f, 1f),
        )

    val dubai =
        MapAssetDefinition(
            assetName = MapAssetPaths.DUBAI,
            displayName = "Dubai",
            rendererMode = MapRendererMode.SVG_POSTER,
            supportsCountrySelection = false,
            defaultViewport = WorldMapViewportState(0.58f, 0.48f, 1f),
        )

    val sharjah =
        MapAssetDefinition(
            assetName = MapAssetPaths.SHARJAH,
            displayName = "Sharjah",
            rendererMode = MapRendererMode.SVG_POSTER,
            supportsCountrySelection = false,
            defaultViewport = WorldMapViewportState(0.51f, 0.58f, 1f),
        )

    val all = listOf(currentWorld, india, dubai, sharjah)

    fun resolve(assetName: String): MapAssetDefinition? =
        all.firstOrNull { it.assetName == assetName }
}
