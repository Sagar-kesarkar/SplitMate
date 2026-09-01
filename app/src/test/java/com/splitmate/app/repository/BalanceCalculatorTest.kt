package com.splitmate.app.repository

import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.SplitType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {
    private fun debt(payer: String, amount: Double, id: String = amount.toString()) =
        Expense(id, "Expense", amount * 2, payer, participantIds = listOf("me", "a"), splitType = SplitType.EQUAL)

    @Test fun oppositeBalancesCancel() = assertEquals(0.0, BalanceCalculator.pairBalance("me", "a", listOf(debt("a", 40.0), debt("me", 40.0, "2")), emptyList()), 0.001)
    @Test fun positiveBalancesAccumulate() = assertEquals(90.0, BalanceCalculator.pairBalance("me", "a", listOf(debt("me", 40.0), debt("me", 50.0, "2")), emptyList()), 0.001)
    @Test fun largerNegativeCrossesZero() = assertEquals(-10.0, BalanceCalculator.pairBalance("me", "a", listOf(debt("me", 100.0), debt("a", 110.0, "2")), emptyList()), 0.001)

    @Test fun membersRemainIsolated() {
        val expenses = listOf(debt("me", 40.0), Expense("b", "B", 100.0, "b", participantIds = listOf("me", "b")))
        assertEquals(40.0, BalanceCalculator.pairBalance("me", "a", expenses, emptyList()), 0.001)
        assertEquals(-50.0, BalanceCalculator.pairBalance("me", "b", expenses, emptyList()), 0.001)
    }

    @Test fun settlementReducesCorrectBalance() {
        val settlement = Settlement("s", "a", "me", 15.0)
        assertEquals(25.0, BalanceCalculator.pairBalance("me", "a", listOf(debt("me", 40.0)), listOf(settlement)), 0.001)
    }

    @Test fun splitRoundingPreservesOriginalTotal() {
        val expense = Expense("e", "Thirds", 100.0, "me", participantIds = listOf("me", "a", "b"))
        assertEquals(100.0, BalanceCalculator.shares(expense).values.sum(), 0.001)
    }

    @Test fun settlementsDoNotIncreaseGroupSpending() {
        val expenses = listOf(Expense("e", "Bill", 125.25, "me", groupId = "g", participantIds = listOf("me", "a")))
        val settlements = listOf(Settlement("s", "a", "me", 50.0, groupId = "g"))
        assertEquals(125.25, BalanceCalculator.groupTotalSpent("g", expenses), 0.001)
        assertEquals(50.0, settlements.sumOf { it.amount }, 0.001)
    }

    @Test fun budgetEmptyZeroAndOverBudgetRemainHonest() {
        assertEquals(null, BalanceCalculator.budgetRemaining(null, 10.0))
        assertEquals(0.0, BalanceCalculator.budgetRemaining(0.0, 0.0)!!, 0.001)
        assertEquals(-25.0, BalanceCalculator.budgetRemaining(100.0, 125.0)!!, 0.001)
    }
}
