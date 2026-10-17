package com.example.prelimproject.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

// Material 3 Expressive-style press motion: surfaces gently squash while
// pressed and spring back on release. Pair with M3 clickable surfaces
// (Card(onClick), buttons) by passing the same interaction source to both.
@Composable
fun rememberPressInteraction() = remember { MutableInteractionSource() }

@Composable
fun pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    ).value
}

fun Modifier.pressableScale(scale: Float): Modifier =
    this.graphicsLayer(scaleX = scale, scaleY = scale)
