package io.gelio.app.features.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.caverock.androidsvg.SVG
import io.gelio.app.core.util.deleteMapPreview
import io.gelio.app.core.util.mapPreviewPath
import io.gelio.app.core.util.writeMapPreviewBitmap
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max

private const val MAP_PREVIEW_WIDTH_PX = 2400
private const val MAP_PREVIEW_HEIGHT_PX = 900
private const val PIN_SELECTION_RADIUS_PX = 36f
private const val MAP_EDITOR_PROXY_VECTOR_MIN_EDGE_PX = 480
private const val MAP_EDITOR_PROXY_VECTOR_MAX_EDGE_PX = 960
private const val MAP_EDITOR_PROXY_POSTER_MIN_EDGE_PX = 960
private const val MAP_EDITOR_PROXY_POSTER_MAX_EDGE_PX = 1600

data class AndroidPosterPalette(
    val background: Int,
    val worldPlanePaint: Paint,
    val countryFillPaint: Paint,
    val titleColor: Int,
    val subtitleColor: Int,
    val pinOuterPaint: Paint,
    val pinInnerPaint: Paint,
    val pinSelectionPaint: Paint,
    val highlightFillPaint: Paint,
    val countryStrokePaint: (Float) -> Paint,
    val highlightStrokePaint: (Float) -> Paint,
)

fun posterPaletteAndroid(): AndroidPosterPalette {
    val palette = WorldMapPosterPalette(
        background = androidx.compose.ui.graphics.Color(0xFF061020),
        worldPlane = androidx.compose.ui.graphics.Color(0xFF061020),
        countryFill = androidx.compose.ui.graphics.Color(0xFF0F2235),
        countryStroke = androidx.compose.ui.graphics.Color(0xFFBF9637),
        highlightedFill = androidx.compose.ui.graphics.Color(0x66D5A84B),
        highlightedStroke = androidx.compose.ui.graphics.Color(0xFFDDB45C),
        pinOuter = androidx.compose.ui.graphics.Color(0xFFDDB45C),
        pinInner = androidx.compose.ui.graphics.Color(0xFF061020),
        pinSelectedRing = androidx.compose.ui.graphics.Color(0x55F7D98F),
        title = androidx.compose.ui.graphics.Color(0xFFE0B457),
        subtitle = androidx.compose.ui.graphics.Color(0xFFF0CF7B),
        placeholder = androidx.compose.ui.graphics.Color(0xFF9EB0C9),
    )
    return AndroidPosterPalette(
        background = palette.background.toArgb(),
        worldPlanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.worldPlane.toArgb()
            style = Paint.Style.FILL
        },
        countryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.countryFill.toArgb()
            style = Paint.Style.FILL
        },
        titleColor = palette.title.toArgb(),
        subtitleColor = palette.subtitle.toArgb(),
        pinOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.pinOuter.toArgb()
            style = Paint.Style.FILL
        },
        pinInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.pinInner.toArgb()
            style = Paint.Style.FILL
        },
        pinSelectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.pinSelectedRing.toArgb()
            style = Paint.Style.FILL
        },
        highlightFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.highlightedFill.toArgb()
            style = Paint.Style.FILL
        },
        countryStrokePaint = { renderScale ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.countryStroke.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = max(0.7f, 1.05f / renderScale)
                strokeCap = Paint.Cap.ROUND
            }
        },
        highlightStrokePaint = { renderScale ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.highlightedStroke.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 1.4f / renderScale)
                strokeCap = Paint.Cap.ROUND
            }
        },
    )
}

