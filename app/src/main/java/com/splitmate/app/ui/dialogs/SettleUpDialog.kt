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
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpDialog(
    users: List<User>,
    groups: List<Group>,
    currentUserId: String,
    prefilledReceiverId: String? = null,
    prefilledGroupId: String? = null,
    onDismiss: () -> Unit,
    onRecordSettlement: (
        payerId: String,
        receiverId: String,
        amount: Double,
        groupId: String?,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    val otherUsers = remember(users, currentUserId) { users.filter { it.id != currentUserId } }

    var payerId by remember { mutableStateOf(currentUserId) }
    var receiverId by remember { mutableStateOf(prefilledReceiverId ?: otherUsers.firstOrNull()?.id ?: "") }
    var amountText by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(prefilledGroupId) }
    var selectedPaymentMethod by remember { mutableStateOf("UPI / Bank") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("UPI / Bank", "Cash", "PayPal", "Venmo", "Card")

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
                .testTag("settle_up_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SplitIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = SplitIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record a Payment",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settle_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Direction: Who is paying who?
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val payerUser = users.find { it.id == payerId }
                    val receiverUser = users.find { it.id == receiverId }

                    // Payer Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Payer", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (payerUser?.id == currentUserId) "You" else payerUser?.name ?: "",
                                color = SplitEmeraldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = {
                            val temp = payerId
                            payerId = receiverId
                            receiverId = temp
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Payer and Receiver",
                            tint = SplitIndigoLight
                        )
                    }

                    // Receiver Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SplitIndigo.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Receiver", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (receiverUser?.id == currentUserId) "You" else receiverUser?.name ?: "",
                                color = SplitIndigoLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Friend
                Text(
                    text = "Select Person",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(otherUsers) { user ->
                        val isSelected = (user.id == receiverId && payerId == currentUserId) ||
                                (user.id == payerId && receiverId == currentUserId)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SplitIndigo.copy(alpha = 0.25f) else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SplitIndigoLight else SurfaceBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (payerId == currentUserId) {
                                        receiverId = user.id
                                    } else {
                                        payerId = user.id
                                    }
                                }
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.name,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Settlement Amount (₹)") },
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
                        .testTag("settle_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = SplitEmerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(paymentMethods) { method ->
                        val isSelected = selectedPaymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SplitEmerald.copy(alpha = 0.25f),
                                selectedLabelColor = SplitEmeraldLight,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group Association (Optional)
                if (groups.isNotEmpty()) {
                    Text(
                        text = "Related Group (Optional)",
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
                                label = { Text("None") }
                            )
                        }
                        items(groups) { grp ->
                            FilterChip(
                                selected = selectedGroupId == grp.id,
                                onClick = { selectedGroupId = grp.id },
                                label = { Text("${grp.icon} ${grp.name}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reference (Optional)") },
                    placeholder = { Text("e.g. Paid via Google Pay, Thanks!") },
                    singleLine = true,
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = SplitRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Settlement
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Please enter a valid amount."
                        } else if (payerId == receiverId) {
                            errorMessage = "Payer and Receiver must be different people."
                        } else {
                            onRecordSettlement(
                                payerId,
                                receiverId,
                                amount,
                                selectedGroupId,
                                selectedPaymentMethod,
                                notes.ifBlank { "Settled via $selectedPaymentMethod" }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_settlement_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Settlement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
