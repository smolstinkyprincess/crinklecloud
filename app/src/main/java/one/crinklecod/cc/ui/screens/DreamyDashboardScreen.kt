package one.crinklecod.cc.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import one.crinklecod.cc.SystemLoad
import one.crinklecod.cc.SystemStats
import one.crinklecod.cc.ui.components.SystemLoadBars
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.crinklecod.cc.BatteryInfo
import one.crinklecod.cc.GuardService
import one.crinklecod.cc.NetworkIps
import one.crinklecod.cc.TouchPause
import one.crinklecod.cc.launcher.AppInfo
import one.crinklecod.cc.launcher.FavoritesStore
import one.crinklecod.cc.launcher.fallbackAppInfo
import one.crinklecod.cc.launcher.queryLaunchableApps
import one.crinklecod.cc.ui.animation.breathingGlow
import one.crinklecod.cc.ui.animation.floatingBob
import one.crinklecod.cc.ui.animation.kawaiiBounceClick
import one.crinklecod.cc.ui.components.AddAppDialog
import one.crinklecod.cc.ui.components.CaptionValueChip
import one.crinklecod.cc.ui.components.CloudBatteryMeter
import one.crinklecod.cc.ui.components.CloudMascot
import one.crinklecod.cc.ui.components.DreamySectionHeader
import one.crinklecod.cc.ui.components.DreamySkyBackground
import one.crinklecod.cc.ui.components.FavoriteAppsDeck
import one.crinklecod.cc.ui.components.GlyphKind
import one.crinklecod.cc.ui.components.MascotMood
import one.crinklecod.cc.ui.components.PastelPill
import one.crinklecod.cc.ui.components.PillowCard
import one.crinklecod.cc.ui.components.ServiceConstellation
import one.crinklecod.cc.ui.components.ServiceStatus
import one.crinklecod.cc.ui.components.ShieldMode
import one.crinklecod.cc.ui.components.cloudPath
import one.crinklecod.cc.ui.components.pillowShadow
import one.crinklecod.cc.ui.components.rememberPauseRemaining
import one.crinklecod.cc.ui.components.shieldModeOf
import one.crinklecod.cc.ui.theme.CloudWhite
import one.crinklecod.cc.ui.theme.CottonCandyPink
import one.crinklecod.cc.ui.theme.HappyMint
import one.crinklecod.cc.ui.theme.LavenderHaze
import one.crinklecod.cc.ui.theme.MintSparkle
import one.crinklecod.cc.ui.theme.OutlineButter
import one.crinklecod.cc.ui.theme.OutlineLilac
import one.crinklecod.cc.ui.theme.OutlineMint
import one.crinklecod.cc.ui.theme.OutlinePeach
import one.crinklecod.cc.ui.theme.OutlinePink
import one.crinklecod.cc.ui.theme.OutlineSky
import one.crinklecod.cc.ui.theme.PillowFill
import one.crinklecod.cc.ui.theme.PillowFillButter
import one.crinklecod.cc.ui.theme.PillowFillLilac
import one.crinklecod.cc.ui.theme.PillowFillMint
import one.crinklecod.cc.ui.theme.PillowFillPeach
import one.crinklecod.cc.ui.theme.PillowFillPink
import one.crinklecod.cc.ui.theme.PillowFillSky
import one.crinklecod.cc.ui.theme.PillowFillStrong
import one.crinklecod.cc.ui.theme.PlumInk
import one.crinklecod.cc.ui.theme.RazrGuardTheme
import one.crinklecod.cc.ui.theme.SleepyRose

private const val KEY_SCREEN_DIMMED = "screen_dimmed"
private const val KEY_CALIBRATING = "dead_zone_calibration"
private const val DIMMED_BRIGHTNESS = 0.02f
private const val SYSTEM_LOAD_POLL_MS = 5_000L
private const val CONSTELLATION_SCROLL_DP_PER_S = 12
private const val CONSTELLATION_REST_MS = 3_500L

