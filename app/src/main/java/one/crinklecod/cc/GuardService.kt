package one.crinklecod.cc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL

data class BatteryInfo(
    val levelPercent: Int,
    val tempCelsius: Float,
    val isPlugged: Boolean,
    val isOverheating: Boolean
)

data class NetworkIps(
    val lanIp: String? = null,
    val tailscaleIp: String? = null
)

/** A temporary touch-blocker suspension; the overlay comes back at [endsAtElapsedMs] (elapsedRealtime). */
data class TouchPause(
    val endsAtElapsedMs: Long,
    val durationMs: Long
)

class GuardService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchdogJob: Job? = null
    private var consecutiveFailures = 0
    private var sshFailures = 0
    private var lastSurvivalCheck = 0L

    private var touchBlockerView: View? = null
    private var touchBlockerParams: WindowManager.LayoutParams? = null
    private var cornerTapCount = 0
    private var lastCornerTapTime = 0L
    private var touchBlockerJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempC = tempRaw / 10.0f
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            val overheating = tempC >= 45.0f

            if (overheating && _batteryInfo.value?.isOverheating != true) {
                Log.w(TAG, "⚠️ HIGH BATTERY TEMPERATURE: ${tempC}°C! Prop phone in tent mode to cool down.")
            }

            _batteryInfo.value = BatteryInfo(
                levelPercent = percent,
                tempCelsius = tempC,
                isPlugged = plugged,
                isOverheating = overheating
            )
        }
    }

    // Fires immediately if Shizuku is already up, and again whenever its server (re)starts
    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        _shizukuStatus.value = true
        ShizukuBridge.requestPermissionIfNeeded()
        scope.launch {
            AdbTcpEnabler.enable()
            _adbTcpStatus.value = AdbTcpEnabler.isPortListening()
        }
        scope.launch { SurvivalLayer.ensure(packageName) }
        applyDisplayAndBacklightOptimizations()
    }

    // Covers the case where permission is granted after the binder is already up
    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, _ ->
            _shizukuStatus.value = ShizukuBridge.isAvailable()
            scope.launch {
                AdbTcpEnabler.enable()
                _adbTcpStatus.value = AdbTcpEnabler.isPortListening()
            }
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true

        val savedKeepScreenOn = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_KEEP_SCREEN_ON, false)
        _keepScreenOn.value = savedKeepScreenOn

        // Start foreground service with Android 14 type safety
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Keep CPU awake when the screen shuts off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RazrGuard::Lock").apply {
            setReferenceCounted(false)
            acquire()
        }

        // Keep Wi-Fi chip in low-latency/high-perf mode (prevents DTIM 802.11 sleep spikes)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "RazrGuard::WifiLock")
        } else {
            @Suppress("DEPRECATION")
            wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "RazrGuard::WifiLock")
        }?.apply {
            setReferenceCounted(false)
            acquire()
        }

        // Register battery and thermal receiver
        try {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register battery receiver", e)
        }

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderListener)

        // Let Ethernet/Wi-Fi come up after boot, then try once regardless of listeners
        scope.launch {
            updateNetworkIps()
            delay(ADB_TCP_BOOT_DELAY_MS)
            AdbTcpEnabler.enable()
            _adbTcpStatus.value = AdbTcpEnabler.isPortListening()
            updateNetworkIps()
        }

        enableTouchBlocker()

        scope.launch {
            delay(8000)
            applyDisplayAndBacklightOptimizations()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (watchdogJob?.isActive != true) {
            watchdogJob = scope.launch { watchdogLoop() }
        }
        return START_STICKY
    }

    private fun isSshRunning(): Boolean {
        val running = isPortListening(SSH_PORT, 2000)
        _sshStatus.value = running
        return running
    }

    private fun applyDisplayAndBacklightOptimizations() {
        if (ShizukuBridge.isAvailable()) {
            scope.launch {
                try {
                    Log.i(TAG, "Applying display/backlight settings via Shizuku...")
                    ShizukuBridge.exec("settings put system screen_brightness 0")
                    if (_keepScreenOn.value) {
                        ShizukuBridge.exec("svc power stayon true")
                        ShizukuBridge.exec("settings put global stay_on_while_plugged_in 7")
                        ShizukuBridge.exec("settings put system screen_off_timeout 2147483647")
                    } else {
                        ShizukuBridge.exec("svc power stayon false")
                        ShizukuBridge.exec("settings put global stay_on_while_plugged_in 0")
                        ShizukuBridge.exec("settings put system screen_off_timeout $DEFAULT_SCREEN_TIMEOUT_MS")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply backlight settings", e)
                }
            }
        } else {
            Log.w(TAG, "Shizuku not ready; skipping backlight optimizations.")
        }
    }

    private suspend fun watchdogLoop() {
        delay(INITIAL_GRACE_MS)
        while (scope.isActive) {
            _shizukuStatus.value = ShizukuBridge.isAvailable()
            _adbTcpStatus.value = AdbTcpEnabler.isPortListening()

            if (isDashboardHealthy()) {
                if (consecutiveFailures > 0) Log.i(TAG, "darkhttpd healthy again")
                consecutiveFailures = 0
            } else {
                consecutiveFailures++
                Log.w(TAG, "Health check failed ($consecutiveFailures/$FAILURE_THRESHOLD)")
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    reviveServerStack()
                    consecutiveFailures = 0
                    // Give the stack (incl. proot services) time to come up before judging again
                    delay(RECOVERY_GRACE_MS)
                    continue
                }
            }
            checkSsh()
            checkMultiServices()
            updateNetworkIps()
            checkSurvivalLayer()
            delay(CHECK_INTERVAL_MS)
        }
    }

    private fun updateNetworkIps() {
        var lan: String? = null
        var tailscale: String? = null

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        when {
                            host.startsWith("100.") || name.contains("tun") || name.contains("tailscale") -> {
                                tailscale = host
                            }
                            name.startsWith("wlan") || name.startsWith("eth") || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.") -> {
                                if (lan == null) lan = host
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }

        _networkIps.value = NetworkIps(lanIp = lan, tailscaleIp = tailscale)
    }

    /**
     * Prefers the shared status feed (stats.json from update-stats.sh: runit state + HTTP
     * checks), so the app, web dashboard and alerts agree. Falls back to probing ports
     * itself when the feed is missing or stale (e.g. darkhttpd or update-stats is down).
     */
    private fun checkMultiServices() {
        val feed = SystemStats.fetchSnapshot()?.takeIf { it.isFresh }?.services
        if (feed != null) {
            _jellyfinStatus.value = feed["jellyfin"]
            _suwayomiStatus.value = feed["suwayomi"]
            _transmissionStatus.value = feed["transmission"]
            _vaultwardenStatus.value = feed["vaultwarden"]?.let { it && feed["caddy"] == true }
            _filebrowserStatus.value = feed["filebrowser"]
            return
        }
        _jellyfinStatus.value = isEndpointHealthy("http://127.0.0.1:8096/health", 2000) || isPortListening(8096, 1000)
        _suwayomiStatus.value = isEndpointHealthy("http://127.0.0.1:4567/", 2000) || isPortListening(4567, 1000)
        _transmissionStatus.value = isPortListening(9091, 1000)
        // Vaultwarden itself (loopback only); Caddy terminates TLS for it on :8443
        _vaultwardenStatus.value = isEndpointHealthy("http://127.0.0.1:8088/alive", 2000) && isPortListening(8443, 1000)
        _filebrowserStatus.value = isEndpointHealthy("http://127.0.0.1:8080/health", 2000)
    }

    private fun checkSurvivalLayer() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSurvivalCheck < SURVIVAL_CHECK_INTERVAL_MS) return
        lastSurvivalCheck = now
        SurvivalLayer.ensure(packageName)
    }

    private fun checkSsh() {
        if (isSshRunning()) {
            if (sshFailures > 0) Log.i(TAG, "sshd reachable again")
            sshFailures = 0
            return
        }
        sshFailures++
        Log.w(TAG, "sshd check failed ($sshFailures/$SSH_FAILURE_THRESHOLD)")
        if (sshFailures >= SSH_FAILURE_THRESHOLD) {
            Log.w(TAG, "sshd unreachable on :$SSH_PORT, restarting it")
            runInTermux(TERMUX_BASH, arrayOf("-c", "SVDIR=$TERMUX_SVDIR exec sv restart sshd"))
            sshFailures = 0
        }
    }

    private fun isDashboardHealthy(): Boolean {
        val healthy = isEndpointHealthy(HEALTH_URL, HTTP_TIMEOUT_MS)
        _darkhttpdStatus.value = healthy
        return healthy
    }

    private fun isEndpointHealthy(urlString: String, timeoutMs: Int = 2000): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
            }
            conn.responseCode < 500
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun isPortListening(port: Int, timeoutMs: Int = 1500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun reviveServerStack() {
        Log.w(TAG, "darkhttpd unreachable, attempting recovery")

        if (ShizukuBridge.isAvailable()) {
            ShizukuBridge.exec("am start-foreground-service -n com.termux/.app.TermuxService")
            ShizukuBridge.exec("am start -n com.termux/.app.TermuxActivity")
        }

        runInTermux(TERMUX_BASH, arrayOf(RECOVERY_SCRIPT))
    }

    fun restartServerStack() {
        Log.w(TAG, "Restarting server stack via $RESTART_SCRIPT")

        if (ShizukuBridge.isAvailable()) {
            ShizukuBridge.exec("am start-foreground-service -n com.termux/.app.TermuxService")
        }

        runInTermux(TERMUX_BASH, arrayOf(RESTART_SCRIPT))
    }

    private fun runInTermux(path: String, args: Array<String>) {
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                component = ComponentName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", path)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "RUN_COMMAND intent failed: $path", e)
        }
    }

    /** Flushes filesystem write buffers; via Shizuku when available, otherwise through Termux. */
    fun syncStorage() {
        scope.launch {
            if (ShizukuBridge.exec("sync") == null) {
                runInTermux(TERMUX_BASH, arrayOf("-c", "sync"))
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "razr_guard_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Razr Guard Watchdog",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Razr Server Guard Running")
                .setContentText("CPU & Wi-Fi active, watchdog monitoring server processes")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Razr Server Guard Running")
                .setContentText("CPU & Wi-Fi active, watchdog monitoring server processes")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }

    fun enableTouchBlocker() {
        // Any explicit (re)engage ends a pending pause
        touchBlockerJob?.cancel()
        touchBlockerJob = null
        _touchPause.value = null
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot enable touch blocker: Overlay permission not granted.")
            _isTouchBlockerActive.value = false
            return
        }
        if (touchBlockerView != null) return

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    (if (_keepScreenOn.value) WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON else 0),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        touchBlockerParams = params

        touchBlockerView = FrameLayout(this).apply {
            addView(createUnlockHeart())
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = SystemClock.elapsedRealtime()
                    val width = v.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
                    val inTopRightCorner = event.x >= (width - 250) && event.y <= 250
                    if (inTopRightCorner) {
                        if (now - lastCornerTapTime < 3000L) {
                            cornerTapCount++
                        } else {
                            cornerTapCount = 1
                        }
                        lastCornerTapTime = now
                        Log.d(TAG, "Touch blocker escape tap: $cornerTapCount/5")

                        if (cornerTapCount >= 5) {
                            cornerTapCount = 0
                            pauseTouchBlockerTemporarily(120_000L)
                            return@setOnTouchListener false
                        }
                    } else {
                        cornerTapCount = 0
                    }
                }
                true
            }
        }

        try {
            windowManager.addView(touchBlockerView, params)
            _isTouchBlockerActive.value = true
            Log.i(TAG, "Touch blocker overlay attached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach touch blocker", e)
            touchBlockerView = null
            touchBlockerParams = null
            _isTouchBlockerActive.value = false
        }
    }

    fun disableTouchBlocker() {
        touchBlockerJob?.cancel()
        _touchPause.value = null
        touchBlockerView?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing touch blocker", e)
            }
            touchBlockerView = null
            touchBlockerParams = null
        }
        _isTouchBlockerActive.value = false
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        _keepScreenOn.value = keepOn
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KEEP_SCREEN_ON, keepOn)
            .apply()

        touchBlockerView?.let { view ->
            touchBlockerParams?.let { params ->
                if (keepOn) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                } else {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                }
                try {
                    (getSystemService(WINDOW_SERVICE) as WindowManager).updateViewLayout(view, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update layout params for keepScreenOn", e)
                }
            }
        }

        applyDisplayAndBacklightOptimizations()
    }

    /**
     * Floating pastel heart inside the overlay. The overlay swallows every touch, so an unlock
     * control has to live on the overlay itself; long-press (not tap) so stray touches can't trip it.
     */
    private fun createUnlockHeart(): View {
        val density = resources.displayMetrics.density
        val sizePx = (56 * density).toInt()
        val marginPx = (24 * density).toInt()
        return TextView(this).apply {
            text = "♡"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(0xFFE5628A.toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6FFE1EF.toInt())
                setStroke((2 * density).toInt(), 0xFFFFB5D8.toInt())
            }
            alpha = 0.9f
            contentDescription = "Hold to unlock touch for ${QUICK_UNLOCK_MS / 1000} seconds"
            setOnClickListener {
                Toast.makeText(
                    applicationContext,
                    "Hold the heart to unlock for ${QUICK_UNLOCK_MS / 1000}s ♡",
                    Toast.LENGTH_SHORT
                ).show()
            }
            setOnLongClickListener {
                // Post so the overlay isn't removed from inside its own event dispatch
                scope.launch(Dispatchers.Main) { pauseTouchBlockerTemporarily(QUICK_UNLOCK_MS) }
                true
            }
            // Bottom-left: clear of the cracked top-right corner and the fold on this Razr
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(marginPx * 2, marginPx, marginPx, marginPx)
            }
        }
    }

    fun pauseTouchBlockerTemporarily(durationMs: Long) {
        disableTouchBlocker()
        _touchPause.value = TouchPause(SystemClock.elapsedRealtime() + durationMs, durationMs)
        val pretty = if (durationMs < 60_000L) "${durationMs / 1000}s" else "${durationMs / 60_000L} min"
        scope.launch(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                "🔓 RazrGuard: Touch shield paused for $pretty",
                Toast.LENGTH_SHORT
            ).show()
        }
        touchBlockerJob = scope.launch {
            delay(durationMs)
            scope.launch(Dispatchers.Main) {
                enableTouchBlocker()
                Toast.makeText(
                    applicationContext,
                    "🔒 RazrGuard: Touch shield re-engaged",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        instance = null
        _isServiceRunning.value = false
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        scope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister battery receiver", e)
        }
        super.onDestroy()
        disableTouchBlocker()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RazrGuard"
        private const val NOTIFICATION_ID = 101

        const val PREFS_NAME = "razrguard_prefs"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val DEFAULT_SCREEN_TIMEOUT_MS = 300_000 // 5 minutes
        const val QUICK_UNLOCK_MS = 15_000L

        private const val HEALTH_URL = "http://127.0.0.1:8000/"

        private const val CHECK_INTERVAL_MS = 150_000L
        private const val INITIAL_GRACE_MS = 60_000L
        private const val RECOVERY_GRACE_MS = 180_000L
        private const val HTTP_TIMEOUT_MS = 10_000
        private const val FAILURE_THRESHOLD = 3
        private const val SSH_PORT = 8022
        private const val SSH_FAILURE_THRESHOLD = 2
        private const val ADB_TCP_BOOT_DELAY_MS = 8_000L
        private const val SURVIVAL_CHECK_INTERVAL_MS = 3_600_000L

        private const val TERMUX_HOME = "/data/data/com.termux/files/home"
        private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
        private const val TERMUX_SVDIR = "/data/data/com.termux/files/usr/var/service"
        private const val RECOVERY_SCRIPT = "$TERMUX_HOME/.termux/boot/start-services.sh"
        private const val RESTART_SCRIPT = "$TERMUX_HOME/restart-services.sh"

        @Volatile
        var instance: GuardService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isTouchBlockerActive = MutableStateFlow(false)
        val isTouchBlockerActive: StateFlow<Boolean> = _isTouchBlockerActive.asStateFlow()

        private val _touchPause = MutableStateFlow<TouchPause?>(null)
        val touchPause: StateFlow<TouchPause?> = _touchPause.asStateFlow()

        private val _keepScreenOn = MutableStateFlow(false)
        val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

        private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
        val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

        private val _networkIps = MutableStateFlow(NetworkIps())
        val networkIps: StateFlow<NetworkIps> = _networkIps.asStateFlow()

        private val _darkhttpdStatus = MutableStateFlow<Boolean?>(null)
        val darkhttpdStatus: StateFlow<Boolean?> = _darkhttpdStatus.asStateFlow()

        private val _sshStatus = MutableStateFlow<Boolean?>(null)
        val sshStatus: StateFlow<Boolean?> = _sshStatus.asStateFlow()

        private val _jellyfinStatus = MutableStateFlow<Boolean?>(null)
        val jellyfinStatus: StateFlow<Boolean?> = _jellyfinStatus.asStateFlow()

        private val _suwayomiStatus = MutableStateFlow<Boolean?>(null)
        val suwayomiStatus: StateFlow<Boolean?> = _suwayomiStatus.asStateFlow()

        private val _transmissionStatus = MutableStateFlow<Boolean?>(null)
        val transmissionStatus: StateFlow<Boolean?> = _transmissionStatus.asStateFlow()

        private val _vaultwardenStatus = MutableStateFlow<Boolean?>(null)
        val vaultwardenStatus: StateFlow<Boolean?> = _vaultwardenStatus.asStateFlow()

        private val _filebrowserStatus = MutableStateFlow<Boolean?>(null)
        val filebrowserStatus: StateFlow<Boolean?> = _filebrowserStatus.asStateFlow()

        private val _adbTcpStatus = MutableStateFlow<Boolean?>(null)
        val adbTcpStatus: StateFlow<Boolean?> = _adbTcpStatus.asStateFlow()

        private val _shizukuStatus = MutableStateFlow(false)
        val shizukuStatus: StateFlow<Boolean> = _shizukuStatus.asStateFlow()

        fun requestToggleTouchBlocker(enable: Boolean) {
            instance?.let { s ->
                if (enable) s.enableTouchBlocker() else s.disableTouchBlocker()
            }
        }

        fun requestPauseTouchBlocker(durationMs: Long = 120_000L) {
            instance?.pauseTouchBlockerTemporarily(durationMs)
        }

        fun requestToggleKeepScreenOn(keepOn: Boolean) {
            instance?.setKeepScreenOn(keepOn) ?: run {
                _keepScreenOn.value = keepOn
            }
        }

        fun requestRestartServices(context: Context) {
            instance?.restartServerStack() ?: run {
                val intent = Intent(context, GuardService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                val restartIntent = Intent("com.termux.RUN_COMMAND").apply {
                    component = ComponentName("com.termux", "com.termux.app.RunCommandService")
                    putExtra("com.termux.RUN_COMMAND_PATH", TERMUX_BASH)
                    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(RESTART_SCRIPT))
                    putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME)
                    putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                    putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(restartIntent)
                } else {
                    context.startService(restartIntent)
                }
            }
        }

        /** False if the service isn't running (nothing to run the command through). */
        fun requestSyncStorage(): Boolean =
            instance?.let { it.syncStorage(); true } ?: false

        fun requestCheckAdbTcp() {
            instance?.scope?.launch {
                AdbTcpEnabler.enable()
                _adbTcpStatus.value = AdbTcpEnabler.isPortListening()
            }
        }

        fun requestReapplySurvival() {
            instance?.let { s ->
                s.scope.launch {
                    SurvivalLayer.ensure(s.packageName)
                }
            }
        }
    }
}
