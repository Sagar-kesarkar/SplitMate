package com.splitmate.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.splitmate.app.model.Group
import com.splitmate.app.model.User
import com.splitmate.app.ui.dialogs.AddExpenseDialog
import com.splitmate.app.ui.theme.SplitmateTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddExpenseDialogModeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickAddDefaultsToPersonalAndHidesSharedSplitControls() {
        setDialog(prefilledGroupId = null)

        composeRule.onNodeWithText("Add Personal Expense").assertExists()
        composeRule.onNodeWithText("Personal").assertExists()
        composeRule.onNodeWithTag("personal_expense_destination_notice").assertExists()
        composeRule.onNodeWithText("Paid By").assertDoesNotExist()
        composeRule.onNodeWithText("Split Method").assertDoesNotExist()
    }

    @Test
    fun groupPrefillKeepsExistingSharedExpenseControls() {
        setDialog(prefilledGroupId = "group_1")

        composeRule.onNodeWithText("Add Group Expense").assertExists()
        composeRule.onNodeWithText("Paid By").assertExists()
        composeRule.onNodeWithText("Split Method").assertExists()
        composeRule.onNodeWithTag("personal_expense_destination_notice").assertDoesNotExist()
    }

    @Test
    fun validPersonalQuickAddSubmitsEnteredValuesWithNoGroup() {
        var submittedTitle: String? = null
        var submittedAmount: Double? = null
        var submittedGroupId: String? = "not_submitted"
        setDialog(prefilledGroupId = null) { title, amount, groupId ->
            submittedTitle = title
            submittedAmount = amount
            submittedGroupId = groupId
        }

        composeRule.onNodeWithTag("expense_title_input").performTextInput("Quick lunch")
        composeRule.onNodeWithTag("expense_amount_input").performTextInput("125.50")
        composeRule.onNodeWithTag("submit_expense_button").performClick()

        org.junit.Assert.assertEquals("Quick lunch", submittedTitle)
        org.junit.Assert.assertEquals(125.50, submittedAmount!!, 0.001)
        org.junit.Assert.assertEquals(null, submittedGroupId)
    }

    private fun setDialog(
        prefilledGroupId: String?,
        onSave: (title: String, amount: Double, groupId: String?) -> Unit = { _, _, _ -> }
    ) {
        val me = User("me", "You", "you@example.com", "", true)
        val friend = User("friend", "Friend", "friend@example.com", "", false)
        val group = Group(
            id = "group_1",
            name = "Trip",
            description = "",
            icon = "T",
            memberIds = listOf(me.id, friend.id),
            createdAt = 1L,
            ownerUserId = me.id
        )
        composeRule.setContent {
            SplitmateTheme {
                AddExpenseDialog(
                    users = listOf(me, friend),
                    groups = listOf(group),
                    currentUserId = me.id,
                    prefilledGroupId = prefilledGroupId,
                    onDismiss = {},
                    onSaveExpense = { _, title, amount, _, groupId, _, _, _, _, _ ->
                        onSave(title, amount, groupId)
                    }
                )
            }
        }
    }
}
