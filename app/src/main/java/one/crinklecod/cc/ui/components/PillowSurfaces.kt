package one.crinklecod.cc.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.crinklecod.cc.ui.animation.kawaiiBounceClick
import one.crinklecod.cc.ui.theme.OutlineLilac
import one.crinklecod.cc.ui.theme.PillowFillStrong
import one.crinklecod.cc.ui.theme.PillowShadow
import one.crinklecod.cc.ui.theme.PlumInk
import one.crinklecod.cc.ui.theme.PlumInkSoft

/**
 * Soft offset "sticker" shadow under a rounded shape. Cheaper and cuter than elevation,
 * and doesn't bleed through translucent fills the way platform shadows do.
 */
fun Modifier.pillowShadow(cornerRadius: Dp, color: Color = PillowShadow, offsetY: Dp = 4.dp): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, offsetY.toPx()),
            size = size,
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }

/**
 * Translucent pillowy container: whipped-cream top fading into a pastel [tint],
 * a thick soft outline, and a lavender sticker shadow.
 */
@Composable
fun PillowCard(
    modifier: Modifier = Modifier,
    tint: Color = Color(0x99FFE6F2),
    outline: Color = OutlineLilac,
    cornerRadius: Dp = 32.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .pillowShadow(cornerRadius)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PillowFillStrong, tint)))
            .border(2.dp, outline, shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/** Little section title: emoji + spaced-out caps + optional trailing slot. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DreamySectionHeader(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        // Long titles glide sideways instead of being cut off
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 2_000, velocity = 24.dp)
        )
        trailing()
    }
}

/** Bouncy pastel capsule used for chips and quick actions. */
@Composable
fun PastelPill(
    text: String,
    fill: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    textColor: Color = PlumInk,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.kawaiiBounceClick(onLongClick = onLongClick, onClick = onClick)
                else Modifier
            )
            .clip(shape)
            .background(fill)
            .border(1.5.dp, outline, shape)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading?.invoke()
        if (emoji != null) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Two-line chip: tiny caption over a bold value (used for IPs and status in the halo bar). */
@Composable
fun CaptionValueChip(
    caption: String,
    value: String,
    fill: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = PlumInk,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.kawaiiBounceClick(onClick = onClick) else Modifier)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, outline, CircleShape)
            .padding(start = 10.dp, end = 14.dp, top = 4.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(7.dp))
        }
        Column {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = PlumInkSoft,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}
