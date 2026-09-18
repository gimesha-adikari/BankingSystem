package com.bankingsystem.mobile.features.home.interfaces.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BalanceCard(balance: Double, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        listOf(cs.onPrimary, cs.inversePrimary)
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier
                .background(gradient)
                .padding(22.dp)
        ) {
            Column {
                Text(
                    "Account Balance",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = balance,
                    transitionSpec = {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 3 } + fadeOut())
                    },
                    label = "balance-anim"
                ) { value ->
                    Text(
                        "$${"%,.2f".format(value)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onPrimaryContainer
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 24.dp))
                    .background(Color.White.copy(0.08f))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(0.10f))
            )
        }
    }
}