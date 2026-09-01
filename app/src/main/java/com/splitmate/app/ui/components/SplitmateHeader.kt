package com.splitmate.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.ui.theme.*

@Composable
fun SplitmateHeader(
    greeting: String,
    isDemoMode: Boolean,
    onSwitchMode: (Boolean) -> Unit,
    onResetDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDark,
                        BackgroundDark
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        ) {
                            append("Split")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFB39DDB), // Soft lavender/periwinkle
                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-0.5).sp
                            )
                        ) {
                            append("Mate")
                        }
                    },
                    fontSize = 28.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("app_brand_title")
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDemoMode) DemoBadgeBg else LiveBadgeBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDemoMode) SplitIndigoLight.copy(alpha = 0.5f) else SplitEmeraldLight.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showMenu = true }
                        .testTag("mode_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDemoMode) SplitIndigoLight else SplitEmerald)
                        )
                        Text(
                            text = if (isDemoMode) "Demo Mode" else "Live Mode",
                            color = if (isDemoMode) DemoBadgeText else LiveBadgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle mode options",
                            tint = if (isDemoMode) DemoBadgeText else LiveBadgeText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("mode_dropdown_menu")
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = SplitIndigoLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Demo Mode (Sample Data)",
                                        color = TextPrimary,
                                        fontWeight = if (isDemoMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = "Preloaded test data to explore SplitMate",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        },
                        onClick = {
                            onSwitchMode(true)
                            showMenu = false
                        },
                        trailingIcon = {
                            if (isDemoMode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = SplitIndigoLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.testTag("menu_demo_mode")
                    )

                    HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SplitEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Live Mode (Persistent Data)",
                                        color = TextPrimary,
                                        fontWeight = if (!isDemoMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = "Your genuine data saved locally with Room",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        },
                        onClick = {
                            onSwitchMode(false)
                            showMenu = false
                        },
                        trailingIcon = {
                            if (!isDemoMode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = SplitEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.testTag("menu_live_mode")
                    )

                    if (isDemoMode) {
                        HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = SplitAmberLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reset Demo Data",
                                        color = SplitAmberLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                onResetDemoData()
                                showMenu = false
                            },
                            modifier = Modifier.testTag("menu_reset_demo")
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isDemoMode) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCard.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SplitIndigo.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SplitIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Demo Mode is active. Switch to Live Mode anytime.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        text = "Go Live",
                        color = SplitIndigoLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { onSwitchMode(false) }
                            .padding(horizontal = 8.dp)
                            .testTag("exit_demo_mode_button")
                    )
                }
            }
        }
    }
}
