package com.splitmate.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*

@Composable
fun CreateGroupDialog(
    users: List<User>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, description: String, icon: String, memberIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🏠") }
    val otherUsers = remember(users, currentUserId) { users.filter { it.id != currentUserId } }
    var selectedMemberIds by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val icons = listOf("🏠", "🏖️", "🍕", "🚗", "💼", "🎉", "✈️", "🍿", "⚽", "🛒")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .testTag("create_group_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Group",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_create_group_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emoji Picker
                Text(
                    text = "Group Icon",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(icons) { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SplitIndigo.copy(alpha = 0.35f) else SurfaceCard)
                                .border(
                                    1.5.dp,
                                    if (isSelected) SplitIndigoLight else SurfaceBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = icon, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Hawaii Trip, Flat 402, Brunch Squad") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = SplitEmerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("What is this group for?") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitIndigo,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Members
                Text(
                    text = "Add Group Members (${selectedMemberIds.size + 1} total)",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceCardElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SplitEmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("You (Admin & Member)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    otherUsers.forEach { user ->
                        val isChecked = selectedMemberIds.contains(user.id)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChecked) SurfaceCardElevated else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChecked) SplitIndigo.copy(alpha = 0.5f) else SurfaceBorder.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedMemberIds = if (isChecked) {
                                        selectedMemberIds - user.id
                                    } else {
                                        selectedMemberIds + user.id
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedMemberIds = if (checked) selectedMemberIds + user.id else selectedMemberIds - user.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SplitIndigo,
                                        uncheckedColor = TextMuted
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(user.avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(user.initials, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(user.name, color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = SplitRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Please enter a group name."
                        } else {
                            onCreateGroup(
                                name.trim(),
                                description.trim(),
                                selectedIcon,
                                (selectedMemberIds + currentUserId).toList()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_create_group_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
