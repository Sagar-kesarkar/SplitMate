package com.splitmate.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.BalanceCard
import com.splitmate.app.ui.components.ExpenseItemRow
import com.splitmate.app.ui.components.GroupCard
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    totalOwed: Double,
    totalOwe: Double,
    netBalance: Double,
    groups: List<Group>,
    expenses: List<Expense>,
    settlements: List<Settlement>,
    users: List<User>,
    currentUserId: String,
    onAddExpenseClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onViewAllExpensesClick: () -> Unit,
    onViewAllGroupsClick: () -> Unit,
    onPersonalExpensesClick: () -> Unit,
    personalExpenseTotalMonth: Double,
    calculateGroupBalance: (String) -> Double,
    calculateGroupTotalSpend: (String) -> Double,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Overview Hero Card
        item {
            BalanceCard(
                totalOwed = totalOwed,
                totalOwe = totalOwe,
                netBalance = netBalance,
                onAddExpenseClick = onAddExpenseClick,
                onSettleUpClick = onSettleUpClick
            )
        }

        // Personal Expenses Card
        item {
            Surface(
                onClick = onPersonalExpensesClick,
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SplitIndigo.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = SplitIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personal Expenses",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = "Track your own spending",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatINR(-personalExpenseTotalMonth),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SplitRoseLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Shortcuts Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.GroupAdd,
                    label = "New Group",
                    onClick = onCreateGroupClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_new_group_action")
                )
                QuickActionChip(
                    icon = Icons.Default.PersonAdd,
                    label = "Add Friend",
                    onClick = onAddFriendClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_add_friend_action")
                )
            }
        }

        // Active Groups Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Groups",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )

                if (groups.isNotEmpty()) {
                    Text(
                        text = "See all (${groups.size})",
                        color = SplitIndigoLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onViewAllGroupsClick() }
                            .testTag("see_all_groups_button")
                    )
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "👥", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No groups yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Create a group for your apartment, trip, or friends",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onCreateGroupClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo)
                        ) {
                            Text("Create Group")
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(groups.take(4)) { group ->
                        val groupMembers = users.filter { group.memberIds.contains(it.id) }
                        val userBalance = calculateGroupBalance(group.id)
                        val totalSpend = calculateGroupTotalSpend(group.id)

                        GroupCard(
                            group = group,
                            members = groupMembers,
                            userBalance = userBalance,
                            totalGroupSpend = totalSpend,
                            onClick = { onGroupClick(group.id) },
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }
            }
        }

        // Recent Activity (Expenses + Settlements)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )

                if (expenses.isNotEmpty()) {
                    Text(
                        text = "View all (${expenses.size})",
                        color = SplitIndigoLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onViewAllExpensesClick() }
                            .testTag("see_all_expenses_button")
                    )
                }
            }
        }

        if (expenses.isEmpty() && settlements.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🧾", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Add your first split expense to start tracking",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onAddExpenseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                        ) {
                            Text("Add First Expense", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Recent settlements indicator if any
            if (settlements.isNotEmpty()) {
                val latestSettle = settlements.first()
                val payer = users.find { it.id == latestSettle.payerId }
                val receiver = users.find { it.id == latestSettle.receiverId }
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val settleDate = dateFormat.format(Date(latestSettle.dateMillis))

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitEmerald.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SplitEmerald.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SplitEmeraldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (payer?.id == currentUserId) "You" else payer?.name ?: "Someone"} paid ${if (receiver?.id == currentUserId) "You" else receiver?.name ?: "Someone"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Settlement • ${latestSettle.paymentMethod} • $settleDate",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatINR(latestSettle.amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SplitEmeraldLight
                            )
                        }
                    }
                }
            }

            items(expenses.take(6)) { expense ->
                val payer = users.find { it.id == expense.paidByUserId }
                val group = groups.find { it.id == expense.groupId }

                ExpenseItemRow(
                    expense = expense,
                    payerUser = payer,
                    group = group,
                    currentUserId = currentUserId,
                    onClick = { onExpenseClick(expense.id) }
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SplitIndigoLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
