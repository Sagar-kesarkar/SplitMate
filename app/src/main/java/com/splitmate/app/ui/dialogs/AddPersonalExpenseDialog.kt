package com.splitmate.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.splitmate.app.model.PersonalExpense
import com.splitmate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseDialog(
    existingExpense: PersonalExpense? = null,
    onDismiss: () -> Unit,
    onSaveExpense: (id: String?, title: String, amount: Double, category: Category, notes: String, dateMillis: Long) -> Unit,
    onDeleteExpense: (String) -> Unit = {}
) {
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(existingExpense?.category ?: Category.GENERAL) }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }
    var selectedDateMillis by remember { mutableLongStateOf(existingExpense?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDateMillis = it }; showDatePicker = false }) { Text("Select") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Dialog(
        onDismissRequest = {
            if (showDeleteConfirmation) showDeleteConfirmation = false else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        if (showDeleteConfirmation && existingExpense != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Delete Personal Expense?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Remove \"${existingExpense.title}\" from Personal Expenses? Your monthly total and category breakdown will update immediately.",
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                        TextButton(
                            onClick = { onDeleteExpense(existingExpense.id) },
                            modifier = Modifier.testTag("confirm_delete_personal_expense")
                        ) { Text("Delete Expense", color = SplitRoseLight) }
                    }
                }
            }
        } else Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                    Text(
                        text = if (existingExpense == null) "New Personal Expense" else "Edit Personal Expense",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (existingExpense != null) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.testTag("delete_personal_expense")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Personal Expense",
                                    tint = SplitRoseLight
                                )
                            }
                        }
                        val validAmount = amountText.toDoubleOrNull()?.let { it > 0.0 } == true
                        TextButton(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && amount > 0.0) {
                                    onSaveExpense(existingExpense?.id, title, amount, selectedCategory, notes, selectedDateMillis)
                                }
                            },
                            enabled = title.isNotBlank() && validAmount
                        ) {
                            Text("Save", color = if (title.isNotBlank() && validAmount) SplitIndigoLight else TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Amount Field
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("AMOUNT", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        TextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 40.sp
                            ),
                            placeholder = { Text("0", color = TextMuted) },
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text("WHAT WAS IT FOR?", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Lunch, Coffee, Rent", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitIndigoLight,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Category
                Text("CATEGORY", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Category.entries.toTypedArray()) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) category.color.copy(alpha = 0.2f) else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) category.color else SurfaceBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("DATE", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis)), color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Notes
                Text("NOTES (OPTIONAL)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitIndigoLight,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }
    }
}
