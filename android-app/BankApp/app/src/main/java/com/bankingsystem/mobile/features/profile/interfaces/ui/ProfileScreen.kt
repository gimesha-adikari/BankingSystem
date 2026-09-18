package com.bankingsystem.mobile.features.profile.interfaces.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.features.profile.domain.model.UserProfile
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,

    editingField: EditableField? = null,
    tempValue: String = "",

    onEditClicked: (EditableField) -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onSaveEditing: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onChangePasswordClick: () -> Unit = {},

    selectedItem: String = "Profile",
    onNavigate: (String) -> Unit = {},

    isCheckingUsername: Boolean = false,
    isUsernameAvailable: Boolean? = null,
) {
    val cs = MaterialTheme.colorScheme
    var isSidebarOpen by remember { mutableStateOf(false) }
    val sidebarOffset by animateDpAsState(if (isSidebarOpen) 0.dp else (-280).dp, label = "sidebar")
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "My Profile",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Open navigation",
                                tint = cs.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
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
                ) { CircularProgressIndicator() }

                errorMessage != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.error
                    )
                }

                else -> {
                    val fields: List<Pair<EditableField, String>> = listOf(
                        EditableField.USERNAME to "Username",
                        EditableField.FIRST_NAME to "First Name",
                        EditableField.LAST_NAME to "Last Name",
                        EditableField.EMAIL to "Email",
                        EditableField.ADDRESS to "Address",
                        EditableField.CITY to "City",
                        EditableField.STATE to "State",
                        EditableField.COUNTRY to "Country",
                        EditableField.POSTAL_CODE to "Postal Code",
                        EditableField.HOME_NUMBER to "Home Number",
                        EditableField.WORK_NUMBER to "Work Number",
                        EditableField.OFFICE_NUMBER to "Office Number",
                        EditableField.MOBILE_NUMBER to "Mobile Number"
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "My Profile",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.35f
                                    )
                                )
                                Spacer(Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    fields.forEach { (field, label) ->
                                        val isEditing = editingField == field
                                        val value =
                                            if (isEditing) tempValue else profile.getFieldValue(
                                                field
                                            )

                                        ProfileTableRow(
                                            label = label,
                                            value = value,
                                            isEditing = isEditing,
                                            onEditClick = { onEditClicked(field) },
                                            onValueChange = onValueChange,
                                            onCancel = onCancelEditing,
                                            onSave = onSaveEditing,
                                            isCheckingUsername = field == EditableField.USERNAME && isCheckingUsername,
                                            isUsernameAvailable = if (field == EditableField.USERNAME) isUsernameAvailable else null
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                profile?.roleName?.takeIf { it.isNotBlank() }?.let { role ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(999.dp),
                                        tonalElevation = 0.dp
                                    ) {
                                        Text(
                                            text = role.uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }

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

        if (isSidebarOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable { isSidebarOpen = false }
            )
        }
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

/* ---------- Rows / chips ---------- */

@Composable
private fun ProfileTableRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isCheckingUsername: Boolean = false,
    isUsernameAvailable: Boolean? = null
) {
    val cs = MaterialTheme.colorScheme

    if (!isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.weight(0.9f)
            )

            Text(
                value.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank())
                    cs.onSurfaceVariant.copy(alpha = 0.9f) else Color.Black,
                fontStyle = if (value.isBlank()) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.6f)
            )

            TextButton(
                onClick = onEditClick,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)
            ) {
                Text("Edit")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(0.9f)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.weight(1.6f),
                shape = RoundedCornerShape(12.dp)
            )
            Row {
                TextButton(onClick = onSave) { Text("Save") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }

        if (label == "Username") {
            val helper = when {
                isCheckingUsername -> "Checking username..."
                isUsernameAvailable == true -> "Username is available"
                isUsernameAvailable == false -> "Username is not available or too short"
                else -> null
            }
            if (helper != null) {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isCheckingUsername -> cs.onSurfaceVariant
                        isUsernameAvailable == true -> cs.tertiary
                        else -> cs.error
                    },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleChip(text: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.primary.copy(alpha = 0.12f),
        contentColor = cs.primary,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 0.dp
    ) {
        Text(
            text = if (text.isBlank()) "UNKNOWN" else text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/* ---------- Helpers ---------- */

private fun buildName(profile: UserProfile?): String {
    if (profile == null) return ""
    val first = profile.firstName?.trim().orEmpty()
    val last = profile.lastName?.trim().orEmpty()
    val user = profile.username?.trim().orEmpty()
    val parts = listOf(first, last).filter { it.isNotBlank() }
    return when {
        parts.isNotEmpty() -> parts.joinToString(" ")
        user.isNotBlank() -> user
        else -> ""
    }
}

private fun UserProfile?.getFieldValue(field: EditableField): String {
    val raw: String? = when (field) {
        EditableField.USERNAME -> this?.username
        EditableField.EMAIL -> this?.email
        EditableField.FIRST_NAME -> this?.firstName
        EditableField.LAST_NAME -> this?.lastName
        EditableField.ADDRESS -> this?.address
        EditableField.CITY -> this?.city
        EditableField.STATE -> this?.state
        EditableField.COUNTRY -> this?.country
        EditableField.POSTAL_CODE -> this?.postalCode
        EditableField.HOME_NUMBER -> this?.homeNumber
        EditableField.WORK_NUMBER -> this?.workNumber
        EditableField.OFFICE_NUMBER -> this?.officeNumber
        EditableField.MOBILE_NUMBER -> this?.mobileNumber
    }
    return raw?.trim().orEmpty()
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White.copy(alpha = 0.05f),
        tonalElevation = 0.dp,
        shadowElevation = 18.dp,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        )
    ) {
        Box(
            Modifier
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.12f),
                        0.25f to Color.White.copy(alpha = 0.06f),
                        1f to Color.Transparent
                    ),
                    shape = shape
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(80f, 80f),
                        radius = 380f
                    ),
                    shape = shape
                )
                .padding(1.dp)
                .border(BorderStroke(1.dp, cs.outline.copy(alpha = 0.12f)), shape)
                .padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}
