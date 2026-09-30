package com.bankingsystem.mobile.core.modules.common.designsystem

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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.R
import com.bankingsystem.mobile.core.modules.common.designsystem.theme.AppBg
import com.bankingsystem.mobile.core.modules.common.designsystem.theme.BrandIndigo
import com.bankingsystem.mobile.core.modules.common.designsystem.theme.BrandRose
import kotlin.math.min

@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        Box(Modifier.fillMaxSize().background(AppBg))

        Image(
            painter = painterResource(id = R.drawable.bg_bank),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
        )

        Box(Modifier.fillMaxSize().background(AppBg.copy(alpha = 0.65f)))

        if (size.width > 0 && size.height > 0) {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val minDim = min(w, h)

            val glowIndigo = Brush.radialGradient(
                colors = listOf(BrandIndigo.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.18f),
                radius = minDim * 0.9f
            )
            val glowRose = Brush.radialGradient(
                colors = listOf(BrandRose.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.62f),
                radius = minDim
            )
            val topHighlight = Brush.verticalGradient(
                colors = listOf(BrandIndigo.copy(alpha = 0.08f), Color.Transparent),
                startY = 0f,
                endY = h * 0.32f
            )
            val vignette = Brush.radialGradient(
                colors = listOf(Color.Transparent, AppBg.copy(alpha = 0.78f)),
                center = Offset(w * 0.5f, h * 0.55f),
                radius = minDim * 0.95f
            )

            Box(Modifier.fillMaxSize().background(glowIndigo).blur(2.dp))
            Box(Modifier.fillMaxSize().background(glowRose).blur(2.dp))
            Box(Modifier.fillMaxSize().background(topHighlight))
            Box(Modifier.fillMaxSize().background(vignette))
        }
    }
}
