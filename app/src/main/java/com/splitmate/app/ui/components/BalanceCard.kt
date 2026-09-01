package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import kotlin.math.abs

@Composable
fun BalanceCard(
    totalOwed: Double,
    totalOwe: Double,
    netBalance: Double,
    onAddExpenseClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("balance_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SurfaceCardElevated,
                            SurfaceCard
                        ),
                        radius = 800f
                    )
                )
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL NET BALANCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            netBalance > 0.01 -> SplitEmerald.copy(alpha = 0.15f)
                            netBalance < -0.01 -> SplitRose.copy(alpha = 0.15f)
                            else -> TextMuted.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = when {
                                netBalance > 0.01 -> "You are owed"
                                netBalance < -0.01 -> "You owe"
                                else -> "All Settled"
                            },
                            color = when {
                                netBalance > 0.01 -> SplitEmeraldLight
                                netBalance < -0.01 -> SplitRoseLight
                                else -> TextSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        netBalance > 0.01 -> "+${CurrencyUtils.formatINR(netBalance)}"
                        netBalance < -0.01 -> "-${CurrencyUtils.formatINR(abs(netBalance))}"
                        else -> CurrencyUtils.formatINR(0.0)
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = when {
                            netBalance > 0.01 -> SplitEmeraldLight
                            netBalance < -0.01 -> SplitRoseLight
                            else -> TextPrimary
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp
                    ),
                    modifier = Modifier.testTag("net_balance_text")
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = SplitEmeraldLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You are owed",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${CurrencyUtils.formatINR(totalOwed)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitEmeraldLight,
                            modifier = Modifier.testTag("total_owed_amount")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(SurfaceBorder)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = SplitRoseLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You owe",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "-${CurrencyUtils.formatINR(totalOwe)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitRoseLight,
                            modifier = Modifier.testTag("total_owe_amount")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddExpenseClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("quick_add_expense_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSettleUpClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("quick_settle_up_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = null,
                            tint = SplitIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Settle Up",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