suspend fun saveMapPreviewToStorage(
    context: Context,
    section: WorldMapSection,
    pins: List<WorldMapPin>,
): String? = withContext(Dispatchers.IO) {
    if (section.assetName.isBlank()) {
        deleteMapPreview(context, section.sectionId)
        return@withContext null
    }
    val asset = try {
        WorldMapAssetLoader.load(context, section.assetName)
    } catch (error: Throwable) {
        Log.e("MapPreviewRender", "Failed to load map asset ${section.assetName} for ${section.sectionId}", error)
        return@withContext null
    }
    val bitmap = createBitmap(MAP_PREVIEW_WIDTH_PX, MAP_PREVIEW_HEIGHT_PX, Bitmap.Config.ARGB_8888)
    return@withContext try {
        renderMapPosterScene(
            canvas = AndroidCanvas(bitmap),
            asset = asset,
            viewport = WorldMapViewportState(
                centerX = section.viewportCenterX,
                centerY = section.viewportCenterY,
                zoomScale = section.zoomScale,
            ),
            highlightedCountryCodes = section.highlightedCountryCodes.toSet(),
            pins = pins,
            selectedPinId = null,
            showSelectedPin = false,
            countryLabel = section.countryLabel,
            cityLabel = section.cityLabel,
            widthPx = MAP_PREVIEW_WIDTH_PX.toFloat(),
            heightPx = MAP_PREVIEW_HEIGHT_PX.toFloat(),
        )
        writeMapPreviewBitmap(context, section.sectionId, bitmap).also { writtenPath ->
            if (writtenPath == null) {
                Log.e("MapPreviewRender", "Preview write returned null for ${section.sectionId}")
            }
        }
    } catch (error: Throwable) {
        Log.e("MapPreviewRender", "Failed to render preview for ${section.sectionId}", error)
        mapPreviewPath(context, section.sectionId)
    } finally {
        bitmap.recycle()
    }
}

fun renderMapPosterScene(
    canvas: AndroidCanvas,
    asset: MapAssetData,
    viewport: WorldMapViewportState,
    highlightedCountryCodes: Set<String>,
    pins: List<WorldMapPin>,
    selectedPinId: String?,
    showSelectedPin: Boolean,
    countryLabel: String,
    cityLabel: String,
    widthPx: Float,
    heightPx: Float,
    pinSvg: SVG? = null,
) {
    val palette = posterPaletteAndroid()
    drawMapAssetLayer(
        canvas = canvas,
        asset = asset,
        highlightedCountryCodes = highlightedCountryCodes,
        viewport = viewport,
        widthPx = widthPx,
        heightPx = heightPx,
        palette = palette,
    )
    renderMapPinsOverlay(
        canvas = canvas,
        asset = asset,
        viewport = viewport,
        pins = pins,
        selectedPinId = selectedPinId,
        showSelectedPin = showSelectedPin,
        widthPx = widthPx,
        heightPx = heightPx,
        pinSvg = pinSvg,
    )
    renderMapPosterChrome(
        canvas = canvas,
        widthPx = widthPx,
        heightPx = heightPx,
        countryLabel = countryLabel,
        cityLabel = cityLabel,
    )
}

suspend fun renderMapEditorProxyBitmap(
    asset: MapAssetData,
    highlightedCountryCodes: Set<String>,
    requestedLongEdgePx: Int,
): Bitmap? = withContext(Dispatchers.Default) {
    val viewWidth = asset.viewBox.width
    val viewHeight = asset.viewBox.height
    if (viewWidth <= 0f || viewHeight <= 0f) return@withContext null
    val longEdgePx = when (asset) {
        is PosterSvgMapAssetData -> (requestedLongEdgePx * 2)
            .coerceIn(MAP_EDITOR_PROXY_POSTER_MIN_EDGE_PX, MAP_EDITOR_PROXY_POSTER_MAX_EDGE_PX)
        is VectorWorldMapAssetData -> requestedLongEdgePx
            .coerceIn(MAP_EDITOR_PROXY_VECTOR_MIN_EDGE_PX, MAP_EDITOR_PROXY_VECTOR_MAX_EDGE_PX)
    }
    val scale = if (viewWidth >= viewHeight) {
        longEdgePx / viewWidth
    } else {
        longEdgePx / viewHeight
    }
    val bitmapWidth = (viewWidth * scale).toInt().coerceAtLeast(1)
    val bitmapHeight = (viewHeight * scale).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    try {
        drawMapAssetLayer(
            canvas = AndroidCanvas(bitmap),
            asset = asset,
            highlightedCountryCodes = highlightedCountryCodes,
            viewport = WorldMapViewportState(0.5f, 0.5f, 1f),
            widthPx = bitmapWidth.toFloat(),
            heightPx = bitmapHeight.toFloat(),
            palette = posterPaletteAndroid(),
        )
        bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        null
    }
}

