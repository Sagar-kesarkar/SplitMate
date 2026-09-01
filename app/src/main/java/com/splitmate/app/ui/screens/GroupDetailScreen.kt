package com.splitmate.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.DebtTransfer
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.ExpenseItemRow
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.components.SplitmateHeader
import com.splitmate.app.ui.theme.*
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.util.CurrencyUtils
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    users: List<User>,
    expenses: List<Expense>,
    currentUserId: String,
    userBalance: Double,
    totalSpend: Double,
    youAreOwed: Double,
    youOwe: Double,
    simplifiedDebts: List<DebtTransfer>,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onSettleUpClick: (String, String?) -> Unit,
    onExpenseClick: (String) -> Unit,
    onSetBudget: (Double?) -> Unit,
    onAddMember: (String) -> Unit,
    onBalanceDetailsClick: () -> Unit,
    onUnavailableFeature: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupExpenses = expenses.filter { it.groupId == group.id }
    val groupMembers = users.filter { group.memberIds.contains(it.id) }
    var showSimplifyDebts by remember { mutableStateOf(true) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var budgetText by remember(group.budget) { mutableStateOf(group.budget?.toString().orEmpty()) }
    var budgetError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showBudgetDialog || showMemberDialog) {
        showBudgetDialog = false
        showMemberDialog = false
    }

    if (showBudgetDialog) AlertDialog(
        onDismissRequest = { showBudgetDialog = false },
        title = { Text(if (group.budget == null) "Set Group Budget" else "Edit Group Budget") },
        text = { OutlinedTextField(value = budgetText, onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' }; budgetError = null }, label = { Text("Budget (₹)") }, singleLine = true, isError = budgetError != null, supportingText = { budgetError?.let { Text(it) } }) },
        confirmButton = { TextButton(onClick = { val value = budgetText.toDoubleOrNull(); if (value == null || value < 0.0) budgetError = "Enter a valid non-negative budget" else { onSetBudget(value); showBudgetDialog = false } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") } }
    )
    if (showMemberDialog) AlertDialog(onDismissRequest = { showMemberDialog = false }, title = { Text("Add Member") }, text = { val available = users.filter { !it.isCurrentUser && it.id !in group.memberIds }; if (available.isEmpty()) Text("All available friends are already members.", color = TextSecondary) else LazyColumn(Modifier.heightIn(max = 360.dp)) { items(available) { user -> Row(Modifier.fillMaxWidth().clickable { onAddMember(user.id); showMemberDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(user.avatar.ifBlank { "👤" }, fontSize = 20.sp); Spacer(Modifier.width(10.dp)); Text(user.name, color = TextPrimary) } } } }, confirmButton = { TextButton(onClick = { showMemberDialog = false }) { Text("Close") } })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = group.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${groupMembers.size} members",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("group_detail_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChatClick(group.id) }
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text("Chat", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.testTag("group_detail_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Overview
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Group Overview", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GROUP BUDGET", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = group.budget?.let(CurrencyUtils::formatINR) ?: "No budget set",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val remaining = BalanceCalculator.budgetRemaining(group.budget, totalSpend)
                                Text(if (remaining != null && remaining < 0) "OVER BUDGET" else "REMAINING", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = remaining?.let { CurrencyUtils.formatINR(abs(it)) } ?: "—",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining != null && remaining < 0) SplitRoseLight else TextPrimary
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        val progress = if ((group.budget ?: 0.0) > 0) (totalSpend / group.budget!!).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = if (group.budget != null && totalSpend > group.budget) SplitRoseLight else SplitIndigoLight, trackColor = SurfaceBorder)
                        Row(Modifier.fillMaxWidth().padding(top = 5.dp), Arrangement.SpaceBetween) { Text("Total Spent", color = TextSecondary, fontSize = 12.sp); Text(CurrencyUtils.formatINR(totalSpend), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("You are owed", color = TextSecondary, fontSize = 10.sp); Text(CurrencyUtils.formatINR(youAreOwed), color = SplitEmeraldLight, fontWeight = FontWeight.Bold) }; Column { Text("You owe", color = TextSecondary, fontSize = 10.sp); Text(CurrencyUtils.formatINR(youOwe), color = SplitRoseLight, fontWeight = FontWeight.Bold) }; Column(horizontalAlignment = Alignment.End) { Text("Net balance", color = TextSecondary, fontSize = 10.sp); Text(when { userBalance > .005 -> "+${CurrencyUtils.formatINR(userBalance)}"; userBalance < -.005 -> "-${CurrencyUtils.formatINR(abs(userBalance))}"; else -> CurrencyUtils.formatINR(0.0) }, color = when { userBalance > .005 -> SplitEmeraldLight; userBalance < -.005 -> SplitRoseLight; else -> TextPrimary }, fontWeight = FontWeight.Bold) } }
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showBudgetDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(if (group.budget == null) "Set Budget" else "Edit Budget", color = SplitIndigoLight) }
                            TextButton(onClick = onBalanceDetailsClick, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("See Balance Details", color = SplitIndigoLight, maxLines = 1) }
                        }

                        if (group.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(group.description, color = TextMuted, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onAddExpenseClick(group.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { onSettleUpClick(group.id, null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Icon(Icons.Default.Paid, contentDescription = null, tint = SplitIndigoLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Settle Up", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Debt Simplification Card
            if (simplifiedDebts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitIndigo.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoGraph,
                                        contentDescription = null,
                                        tint = SplitIndigoLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Smart Debt Simplification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { showSimplifyDebts = !showSimplifyDebts },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showSimplifyDebts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle",
                                        tint = TextSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showSimplifyDebts) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Consolidated into minimal transfers to square up everyone:",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    simplifiedDebts.forEach { transfer ->
                                        val debtor = users.find { it.id == transfer.fromUserId }
                                        val creditor = users.find { it.id == transfer.toUserId }
                                        val isMeDebtor = transfer.fromUserId == currentUserId
                                        val isMeCreditor = transfer.toUserId == currentUserId
                                        
                                        if (isMeDebtor || isMeCreditor) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SurfaceCard,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isMeDebtor) debtor?.avatarColor ?: Color.Gray else creditor?.avatarColor ?: Color.Gray),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            if (isMeDebtor) debtor?.initials ?: "" else creditor?.initials ?: "",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (isMeDebtor) creditor?.name ?: "" else debtor?.name ?: "",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = if (isMeDebtor) {
                                                                buildAnnotatedString {
                                                                    withStyle(SpanStyle(color = SplitRoseLight, fontWeight = FontWeight.Bold)) {
                                                                        append("YOU OWE")
                                                                    }
                                                                }
                                                            } else {
                                                                buildAnnotatedString {
                                                                    withStyle(SpanStyle(color = SplitEmeraldLight, fontWeight = FontWeight.Bold)) {
                                                                        append("YOU ARE OWED")
                                                                    }
                                                                }
                                                            },
                                                            fontSize = 10.sp
                                                        )
                                                        Text(
                                                            text = if (isMeDebtor) "Pay ${creditor?.name?.split(" ")?.firstOrNull()} to settle" else "${debtor?.name?.split(" ")?.firstOrNull()} owes you",
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = CurrencyUtils.formatINR(transfer.amount),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isMeDebtor) SplitRoseLight else SplitEmeraldLight
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = (if (isMeDebtor) SplitIndigo else SurfaceCardElevated),
                                                            border = if (!isMeDebtor) androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder) else null,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    if (isMeDebtor) {
                                                                        onSettleUpClick(group.id, creditor?.id)
                                                                    } else {
                                                                        onUnavailableFeature()
                                                                    }
                                                                }
                                                        ) {
                                                            Text(
                                                                text = if (isMeDebtor) "Settle" else "Remind",
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMeDebtor) Color.White else SplitIndigoLight
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Group Members List
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(
                    text = "Group Members (${groupMembers.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                ); TextButton(onClick = { showMemberDialog = true }) { Text("+ Add Member", color = SplitIndigoLight) } }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupMembers.forEach { member ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.width(80.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(member.avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.initials, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (member.id == currentUserId) "You" else member.name.split(" ").firstOrNull() ?: member.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Group Expenses Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Expenses (${groupExpenses.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            if (groupExpenses.isEmpty()) {
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
                            Text("No expenses in this group yet", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Tap Add Expense to log the first shared bill.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(groupExpenses) { expense ->
                    val payer = users.find { it.id == expense.paidByUserId }
                    ExpenseItemRow(
                        expense = expense,
                        payerUser = payer,
                        group = group,
                        currentUserId = currentUserId,
                        onClick = { onExpenseClick(expense.id) },
                        useSignedEffectLabels = true,
                        showDetailChevron = true
                    )
                }
            }
        }
    }
}
