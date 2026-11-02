package one.crinklecod.razrguard.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import one.crinklecod.razrguard.ui.theme.BabySkyBlue
import one.crinklecod.razrguard.ui.theme.CloudWhite
import one.crinklecod.razrguard.ui.theme.CottonCandyPink
import one.crinklecod.razrguard.ui.theme.LavenderHaze
import one.crinklecod.razrguard.ui.theme.MarshmallowMist
import one.crinklecod.razrguard.ui.theme.PlumInk
import one.crinklecod.razrguard.ui.theme.PowderBlue
import one.crinklecod.razrguard.ui.theme.SleepyRose

enum class MascotMood { Happy, Worried, Sleepy, Toasty }

/** Little cloud friend whose face mirrors how the server is doing. Dot eyes blink now and then. */
@Composable
fun CloudMascot(mood: MascotMood, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mascotBlink")
    val blink = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4600
                1f at 4300
                0.1f at 4400
                1f at 4500
            }
        ),
        label = "blink"
    )

    Canvas(modifier) {
        val body = cloudPath(size)
        drawPath(body, color = LavenderHaze.copy(alpha = 0.35f), style = Stroke(10.dp.toPx(), join = StrokeJoin.Round))
        drawPath(body, brush = Brush.verticalGradient(listOf(CloudWhite, MarshmallowMist)))
        drawPath(body, color = LavenderHaze, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))

        val w = size.width
        val h = size.height
        val eyeY = h * 0.58f
        val leftEye = Offset(w * 0.4f, eyeY)
        val rightEye = Offset(w * 0.62f, eyeY)
        val eyeR = h * 0.055f
        val line = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)

        // Rosy cheeks (extra rosy when toasty)
        val cheekAlpha = if (mood == MascotMood.Toasty) 0.85f else 0.55f
        listOf(Offset(w * 0.33f, h * 0.7f), Offset(w * 0.69f, h * 0.7f)).forEach { c ->
            drawOval(
                color = CottonCandyPink.copy(alpha = cheekAlpha),
                topLeft = Offset(c.x - eyeR * 1.4f, c.y - eyeR * 0.75f),
                size = Size(eyeR * 2.8f, eyeR * 1.5f)
            )
        }

        when (mood) {
            MascotMood.Happy -> {
                // ^ ^ eyes
                listOf(leftEye, rightEye).forEach { e -> drawCaret(e, eyeR * 1.3f, line) }
                drawArc(
                    color = PlumInk,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.47f, h * 0.62f),
                    size = Size(w * 0.08f, h * 0.12f),
                    style = line
                )
            }
            MascotMood.Sleepy -> {
                listOf(leftEye, rightEye).forEach { e ->
                    drawLine(PlumInk, Offset(e.x - eyeR, e.y), Offset(e.x + eyeR, e.y), line.width, StrokeCap.Round)
                }
                drawCircle(PlumInk, radius = eyeR * 0.45f, center = Offset(w * 0.51f, h * 0.71f))
            }
            MascotMood.Worried, MascotMood.Toasty -> {
                listOf(leftEye, rightEye).forEach { e ->
                    drawOval(
                        color = PlumInk,
                        topLeft = Offset(e.x - eyeR, e.y - eyeR * blink.value),
                        size = Size(eyeR * 2f, eyeR * 2f * blink.value)
                    )
                }
                // Little wobbly mouth
                val mouth = Path().apply {
                    moveTo(w * 0.47f, h * 0.73f)
                    cubicTo(w * 0.49f, h * 0.7f, w * 0.53f, h * 0.76f, w * 0.55f, h * 0.73f)
                }
                drawPath(mouth, PlumInk, style = line)
                if (mood == MascotMood.Toasty) {
                    // Sweat drop
                    val drop = Path().apply {
                        val x = w * 0.76f
                        val y = h * 0.38f
                        moveTo(x, y)
                        cubicTo(x + eyeR * 1.4f, y + eyeR * 2f, x + eyeR, y + eyeR * 3f, x, y + eyeR * 3f)
                        cubicTo(x - eyeR, y + eyeR * 3f, x - eyeR * 1.4f, y + eyeR * 2f, x, y)
                        close()
                    }
                    drawPath(drop, brush = Brush.verticalGradient(listOf(BabySkyBlue, PowderBlue)))
                } else {
                    // Worried brows
                    drawLine(SleepyRose, Offset(leftEye.x - eyeR, eyeY - eyeR * 2.6f), Offset(leftEye.x + eyeR, eyeY - eyeR * 3.2f), line.width * 0.8f, StrokeCap.Round)
                    drawLine(SleepyRose, Offset(rightEye.x - eyeR, eyeY - eyeR * 3.2f), Offset(rightEye.x + eyeR, eyeY - eyeR * 2.6f), line.width * 0.8f, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawCaret(center: Offset, half: Float, stroke: Stroke) {
    val p = Path().apply {
        moveTo(center.x - half, center.y + half * 0.4f)
        lineTo(center.x, center.y - half * 0.5f)
        lineTo(center.x + half, center.y + half * 0.4f)
    }
    drawPath(p, PlumInk, style = stroke)
}
