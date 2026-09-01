package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Group
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseItemRow(
    expense: Expense,
    payerUser: User?,
    group: Group?,
    currentUserId: String,
    onClick: () -> Unit,
    useSignedEffectLabels: Boolean = false,
    showDetailChevron: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPayer = expense.paidByUserId == currentUserId
    val isParticipant = expense.participantIds.contains(currentUserId)

    val myShare = if (!isParticipant) 0.0 else {
        when (expense.splitType) {
            SplitType.EQUAL -> {
                if (expense.participantIds.isNotEmpty()) expense.amount / expense.participantIds.size else 0.0
            }
            SplitType.EXACT -> expense.customSplits[currentUserId] ?: 0.0
            SplitType.PERCENT -> {
                val p = expense.customSplits[currentUserId] ?: 0.0
                (expense.amount * p) / 100.0
            }
            SplitType.SHARES -> {
                val totalShares = expense.customSplits.values.sum().coerceAtLeast(1.0)
                val s = expense.customSplits[currentUserId] ?: 1.0
                (expense.amount * s) / totalShares
            }
        }
    }

    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(expense.dateMillis))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("expense_row_${expense.id}"),
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(expense.category.color.copy(alpha = 0.2f))
                    .border(1.dp, expense.category.color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = expense.category.icon,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (group != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SplitIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${group.icon} ${group.name}",
                                color = SplitIndigoLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = if (isPayer) "You paid ${CurrencyUtils.formatINR(expense.amount)}"
                        else "${payerUser?.name ?: "Someone"} paid ${CurrencyUtils.formatINR(expense.amount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                when {
                    isPayer -> {
                        val lentAmount = expense.amount - myShare
                        if (lentAmount > 0.01) {
                            Text(
                                text = if (useSignedEffectLabels) "you are owed" else "you lent",
                                fontSize = 10.sp,
                                color = SplitEmeraldLight,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+${CurrencyUtils.formatINR(lentAmount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitEmeraldLight
                            )
                        } else {
                            Text(
                                text = "you paid",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyUtils.formatINR(expense.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                    isParticipant -> {
                        Text(
                            text = if (useSignedEffectLabels) "you owe" else "you borrowed",
                            fontSize = 10.sp,
                            color = SplitRoseLight,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "-${CurrencyUtils.formatINR(myShare)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitRoseLight
                        )
                    }
                    else -> {
                        Text(
                            text = "not involved",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (showDetailChevron) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "View details", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
