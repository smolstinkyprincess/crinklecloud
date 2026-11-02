package one.crinklecod.cc.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import one.crinklecod.cc.launcher.AppIconCache
import one.crinklecod.cc.launcher.AppInfo
import one.crinklecod.cc.ui.animation.floatingBob
import one.crinklecod.cc.ui.animation.kawaiiBounceClick
import one.crinklecod.cc.ui.theme.CloudWhite
import one.crinklecod.cc.ui.theme.CottonCandyPink
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
import one.crinklecod.cc.ui.theme.PlumInkFaint
import one.crinklecod.cc.ui.theme.PlumInkSoft
import one.crinklecod.cc.ui.theme.SleepyRose
import one.crinklecod.cc.ui.theme.WhippedCream

private val TileTints = listOf(
    PillowFillPink to OutlinePink,
    PillowFillLilac to OutlineLilac,
    PillowFillSky to OutlineSky,
    PillowFillMint to OutlineMint,
    PillowFillButter to OutlineButter,
    PillowFillPeach to OutlinePeach,
)

/** Rasterized launcher icon for [packageName], loaded off the main thread and cached. */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val icon by produceState(AppIconCache.peek(packageName), packageName) {
        if (value == null) value = AppIconCache.load(context, packageName, sizePx)
    }
    return icon
}

/**
 * Right-column launcher deck: bouncy pastel app tiles in an adaptive grid plus a dashed
 * cloud "+ Add App" pill. Long-press any tile to enter wiggle-edit mode and remove favorites.
 */
@Composable
fun FavoriteAppsDeck(
    apps: List<AppInfo>,
    onOpenApp: (AppInfo) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onAddApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(apps.isEmpty()) { if (apps.isEmpty()) editing = false }

    PillowCard(
        modifier = modifier,
        tint = Color(0x99E9F3FF),
        outline = OutlineSky,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)
    ) {
        DreamySectionHeader(emoji = "🎀", title = "Favorite Apps · ${apps.size}") {
            if (editing) {
                PastelPill(
                    text = "Done ♡",
                    fill = PillowFillMint,
                    outline = OutlineMint,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { editing = false }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 74.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                AppTile(
                    app = app,
                    index = index,
                    editing = editing,
                    onOpen = { if (editing) editing = false else onOpenApp(app) },
                    onLongPress = { editing = true },
                    onRemove = { onRemoveApp(app) }
                )
            }
            item(key = "add-app", span = { GridItemSpan(maxLineSpan) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (apps.isEmpty()) {
                        Text(
                            text = "No favorites yet ☁️\nPin your server tools here!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    AddAppCloudPill(onClick = onAddApp)
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: AppInfo,
    index: Int,
    editing: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
) {
    val (fill, outline) = TileTints[index % TileTints.size]
    val icon = rememberAppIcon(app.packageName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .jellyWiggle(enabled = editing, phaseMillis = (index * 97) % 300)
            .kawaiiBounceClick(onLongClick = onLongPress, onClick = onOpen)
            .semantics { contentDescription = "Open ${app.label}" }
    ) {
        Box {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .size(56.dp)
                    .pillowShadow(cornerRadius = 22.dp, offsetY = 3.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(fill)
                    .border(2.dp, outline, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(38.dp))
                } else {
                    Text(
                        text = app.label.firstOrNull()?.uppercase() ?: "♡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PlumInk
                    )
                }
            }
            if (editing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .kawaiiBounceClick(pressedScale = 0.8f, onClick = onRemove)
                        .semantics { contentDescription = "Remove ${app.label} from favorites" }
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CloudWhite)
                        .border(2.dp, SleepyRose.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SleepyRose)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = PlumInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp)
        )
    }
}

/** Little rocking motion for edit mode, like jelly on a plate. */
private fun Modifier.jellyWiggle(enabled: Boolean, phaseMillis: Int): Modifier =
    if (!enabled) this else this.then(
        Modifier.composed {
            val transition = rememberInfiniteTransition(label = "jellyWiggle")
            val angle = transition.animateFloat(
                initialValue = -2.5f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(260, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(phaseMillis)
                ),
                label = "wiggleAngle"
            )
            graphicsLayer { rotationZ = angle.value }
        }
    )

/** Cloud-shaped "+ Add App" button with a dashed pastel outline. */
@Composable
fun AddAppCloudPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 6.dp)
            .floatingBob(amplitude = 2.dp, periodMillis = 4200)
            .kawaiiBounceClick(onClick = onClick)
            .semantics { contentDescription = "Add a favorite app" }
            .width(190.dp)
            .height(70.dp)
            .drawBehind {
                val cloud = cloudPath(size)
                drawPath(cloud, color = PillowFill)
                drawPath(
                    cloud,
                    color = CottonCandyPink,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Add App ♡",
            style = MaterialTheme.typography.labelLarge,
            color = PlumInk,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** Searchable picker of every launchable app that isn't already a favorite. */
@Composable
fun AddAppDialog(
    allApps: List<AppInfo>,
    currentFavorites: Set<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectApp: (AppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val selectable = remember(allApps, currentFavorites, query) {
        allApps.filter { app ->
            app.packageName !in currentFavorites && (
                query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PillowCard(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .fillMaxHeight(0.9f),
            tint = Color(0xF2FFEFF7),
            outline = OutlinePink,
            cornerRadius = 36.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            DreamySectionHeader(emoji = "☁️", title = "Add to favorites") {
                PastelPill(
                    text = "Close",
                    emoji = "♡",
                    fill = PillowFillPink,
                    outline = OutlinePink,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = onDismiss
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps… 🔍", color = PlumInkFaint) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CottonCandyPink,
                    unfocusedBorderColor = OutlineLilac,
                    focusedContainerColor = PillowFillStrong,
                    unfocusedContainerColor = PillowFill,
                    cursorColor = SleepyRose,
                    focusedTextColor = PlumInk,
                    unfocusedTextColor = PlumInk
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            if (selectable.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLoading) "Gathering apps… ☁️" else "No matching apps 🌸",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PlumInkSoft
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(selectable, key = { it.packageName }) { app ->
                        PickerRow(app = app, onClick = { onSelectApp(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(app: AppInfo, onClick: () -> Unit) {
    val icon = rememberAppIcon(app.packageName)
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .kawaiiBounceClick(pressedScale = 0.96f, onClick = onClick)
            .clip(shape)
            .background(WhippedCream)
            .border(1.5.dp, OutlineLilac, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PillowFillLilac),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            } else {
                Text(app.label.firstOrNull()?.uppercase() ?: "♡", fontWeight = FontWeight.Black, color = PlumInk)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = PlumInkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(PillowFillPink)
                .border(1.5.dp, OutlinePink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SleepyRose)
        }
    }
}