private fun drawMapAssetLayer(
    canvas: AndroidCanvas,
    asset: MapAssetData,
    highlightedCountryCodes: Set<String>,
    viewport: WorldMapViewportState,
    widthPx: Float,
    heightPx: Float,
    palette: AndroidPosterPalette,
) {
    canvas.drawColor(palette.background)
    val renderScale = renderScale(asset, widthPx, heightPx, viewport.zoomScale)
    val centerWorldX = asset.viewBox.width * viewport.centerX
    val centerWorldY = asset.viewBox.height * viewport.centerY
    val translationX = widthPx / 2f - centerWorldX * renderScale
    val translationY = heightPx / 2f - centerWorldY * renderScale

    canvas.save()
    canvas.clipRect(0f, 0f, widthPx, heightPx)
    when (asset) {
        is VectorWorldMapAssetData -> {
            canvas.drawRect(0f, 0f, widthPx, heightPx, palette.worldPlanePaint)
            canvas.save()
            canvas.translate(translationX, translationY)
            canvas.scale(renderScale, renderScale)
            asset.countries.forEach { country ->
                val isHighlighted = highlightedCountryCodes.contains(country.code)
                val androidPath = country.path.asAndroidPath()
                canvas.drawPath(
                    androidPath,
                    if (isHighlighted) palette.highlightFillPaint else palette.countryFillPaint,
                )
                canvas.drawPath(
                    androidPath,
                    if (isHighlighted) palette.highlightStrokePaint(renderScale) else palette.countryStrokePaint(renderScale),
                )
            }
            canvas.restore()
        }

        is PosterSvgMapAssetData -> {
            val targetRect = RectF(
                translationX,
                translationY,
                translationX + asset.viewBox.width * renderScale,
                translationY + asset.viewBox.height * renderScale,
            )
            canvas.drawBitmap(
                asset.posterBitmap,
                null,
                targetRect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true
                    isDither = true
                },
            )
        }
    }
    canvas.restore()
}

fun renderMapPinsOverlay(
    canvas: AndroidCanvas,
    asset: MapAssetData,
    viewport: WorldMapViewportState,
    pins: List<WorldMapPin>,
    selectedPinId: String?,
    showSelectedPin: Boolean,
    widthPx: Float,
    heightPx: Float,
    pinBitmap: Bitmap? = null,
    pinSvg: SVG? = null,
) {
    val palette = posterPaletteAndroid()
    pins.forEach { pin ->
        val screenOffset = worldToScreen(
            asset = asset,
            viewport = viewport,
            canvasSize = Size(widthPx, heightPx),
            worldOffset = pin.toWorldOffset(asset),
        )
        drawPin(
            canvas = canvas,
            center = screenOffset,
            selected = showSelectedPin && pin.id == selectedPinId,
            palette = palette,
            pinBitmap = pinBitmap,
            pinSvg = pinSvg,
        )
        if (pin.label.isNotBlank()) {
            drawPinLabel(
                canvas = canvas,
                anchor = screenOffset,
                label = pin.label,
                selected = showSelectedPin && pin.id == selectedPinId,
                titleColor = palette.titleColor,
                subtitleColor = palette.subtitleColor,
            )
        }
    }
}

fun renderMapPosterChrome(
    canvas: AndroidCanvas,
    widthPx: Float,
    heightPx: Float,
    countryLabel: String,
    cityLabel: String,
    showViewfinderMask: Boolean = true,
) {
    val palette = posterPaletteAndroid()
    if (showViewfinderMask) {
        drawViewfinderMask(canvas, widthPx, heightPx, palette.background)
    }
    drawPosterTitle(
        canvas = canvas,
        widthPx = widthPx,
        heightPx = heightPx,
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        titleColor = palette.titleColor,
        subtitleColor = palette.subtitleColor,
    )
}

fun findNearestPin(
    asset: MapAssetData,
    pins: List<WorldMapPin>,
    viewport: WorldMapViewportState,
    canvasSize: Size,
    tapOffset: Offset,
): WorldMapPin? =
    pins
        .map { pin ->
            pin to worldToScreen(
                asset = asset,
                viewport = viewport,
                canvasSize = canvasSize,
                worldOffset = pin.toWorldOffset(asset),
            )
        }
        .minByOrNull { (_, point) ->
            hypot((point.x - tapOffset.x).toDouble(), (point.y - tapOffset.y).toDouble())
        }
        ?.takeIf { (_, point) ->
            hypot((point.x - tapOffset.x).toDouble(), (point.y - tapOffset.y).toDouble()) <= PIN_SELECTION_RADIUS_PX.toDouble()
        }
        ?.first

