package io.gelio.app.core.performance

import android.app.Activity
import android.view.FrameMetrics
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Attaches to Activity.window and logs slow / janky frames via FrameMetrics.
 * Also periodically emits a rollup (frame counts + jank% + worst frame).
 */
object FrameTracker {

    private val totalFrames = AtomicInteger(0)
    private val slowFrames = AtomicInteger(0)     // > 20ms
    private val jankFrames = AtomicInteger(0)     // > 34ms
    private val frozenFrames = AtomicInteger(0)   // > 700ms
    private val worstFrameNs = AtomicLong(0)
    private val lastRollupMs = AtomicLong(0)

    fun attach(activity: Activity) {
        val window = activity.window
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        window.addOnFrameMetricsAvailableListener({ _, frameMetrics, _ ->
            val copy = FrameMetrics(frameMetrics)
            val totalNs = copy.getMetric(FrameMetrics.TOTAL_DURATION)
            val totalMs = totalNs / 1_000_000.0
            totalFrames.incrementAndGet()

            // Stage breakdown
            fun ms(key: Int): Double = copy.getMetric(key) / 1_000_000.0

            if (totalMs > PerfLog.FRAME_FROZEN_MS) {
                frozenFrames.incrementAndGet()
                PerfLog.e(
                    "FRAME",
                    "FROZEN ${"%.1f".format(totalMs)}ms " +
                        "(input=${"%.1f".format(ms(FrameMetrics.INPUT_HANDLING_DURATION))} " +
                        "anim=${"%.1f".format(ms(FrameMetrics.ANIMATION_DURATION))} " +
                        "layout=${"%.1f".format(ms(FrameMetrics.LAYOUT_MEASURE_DURATION))} " +
                        "draw=${"%.1f".format(ms(FrameMetrics.DRAW_DURATION))} " +
                        "sync=${"%.1f".format(ms(FrameMetrics.SYNC_DURATION))} " +
                        "gpu=${"%.1f".format(ms(FrameMetrics.COMMAND_ISSUE_DURATION))})"
                )
            } else if (totalMs > PerfLog.FRAME_JANK_MS) {
                jankFrames.incrementAndGet()
                PerfLog.w(
                    "FRAME",
                    "JANK ${"%.1f".format(totalMs)}ms " +
                        "(input=${"%.1f".format(ms(FrameMetrics.INPUT_HANDLING_DURATION))} " +
                        "anim=${"%.1f".format(ms(FrameMetrics.ANIMATION_DURATION))} " +
                        "layout=${"%.1f".format(ms(FrameMetrics.LAYOUT_MEASURE_DURATION))} " +
                        "draw=${"%.1f".format(ms(FrameMetrics.DRAW_DURATION))} " +
                        "gpu=${"%.1f".format(ms(FrameMetrics.COMMAND_ISSUE_DURATION))})"
                )
            } else if (totalMs > PerfLog.FRAME_WARN_MS) {
                slowFrames.incrementAndGet()
                PerfLog.d("FRAME", "slow ${"%.1f".format(totalMs)}ms")
            }

            if (totalNs > worstFrameNs.get()) worstFrameNs.set(totalNs)

            val now = android.os.SystemClock.elapsedRealtime()
            val last = lastRollupMs.get()
            if (last == 0L) lastRollupMs.set(now)
            else if (now - last >= 5_000L && lastRollupMs.compareAndSet(last, now)) {
                val total = totalFrames.getAndSet(0)
                val slow = slowFrames.getAndSet(0)
                val jank = jankFrames.getAndSet(0)
                val frozen = frozenFrames.getAndSet(0)
                val worstMs = worstFrameNs.getAndSet(0) / 1_000_000.0
                if (total > 0) {
                    val jankPct = (jank + frozen) * 100.0 / total
                    PerfLog.d(
                        "FRAME",
                        "rollup 5s: frames=$total slow=$slow jank=$jank frozen=$frozen " +
                            "jank%=${"%.1f".format(jankPct)} worst=${"%.1f".format(worstMs)}ms"
                    )
                }
            }
        }, handler)
        PerfLog.d("LIFE", "FrameTracker attached")
    }
}
