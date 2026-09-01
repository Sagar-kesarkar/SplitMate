package com.splitmate.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.splitmate.app.model.Category
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    users: List<User>,
    groups: List<Group>,
    currentUserId: String,
    prefilledGroupId: String? = null,
    existingExpense: Expense? = null,
    onDismiss: () -> Unit,
    onSaveExpense: (
        id: String?,
        title: String,
        amount: Double,
        paidByUserId: String,
        groupId: String?,
        category: Category,
        participantIds: List<String>,
        splitType: SplitType,
        customSplits: Map<String, Double>,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (existingExpense != null && existingExpense.amount > 0) String.format(java.util.Locale.US, "%.2f", existingExpense.amount)
            else ""
        )
    }
    var selectedGroupId by remember { mutableStateOf(existingExpense?.groupId ?: prefilledGroupId) }
    var paidByUserId by remember {
        mutableStateOf(existingExpense?.paidByUserId ?: currentUserId)
    }
    var selectedCategory by remember { mutableStateOf(existingExpense?.category ?: Category.FOOD) }
    var splitType by remember { mutableStateOf(existingExpense?.splitType ?: SplitType.EQUAL) }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }

    val availableUsers = remember(selectedGroupId, groups, users) {
        val group = groups.find { it.id == selectedGroupId }
        if (group != null) {
            users.filter { group.memberIds.contains(it.id) }
        } else if (existingExpense == null) {
            users.filter { it.id == currentUserId }
        } else {
            users
        }
    }

    var selectedParticipantIds by remember(availableUsers, existingExpense) {
        mutableStateOf(
            if (existingExpense != null) existingExpense.participantIds.toSet()
            else availableUsers.map { it.id }.toSet()
        )
    }

    var customInputs by remember(existingExpense) {
        mutableStateOf(
            existingExpense?.customSplits?.mapValues { it.value.toString() } ?: mapOf<String, String>()
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isNewPersonalExpense = existingExpense == null && selectedGroupId == null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .testTag("add_expense_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            existingExpense != null -> "Edit Expense"
                            isNewPersonalExpense -> "Add Personal Expense"
                            else -> "Add Group Expense"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_expense_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = null
                        },
                        label = { Text("What was this for?") },
                        placeholder = { Text("e.g. Dinner, Groceries, Flight tickets") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SplitIndigoLight)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SplitEmerald,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = SplitEmerald,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountText = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Text(
                                text = "₹",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitEmeraldLight,
                                modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SplitEmerald,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = SplitEmerald,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(Category.values()) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
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
                                    )
                                )
                            }
                        }
                    }

                    // Expense destination
                    Column {
                        Text(
                            text = "Expense Type",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedGroupId == null,
                                    onClick = { selectedGroupId = null },
                                    label = { Text(if (existingExpense == null) "Personal" else "No Group") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SplitIndigo.copy(alpha = 0.3f),
                                        selectedLabelColor = SplitIndigoLight,
                                        containerColor = SurfaceCard,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                            items(groups) { group ->
                                val isSelected = selectedGroupId == group.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGroupId = group.id },
                                    label = { Text("${group.icon} ${group.name}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SplitIndigo.copy(alpha = 0.3f),
                                        selectedLabelColor = SplitIndigoLight,
                                        containerColor = SurfaceCard,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    if (isNewPersonalExpense) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("personal_expense_destination_notice"),
                            shape = RoundedCornerShape(12.dp),
                            color = SplitIndigo.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SplitIndigo.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "Personal expense - updates Monthly Spending, category totals, and Personal history. It does not affect group balances.",
                                modifier = Modifier.padding(12.dp),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (!isNewPersonalExpense) {
                    // Paid By Selector
                    Column {
                        Text(
                            text = "Paid By",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableUsers) { user ->
                                val isSelected = paidByUserId == user.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) SplitEmerald.copy(alpha = 0.2f) else SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) SplitEmerald else SurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { paidByUserId = user.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(user.avatarColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.initials,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (user.id == currentUserId) "You" else user.name,
                                            color = if (isSelected) SplitEmeraldLight else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Split Method Selector
                    Column {
                        Text(
                            text = "Split Method",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SplitType.values()) { type ->
                                val isSelected = splitType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        splitType = type
                                        errorMessage = null
                                    },
                                    label = { Text(type.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SplitEmerald.copy(alpha = 0.25f),
                                        selectedLabelColor = SplitEmeraldLight,
                                        containerColor = SurfaceCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) SplitEmerald else SurfaceBorder
                                    )
                                )
                            }
                        }
                    }

                    // Split Among Members
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Split With (${selectedParticipantIds.size} people)",
                                style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                            )

                            Text(
                                text = if (selectedParticipantIds.size == availableUsers.size) "Deselect All" else "Select All",
                                color = SplitIndigoLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    selectedParticipantIds = if (selectedParticipantIds.size == availableUsers.size) {
                                        setOf(paidByUserId)
                                    } else {
                                        availableUsers.map { it.id }.toSet()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val splitPerPerson = if (selectedParticipantIds.isNotEmpty()) amt / selectedParticipantIds.size else 0.0

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableUsers.forEach { user ->
                                val isChecked = selectedParticipantIds.contains(user.id)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isChecked) SurfaceCardElevated else SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChecked) SplitIndigo.copy(alpha = 0.5f) else SurfaceBorder.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    selectedParticipantIds = if (isChecked) {
                                                        if (selectedParticipantIds.size > 1) selectedParticipantIds - user.id else selectedParticipantIds
                                                    } else {
                                                        selectedParticipantIds + user.id
                                                    }
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedParticipantIds = if (checked) {
                                                            selectedParticipantIds + user.id
                                                        } else {
                                                            if (selectedParticipantIds.size > 1) selectedParticipantIds - user.id else selectedParticipantIds
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = SplitIndigo,
                                                        uncheckedColor = TextMuted
                                                    )
                                                )
                                                Text(
                                                    text = if (user.id == currentUserId) "You" else user.name,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            if (isChecked) {
                                                when (splitType) {
                                                    SplitType.EQUAL -> {
                                                        if (amt > 0.0) {
                                                            Text(
                                                                text = CurrencyUtils.formatINR(splitPerPerson),
                                                                color = SplitEmeraldLight,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                    SplitType.EXACT -> {
                                                        OutlinedTextField(
                                                            value = customInputs[user.id] ?: "",
                                                            onValueChange = { input ->
                                                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                                    customInputs = customInputs + (user.id to input)
                                                                    errorMessage = null
                                                                }
                                                            },
                                                            placeholder = { Text("0.00", fontSize = 12.sp) },
                                                            prefix = { Text("₹", color = TextSecondary, fontSize = 12.sp) },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.width(100.dp).height(48.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = SplitEmerald,
                                                                unfocusedBorderColor = SurfaceBorder,
                                                                focusedTextColor = TextPrimary,
                                                                unfocusedTextColor = TextPrimary
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                    }
                                                    SplitType.PERCENT -> {
                                                        OutlinedTextField(
                                                            value = customInputs[user.id] ?: "",
                                                            onValueChange = { input ->
                                                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,1}$"))) {
                                                                    customInputs = customInputs + (user.id to input)
                                                                    errorMessage = null
                                                                }
                                                            },
                                                            placeholder = { Text("0", fontSize = 12.sp) },
                                                            suffix = { Text("%", color = TextSecondary, fontSize = 12.sp) },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.width(90.dp).height(48.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = SplitEmerald,
                                                                unfocusedBorderColor = SurfaceBorder,
                                                                focusedTextColor = TextPrimary,
                                                                unfocusedTextColor = TextPrimary
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                    }
                                                    SplitType.SHARES -> {
                                                        OutlinedTextField(
                                                            value = customInputs[user.id] ?: "1",
                                                            onValueChange = { input ->
                                                                if (input.isEmpty() || input.matches(Regex("^\\d*$"))) {
                                                                    customInputs = customInputs + (user.id to input)
                                                                    errorMessage = null
                                                                }
                                                            },
                                                            placeholder = { Text("1", fontSize = 12.sp) },
                                                            suffix = { Text("shr", color = TextSecondary, fontSize = 11.sp) },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.width(86.dp).height(48.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = SplitEmerald,
                                                                unfocusedBorderColor = SurfaceBorder,
                                                                focusedTextColor = TextPrimary,
                                                                unfocusedTextColor = TextPrimary
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
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

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Add extra details, receipt notes...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SplitIndigo,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = SplitRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_expense_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorMessage = "Please enter an expense title."
                            } else if (amount == null || amount <= 0.0) {
                                errorMessage = "Please enter a valid amount greater than $0."
                            } else if (selectedParticipantIds.isEmpty()) {
                                errorMessage = "Please select at least one person to split with."
                            } else {
                                val computedCustomSplits = mutableMapOf<String, Double>()
                                var valid = true

                                when (splitType) {
                                    SplitType.EQUAL -> {
                                        // Equal split
                                    }
                                    SplitType.EXACT -> {
                                        var sum = 0.0
                                        selectedParticipantIds.forEach { uid ->
                                            val entered = customInputs[uid]?.toDoubleOrNull() ?: 0.0
                                            computedCustomSplits[uid] = entered
                                            sum += entered
                                        }
                                        if (kotlin.math.abs(sum - amount) > 0.01) {
                                            errorMessage = "Total exact splits (${CurrencyUtils.formatINR(sum)}) must equal total amount (${CurrencyUtils.formatINR(amount)})."
                                            valid = false
                                        }
                                    }
                                    SplitType.PERCENT -> {
                                        var sumPercent = 0.0
                                        selectedParticipantIds.forEach { uid ->
                                            val entered = customInputs[uid]?.toDoubleOrNull() ?: 0.0
                                            computedCustomSplits[uid] = entered
                                            sumPercent += entered
                                        }
                                        if (kotlin.math.abs(sumPercent - 100.0) > 0.1) {
                                            errorMessage = "Total percentages (${String.format(java.util.Locale.US, "%.1f", sumPercent)}%) must equal 100%."
                                            valid = false
                                        }
                                    }
                                    SplitType.SHARES -> {
                                        var totalShares = 0.0
                                        selectedParticipantIds.forEach { uid ->
                                            val entered = customInputs[uid]?.toDoubleOrNull() ?: 1.0
                                            val shareVal = if (entered <= 0.0) 1.0 else entered
                                            computedCustomSplits[uid] = shareVal
                                            totalShares += shareVal
                                        }
                                        if (totalShares <= 0.0) {
                                            errorMessage = "Please provide at least 1 total share."
                                            valid = false
                                        }
                                    }
                                }

                                if (valid) {
                                    onSaveExpense(
                                        existingExpense?.id,
                                        title.trim(),
                                        amount,
                                        paidByUserId,
                                        selectedGroupId,
                                        selectedCategory,
                                        selectedParticipantIds.toList(),
                                        splitType,
                                        computedCustomSplits,
                                        notes.trim()
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("submit_expense_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (existingExpense != null) "Update Expense" else "Save Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
