package one.crinklecod.razrguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import one.crinklecod.razrguard.ui.animation.breathingGlow
import one.crinklecod.razrguard.ui.theme.BubblegumPink
import one.crinklecod.razrguard.ui.theme.CloudWhite
import one.crinklecod.razrguard.ui.theme.CottonCandyPink
import one.crinklecod.razrguard.ui.theme.DreamLavender
import one.crinklecod.razrguard.ui.theme.HappyMint
import one.crinklecod.razrguard.ui.theme.LavenderHaze
import one.crinklecod.razrguard.ui.theme.OutlineButter
import one.crinklecod.razrguard.ui.theme.PendingLilac
import one.crinklecod.razrguard.ui.theme.PlumInkFaint
import one.crinklecod.razrguard.ui.theme.ShimmerButter
import one.crinklecod.razrguard.ui.theme.SleepyRose
import one.crinklecod.razrguard.ui.theme.StarlightYellow

enum class GlyphKind { Star, Heart }

/** One service in the constellation; [online] is null until the first check has run. */
@Immutable
data class ServiceStatus(
    val name: String,
    val detail: String,
    val online: Boolean?,
    val glyph: GlyphKind = GlyphKind.Star,
)

/**
 * Star (servers) or heart (connectivity) badge. Healthy badges breathe a soft aura;
 * down ones go pale with a rose outline; unknown ones wait in faint lavender.
 */
@Composable
fun StatusGlyph(
    kind: GlyphKind,
    online: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val fill = when (online) {
        true -> if (kind == GlyphKind.Star) listOf(ShimmerButter, StarlightYellow) else listOf(BubblegumPink, CottonCandyPink)
        false -> listOf(CloudWhite, DreamLavender)
        null -> listOf(CloudWhite, LavenderHaze.copy(alpha = 0.6f))
    }
    val outline = when (online) {
        true -> CloudWhite
        false -> SleepyRose.copy(alpha = 0.75f)
        null -> PlumInkFaint
    }
    val glow = if (kind == GlyphKind.Star) StarlightYellow else CottonCandyPink

    Box(
        modifier = modifier
            .size(size)
            .breathingGlow(color = glow, glowRadius = size / 2.5f, enabled = online == true),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = when (kind) {
                GlyphKind.Star -> starPath(center, this.size.minDimension / 2f, innerRatio = 0.52f)
                GlyphKind.Heart -> heartPath(center, this.size.minDimension * 0.95f)
            }
            drawPath(path, brush = Brush.verticalGradient(fill))
            drawPath(path, color = outline, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

/** Left-column card listing every watched service as a twinkling badge. */
@Composable
fun ServiceConstellation(
    services: List<ServiceStatus>,
    modifier: Modifier = Modifier,
) {
    val shining = services.count { it.online == true }
    PillowCard(modifier = modifier, tint = Color(0x99FFF7DA), outline = OutlineButter) {
        DreamySectionHeader(emoji = "🌟", title = "Service Constellation") {
            Text(
                text = "$shining/${services.size} ✨",
                style = MaterialTheme.typography.labelMedium,
                color = if (shining == services.size) HappyMint else PendingLilac
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            services.forEach { service -> ServiceRow(service) }
        }
    }
}

@Composable
private fun ServiceRow(service: ServiceStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusGlyph(kind = service.glyph, online = service.online)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = service.detail,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = when (service.online) {
                true -> "shining"
                false -> "sleepy 💤"
                null -> "peeking…"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (service.online) {
                true -> HappyMint
                false -> SleepyRose
                null -> PendingLilac
            }
        )
    }
}
