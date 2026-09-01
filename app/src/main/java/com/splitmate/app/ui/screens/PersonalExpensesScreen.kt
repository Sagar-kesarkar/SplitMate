package com.splitmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.model.*
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PersonalHistoryFilter { ALL, GROUPS, PERSONAL }
private data class HistoryItem(val time: Long, val personal: PersonalExpense? = null, val shared: Expense? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalExpensesScreen(
    expenses: List<PersonalExpense>, sharedExpenses: List<Expense>, users: List<User>, groups: List<Group>,
    currentUserId: String, totalMonth: Double, onBackClick: () -> Unit, onAddExpenseClick: () -> Unit,
    onExpenseClick: (PersonalExpense) -> Unit, onSharedExpenseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(PersonalHistoryFilter.ALL) }
    val categoryTotals = BalanceCalculator.personalCategoryTotals(expenses).toList().sortedByDescending { it.second }
    val history = buildList {
        if (filter != PersonalHistoryFilter.GROUPS) expenses.forEach { add(HistoryItem(it.dateMillis, personal = it)) }
        if (filter != PersonalHistoryFilter.PERSONAL) sharedExpenses.forEach { add(HistoryItem(it.dateMillis, shared = it)) }
    }.sortedByDescending { it.time }
    Scaffold(topBar = { TopAppBar(title = { Text("Personal Expenses", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, actions = { IconButton(onClick = onAddExpenseClick) { Icon(Icons.Default.Add, "Add Personal Expense", tint = SplitIndigoLight) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)) }, containerColor = BackgroundDark, modifier = modifier.testTag("personal_expenses_screen")) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SummaryCard(totalMonth) }
            item { SegmentedFilter(filter) { filter = it } }
            if (filter != PersonalHistoryFilter.GROUPS && categoryTotals.isNotEmpty()) item { CategoryCard(categoryTotals, totalMonth) }
            item { Text("Recent Expenses", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp)) }
            if (history.isEmpty()) item { Text("No expenses for this filter", color = TextSecondary, modifier = Modifier.padding(24.dp)) }
            items(history, key = { it.personal?.id ?: it.shared!!.id }) { item ->
                item.personal?.let { PersonalRow(it) { onExpenseClick(it) } }
                item.shared?.let { expense -> SharedRow(expense, users, groups, currentUserId) { onSharedExpenseClick(expense.id) } }
            }
        }
    }
}

@Composable private fun SummaryCard(total: Double) = Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SurfaceCardElevated, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase(), color = TextSecondary, fontSize = 11.sp); Text("Monthly Spending", color = TextPrimary, fontWeight = FontWeight.Bold) }; Text(CurrencyUtils.formatINR(total), color = SplitRoseLight, fontWeight = FontWeight.Black, fontSize = 23.sp) } }
@Composable private fun SegmentedFilter(selected: PersonalHistoryFilter, onSelect: (PersonalHistoryFilter) -> Unit) = Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceCardElevated).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { PersonalHistoryFilter.entries.forEach { value -> val active = value == selected; Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (active) SplitIndigo else SurfaceCard).clickable { onSelect(value) }.testTag("personal_filter_${value.name.lowercase()}"), contentAlignment = Alignment.Center) { Text(value.name.lowercase().replaceFirstChar(Char::uppercase), color = if (active) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) } } }
@Composable private fun CategoryCard(values: List<Pair<Category, Double>>, total: Double) = Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SurfaceCardElevated, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { Text("Spending by Category", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp); values.forEach { (category, amount) -> val fraction = if (total <= 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f); Row(verticalAlignment = Alignment.CenterVertically) { Text(category.icon, fontSize = 20.sp, modifier = Modifier.width(34.dp)); Column(Modifier.weight(1f)) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(category.label, color = TextPrimary, fontSize = 13.sp); Text("${CurrencyUtils.formatINR(amount)}  ${(fraction * 100).roundToInt()}%", color = SplitRoseLight, fontSize = 12.sp, fontWeight = FontWeight.Bold) }; LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = category.color, trackColor = SurfaceBorder) } } } } }
@Composable private fun PersonalRow(expense: PersonalExpense, onClick: () -> Unit) = Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick), RoundedCornerShape(13.dp), SurfaceCard, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(expense.category.icon, fontSize = 22.sp, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { Text(expense.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${expense.category.label} • ${date(expense.dateMillis)}", color = TextSecondary, fontSize = 11.sp) }; Text(CurrencyUtils.formatINR(expense.amount), color = SplitRoseLight, fontWeight = FontWeight.Bold) } }
@Composable private fun SharedRow(expense: Expense, users: List<User>, groups: List<Group>, me: String, onClick: () -> Unit) { val payer = users.find { it.id == expense.paidByUserId }; val group = groups.find { it.id == expense.groupId }; val shares = BalanceCalculator.shares(expense); val effect = when { expense.paidByUserId == me -> shares.filterKeys { it != me }.values.sum(); me in expense.participantIds -> -(shares[me] ?: 0.0); else -> 0.0 }; Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick), RoundedCornerShape(13.dp), SurfaceCard, border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(expense.category.icon, fontSize = 22.sp, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { Text(expense.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("${group?.name ?: "Shared"} • ${if (expense.paidByUserId == me) "You" else payer?.name ?: "Someone"} paid ${CurrencyUtils.formatINR(expense.amount)}", color = TextSecondary, fontSize = 11.sp); Text(date(expense.dateMillis), color = TextMuted, fontSize = 10.sp) }; Column(horizontalAlignment = Alignment.End) { Text(when { effect > .005 -> "you are owed +${CurrencyUtils.formatINR(effect)}"; effect < -.005 -> "you owe -${CurrencyUtils.formatINR(abs(effect))}"; else -> "settled ${CurrencyUtils.formatINR(0.0)}" }, color = when { effect > .005 -> SplitEmeraldLight; effect < -.005 -> SplitRoseLight; else -> TextSecondary }, fontSize = 11.sp, fontWeight = FontWeight.Bold); Icon(Icons.Default.ChevronRight, "Details", tint = TextMuted, modifier = Modifier.size(18.dp)) } } } }
private fun date(value: Long) = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(value))
