package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.splitmate.app.model.User
import com.splitmate.app.ui.components.FriendBalanceRow
import com.splitmate.app.ui.theme.*

@Composable
fun FriendsScreen(
    users: List<User>,
    currentUserId: String,
    onAddFriendClick: () -> Unit,
    onFriendClick: (String) -> Unit,
    onSettleClick: (String) -> Unit,
    calculateBalanceWithUser: (String) -> Double,
    modifier: Modifier = Modifier
) {
    val friends = users.filter { it.id != currentUserId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("friends_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Friends & Balances",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${friends.size} friends connected",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onAddFriendClick,
                colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_friend_top_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Friend", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

        if (friends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🤝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No friends added yet",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Add roommates or friends to easily split individual cabs, meals, or groceries 1-on-1.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                    )
                    Button(
                        onClick = onAddFriendClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Your First Friend", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(friends) { friend ->
                    val balance = calculateBalanceWithUser(friend.id)
                    FriendBalanceRow(
                        user = friend,
                        balance = balance,
                        onClick = { onFriendClick(friend.id) },
                        onSettleClick = { onSettleClick(friend.id) }
                    )
                }
            }
        }
    }
}
