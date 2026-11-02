package one.crinklecod.cc.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Puffy three-bump cloud built from cubic Beziers, filling a [size] box at [origin].
 * The bottom is a shallow curve so it reads as a soft pillow rather than a flat shelf.
 */
fun cloudPath(size: Size, origin: Offset = Offset.Zero): Path {
    val w = size.width
    val h = size.height
    val x = origin.x
    val y = origin.y
    val base = y + h * 0.9f
    return Path().apply {
        moveTo(x + w * 0.2f, base)
        // Left end puff
        cubicTo(x + w * 0.02f, base, x - w * 0.01f, y + h * 0.52f, x + w * 0.17f, y + h * 0.48f)
        // Small top-left puff
        cubicTo(x + w * 0.15f, y + h * 0.2f, x + w * 0.4f, y + h * 0.12f, x + w * 0.46f, y + h * 0.3f)
        // Big crown puff
        cubicTo(x + w * 0.5f, y + h * 0.0f, x + w * 0.86f, y + h * 0.02f, x + w * 0.8f, y + h * 0.4f)
        // Right end puff
        cubicTo(x + w * 1.01f, y + h * 0.4f, x + w * 1.0f, base, x + w * 0.8f, base)
        // Soft, slightly sagging bottom
        cubicTo(x + w * 0.62f, y + h * 0.98f, x + w * 0.38f, y + h * 0.98f, x + w * 0.2f, base)
        close()
    }
}

/** [cloudPath] as a [Shape], for clipping and borders. */
object CloudShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(cloudPath(size))
}

/** Five-point star; [innerRatio] controls how chubby the arms are. */
fun starPath(center: Offset, outerRadius: Float, innerRatio: Float = 0.5f): Path = Path().apply {
    val inner = outerRadius * innerRatio
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else inner
        val angle = -PI / 2 + i * PI / 5
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** Classic two-lobe heart inside a square of side [size] centred on [center]. */
fun heartPath(center: Offset, size: Float): Path {
    val x = center.x - size / 2f
    val y = center.y - size / 2f
    fun px(f: Float) = x + size * f
    fun py(f: Float) = y + size * f
    return Path().apply {
        moveTo(px(0.5f), py(0.94f))
        cubicTo(px(0.12f), py(0.66f), px(-0.04f), py(0.36f), px(0.18f), py(0.16f))
        cubicTo(px(0.33f), py(0.03f), px(0.48f), py(0.12f), px(0.5f), py(0.27f))
        cubicTo(px(0.52f), py(0.12f), px(0.67f), py(0.03f), px(0.82f), py(0.16f))
        cubicTo(px(1.04f), py(0.36f), px(0.88f), py(0.66f), px(0.5f), py(0.94f))
        close()
    }
}

/** Four-point concave twinkle, the classic ✦ sparkle. */
fun sparklePath(center: Offset, radius: Float): Path {
    val c = center
    val k = radius * 0.14f
    return Path().apply {
        moveTo(c.x, c.y - radius)
        cubicTo(c.x + k, c.y - k, c.x + k, c.y - k, c.x + radius, c.y)
        cubicTo(c.x + k, c.y + k, c.x + k, c.y + k, c.x, c.y + radius)
        cubicTo(c.x - k, c.y + k, c.x - k, c.y + k, c.x - radius, c.y)
        cubicTo(c.x - k, c.y - k, c.x - k, c.y - k, c.x, c.y - radius)
        close()
    }
}
