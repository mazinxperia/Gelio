package io.gelio.app.features.map

import android.os.SystemClock
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.gelio.app.data.model.WorldMapPin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

@Composable
fun MapEditorViewport(
    asset: MapAssetData,
    highlightedCountryCodes: Set<String>,
    pins: List<WorldMapPin>,
    centerX: Float,
    centerY: Float,
    zoomScale: Float,
    countryLabel: String,
    cityLabel: String,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    pinDropMode: Boolean = false,
    selectedPinId: String? = null,
    onViewportChange: ((WorldMapViewportState) -> Unit)? = null,
    onCountryTapped: ((WorldMapCountryShape) -> Unit)? = null,
    onPinDropped: ((Float, Float) -> Unit)? = null,
    onPinTapped: ((String?) -> Unit)? = null,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var proxyBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val canvasWidth = canvasSize.width.toFloat()
    val canvasHeight = canvasSize.height.toFloat()
    val proxyPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    }
    val externalViewportState = clampViewportState(
        asset = asset,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        centerX = centerX,
        centerY = centerY,
        zoomScale = zoomScale,
    )
    var liveViewportState by remember(asset, canvasSize) { mutableStateOf(externalViewportState) }
    var lastLocalViewportTouchMs by remember(asset) { mutableLongStateOf(0L) }
    val latestViewportCallback by rememberUpdatedState(onViewportChange)
    val latestViewportState by rememberUpdatedState(liveViewportState)
    val latestPins by rememberUpdatedState(pins)
    val latestSelectedPinId by rememberUpdatedState(selectedPinId)
    val latestOnCountryTapped by rememberUpdatedState(onCountryTapped)
    val latestOnPinDropped by rememberUpdatedState(onPinDropped)
    val latestOnPinTapped by rememberUpdatedState(onPinTapped)
    val palette = rememberWorldMapPosterPalette()
    val proxySignature = remember(asset, highlightedCountryCodes) {
        highlightedCountryCodes.toList().sorted().joinToString("|")
    }

    LaunchedEffect(asset, canvasSize, centerX, centerY, zoomScale, interactive) {
        val idleForMs = SystemClock.uptimeMillis() - lastLocalViewportTouchMs
        if (!interactive || idleForMs > 120L) {
            liveViewportState = externalViewportState
        }
    }

    LaunchedEffect(asset, interactive, latestViewportCallback) {
        val viewportCallback = latestViewportCallback ?: return@LaunchedEffect
        snapshotFlow { liveViewportState }
            .distinctUntilChanged()
            .collectLatest(viewportCallback)
    }

    LaunchedEffect(asset, proxySignature, canvasSize) {
        val previous = proxyBitmap
        proxyBitmap = null
        if (canvasSize == IntSize.Zero) {
            if (previous != null && !previous.isRecycled) {
                previous.recycle()
            }
            return@LaunchedEffect
        }
        val workingLongEdge = (max(canvasSize.width, canvasSize.height) * 0.72f).toInt()
        val rendered = renderMapEditorProxyBitmap(
            asset = asset,
            highlightedCountryCodes = highlightedCountryCodes,
            requestedLongEdgePx = workingLongEdge,
        )
        proxyBitmap = rendered
        if (previous != null && previous !== rendered && !previous.isRecycled) {
            previous.recycle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val current = proxyBitmap
            if (current != null && !current.isRecycled) {
                current.recycle()
            }
            proxyBitmap = null
        }
    }

    MapPosterScaffold(
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        compactLandscape = compactLandscape,
        palette = palette,
        showPosterChromeOverlay = false,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(asset, interactive) {
                        if (!interactive) return@pointerInput
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            if (canvasSize == IntSize.Zero) return@detectTransformGestures
                            val currentViewportState = latestViewportState
                            val currentScale = renderScale(asset, canvasWidth, canvasHeight, currentViewportState.zoomScale)
                            if (currentScale <= 0f) return@detectTransformGestures
                            val worldPanX = pan.x / currentScale
                            val worldPanY = pan.y / currentScale
                            val centerWorld = Offset(
                                asset.viewBox.width * currentViewportState.centerX,
                                asset.viewBox.height * currentViewportState.centerY,
                            )
                            liveViewportState = clampViewportState(
                                asset = asset,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                centerX = (centerWorld.x - worldPanX) / asset.viewBox.width,
                                centerY = (centerWorld.y - worldPanY) / asset.viewBox.height,
                                zoomScale = currentViewportState.zoomScale * gestureZoom,
                            )
                            lastLocalViewportTouchMs = SystemClock.uptimeMillis()
                        }
                    }
                    .pointerInput(asset, interactive, pinDropMode) {
                        if (!interactive || canvasSize == IntSize.Zero) return@pointerInput
                        detectTapGestures { tapOffset ->
                            val currentViewportState = latestViewportState
                            val nearestPin = findNearestPin(
                                asset = asset,
                                pins = latestPins,
                                viewport = currentViewportState,
                                canvasSize = Size(canvasWidth, canvasHeight),
                                tapOffset = tapOffset,
                            )
                            if (pinDropMode) {
                                val worldOffset = screenToWorld(
                                    asset = asset,
                                    canvasSize = Size(canvasWidth, canvasHeight),
                                    viewport = currentViewportState,
                                    screenOffset = tapOffset,
                                )
                                latestOnPinDropped?.invoke(
                                    (worldOffset.x / asset.viewBox.width).coerceIn(0f, 1f),
                                    (worldOffset.y / asset.viewBox.height).coerceIn(0f, 1f),
                                )
                                return@detectTapGestures
                            }
                            if (nearestPin != null) {
                                latestOnPinTapped?.invoke(nearestPin.id)
                                return@detectTapGestures
                            }
                            if (asset is VectorWorldMapAssetData) {
                                val worldOffset = screenToWorld(
                                    asset = asset,
                                    canvasSize = Size(canvasWidth, canvasHeight),
                                    viewport = currentViewportState,
                                    screenOffset = tapOffset,
                                )
                                val tappedCountry = asset.countries
                                    .asReversed()
                                    .firstOrNull { country ->
                                        country.bounds.contains(worldOffset) &&
                                            country.hitRegion.contains(worldOffset.x.toInt(), worldOffset.y.toInt())
                                    }
                                if (tappedCountry != null) {
                                    latestOnCountryTapped?.invoke(tappedCountry)
                                } else {
                                    latestOnPinTapped?.invoke(null)
                                }
                            } else {
                                latestOnPinTapped?.invoke(null)
                            }
                        }
                    },
            ) {
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.drawColor(palette.background.toArgb())
                    val currentProxy = proxyBitmap
                    val currentViewportState = latestViewportState
                    if (currentProxy != null && !currentProxy.isRecycled) {
                        val currentRenderScale = renderScale(asset, size.width, size.height, currentViewportState.zoomScale)
                        val left = size.width / 2f - asset.viewBox.width * currentViewportState.centerX * currentRenderScale
                        val top = size.height / 2f - asset.viewBox.height * currentViewportState.centerY * currentRenderScale
                        nativeCanvas.drawBitmap(
                            currentProxy,
                            null,
                            RectF(
                                left,
                                top,
                                left + asset.viewBox.width * currentRenderScale,
                                top + asset.viewBox.height * currentRenderScale,
                            ),
                            proxyPaint,
                        )
                    }
                    renderMapPinsOverlay(
                        canvas = nativeCanvas,
                        asset = asset,
                        viewport = currentViewportState,
                        pins = latestPins,
                        selectedPinId = latestSelectedPinId,
                        showSelectedPin = interactive,
                        widthPx = size.width,
                        heightPx = size.height,
                    )
                    renderMapPosterChrome(
                        canvas = nativeCanvas,
                        widthPx = size.width,
                        heightPx = size.height,
                        countryLabel = countryLabel,
                        cityLabel = cityLabel,
                        showViewfinderMask = false,
                    )
                }
            }
            if (proxyBitmap == null) {
                Text(
                    text = "Preparing lightweight editor preview",
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.placeholder,
                )
            }
        }
    }
}
