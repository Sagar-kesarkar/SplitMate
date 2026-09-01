package com.splitmate.app.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.data.repository.BalanceTimelineBuilder
import com.splitmate.app.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SplitCalculationTest {

    private lateinit var context: Context
    private lateinit var liveDb: AppDatabase
    private lateinit var demoDb: AppDatabase
    private lateinit var repository: SplitmateRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
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
    fun testEqualSplitAndRoundingPreservation() {
        val currentUserId = "me"
        val expenses = listOf(
            Expense(
                id = "1",
                title = "Dinner",
                amount = 100.0,
                paidByUserId = "me",
                participantIds = listOf("me", "f1", "f2"),
                splitType = SplitType.EQUAL
            )
        )

        val (net, owed, owe) = repository.calculateNetBalance(currentUserId, expenses, emptyList())

        // 100 / 3 = 33.333333333333336 per person. "me" is owed 2 * 33.3333333 = 66.6666...
        assertEquals(66.666, owed, 0.01)
        assertEquals(0.0, owe, 0.01)
        assertEquals(66.666, net, 0.01)
    }

    @Test
    fun testExactSplit() {
        val currentUserId = "me"
        val expenses = listOf(
            Expense(
                id = "1",
                title = "Dinner",
                amount = 300.0,
                paidByUserId = "f1",
                participantIds = listOf("me", "f1"),
                splitType = SplitType.EXACT,
                customSplits = mapOf("me" to 120.0, "f1" to 180.0)
            )
        )

        val (net, owed, owe) = repository.calculateNetBalance(currentUserId, expenses, emptyList())

        assertEquals(0.0, owed, 0.01)
        assertEquals(120.0, owe, 0.01)
        assertEquals(-120.0, net, 0.01)
    }

    @Test
    fun testPercentSplit() {
        val currentUserId = "me"
        val expenses = listOf(
            Expense(
                id = "1",
                title = "Hotel Stay",
                amount = 200.0,
                paidByUserId = "me",
                participantIds = listOf("me", "f1"),
                splitType = SplitType.PERCENT,
                customSplits = mapOf("me" to 40.0, "f1" to 60.0)
            )
        )

        val (net, owed, owe) = repository.calculateNetBalance(currentUserId, expenses, emptyList())

        // me paid 200. f1 owes 60% of 200 = 120.
        assertEquals(120.0, owed, 0.01)
        assertEquals(0.0, owe, 0.01)
        assertEquals(120.0, net, 0.01)
    }

    @Test
    fun testShareSplit() {
        val currentUserId = "me"
        val expenses = listOf(
            Expense(
                id = "1",
                title = "Rental Car",
                amount = 500.0,
                paidByUserId = "me",
                participantIds = listOf("me", "f1"),
                splitType = SplitType.SHARES,
                customSplits = mapOf("me" to 2.0, "f1" to 3.0) // Total 5 shares -> $100/share
            )
        )

        val (net, owed, owe) = repository.calculateNetBalance(currentUserId, expenses, emptyList())

        // me paid 500. f1 share = 3 * 100 = 300.
        assertEquals(300.0, owed, 0.01)
        assertEquals(0.0, owe, 0.01)
        assertEquals(300.0, net, 0.01)
    }

    @Test
    fun testSharedExpenseCRUDAndRecalculations() = runBlocking {
        repository.setDemoMode(false)

        repository.addExpense(
            title = "Team Brunch",
            amount = 150.0,
            paidByUserId = "live_user_me",
            groupId = "g1",
            category = Category.FOOD,
            participantIds = listOf("live_user_me", "f1"),
            splitType = SplitType.EQUAL
        )

        val exps = repository.expenses.first()
        assertEquals(1, exps.size)
        val exp = exps.first()

        val (net1, owed1, _) = repository.calculateNetBalance("live_user_me", exps, emptyList())
        assertEquals(75.0, owed1, 0.01)

        // Update amount to 300
        val updatedExp = exp.copy(amount = 300.0)
        repository.updateExpense(updatedExp)

        val exps2 = repository.expenses.first()
        val (net2, owed2, _) = repository.calculateNetBalance("live_user_me", exps2, emptyList())
        assertEquals(150.0, owed2, 0.01)

        // Delete expense
        repository.deleteExpense(exp.id)
        val exps3 = repository.expenses.first()
        assertEquals(0, exps3.size)
    }

    @Test
    fun testSettlementMath() {
        val currentUserId = "me"
        val expenses = listOf(
            Expense(id = "1", title = "Lunch", amount = 100.0, paidByUserId = "me", participantIds = listOf("me", "f1"))
        )
        val settlements = listOf(
            Settlement(id = "s1", fromUserId = "f1", toUserId = "me", amount = 30.0, paymentMethod = PaymentMethod.UPI)
        )

        val (net, owed, owe) = repository.calculateNetBalance(currentUserId, expenses, settlements)

        // Initial f1 owed me 50. f1 paid 30 via UPI. Owed remains 20.
        assertEquals(20.0, owed, 0.01)
        assertEquals(0.0, owe, 0.01)
        assertEquals(20.0, net, 0.01)
    }

    @Test
    fun testDebtSimplificationPreservesNetPosition() {
        val uA = User("A", "Alice", "a@example.com", "")
        val uB = User("B", "Bob", "b@example.com", "")
        val uC = User("C", "Charlie", "c@example.com", "")
        val members = listOf(uA, uB, uC)

        // A paid 100 for A and B -> B owes A 50
        // B paid 100 for B and C -> C owes B 50
        val expenses = listOf(
            Expense("e1", "Lunch", 100.0, paidByUserId = "A", participantIds = listOf("A", "B")),
            Expense("e2", "Dinner", 100.0, paidByUserId = "B", participantIds = listOf("B", "C"))
        )

        val simplified = repository.simplifyDebts(null, expenses, emptyList(), members)

        // B net position: +50 from C, -50 to A = 0 net position.
        // Simplified result should be: C pays A 50 directly (1 transaction instead of 2).
        assertEquals(1, simplified.size)
        val transfer = simplified.first()
        assertEquals("C", transfer.fromUserId)
        assertEquals("A", transfer.toUserId)
        assertEquals(50.0, transfer.amount, 0.01)
    }

    @Test
    fun testGroupChatPersistenceAndIsolation() = runBlocking {
        repository.setDemoMode(false)

        repository.sendMessage("grp_1", "usr_1", "Hello Group 1")
        repository.sendMessage("grp_2", "usr_1", "Hello Group 2")

        val msgs1 = repository.observeMessagesForGroup("grp_1").first()
        val msgs2 = repository.observeMessagesForGroup("grp_2").first()

        assertEquals(1, msgs1.size)
        assertEquals("Hello Group 1", msgs1.first().message)

        assertEquals(1, msgs2.size)
        assertEquals("Hello Group 2", msgs2.first().message)
    }

    @Test
    fun testDemoAndLiveIsolation() = runBlocking {
        repository.setDemoMode(true)
        val demoExpensesCount = repository.expenses.first().size

        repository.setDemoMode(false)
        val liveExpensesCount = repository.expenses.first().size

        assertEquals(8, demoExpensesCount)
        assertEquals(0, liveExpensesCount)
    }

    @Test
    fun expenseEditAndDeleteLedgerReconcilesToCurrentBalance() = runBlocking {
        repository.setDemoMode(false)
        repository.addExpense("Dinner", 100.0, "live_user_me", "g", Category.FOOD,
            listOf("live_user_me", "friend"), SplitType.EQUAL)
        val original = repository.expenses.first().single()
        repository.updateExpense(original.copy(amount = 200.0))
        val afterEdit = repository.expenses.first()
        assertEquals(100.0, repository.calculateFriendBalance("live_user_me", "friend", afterEdit, emptyList()), 0.001)

        repository.deleteExpense(original.id)
        assertEquals(0, repository.expenses.first().size)
        val timeline = BalanceTimelineBuilder.build(repository.balanceHistory.first(), "live_user_me")
        assertEquals(0.0, timeline.finalBalances["friend"] ?: 0.0, 0.001)
        assertTrue(timeline.entries.any { it.eventType == BalanceEventType.EDIT_REVERSAL })
        assertTrue(timeline.entries.any { it.eventType == BalanceEventType.EDIT_APPLIED })
        assertTrue(timeline.entries.any { it.eventType == BalanceEventType.DELETION_ADJUSTMENT })
    }

    @Test
    fun settlementReversalRestoresPriorPairBalanceAndLedger() = runBlocking {
        repository.setDemoMode(false)
        repository.addExpense("Dinner", 100.0, "live_user_me", "g", Category.FOOD,
            listOf("live_user_me", "friend"), SplitType.EQUAL)
        repository.recordSettlement("friend", "live_user_me", 20.0, "g", PaymentMethod.UPI, "Partial payment")
        val settlement = repository.settlements.first().single()
        assertEquals(30.0, repository.calculateFriendBalance("live_user_me", "friend", repository.expenses.first(), repository.settlements.first()), 0.001)

        repository.reverseSettlement(settlement.id)
        assertEquals(50.0, repository.calculateFriendBalance("live_user_me", "friend", repository.expenses.first(), repository.settlements.first()), 0.001)
        val timeline = BalanceTimelineBuilder.build(repository.balanceHistory.first(), "live_user_me")
        assertEquals(50.0, timeline.finalBalances["friend"]!!, 0.001)
        assertTrue(timeline.entries.any { it.eventType == BalanceEventType.SETTLEMENT_REVERSAL })
    }

    @Test
    fun addMemberPersistsAndPreventsDuplicates() = runBlocking {
        repository.setDemoMode(false)
        repository.createGroup("Members", "", "M", emptyList())
        val group = repository.groups.first().single()
        repository.addMemberToGroup(group.id, "friend")
        repository.addMemberToGroup(group.id, "friend")
        val updated = repository.groups.first().single()
        assertEquals(listOf("live_user_me", "friend"), updated.memberIds)
    }
}
