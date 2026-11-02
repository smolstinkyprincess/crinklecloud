package one.crinklecod.cc.ui.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import one.crinklecod.cc.TouchPause

/** Shield state as the dashboard sees it. */
enum class ShieldMode { Off, Active, Paused }

fun shieldModeOf(isActive: Boolean, pause: TouchPause?): ShieldMode = when {
    pause != null -> ShieldMode.Paused
    isActive -> ShieldMode.Active
    else -> ShieldMode.Off
}

/**
 * Milliseconds left in [pause], ticking every frame. Returned as [State] so callers can derive
 * coarser values (e.g. whole seconds) without recomposing every frame.
 */
@Composable
fun rememberPauseRemaining(pause: TouchPause?): State<Long> =
    produceState(initialValue = pause?.remainingMs() ?: 0L, pause) {
        if (pause == null) {
            value = 0L
            return@produceState
        }
        while (true) {
            val left = pause.remainingMs()
            value = left
            if (left == 0L) break
            withFrameMillis { }
        }
    }

private fun TouchPause.remainingMs(): Long =
    (endsAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
