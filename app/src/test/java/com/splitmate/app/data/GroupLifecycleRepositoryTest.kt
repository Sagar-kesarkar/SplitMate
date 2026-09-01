package com.splitmate.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.local.GroupEntity
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.model.Category
import com.splitmate.app.model.Group
import com.splitmate.app.model.GroupLifecyclePolicy
import com.splitmate.app.model.MuteDuration
import com.splitmate.app.model.SplitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GroupLifecycleRepositoryTest {
    private lateinit var context: Context
    private lateinit var liveDb: AppDatabase
    private lateinit var demoDb: AppDatabase
    private lateinit var repository: SplitmateRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        liveDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        demoDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = SplitmateRepository(context, liveDb, demoDb)
        repository.setDemoMode(false)
    }

    @After
    fun tearDown() {
        liveDb.close()
        demoDb.close()
    }

    @Test
    fun leaveRemovesOnlyCurrentMembershipAndProtectsOwner() = runBlocking {
        val memberGroup = group("member_group", owner = "friend")
        liveDb.groupDao().insertGroup(GroupEntity.fromDomainModel(memberGroup))
        val left = repository.leaveGroups(setOf(memberGroup.id), "live_user_me")
        assertEquals(1, left.appliedGroups.size)
        val persisted = liveDb.groupDao().getGroupById(memberGroup.id)!!.toDomainModel()
        assertFalse("live_user_me" in persisted.memberIds)
        assertTrue("friend" in persisted.memberIds)

        val owned = group("owned_group", owner = "live_user_me")
        liveDb.groupDao().insertGroup(GroupEntity.fromDomainModel(owned))
        val blocked = repository.leaveGroups(setOf(owned.id), "live_user_me")
        assertTrue(blocked.appliedGroups.isEmpty())
        assertTrue(blocked.blockedReasons.values.single().contains("Transfer ownership"))
        assertNotNull(liveDb.groupDao().getGroupById(owned.id))
    }

    @Test
    fun mutePersistsExpiresAndRemainsIsolatedFromDemo() = runBlocking {
        val liveGroup = group("live_group", owner = "live_user_me")
        liveDb.groupDao().insertGroup(GroupEntity.fromDomainModel(liveGroup))
        val now = 10_000L
        val until = MuteDuration.ONE_HOUR.mutedUntil(now)
        repository.muteGroups(setOf(liveGroup.id), until)
        assertTrue(GroupLifecyclePolicy.isMuted(liveDb.groupDao().getGroupById(liveGroup.id)!!.toDomainModel(), now))

        repository.refreshLifecycleState(until)
        assertNull(liveDb.groupDao().getGroupById(liveGroup.id)!!.mutedUntilMillis)

        repository.setDemoMode(true)
        assertTrue(repository.groups.first().none { it.id == liveGroup.id })
        repository.setDemoMode(false)
        assertFalse(GroupLifecyclePolicy.isMuted(liveDb.groupDao().getGroupById(liveGroup.id)!!.toDomainModel(), until))
    }

    @Test
    fun indefiniteMutePersistsAcrossRoomReopenAndCanBeUnmuted() = runBlocking {
        val databaseName = "group_mute_${UUID.randomUUID()}.db"
        var persistentLive = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries().build()
        var temporaryDemo = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        var persistentRepository = SplitmateRepository(context, persistentLive, temporaryDemo)
        persistentRepository.setDemoMode(false)
        val group = group("persistent_mute", owner = "live_user_me")
        persistentLive.groupDao().insertGroup(GroupEntity.fromDomainModel(group))
        persistentRepository.muteGroups(
            setOf(group.id),
            MuteDuration.INDEFINITE.mutedUntil(System.currentTimeMillis())
        )
        persistentLive.close()
        temporaryDemo.close()

        persistentLive = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries().build()
        temporaryDemo = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        persistentRepository = SplitmateRepository(context, persistentLive, temporaryDemo)
        persistentRepository.setDemoMode(false)
        val reopened = persistentLive.groupDao().getGroupById(group.id)!!.toDomainModel()
        assertTrue(GroupLifecyclePolicy.isMuted(reopened, System.currentTimeMillis()))

        persistentRepository.unmuteGroups(setOf(group.id))
        assertNull(persistentLive.groupDao().getGroupById(group.id)!!.mutedUntilMillis)
        persistentLive.close()
        temporaryDemo.close()
        context.deleteDatabase(databaseName)
        Unit
    }

    @Test
    fun deleteRequiresOwnerUndoRestoresAndExpiryCascadesRelatedData() = runBlocking {
        val owned = group("delete_group", owner = "live_user_me")
        liveDb.groupDao().insertGroup(GroupEntity.fromDomainModel(owned))
        repository.addExpense(
            "Dinner", 100.0, "live_user_me", owned.id, Category.FOOD,
            listOf("live_user_me", "friend"), SplitType.EQUAL
        )
        val beforeExpenses = repository.expenses.first().filter { it.groupId == owned.id }
        assertEquals(1, beforeExpenses.size)
        assertTrue(repository.balanceHistory.first().any { it.groupId == owned.id })

        val now = 20_000L
        val marked = repository.markGroupsPendingDeletion(setOf(owned.id), "live_user_me", now, now + 5_000L)
        assertEquals(1, marked.appliedGroups.size)
        assertTrue(repository.groups.first().none { it.id == owned.id })
        assertEquals(1, repository.pendingGroupDeletions.first().size)

        val restored = repository.undoGroupDeletions(setOf(owned.id), demoMode = false)
        assertEquals(1, restored.appliedGroups.size)
        assertNotNull(liveDb.groupDao().getGroupById(owned.id))
        assertEquals(1, repository.expenses.first().count { it.groupId == owned.id })

        repository.markGroupsPendingDeletion(setOf(owned.id), "live_user_me", now, now + 5_000L)
        assertEquals(1, repository.finalizeExpiredGroupDeletions(now + 5_000L))
        assertNull(liveDb.groupDao().getGroupById(owned.id))
        assertTrue(repository.expenses.first().none { it.groupId == owned.id })
        assertTrue(repository.balanceHistory.first().none { it.groupId == owned.id })
    }

    @Test
    fun nonOwnerDeleteIsBlockedAndLeavesDataUntouched() = runBlocking {
        val group = group("not_owned", owner = "friend")
        liveDb.groupDao().insertGroup(GroupEntity.fromDomainModel(group))
        val result = repository.markGroupsPendingDeletion(setOf(group.id), "live_user_me", 1L, 5_001L)
        assertTrue(result.appliedGroups.isEmpty())
        assertTrue(result.blockedReasons.values.single().contains("Only the group owner"))
        assertNotNull(liveDb.groupDao().getGroupById(group.id))
    }

    private fun group(id: String, owner: String) = Group(
        id = id,
        name = id,
        description = "",
        icon = "G",
        memberIds = listOf("live_user_me", "friend"),
        createdAt = 1L,
        ownerUserId = owner
    )
}
