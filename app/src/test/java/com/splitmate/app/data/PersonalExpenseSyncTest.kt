package com.splitmate.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PersonalExpenseSyncTest {

    private lateinit var context: Context
    private lateinit var liveDb: AppDatabase
    private lateinit var demoDb: AppDatabase
    private lateinit var repository: SplitmateRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        liveDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        demoDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = SplitmateRepository(context, liveDb, demoDb)
    }

    @After
    fun tearDown() {
        liveDb.close()
        demoDb.close()
    }

    @Test
    fun testMonthBoundaryFirstDayInclusionAndNextMonthExclusion() = runBlocking {
        repository.setDemoMode(false) // live mode starting empty

        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis

        // Add expense on first millisecond of month
        repository.addPersonalExpense("First Day Expense", 100.0, Category.FOOD)
        // Add expense in next month
        val nextMonthExpense = com.splitmate.app.data.local.PersonalExpenseEntity(
            id = "next_m",
            title = "Next Month Item",
            amount = 500.0,
            categoryName = Category.TRAVEL.name,
            notes = "",
            dateMillis = startOfNextMonth + 1000
        )
        liveDb.personalExpenseDao().insertPersonalExpense(nextMonthExpense)

        val allExps = repository.personalExpenses.first()
        val currentMonthTotal = allExps.filter { it.dateMillis >= startOfMonth && it.dateMillis < startOfNextMonth }.sumOf { it.amount }

        assertEquals(100.0, currentMonthTotal, 0.01)
    }

    @Test
    fun testDecemberToJanuaryTransition() = runBlocking {
        repository.setDemoMode(false)

        val decCal = Calendar.getInstance(TimeZone.getDefault())
        decCal.set(2025, Calendar.DECEMBER, 31, 23, 59, 59)
        val decTime = decCal.timeInMillis

        val janCal = Calendar.getInstance(TimeZone.getDefault())
        janCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        val janTime = janCal.timeInMillis

        val decExp = com.splitmate.app.data.local.PersonalExpenseEntity("e_dec", "Dec Item", 250.0, "FOOD", "", decTime)
        val janExp = com.splitmate.app.data.local.PersonalExpenseEntity("e_jan", "Jan Item", 350.0, "RENT", "", janTime)

        liveDb.personalExpenseDao().insertPersonalExpense(decExp)
        liveDb.personalExpenseDao().insertPersonalExpense(janExp)

        val allExps = repository.personalExpenses.first()

        // Calculate Jan 2026 bounds
        val cal2026 = Calendar.getInstance(TimeZone.getDefault())
        cal2026.set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        cal2026.set(Calendar.MILLISECOND, 0)
        val startJan2026 = cal2026.timeInMillis
        cal2026.add(Calendar.MONTH, 1)
        val endJan2026 = cal2026.timeInMillis

        val janTotal = allExps.filter { it.dateMillis >= startJan2026 && it.dateMillis < endJan2026 }.sumOf { it.amount }
        assertEquals(350.0, janTotal, 0.01)
    }

    @Test
    fun testPersonalExpensesDoNotAffectSharedBalances() = runBlocking {
        repository.setDemoMode(true)
        val (netBefore, owedBefore, oweBefore) = repository.calculateNetBalance("u1", repository.expenses.first(), repository.settlements.first())

        repository.addPersonalExpense("Huge Private Shopping", 50000.0, Category.SHOPPING)

        val (netAfter, owedAfter, oweAfter) = repository.calculateNetBalance("u1", repository.expenses.first(), repository.settlements.first())

        assertEquals(netBefore, netAfter, 0.001)
        assertEquals(owedBefore, owedAfter, 0.001)
        assertEquals(oweBefore, oweAfter, 0.001)
    }

    @Test
    fun editCategoryAndDateImmediatelyRecalculateMonthlyBreakdown() = runBlocking {
        repository.setDemoMode(false)
        val (start, end) = BalanceCalculator.currentMonthBounds()
        repository.addPersonalExpense("Editable", 90.0, Category.FOOD, dateMillis = start + 86_400_000L)
        var rows = repository.personalExpenses.first()
        assertEquals(90.0, BalanceCalculator.personalMonthlyTotal(rows), 0.001)
        assertEquals(90.0, BalanceCalculator.personalCategoryTotals(rows)[Category.FOOD]!!, 0.001)

        repository.updatePersonalExpense(rows.single().copy(category = Category.TRAVEL, dateMillis = end + 1L))
        rows = repository.personalExpenses.first()
        assertEquals(0.0, BalanceCalculator.personalMonthlyTotal(rows), 0.001)
        assertEquals(null, BalanceCalculator.personalCategoryTotals(rows)[Category.FOOD])
        assertEquals(null, BalanceCalculator.personalCategoryTotals(rows)[Category.TRAVEL])
    }

    @Test
    fun demoAddEditDeleteRecalculatesEveryPersonalViewAndSurvivesRestart() = runBlocking {
        repository.setDemoMode(true)
        val initial = repository.personalExpenses.first { it.size == 6 }
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(initial), 0.001)

        repository.addPersonalExpense("Demo Test Snack", 125.0, Category.GROCERIES)
        val afterAdd = repository.personalExpenses.first { rows -> rows.any { it.title == "Demo Test Snack" } }
        val added = afterAdd.single { it.title == "Demo Test Snack" }
        assertEquals(1_300.0, BalanceCalculator.personalMonthlyTotal(afterAdd), 0.001)
        assertEquals(260.0, BalanceCalculator.personalCategoryTotals(afterAdd)[Category.GROCERIES]!!, 0.001)

        repository.updatePersonalExpense(added.copy(amount = 75.0, category = Category.UTILITIES))
        val afterEdit = repository.personalExpenses.first { rows ->
            rows.any { it.id == added.id && it.amount == 75.0 && it.category == Category.UTILITIES }
        }
        assertEquals(1_250.0, BalanceCalculator.personalMonthlyTotal(afterEdit), 0.001)
        assertEquals(135.0, BalanceCalculator.personalCategoryTotals(afterEdit)[Category.GROCERIES]!!, 0.001)
        assertEquals(135.0, BalanceCalculator.personalCategoryTotals(afterEdit)[Category.UTILITIES]!!, 0.001)

        repository.deletePersonalExpense(added.id)
        val afterDelete = repository.personalExpenses.first { rows -> rows.none { it.id == added.id } }
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(afterDelete), 0.001)
        assertEquals(6, afterDelete.size)

        val reopened = SplitmateRepository(context, liveDb, demoDb)
        reopened.setDemoMode(true)
        val afterRestart = reopened.personalExpenses.first()
        assertEquals(6, afterRestart.size)
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(afterRestart), 0.001)

        reopened.setDemoMode(false)
        assertEquals(0, reopened.personalExpenses.first().size)
    }

    @Test
    fun livePersonalCrudPersistsWithoutChangingDemoSamples() = runBlocking {
        repository.setDemoMode(false)
        repository.addPersonalExpense("Live Local Expense", 82.50, Category.FOOD)
        val liveAdded = repository.personalExpenses.first { rows -> rows.any { it.title == "Live Local Expense" } }
        val liveExpense = liveAdded.single { it.title == "Live Local Expense" }
        assertEquals(82.50, BalanceCalculator.personalMonthlyTotal(liveAdded), 0.001)

        val reopened = SplitmateRepository(context, liveDb, demoDb)
        reopened.setDemoMode(false)
        assertEquals(82.50, BalanceCalculator.personalMonthlyTotal(reopened.personalExpenses.first()), 0.001)

        reopened.setDemoMode(true)
        val demoRows = reopened.personalExpenses.first()
        assertEquals(6, demoRows.size)
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(demoRows), 0.001)
        assertEquals(false, demoRows.any { it.title == "Live Local Expense" })

        reopened.setDemoMode(false)
        reopened.deletePersonalExpense(liveExpense.id)
        assertEquals(0, reopened.personalExpenses.first().size)
        reopened.setDemoMode(true)
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(reopened.personalExpenses.first()), 0.001)
    }
}