@Immutable
data class DashboardUiState(
    val isGuardRunning: Boolean = false,
    val battery: BatteryInfo? = null,
    val networkIps: NetworkIps = NetworkIps(),
    val services: List<ServiceStatus> = emptyList(),
    val isCalibrating: Boolean = false,
    val isShieldActive: Boolean = false,
    val touchPause: TouchPause? = null,
    val keepScreenOn: Boolean = false,
    val isScreenDimmed: Boolean = false,
    val favoriteApps: List<AppInfo> = emptyList(),
    val systemLoad: SystemLoad? = null,
)

/** Every user intent the dashboard can raise; defaults are no-ops so previews stay terse. */
class DashboardActions(
    val onStartGuard: () -> Unit = {},
    val onCopyIp: (label: String, ip: String) -> Unit = { _, _ -> },
    val onToggleShield: () -> Unit = {},
    val onRestartStack: () -> Unit = {},
    val onRebindAdb: () -> Unit = {},
    val onSyncStorage: () -> Unit = {},
    val onToggleDim: () -> Unit = {},
    val onToggleKeepScreenOn: () -> Unit = {},
    val onReapplySurvival: () -> Unit = {},
    val onRequestPerms: () -> Unit = {},
    val onToggleCalibration: () -> Unit = {},
    val onOpenApp: (AppInfo) -> Unit = {},
    val onRemoveApp: (AppInfo) -> Unit = {},
    val onAddApp: () -> Unit = {},
)

/**
 * Stateful entry point: collects [GuardService] state, owns favorites and window flags,
 * and turns taps into service requests.
 */
