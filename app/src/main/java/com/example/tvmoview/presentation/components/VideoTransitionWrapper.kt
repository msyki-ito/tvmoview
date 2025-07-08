package com.example.tvmoview.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box

@Composable
fun VideoTransitionWrapper(
    isFullScreen: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(targetState = isFullScreen, label = "video_transition")

    val scale by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        }, label = "scale"
    ) { full -> if (full) 1f else 0.8f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) }, label = "alpha"
    ) { if (it) 1f else 0.95f }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}
