package one.crinklecod.razrguard.launcher

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.crinklecod.razrguard.GuardService

data class AppInfo(
    val label: String,
    val packageName: String
)

/** Favorite launcher apps, persisted in the shared RazrGuard prefs file. */
object FavoritesStore {

    private const val KEY_FAVORITES = "favorite_packages"
    private val DEFAULT_FAVORITES = setOf(
        "com.termux",
        "com.tailscale.ipn",
        "moe.shizuku.privileged.api",
        "com.android.settings"
    )

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(GuardService.PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, null)
            ?.toSet()
            ?: DEFAULT_FAVORITES

    fun save(context: Context, favorites: Set<String>) {
        context.getSharedPreferences(GuardService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_FAVORITES, favorites.toHashSet())
            .apply()
    }
}

/** Every launchable app except RazrGuard itself, sorted by label. Blocking: call off the main thread. */
fun queryLaunchableApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(mainIntent, 0)
        .mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) null
            else AppInfo(label = info.loadLabel(pm).toString(), packageName = pkg)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

/** Placeholder label for a favorite whose package isn't (or isn't yet known to be) installed. */
fun fallbackAppInfo(packageName: String) = AppInfo(
    label = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
    packageName = packageName
)

/** Small LRU of rasterized launcher icons so scrolling the deck/picker doesn't re-decode drawables. */
object AppIconCache {

    private const val TAG = "RazrGuard/Icons"
    private val cache = LruCache<String, ImageBitmap>(96)

    fun peek(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
                    .also { cache.put(packageName, it) }
            } catch (e: Exception) {
                Log.d(TAG, "No icon for $packageName: ${e.message}")
                null
            }
        }
    }
}
