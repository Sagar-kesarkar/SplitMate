package com.splitmate.app.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.splitmate.app.model.Group
import com.splitmate.app.model.GroupLifecyclePolicy
import com.splitmate.app.model.MuteDuration
import com.splitmate.app.ui.theme.SplitEmerald
import com.splitmate.app.ui.theme.SplitIndigo
import com.splitmate.app.ui.theme.SplitRose
import com.splitmate.app.ui.theme.SplitRoseLight
import com.splitmate.app.ui.theme.SurfaceCardElevated
import com.splitmate.app.ui.theme.TextPrimary
import com.splitmate.app.ui.theme.TextSecondary
import com.splitmate.app.viewmodel.GroupActionType

@Composable
fun GroupActionDialog(
    type: GroupActionType,
    selectedGroups: List<Group>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onMute: (MuteDuration) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onOfferLeave: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    when (type) {
        GroupActionType.MUTE -> MuteGroupsDialog(selectedGroups.size, onDismiss, onMute)
        GroupActionType.LEAVE -> {
            val eligible = selectedGroups.filter { GroupLifecyclePolicy.canLeave(it, currentUserId) }
            val blocked = selectedGroups - eligible.toSet()
            ConfirmGroupActionDialog(
                title = "Leave Group?",
                description = leaveDescription(selectedGroups, eligible, blocked),
                confirmLabel = "Leave Group",
                confirmEnabled = eligible.isNotEmpty(),
                destructive = true,
                onDismiss = onDismiss,
                onConfirm = onLeave,
                testTag = "leave_group_dialog"
            )
        }
        GroupActionType.DELETE -> {
            val eligible = selectedGroups.filter { GroupLifecyclePolicy.canDelete(it, currentUserId) }
            val blocked = selectedGroups - eligible.toSet()
            ConfirmGroupActionDialog(
                title = "Delete Group?",
                description = deleteDescription(selectedGroups, eligible, blocked),
                confirmLabel = "Delete Group",
                confirmEnabled = eligible.isNotEmpty(),
                destructive = true,
                secondaryLabel = if (blocked.isNotEmpty()) "Leave instead" else null,
                onSecondary = onOfferLeave,
                onDismiss = onDismiss,
                onConfirm = onDelete,
                testTag = "delete_group_dialog"
            )
        }
    }
}

@Composable
private fun MuteGroupsDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onMute: (MuteDuration) -> Unit
) {
    var duration by rememberSaveable { mutableStateOf(MuteDuration.ONE_HOUR) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardElevated,
        title = { Text("Mute notifications", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedCount > 1) {
                    Text("Apply to $selectedCount selected groups", color = TextSecondary)
                }
                MuteDuration.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mute_option_${option.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = duration == option,
                            onClick = { duration = option },
                            colors = RadioButtonDefaults.colors(selectedColor = SplitIndigo),
                            modifier = Modifier.testTag("mute_radio_${option.name.lowercase()}")
                        )
                        Text(option.displayName, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMute(duration) },
                colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo),
                modifier = Modifier.testTag("confirm_mute_groups")
            ) { Text("Mute") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        modifier = Modifier.testTag("mute_group_dialog")
    )
}

@Composable
private fun ConfirmGroupActionDialog(
    title: String,
    description: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    testTag: String,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardElevated,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(description, color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) SplitRose else SplitEmerald,
                    contentColor = Color.White,
                    disabledContainerColor = SplitRose.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("confirm_${testTag}")
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (secondaryLabel != null) {
                    TextButton(onClick = onSecondary) { Text(secondaryLabel, color = SplitRoseLight) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
            }
        },
        modifier = Modifier.testTag(testTag)
    )
}

private fun leaveDescription(selected: List<Group>, eligible: List<Group>, blocked: List<Group>): String {
    val subject = if (selected.size == 1) {
        "Are you sure you want to leave \"${selected.first().name}\"?\nYou will no longer be a member of this group."
    } else {
        "You are about to leave ${eligible.size} of ${selected.size} selected groups."
    }
    return buildString {
        append(subject)
        if (blocked.isNotEmpty()) {
            append("\n\nCannot leave: ")
            append(blocked.joinToString { "${it.name} (you own this group; transfer ownership or delete it)" })
        }
    }
}

private fun deleteDescription(selected: List<Group>, eligible: List<Group>, blocked: List<Group>): String {
    val subject = if (selected.size == 1) {
        "Are you sure you want to delete \"${selected.first().name}\"?\n\nThis will remove the group and its associated local data. You can undo this action briefly."
    } else {
        "${eligible.size} of ${selected.size} selected groups will be removed with their associated local data. You can undo this action briefly."
    }
    return buildString {
        append(subject)
        if (blocked.isNotEmpty()) {
            append("\n\nNot owned by you: ")
            append(blocked.joinToString { it.name })
            append(". Use Leave instead.")
        }
    }
}
