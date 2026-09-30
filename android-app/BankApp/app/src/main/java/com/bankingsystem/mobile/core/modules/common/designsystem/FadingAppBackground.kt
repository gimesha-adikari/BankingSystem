package com.bankingsystem.mobile.core.modules.common.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.core.modules.common.designsystem.theme.BrandIndigo
import kotlinx.coroutines.launch

@Composable
fun FadingAppBackground() {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1.02f) }
    val lift = remember { Animatable(12f) }
    val overlay = remember { Animatable(0.22f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(900, easing = EaseOutCubic)) }
        launch { scale.animateTo(1f, tween(900, easing = EaseOutCubic)) }
        launch { lift.animateTo(0f, tween(900, easing = EaseOutCubic)) }
        launch { overlay.animateTo(0f, tween(1200)) }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(
            modifier = Modifier.graphicsLayer(
                alpha = alpha.value,
                scaleX = scale.value,
                scaleY = scale.value,
                translationY = with(density) { lift.value.dp.toPx() }
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to BrandIndigo.copy(alpha = overlay.value),
                        1f to Color.Transparent
                    )
                )
        )
    }
}
