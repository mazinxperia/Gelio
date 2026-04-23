package io.gelio.app.features.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.util.Base64
import android.util.Xml
import androidx.core.graphics.createBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import io.gelio.app.data.model.WorldMapPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val POSTER_MAP_MASTER_LONG_EDGE_PX = 2560

data class WorldMapCountryShape(
    val code: String,
    val name: String,
    val path: Path,
    val bounds: Rect,
    val hitRegion: Region,
)

sealed interface MapAssetData {
    val definition: MapAssetDefinition
    val viewBox: Rect
}

data class VectorWorldMapAssetData(
    override val definition: MapAssetDefinition,
    override val viewBox: Rect,
    val countries: List<WorldMapCountryShape>,
) : MapAssetData {
    private val codeIndex = countries.associateBy { it.code.uppercase() }
    private val normalizedNameIndex = countries.associateBy { normalizeCountryQuery(it.name) }

    fun findCountry(query: String): WorldMapCountryShape? {
        val normalized = normalizeCountryQuery(query)
        if (normalized.isBlank()) return null
        return codeIndex[query.trim().uppercase()]
            ?: normalizedNameIndex[normalized]
            ?: countries.firstOrNull { normalizeCountryQuery(it.name).contains(normalized) }
    }

    fun focusFor(country: WorldMapCountryShape): WorldMapViewportState {
        val widthFraction = max(country.bounds.width / viewBox.width, 0.04f)
        val heightFraction = max(country.bounds.height / viewBox.height, 0.04f)
        val zoom = (0.58f / max(widthFraction, heightFraction)).coerceIn(1f, 8f)
        return WorldMapViewportState(
            centerX = ((country.bounds.left + country.bounds.right) / 2f / viewBox.width).coerceIn(0f, 1f),
            centerY = ((country.bounds.top + country.bounds.bottom) / 2f / viewBox.height).coerceIn(0f, 1f),
            zoomScale = zoom,
        )
    }
}

data class PosterSvgMapAssetData(
    override val definition: MapAssetDefinition,
    override val viewBox: Rect,
    val posterBitmap: Bitmap,
) : MapAssetData

data class WorldMapViewportState(
    val centerX: Float,
    val centerY: Float,
    val zoomScale: Float,
)

object WorldMapAssetLoader {
    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, MapAssetData>()

    fun peek(assetName: String): MapAssetData? = cache[assetName]

    suspend fun load(context: Context, assetName: String): MapAssetData =
        mutex.withLock { cache[assetName] } ?: withContext(Dispatchers.IO) {
            val definition = MapAssetRegistry.resolve(assetName)
                ?: error("Unknown map asset: $assetName")
            val parsed = when (definition.rendererMode) {
                MapRendererMode.VECTOR_WORLD -> parseVectorWorldAsset(context, definition)
                MapRendererMode.SVG_POSTER -> parsePosterSvgAsset(context, definition)
            }
            mutex.withLock {
                cache[assetName] = parsed
            }
            parsed
        }

    private fun parseVectorWorldAsset(
        context: Context,
        definition: MapAssetDefinition,
    ): VectorWorldMapAssetData {
        val parser = Xml.newPullParser()
        val metadataNames =
            if (definition.assetName == MapAssetPaths.WORLD) {
                loadVectorCountryNames(context, MapAssetPaths.WORLD_METADATA)
            } else {
                emptyMap()
            }
        context.assets.open(definition.assetName).bufferedReader().use { reader ->
            parser.setInput(reader)
            var eventType = parser.eventType
            var viewBox = Rect(0f, 0f, 2000f, 857f)
            val groupedCountries = linkedMapOf<String, MutableCountryShapeBuilder>()
            var defsDepth = 0
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                        "svg" -> {
                            viewBox = parseViewBox(
                                parser.getAttributeValue(null, "viewBox")
                                    ?: parser.getAttributeValue(null, "viewbox"),
                                parser.getAttributeValue(null, "width"),
                                parser.getAttributeValue(null, "height"),
                            )
                        }

                        "defs" -> defsDepth += 1

                        "path" -> {
                            if (defsDepth > 0) {
                                eventType = parser.next()
                                continue
                            }
                            val code = parser.getAttributeValue(null, "id")?.trim().orEmpty()
                            val pathData = parser.getAttributeValue(null, "d")?.trim().orEmpty()
                            val countryName = parser.getAttributeValue(null, "name")?.trim().orEmpty()
                            if (code.isNotBlank() && pathData.isNotBlank()) {
                                val normalizedCode = extractCountryCode(code)
                                val displayName =
                                    countryName
                                        .ifBlank { metadataNames[normalizedCode].orEmpty() }
                                        .ifBlank { prettifyCountryId(code) }
                                val path = PathParser().parsePathString(pathData).toPath()
                                val bounds = path.getBounds()
                                val builder =
                                    groupedCountries.getOrPut(normalizedCode) {
                                        MutableCountryShapeBuilder(
                                            code = normalizedCode,
                                            name = displayName,
                                        )
                                    }
                                if (builder.name == builder.code && displayName.isNotBlank()) {
                                    builder.name = displayName
                                }
                                builder.path.addPath(path)
                                builder.bounds = unionBounds(builder.bounds, bounds)
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("defs", ignoreCase = true) && defsDepth > 0) {
                            defsDepth -= 1
                        }
                    }
                }
                eventType = parser.next()
            }
            val countries =
                groupedCountries.values.map { builder ->
                    val bounds = builder.bounds ?: Rect.Zero
                    WorldMapCountryShape(
                        code = builder.code,
                        name = builder.name.ifBlank { builder.code },
                        path = builder.path,
                        bounds = bounds,
                        hitRegion = buildHitRegion(builder.path, bounds),
                    )
                }
            return VectorWorldMapAssetData(
                definition = definition,
                viewBox = viewBox,
                countries = countries,
            )
        }
    }

    private fun parsePosterSvgAsset(
        context: Context,
        definition: MapAssetDefinition,
    ): PosterSvgMapAssetData {
        val viewBox = readPosterSvgViewBox(context, definition.assetName)
        val posterBitmap = renderPosterSvgBitmap(
            context = context,
            assetName = definition.assetName,
            viewBox = viewBox,
            requestedLongEdgePx = POSTER_MAP_MASTER_LONG_EDGE_PX,
        )
        return PosterSvgMapAssetData(
            definition = definition,
            viewBox = viewBox,
            posterBitmap = posterBitmap,
        )
    }
}