@Composable
fun DreamyDashboardScreen(
    onStartService: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(GuardService.PREFS_NAME, Context.MODE_PRIVATE) }

    val isRunning by GuardService.isServiceRunning.collectAsState()
    val isTouchActive by GuardService.isTouchBlockerActive.collectAsState()
    val touchPause by GuardService.touchPause.collectAsState()
    val keepScreenOn by GuardService.keepScreenOn.collectAsState()
    val batteryInfo by GuardService.batteryInfo.collectAsState()
    val networkIps by GuardService.networkIps.collectAsState()
    val darkhttpdStatus by GuardService.darkhttpdStatus.collectAsState()
    val sshStatus by GuardService.sshStatus.collectAsState()
    val shizukuStatus by GuardService.shizukuStatus.collectAsState()
    val jellyfinStatus by GuardService.jellyfinStatus.collectAsState()
    val suwayomiStatus by GuardService.suwayomiStatus.collectAsState()
    val transmissionStatus by GuardService.transmissionStatus.collectAsState()
    // Polled only while the dashboard is on screen; cleared after a few misses so a dead
    // stats feed shows "listening…" instead of frozen numbers
    val systemLoad by produceState<SystemLoad?>(initialValue = null) {
        var misses = 0
        while (true) {
            val fresh = withContext(Dispatchers.IO) { SystemStats.fetch() }
            if (fresh != null) { value = fresh; misses = 0 } else if (++misses >= 3) value = null
            delay(SYSTEM_LOAD_POLL_MS)
        }
    }
    val vaultwardenStatus by GuardService.vaultwardenStatus.collectAsState()
    val fileShareStatus by GuardService.fileShareStatus.collectAsState()

    var isDimmed by remember { mutableStateOf(prefs.getBoolean(KEY_SCREEN_DIMMED, false)) }
    var isCalibrating by remember { mutableStateOf(prefs.getBoolean(KEY_CALIBRATING, false)) }

    LaunchedEffect(keepScreenOn) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    LaunchedEffect(isDimmed) {
        activity?.window?.let { window ->
            window.attributes = window.attributes.apply {
                screenBrightness = if (isDimmed) DIMMED_BRIGHTNESS
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Favorites & installed apps
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var appsLoading by remember { mutableStateOf(true) }
    var favoritePackages by remember { mutableStateOf(FavoritesStore.load(context)) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Load at start, and refresh whenever the picker opens so new installs show up
    LaunchedEffect(showAddDialog) {
        if (allApps.isEmpty() || showAddDialog) {
            allApps = withContext(Dispatchers.IO) { queryLaunchableApps(context) }
            appsLoading = false
        }
    }
    val favoriteApps = remember(favoritePackages, allApps) {
        val byPackage = allApps.associateBy { it.packageName }
        favoritePackages
            .map { byPackage[it] ?: fallbackAppInfo(it) }
            .sortedBy { it.label.lowercase() }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun whenGuardAwake(action: () -> Unit) {
        if (isRunning) action() else toast("Wake the guard first ☁️ (tap the Guard chip)")
    }

    fun engageShield() {
        if (!Settings.canDrawOverlays(context)) onRequestOverlayPermission()
        else whenGuardAwake { GuardService.requestToggleTouchBlocker(true) }
    }

    if (showAddDialog) {
        AddAppDialog(
            allApps = allApps,
            currentFavorites = favoritePackages,
            isLoading = appsLoading,
            onDismiss = { showAddDialog = false },
            onSelectApp = { app ->
                favoritePackages = favoritePackages + app.packageName
                FavoritesStore.save(context, favoritePackages)
                showAddDialog = false
                toast("Pinned ${app.label} 🌸")
            }
        )
    }

    val actions = DashboardActions(
        onStartGuard = {
            onStartService()
            if (!isRunning) toast("Waking the guard… ☁️")
        },
        onCopyIp = { label, ip ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("$label IP", ip))
            toast("Copied $label IP $ip 📋")
        },
        onToggleShield = {
            if (shieldModeOf(isTouchActive, touchPause) == ShieldMode.Off) engageShield()
            else GuardService.requestToggleTouchBlocker(false)
        },
        onRestartStack = {
            GuardService.requestRestartServices(context)
            toast("Restarting the server stack… 🔄")
        },
        onRebindAdb = {
            whenGuardAwake {
                GuardService.requestCheckAdbTcp()
                toast("Re-binding ADB on :5555… 📡")
            }
        },
        onSyncStorage = {
            if (GuardService.requestSyncStorage()) toast("Tucking data safely onto disk… ☁️")
            else toast("Wake the guard first ☁️ (tap the Guard chip)")
        },
        onToggleDim = {
            isDimmed = !isDimmed
            prefs.edit().putBoolean(KEY_SCREEN_DIMMED, isDimmed).apply()
            toast(if (isDimmed) "Dimmed the dashboard 🌙" else "Brightened the dashboard 💡")
        },
        onToggleKeepScreenOn = {
            val target = !keepScreenOn
            GuardService.requestToggleKeepScreenOn(target)
            toast(if (target) "Display: always on ☀️" else "Display: sleeps after 5m 🌙")
        },
        onReapplySurvival = {
            whenGuardAwake {
                GuardService.requestReapplySurvival()
                toast("Re-checking survival & Doze settings 🛡️")
            }
        },
        onRequestPerms = {
            onRequestShizuku()
            onRequestOverlayPermission()
        },
        onToggleCalibration = {
            isCalibrating = !isCalibrating
            prefs.edit().putBoolean(KEY_CALIBRATING, isCalibrating).apply()
        },
        onOpenApp = { app ->
            val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
            try {
                if (launch != null) context.startActivity(launch) else toast("Can't open ${app.label} 🥺")
            } catch (e: ActivityNotFoundException) {
                toast("Can't open ${app.label} 🥺")
            }
        },
        onRemoveApp = { app ->
            favoritePackages = favoritePackages - app.packageName
            FavoritesStore.save(context, favoritePackages)
            toast("Unpinned ${app.label} 🌸")
        },
        onAddApp = { showAddDialog = true }
    )

    val state = DashboardUiState(
        isGuardRunning = isRunning,
        battery = batteryInfo,
        networkIps = networkIps,
        services = listOf(
            ServiceStatus("darkhttpd", "web dashboard · :8000", darkhttpdStatus),
            ServiceStatus("sshd", "secure shell · :8022", sshStatus),
            ServiceStatus("Tailscale", networkIps.tailscaleIp ?: "tailnet", networkIps.tailscaleIp != null, GlyphKind.Heart),
            ServiceStatus("Shizuku", "privileged bridge", shizukuStatus, GlyphKind.Heart),
            ServiceStatus("Jellyfin", "media · :8096", jellyfinStatus),
            ServiceStatus("Suwayomi", "manga · :4567", suwayomiStatus),
            ServiceStatus("Transmission", "torrents · :9091", transmissionStatus),
            ServiceStatus("Vaultwarden", "passwords · :8443", vaultwardenStatus),
            ServiceStatus("copyparty", "files · :8080", fileShareStatus),
        ),
        isCalibrating = isCalibrating,
        isShieldActive = isTouchActive,
        touchPause = touchPause,
        keepScreenOn = keepScreenOn,
        isScreenDimmed = isDimmed,
        favoriteApps = favoriteApps,
        systemLoad = systemLoad,
    )

    DreamyDashboardContent(state = state, actions = actions)
}

/**
 * Landscape layout shaped around the Razr's dead zones (see [DeadZones]): content starts below the
 * top strip, the two halves leave the fold empty so only sky shows through, and the far-right
 * column starts below the dead corner.
 */
@Composable
fun DreamyDashboardContent(state: DashboardUiState, actions: DashboardActions) {
    DreamySkyBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp, start = 14.dp, end = 14.dp)
        ) {
            // ── Left half: status pill, health, mascot & touch guard
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                    .padding(top = DeadZones.TopStrip + 6.dp)
            ) {
                HaloPill(state = state, actions = actions)
                Spacer(Modifier.height(10.dp))
                // The sanctuary (mascot + health) column overhangs into the spare sky beside
                // the fold by DeadZones.SanctuaryOverhang; the constellation keeps its width.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val gap = 10.dp
                    val constellationWidth = (maxWidth - gap) / 2
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(Alignment.Start, unbounded = true)
                            .requiredWidth(maxWidth + DeadZones.SanctuaryOverhang),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        ConstellationColumn(state, Modifier.width(constellationWidth).fillMaxHeight())
                        SanctuaryColumn(state, actions, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }

            // ── The fold: nothing but sky
            Spacer(Modifier.width(DeadZones.FoldWidth))

            // ── Right half: full-height favorites beside the fold; quick magic in the corner column,
            // scrolling right up to the screen edge but starting below the dead corner
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FavoriteAppsDeck(
                    apps = state.favoriteApps,
                    onOpenApp = actions.onOpenApp,
                    onRemoveApp = actions.onRemoveApp,
                    onAddApp = actions.onAddApp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = DeadZones.TopStrip + 6.dp, bottom = 4.dp)
                )
                QuickMagicColumn(state, actions, Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (state.isCalibrating) DeadZoneCalibrationOverlay()
    }
}

// ─── Halo pills ──────────────────────────────────────────────────────────────

@Composable
private fun HaloPill(state: DashboardUiState, actions: DashboardActions) {
    val shape = CircleShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .floatingBob(amplitude = 2.dp, periodMillis = 5200)
            .pillowShadow(cornerRadius = 40.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(PillowFillStrong, Color(0xCCFFF0F8), PillowFillStrong)))
            .border(2.dp, OutlinePink, shape)
            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(Modifier.size(width = 30.dp, height = 20.dp)) {
            val cloud = cloudPath(Size(size.width, size.height))
            drawPath(cloud, brush = Brush.verticalGradient(listOf(CloudWhite, LavenderHaze)))
            drawPath(cloud, color = CottonCandyPink, style = Stroke(1.8.dp.toPx(), join = StrokeJoin.Round))
        }
        Column(Modifier.weight(1f)) {
            Text("CrinkleCloud ✨", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("dreamy little server", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        CaptionValueChip(
            caption = "Guard",
            value = if (state.isGuardRunning) "watching" else "napping",
            fill = if (state.isGuardRunning) PillowFillMint else PillowFillPeach,
            outline = if (state.isGuardRunning) OutlineMint else OutlinePeach,
            valueColor = if (state.isGuardRunning) HappyMint else SleepyRose,
            leading = { StatusDot(on = state.isGuardRunning) },
            onClick = actions.onStartGuard
        )
        ShieldStatusChip(
            isActive = state.isShieldActive,
            pause = state.touchPause,
            onClick = actions.onToggleShield
        )
    }
}

@Composable
private fun NetworkPill(state: DashboardUiState, actions: DashboardActions) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .floatingBob(amplitude = 2.dp, periodMillis = 5200, phaseMillis = 800)
            .pillowShadow(cornerRadius = 28.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PillowFillStrong, Color(0x99E9F3FF))))
            .border(2.dp, OutlineSky, shape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val lan = state.networkIps.lanIp
        CaptionValueChip(
            caption = "🏠 LAN · tap to copy",
            value = lan ?: "offline",
            fill = PillowFillSky,
            outline = OutlineSky,
            valueColor = if (lan != null) PlumInk else SleepyRose,
            modifier = Modifier.fillMaxWidth(),
            onClick = lan?.let { ip -> { actions.onCopyIp("LAN", ip) } }
        )
        val tailscale = state.networkIps.tailscaleIp
        CaptionValueChip(
            caption = "🔐 Tailscale · tap to copy",
            value = tailscale ?: "offline",
            fill = PillowFillLilac,
            outline = OutlineLilac,
            valueColor = if (tailscale != null) PlumInk else SleepyRose,
            modifier = Modifier.fillMaxWidth(),
            onClick = tailscale?.let { ip -> { actions.onCopyIp("Tailscale", ip) } }
        )
    }
}

