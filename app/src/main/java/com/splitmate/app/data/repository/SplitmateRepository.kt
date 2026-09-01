package com.splitmate.app.data.repository

import android.content.Context
import com.splitmate.app.data.demo.DemoDataProvider
import com.splitmate.app.data.local.AppDatabase
import com.splitmate.app.data.local.AppPreferenceEntity
import com.splitmate.app.data.local.ChatMessageEntity
import com.splitmate.app.data.local.ExpenseEntity
import com.splitmate.app.data.local.FriendEntity
import com.splitmate.app.data.local.GroupEntity
import com.splitmate.app.data.local.PersonalExpenseEntity
import com.splitmate.app.data.local.SettlementEntity
import com.splitmate.app.data.local.UserEntity
import com.splitmate.app.data.local.BalanceHistoryEntity
import com.splitmate.app.model.BalanceEventType
import com.splitmate.app.model.BalanceHistoryEvent
import com.splitmate.app.model.Category
import com.splitmate.app.model.CategorySpending
import com.splitmate.app.model.ChatMessage
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Friend
import com.splitmate.app.model.FriendBalance
import com.splitmate.app.model.Group
import com.splitmate.app.model.GroupActionResult
import com.splitmate.app.model.GroupLifecyclePolicy
import com.splitmate.app.model.INDEFINITE_MUTE_MILLIS
import com.splitmate.app.model.PaymentMethod
import com.splitmate.app.model.PersonalExpense
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.SimplifiedDebt
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

import androidx.room.withTransaction

