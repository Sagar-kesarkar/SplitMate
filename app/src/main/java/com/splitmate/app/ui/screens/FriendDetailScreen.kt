package com.splitmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.ExpenseItemRow
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    friend: User,
    users: List<User>,
    groups: List<Group>,
    expenses: List<Expense>,
    currentUserId: String,
    balance: Double,
    onBackClick: () -> Unit,
    onSettleUpClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onSendReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedExpenses = expenses.filter { exp ->
        (exp.paidByUserId == friend.id && exp.participantIds.contains(currentUserId)) ||
                (exp.paidByUserId == currentUserId && exp.participantIds.contains(friend.id)) ||
                (exp.participantIds.contains(friend.id) && exp.participantIds.contains(currentUserId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(friend.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.initials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = friend.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (friend.email.isNotBlank()) {
                                Text(friend.email, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("friend_detail_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.testTag("friend_detail_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Friend Balance Overview Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                balance > 0.01 -> "${friend.name} owes you"
                                balance < -0.01 -> "You owe ${friend.name}"
                                else -> "You and ${friend.name} are all settled up"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when {
                                balance > 0.01 -> "+${CurrencyUtils.formatINR(balance)}"
                                balance < -0.01 -> "-${CurrencyUtils.formatINR(abs(balance))}"
                                else -> CurrencyUtils.formatINR(0.0)
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                balance > 0.01 -> SplitEmeraldLight
                                balance < -0.01 -> SplitRoseLight
                                else -> TextPrimary
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onSettleUpClick(friend.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Settle Up", fontWeight = FontWeight.Bold)
                            }

                            if (balance > 0.01) {
                                OutlinedButton(
                                    onClick = onSendReminder,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitIndigoLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SplitIndigo.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Reminder", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Shared History Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shared Expenses (${sharedExpenses.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            if (sharedExpenses.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🧾", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No shared expenses yet", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Expenses involving both of you will appear here.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(sharedExpenses) { expense ->
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
}
