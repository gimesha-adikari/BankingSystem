package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.features.wallet.interfaces.ui.components.EmptyState
import com.bankingsystem.mobile.features.wallet.interfaces.ui.components.ErrorBanner
import com.bankingsystem.mobile.features.wallet.interfaces.ui.components.ShimmerBox
import com.bankingsystem.mobile.features.wallet.interfaces.ui.objects.WalletStrings
import com.bankingsystem.mobile.features.wallet.interfaces.ui.objects.WalletUiTokens

@Composable
fun CardListScreen(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onAddCard: () -> Unit,
    onBack: () -> Unit,
    onMakeDefault: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Cards", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onAddCard) { Text("Add card") }
            }
        }
        Spacer(Modifier.height(12.dp))

        state.lastError?.let { ErrorBanner(message = it); Spacer(Modifier.height(12.dp)) }

        when {
            state.isLoadingCards -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { ShimmerBox(height = 72) }
                }
            }
            state.cards.isEmpty() -> {
                EmptyState(
                    title = WalletStrings.NoCardsTitle,
                    subtitle = WalletStrings.NoCardsSubtitle,
                    cta = WalletStrings.AddCardCta,
                    onCta = onAddCard
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.cards, key = { it.id }) { card ->
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(
                                    alpha = WalletUiTokens.CardSurfaceAlpha
                                )
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${card.brand} •••• ${card.last4}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Exp ${card.expMonth}/${card.expYear}",
                                        color = Color.White.copy(0.85f)
                                    )
                                }
                                if (card.isDefault) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    OutlinedButton(onClick = { onMakeDefault(card.id) }) {
                                        Text("Make default")
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { onDelete(card.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
