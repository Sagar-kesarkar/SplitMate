package com.splitmate.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.splitmate.app.model.Group
import com.splitmate.app.ui.screens.GroupsScreen
import com.splitmate.app.ui.theme.SplitmateTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupSelectionInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longPressEntersSelectionMode() {
        setSelectionContent()
        composeRule.onNodeWithTag("group_card_g1").performTouchInput { longClick() }
        composeRule.onNodeWithTag("group_card_g1").assertIsSelected()
        composeRule.onNodeWithTag("group_card_g2").assertIsNotSelected()
    }

    @Test
    fun selectionModeSupportsMultipleGroupsAndToggleOff() {
        setSelectionContent()
        composeRule.onNodeWithTag("group_card_g1").performTouchInput { longClick() }
        composeRule.onNodeWithTag("group_card_g2").performClick()
        composeRule.onNodeWithTag("group_card_g1").assertIsSelected()
        composeRule.onNodeWithTag("group_card_g2").assertIsSelected()
        composeRule.onNodeWithTag("group_card_g1").performClick()
        composeRule.onNodeWithTag("group_card_g1").assertIsNotSelected()
        composeRule.onNodeWithTag("group_card_g2").assertIsSelected()
    }

    @Test
    fun backExitsSelectionBeforeLeavingScreen() {
        setSelectionContent()
        composeRule.onNodeWithTag("group_card_g1").performTouchInput { longClick() }
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("group_card_g1").assertIsNotSelected()
        composeRule.onNodeWithTag("group_card_g2").assertIsNotSelected()
    }

    private fun setSelectionContent() {
        val groups = listOf(
            Group("g1", "Goa", "", "G", listOf("me"), 1L, ownerUserId = "me"),
            Group("g2", "College", "", "C", listOf("me"), 2L, ownerUserId = "me")
        )
        composeRule.setContent {
            SplitmateTheme {
                var selected by remember { mutableStateOf(emptySet<String>()) }
                BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
                GroupsScreen(
                    groups = groups,
                    users = emptyList(),
                    onCreateGroupClick = {},
                    onGroupClick = {},
                    selectedGroupIds = selected,
                    onGroupLongPress = { groupId ->
                        if (selected.isEmpty()) selected = setOf(groupId)
                    },
                    onToggleGroupSelection = { groupId ->
                        selected = if (groupId in selected) selected - groupId else selected + groupId
                    },
                    calculateGroupBalance = { 0.0 },
                    calculateGroupTotalSpend = { 0.0 }
                )
            }
        }
    }
}