@Composable
private fun StatusDot(on: Boolean) {
    Box(
        Modifier
            .size(9.dp)
            .breathingGlow(color = MintSparkle, glowRadius = 5.dp, enabled = on)
            .clip(CircleShape)
            .background(if (on) HappyMint else SleepyRose)
    )
}

@Composable
private fun ShieldStatusChip(isActive: Boolean, pause: TouchPause?, onClick: () -> Unit) {
    val mode = shieldModeOf(isActive, pause)
    val remaining = rememberPauseRemaining(pause)
    val secondsLeft by remember { derivedStateOf { ((remaining.value + 999) / 1000).toInt() } }
    CaptionValueChip(
        caption = "Touch",
        value = when (mode) {
            ShieldMode.Active -> "🔒 on"
            ShieldMode.Paused -> "⏳ ${secondsLeft}s"
            ShieldMode.Off -> "🔓 off"
        },
        fill = when (mode) {
            ShieldMode.Active -> PillowFillPink
            ShieldMode.Paused -> PillowFillButter
            ShieldMode.Off -> PillowFill
        },
        outline = when (mode) {
            ShieldMode.Active -> OutlinePink
            ShieldMode.Paused -> OutlineButter
            ShieldMode.Off -> OutlineLilac
        },
        onClick = onClick
    )
}

