package one.crinklecod.cc

import android.util.Log

/**
 * Keeps Android from killing the server stack: phantom process killer off, Doze/standby
 * exemptions for every package the stack depends on. setup-razrguard.sh applies these once
 * over adb; OTAs, device_config syncs and the system itself can quietly undo them, so this
 * re-verifies through Shizuku and re-applies only when something has drifted.
 */
object SurvivalLayer {

    private const val TAG = "RazrGuard/Survival"
    private const val MAX_PHANTOM = "2147483647"

    // Shizuku must survive too, or every privileged recovery path disappears with it
    private val EXTRA_PACKAGES = listOf(
        "com.termux",
        "com.termux.boot",
        "com.tailscale.ipn",
        "moe.shizuku.privileged.api",
    )

    @Volatile
    private var running = false

    /** Safe to call repeatedly; no-ops if Shizuku is unavailable. */
    fun ensure(selfPackage: String) {
        if (running) return
        running = true
        try {
            val packages = listOf(selfPackage) + EXTRA_PACKAGES
            val drift = findDrift(packages) ?: return
            if (drift.isEmpty()) {
                Log.i(TAG, "Verified: survival settings intact")
                return
            }
            Log.w(TAG, "Drift detected (${drift.joinToString()}), re-applying")
            ShizukuBridge.exec(applyScript(packages))
            val after = findDrift(packages)
            if (after.isNullOrEmpty()) {
                Log.i(TAG, "Re-applied survival settings")
            } else {
                Log.e(TAG, "Still drifted after re-apply: ${after.joinToString()}")
            }
        } finally {
            running = false
        }
    }

    /** Names of settings that are wrong; empty if all good, null if Shizuku is unavailable. */
    private fun findDrift(packages: List<String>): List<String>? {
        val phantom = ShizukuBridge.run(
            "device_config get activity_manager max_phantom_processes"
        ) ?: return null
        val whitelist = ShizukuBridge.run("dumpsys deviceidle whitelist") ?: return null

        val drift = mutableListOf<String>()
        if (phantom.second.trim() != MAX_PHANTOM) drift += "max_phantom_processes"
        // Uninstalled packages can't be whitelisted; only flag ones that are actually present
        val installed = ShizukuBridge.run("pm list packages")?.second.orEmpty()
        val present = packages.filter { "package:$it" in installed.lines() }
        // Whitelist lines look like "user,com.termux,10234"
        val whitelisted = whitelist.second.replace('\n', ',')
        present.filter { ",$it," !in whitelisted }.forEach { drift += "doze:$it" }

        // 10 = STANDBY_BUCKET_ACTIVE; one shell for all packages
        val buckets = ShizukuBridge.run(
            present.joinToString("; ") { "echo \"$it \$(am get-standby-bucket $it)\"" }
        )?.second.orEmpty().lines().map { it.trim() }
        present.filter { "$it 10" !in buckets }.forEach { drift += "bucket:$it" }
        return drift
    }

    // One sh invocation; every step is idempotent and allowed to fail independently
    private fun applyScript(packages: List<String>): String = buildString {
        append("device_config set_sync_disabled_for_tests persistent; ")
        append("device_config put activity_manager max_phantom_processes $MAX_PHANTOM; ")
        append("settings put global settings_enable_monitor_phantom_procs false; ")
        // Prevent Wi-Fi radio sleep spikes (DTIM)
        append("iw dev wlan0 set power_save off 2>/dev/null || true; ")
        for (p in packages) {
            append("dumpsys deviceidle whitelist +$p; ")
            append("cmd appops set $p RUN_ANY_IN_BACKGROUND allow; ")
            append("cmd appops set $p RUN_IN_BACKGROUND allow; ")
            append("am set-standby-bucket $p active; ")
            // Immunize against Low Memory Killer (LMK) by setting oom_score_adj to -1000
            append("for pid in \$(pidof $p 2>/dev/null); do echo -1000 > /proc/\$pid/oom_score_adj 2>/dev/null || true; done; ")
        }
        append("true")
    }
}
