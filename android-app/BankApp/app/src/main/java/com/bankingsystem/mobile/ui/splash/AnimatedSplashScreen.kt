package com.bankingsystem.mobile.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.R
import kotlinx.coroutines.delay

/**
 * A composable function that displays an animated splash screen.
 *
 * The splash screen features a logo that scales in and out before triggering an action
 * after a specified delay.
 *
 * @param onTimeout A lambda function to be invoked when the splash screen animation completes and the delay has passed.
 */
@Composable
fun AnimatedSplashScreen(onTimeout: () -> Unit) {
    // State for the scale animation of the logo
    val scale = remember { Animatable(0.8f) } // Initial scale value
    // Effect to run when the composable enters the composition
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        delay(1000) // Wait for 1 second after the animation
        onTimeout() // Trigger the timeout action
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        // Center the content within the Box
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .scale(scale.value)
        )
    }
}
