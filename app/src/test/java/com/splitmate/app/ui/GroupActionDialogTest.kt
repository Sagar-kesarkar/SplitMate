package com.splitmate.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.splitmate.app.model.Group
import com.splitmate.app.model.MuteDuration
import com.splitmate.app.ui.dialogs.GroupActionDialog
import com.splitmate.app.ui.theme.SplitmateTheme
import com.splitmate.app.viewmodel.GroupActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupActionDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun muteDialogOffersExactDurationsAndReturnsSingleChoice() {
        var chosen: MuteDuration? = null
        setDialog(GroupActionType.MUTE, listOf(group("g1", "owner")), onMute = { chosen = it })
        composeRule.onNodeWithText("1 hour").assertExists()
        composeRule.onNodeWithTag("mute_radio_eight_hours").performClick()
        composeRule.onNodeWithText("1 week").assertExists()
        composeRule.onNodeWithText("Until I turn it back on").assertExists()
        composeRule.onNodeWithTag("confirm_mute_groups").performClick()
        assertEquals(MuteDuration.EIGHT_HOURS, chosen)
    }

    @Test
    fun soleOwnerLeaveIsExplainedAndDisabled() {
        setDialog(GroupActionType.LEAVE, listOf(group("owned", "me")))
        composeRule.onNodeWithText("Leave Group?").assertExists()
        composeRule.onNodeWithText("Transfer ownership", substring = true, ignoreCase = true).assertExists()
        composeRule.onNodeWithTag("confirm_leave_group_dialog").assertIsNotEnabled()
    }

    @Test
    fun mixedDeleteSelectionKeepsOwnedEligibleAndOffersLeaveForNonOwner() {
        var deleteInvoked = false
        var leaveOffered = false
        setDialog(
            type = GroupActionType.DELETE,
            groups = listOf(group("owned", "me"), group("shared", "friend")),
            onDelete = { deleteInvoked = true },
            onOfferLeave = { leaveOffered = true }
        )
        composeRule.onNodeWithText("1 of 2 selected groups", substring = true).assertExists()
        composeRule.onNodeWithText("Use Leave instead", substring = true).assertExists()
        composeRule.onNodeWithText("Leave instead").performClick()
        assertTrue(leaveOffered)
        composeRule.onNodeWithTag("confirm_delete_group_dialog").assertIsEnabled().performClick()
        assertTrue(deleteInvoked)
    }

    private fun setDialog(
        type: GroupActionType,
        groups: List<Group>,
        onMute: (MuteDuration) -> Unit = {},
        onDelete: () -> Unit = {},
        onOfferLeave: () -> Unit = {}
    ) {
        composeRule.setContent {
            SplitmateTheme {
                GroupActionDialog(
                    type = type,
                    selectedGroups = groups,
                    currentUserId = "me",
                    onDismiss = {},
                    onMute = onMute,
                    onLeave = {},
                    onDelete = onDelete,
                    onOfferLeave = onOfferLeave
                )
            }
        }
    }

    private fun group(id: String, owner: String) = Group(
        id = id,
        name = id.replaceFirstChar(Char::uppercase),
        description = "",
        icon = "G",
        memberIds = listOf("me", "friend"),
        createdAt = 1L,
        ownerUserId = owner
    )
}
