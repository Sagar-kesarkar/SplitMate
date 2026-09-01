package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Group
import com.splitmate.app.model.GroupLifecyclePolicy
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.GroupCard
import com.splitmate.app.ui.theme.*

@Composable
fun GroupsScreen(
    groups: List<Group>,
    users: List<User>,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    selectedGroupIds: Set<String> = emptySet(),
    onGroupLongPress: (String) -> Unit = {},
    onToggleGroupSelection: (String) -> Unit = {},
    calculateGroupBalance: (String) -> Double,
    calculateGroupTotalSpend: (String) -> Double,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val selectionMode = selectedGroupIds.isNotEmpty()
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("groups_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Groups",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${groups.size} active shared spaces",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onCreateGroupClick,
                colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_group_top_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Group", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🏠", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No groups yet",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Create groups to share apartment bills, road trips, or dining expenses with multiple friends.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                    )
                    Button(
                        onClick = onCreateGroupClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Your First Group", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups) { group ->
                    val members = users.filter { group.memberIds.contains(it.id) }
                    val balance = calculateGroupBalance(group.id)
                    val totalSpend = calculateGroupTotalSpend(group.id)

                    GroupCard(
                        group = group,
                        members = members,
                        userBalance = balance,
                        totalGroupSpend = totalSpend,
                        onClick = {
                            if (selectionMode) onToggleGroupSelection(group.id)
                            else onGroupClick(group.id)
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onGroupLongPress(group.id)
                        },
                        selectionMode = selectionMode,
                        isSelected = group.id in selectedGroupIds,
                        isMuted = GroupLifecyclePolicy.isMuted(group, System.currentTimeMillis())
                    )
                }
            }
        }
    }
}
