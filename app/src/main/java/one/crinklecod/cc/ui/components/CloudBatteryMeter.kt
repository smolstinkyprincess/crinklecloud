package one.crinklecod.cc.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.crinklecod.cc.BatteryInfo
import one.crinklecod.cc.ui.theme.BabySkyBlue
import one.crinklecod.cc.ui.theme.BubblegumPink
import one.crinklecod.cc.ui.theme.CloudWhite
import one.crinklecod.cc.ui.theme.CottonCandyPink
import one.crinklecod.cc.ui.theme.HappyMint
import one.crinklecod.cc.ui.theme.LavenderHaze
import one.crinklecod.cc.ui.theme.MarshmallowMist
import one.crinklecod.cc.ui.theme.MintMilk
import one.crinklecod.cc.ui.theme.MintSparkle
import one.crinklecod.cc.ui.theme.OutlineButter
import one.crinklecod.cc.ui.theme.OutlineMint
import one.crinklecod.cc.ui.theme.OutlinePeach
import one.crinklecod.cc.ui.theme.OutlinePink
import one.crinklecod.cc.ui.theme.PeachSorbet
import one.crinklecod.cc.ui.theme.PillowFillButter
import one.crinklecod.cc.ui.theme.PillowFillMint
import one.crinklecod.cc.ui.theme.PillowFillPeach
import one.crinklecod.cc.ui.theme.PillowFillPink
import one.crinklecod.cc.ui.theme.PlumInk
import one.crinklecod.cc.ui.theme.PlumInkSoft
import one.crinklecod.cc.ui.theme.PowderBlue
import one.crinklecod.cc.ui.theme.ShimmerButter
import one.crinklecod.cc.ui.theme.SleepyRose
import one.crinklecod.cc.ui.theme.StarlightYellow
import one.crinklecod.cc.ui.theme.ToastyPeach
import kotlin.math.PI
import kotlin.math.sin

private const val THERMO_MIN_C = 20f
private const val THERMO_MAX_C = 50f

private data class ThermalMood(val label: String, val emoji: String, val ink: Color, val fill: Color, val outline: Color)

private fun thermalMood(tempC: Float, overheating: Boolean): ThermalMood = when {
    overheating -> ThermalMood("too toasty!", "🥵", SleepyRose, PillowFillPink, OutlinePink)
    tempC >= 41f -> ThermalMood("toasty", "🔥", ToastyPeach, PillowFillPeach, OutlinePeach)
    tempC >= 35f -> ThermalMood("warm", "☀️", ToastyPeach, PillowFillButter, OutlineButter)
    else -> ThermalMood("cozy", "🌡️", HappyMint, PillowFillMint, OutlineMint)
}

private fun levelColors(level: Float): List<Color> = when {
    level >= 0.5f -> listOf(MintSparkle, BabySkyBlue)
    level >= 0.2f -> listOf(StarlightYellow, PeachSorbet)
    else -> listOf(CottonCandyPink, BubblegumPink)
}

/**
 * Battery level as pastel "liquid" inside a pillowy cloud, with a gently sloshing wave on top
 * (plus rising bubbles while charging), and a thermal strip underneath.
 */
@Composable
fun CloudBatteryMeter(
    battery: BatteryInfo?,
    modifier: Modifier = Modifier,
) {
    val percent = battery?.levelPercent?.takeIf { it >= 0 }
    val target = (percent ?: 0) / 100f
    val level by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow),
        label = "batteryLevel"
    )

    val transition = rememberInfiniteTransition(label = "batteryWave")
    val wave = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "wavePhase"
    )
    val charging = battery?.isPlugged == true

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.4f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cloud = cloudPath(size)
                // Cream base so the empty part of the cloud still looks soft
                drawPath(cloud, brush = Brush.verticalGradient(listOf(CloudWhite, MarshmallowMist)))

                clipPath(cloud) {
                    // Cloud's usable vertical range is roughly 5%..92% of the box
                    val top = size.height * 0.05f
                    val bottom = size.height * 0.92f
                    val surfaceY = bottom - (bottom - top) * level
                    val amp = 3.5.dp.toPx() * if (level in 0.02f..0.98f) 1f else 0f
                    val phase = wave.value * 2f * PI.toFloat()

                    val liquid = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, surfaceY)
                        val steps = 28
                        for (s in 0..steps) {
                            val x = size.width * s / steps
                            val y = surfaceY + amp * sin(phase + x / size.width * 4f * PI.toFloat())
                            lineTo(x, y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(
                        liquid,
                        brush = Brush.verticalGradient(levelColors(level), startY = surfaceY, endY = size.height)
                    )

                    if (charging && level > 0.05f) {
                        // Three bubbles floating up through the liquid
                        repeat(3) { i ->
                            val p = (wave.value + i / 3f) % 1f
                            val bx = size.width * (0.3f + 0.2f * i)
                            val by = bottom - (bottom - surfaceY) * p
                            drawCircle(
                                color = CloudWhite.copy(alpha = 0.75f * (1f - p)),
                                radius = 3.dp.toPx() + 2.dp.toPx() * i,
                                center = Offset(bx, by)
                            )
                        }
                    }
                }

                // Double outline: thick white puff + thin lavender line inside it
                drawPath(cloud, color = CloudWhite, style = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Round))
                drawPath(cloud, color = LavenderHaze, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = if (percent != null) "$percent%" else "··%",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 26.sp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when {
                        percent == null -> "listening… ☁️"
                        charging -> "⚡ charging"
                        else -> "🔋 on battery"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PlumInkSoft
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (battery != null) {
            val mood = thermalMood(battery.tempCelsius, battery.isOverheating)
            ThermalStrip(tempC = battery.tempCelsius, mood = mood)
        }
    }
}

@Composable
private fun ThermalStrip(tempC: Float, mood: ThermalMood) {
    val fraction by animateFloatAsState(
        targetValue = ((tempC - THERMO_MIN_C) / (THERMO_MAX_C - THERMO_MIN_C)).coerceIn(0f, 1f),
        label = "thermoMarker"
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PastelPill(
                text = "%.1f°C".format(tempC),
                emoji = mood.emoji,
                fill = mood.fill,
                outline = mood.outline,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = mood.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = mood.ink
            )
        }
        Spacer(Modifier.height(5.dp))
        // Gradient track with a little white bead marking the current temperature
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .drawWithCache {
                    val track = Brush.horizontalGradient(listOf(MintMilk, ShimmerButter, PeachSorbet, CottonCandyPink))
                    onDrawBehind {
                        val r = CornerRadius(size.height / 2f)
                        drawRoundRect(brush = track, cornerRadius = r)
                        drawRoundRect(
                            color = PowderBlue.copy(alpha = 0.5f),
                            cornerRadius = r,
                            style = Stroke(1.dp.toPx())
                        )
                        val cx = size.height / 2f + (size.width - size.height) * fraction
                        val c = Offset(cx, size.height / 2f)
                        drawCircle(CloudWhite, radius = size.height * 0.85f, center = c)
                        drawCircle(mood.ink, radius = size.height * 0.85f, center = c, style = Stroke(2.dp.toPx()))
                    }
                }
        )
    }
}
