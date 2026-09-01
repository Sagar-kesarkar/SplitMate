package com.splitmate.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.model.Category
import com.splitmate.app.model.SplitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomPersistenceTest {
    @Test
    fun budgetExpensesAndLiveIsolationSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "splitmate_persistence_${UUID.randomUUID()}.db"
        var live = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        var demo = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        var repository = SplitmateRepository(context, live, demo)
        repository.setDemoMode(false)
        repository.createGroup("Persistent Group", "", "P", listOf("friend"))
        val group = repository.groups.first().single()
        repository.setGroupBudget(group.id, 100.0)
        repository.addExpense("Over budget bill", 125.0, "live_user_me", group.id, Category.GENERAL,
            listOf("live_user_me", "friend"), SplitType.EQUAL)
        assertEquals(4, live.openHelper.writableDatabase.version)
        live.close()
        demo.close()

        live = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        demo = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = SplitmateRepository(context, live, demo)
        repository.setDemoMode(false)
        val reopenedGroup = repository.groups.first().single()
        val reopenedExpenses = repository.expenses.first()
        assertEquals(100.0, reopenedGroup.budget!!, 0.001)
        assertEquals(125.0, BalanceCalculator.groupTotalSpent(reopenedGroup.id, reopenedExpenses), 0.001)
        assertEquals(-25.0, BalanceCalculator.budgetRemaining(reopenedGroup.budget, 125.0)!!, 0.001)
        assertFalse(reopenedExpenses.any { it.id.startsWith("demo_") })
        live.close()
        demo.close()
        context.deleteDatabase(name)
        Unit
    }
}
