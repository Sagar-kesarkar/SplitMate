package com.splitmate.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.data.repository.BalanceTimelineBuilder
import com.splitmate.app.data.demo.DemoDataProvider
import com.splitmate.app.data.local.AppPreferenceEntity
import com.splitmate.app.data.local.PersonalExpenseEntity
import com.splitmate.app.data.local.UserEntity
import com.splitmate.app.model.Category
import com.splitmate.app.model.PersonalExpense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DemoSeedingTest {

    private lateinit var context: Context
    private lateinit var liveDb: AppDatabase
    private lateinit var demoDb: AppDatabase
    private lateinit var repository: SplitmateRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        liveDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        demoDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SplitmateRepository(context, liveDb, demoDb)
    }

    @After
    fun tearDown() {
        liveDb.close()
        demoDb.close()
    }

    @Test
    fun testInitialDemoSeedingOccursOnce() = runBlocking {
        repository.setDemoMode(true)
        val pExps = repository.personalExpenses.first()
        assertEquals(6, pExps.size)
        val total = pExps.sumOf { it.amount }
        assertEquals(1175.0, total, 0.01)
    }

    @Test
    fun testOrdinaryRestartDoesNotDuplicateRecords() = runBlocking {
        repository.setDemoMode(true)
        val initialSize = repository.personalExpenses.first().size

        val newRepo = SplitmateRepository(context, liveDb, demoDb)
        newRepo.setDemoMode(true)
        val restartSize = newRepo.personalExpenses.first().size

        assertEquals(initialSize, restartSize)
    }

    @Test
    fun testDeletionFollowedByRestartDoesNotRestoreRecords() = runBlocking {
        repository.setDemoMode(true)
        val pExps = repository.personalExpenses.first()
        val firstId = pExps.first().id

        repository.deletePersonalExpense(firstId)
        val afterDeleteSize = repository.personalExpenses.first().size
        assertEquals(5, afterDeleteSize)

        val newRepo = SplitmateRepository(context, liveDb, demoDb)
        newRepo.setDemoMode(true)
        val afterRestartSize = newRepo.personalExpenses.first().size
        assertEquals(5, afterRestartSize)
    }

    @Test
    fun testDemoResetRestoresOriginalRecords() = runBlocking {
        repository.setDemoMode(true)
        val pExps = repository.personalExpenses.first()
        repository.deletePersonalExpense(pExps.first().id)
        assertEquals(5, repository.personalExpenses.first().size)

        repository.resetDemoData()
        val restoredExps = repository.personalExpenses.first()
        assertEquals(6, restoredExps.size)
        assertEquals(1175.0, restoredExps.sumOf { it.amount }, 0.01)
    }

    @Test
    fun testLiveModeDoesNotReceiveDemoRecords() = runBlocking {
        repository.setDemoMode(false)
        val liveExps = repository.personalExpenses.first()
        assertEquals(0, liveExps.size)
    }

    @Test
    fun exactGoaDatasetReconcilesBudgetAndEveryPairBalance() = runBlocking {
        repository.resetDemoData()
        repository.setDemoMode(true)
        val group = repository.groups.first().single { it.id == DemoDataProvider.GOA_GROUP_ID }
        val expenses = repository.expenses.first().filter { it.groupId == group.id }
        val settlements = repository.settlements.first().filter { it.groupId == group.id }

        assertEquals(20_500.0, group.budget!!, 0.001)
        assertEquals(2_480.0, BalanceCalculator.groupTotalSpent(group.id, expenses), 0.001)
        assertEquals(18_020.0, BalanceCalculator.budgetRemaining(group.budget, 2_480.0)!!, 0.001)
        assertEquals(0.0, BalanceCalculator.pairBalance("u1", "u2", expenses, settlements), 0.001)
        assertEquals(-100.0, BalanceCalculator.pairBalance("u1", "u3", expenses, settlements), 0.001)
        assertEquals(340.0, BalanceCalculator.pairBalance("u1", "u4", expenses, settlements), 0.001)
        val (net, owed, owe) = repository.calculateNetBalance("u1", expenses, settlements)
        assertEquals(240.0, net, 0.001)
        assertEquals(340.0, owed, 0.001)
        assertEquals(100.0, owe, 0.001)
    }

    @Test
    fun seededTimelineHasAllocationsAndReconcilableBeforeDeltaAfter() = runBlocking {
        repository.resetDemoData()
        repository.setDemoMode(true)
        val history = repository.balanceHistory.first().filter { it.groupId == DemoDataProvider.GOA_GROUP_ID }
        val timeline = BalanceTimelineBuilder.build(history, DemoDataProvider.CURRENT_USER_ID)

        assertEquals(5, timeline.entries.size)
        assertEquals(mapOf("u1" to 160.0, "u2" to 160.0, "u3" to 160.0, "u4" to 160.0), timeline.entries.first().allocations)
        assertEquals(0.0, timeline.finalBalances["u2"]!!, 0.001)
        assertEquals(-100.0, timeline.finalBalances["u3"]!!, 0.001)
        assertEquals(340.0, timeline.finalBalances["u4"]!!, 0.001)
        val carSarah = timeline.entries.single { it.title == "Car Rental" }.changes.single { it.memberId == "u3" }
        assertEquals(160.0, carSarah.before, 0.001)
        assertEquals(-300.0, carSarah.delta, 0.001)
        assertEquals(-140.0, carSarah.after, 0.001)
    }

    @Test
    fun personalDemoRecordsAreCurrentMonthAndCategoryExact() = runBlocking {
        repository.resetDemoData()
        repository.setDemoMode(true)
        val personal = repository.personalExpenses.first()
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(personal), 0.001)
        val totals = BalanceCalculator.personalCategoryTotals(personal)
        assertEquals(480.0, totals[com.splitmate.app.model.Category.RENT]!!, 0.001)
        assertEquals(235.0, totals[com.splitmate.app.model.Category.FOOD]!!, 0.001)
        assertEquals(220.0, totals[com.splitmate.app.model.Category.ENTERTAINMENT]!!, 0.001)
        assertEquals(135.0, totals[com.splitmate.app.model.Category.GROCERIES]!!, 0.001)
        assertEquals(60.0, totals[com.splitmate.app.model.Category.UTILITIES]!!, 0.001)
        assertEquals(45.0, totals[com.splitmate.app.model.Category.TRAVEL]!!, 0.001)
        val now = System.currentTimeMillis()
        assertEquals(true, personal.all { it.dateMillis <= now })
    }

    @Test
    fun existingDemoDatabaseReceivesCurrentPersonalSamplesExactlyOnce() = runBlocking {
        val legacyLiveDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val legacyDemoDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        legacyDemoDb.userDao().insertUser(UserEntity.fromDomainModel(DemoDataProvider.currentUser))
        legacyDemoDb.preferenceDao().setPreference(AppPreferenceEntity("demo_seed_complete", "true"))
        legacyDemoDb.personalExpenseDao().insertPersonalExpense(
            PersonalExpenseEntity.fromDomainModel(
                PersonalExpense(
                    id = "demo_personal_food",
                    title = "Old sample",
                    amount = 1.0,
                    category = Category.FOOD,
                    dateMillis = 1L
                )
            )
        )

        val legacyRepository = SplitmateRepository(context, legacyLiveDb, legacyDemoDb)
        legacyRepository.setDemoMode(true)
        val backfilled = legacyRepository.personalExpenses.first()
        assertEquals(6, backfilled.size)
        assertEquals(1_175.0, BalanceCalculator.personalMonthlyTotal(backfilled), 0.001)

        legacyRepository.deletePersonalExpense("demo_personal_food")
        val reopened = SplitmateRepository(context, legacyLiveDb, legacyDemoDb)
        reopened.setDemoMode(true)
        assertEquals(5, reopened.personalExpenses.first().size)
        legacyLiveDb.close()
        legacyDemoDb.close()
        Unit
    }
}
