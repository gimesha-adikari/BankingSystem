package com.bankingsystem.mobile.core.modules.common.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    headerAction: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            cs.primary.copy(alpha = 0.25f),
            cs.primary.copy(alpha = 0.10f),
            cs.primary.copy(alpha = 0.04f)
        )
    )
    val tintBrush = Brush.linearGradient(
        listOf(
            cs.primary.copy(alpha = 0.06f),
            cs.primary.copy(alpha = 0.00f)
        )
    )

    Surface(
        modifier = modifier
            .border(BorderStroke(1.dp, borderBrush), shape)
            .background(tintBrush, shape),
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(contentPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.7f)
                )
            }
            if (showDivider) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = cs.outlineVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            if (headerAction != null) {
                headerAction()
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}
