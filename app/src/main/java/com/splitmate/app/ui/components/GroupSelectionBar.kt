package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.ui.theme.SplitRoseLight
import com.splitmate.app.ui.theme.SurfaceDark
import com.splitmate.app.ui.theme.TextPrimary
import com.splitmate.app.ui.theme.TextSecondary

@Composable
fun GroupSelectionBar(
    selectedCount: Int,
    showUnmute: Boolean,
    onClose: () -> Unit,
    onMute: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SelectionActionButton(
            icon = { Icon(Icons.Default.Close, contentDescription = null) },
            label = "Close selection",
            onClick = onClose,
            modifier = Modifier.testTag("group_selection_close")
        )
        Text(
            text = "$selectedCount selected",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("group_selection_count")
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionActionButton(
                icon = {
                    Icon(
                        if (showUnmute) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null
                    )
                },
                label = if (showUnmute) "Unmute selected groups" else "Mute selected groups",
                onClick = onMute,
                modifier = Modifier.testTag("group_selection_mute")
            )
            SelectionActionButton(
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                label = "Leave selected groups",
                onClick = onLeave,
                modifier = Modifier.testTag("group_selection_leave")
            )
            SelectionActionButton(
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SplitRoseLight) },
                label = "Delete selected groups",
                onClick = onDelete,
                modifier = Modifier.testTag("group_selection_delete")
            )
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = label }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides TextSecondary
        ) {
            icon()
        }
    }
}