fun renderScale(
    asset: MapAssetData,
    canvasWidth: Float,
    canvasHeight: Float,
    zoomScale: Float,
): Float =
    minOf(canvasWidth / asset.viewBox.width, canvasHeight / asset.viewBox.height) * zoomScale

fun worldToScreen(
    asset: MapAssetData,
    viewport: WorldMapViewportState,
    canvasSize: Size,
    worldOffset: Offset,
): Offset {
    val renderScale = renderScale(asset, canvasSize.width, canvasSize.height, viewport.zoomScale)
    val centerWorld = Offset(asset.viewBox.width * viewport.centerX, asset.viewBox.height * viewport.centerY)
    val translationX = canvasSize.width / 2f - centerWorld.x * renderScale
    val translationY = canvasSize.height / 2f - centerWorld.y * renderScale
    return Offset(
        x = translationX + worldOffset.x * renderScale,
        y = translationY + worldOffset.y * renderScale,
    )
}

fun screenToWorld(
    asset: MapAssetData,
    canvasSize: Size,
    viewport: WorldMapViewportState,
    screenOffset: Offset,
): Offset {
    val renderScale = renderScale(asset, canvasSize.width, canvasSize.height, viewport.zoomScale)
    val centerWorld = Offset(asset.viewBox.width * viewport.centerX, asset.viewBox.height * viewport.centerY)
    val translationX = canvasSize.width / 2f - centerWorld.x * renderScale
    val translationY = canvasSize.height / 2f - centerWorld.y * renderScale
    return Offset(
        x = ((screenOffset.x - translationX) / renderScale).coerceIn(asset.viewBox.left, asset.viewBox.right),
        y = ((screenOffset.y - translationY) / renderScale).coerceIn(asset.viewBox.top, asset.viewBox.bottom),
    )
}

private fun drawPin(
    canvas: AndroidCanvas,
    center: Offset,
    selected: Boolean,
    palette: AndroidPosterPalette,
    pinBitmap: Bitmap? = null,
    pinSvg: SVG?,
) {
    if (selected) {
        canvas.drawCircle(center.x, center.y - 6f, 28f, palette.pinSelectionPaint)
    }
    val markerPath = AndroidPath().apply {
        val width = 28f
        val height = 38f
        moveTo(center.x, center.y + height * 0.48f)
        cubicTo(
            center.x - width * 0.72f,
            center.y + height * 0.04f,
            center.x - width * 0.76f,
            center.y - height * 0.5f,
            center.x,
            center.y - height * 0.5f,
        )
        cubicTo(
            center.x + width * 0.76f,
            center.y - height * 0.5f,
            center.x + width * 0.72f,
            center.y + height * 0.04f,
            center.x,
            center.y + height * 0.48f,
        )
        close()
    }
    canvas.drawPath(markerPath, palette.pinOuterPaint)
    canvas.drawCircle(center.x, center.y - 8f, 7.6f, palette.pinInnerPaint)
}

private fun drawPinLabel(
    canvas: AndroidCanvas,
    anchor: Offset,
    label: String,
    selected: Boolean,
    titleColor: Int,
    subtitleColor: Int,
) {
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subtitleColor
        textSize = 24f
        letterSpacing = 0.04f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.NORMAL)
    }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = adjustAlpha(0xFF081A31.toInt(), if (selected) 0.96f else 0.88f)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = adjustAlpha(titleColor, if (selected) 0.46f else 0.28f)
        style = Paint.Style.STROKE
        strokeWidth = if (selected) 2.2f else 1.5f
    }
    val safeLabel = label.trim()
    if (safeLabel.isBlank()) return
    val textWidth = textPaint.measureText(safeLabel)
    val paddingX = 16f
    val paddingY = 10f
    val bubbleHeight = 38f
    val bubbleLeft = anchor.x + 18f
    val bubbleTop = anchor.y - 42f
    val bubbleRect = RectF(
        bubbleLeft,
        bubbleTop,
        bubbleLeft + textWidth + paddingX * 2f,
        bubbleTop + bubbleHeight,
    )
    canvas.drawRoundRect(bubbleRect, 18f, 18f, backgroundPaint)
    canvas.drawRoundRect(bubbleRect, 18f, 18f, strokePaint)
    canvas.drawText(
        safeLabel,
        bubbleRect.left + paddingX,
        bubbleRect.centerY() + textPaint.textSize * 0.32f,
        textPaint,
    )
}

