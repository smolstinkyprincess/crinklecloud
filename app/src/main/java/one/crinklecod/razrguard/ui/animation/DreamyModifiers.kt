package one.crinklecod.razrguard.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.sin

/**
 * Gentle, endless vertical bob, like something resting on a cloud.
 * [phaseMillis] staggers neighbouring elements so they don't bob in lockstep.
 */
fun Modifier.floatingBob(
    amplitude: Dp = 5.dp,
    periodMillis: Int = 3600,
    phaseMillis: Int = 0,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "floatingBob")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            initialStartOffset = StartOffset(phaseMillis)
        ),
        label = "bobPhase"
    )
    graphicsLayer {
        translationY = sin(phase.value * 2f * PI.toFloat()) * amplitude.toPx()
    }
}

/**
 * Squishy marshmallow click: squashes on press, then springs back with an underdamped wobble.
 * Quick taps still get the full squish because the release always passes through [pressedScale].
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.kawaiiBounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.9f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> scale.animateTo(
                    pressedScale,
                    spring(stiffness = Spring.StiffnessHigh)
                )
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    if (scale.value > pressedScale + 0.01f) {
                        scale.animateTo(pressedScale, tween(70))
                    }
                    scale.animateTo(
                        1f,
                        spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onLongClick = onLongClick?.let { longClick ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    longClick()
                }
            },
            onClick = onClick
        )
}

/**
 * Soft radial aura that slowly inhales and exhales behind the element.
 * Drawn outside the element's bounds, so leave a little room around it.
 */
fun Modifier.breathingGlow(
    color: Color,
    glowRadius: Dp = 10.dp,
    periodMillis: Int = 2600,
    enabled: Boolean = true,
): Modifier = if (!enabled) this else this.then(
    Modifier.composed {
        val transition = rememberInfiniteTransition(label = "breathingGlow")
        val breath = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(periodMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        drawBehind {
            val b = breath.value
            val radius = size.minDimension / 2f + glowRadius.toPx() * (0.45f + 0.55f * b)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.25f + 0.4f * b), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
)
