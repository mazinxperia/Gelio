package io.gelio.app.core.performance

import android.os.SystemClock
import android.os.Trace
import android.util.Log
import io.gelio.app.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/**
 * Deep performance logger. Filter logcat with tag [TAG].
 *
 * adb logcat -s OMPerf:V
 *
 * Categories:
 *   [FRAME]  slow/jank frames from Window.FrameMetrics
 *   [NAV]    navigation route changes + elapsed time
 *   [VM]     ViewModel flow first-emit latency
 *   [DB]     repository snapshot/query timings
 *   [IMG]    image load results
 *   [RECOMP] composable recomposition counts
 *   [LIFE]   lifecycle / cold-start markers
 *   [MEM]    memory snapshots
 */
object PerfLog {
    const val TAG = "OMPerf"

    // Master switch. Debug builds log; release builds stay silent (zero-cost).
    @Volatile var enabled: Boolean = BuildConfig.DEBUG

    // Thresholds (ms)
    const val FRAME_WARN_MS = 20L    // >20ms = dropped frame at 60Hz
    const val FRAME_JANK_MS = 34L    // >34ms = multi-frame jank
    const val FRAME_FROZEN_MS = 700L // >700ms = frozen frame (ANR-ish)

    private val bootMs = SystemClock.elapsedRealtime()
    fun uptimeMs(): Long = SystemClock.elapsedRealtime() - bootMs

    fun d(category: String, msg: String) {
        if (!enabled) return
        Log.d(TAG, "[${"%6d".format(uptimeMs())}][$category] $msg")
    }

    fun w(category: String, msg: String) {
        if (!enabled) return
        Log.w(TAG, "[${"%6d".format(uptimeMs())}][$category] $msg")
    }

    fun e(category: String, msg: String, t: Throwable? = null) {
        if (!enabled) return
        if (t != null) Log.e(TAG, "[${"%6d".format(uptimeMs())}][$category] $msg", t)
        else Log.e(TAG, "[${"%6d".format(uptimeMs())}][$category] $msg")
    }

    inline fun <T> trace(section: String, block: () -> T): T {
        if (!enabled) return block()
        val start = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection(section)
        try {
            return block()
        } finally {
            Trace.endSection()
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
            if (elapsedMs > 8) d("TRACE", "$section took ${"%.1f".format(elapsedMs)}ms")
        }
    }
}

/**
 * Wraps a Flow to log the time between subscription and first emission.
 * Cheap: only logs once per [key] and per subscription.
 */
fun <T> Flow<T>.logFirstEmit(key: String): Flow<T> {
    var start = 0L
    var logged = false
    return onStart {
        start = SystemClock.elapsedRealtime()
        logged = false
    }.onEach {
        if (!logged) {
            val dt = SystemClock.elapsedRealtime() - start
            PerfLog.d("VM", "$key first-emit in ${dt}ms")
            logged = true
        }
    }
}

/**
 * Tracks how often a composable recomposes. Logs on every Nth recomp.
 * Usage:  TrackRecomposition("DesignShell")
 */
@Composable
fun TrackRecomposition(name: String, logEvery: Int = 5) {
    val counter = remember { AtomicInteger(0) }
    val c = counter.incrementAndGet()
    if (c == 1 || c % logEvery == 0) {
        PerfLog.d("RECOMP", "$name recomposed $c times")
    }
    DisposableEffect(name) {
        PerfLog.d("RECOMP", "$name entered composition")
        onDispose { PerfLog.d("RECOMP", "$name left composition (final count=${counter.get()})") }
    }
}
