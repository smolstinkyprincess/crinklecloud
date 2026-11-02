package one.crinklecod.razrguard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import one.crinklecod.razrguard.ui.theme.BubblegumPink
import one.crinklecod.razrguard.ui.theme.CloudWhite
import one.crinklecod.razrguard.ui.theme.CottonCandyPink
import one.crinklecod.razrguard.ui.theme.DreamLavender
import one.crinklecod.razrguard.ui.theme.LavenderHaze
import one.crinklecod.razrguard.ui.theme.MintSparkle
import one.crinklecod.razrguard.ui.theme.PowderBlue
import one.crinklecod.razrguard.ui.theme.SkyBabyBlue
import one.crinklecod.razrguard.ui.theme.SkyBlush
import one.crinklecod.razrguard.ui.theme.SkyCream
import one.crinklecod.razrguard.ui.theme.SkyLavender
import one.crinklecod.razrguard.ui.theme.SkyPeriwinkle
import one.crinklecod.razrguard.ui.theme.StarlightYellow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// One full drift cycle; every cloud's speed is a whole number of screen-crossings per cycle,
// so the animation wraps seamlessly instead of jumping when the transition restarts.
private const val DRIFT_CYCLE_MS = 240_000
private const val TWINKLE_CYCLE_MS = 7_000

private val SkyGradient = Brush.verticalGradient(
    0.0f to SkyBabyBlue,
    0.3f to SkyPeriwinkle,
    0.55f to SkyLavender,
    0.8f to SkyBlush,
    1.0f to SkyCream,
)

private class SkyCloud(
    val yFraction: Float,
    /** Width as a fraction of the canvas height, so clouds scale with the screen. */
    val widthOfHeight: Float,
    val crossingsPerCycle: Int,
    val phase: Float,
    val belly: Color,
    val alpha: Float,
)

private val Clouds = listOf(
    // Far layer: small, slow, hazy
    SkyCloud(0.10f, 0.30f, 1, 0.05f, DreamLavender, 0.55f),
    SkyCloud(0.34f, 0.26f, 1, 0.45f, BubblegumPink, 0.5f),
    SkyCloud(0.56f, 0.32f, 1, 0.78f, SkyBabyBlue, 0.5f),
    // Mid layer
    SkyCloud(0.20f, 0.46f, 2, 0.30f, BubblegumPink, 0.72f),
    SkyCloud(0.64f, 0.5f, 2, 0.82f, DreamLavender, 0.7f),
    // Near layer: big puffs hugging the bottom edge
    SkyCloud(0.80f, 0.72f, 3, 0.15f, SkyBlush, 0.9f),
    SkyCloud(0.86f, 0.62f, 3, 0.62f, DreamLavender, 0.85f),
)

private class TwinkleStar(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val color: Color,
    val isSparkle: Boolean,
    val speed: Int,
    val phase: Float,
)

private val Stars: List<TwinkleStar> = run {
    val rnd = Random(0x5EED)
    val palette = listOf(StarlightYellow, CloudWhite, CottonCandyPink, LavenderHaze, PowderBlue, MintSparkle)
    List(30) { i ->
        TwinkleStar(
            x = rnd.nextFloat(),
            // Keep most stars in the upper sky; a few drift lower for depth
            y = if (i % 4 == 0) 0.45f + rnd.nextFloat() * 0.4f else rnd.nextFloat() * 0.5f,
            radiusDp = if (i % 3 == 0) 5f + rnd.nextFloat() * 4f else 1.8f + rnd.nextFloat() * 1.8f,
            color = palette[i % palette.size],
            isSparkle = i % 3 == 0,
            speed = 1 + rnd.nextInt(3),
            phase = rnd.nextFloat()
        )
    }
}

/**
 * Full-screen pastel sky: gradient wash, slow aurora blobs, three parallax layers of drifting
 * Bezier clouds and twinkling sparkles. Animated values are only read during drawing, so the
 * sky never triggers recomposition of [content].
 */
@Composable
fun DreamySkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "dreamySky")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_CYCLE_MS, easing = LinearEasing)),
        label = "drift"
    )
    val twinkle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TWINKLE_CYCLE_MS, easing = LinearEasing)),
        label = "twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkyGradient)
            .drawWithCache {
                // Paths are rebuilt only when the canvas size changes
                val cloudPaths = Clouds.map { c ->
                    val w = size.height * c.widthOfHeight
                    cloudPath(Size(w, w * 0.56f))
                }
                val unitSparkle = sparklePath(Offset.Zero, 1f)

                onDrawBehind {
                    val t = drift.value
                    drawAurora(t)
                    Clouds.forEachIndexed { i, cloud ->
                        val cw = size.height * cloud.widthOfHeight
                        val ch = cw * 0.56f
                        val progress = (cloud.phase + t * cloud.crossingsPerCycle) % 1f
                        val left = -cw + progress * (size.width + cw)
                        val top = size.height * cloud.yFraction - ch / 2f
                        translate(left, top) {
                            // Lavender under-puff for volume, then the cloud body
                            translate(0f, ch * 0.06f) {
                                drawPath(cloudPaths[i], color = cloud.belly.copy(alpha = 0.35f * cloud.alpha))
                            }
                            drawPath(
                                cloudPaths[i],
                                brush = Brush.verticalGradient(
                                    listOf(CloudWhite, cloud.belly),
                                    startY = 0f,
                                    endY = ch
                                ),
                                alpha = cloud.alpha
                            )
                        }
                    }

                    val tw = twinkle.value
                    Stars.forEach { star ->
                        val v = 0.5f + 0.5f * sin(2f * PI.toFloat() * (tw * star.speed + star.phase))
                        val center = Offset(star.x * size.width, star.y * size.height)
                        val r = star.radiusDp.dp.toPx() * (0.7f + 0.3f * v)
                        val alpha = 0.25f + 0.75f * v
                        if (star.isSparkle) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(star.color.copy(alpha = 0.45f * alpha), Color.Transparent),
                                    center = center,
                                    radius = r * 2.2f
                                ),
                                radius = r * 2.2f,
                                center = center
                            )
                            withTransform({
                                translate(center.x, center.y)
                                scale(r, r, pivot = Offset.Zero)
                            }) {
                                drawPath(unitSparkle, color = star.color, alpha = alpha)
                            }
                        } else {
                            drawCircle(color = star.color, radius = r, center = center, alpha = alpha)
                        }
                    }
                }
            },
        content = content
    )
}

/** Three big, very soft colour blobs wandering slowly behind the clouds. */
private fun DrawScope.drawAurora(t: Float) {
    val tau = 2f * PI.toFloat()
    val blobs = listOf(
        Triple(CottonCandyPink, Offset(0.18f + 0.06f * sin(tau * t), 0.3f), 0.55f),
        Triple(PowderBlue, Offset(0.78f + 0.05f * sin(tau * (t + 0.33f)), 0.22f), 0.5f),
        Triple(LavenderHaze, Offset(0.5f, 0.7f + 0.05f * sin(tau * (t + 0.66f))), 0.6f),
    )
    blobs.forEach { (color, frac, radiusOfHeight) ->
        val center = Offset(frac.x * size.width, frac.y * size.height)
        val radius = size.height * radiusOfHeight
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.32f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
