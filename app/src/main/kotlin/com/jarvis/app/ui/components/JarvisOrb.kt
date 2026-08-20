package com.jarvis.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.core.domain.model.JarvisState

/**
 * Section 15's orb behavior table. Animation speed is capped (no unbounded high-rate
 * animation) and is only meaningfully driven while composed on screen — the Activity's
 * lifecycle (STARTED-gated recomposition) takes care of not animating in the background,
 * per section 4's battery guidance.
 */
@Composable
fun JarvisOrb(state: JarvisState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jarvis-orb")
    val periodMillis = when (state) {
        is JarvisState.Idle -> 4000
        is JarvisState.Listening -> 1200
        is JarvisState.Thinking -> 900
        is JarvisState.Executing -> 600
        else -> 1500
    }
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val baseColor = colorFor(state)

    Canvas(modifier = modifier.size(160.dp)) {
        val radius = size.minDimension / 2.5f
        val pulse = when (state) {
            is JarvisState.Idle -> 0.9f + 0.1f * kotlin.math.sin(phase * 2 * Math.PI).toFloat()
            is JarvisState.Executing -> 0.85f + 0.3f * phase
            else -> 1.0f
        }
        drawCircle(
            color = baseColor,
            radius = radius * pulse,
            center = Offset(size.width / 2, size.height / 2)
        )
    }
}

private fun colorFor(state: JarvisState): Color = when (state) {
    is JarvisState.Idle -> Color(0xFF4EA1FF)
    is JarvisState.Prompting -> Color(0xFF7C4DFF)
    is JarvisState.Listening -> Color(0xFF4EE1B0)
    is JarvisState.Thinking -> Color(0xFFFFC24E)
    is JarvisState.Executing -> Color(0xFFFF9F4E)
    is JarvisState.Success -> Color(0xFF4EE18C)
    is JarvisState.Error -> Color(0xFFFF5A5A)
    is JarvisState.Cancelled -> Color(0xFF8892A6)
}
