package one.crinklecod.cc

import androidx.compose.runtime.Immutable
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** CPU and RAM load for the whole phone (not just this app). */
@Immutable
data class SystemLoad(
    val cpuPercent: Int,
    val ramUsedMb: Int,
    val ramTotalMb: Int,
) {
    val ramPercent: Int get() = if (ramTotalMb > 0) ramUsedMb * 100 / ramTotalMb else 0
}

/** One read of stats.json: system load plus each runit service's health. */
data class StatsSnapshot(
    val load: SystemLoad,
    /** runit service name -> healthy (running, and its HTTP check passing if it has one) */
    val services: Map<String, Boolean>,
    /** Unix seconds when update-stats.sh wrote it */
    val epochSeconds: Long,
) {
    val isFresh: Boolean get() = System.currentTimeMillis() / 1000 - epochSeconds < STALE_AFTER_S

    private companion object {
        const val STALE_AFTER_S = 60
    }
}

/**
 * Apps can't read /proc/stat on Android 10+, so system-wide CPU comes from the Termux
 * side: update-stats.sh samples it through Shizuku every 4 s into ~/dashboard/stats.json,
 * which darkhttpd serves on :8000.
 */
object SystemStats {
    private const val URL_STATS = "http://127.0.0.1:8000/stats.json"

    /** Blocking; call off the main thread. Null when the feed is missing or stale. */
    fun fetch(timeoutMs: Int = 2000): SystemLoad? = fetchSnapshot(timeoutMs)?.takeIf { it.isFresh }?.load

    /** Blocking; call off the main thread. Null when the file is missing or unreadable. */
    fun fetchSnapshot(timeoutMs: Int = 2000): StatsSnapshot? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(URL_STATS).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
            }
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val ram = json.getJSONObject("ram")
            val services = json.optJSONObject("services")?.let { obj ->
                obj.keys().asSequence().associateWith { obj.getJSONObject(it).optBoolean("ok") }
            }.orEmpty()
            val load = SystemLoad(
                cpuPercent = json.getJSONObject("cpu").optInt("percent", 0).coerceIn(0, 100),
                // "5175MB" -> 5175
                ramUsedMb = ram.optString("used").filter(Char::isDigit).toIntOrNull() ?: 0,
                ramTotalMb = ram.optString("total").filter(Char::isDigit).toIntOrNull() ?: 0,
            )
            StatsSnapshot(load, services, json.optLong("epoch", 0L))
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
