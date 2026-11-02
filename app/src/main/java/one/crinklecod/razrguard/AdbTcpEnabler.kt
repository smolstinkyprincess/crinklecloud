package one.crinklecod.razrguard

import android.util.Log
import kotlinx.coroutines.delay
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Re-binds adbd to TCP port 5555 through Shizuku (shell uid) so the headless phone stays
 * reachable with `adb connect <ip>:5555` after a reboot.
 */
object AdbTcpEnabler {

    private const val TAG = "RazrGuard/AdbTcp"
    private const val PORT = 5555
    private const val VERIFY_DELAY_MS = 3_000L

    @Volatile
    private var running = false

    /** Safe to call repeatedly; no-ops if Shizuku is unavailable or adbd is already on 5555. */
    suspend fun enable() {
        if (running) return
        running = true
        try {
            if (isPortListening()) {
                Log.i(TAG, "Skipped: adbd already listening on :$PORT")
                return
            }
            if (!ShizukuBridge.isAvailable()) {
                Log.i(TAG, "Skipped: Shizuku not running or permission not granted")
                return
            }

            // One sh invocation so adbd is restarted even if this shell's parent goes away mid-way
            val exit = ShizukuBridge.exec(
                "setprop service.adb.tcp.port $PORT && stop adbd && start adbd"
            )
            delay(VERIFY_DELAY_MS)

            if (isPortListening()) {
                Log.i(TAG, "Enabled: network ADB listening on :$PORT")
            } else {
                Log.w(TAG, "Failed: adbd not listening on :$PORT after restart (exit=$exit)")
            }
        } finally {
            running = false
        }
    }

    fun isPortListening(): Boolean =
        try {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", PORT), 1_000) }
            true
        } catch (e: Exception) {
            false
        }
}