class SplitmateRepository(
    private val context: Context,
    private val liveDb: AppDatabase = AppDatabase.getLiveDatabase(context),
    private val demoDb: AppDatabase = AppDatabase.getDemoDatabase(context)
) {
    private companion object {
        const val DEMO_PERSONAL_SAMPLE_VERSION_KEY = "demo_personal_sample_version"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val demoSeedMutex = Mutex()
    private val initialization = CompletableDeferred<Unit>()

    private val _isDemoMode = MutableStateFlow(true)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    init {
        scope.launch {
            try {
                // Load saved demo mode preference
                val savedMode = liveDb.preferenceDao().getPreference("is_demo_mode")
                if (savedMode != null) {
                    _isDemoMode.value = savedMode.toBoolean()
                }
                // Seed databases if needed
                seedLiveDatabaseIfNeeded()
                ensureDemoDatabaseInitialized()
                refreshLifecycleState(liveDb, System.currentTimeMillis())
                refreshLifecycleState(demoDb, System.currentTimeMillis())
                initialization.complete(Unit)
            } catch (error: Throwable) {
                initialization.completeExceptionally(error)
            }
        }
    }

    private suspend fun seedLiveDatabaseIfNeeded() {
        withContext(Dispatchers.IO) {
            val user = liveDb.userDao().getUserById("live_user_me")
            if (user == null) {
                val defaultLiveUser = UserEntity(
                    id = "live_user_me",
                    name = "You",
                    email = "you@splitmate.app",
                    avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    isCurrentUser = true
                )
                liveDb.userDao().insertUser(defaultLiveUser)
            }
        }
    }

    private suspend fun ensureDemoDatabaseInitialized() = demoSeedMutex.withLock {
        withContext(Dispatchers.IO) {
            val marker = demoDb.preferenceDao().getPreference("demo_seed_complete")
            if (marker != "true") {
                val legacyUser = demoDb.userDao().getUserById(DemoDataProvider.currentUser.id)
                if (legacyUser != null) {
                    // Preserve an existing pre-marker Demo database exactly as the user left it.
                    demoDb.preferenceDao().setPreference(AppPreferenceEntity("demo_seed_complete", "true"))
                } else {
                    seedDemoDataLocked()
                }
            }
            ensureCurrentDemoPersonalSamplesLocked()
        }
    }

    /**
     * Applies each requested Demo-content revision once. Stable IDs update last month's
     * samples instead of creating duplicates, while the marker prevents launch-time reseeding.
     */
    private suspend fun ensureCurrentDemoPersonalSamplesLocked() {
        val installedVersion = demoDb.preferenceDao().getPreference(DEMO_PERSONAL_SAMPLE_VERSION_KEY)
        if (installedVersion == DemoDataProvider.PERSONAL_SAMPLE_DATA_VERSION) return
        demoDb.withTransaction {
            demoDb.personalExpenseDao().insertPersonalExpenses(
                DemoDataProvider.demoPersonalExpenses.map(PersonalExpenseEntity::fromDomainModel)
            )
            demoDb.preferenceDao().setPreference(
                AppPreferenceEntity(
                    DEMO_PERSONAL_SAMPLE_VERSION_KEY,
                    DemoDataProvider.PERSONAL_SAMPLE_DATA_VERSION
                )
            )
        }
    }

    private suspend fun seedDemoDataLocked() {
        demoDb.withTransaction {
                    demoDb.expenseDao().deleteAll()
                    demoDb.settlementDao().deleteAll()
                    demoDb.groupDao().deleteAll()
                    demoDb.friendDao().deleteAll()
                    demoDb.userDao().deleteAll()
                    demoDb.personalExpenseDao().deleteAll()
                    demoDb.balanceHistoryDao().deleteAll()
                    demoDb.chatMessageDao().deleteAll()

                    demoDb.userDao().insertUsers(DemoDataProvider.allDemoUsers.map { UserEntity.fromDomainModel(it) })
                    demoDb.friendDao().insertFriends(DemoDataProvider.demoFriends.map { FriendEntity.fromDomainModel(it) })
                    demoDb.groupDao().insertGroups(DemoDataProvider.demoGroups.map { GroupEntity.fromDomainModel(it) })
                    demoDb.expenseDao().insertExpenses(DemoDataProvider.demoExpenses.map { ExpenseEntity.fromDomainModel(it) })
                    demoDb.settlementDao().insertSettlements(DemoDataProvider.demoSettlements.map { SettlementEntity.fromDomainModel(it) })
                    demoDb.personalExpenseDao().insertPersonalExpenses(DemoDataProvider.demoPersonalExpenses.map { PersonalExpenseEntity.fromDomainModel(it) })
                    val events = DemoDataProvider.demoExpenses.flatMap { expense ->
                        expenseEvents(expense, BalanceEventType.EXPENSE, 1.0, expense.dateMillis, "seed_${expense.id}", DemoDataProvider.CURRENT_USER_ID)
                    } + DemoDataProvider.demoSettlements.mapNotNull { settlement ->
                        settlementEvent(settlement, BalanceEventType.SETTLEMENT, 1.0, settlement.dateMillis, "seed_${settlement.id}", DemoDataProvider.CURRENT_USER_ID)
                    }
                    demoDb.balanceHistoryDao().insertAll(events.map(BalanceHistoryEntity::fromDomainModel))
                    demoDb.preferenceDao().setPreference(AppPreferenceEntity("demo_seed_complete", "true"))
                    demoDb.preferenceDao().setPreference(
                        AppPreferenceEntity(
                            DEMO_PERSONAL_SAMPLE_VERSION_KEY,
                            DemoDataProvider.PERSONAL_SAMPLE_DATA_VERSION
                        )
                    )
        }
    }

    private suspend fun resetDemoDataLocked() = demoSeedMutex.withLock {
        withContext(Dispatchers.IO) {
            seedDemoDataLocked()
        }
    }

    suspend fun setDemoMode(enabled: Boolean) {
        initialization.await()
        _isDemoMode.value = enabled
        withContext(Dispatchers.IO) {
            liveDb.preferenceDao().setPreference(AppPreferenceEntity("is_demo_mode", enabled.toString()))
            if (enabled) {
                ensureDemoDatabaseInitialized()
            } else {
                seedLiveDatabaseIfNeeded()
            }
        }
    }

    suspend fun resetDemoData() {
        initialization.await()
        withContext(Dispatchers.IO) {
            resetDemoDataLocked()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: Flow<List<User>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.userDao().getAllUsersFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: Flow<List<Friend>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.friendDao().getAllFriendsFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val groups: Flow<List<Group>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.groupDao().getAllGroupsFlow().map { list ->
            list.map { it.toDomainModel() }.filterNot(GroupLifecyclePolicy::isPendingDeletion)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingGroupDeletions: Flow<List<Group>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.groupDao().getPendingDeletionGroupsFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: Flow<List<Expense>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.expenseDao().getAllExpensesFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val settlements: Flow<List<Settlement>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.settlementDao().getAllSettlementsFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val personalExpenses: Flow<List<PersonalExpense>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.personalExpenseDao().getAllPersonalExpensesFlow().map { list -> list.map { it.toDomainModel() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val balanceHistory: Flow<List<BalanceHistoryEvent>> = _isDemoMode.flatMapLatest { isDemo ->
        (if (isDemo) demoDb else liveDb).balanceHistoryDao().getAllFlow().map { rows -> rows.map { it.toDomainModel() } }
    }

    private fun expenseEvents(
        expense: Expense,
        type: BalanceEventType,
        multiplier: Double,
        eventTime: Long,
        batchId: String = UUID.randomUUID().toString(),
        perspectiveUserId: String = if (_isDemoMode.value) DemoDataProvider.CURRENT_USER_ID else "live_user_me"
    ): List<BalanceHistoryEvent> {
        val groupId = expense.groupId ?: return emptyList()
        val me = perspectiveUserId
        val shares = BalanceCalculator.shares(expense)
        return expense.participantIds.distinct().mapIndexed { index, participantId ->
            val allocation = shares[participantId] ?: 0.0
            val change = when {
                expense.paidByUserId == me && participantId != me -> allocation * multiplier
                expense.paidByUserId != me && participantId == me -> -allocation * multiplier
                else -> 0.0
            }
            BalanceHistoryEvent("hist_${batchId}_${participantId}_$index", groupId, expense.id, expense.title, type,
                expense.paidByUserId, participantId, expense.amount, allocation, change, eventTime)
        }
    }

    private fun settlementEvent(
        settlement: Settlement,
        type: BalanceEventType,
        multiplier: Double,
        eventTime: Long,
        batchId: String = UUID.randomUUID().toString(),
        perspectiveUserId: String = if (_isDemoMode.value) DemoDataProvider.CURRENT_USER_ID else "live_user_me"
    ): BalanceHistoryEvent? {
        val groupId = settlement.groupId ?: return null
        val me = perspectiveUserId
        if (settlement.fromUserId != me && settlement.toUserId != me) return null
        val other = if (settlement.fromUserId == me) settlement.toUserId else settlement.fromUserId
        val change = (if (settlement.fromUserId == me) settlement.amount else -settlement.amount) * multiplier
        return BalanceHistoryEvent("hist_${batchId}_$other", groupId, settlement.id,
            settlement.note.ifBlank { "Settlement" }, type, settlement.fromUserId, other,
            settlement.amount, settlement.amount, change, eventTime)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMessagesForGroup(groupId: String): Flow<List<ChatMessage>> = _isDemoMode.flatMapLatest { isDemo ->
        val db = if (isDemo) demoDb else liveDb
        db.chatMessageDao().getMessagesForGroupFlow(groupId).map { list -> list.map { it.toDomainModel() } }
    }

    private fun currentDb(): AppDatabase {
        return if (_isDemoMode.value) demoDb else liveDb
    }

    suspend fun addExpense(
        title: String,
        amount: Double,
        paidByUserId: String,
        groupId: String?,
        category: Category,
        participantIds: List<String>,
        splitType: SplitType,
        customSplits: Map<String, Double> = emptyMap(),
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val expense = Expense(
            id = "exp_${UUID.randomUUID().toString().take(8)}",
            title = title,
            amount = amount,
            paidByUserId = paidByUserId,
            groupId = groupId,
            category = category,
            dateMillis = System.currentTimeMillis(),
            participantIds = participantIds,
            splitType = splitType,
            customSplits = customSplits,
            notes = notes
        )
        val db = currentDb()
        db.withTransaction {
            db.expenseDao().insertExpense(ExpenseEntity.fromDomainModel(expense))
            db.balanceHistoryDao().insertAll(expenseEvents(expense, BalanceEventType.EXPENSE, 1.0, expense.dateMillis).map(BalanceHistoryEntity::fromDomainModel))
        }
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val db = currentDb()
        db.withTransaction {
            val now = System.currentTimeMillis()
            db.expenseDao().getExpenseById(expense.id)?.toDomainModel()?.let { old ->
                db.balanceHistoryDao().insertAll(expenseEvents(old, BalanceEventType.EDIT_REVERSAL, -1.0, now).map(BalanceHistoryEntity::fromDomainModel))
            }
            db.expenseDao().updateExpense(ExpenseEntity.fromDomainModel(expense))
            db.balanceHistoryDao().insertAll(expenseEvents(expense, BalanceEventType.EDIT_APPLIED, 1.0, now + 1L).map(BalanceHistoryEntity::fromDomainModel))
        }
    }

    suspend fun deleteExpense(expenseId: String) = withContext(Dispatchers.IO) {
        val db = currentDb()
        db.withTransaction {
            db.expenseDao().getExpenseById(expenseId)?.toDomainModel()?.let { old ->
                db.balanceHistoryDao().insertAll(expenseEvents(old, BalanceEventType.DELETION_ADJUSTMENT, -1.0, System.currentTimeMillis()).map(BalanceHistoryEntity::fromDomainModel))
            }
            db.expenseDao().deleteExpense(expenseId)
        }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        icon: String,
        memberIds: List<String>
    ) = withContext(Dispatchers.IO) {
        val db = currentDb()
        val allUsers = if (_isDemoMode.value) DemoDataProvider.currentUser.id else "live_user_me"
        val fullMembers = (listOf(allUsers) + memberIds).distinct()
        val group = Group(
            id = "grp_${UUID.randomUUID().toString().take(8)}",
            name = name,
            description = description,
            icon = icon,
            memberIds = fullMembers,
            createdAt = System.currentTimeMillis(),
            ownerUserId = allUsers
        )
        db.groupDao().insertGroup(GroupEntity.fromDomainModel(group))
    }

    suspend fun updateGroup(group: Group) = withContext(Dispatchers.IO) {
        currentDb().groupDao().updateGroup(GroupEntity.fromDomainModel(group))
    }

    suspend fun setGroupBudget(groupId: String, budget: Double?) = withContext(Dispatchers.IO) {
        val dao = currentDb().groupDao()
        val group = dao.getGroupById(groupId)?.toDomainModel() ?: return@withContext
        dao.updateGroup(GroupEntity.fromDomainModel(group.copy(budget = budget)))
    }

    suspend fun addMemberToGroup(groupId: String, memberId: String) = withContext(Dispatchers.IO) {
        val db = currentDb()
        val groupEntity = db.groupDao().getGroupById(groupId) ?: return@withContext
        val group = groupEntity.toDomainModel()
        if (!group.memberIds.contains(memberId)) {
            val updated = group.copy(memberIds = group.memberIds + memberId)
            db.groupDao().updateGroup(GroupEntity.fromDomainModel(updated))
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, memberId: String) = withContext(Dispatchers.IO) {
        val db = currentDb()
        val groupEntity = db.groupDao().getGroupById(groupId) ?: return@withContext
        val group = groupEntity.toDomainModel()
        val updated = group.copy(memberIds = group.memberIds - memberId)
        db.groupDao().updateGroup(GroupEntity.fromDomainModel(updated))
    }

    suspend fun muteGroups(groupIds: Set<String>, mutedUntilMillis: Long): GroupActionResult = withContext(Dispatchers.IO) {
        val db = currentDb()
        val applied = mutableListOf<Group>()
        val blocked = linkedMapOf<String, String>()
        db.withTransaction {
            groupIds.forEach { groupId ->
                val group = db.groupDao().getGroupById(groupId)?.toDomainModel()
                when {
                    group == null -> blocked[groupId] = "Group is no longer available"
                    GroupLifecyclePolicy.isPendingDeletion(group) -> blocked[group.name] = "Pending deletion"
                    else -> {
                        db.groupDao().updateMute(groupId, mutedUntilMillis)
                        applied += group.copy(mutedUntilMillis = mutedUntilMillis)
                    }
                }
            }
        }
        GroupActionResult(applied, blocked)
    }

    suspend fun unmuteGroups(groupIds: Set<String>): GroupActionResult = withContext(Dispatchers.IO) {
        val db = currentDb()
        val applied = mutableListOf<Group>()
        val blocked = linkedMapOf<String, String>()
        db.withTransaction {
            groupIds.forEach { groupId ->
                val group = db.groupDao().getGroupById(groupId)?.toDomainModel()
                if (group == null) blocked[groupId] = "Group is no longer available"
                else {
                    db.groupDao().updateMute(groupId, null)
                    applied += group.copy(mutedUntilMillis = null)
                }
            }
        }
        GroupActionResult(applied, blocked)
    }

    suspend fun leaveGroups(groupIds: Set<String>, currentUserId: String): GroupActionResult = withContext(Dispatchers.IO) {
        val db = currentDb()
        val applied = mutableListOf<Group>()
        val blocked = linkedMapOf<String, String>()
        db.withTransaction {
            groupIds.forEach { groupId ->
                val entity = db.groupDao().getGroupById(groupId)
                val group = entity?.toDomainModel()
                when {
                    group == null -> blocked[groupId] = "Group is no longer available"
                    currentUserId !in group.memberIds -> blocked[group.name] = "You are no longer a member"
                    GroupLifecyclePolicy.ownerId(group) == currentUserId ->
                        blocked[group.name] = "You own this group. Transfer ownership or delete it instead."
                    else -> {
                        val updated = group.copy(memberIds = group.memberIds - currentUserId)
                        db.groupDao().updateGroup(GroupEntity.fromDomainModel(updated))
                        applied += updated
                    }
                }
            }
        }
        GroupActionResult(applied, blocked)
    }

    suspend fun markGroupsPendingDeletion(
        groupIds: Set<String>,
        currentUserId: String,
        deletedAtMillis: Long,
        deadlineMillis: Long
    ): GroupActionResult = withContext(Dispatchers.IO) {
        val db = currentDb()
        val applied = mutableListOf<Group>()
        val blocked = linkedMapOf<String, String>()
        db.withTransaction {
            groupIds.forEach { groupId ->
                val group = db.groupDao().getGroupById(groupId)?.toDomainModel()
                when {
                    group == null -> blocked[groupId] = "Group is no longer available"
                    !GroupLifecyclePolicy.canDelete(group, currentUserId) ->
                        blocked[group.name] = "Only the group owner can delete it. Use Leave instead."
                    GroupLifecyclePolicy.isPendingDeletion(group) -> blocked[group.name] = "Already pending deletion"
                    else -> {
                        db.groupDao().markPendingDeletion(groupId, deletedAtMillis, deadlineMillis)
                        applied += group.copy(
                            pendingDeletionAtMillis = deletedAtMillis,
                            deletionDeadlineMillis = deadlineMillis
                        )
                    }
                }
            }
        }
        GroupActionResult(applied, blocked)
    }

    suspend fun undoGroupDeletions(
        groupIds: Set<String>,
        demoMode: Boolean = _isDemoMode.value
    ): GroupActionResult = withContext(Dispatchers.IO) {
        // The undo snackbar can outlive a mode switch. Keep the operation tied to
        // the database in which deletion was requested instead of the current UI mode.
        val db = if (demoMode) demoDb else liveDb
        val applied = mutableListOf<Group>()
        val blocked = linkedMapOf<String, String>()
        db.withTransaction {
            groupIds.forEach { groupId ->
                val group = db.groupDao().getGroupById(groupId)?.toDomainModel()
                if (group == null) blocked[groupId] = "Undo window has expired"
                else if (!GroupLifecyclePolicy.isPendingDeletion(group)) blocked[group.name] = "Group is not pending deletion"
                else {
                    db.groupDao().markPendingDeletion(groupId, null, null)
                    applied += group.copy(pendingDeletionAtMillis = null, deletionDeadlineMillis = null)
                }
            }
        }
        GroupActionResult(applied, blocked)
    }

    suspend fun finalizeExpiredGroupDeletions(nowMillis: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        finalizeExpiredGroupDeletions(currentDb(), nowMillis)
    }

    suspend fun refreshLifecycleState(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        refreshLifecycleState(currentDb(), nowMillis)
    }

    private suspend fun refreshLifecycleState(db: AppDatabase, nowMillis: Long) {
        db.groupDao().clearExpiredMutes(nowMillis, INDEFINITE_MUTE_MILLIS)
        finalizeExpiredGroupDeletions(db, nowMillis)
    }

    private suspend fun finalizeExpiredGroupDeletions(db: AppDatabase, nowMillis: Long): Int {
        val expiredGroups = db.groupDao().getAllGroups()
            .filter { it.pendingDeletionAtMillis != null && (it.deletionDeadlineMillis ?: Long.MAX_VALUE) <= nowMillis }
        if (expiredGroups.isEmpty()) return 0
        db.withTransaction {
            expiredGroups.forEach { group ->
                    db.balanceHistoryDao().deleteForGroup(group.id)
                    db.chatMessageDao().deleteMessagesForGroup(group.id)
                    db.settlementDao().deleteSettlementsForGroup(group.id)
                    db.expenseDao().deleteExpensesForGroup(group.id)
                    db.groupDao().deleteGroup(group.id)
            }
        }
        return expiredGroups.size
    }

    suspend fun addFriend(name: String, email: String, phone: String = "") = withContext(Dispatchers.IO) {
        val db = currentDb()
        val friendId = "usr_${UUID.randomUUID().toString().take(8)}"
        val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80"
        val friend = Friend(id = friendId, name = name, email = email, avatar = avatar, phone = phone)
        val user = User(id = friendId, name = name, email = email, avatar = avatar, isCurrentUser = false)
        db.friendDao().insertFriend(FriendEntity.fromDomainModel(friend))
        db.userDao().insertUser(UserEntity.fromDomainModel(user))
    }

    suspend fun deleteFriend(friendId: String) = withContext(Dispatchers.IO) {
        val db = currentDb()
        db.friendDao().deleteFriend(friendId)
        db.userDao().deleteUser(friendId)
    }

    suspend fun addPersonalExpense(
        title: String,
        amount: Double,
        category: Category,
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        initialization.await()
        val expense = PersonalExpense(
            id = "pexp_${UUID.randomUUID().toString().take(8)}",
            title = title,
            amount = amount,
            category = category,
            notes = notes,
            dateMillis = dateMillis
        )
        currentDb().personalExpenseDao().insertPersonalExpense(PersonalExpenseEntity.fromDomainModel(expense))
    }

    suspend fun updatePersonalExpense(expense: PersonalExpense) = withContext(Dispatchers.IO) {
        initialization.await()
        currentDb().personalExpenseDao().updatePersonalExpense(PersonalExpenseEntity.fromDomainModel(expense))
    }

    suspend fun deletePersonalExpense(expenseId: String) = withContext(Dispatchers.IO) {
        initialization.await()
        currentDb().personalExpenseDao().deletePersonalExpense(expenseId)
    }

    suspend fun sendMessage(groupId: String, senderId: String, message: String) = withContext(Dispatchers.IO) {
        val chat = ChatMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            groupId = groupId,
            senderId = senderId,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        currentDb().chatMessageDao().insertMessage(ChatMessageEntity.fromDomainModel(chat))
    }

    suspend fun recordSettlement(
        fromUserId: String,
        toUserId: String,
        amount: Double,
        groupId: String?,
        paymentMethod: PaymentMethod,
        note: String
    ) = withContext(Dispatchers.IO) {
        val settlement = Settlement(
            id = "stl_${UUID.randomUUID().toString().take(8)}",
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            dateMillis = System.currentTimeMillis(),
            groupId = groupId,
            paymentMethod = paymentMethod,
            note = note
        )
        val db = currentDb()
        db.withTransaction {
            db.settlementDao().insertSettlement(SettlementEntity.fromDomainModel(settlement))
            settlementEvent(settlement, BalanceEventType.SETTLEMENT, 1.0, settlement.dateMillis)?.let { event ->
                db.balanceHistoryDao().insertAll(listOf(BalanceHistoryEntity.fromDomainModel(event)))
            }
        }
    }

    suspend fun reverseSettlement(settlementId: String) = withContext(Dispatchers.IO) {
        val db = currentDb()
        db.withTransaction {
            val settlement = db.settlementDao().getSettlementById(settlementId)?.toDomainModel() ?: return@withTransaction
            settlementEvent(settlement, BalanceEventType.SETTLEMENT_REVERSAL, -1.0, System.currentTimeMillis())?.let { event ->
                db.balanceHistoryDao().insertAll(listOf(BalanceHistoryEntity.fromDomainModel(event)))
            }
            db.settlementDao().deleteSettlement(settlementId)
        }
    }

    // Calculations
    fun calculateNetBalance(
        currentUserId: String,
        expensesList: List<Expense>,
        settlementsList: List<Settlement>
    ): Triple<Double, Double, Double> {
        val balances = BalanceCalculator.allPairBalances(currentUserId, expensesList, settlementsList).values
        val youAreOwed = balances.filter { it > 0.0 }.sum()
        val youOwe = -balances.filter { it < 0.0 }.sum()
        val net = balances.sum()
        return Triple(net, youAreOwed, youOwe)
    }

    fun calculateFriendBalance(
        currentUserId: String,
        friendId: String,
        expensesList: List<Expense>,
        settlementsList: List<Settlement>
    ): Double {
        return BalanceCalculator.pairBalance(currentUserId, friendId, expensesList, settlementsList)
    }

    fun simplifyDebts(
        groupId: String?,
        expensesList: List<Expense>,
        settlementsList: List<Settlement>,
        membersList: List<User>
    ): List<SimplifiedDebt> {
        val memberIds = membersList.map { it.id }.toSet()
        val netBalances = mutableMapOf<String, Double>()
        memberIds.forEach { netBalances[it] = 0.0 }

        val filteredExpenses = if (groupId != null) expensesList.filter { it.groupId == groupId } else expensesList
        val filteredSettlements = if (groupId != null) settlementsList.filter { it.groupId == groupId } else settlementsList

        for (expense in filteredExpenses) {
            val participants = expense.participantIds.filter { memberIds.contains(it) }
            if (participants.isEmpty()) continue

            val userShares: Map<String, Double> = when (expense.splitType) {
                SplitType.EQUAL -> {
                    val share = expense.amount / participants.size
                    participants.associateWith { share }
                }
                SplitType.EXACT -> expense.customSplits
                SplitType.PERCENT -> expense.customSplits.mapValues { (_, p) -> (expense.amount * p) / 100.0 }
                SplitType.SHARES -> {
                    val totalShares = expense.customSplits.values.sum().coerceAtLeast(1.0)
                    expense.customSplits.mapValues { (_, s) -> (expense.amount * s) / totalShares }
                }
            }

            if (memberIds.contains(expense.paidByUserId)) {
                netBalances[expense.paidByUserId] = (netBalances[expense.paidByUserId] ?: 0.0) + expense.amount
            }

            for ((uid, share) in userShares) {
                if (memberIds.contains(uid)) {
                    netBalances[uid] = (netBalances[uid] ?: 0.0) - share
                }
            }
        }

        for (settlement in filteredSettlements) {
            if (memberIds.contains(settlement.fromUserId)) {
                netBalances[settlement.fromUserId] = (netBalances[settlement.fromUserId] ?: 0.0) + settlement.amount
            }
            if (memberIds.contains(settlement.toUserId)) {
                netBalances[settlement.toUserId] = (netBalances[settlement.toUserId] ?: 0.0) - settlement.amount
            }
        }

        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        for ((uid, bal) in netBalances) {
            if (bal < -0.01) {
                debtors.add(uid to -bal)
            } else if (bal > 0.01) {
                creditors.add(uid to bal)
            }
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val simplified = mutableListOf<SimplifiedDebt>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtorId, debtAmt) = debtors[dIdx]
            val (creditorId, creditAmt) = creditors[cIdx]

            val settled = minOf(debtAmt, creditAmt)
            simplified.add(SimplifiedDebt(debtorId, creditorId, settled))

            if (debtAmt - settled < 0.01) {
                dIdx++
            } else {
                debtors[dIdx] = debtorId to (debtAmt - settled)
            }

            if (creditAmt - settled < 0.01) {
                cIdx++
            } else {
                creditors[cIdx] = creditorId to (creditAmt - settled)
            }
        }

        return simplified
    }

    fun getCategorySpending(expensesList: List<Expense>): List<CategorySpending> {
        val total = expensesList.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return Category.values().mapNotNull { cat ->
            val catTotal = expensesList.filter { it.category == cat }.sumOf { it.amount }
            if (catTotal > 0.0) {
                CategorySpending(
                    category = cat,
                    totalAmount = catTotal,
                    percentage = (catTotal / total) * 100.0
                )
            } else null
        }.sortedByDescending { it.totalAmount }
    }
}
