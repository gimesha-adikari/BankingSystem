package com.bankingsystem.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.data.model.Transaction
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListSwipe(
    transactions: List<Transaction>,
    snackbarHostState: SnackbarHostState
) {
    val list = remember(transactions) { mutableStateListOf(*transactions.toTypedArray()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items = list, key = { it.id }) { txn ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    value == SwipeToDismissBoxValue.EndToStart
                },
                positionalThreshold = { totalDistance -> totalDistance * 0.35f }
            )

            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                LaunchedEffect(txn.id) {
                    val index = list.indexOf(txn)
                    if (index >= 0) {
                        list.removeAt(index)
                        val result = snackbarHostState.showSnackbar(
                            message = "Transaction '${txn.title}' deleted",
                            actionLabel = "Undo",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            list.add(index.coerceAtMost(list.size), txn)
                        }
                    }
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 20.dp)
                        )
                    }
                },
                content = {
                    Surface(
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        TransactionItem(
                            transaction = txn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, modifier: Modifier = Modifier) {
    val isCredit = transaction.amount >= 0
    val dotColor = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val amountText = (if (isCredit) "+$" else "-$") + "%,.2f".format(abs(transaction.amount))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(transaction.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(transaction.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                amountText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = if (isCredit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}