package com.splitmate.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends")
    fun getAllFriendsFlow(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: String)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE pendingDeletionAtMillis IS NOT NULL ORDER BY pendingDeletionAtMillis ASC")
    fun getPendingDeletionGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("UPDATE groups SET mutedUntilMillis = :mutedUntilMillis WHERE id = :id")
    suspend fun updateMute(id: String, mutedUntilMillis: Long?)

    @Query("UPDATE groups SET mutedUntilMillis = NULL WHERE mutedUntilMillis IS NOT NULL AND mutedUntilMillis != :indefiniteValue AND mutedUntilMillis <= :nowMillis")
    suspend fun clearExpiredMutes(nowMillis: Long, indefiniteValue: Long = Long.MAX_VALUE)

    @Query("UPDATE groups SET pendingDeletionAtMillis = :deletedAtMillis, deletionDeadlineMillis = :deadlineMillis WHERE id = :id")
    suspend fun markPendingDeletion(id: String, deletedAtMillis: Long?, deadlineMillis: Long?)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesForGroup(groupId: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements ORDER BY dateMillis DESC")
    fun getAllSettlementsFlow(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :id LIMIT 1")
    suspend fun getSettlementById(id: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlement(id: String)

    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsForGroup(groupId: String)

    @Query("DELETE FROM settlements")
    suspend fun deleteAll()
}

@Dao
interface PreferenceDao {
    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    fun getPreferenceFlow(key: String): Flow<String?>

    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface PersonalExpenseDao {
    @Query("SELECT * FROM personal_expenses ORDER BY dateMillis DESC")
    fun getAllPersonalExpensesFlow(): Flow<List<PersonalExpenseEntity>>

    @Query("SELECT * FROM personal_expenses WHERE dateMillis >= :startOfMonth AND dateMillis < :endOfMonth ORDER BY dateMillis DESC")
    fun getPersonalExpensesForMonthFlow(startOfMonth: Long, endOfMonth: Long): Flow<List<PersonalExpenseEntity>>

    @Query("SELECT * FROM personal_expenses WHERE id = :id LIMIT 1")
    suspend fun getPersonalExpenseById(id: String): PersonalExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalExpense(expense: PersonalExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalExpenses(expenses: List<PersonalExpenseEntity>)

    @Update
    suspend fun updatePersonalExpense(expense: PersonalExpenseEntity)

    @Query("DELETE FROM personal_expenses WHERE id = :id")
    suspend fun deletePersonalExpense(id: String)

    @Query("DELETE FROM personal_expenses")
    suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroupFlow(groupId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesForGroup(groupId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}

@Dao
interface BalanceHistoryDao {
    @Query("SELECT * FROM balance_history ORDER BY dateMillis ASC")
    fun getAllFlow(): Flow<List<BalanceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BalanceHistoryEntity>)

    @Query("DELETE FROM balance_history")
    suspend fun deleteAll()

    @Query("DELETE FROM balance_history WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)
}
