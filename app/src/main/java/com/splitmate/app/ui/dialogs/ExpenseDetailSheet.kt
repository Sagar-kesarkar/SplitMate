package com.splitmate.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseDetailSheet(
    expense: Expense,
    payer: User?,
    group: Group?,
    users: List<User>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(expense.dateMillis))

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Expense", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${expense.title}\"? This action cannot be undone and will recalculate all balances.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteExpense(expense.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SplitRose),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

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
                .testTag("expense_detail_dialog"),
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
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(expense.category.color.copy(alpha = 0.2f))
                            .border(1.dp, expense.category.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = expense.category.icon, fontSize = 22.sp)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_expense_detail_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title & Total Amount
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.testTag("expense_detail_title")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = CurrencyUtils.formatINR(expense.amount),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = SplitEmeraldLight,
                    modifier = Modifier.testTag("expense_detail_amount")
                )

                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (group != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SplitIndigo.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Group: ${group.icon} ${group.name}",
                            color = SplitIndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Paid By Info
                Text("Paid By", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(payer?.avatarColor ?: SplitIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(payer?.initials ?: "??", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${if (payer?.id == currentUserId) "You" else payer?.name ?: "Unknown"} paid ${CurrencyUtils.formatINR(expense.amount)}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Split Breakdown List
                val splitMethodLabel = expense.splitType.label
                Text("Split Breakdown • $splitMethodLabel (${expense.participantIds.size} participants)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(8.dp))

                val totalShares = if (expense.splitType == SplitType.SHARES) {
                    expense.customSplits.values.sum().coerceAtLeast(1.0)
                } else 1.0

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    expense.participantIds.forEach { userId ->
                        val user = users.find { it.id == userId }
                        val shareAmount = when (expense.splitType) {
                            SplitType.EQUAL -> if (expense.participantIds.isNotEmpty()) expense.amount / expense.participantIds.size else 0.0
                            SplitType.EXACT -> expense.customSplits[userId] ?: 0.0
                            SplitType.PERCENT -> (expense.amount * (expense.customSplits[userId] ?: 0.0)) / 100.0
                            SplitType.SHARES -> (expense.amount * (expense.customSplits[userId] ?: 1.0)) / totalShares
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(user?.avatarColor ?: SplitIndigo),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(user?.initials ?: "??", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (user?.id == currentUserId) "You" else user?.name ?: "Member",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = CurrencyUtils.formatINR(shareAmount),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceCardElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Note", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(expense.notes, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Edit and Delete Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onEditExpense(expense) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("edit_expense_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("delete_expense_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitRoseLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitRose.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
