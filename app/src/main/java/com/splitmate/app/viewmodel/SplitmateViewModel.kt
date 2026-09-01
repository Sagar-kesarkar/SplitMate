package com.splitmate.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.splitmate.app.data.demo.DemoDataProvider
import com.splitmate.app.data.repository.SplitmateRepository
import com.splitmate.app.data.repository.BalanceCalculator
import com.splitmate.app.model.Category
import com.splitmate.app.model.CategorySpending
import com.splitmate.app.model.ChatMessage
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Friend
import com.splitmate.app.model.FriendBalance
import com.splitmate.app.model.Group
import com.splitmate.app.model.GROUP_DELETE_UNDO_WINDOW_MILLIS
import com.splitmate.app.model.GroupLifecyclePolicy
import com.splitmate.app.model.MuteDuration
import com.splitmate.app.model.PaymentMethod
import com.splitmate.app.model.PersonalExpense
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.SimplifiedDebt
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import com.splitmate.app.model.BalanceHistoryEvent
import com.splitmate.app.util.CurrencyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavigationTab(val testTag: String) {
    HOME("nav_tab_home"),
    EXPENSES("nav_tab_expenses"),
    GROUPS("nav_tab_groups"),
    FRIENDS("nav_tab_friends"),
    ANALYTICS("nav_tab_analytics"),
    PERSONAL_EXPENSES("nav_tab_personal"),
    GROUP_CHAT("nav_tab_chat"),
    GROUP_BALANCE_DETAILS("nav_group_balance_details")
}

internal enum class QuickExpenseDestination { PERSONAL, SHARED }

internal fun quickExpenseDestination(groupId: String?): QuickExpenseDestination =
    if (groupId == null) QuickExpenseDestination.PERSONAL else QuickExpenseDestination.SHARED

enum class GroupActionType { MUTE, LEAVE, DELETE }

data class GroupActionDialogState(
    val type: GroupActionType,
    val groupIds: Set<String>
)

data class GroupDeletionNotice(
    val groupIds: Set<String>,
    val demoMode: Boolean,
    val message: String
)

data class SplitmateUiState(
    val isDemoMode: Boolean = true,
    val currentMode: String = "DEMO",
    val currentUserId: String = DemoDataProvider.currentUser.id,
    val selectedTab: NavigationTab = NavigationTab.HOME,
    val users: List<User> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val groups: List<Group> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val personalExpenses: List<PersonalExpense> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val personalExpenseTotalMonth: Double = 0.0,
    val totalOwed: Double = 0.0,
    val totalOwe: Double = 0.0,
    val netBalance: Double = 0.0,
    val searchQuery: String = "",
    val selectedCategoryFilter: Category? = null,
    val selectedGroupId: String? = null,
    val selectedFriendId: String? = null,
    val selectedExpenseId: String? = null,
    val selectedPersonalExpenseId: String? = null,
    val showAddExpenseDialog: Boolean = false,
    val showAddPersonalExpenseDialog: Boolean = false,
    val editingExpense: Expense? = null,
    val editingPersonalExpense: PersonalExpense? = null,
    val prefilledGroupId: String? = null,
    val showCreateGroupDialog: Boolean = false,
    val showAddFriendDialog: Boolean = false,
    val showSettleUpDialog: Boolean = false,
    val prefilledSettleReceiverId: String? = null,
    val prefilledSettleGroupId: String? = null,
    val snackbarMessage: String? = null
)

class SplitmateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SplitmateRepository(application.applicationContext)

    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode
    val users: StateFlow<List<User>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val friends: StateFlow<List<Friend>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val groups: StateFlow<List<Group>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingGroupDeletions: StateFlow<List<Group>> = repository.pendingGroupDeletions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val expenses: StateFlow<List<Expense>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settlements: StateFlow<List<Settlement>> = repository.settlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalExpenses: StateFlow<List<PersonalExpense>> = repository.personalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val balanceHistory: StateFlow<List<BalanceHistoryEvent>> = repository.balanceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(NavigationTab.HOME)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Category?>(null)
    val selectedCategoryFilter: StateFlow<Category?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _selectedFriendId = MutableStateFlow<String?>(null)
    val selectedFriendId: StateFlow<String?> = _selectedFriendId.asStateFlow()

    private val _selectedExpenseId = MutableStateFlow<String?>(null)
    val selectedExpenseId: StateFlow<String?> = _selectedExpenseId.asStateFlow()

    private val _selectedPersonalExpenseId = MutableStateFlow<String?>(null)
    val selectedPersonalExpenseId: StateFlow<String?> = _selectedPersonalExpenseId.asStateFlow()

    private val _showAddExpenseDialog = MutableStateFlow(false)
    val showAddExpenseDialog: StateFlow<Boolean> = _showAddExpenseDialog.asStateFlow()

    private val _showAddPersonalExpenseDialog = MutableStateFlow(false)
    val showAddPersonalExpenseDialog: StateFlow<Boolean> = _showAddPersonalExpenseDialog.asStateFlow()

    private val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()

    private val _editingPersonalExpense = MutableStateFlow<PersonalExpense?>(null)
    val editingPersonalExpense: StateFlow<PersonalExpense?> = _editingPersonalExpense.asStateFlow()

    private val _prefilledGroupId = MutableStateFlow<String?>(null)
    val prefilledGroupId: StateFlow<String?> = _prefilledGroupId.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    private val _showAddFriendDialog = MutableStateFlow(false)
    val showAddFriendDialog: StateFlow<Boolean> = _showAddFriendDialog.asStateFlow()

    private val _showSettleUpDialog = MutableStateFlow(false)
    val showSettleUpDialog: StateFlow<Boolean> = _showSettleUpDialog.asStateFlow()

    private val _prefilledSettleReceiverId = MutableStateFlow<String?>(null)
    val prefilledSettleReceiverId: StateFlow<String?> = _prefilledSettleReceiverId.asStateFlow()

    private val _prefilledSettleGroupId = MutableStateFlow<String?>(null)
    val prefilledSettleGroupId: StateFlow<String?> = _prefilledSettleGroupId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _selectedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedGroupIds: StateFlow<Set<String>> = _selectedGroupIds.asStateFlow()

    private val _groupActionDialog = MutableStateFlow<GroupActionDialogState?>(null)
    val groupActionDialog: StateFlow<GroupActionDialogState?> = _groupActionDialog.asStateFlow()

    private val _groupDeletionNotice = MutableStateFlow<GroupDeletionNotice?>(null)
    val groupDeletionNotice: StateFlow<GroupDeletionNotice?> = _groupDeletionNotice.asStateFlow()

    private var muteExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            repository.groups.collectLatest { currentGroups ->
                val availableIds = currentGroups.mapTo(mutableSetOf()) { it.id }
                _selectedGroupIds.value = _selectedGroupIds.value.intersect(availableIds)

                muteExpiryJob?.cancel()
                val now = System.currentTimeMillis()
                val nextExpiry = currentGroups.mapNotNull { group ->
                    group.mutedUntilMillis?.takeIf { it != Long.MAX_VALUE && it > now }
                }.minOrNull()
                if (nextExpiry != null) {
                    muteExpiryJob = launch {
                        delay((nextExpiry - now).coerceAtLeast(1L))
                        repository.refreshLifecycleState()
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.pendingGroupDeletions.collectLatest { pending ->
                if (pending.isEmpty()) return@collectLatest
                val now = System.currentTimeMillis()
                val active = pending.filter { (it.deletionDeadlineMillis ?: 0L) > now }
                if (active.isNotEmpty() && _groupDeletionNotice.value == null) {
                    _groupDeletionNotice.value = GroupDeletionNotice(
                        groupIds = active.mapTo(linkedSetOf()) { it.id },
                        demoMode = isDemoMode.value,
                        message = deletionSuccessMessage(active.size)
                    )
                }
                val nextDeadline = pending.mapNotNull { it.deletionDeadlineMillis }.minOrNull()
                if (nextDeadline != null) {
                    delay((nextDeadline - System.currentTimeMillis()).coerceAtLeast(1L))
                    repository.finalizeExpiredGroupDeletions()
                }
            }
        }
    }

    val currentUserId: StateFlow<String> = combine(isDemoMode, users) { isDemo, uList ->
        if (isDemo) {
            DemoDataProvider.currentUser.id
        } else {
            uList.firstOrNull { it.isCurrentUser }?.id ?: "live_user_me"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoDataProvider.currentUser.id)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedGroupId.flatMapLatest { groupId ->
        if (groupId != null) repository.observeMessagesForGroup(groupId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SplitmateUiState> = combine(
        listOf(
            isDemoMode,
            currentUserId,
            selectedTab,
            users,
            friends,
            groups,
            expenses,
            settlements,
            personalExpenses,
            chatMessages,
            _searchQuery,
            _selectedCategoryFilter,
            _selectedGroupId,
            _selectedFriendId,
            _selectedExpenseId,
            _selectedPersonalExpenseId,
            _showAddExpenseDialog,
            _showAddPersonalExpenseDialog,
            _editingExpense,
            _editingPersonalExpense,
            _prefilledGroupId,
            _showCreateGroupDialog,
            _showAddFriendDialog,
            _showSettleUpDialog,
            _prefilledSettleReceiverId,
            _prefilledSettleGroupId,
            _snackbarMessage
        )
    ) { params ->
        val isDemo = params[0] as Boolean
        val uid = params[1] as String
        val tab = params[2] as NavigationTab
        val uList = params[3] as List<User>
        val fList = params[4] as List<Friend>
        val gList = params[5] as List<Group>
        val expList = params[6] as List<Expense>
        val stlList = params[7] as List<Settlement>
        val pexpList = params[8] as List<PersonalExpense>
        val msgList = params[9] as List<ChatMessage>

        val (net, owed, owe) = repository.calculateNetBalance(uid, expList, stlList)

        // Calculate current month personal total
        val monthTotal = BalanceCalculator.personalMonthlyTotal(pexpList)

        SplitmateUiState(
            isDemoMode = isDemo,
            currentMode = if (isDemo) "DEMO" else "LIVE",
            currentUserId = uid,
            selectedTab = tab,
            users = uList,
            friends = fList,
            groups = gList.filter { uid in it.memberIds },
            expenses = expList,
            settlements = stlList,
            personalExpenses = pexpList,
            chatMessages = msgList,
            personalExpenseTotalMonth = monthTotal,
            totalOwed = owed,
            totalOwe = owe,
            netBalance = net,
            searchQuery = params[10] as String,
            selectedCategoryFilter = params[11] as Category?,
            selectedGroupId = params[12] as String?,
            selectedFriendId = params[13] as String?,
            selectedExpenseId = params[14] as String?,
            selectedPersonalExpenseId = params[15] as String?,
            showAddExpenseDialog = params[16] as Boolean,
            showAddPersonalExpenseDialog = params[17] as Boolean,
            editingExpense = params[18] as Expense?,
            editingPersonalExpense = params[19] as PersonalExpense?,
            prefilledGroupId = params[20] as String?,
            showCreateGroupDialog = params[21] as Boolean,
            showAddFriendDialog = params[22] as Boolean,
            showSettleUpDialog = params[23] as Boolean,
            prefilledSettleReceiverId = params[24] as String?,
            prefilledSettleGroupId = params[25] as String?,
            snackbarMessage = params[26] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitmateUiState())

    // --- Navigation Controls ---

    fun selectTab(tab: NavigationTab) {
        android.util.Log.d("SplitmateVM", "selectTab called with: $tab")
        _selectedTab.value = tab
        _selectedGroupId.value = null
        _selectedFriendId.value = null
        clearGroupSelection()
    }

    fun openGroupDetail(groupId: String) {
        clearGroupSelection()
        _selectedTab.value = NavigationTab.GROUPS
        _selectedGroupId.value = groupId
        _selectedFriendId.value = null
    }

    fun closeGroupDetail() {
        _selectedGroupId.value = null
        _selectedTab.value = NavigationTab.GROUPS
    }

    fun openGroupChat() {
        if (_selectedGroupId.value != null) _selectedTab.value = NavigationTab.GROUP_CHAT
    }

    fun closeGroupChat() {
        _selectedTab.value = NavigationTab.GROUPS
    }

    fun openGroupBalanceDetails() {
        if (_selectedGroupId.value != null) _selectedTab.value = NavigationTab.GROUP_BALANCE_DETAILS
    }

    fun closeGroupBalanceDetails() {
        _selectedTab.value = NavigationTab.GROUPS
    }

    fun openFriendDetail(friendId: String) {
        _selectedFriendId.value = friendId
        _selectedGroupId.value = null
    }

    fun closeFriendDetail() {
        _selectedFriendId.value = null
    }

    fun openExpenseDetail(expenseId: String) {
        _selectedExpenseId.value = expenseId
    }

    fun closeExpenseDetail() {
        _selectedExpenseId.value = null
    }

    fun openPersonalExpenseDetail(expenseId: String) {
        _selectedPersonalExpenseId.value = expenseId
    }

    fun closePersonalExpenseDetail() {
        _selectedPersonalExpenseId.value = null
    }

    // --- Group selection and lifecycle actions ---

    fun startGroupSelection(groupId: String) {
        if (groups.value.any { it.id == groupId && currentUserId.value in it.memberIds }) {
            _selectedGroupIds.value = setOf(groupId)
        }
    }

    fun toggleGroupSelection(groupId: String) {
        val selected = _selectedGroupIds.value
        _selectedGroupIds.value = if (groupId in selected) selected - groupId else selected + groupId
    }

    fun clearGroupSelection() {
        _selectedGroupIds.value = emptySet()
    }

    fun requestGroupAction(type: GroupActionType) {
        val snapshot = _selectedGroupIds.value
        if (snapshot.isEmpty()) {
            _snackbarMessage.value = "Select at least one group"
            return
        }
        _groupActionDialog.value = GroupActionDialogState(type, snapshot)
    }

    fun dismissGroupAction() {
        _groupActionDialog.value = null
        clearGroupSelection()
    }

    fun confirmMute(duration: MuteDuration) {
        val action = _groupActionDialog.value?.takeIf { it.type == GroupActionType.MUTE } ?: return
        viewModelScope.launch {
            runCatching {
                val until = duration.mutedUntil(System.currentTimeMillis())
                repository.muteGroups(action.groupIds, until)
            }.onSuccess { result ->
                _snackbarMessage.value = actionResultMessage(
                    result.appliedGroups.size,
                    result.blockedReasons,
                    if (result.appliedGroups.size == 1) "group muted ${duration.confirmationLabel}" else "groups muted ${duration.confirmationLabel}"
                )
            }.onFailure { error ->
                _snackbarMessage.value = "Groups could not be muted: ${error.message ?: "database error"}"
            }
            _groupActionDialog.value = null
            clearGroupSelection()
        }
    }

    fun unmuteSelectedGroups() {
        val snapshot = _selectedGroupIds.value
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.unmuteGroups(snapshot) }
                .onSuccess { result ->
                    _snackbarMessage.value = actionResultMessage(
                        result.appliedGroups.size,
                        result.blockedReasons,
                        if (result.appliedGroups.size == 1) "group unmuted" else "groups unmuted"
                    )
                }
                .onFailure { error ->
                    _snackbarMessage.value = "Groups could not be unmuted: ${error.message ?: "database error"}"
                }
            clearGroupSelection()
        }
    }

    fun confirmLeaveGroups() {
        val action = _groupActionDialog.value?.takeIf { it.type == GroupActionType.LEAVE } ?: return
        viewModelScope.launch {
            runCatching { repository.leaveGroups(action.groupIds, currentUserId.value) }
                .onSuccess { result ->
                    _snackbarMessage.value = actionResultMessage(
                        result.appliedGroups.size,
                        result.blockedReasons,
                        if (result.appliedGroups.size == 1) "group left" else "groups left"
                    )
                }
                .onFailure { error ->
                    _snackbarMessage.value = "Groups could not be left: ${error.message ?: "database error"}"
                }
            _groupActionDialog.value = null
            clearGroupSelection()
        }
    }

    fun confirmDeleteGroups() {
        val action = _groupActionDialog.value?.takeIf { it.type == GroupActionType.DELETE } ?: return
        val now = System.currentTimeMillis()
        val modeAtDeletion = isDemoMode.value
        viewModelScope.launch {
            runCatching {
                repository.markGroupsPendingDeletion(
                    groupIds = action.groupIds,
                    currentUserId = currentUserId.value,
                    deletedAtMillis = now,
                    deadlineMillis = now + GROUP_DELETE_UNDO_WINDOW_MILLIS
                )
            }.onSuccess { result ->
                if (result.appliedGroups.isNotEmpty()) {
                    _groupDeletionNotice.value = GroupDeletionNotice(
                        groupIds = result.appliedGroups.mapTo(linkedSetOf()) { it.id },
                        demoMode = modeAtDeletion,
                        message = buildString {
                            append(deletionSuccessMessage(result.appliedGroups.size))
                            if (result.blockedReasons.isNotEmpty()) {
                                append(". ")
                                append(blockedMessage(result.blockedReasons))
                            }
                        }
                    )
                } else if (result.blockedReasons.isNotEmpty()) {
                    _snackbarMessage.value = blockedMessage(result.blockedReasons)
                }
            }.onFailure { error ->
                _snackbarMessage.value = "Groups could not be deleted: ${error.message ?: "database error"}"
            }
            _groupActionDialog.value = null
            clearGroupSelection()
        }
    }

    fun undoPendingGroupDeletion() {
        val notice = _groupDeletionNotice.value ?: return
        _groupDeletionNotice.value = null
        viewModelScope.launch {
            runCatching { repository.undoGroupDeletions(notice.groupIds, notice.demoMode) }
                .onSuccess { result ->
                    _snackbarMessage.value = if (result.appliedGroups.isNotEmpty()) {
                        if (result.appliedGroups.size == 1) "Group restored" else "${result.appliedGroups.size} groups restored"
                    } else blockedMessage(result.blockedReasons)
                }
                .onFailure { error ->
                    _snackbarMessage.value = "Undo failed: ${error.message ?: "the undo window expired"}"
                }
        }
    }

    fun consumeGroupDeletionNotice() {
        _groupDeletionNotice.value = null
    }

    private fun deletionSuccessMessage(count: Int): String =
        if (count == 1) "Group deleted successfully" else "$count groups deleted successfully"

    private fun actionResultMessage(
        appliedCount: Int,
        blocked: Map<String, String>,
        successSuffix: String
    ): String = when {
        appliedCount > 0 && blocked.isEmpty() -> "$appliedCount $successSuffix"
        appliedCount > 0 -> "$appliedCount $successSuffix. ${blockedMessage(blocked)}"
        else -> blockedMessage(blocked)
    }

    private fun blockedMessage(blocked: Map<String, String>): String =
        blocked.entries.joinToString("; ") { (name, reason) -> "$name: $reason" }
            .ifBlank { "No eligible groups were changed" }

    // --- Dialog Controls ---

    fun showAddExpense(groupId: String? = null, expense: Expense? = null) {
        _prefilledGroupId.value = groupId ?: expense?.groupId
        _editingExpense.value = expense
        _showAddExpenseDialog.value = true
    }

    fun openEditExpense(expense: Expense) {
        _selectedExpenseId.value = null
        showAddExpense(expense.groupId, expense)
    }

    fun hideAddExpense() {
        _showAddExpenseDialog.value = false
        _editingExpense.value = null
        _prefilledGroupId.value = null
    }

    fun showAddPersonalExpense(expense: PersonalExpense? = null) {
        _editingPersonalExpense.value = expense
        _showAddPersonalExpenseDialog.value = true
    }

    fun openEditPersonalExpense(expense: PersonalExpense) {
        _selectedPersonalExpenseId.value = null
        showAddPersonalExpense(expense)
    }

    fun hideAddPersonalExpense() {
        _showAddPersonalExpenseDialog.value = false
        _editingPersonalExpense.value = null
    }

    fun showCreateGroup() {
        _showCreateGroupDialog.value = true
    }

    fun hideCreateGroup() {
        _showCreateGroupDialog.value = false
    }

    fun showAddFriend() {
        _showAddFriendDialog.value = true
    }

    fun hideAddFriend() {
        _showAddFriendDialog.value = false
    }

    fun showSettleUp(prefilledReceiverId: String? = null, prefilledGroupId: String? = null) {
        _prefilledSettleReceiverId.value = prefilledReceiverId
        _prefilledSettleGroupId.value = prefilledGroupId
        _showSettleUpDialog.value = true
    }

    fun hideSettleUp() {
        _showSettleUpDialog.value = false
        _prefilledSettleReceiverId.value = null
        _prefilledSettleGroupId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: Category?) {
        _selectedCategoryFilter.value = category
    }

    fun switchMode(modeName: String) {
        val enableDemo = modeName.equals("DEMO", ignoreCase = true)
        viewModelScope.launch {
            repository.setDemoMode(enableDemo)
            _snackbarMessage.value = if (enableDemo) "Switched to Demo Mode" else "Switched to Live Mode"
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDemoData()
            _snackbarMessage.value = "Demo data has been reset to initial state"
        }
    }

    // --- CRUD Operations ---

    /**
     * Routes the global/Home Add Expense action without mixing personal spending into
     * shared balances. A selected group is shared; the default Personal choice is private.
     */
    fun addExpenseFromQuickAction(
        title: String,
        amount: Double,
        paidByUserId: String,
        groupId: String?,
        category: Category,
        participantIds: List<String>,
        splitType: SplitType,
        customSplits: Map<String, Double> = emptyMap(),
        notes: String = ""
    ) {
        when (quickExpenseDestination(groupId)) {
            QuickExpenseDestination.PERSONAL -> addPersonalExpense(title, amount, category, notes)
            QuickExpenseDestination.SHARED -> addExpense(
                    title = title,
                    amount = amount,
                    paidByUserId = paidByUserId,
                    groupId = requireNotNull(groupId),
                    category = category,
                    participantIds = participantIds,
                    splitType = splitType,
                    customSplits = customSplits,
                    notes = notes
                )
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        paidByUserId: String,
        groupId: String?,
        category: Category,
        participantIds: List<String>,
        splitType: SplitType,
        customSplits: Map<String, Double> = emptyMap(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addExpense(
                title = title,
                amount = amount,
                paidByUserId = paidByUserId,
                groupId = groupId,
                category = category,
                participantIds = participantIds,
                splitType = splitType,
                customSplits = customSplits,
                notes = notes
            )
            hideAddExpense()
            _snackbarMessage.value = "Expense \"$title\" added"
        }
    }

    fun updateExpense(
        id: String,
        title: String,
        amount: Double,
        paidByUserId: String,
        groupId: String?,
        category: Category,
        participantIds: List<String>,
        splitType: SplitType,
        customSplits: Map<String, Double> = emptyMap(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val existing = expenses.value.find { it.id == id }
            val updated = Expense(
                id = id,
                title = title,
                amount = amount,
                paidByUserId = paidByUserId,
                groupId = groupId,
                category = category,
                dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
                participantIds = participantIds,
                splitType = splitType,
                customSplits = customSplits,
                notes = notes
            )
            repository.updateExpense(updated)
            hideAddExpense()
            _snackbarMessage.value = "Expense \"$title\" updated"
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val exp = expenses.value.find { it.id == expenseId }
            repository.deleteExpense(expenseId)
            closeExpenseDetail()
            _snackbarMessage.value = "Expense \"${exp?.title ?: "Item"}\" deleted"
        }
    }

    fun addPersonalExpense(
        title: String,
        amount: Double,
        category: Category,
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addPersonalExpense(title, amount, category, notes, dateMillis)
            hideAddPersonalExpense()
            _snackbarMessage.value = "Personal expense \"$title\" added"
        }
    }

    fun updatePersonalExpense(
        id: String,
        title: String,
        amount: Double,
        category: Category,
        notes: String = "",
        dateMillis: Long? = null
    ) {
        viewModelScope.launch {
            val existing = personalExpenses.value.find { it.id == id }
            val updated = PersonalExpense(
                id = id,
                title = title,
                amount = amount,
                category = category,
                notes = notes,
                dateMillis = dateMillis ?: existing?.dateMillis ?: System.currentTimeMillis()
            )
            repository.updatePersonalExpense(updated)
            hideAddPersonalExpense()
            _snackbarMessage.value = "Personal expense \"$title\" updated"
        }
    }

    fun deletePersonalExpense(expenseId: String) {
        viewModelScope.launch {
            val exp = personalExpenses.value.find { it.id == expenseId }
            repository.deletePersonalExpense(expenseId)
            hideAddPersonalExpense()
            closePersonalExpenseDetail()
            _snackbarMessage.value = "Personal expense \"${exp?.title ?: "Item"}\" deleted"
        }
    }

    fun sendMessage(message: String) {
        val groupId = _selectedGroupId.value ?: return
        val senderId = currentUserId.value
        if (message.isBlank()) return
        
        viewModelScope.launch {
            repository.sendMessage(groupId, senderId, message)
        }
    }

    fun createGroup(
        name: String,
        description: String,
        icon: String,
        memberIds: List<String>
    ) {
        viewModelScope.launch {
            repository.createGroup(name, description, icon, memberIds)
            hideCreateGroup()
            _snackbarMessage.value = "Group \"$name\" created"
        }
    }

    fun setGroupBudget(groupId: String, budget: Double?) {
        viewModelScope.launch { repository.setGroupBudget(groupId, budget); _snackbarMessage.value = "Group budget updated" }
    }

    fun addMemberToGroup(groupId: String, memberId: String) {
        viewModelScope.launch { repository.addMemberToGroup(groupId, memberId); _snackbarMessage.value = "Member added" }
    }

    fun addFriend(name: String, email: String, phone: String = "") {
        viewModelScope.launch {
            repository.addFriend(name, email, phone)
            hideAddFriend()
            _snackbarMessage.value = "Friend \"$name\" added"
        }
    }

    fun recordSettlement(
        payerId: String,
        receiverId: String,
        amount: Double,
        groupId: String?,
        paymentMethodStr: String,
        notes: String
    ) {
        viewModelScope.launch {
            val method = when {
                paymentMethodStr.contains("Cash", ignoreCase = true) -> PaymentMethod.CASH
                paymentMethodStr.contains("PayPal", ignoreCase = true) -> PaymentMethod.PAYPAL
                paymentMethodStr.contains("Venmo", ignoreCase = true) -> PaymentMethod.VENMO
                else -> PaymentMethod.UPI
            }
            repository.recordSettlement(
                fromUserId = payerId,
                toUserId = receiverId,
                amount = amount,
                groupId = groupId,
                paymentMethod = method,
                note = notes
            )
            hideSettleUp()
            _snackbarMessage.value = "Payment of ${CurrencyUtils.formatINR(amount)} recorded"
        }
    }

    // --- Calculations ---

    fun getGroupUserBalance(groupId: String): Double {
        val uid = currentUserId.value
        val groupExpenses = expenses.value.filter { it.groupId == groupId }
        val groupSettlements = settlements.value.filter { it.groupId == groupId }
        return repository.calculateNetBalance(uid, groupExpenses, groupSettlements).first
    }

    fun getGroupBalanceSummary(groupId: String): Triple<Double, Double, Double> {
        val groupExpenses = expenses.value.filter { it.groupId == groupId }
        val groupSettlements = settlements.value.filter { it.groupId == groupId }
        return repository.calculateNetBalance(currentUserId.value, groupExpenses, groupSettlements)
    }

    fun getGroupTotalSpend(groupId: String): Double {
        return BalanceCalculator.groupTotalSpent(groupId, expenses.value)
    }

    fun getBalanceWithUser(otherUserId: String): Double {
        val uid = currentUserId.value
        return repository.calculateFriendBalance(uid, otherUserId, expenses.value, settlements.value)
    }

    fun simplifyGroupDebts(groupId: String): List<SimplifiedDebt> {
        return repository.simplifyDebts(
            groupId = groupId,
            expensesList = expenses.value,
            settlementsList = settlements.value,
            membersList = users.value
        )
    }

    fun getTimeGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun showUnavailableOnlineFeature() {
        _snackbarMessage.value = "This feature requires an online service and is still in development."
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
