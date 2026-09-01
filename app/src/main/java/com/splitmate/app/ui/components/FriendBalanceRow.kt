package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.model.User
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.CurrencyUtils
import kotlin.math.abs

val User.initials: String
    get() = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifEmpty { "U" }

val User.avatarColor: Color
    get() = when (abs(id.hashCode()) % 5) {
        0 -> Color(0xFF6366F1)
        1 -> Color(0xFF10B981)
        2 -> Color(0xFFF59E0B)
        3 -> Color(0xFFEC4899)
        else -> Color(0xFF3B82F6)
    }

@Composable
fun FriendBalanceRow(
    user: User,
    balance: Double,
    onClick: () -> Unit,
    onSettleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("friend_row_${user.id}"),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(user.avatarColor)
                    .border(1.5.dp, SurfaceBorderLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                when {
                    balance > 0.01 -> {
                        Text(
                            text = "owes you",
                            fontSize = 10.sp,
                            color = SplitEmeraldLight,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "+${CurrencyUtils.formatINR(balance)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitEmeraldLight
                        )
                    }
                    balance < -0.01 -> {
                        Text(
                            text = "you owe",
                            fontSize = 10.sp,
                            color = SplitRoseLight,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "-${CurrencyUtils.formatINR(abs(balance))}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitRoseLight
                        )
                    }
                    else -> {
                        Text(
                            text = "settled",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (abs(balance) > 0.01) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SplitIndigo.copy(alpha = 0.18f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSettleClick() }
                            .testTag("settle_friend_button_${user.id}")
                    ) {
                        Text(
                            text = "Settle",
                            color = SplitIndigoLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
