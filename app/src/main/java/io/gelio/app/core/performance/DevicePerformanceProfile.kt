package io.gelio.app.core.performance

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.roundToInt

enum class DevicePerformanceClass {
    Compact,
    Balanced,
    High,
}

data class DevicePerformanceProfile(
    val performanceClass: DevicePerformanceClass,
    val imageDecodeScale: Float,
    val thumbnailDecodePx: Int,
    val pdfRenderMaxDimension: Int,
    val useLighterBackgroundEffects: Boolean,
) {
    val isConstrained: Boolean
        get() = performanceClass == DevicePerformanceClass.Compact
}

val LocalDevicePerformanceProfile = compositionLocalOf {
    DevicePerformanceProfile(
        performanceClass = DevicePerformanceClass.Balanced,
        imageDecodeScale = 0.85f,
        thumbnailDecodePx = 420,
        pdfRenderMaxDimension = 1400,
        useLighterBackgroundEffects = false,
    )
}

@Composable
fun rememberDevicePerformanceProfile(): DevicePerformanceProfile {
    val context = LocalContext.current
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp().value.roundToInt() }
    val screenHeightDp = with(density) { containerSize.height.toDp().value.roundToInt() }
    return remember(context, screenWidthDp, screenHeightDp) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryClassMb = activityManager?.memoryClass ?: 256
        val largeMemoryClassMb = activityManager?.largeMemoryClass ?: memoryClassMb
        val lowRam = activityManager?.isLowRamDevice == true
        val screenDp = maxOf(screenWidthDp, screenHeightDp)
        val ramGb = memoryClassMb / 1024f
        val hasHighEndGraphics = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

        when {
            lowRam || memoryClassMb <= 192 || ramGb <= 0.25f -> DevicePerformanceProfile(
                performanceClass = DevicePerformanceClass.Compact,
                imageDecodeScale = 0.62f,
                thumbnailDecodePx = 320,
                pdfRenderMaxDimension = 1200,
                useLighterBackgroundEffects = true,
            )

            largeMemoryClassMb >= 512 && screenDp >= 1000 && hasHighEndGraphics -> DevicePerformanceProfile(
                performanceClass = DevicePerformanceClass.High,
                imageDecodeScale = 1f,
                thumbnailDecodePx = 512,
                pdfRenderMaxDimension = 1600,
                useLighterBackgroundEffects = false,
            )

            else -> DevicePerformanceProfile(
                performanceClass = DevicePerformanceClass.Balanced,
                imageDecodeScale = 0.78f,
                thumbnailDecodePx = 384,
                pdfRenderMaxDimension = 1400,
                useLighterBackgroundEffects = false,
            )
        }
    }
}
