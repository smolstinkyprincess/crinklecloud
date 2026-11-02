package one.crinklecod.razrguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.crinklecod.razrguard.ui.theme.BabySkyBlue
import one.crinklecod.razrguard.ui.theme.BorderLilacGlow
import one.crinklecod.razrguard.ui.theme.BorderPinkGlow
import one.crinklecod.razrguard.ui.theme.CottonCandyPink
import one.crinklecod.razrguard.ui.theme.FairyLilac
import one.crinklecod.razrguard.ui.theme.LilacChipBorder
import one.crinklecod.razrguard.ui.theme.LilacChipFill
import one.crinklecod.razrguard.ui.theme.MintChipBorder
import one.crinklecod.razrguard.ui.theme.MintChipFill
import one.crinklecod.razrguard.ui.theme.PastelButtercup
import one.crinklecod.razrguard.ui.theme.PastelPeach
import one.crinklecod.razrguard.ui.theme.PastelPlumCard
import one.crinklecod.razrguard.ui.theme.PastelPlumElevated
import one.crinklecod.razrguard.ui.theme.PastelPlumLight
import one.crinklecod.razrguard.ui.theme.PeachChipBorder
import one.crinklecod.razrguard.ui.theme.PeachChipFill
import one.crinklecod.razrguard.ui.theme.PlumOledCanvas
import one.crinklecod.razrguard.ui.theme.RazrGuardTheme
import one.crinklecod.razrguard.ui.theme.SkyBlueChipBorder
import one.crinklecod.razrguard.ui.theme.SkyBlueChipFill
import one.crinklecod.razrguard.ui.theme.SoftMatchaMint
import one.crinklecod.razrguard.ui.theme.StrawberryMilk
import one.crinklecod.razrguard.ui.theme.WhippedCream
import rikka.shizuku.Shizuku

