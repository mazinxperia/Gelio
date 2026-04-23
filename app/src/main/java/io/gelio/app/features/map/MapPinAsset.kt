package io.gelio.app.features.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val DEFAULT_PIN_FILL = "rgb(4,136,219)"
private const val THEMED_PIN_FILL = "rgb(221,180,92)"
private const val PIN_BITMAP_WIDTH_PX = 120
private const val PIN_BITMAP_HEIGHT_PX = 168

object MapPinAssetLoader {
    private val mutex = Mutex()
    private var cached: SVG? = null
    private var cachedBitmap: Bitmap? = null

    suspend fun load(context: android.content.Context): SVG {
        mutex.withLock { cached }?.let { return it }
        return withContext(Dispatchers.IO) {
            val rawSvg = context.assets.open(MapAssetPaths.PIN).bufferedReader().use { it.readText() }
            val themedSvg = rawSvg.replace(DEFAULT_PIN_FILL, THEMED_PIN_FILL)
            val svg = SVG.getFromString(themedSvg)
            mutex.withLock {
                cached = svg
            }
            svg
        }
    }

    suspend fun loadBitmap(context: android.content.Context): Bitmap {
        mutex.withLock { cachedBitmap }?.let { return it }
        return withContext(Dispatchers.IO) {
            val svg = load(context)
            val bitmap = createBitmap(PIN_BITMAP_WIDTH_PX, PIN_BITMAP_HEIGHT_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            svg.renderToCanvas(
                canvas,
                RectF(0f, 0f, PIN_BITMAP_WIDTH_PX.toFloat(), PIN_BITMAP_HEIGHT_PX.toFloat()),
            )
            mutex.withLock {
                cachedBitmap = bitmap
            }
            bitmap
        }
    }
}

@Composable
fun rememberMapPinAsset(): SVG? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val asset by produceState<SVG?>(initialValue = null) {
        value = runCatching { MapPinAssetLoader.load(context) }.getOrNull()
    }
    return asset
}

@Composable
fun rememberMapPinBitmap(): Bitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val asset by produceState<Bitmap?>(initialValue = null) {
        value = runCatching { MapPinAssetLoader.loadBitmap(context) }.getOrNull()
    }
    return asset
}