private data class MutableCountryShapeBuilder(
    val code: String,
    var name: String,
    val path: Path = Path(),
    var bounds: Rect? = null,
)

@Composable
fun rememberMapAsset(assetName: String): MapAssetData? {
    if (assetName.isBlank()) return null
    val context = androidx.compose.ui.platform.LocalContext.current
    val cachedAsset = WorldMapAssetLoader.peek(assetName)
    val asset by produceState<MapAssetData?>(initialValue = cachedAsset, assetName) {
        value = runCatching { WorldMapAssetLoader.load(context, assetName) }.getOrElse { value }
    }
    return asset
}

@Composable
fun rememberWorldMapAsset(assetName: String): MapAssetData? = rememberMapAsset(assetName)

fun clampViewportState(
    asset: MapAssetData,
    canvasWidth: Float,
    canvasHeight: Float,
    centerX: Float,
    centerY: Float,
    zoomScale: Float,
): WorldMapViewportState {
    val safeZoom = zoomScale.coerceIn(1f, 8f)
    if (canvasWidth <= 0f || canvasHeight <= 0f) {
        return WorldMapViewportState(centerX.coerceIn(0f, 1f), centerY.coerceIn(0f, 1f), safeZoom)
    }
    val baseScale = minOf(canvasWidth / asset.viewBox.width, canvasHeight / asset.viewBox.height)
    val scale = baseScale * safeZoom
    val visibleWorldWidth = canvasWidth / scale
    val visibleWorldHeight = canvasHeight / scale
    val halfVisibleXNorm = (visibleWorldWidth / asset.viewBox.width / 2f).coerceAtMost(0.5f)
    val halfVisibleYNorm = (visibleWorldHeight / asset.viewBox.height / 2f).coerceAtMost(0.5f)
    return WorldMapViewportState(
        centerX = centerX.coerceIn(halfVisibleXNorm, 1f - halfVisibleXNorm),
        centerY = centerY.coerceIn(halfVisibleYNorm, 1f - halfVisibleYNorm),
        zoomScale = safeZoom,
    )
}

private fun parseViewBox(rawViewBox: String?, rawWidth: String?, rawHeight: String?): Rect {
    val parts = rawViewBox
        ?.replace(',', ' ')
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.mapNotNull { it.toFloatOrNull() }
        .orEmpty()
    return if (parts.size == 4) {
        Rect(parts[0], parts[1], parts[0] + parts[2], parts[1] + parts[3])
    } else {
        Rect(
            0f,
            0f,
            rawWidth?.filter { it.isDigit() || it == '.' }?.toFloatOrNull() ?: 2000f,
            rawHeight?.filter { it.isDigit() || it == '.' }?.toFloatOrNull() ?: 857f,
        )
    }
}

private fun readPosterSvgViewBox(
    context: Context,
    assetName: String,
): Rect {
    val parser = Xml.newPullParser()
    context.assets.open(assetName).bufferedReader().use { reader ->
        parser.setInput(reader)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("svg", ignoreCase = true)) {
                return parseViewBox(
                    parser.getAttributeValue(null, "viewBox")
                        ?: parser.getAttributeValue(null, "viewbox"),
                    parser.getAttributeValue(null, "width"),
                    parser.getAttributeValue(null, "height"),
                )
            }
            eventType = parser.next()
        }
    }
    return Rect(0f, 0f, 3543f, 1994f)
}

