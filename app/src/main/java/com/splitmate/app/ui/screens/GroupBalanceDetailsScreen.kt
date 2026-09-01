package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.repository.BalanceTimelineBuilder
import com.splitmate.app.data.repository.MemberBalanceChange
import com.splitmate.app.model.BalanceEventType
import com.splitmate.app.model.BalanceHistoryEvent
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupBalanceDetailsScreen(
    group: Group,
    users: List<User>,
    currentUserId: String,
    history: List<BalanceHistoryEvent>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeline = BalanceTimelineBuilder.build(history, currentUserId)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Balance Details", fontWeight = FontWeight.Bold); Text(group.name, color = TextSecondary, fontSize = 11.sp) } },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Group Details") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.testTag("group_balance_details_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Auditable timeline", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            if (timeline.entries.isEmpty()) item {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), SurfaceCard, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) {
                    Text("No balance events have been recorded for this group.", modifier = Modifier.padding(16.dp), color = TextSecondary)
                }
            }
            items(timeline.entries, key = { "${it.sourceId}_${it.eventType}_${it.dateMillis}" }) { entry ->
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), SurfaceCard, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${eventLabel(entry.eventType)} • ${formatDate(entry.dateMillis)} • ${group.name}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Text(CurrencyUtils.formatINR(entry.fullAmount), color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        val payer = users.find { it.id == entry.paidByUserId }?.let { if (it.id == currentUserId) "You" else it.name } ?: "Unknown member"
                        val isSettlement = entry.eventType == BalanceEventType.SETTLEMENT ||
                            entry.eventType == BalanceEventType.SETTLEMENT_REVERSAL
                        Text(
                            if (isSettlement) "$payer made the settlement payment" else "$payer paid the full bill",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (entry.allocations.isNotEmpty()) {
                            Text("Split allocations", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            entry.allocations.forEach { (userId, amount) ->
                                val name = users.find { it.id == userId }?.let { if (it.id == currentUserId) "You" else it.name } ?: userId
                                Text("$name  ${CurrencyUtils.formatINR(amount)}", color = TextSecondary, fontSize = 11.sp)
                            }
                        } else {
                            Text("Settlement payment ${CurrencyUtils.formatINR(entry.fullAmount)}", color = TextSecondary, fontSize = 11.sp)
                        }
                        entry.changes.forEach { change ->
                            BalanceChangeBlock(change, users, currentUserId, isSettlement)
                        }
                    }
                }
            }
            item { Text("Final balance per member", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp)) }
            items(timeline.finalBalances.entries.sortedBy { users.find { user -> user.id == it.key }?.name }) { (memberId, balance) ->
                val member = users.find { it.id == memberId }
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), SurfaceCardElevated, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(member?.name ?: memberId, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(balanceState(balance, member?.name ?: "Member"), color = balanceColor(balance), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BalanceChangeBlock(
    change: MemberBalanceChange,
    users: List<User>,
    currentUserId: String,
    isSettlement: Boolean
) {
    val memberName = users.find { it.id == change.memberId }?.let { if (it.id == currentUserId) "You" else it.name } ?: change.memberId
    HorizontalDivider(color = SurfaceBorder.copy(alpha = .6f))
    Text(
        if (isSettlement) "Signed settlement effect for $memberName: ${signed(change.delta)}"
        else owingExplanation(memberName, change.delta),
        color = balanceColor(change.delta),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
    Text("Before: ${signed(change.before)}  •  Change: ${signed(change.delta)}  •  After: ${balanceState(change.after, memberName)}", color = TextSecondary, fontSize = 11.sp)
}

private fun eventLabel(type: BalanceEventType) = when (type) {
    BalanceEventType.EXPENSE -> "Expense"
    BalanceEventType.EDIT_REVERSAL -> "Expense edit reversal"
    BalanceEventType.EDIT_APPLIED -> "Edited expense"
    BalanceEventType.DELETION_ADJUSTMENT -> "Expense deletion reversal"
    BalanceEventType.SETTLEMENT -> "Settlement"
    BalanceEventType.SETTLEMENT_REVERSAL -> "Settlement reversal"
}
private fun formatDate(value: Long) = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(value))
private fun signed(value: Double) = when { value > .005 -> "+${CurrencyUtils.formatINR(value)}"; value < -.005 -> "-${CurrencyUtils.formatINR(abs(value))}"; else -> CurrencyUtils.formatINR(0.0) }
private fun balanceState(value: Double, member: String) = when { value > .005 -> "$member owes You ${signed(value)}"; value < -.005 -> "You owe $member ${signed(value)}"; else -> "Settled ${CurrencyUtils.formatINR(0.0)}" }
private fun owingExplanation(member: String, delta: Double) = when {
    delta > .005 -> "$member's share increased what they owe You by ${CurrencyUtils.formatINR(delta)}"
    delta < -.005 -> "Your share increased what You owe $member by ${CurrencyUtils.formatINR(abs(delta))}"
    else -> "No balance change"
}
private fun balanceColor(value: Double): Color = when { value > .005 -> SplitEmeraldLight; value < -.005 -> SplitRoseLight; else -> TextSecondary }
