package io.gelio.app.core.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import kotlinx.coroutines.Dispatchers

/**
 * App-wide tuned Coil ImageLoader. Kiosk workload: plenty of RAM, repeated bitmap
 * reuse across navigation, quality > battery. Registered via ImageLoaderFactory in
 * [io.gelio.app.app.GelioApp].
 */
object AppImageLoader {
    fun build(context: Context): ImageLoader = ImageLoader.Builder(context)
        .allowHardware(true)
        .allowRgb565(false)
        .respectCacheHeaders(false)
        .fetcherDispatcher(Dispatchers.IO)
        .decoderDispatcher(Dispatchers.IO)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.30) // 30% of app heap — kiosk
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.05)
                .build()
        }
        .build()
}
