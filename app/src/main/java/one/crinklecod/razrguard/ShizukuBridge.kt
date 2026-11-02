package one.crinklecod.razrguard

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Optional privileged shell via Shizuku (runs as the `shell` uid, same as adb).
 * Every call degrades gracefully to "not available" if Shizuku isn't running or authorized.
 */
object ShizukuBridge {

    private const val TAG = "RazrGuard/Shizuku"
    private const val PERMISSION_REQUEST_CODE = 4242

    fun isAvailable(): Boolean =
        try {
            Shizuku.pingBinder() && !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }

    /** Asks the Shizuku manager to show its grant dialog if the binder is up but we're not yet authorized. */
    fun requestPermissionIfNeeded() {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
                !Shizuku.shouldShowRequestPermissionRationale()
            ) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku permission request failed", e)
        }
    }

    /**
     * Runs [command] through `sh -c` with shell privileges. Returns the exit code, or null if
     * Shizuku is unavailable. Shizuku 13.x made newProcess() private (it recommends UserService),
     * so it's reached by reflection here; that's fine for a one-shot `am` call.
     */
    fun exec(command: String): Int? = run(command)?.first

    /** Like [exec], but also returns combined stdout+stderr. Null if Shizuku is unavailable. */
    fun run(command: String): Pair<Int, String>? {
        if (!isAvailable()) return null
        return try {
            val newProcess = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = newProcess.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText() +
                process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            Log.i(TAG, "`$command` -> $exit ${output.trim()}")
            exit to output
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku exec failed: $command", e)
            null
        }
    }
}
