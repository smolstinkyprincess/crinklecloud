package one.crinklecod.cc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.crinklecod.cc.SystemLoad
import one.crinklecod.cc.ui.theme.BabySkyBlue
import one.crinklecod.cc.ui.theme.CloudWhite
import one.crinklecod.cc.ui.theme.CottonCandyPink
import one.crinklecod.cc.ui.theme.LavenderHaze
import one.crinklecod.cc.ui.theme.MintMilk
import one.crinklecod.cc.ui.theme.PeachSorbet
import one.crinklecod.cc.ui.theme.PlumInkSoft
import one.crinklecod.cc.ui.theme.ShimmerButter

/** CPU and RAM as two soft pastel bars; shows a gentle placeholder until data arrives. */
@Composable
fun SystemLoadBars(load: SystemLoad?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadBar(
            emoji = "🧠", label = "CPU",
            value = load?.let { "${it.cpuPercent}%" },
            fraction = (load?.cpuPercent ?: 0) / 100f,
            colors = listOf(MintMilk, BabySkyBlue, LavenderHaze),
        )
        LoadBar(
            emoji = "🍡", label = "RAM",
            value = load?.let { "%.1f/%.1fG".format(it.ramUsedMb / 1024f, it.ramTotalMb / 1024f) },
            fraction = (load?.ramPercent ?: 0) / 100f,
            colors = listOf(MintMilk, ShimmerButter, PeachSorbet, CottonCandyPink),
        )
    }
}

@Composable
private fun LoadBar(emoji: String, label: String, value: String?, fraction: Float, colors: List<Color>) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "loadBar")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$emoji $label", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f).widthIn(min = 6.dp))
            Text(
                text = value ?: "listening… ☁️",
                style = MaterialTheme.typography.labelMedium,
                color = PlumInkSoft
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .drawWithCache {
                    // Gradient spans the full track so a fuller bar reaches the "warmer" colours
                    val fill = Brush.horizontalGradient(colors, endX = size.width)
                    onDrawBehind {
                        val r = CornerRadius(size.height / 2f)
                        drawRoundRect(color = CloudWhite.copy(alpha = 0.85f), cornerRadius = r)
                        if (shown > 0f) {
                            val w = (size.width * shown).coerceAtLeast(size.height)
                            drawRoundRect(brush = fill, size = Size(w, size.height), cornerRadius = r)
                        }
                        drawRoundRect(
                            color = LavenderHaze.copy(alpha = 0.6f),
                            cornerRadius = r,
                            style = Stroke(1.dp.toPx())
                        )
                    }
                }
        )
    }
}
