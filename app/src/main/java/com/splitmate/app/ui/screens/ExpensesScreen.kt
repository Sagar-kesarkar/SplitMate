package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Category
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.ExpenseItemRow
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    users: List<User>,
    groups: List<Group>,
    currentUserId: String,
    searchQuery: String,
    selectedCategory: Category?,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (Category?) -> Unit,
    onExpenseClick: (String) -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredExpenses = expenses.filter { exp ->
        val matchesCategory = selectedCategory == null || exp.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                exp.title.contains(searchQuery, ignoreCase = true) ||
                exp.notes.contains(searchQuery, ignoreCase = true) ||
                (users.find { it.id == exp.paidByUserId }?.name?.contains(searchQuery, ignoreCase = true) == true)
        matchesCategory && matchesSearch
    }

    val totalFilteredSpend = filteredExpenses.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("expenses_screen")
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search expenses, notes, payers...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_bar"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SplitIndigo,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("All (${expenses.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SplitIndigo.copy(alpha = 0.35f),
                            selectedLabelColor = SplitIndigoLight,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("filter_all_categories")
                    )
                }

                items(Category.values()) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onCategoryFilterChange(if (isSelected) null else cat)
                        },
                        label = { Text("${cat.icon} ${cat.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.color.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimary,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) cat.color else SurfaceBorder
                        ),
                        modifier = Modifier.testTag("filter_cat_${cat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick summary banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredExpenses.size} ${if (filteredExpenses.size == 1) "transaction" else "transactions"}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Total: ${CurrencyUtils.formatINR(totalFilteredSpend)}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

        // Expenses List
        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching expenses",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Try adjusting your search query or category filters.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = onAddExpenseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredExpenses) { expense ->
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