private fun drawPosterTitle(
    canvas: AndroidCanvas,
    widthPx: Float,
    heightPx: Float,
    countryLabel: String,
    cityLabel: String,
    titleColor: Int,
    subtitleColor: Int,
) {
    val resolvedCountry = countryLabel.trim().ifBlank { "MAP" }.uppercase()
    val resolvedCity = cityLabel.trim().uppercase()

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = titleColor
        textAlign = Paint.Align.CENTER
        textSize = heightPx * 0.066f
        letterSpacing = 0.22f
    }
    val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subtitleColor
        textAlign = Paint.Align.CENTER
        textSize = heightPx * 0.025f
        letterSpacing = 0.18f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.NORMAL)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            widthPx * 0.38f,
            0f,
            widthPx * 0.62f,
            0f,
            intArrayOf(android.graphics.Color.TRANSPARENT, titleColor, android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        strokeWidth = max(2f, heightPx * 0.0022f)
    }

    val baseY = heightPx * 0.82f
    canvas.drawText(resolvedCountry, widthPx / 2f, baseY, titlePaint)
    val dividerY = baseY + heightPx * 0.022f
    canvas.drawLine(widthPx * 0.39f, dividerY, widthPx * 0.61f, dividerY, linePaint)
    if (resolvedCity.isNotBlank()) {
        canvas.drawText(resolvedCity, widthPx / 2f, dividerY + heightPx * 0.04f, subtitlePaint)
    }
}

private fun drawViewfinderMask(
    canvas: AndroidCanvas,
    widthPx: Float,
    heightPx: Float,
    backgroundColor: Int,
) {
    val edgeWidth = widthPx * 0.18f
    val topFadeHeight = heightPx * 0.14f
    val bottomFadeHeight = heightPx * 0.34f
    val cornerRadius = minOf(widthPx, heightPx) * 0.34f
    val strong = adjustAlpha(backgroundColor, 0.96f)
    val medium = adjustAlpha(backgroundColor, 0.62f)
    val light = adjustAlpha(backgroundColor, 0.24f)

    canvas.drawRect(
        0f,
        0f,
        edgeWidth,
        heightPx,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                edgeWidth,
                0f,
                intArrayOf(strong, medium, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
        },
    )
    canvas.drawRect(
        widthPx - edgeWidth,
        0f,
        widthPx,
        heightPx,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                widthPx - edgeWidth,
                0f,
                widthPx,
                0f,
                intArrayOf(android.graphics.Color.TRANSPARENT, medium, strong),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        },
    )
    canvas.drawRect(
        0f,
        0f,
        widthPx,
        topFadeHeight,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                topFadeHeight,
                intArrayOf(strong, light, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        },
    )
    canvas.drawRect(
        0f,
        heightPx - bottomFadeHeight,
        widthPx,
        heightPx,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                heightPx - bottomFadeHeight,
                0f,
                heightPx,
                intArrayOf(android.graphics.Color.TRANSPARENT, light, medium, strong),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
        },
    )

    listOf(
        Offset(0f, 0f),
        Offset(widthPx, 0f),
        Offset(0f, heightPx),
        Offset(widthPx, heightPx),
    ).forEach { center ->
        canvas.drawCircle(
            center.x,
            center.y,
            cornerRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    center.x,
                    center.y,
                    cornerRadius,
                    intArrayOf(strong, medium, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
    }
}

private fun adjustAlpha(
    color: Int,
    alphaFraction: Float,
): Int {
    val safeAlpha = (alphaFraction.coerceIn(0f, 1f) * 255f).toInt()
    return (color and 0x00FFFFFF) or (safeAlpha shl 24)
}