private fun renderPosterSvgBitmap(
    context: Context,
    assetName: String,
    viewBox: Rect,
    requestedLongEdgePx: Int,
): Bitmap {
    val longEdge = requestedLongEdgePx.coerceAtLeast(720)
    val scale = if (viewBox.width >= viewBox.height) {
        longEdge / viewBox.width
    } else {
        longEdge / viewBox.height
    }
    val bitmapWidth = (viewBox.width * scale).toInt().coerceAtLeast(1)
    val bitmapHeight = (viewBox.height * scale).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    val parser = Xml.newPullParser()
    context.assets.open(assetName).bufferedReader().use { reader ->
        parser.setInput(reader)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("image", ignoreCase = true)) {
                val href = parser.getAttributeValue(null, "href")
                    ?: parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                    ?: ""
                if (href.startsWith("data:image", ignoreCase = true)) {
                    val commaIndex = href.indexOf(',')
                    if (commaIndex > 0) {
                        val encoded = href.substring(commaIndex + 1)
                        val bytes = Base64.decode(encoded, Base64.DEFAULT)
                        val layerBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (layerBitmap != null) {
                            val x = parseSvgFloat(parser.getAttributeValue(null, "x"))
                            val y = parseSvgFloat(parser.getAttributeValue(null, "y"))
                            val width = parseSvgFloat(parser.getAttributeValue(null, "width"))
                                .takeIf { it > 0f } ?: layerBitmap.width.toFloat()
                            val height = parseSvgFloat(parser.getAttributeValue(null, "height"))
                                .takeIf { it > 0f } ?: layerBitmap.height.toFloat()
                            canvas.drawBitmap(
                                layerBitmap,
                                null,
                                RectF(
                                    x * scale,
                                    y * scale,
                                    (x + width) * scale,
                                    (y + height) * scale,
                                ),
                                paint,
                            )
                            layerBitmap.recycle()
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }
    return bitmap
}

private fun parseSvgFloat(raw: String?): Float =
    raw
        ?.trim()
        ?.replace(Regex("[^0-9+\\-eE.]"), "")
        ?.toFloatOrNull()
        ?: 0f

private fun extractCountryCode(rawId: String): String {
    val suffix = rawId.substringAfterLast('_', rawId).trim()
    return if (suffix.length in 2..3 && suffix.all { it.isLetter() }) {
        suffix.uppercase()
    } else {
        rawId.trim().uppercase()
    }
}

private fun prettifyCountryId(rawId: String): String {
    val base = rawId.substringBeforeLast('_', rawId)
    return base.replace('_', ' ').trim().ifBlank { rawId.trim() }
}

private fun unionBounds(current: Rect?, next: Rect): Rect =
    if (current == null) {
        next
    } else {
        Rect(
            left = min(current.left, next.left),
            top = min(current.top, next.top),
            right = max(current.right, next.right),
            bottom = max(current.bottom, next.bottom),
        )
    }

private fun loadVectorCountryNames(
    context: Context,
    assetName: String,
): Map<String, String> {
    val parser = Xml.newPullParser()
    val namesByCode = linkedMapOf<String, String>()
    context.assets.open(assetName).bufferedReader().use { reader ->
        parser.setInput(reader)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("path", ignoreCase = true)) {
                val rawCode = parser.getAttributeValue(null, "id")?.trim().orEmpty()
                val name = parser.getAttributeValue(null, "name")?.trim().orEmpty()
                val normalizedCode = extractCountryCode(rawCode)
                if (normalizedCode.isNotBlank() && name.isNotBlank()) {
                    namesByCode.putIfAbsent(normalizedCode, name)
                }
            }
            eventType = parser.next()
        }
    }
    return namesByCode
}

private fun buildHitRegion(path: Path, bounds: Rect): Region {
    val clipBounds = android.graphics.Rect(
        floor(bounds.left).toInt(),
        floor(bounds.top).toInt(),
        ceil(bounds.right).toInt(),
        ceil(bounds.bottom).toInt(),
    )
    val safeClip = if (clipBounds.width() <= 0 || clipBounds.height() <= 0) {
        android.graphics.Rect(0, 0, 1, 1)
    } else {
        clipBounds
    }
    val androidPath = path.asAndroidPath()
    androidPath.computeBounds(RectF(), true)
    return Region().apply {
        setPath(androidPath, Region(safeClip))
    }
}

private fun normalizeCountryQuery(value: String): String =
    value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

fun WorldMapPin.toWorldOffset(asset: MapAssetData): Offset =
    Offset(asset.viewBox.width * xNorm, asset.viewBox.height * yNorm)