data class AppInfo(
    val label: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startGuardServiceIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            RazrGuardTheme {
                LauncherDashboardScreen(
                    onStartService = { startGuardServiceIfNeeded() },
                    onRequestShizuku = { requestShizukuPermission() },
                    onRequestOverlayPermission = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun startGuardServiceIfNeeded() {
        val intent = Intent(this, GuardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Shizuku permission already granted ✨", Toast.LENGTH_SHORT).show()
                } else {
                    Shizuku.requestPermission(1001)
                }
            } else {
                Toast.makeText(this, "Shizuku service is not running", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to request Shizuku: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Overlay permission already granted ✨", Toast.LENGTH_SHORT).show()
        }
    }
}

private const val PREFS_NAME = "razrguard_prefs"
private const val KEY_FAVORITES = "favorite_packages"
private val DEFAULT_FAVORITES = setOf(
    "com.termux",
    "com.tailscale.ipn",
    "moe.shizuku.privileged.api",
    "com.android.settings"
)

private fun loadFavorites(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_FAVORITES, null) ?: DEFAULT_FAVORITES
}

private fun saveFavorites(context: Context, favorites: Set<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
}

@Composable
fun LauncherDashboardScreen(
    onStartService: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isRunning by GuardService.isServiceRunning.collectAsState()
    val isTouchActive by GuardService.isTouchBlockerActive.collectAsState()
    val keepScreenOn by GuardService.keepScreenOn.collectAsState()
    val batteryInfo by GuardService.batteryInfo.collectAsState()
    val networkIps by GuardService.networkIps.collectAsState()
    val darkhttpdStatus by GuardService.darkhttpdStatus.collectAsState()
    val sshStatus by GuardService.sshStatus.collectAsState()
    val adbTcpStatus by GuardService.adbTcpStatus.collectAsState()
    val shizukuStatus by GuardService.shizukuStatus.collectAsState()
    val jellyfinStatus by GuardService.jellyfinStatus.collectAsState()
    val suwayomiStatus by GuardService.suwayomiStatus.collectAsState()
    val transmissionStatus by GuardService.transmissionStatus.collectAsState()

    val activity = context as? ComponentActivity
    LaunchedEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var allInstalledApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var favoritePackages by remember { mutableStateOf(loadFavorites(context)) }
    var showAddAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved = pm.queryIntentActivities(mainIntent, 0)

            allInstalledApps = resolved
                .mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm).toString()
                    AppInfo(label = label, packageName = pkg)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }
    }

    val appMap = remember(allInstalledApps) {
        allInstalledApps.associateBy { it.packageName }
    }

    val favoriteApps = remember(favoritePackages, appMap) {
        favoritePackages.mapNotNull { pkg ->
            appMap[pkg] ?: AppInfo(
                label = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                packageName = pkg
            )
        }.sortedBy { it.label.lowercase() }
    }

    if (showAddAppDialog) {
        AddAppDialog(
            allApps = allInstalledApps,
            currentFavorites = favoritePackages,
            onDismiss = { showAddAppDialog = false },
            onSelectApp = { app ->
                val updated = favoritePackages + app.packageName
                favoritePackages = updated
                saveFavorites(context, updated)
                showAddAppDialog = false
                Toast.makeText(context, "Added ${app.label} to favorites 🌸", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        containerColor = PlumOledCanvas,
        bottomBar = {
            QuickActionsBar(
                isTouchActive = isTouchActive,
                keepScreenOn = keepScreenOn,
                onRestartStack = {
                    GuardService.requestRestartServices(context)
                    Toast.makeText(context, "Restarting server stack (~/restart-services.sh)...", Toast.LENGTH_SHORT).show()
                },
                onToggleTouchShield = {
                    val target = !isTouchActive
                    if (target && !Settings.canDrawOverlays(context)) {
                        onRequestOverlayPermission()
                    } else {
                        GuardService.requestToggleTouchBlocker(target)
                    }
                },
                onToggleKeepScreenOn = {
                    val target = !keepScreenOn
                    GuardService.requestToggleKeepScreenOn(target)
                    Toast.makeText(
                        context,
                        if (target) "Display set to: Always On ☀️" else "Display set to: 5m Sleep Timeout 🌙",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onRebindAdb = {
                    coroutineScope.launch {
                        GuardService.requestCheckAdbTcp()
                        Toast.makeText(context, "Re-binding ADB TCP :5555...", Toast.LENGTH_SHORT).show()
                    }
                },
                onReapplySurvival = {
                    GuardService.requestReapplySurvival()
                    Toast.makeText(context, "Re-applied survival & Doze settings ✨", Toast.LENGTH_SHORT).show()
                },
                onRequestPerms = {
                    onRequestShizuku()
                    onRequestOverlayPermission()
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeaderSection(isRunning = isRunning, onStartService = onStartService)
            }

            item {
                DashboardCard(
                    darkhttpdStatus = darkhttpdStatus,
                    sshStatus = sshStatus,
                    adbTcpStatus = adbTcpStatus,
                    shizukuStatus = shizukuStatus,
                    isTouchActive = isTouchActive,
                    keepScreenOn = keepScreenOn,
                    isRunning = isRunning,
                    batteryInfo = batteryInfo,
                    networkIps = networkIps,
                    jellyfinStatus = jellyfinStatus,
                    suwayomiStatus = suwayomiStatus,
                    transmissionStatus = transmissionStatus
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎀 ✨ FAVORITE APPS (${favoriteApps.size}) ✨ 🎀",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FairyLilac,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(PastelPlumLight)
                            .border(BorderStroke(1.dp, BorderPinkGlow), RoundedCornerShape(14.dp))
                            .clickable { showAddAppDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ ADD APP ♡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CottonCandyPink
                        )
                    }
                }
            }

            if (favoriteApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddAppDialog = true }
                            .padding(vertical = 10.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, BorderPinkGlow),
                        colors = CardDefaults.cardColors(containerColor = PastelPlumCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No favorites yet 🌸",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhippedCream
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap here or the + button to choose apps to pin",
                                fontSize = 13.sp,
                                color = FairyLilac
                            )
                        }
                    }
                }
            } else {
                items(favoriteApps, key = { it.packageName }) { app ->
                    FavoriteAppListItem(
                        app = app,
                        onOpen = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRemove = {
                            val updated = favoritePackages - app.packageName
                            favoritePackages = updated
                            saveFavorites(context, updated)
                            Toast.makeText(context, "Removed ${app.label} from favorites 🌸", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AddAppDialog(
    allApps: List<AppInfo>,
    currentFavorites: Set<String>,
    onDismiss: () -> Unit,
    onSelectApp: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val selectableApps = remember(allApps, currentFavorites, searchQuery) {
        val nonFavorites = allApps.filter { it.packageName !in currentFavorites }
        if (searchQuery.isBlank()) nonFavorites else nonFavorites.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(26.dp),
            color = PastelPlumCard,
            border = BorderStroke(1.5.dp, BorderPinkGlow),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Favorites 🌸",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhippedCream
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = CottonCandyPink, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter apps...", color = FairyLilac.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CottonCandyPink,
                        unfocusedBorderColor = BorderLilacGlow,
                        focusedTextColor = WhippedCream,
                        unfocusedTextColor = WhippedCream,
                        focusedContainerColor = PastelPlumElevated,
                        unfocusedContainerColor = PastelPlumElevated
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectableApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allApps.isEmpty()) "Loading apps..." else "No matching apps found ☁️",
                            color = FairyLilac,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectableApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PastelPlumElevated)
                                    .border(BorderStroke(1.dp, BorderPinkGlow.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
                                    .clickable { onSelectApp(app) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WhippedCream
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 11.sp,
                                        color = FairyLilac.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "＋",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CottonCandyPink
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(isRunning: Boolean, onStartService: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "RazrGuard 🌸",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WhippedCream
            )
            Text(
                text = "☁️ cozy headless server node ☁️",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FairyLilac
            )
        }

        val chipBg = if (isRunning) MintChipFill else PeachChipFill
        val chipBorder = if (isRunning) MintChipBorder else PeachChipBorder
        val dotColor = if (isRunning) SoftMatchaMint else StrawberryMilk
        val textColor = if (isRunning) SoftMatchaMint else StrawberryMilk
        val statusText = if (isRunning) "✨ GUARD ACTIVE ✨" else "⚠️ RESTART GUARD"

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(chipBg)
                .border(BorderStroke(1.dp, chipBorder), CircleShape)
                .clickable { onStartService() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardCard(
    darkhttpdStatus: Boolean?,
    sshStatus: Boolean?,
    adbTcpStatus: Boolean?,
    shizukuStatus: Boolean,
    isTouchActive: Boolean,
    keepScreenOn: Boolean,
    isRunning: Boolean,
    batteryInfo: BatteryInfo?,
    networkIps: NetworkIps,
    jellyfinStatus: Boolean?,
    suwayomiStatus: Boolean?,
    transmissionStatus: Boolean?
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, BorderPinkGlow),
        colors = CardDefaults.cardColors(containerColor = PastelPlumCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌸 ✨ SYSTEM HEALTH ✨ 🌸",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairyLilac,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (isRunning) "☕ CPU WAKELOCK ON" else "💤 OFFLINE",
                    fontSize = 11.sp,
                    color = if (isRunning) SoftMatchaMint else StrawberryMilk,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Network IP Badges (Tap to copy)
                StatusPillBadge(
                    label = "🌐 LAN",
                    status = networkIps.lanIp ?: "Offline",
                    isOnline = networkIps.lanIp != null,
                    onClick = networkIps.lanIp?.let { ip ->
                        {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("LAN IP", ip))
                            Toast.makeText(context, "Copied LAN IP: $ip 📋", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                StatusPillBadge(
                    label = "🔒 Tailscale",
                    status = networkIps.tailscaleIp ?: "Offline",
                    isOnline = networkIps.tailscaleIp != null,
                    onClick = networkIps.tailscaleIp?.let { ip ->
                        {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("Tailscale IP", ip))
                            Toast.makeText(context, "Copied Tailscale IP: $ip 📋", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                StatusPillBadge(
                    label = "Web (:8000)",
                    status = if (darkhttpdStatus == true) "" else "Down",
                    isOnline = darkhttpdStatus
                )
                StatusPillBadge(
                    label = "SSH (:8022)",
                    status = if (sshStatus == true) "" else "Down",
                    isOnline = sshStatus
                )
                StatusPillBadge(
                    label = "Jellyfin (:8096)",
                    status = if (jellyfinStatus == true) "" else "Down",
                    isOnline = jellyfinStatus
                )
                StatusPillBadge(
                    label = "Suwayomi (:4567)",
                    status = if (suwayomiStatus == true) "" else "Down",
                    isOnline = suwayomiStatus
                )
                StatusPillBadge(
                    label = "Torrents (:9091)",
                    status = if (transmissionStatus == true) "" else "Down",
                    isOnline = transmissionStatus
                )/*
                StatusPillBadge(
                    label = "ADB TCP (:5555)",
                    status = if (adbTcpStatus == true) "" else "Offline",
                    isOnline = adbTcpStatus
                )*/
                StatusPillBadge(
                    label = "Shizuku",
                    status = if (shizukuStatus) "" else "Unavailable",
                    isOnline = shizukuStatus
                )
                batteryInfo?.let { b ->
                    val isCool = !b.isOverheating
                    val label = if (b.isPlugged) "⚡ Battery" else "🔋 Battery"
                    val tempFormatted = "%.1f".format(b.tempCelsius)
                    val statusText = if (b.isOverheating) "${b.levelPercent}% • ${tempFormatted}°C 🔥" else "${b.levelPercent}% • ${tempFormatted}°C"
                    StatusPillBadge(
                        label = label,
                        status = statusText,
                        isOnline = isCool
                    )
                } ?: StatusPillBadge(
                    label = "🔋 Battery",
                    status = "Monitoring...",
                    isOnline = null
                )
            }
        }
    }
}

/**
 * Reusable pill-style badge with a clean status dot indicator.
 */
@Composable
fun StatusPillBadge(
    label: String,
    status: String,
    isOnline: Boolean?,
    onClick: (() -> Unit)? = null
) {
    val textColor = when (isOnline) {
        true -> SoftMatchaMint
        false -> StrawberryMilk
        null -> FairyLilac
    }
    val borderColor = when (isOnline) {
        true -> MintChipBorder
        false -> PeachChipBorder
        null -> LilacChipBorder
    }
    val containerColor = when (isOnline) {
        true -> MintChipFill
        false -> PeachChipFill
        null -> LilacChipFill
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ",
            fontSize = 11.sp,
            color = FairyLilac,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = status,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FavoriteAppListItem(
    app: AppInfo,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PastelPlumCard)
            .border(BorderStroke(1.2.dp, BorderPinkGlow), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WhippedCream
            )
            Text(
                text = app.packageName,
                fontSize = 11.sp,
                color = FairyLilac.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "OPEN ♡",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PastelPeach,
                modifier = Modifier.padding(end = 12.dp)
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PeachChipFill)
                    .border(BorderStroke(1.dp, PeachChipBorder), CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 11.sp,
                    color = StrawberryMilk,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsBar(
    isTouchActive: Boolean,
    keepScreenOn: Boolean,
    onRestartStack: () -> Unit,
    onToggleTouchShield: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onRebindAdb: () -> Unit,
    onReapplySurvival: () -> Unit,
    onRequestPerms: () -> Unit
) {
    Surface(
        color = PlumOledCanvas,
        border = BorderStroke(1.5.dp, BorderPinkGlow),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "🧸 ☁️ QUICK ACTIONS ☁️ 🧸",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FairyLilac,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestartStack,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelPlumLight),
                    border = BorderStroke(1.2.dp, BorderLilacGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text("🔄 Restart Stack", fontSize = 12.sp, color = FairyLilac, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleTouchShield,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTouchActive) PeachChipFill else PastelPlumLight
                    ),
                    border = BorderStroke(1.2.dp, if (isTouchActive) PeachChipBorder else BorderPinkGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (isTouchActive) "🔓 Unlock Shield" else "🔒 Lock Shield",
                        fontSize = 12.sp,
                        color = if (isTouchActive) StrawberryMilk else CottonCandyPink,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleKeepScreenOn,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (keepScreenOn) PastelPlumLight else PastelPlumCard
                    ),
                    border = BorderStroke(1.2.dp, if (keepScreenOn) BorderPinkGlow else BorderLilacGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (keepScreenOn) "☀️ Display: Always On" else "🌙 Display: 5m Sleep",
                        fontSize = 12.sp,
                        color = if (keepScreenOn) PastelButtercup else FairyLilac,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onRebindAdb,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, BorderLilacGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text("📡 ADB :5555", fontSize = 12.sp, color = BabySkyBlue, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReapplySurvival,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, BorderLilacGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text("🛡️ Survival Recheck", fontSize = 12.sp, color = SoftMatchaMint, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRequestPerms,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, BorderLilacGlow),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text("🔑 Perms & Shizuku", fontSize = 12.sp, color = FairyLilac, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
