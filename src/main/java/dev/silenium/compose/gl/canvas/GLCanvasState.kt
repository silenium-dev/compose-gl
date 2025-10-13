package dev.silenium.compose.gl.canvas

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

interface Stats<T : Comparable<T>> {
    val values: List<T>
    val sum: T
    val average: T
    val min: T
    val max: T
    val median: T
    fun percentile(percentile: Double, direction: Percentile = Percentile.UP): T

    enum class Percentile {
        UP, LOWEST
    }
}

data class DurationStats(override val values: List<Duration>) : Stats<Duration> {
    override val sum by lazy { values.fold(Duration.ZERO) { a, it -> a + it } }
    override val average by lazy { sum / values.size }
    override val min by lazy { values.minOrNull() ?: Duration.ZERO }
    override val max by lazy { values.maxOrNull() ?: Duration.ZERO }
    override val median by lazy {
        if (values.isEmpty()) return@lazy Duration.ZERO
        if (values.size == 1) return@lazy values.first()
        val sorted = values.sorted()
        val middle = sorted.size / 2
        if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }

    override fun percentile(percentile: Double, direction: Stats.Percentile): Duration {
        if (values.isEmpty()) return Duration.ZERO
        val sorted = values.sorted()
        val index = when (direction) {
            Stats.Percentile.UP -> (percentile * sorted.size).toInt()
            Stats.Percentile.LOWEST -> sorted.size - (percentile * sorted.size).toInt() - 1
        }
        return sorted[index]
    }
}

data class DoubleStats(override val values: List<Double>) : Stats<Double> {
    override val sum by lazy { values.fold(0.0) { a, it -> a + it } }
    override val average by lazy { sum / values.size }
    override val min by lazy { values.minOrNull() ?: 0.0 }
    override val max by lazy { values.maxOrNull() ?: 0.0 }
    override val median by lazy {
        if (values.isEmpty()) return@lazy 0.0
        if (values.size == 1) return@lazy values.first()
        val sorted = values.sorted()
        val middle = sorted.size / 2
        if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }

    override fun percentile(percentile: Double, direction: Stats.Percentile): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = when (direction) {
            Stats.Percentile.UP -> (percentile * sorted.size).toInt()
            Stats.Percentile.LOWEST -> sorted.size - (percentile * sorted.size).toInt() - 1
        }
        return sorted[index]
    }
}

data class RollingWindowStatistics(
    val windowSize: Duration = 5.seconds,
    val values: Map<Long, Duration> = emptyMap(),
) {
    val frameTimes by lazy { DurationStats(values.values.toList()) }
    val fps by lazy {
        DoubleStats(
            if (values.size < 2) emptyList()
            else values.keys.sorted().zipWithNext().map { (a, b) -> 1_000_000_000.0 / (b - a) }
        )
    }

    fun add(nanos: Long, time: Duration): RollingWindowStatistics {
        val newValues = values.toMutableMap()
        newValues[nanos] = time
        return copy(values = newValues.filter { it.key >= nanos - windowSize.inWholeNanoseconds })
    }
}

@OptIn(ExperimentalTime::class)
class GLCanvasState {
    private val renderStatisticsMutable = MutableStateFlow(RollingWindowStatistics())
    private val displayStatisticsMutable = MutableStateFlow(RollingWindowStatistics())
    internal var invalidations by mutableStateOf(0L)
    internal var lastFrame: Long? = null

    val renderStatistics: StateFlow<RollingWindowStatistics> get() = renderStatisticsMutable.asStateFlow()
    val displayStatistics: StateFlow<RollingWindowStatistics> get() = displayStatisticsMutable.asStateFlow()

    fun requestUpdate() {
        invalidations = System.nanoTime()
    }

    internal fun onDisplay(nanos: Long, frameTime: Duration) {
        displayStatisticsMutable.tryEmit(displayStatisticsMutable.value.add(nanos, frameTime))
    }

    internal fun onRender(nanos: Long, frameTime: Duration) {
        renderStatisticsMutable.tryEmit(renderStatisticsMutable.value.add(nanos, frameTime))
    }
}

@Composable
fun rememberGLCanvasState() = remember { GLCanvasState() }
