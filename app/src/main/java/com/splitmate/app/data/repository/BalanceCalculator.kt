package com.splitmate.app.data.repository

import com.splitmate.app.model.Expense
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.PersonalExpense
import com.splitmate.app.model.Category
import java.util.Calendar
import kotlin.math.roundToLong

/** Money-safe split and signed-balance calculations. Positive means the other user owes us. */
object BalanceCalculator {
    fun groupTotalSpent(groupId: String, expenses: List<Expense>): Double =
        expenses.asSequence().filter { it.groupId == groupId }.sumOf { (it.amount * 100.0).roundToLong() } / 100.0

    fun budgetRemaining(budget: Double?, totalSpent: Double): Double? =
        budget?.let { ((it * 100.0).roundToLong() - (totalSpent * 100.0).roundToLong()) / 100.0 }

    fun currentMonthBounds(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun personalForMonth(expenses: List<PersonalExpense>, nowMillis: Long = System.currentTimeMillis()): List<PersonalExpense> {
        val (start, end) = currentMonthBounds(nowMillis)
        return expenses.filter { it.dateMillis >= start && it.dateMillis < end }
    }

    fun personalMonthlyTotal(expenses: List<PersonalExpense>, nowMillis: Long = System.currentTimeMillis()): Double =
        personalForMonth(expenses, nowMillis).sumOf { (it.amount * 100.0).roundToLong() } / 100.0

    fun personalCategoryTotals(expenses: List<PersonalExpense>, nowMillis: Long = System.currentTimeMillis()): Map<Category, Double> =
        personalForMonth(expenses, nowMillis).groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { (it.amount * 100.0).roundToLong() } / 100.0 }

    fun shares(expense: Expense): Map<String, Double> {
        val ids = expense.participantIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val totalCents = (expense.amount * 100.0).roundToLong()
        val rawWeights = when (expense.splitType) {
            SplitType.EQUAL -> ids.associateWith { 1.0 }
            SplitType.EXACT -> ids.associateWith { (expense.customSplits[it] ?: 0.0) * 100.0 }
            SplitType.PERCENT -> ids.associateWith { expense.customSplits[it] ?: 0.0 }
            SplitType.SHARES -> ids.associateWith { expense.customSplits[it] ?: 0.0 }
        }
        if (expense.splitType == SplitType.EXACT) {
            val cents = ids.map { ((expense.customSplits[it] ?: 0.0) * 100.0).roundToLong() }.toMutableList()
            if (cents.isNotEmpty()) cents[cents.lastIndex] += totalCents - cents.sum()
            return ids.zip(cents).associate { (id, value) -> id to value / 100.0 }
        }
        val weightTotal = rawWeights.values.sum()
        if (weightTotal <= 0.0) return emptyMap()
        val cents = ids.map { id -> (totalCents * (rawWeights[id] ?: 0.0) / weightTotal).roundToLong() }.toMutableList()
        cents[cents.lastIndex] += totalCents - cents.sum()
        return ids.zip(cents).associate { (id, value) -> id to value / 100.0 }
    }

    fun pairBalance(me: String, other: String, expenses: List<Expense>, settlements: List<Settlement>): Double {
        var cents = 0L
        expenses.forEach { expense ->
            val allocations = shares(expense)
            if (expense.paidByUserId == me) cents += ((allocations[other] ?: 0.0) * 100).roundToLong()
            else if (expense.paidByUserId == other) cents -= ((allocations[me] ?: 0.0) * 100).roundToLong()
        }
        settlements.forEach { settlement ->
            val value = (settlement.amount * 100).roundToLong()
            if (settlement.fromUserId == me && settlement.toUserId == other) cents += value
            else if (settlement.fromUserId == other && settlement.toUserId == me) cents -= value
        }
        return cents / 100.0
    }

    fun allPairBalances(me: String, expenses: List<Expense>, settlements: List<Settlement>): Map<String, Double> {
        val others = buildSet {
            expenses.forEach { add(it.paidByUserId); addAll(it.participantIds) }
            settlements.forEach { add(it.fromUserId); add(it.toUserId) }
        } - me
        return others.associateWith { pairBalance(me, it, expenses, settlements) }
    }
}
