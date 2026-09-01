package com.splitmate.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.Category
import com.splitmate.app.model.Expense
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.avatarColor
import com.splitmate.app.ui.components.initials
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import java.util.Locale

@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    users: List<User>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val totalExpenseVolume = expenses.sumOf { it.amount }

    // Category spending map
    val categorySpending = Category.values().associateWith { cat ->
        expenses.filter { it.category == cat }.sumOf { it.amount }
    }.filterValues { it > 0.0 }

    // Top spenders
    val spenderAmounts = users.associateWith { u ->
        expenses.filter { it.paidByUserId == u.id }.sumOf { it.amount }
    }.filterValues { it > 0.0 }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Spending & Insights",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Breakdown of shared finances and group totals",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Shared Volume Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("TOTAL RECORDED SPEND", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatINR(totalExpenseVolume),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = SplitEmeraldLight
                        )
                        Text(
                            text = "Across ${expenses.size} expenses recorded in SplitMate",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Category Spending Breakdown
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Spending by Category",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (categorySpending.isEmpty()) {
                            Text("No category data yet", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                categorySpending.entries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                                    val percent = if (totalExpenseVolume > 0.0) (amount / totalExpenseVolume).toFloat() else 0f
                                    val animatedPercent by animateFloatAsState(targetValue = percent, label = "cat_bar")

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(cat.icon, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = cat.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                            }

                                            Text(
                                                text = "${CurrencyUtils.formatINR(amount)} (${(percent * 100).toInt()}%)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cat.color
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SurfaceDark)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedPercent.coerceIn(0.02f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(cat.color)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Payers Leaderboard
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Who Paid Most",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (spenderAmounts.isEmpty()) {
                            Text("No payment activity yet", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                spenderAmounts.forEachIndexed { index, (user, amount) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SurfaceCardElevated,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (index == 0) SplitAmberLight else TextSecondary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(user.avatarColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(user.initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (user.id == currentUserId) "You" else user.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                            }

                                            Text(
                                                text = CurrencyUtils.formatINR(amount),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SplitEmeraldLight
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
