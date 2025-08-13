package com.bankingsystem.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.R
import kotlin.math.min
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        // Base blurred background image
        Image(
            painter = painterResource(id = R.drawable.bg_bank), // Your background image
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp) // Blur effect
        )

        if (size.width > 0 && size.height > 0) {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val minDim = min(w, h)

            // Blue glow overlays
            val blob1 = Brush.radialGradient(
                colors = listOf(Color(0xFF3F51B5).copy(alpha = 0.14f), Color.Transparent),
                center = Offset(w * 0.20f, h * 0.15f),
                radius = minDim * 0.9f
            )
            val blob2 = Brush.radialGradient(
                colors = listOf(Color(0xFF2196F3).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.90f, h * 0.60f),
                radius = minDim * 1.0f
            )

            Box(Modifier.fillMaxSize().background(blob1).blur(2.dp))
            Box(Modifier.fillMaxSize().background(blob2).blur(2.dp))
        }
    }
}
