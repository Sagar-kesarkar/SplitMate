package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCard(
    group: Group,
    members: List<User>,
    userBalance: Double,
    totalGroupSpend: Double,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                role = if (selectionMode) Role.Checkbox else Role.Button
                selected = isSelected
            }
            .testTag("group_card_${group.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) SplitEmeraldLight else SurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectionMode) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SplitEmerald else Color.Transparent)
                                .border(
                                    width = if (isSelected) 0.dp else 1.5.dp,
                                    color = if (isSelected) Color.Transparent else TextSecondary,
                                    shape = CircleShape
                                )
                                .testTag("group_selection_indicator_${group.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SplitIndigo.copy(alpha = 0.2f))
                            .border(1.dp, SplitIndigo.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = group.icon, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.memberIds.size} members • Total ${CurrencyUtils.formatINR(totalGroupSpend)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isMuted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.testTag("group_muted_${group.id}")
                        ) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = "Notifications muted",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Muted", fontSize = 9.sp, color = TextMuted)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    when {
                        userBalance > 0.01 -> {
                            Text(
                                text = "you are owed",
                                fontSize = 10.sp,
                                color = SplitEmeraldLight,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+${CurrencyUtils.formatINR(userBalance)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitEmeraldLight
                            )
                        }
                        userBalance < -0.01 -> {
                            Text(
                                text = "you owe",
                                fontSize = 10.sp,
                                color = SplitRoseLight,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "-${CurrencyUtils.formatINR(abs(userBalance))}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitRoseLight
                            )
                        }
                        else -> {
                            Text(
                                text = "settled up",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                members.take(5).forEachIndexed { index, member ->
                    Box(
                        modifier = Modifier
                            .offset(x = (-index * 8).dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(member.avatarColor)
                            .border(1.5.dp, SurfaceCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initials,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (members.size > 5) {
                    Text(
                        text = "+${members.size - 5} more",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