// ─── Left half ────────────────────────────────────────────────────────────────

@Composable
private fun ConstellationColumn(state: DashboardUiState, modifier: Modifier) {
    val scroll = rememberScrollState()
    val pxPerSecond = with(LocalDensity.current) { CONSTELLATION_SCROLL_DP_PER_S.dp.toPx() }
    // Slow ping-pong drift through the list: rest, glide to the bottom, rest, glide back.
    // A finger drag cancels the glide; it simply resumes after the next rest.
    LaunchedEffect(scroll, pxPerSecond) {
        while (true) {
            delay(CONSTELLATION_REST_MS)
            if (scroll.maxValue == 0 || scroll.isScrollInProgress) continue
            val target = if (scroll.value < scroll.maxValue) scroll.maxValue else 0
            val millis = (abs(target - scroll.value) / pxPerSecond * 1000).toInt().coerceAtLeast(600)
            try {
                scroll.animateScrollTo(target, tween(durationMillis = millis, easing = LinearEasing))
            } catch (e: CancellationException) {
                if (!isActive) throw e // real cancellation; otherwise a user drag interrupted us
            }
        }
    }
    Column(modifier = modifier.verticalScroll(scroll)) {
        ServiceConstellation(services = state.services)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SanctuaryColumn(state: DashboardUiState, actions: DashboardActions, modifier: Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MascotCard(state = state, onClick = actions.onStartGuard)
        PillowCard(tint = Color(0x99E4F6FF), outline = OutlineSky, contentPadding = PaddingValues(12.dp)) {
            DreamySectionHeader(emoji = "🌙", title = "Celestial Health")
            Spacer(Modifier.height(6.dp))
            // Battery cloud on the left, CPU/RAM bars beside it (uses the overhang width
            // instead of making the card taller)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CloudBatteryMeter(battery = state.battery, modifier = Modifier.weight(1.3f))
                SystemLoadBars(load = state.systemLoad, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ─── Right half ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickMagicColumn(state: DashboardUiState, actions: DashboardActions, modifier: Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(DeadZones.CornerRadius - 10.dp))
        NetworkPill(state = state, actions = actions)
        PillowCard(tint = Color(0x99F1E8FF), outline = OutlineLilac, contentPadding = PaddingValues(12.dp)) {
            DreamySectionHeader(emoji = "🪄", title = "Quick Magic")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionPill(
                    emoji = "🔄", title = "Restart Stack", subtitle = "fluff the servers",
                    fill = PillowFillPink, outline = OutlinePink,
                    onClick = actions.onRestartStack, modifier = Modifier.fillMaxWidth()
                )
                QuickActionPill(
                    emoji = "📡", title = "Rebind ADB", subtitle = ":5555",
                    fill = PillowFillSky, outline = OutlineSky,
                    onClick = actions.onRebindAdb, modifier = Modifier.fillMaxWidth()
                )
                QuickActionPill(
                    emoji = "☁️", title = "Sync Storage", subtitle = "flush to disk",
                    fill = PillowFillMint, outline = OutlineMint,
                    onClick = actions.onSyncStorage, modifier = Modifier.fillMaxWidth()
                )
                QuickActionPill(
                    emoji = "💡",
                    title = if (state.isScreenDimmed) "Brighten" else "Dim Screen",
                    subtitle = if (state.isScreenDimmed) "dimmed 🌙" else "normal glow",
                    fill = PillowFillButter, outline = OutlineButter,
                    onClick = actions.onToggleDim, modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PastelPill(
                    text = if (state.keepScreenOn) "Always on" else "5m sleep",
                    emoji = if (state.keepScreenOn) "☀️" else "🌙",
                    fill = PillowFill, outline = OutlineButter,
                    onClick = actions.onToggleKeepScreenOn
                )
                PastelPill(
                    text = "Survival", emoji = "🛡️",
                    fill = PillowFill, outline = OutlineMint,
                    onClick = actions.onReapplySurvival
                )
                PastelPill(
                    text = "Perms", emoji = "🔑",
                    fill = PillowFill, outline = OutlineLilac,
                    onClick = actions.onRequestPerms
                )
                PastelPill(
                    text = if (state.isCalibrating) "Hide zones" else "Calibrate",
                    emoji = "📏",
                    fill = if (state.isCalibrating) PillowFillPink else PillowFill,
                    outline = OutlinePink,
                    onClick = actions.onToggleCalibration
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun QuickActionPill(
    emoji: String,
    title: String,
    subtitle: String,
    fill: Color,
    outline: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .kawaiiBounceClick(onClick = onClick)
            .pillowShadow(cornerRadius = 26.dp, offsetY = 3.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PillowFillStrong, fill)))
            .border(2.dp, outline, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CloudWhite)
                .border(1.5.dp, outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MascotCard(state: DashboardUiState, onClick: () -> Unit) {
    val (mood, headline, subline) = cozyStatus(state)
    PillowCard(
        tint = Color(0x99FFE6F2),
        outline = OutlinePink,
        modifier = Modifier.fillMaxWidth().kawaiiBounceClick(pressedScale = 0.96f, onClick = onClick),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        CloudMascot(
            mood = mood,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 96.dp, height = 62.dp)
                .floatingBob(amplitude = 5.dp, periodMillis = 3400)
        )
        Spacer(Modifier.height(6.dp))
        Text(headline, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(subline, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/** Mood, headline and subline for the mascot, worst problem first. */
internal fun cozyStatus(state: DashboardUiState): Triple<MascotMood, String, String> {
    val known = state.services.filter { it.online != null }
    val down = known.filter { it.online == false }
    val shining = known.size - down.size
    val tally = "$shining/${state.services.size} stars shining"
    return when {
        !state.isGuardRunning ->
            Triple(MascotMood.Sleepy, "Guard is napping… 💤", "Tap me to wake the watchdog")
        state.battery?.isOverheating == true ->
            Triple(MascotMood.Toasty, "Feeling a bit toasty… 🥵", "Tent the phone so it can cool off")
        down.size == 1 ->
            Triple(MascotMood.Worried, "${down.first().name} needs a hug 🩹", "$tally · the watchdog is on it")
        down.size > 1 ->
            Triple(MascotMood.Worried, "${down.size} services are sleepy 🩹", "$tally · try Restart Stack")
        known.isEmpty() ->
            Triple(MascotMood.Happy, "Waking up gently… ☁️", "First health check is on its way")
        else ->
            Triple(MascotMood.Happy, "All systems cozy… ✨", tally)
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Preview(name = "Dreamy dashboard", device = "spec:width=1023dp,height=418dp,dpi=413")
@Composable
private fun DreamyDashboardPreview() {
    RazrGuardTheme {
        DreamyDashboardContent(
            state = DashboardUiState(
                isGuardRunning = true,
                battery = BatteryInfo(levelPercent = 76, tempCelsius = 31.5f, isPlugged = true, isOverheating = false),
                networkIps = NetworkIps(lanIp = "192.168.1.42", tailscaleIp = "100.88.12.7"),
                services = listOf(
                    ServiceStatus("darkhttpd", "web dashboard · :8000", true),
                    ServiceStatus("sshd", "secure shell · :8022", true),
                    ServiceStatus("Tailscale", "100.88.12.7", true, GlyphKind.Heart),
                    ServiceStatus("Shizuku", "privileged bridge", true, GlyphKind.Heart),
                    ServiceStatus("Jellyfin", "media · :8096", null),
                ),
                isShieldActive = true,
                favoriteApps = listOf(
                    AppInfo("Termux", "com.termux"),
                    AppInfo("Tailscale", "com.tailscale.ipn"),
                    AppInfo("Shizuku", "moe.shizuku.privileged.api"),
                    AppInfo("Settings", "com.android.settings"),
                    AppInfo("Jellyfin", "org.jellyfin.mobile"),
                ),
                systemLoad = SystemLoad(cpuPercent = 42, ramUsedMb = 5175, ramTotalMb = 7367),
            ),
            actions = DashboardActions()
        )
    }
}
