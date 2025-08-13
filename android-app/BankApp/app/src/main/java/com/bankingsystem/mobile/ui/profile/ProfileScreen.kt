package com.bankingsystem.mobile.ui.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.data.model.UserProfile
import com.bankingsystem.mobile.ui.components.FadingAppBackground
import com.bankingsystem.mobile.ui.components.SectionCard
import com.bankingsystem.mobile.ui.components.Sidebar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    // inline editing controls (as you already had)
    editingField: String? = null,
    tempValue: String = "",
    onEditClicked: (field: String) -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onSaveEditing: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    // navigation drawer
    selectedItem: String = "Profile",
    onNavigate: (String) -> Unit = {}
) {
    var isSidebarOpen by remember { mutableStateOf(false) }
    val sidebarOffset by animateDpAsState(if (isSidebarOpen) 0.dp else (-280).dp, label = "sidebar")
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(Modifier.fillMaxSize()) {
        // Background behind EVERYTHING
        FadingAppBackground()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = { Text("My Profile") },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                errorMessage != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    val fields = listOf(
                        "username" to "Username",
                        "firstName" to "First Name",
                        "lastName" to "Last Name",
                        "email" to "Email",
                        "address" to "Address",
                        "city" to "City",
                        "state" to "State",
                        "country" to "Country",
                        "postalCode" to "Postal Code",
                        "homeNumber" to "Home Number",
                        "workNumber" to "Work Number",
                        "officeNumber" to "Office Number",
                        "mobileNumber" to "Mobile Number"
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- Hero header ---
                        item {
                            ProfileHeader(
                                name = buildName(profile),
                                email = profile?.email.orEmpty(),
                                role = profile?.roleName
                            )
                        }

                        // --- Editable fields ---
                        item {
                            SectionCard(title = "Details", modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    fields.forEach { (fieldKey, label) ->
                                        val isEditing = editingField == fieldKey
                                        val value = if (isEditing) tempValue else profile.getFieldValue(fieldKey).orEmpty()

                                        ProfileFieldRow(
                                            label = label,
                                            value = value,
                                            isEditing = isEditing,
                                            onEditClick = { onEditClicked(fieldKey) },
                                            onValueChange = onValueChange,
                                            onCancel = onCancelEditing,
                                            onSave = onSaveEditing
                                        )
                                    }
                                }
                            }
                        }

                        // --- Actions ---
                        item {
                            Button(
                                onClick = onChangePasswordClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Change Password", color = MaterialTheme.colorScheme.onError)
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        // Scrim when sidebar is open
        if (isSidebarOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable { isSidebarOpen = false }
            )
        }

        // Sidebar panel
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .offset(x = sidebarOffset),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        ) {
            Sidebar(
                selectedItem = selectedItem,
                onItemClick = {
                    isSidebarOpen = false
                    onNavigate(it)
                },
                userName = buildName(profile)
            )
        }
    }
}

/* --------------------------------- Pieces --------------------------------- */

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    role: String?
) {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(cs.primary, cs.primaryContainer))

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(cs.onPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimary
                        )
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = name.ifBlank { "User" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!role.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = cs.onPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = role,
                                color = cs.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                FilledTonalButton(onClick = onSave, shape = RoundedCornerShape(12.dp)) {
                    Text("Save")
                }
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancel")
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value.ifBlank { "Not set" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isNotBlank())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit $label",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/* --------------------------------- Helpers -------------------------------- */

private fun buildName(profile: UserProfile?): String {
    if (profile == null) return ""
    val parts = listOfNotNull(profile.firstName.trim(), profile.lastName.trim()).filter { it.isNotBlank() }
    return when {
        parts.isNotEmpty() -> parts.joinToString(" ")
        profile.username.isNotBlank() -> profile.username
        else -> ""
    }
}

private fun UserProfile?.getFieldValue(fieldName: String): String? = when (fieldName) {
    "username" -> this?.username
    "firstName" -> this?.firstName
    "lastName" -> this?.lastName
    "email" -> this?.email
    "address" -> this?.address
    "city" -> this?.city
    "state" -> this?.state
    "country" -> this?.country
    "postalCode" -> this?.postalCode
    "homeNumber" -> this?.homeNumber
    "workNumber" -> this?.workNumber
    "officeNumber" -> this?.officeNumber
    "mobileNumber" -> this?.mobileNumber
    else -> null
}
