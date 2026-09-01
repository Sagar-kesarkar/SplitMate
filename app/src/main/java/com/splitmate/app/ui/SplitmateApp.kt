package com.splitmate.app.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.splitmate.app.ui.components.SplitmateHeader
import com.splitmate.app.ui.components.GroupSelectionBar
import com.splitmate.app.ui.dialogs.*
import com.splitmate.app.ui.screens.*
import com.splitmate.app.ui.theme.*
import com.splitmate.app.viewmodel.NavigationTab
import com.splitmate.app.viewmodel.GroupActionType
import com.splitmate.app.viewmodel.SplitmateViewModel
import com.splitmate.app.model.GroupLifecyclePolicy

@Composable
fun SplitmateApp(
    viewModel: SplitmateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceHistory by viewModel.balanceHistory.collectAsStateWithLifecycle()
    val selectedGroupIds by viewModel.selectedGroupIds.collectAsStateWithLifecycle()
    val groupActionDialog by viewModel.groupActionDialog.collectAsStateWithLifecycle()
    val groupDeletionNotice by viewModel.groupDeletionNotice.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Propagate snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(groupDeletionNotice) {
        groupDeletionNotice?.let { notice ->
            val result = snackbarHostState.showSnackbar(
                message = notice.message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoPendingGroupDeletion()
            else viewModel.consumeGroupDeletionNotice()
        }
    }

    val currentUserName = uiState.users.find { it.id == uiState.currentUserId }?.name?.split(" ")?.firstOrNull() ?: "there"

    val isRootHome = (uiState.selectedGroupId == null &&
            uiState.selectedFriendId == null &&
            uiState.selectedExpenseId == null &&
            uiState.selectedPersonalExpenseId == null &&
            !uiState.showAddExpenseDialog &&
            !uiState.showAddPersonalExpenseDialog &&
            !uiState.showCreateGroupDialog &&
            !uiState.showAddFriendDialog &&
            !uiState.showSettleUpDialog &&
            uiState.selectedTab == NavigationTab.HOME)

    val isGroupSelectionMode = uiState.selectedTab == NavigationTab.GROUPS &&
            uiState.selectedGroupId == null && selectedGroupIds.isNotEmpty()

    BackHandler(enabled = !isRootHome || isGroupSelectionMode || groupActionDialog != null) {
        when {
            groupActionDialog != null -> viewModel.dismissGroupAction()
            isGroupSelectionMode -> viewModel.clearGroupSelection()
            uiState.selectedPersonalExpenseId != null -> viewModel.closePersonalExpenseDetail()
            uiState.selectedExpenseId != null -> viewModel.closeExpenseDetail()
            uiState.showAddExpenseDialog -> viewModel.hideAddExpense()
            uiState.showAddPersonalExpenseDialog -> viewModel.hideAddPersonalExpense()
            uiState.showCreateGroupDialog -> viewModel.hideCreateGroup()
            uiState.showAddFriendDialog -> viewModel.hideAddFriend()
            uiState.showSettleUpDialog -> viewModel.hideSettleUp()
            uiState.selectedTab == NavigationTab.GROUP_BALANCE_DETAILS -> viewModel.closeGroupBalanceDetails()
            uiState.selectedTab == NavigationTab.GROUP_CHAT -> viewModel.closeGroupChat()
            uiState.selectedGroupId != null -> viewModel.closeGroupDetail()
            uiState.selectedFriendId != null -> viewModel.closeFriendDetail()
            uiState.selectedTab == NavigationTab.PERSONAL_EXPENSES -> viewModel.selectTab(NavigationTab.HOME)
            uiState.selectedTab != NavigationTab.HOME -> viewModel.selectTab(NavigationTab.HOME)
        }
    }

    Scaffold(
        topBar = {
            if (isGroupSelectionMode) {
                val selectedGroups = uiState.groups.filter { it.id in selectedGroupIds }
                val allMuted = selectedGroups.isNotEmpty() && selectedGroups.all {
                    GroupLifecyclePolicy.isMuted(it, System.currentTimeMillis())
                }
                GroupSelectionBar(
                    selectedCount = selectedGroupIds.size,
                    showUnmute = allMuted,
                    onClose = viewModel::clearGroupSelection,
                    onMute = {
                        if (allMuted) viewModel.unmuteSelectedGroups()
                        else viewModel.requestGroupAction(GroupActionType.MUTE)
                    },
                    onLeave = { viewModel.requestGroupAction(GroupActionType.LEAVE) },
                    onDelete = { viewModel.requestGroupAction(GroupActionType.DELETE) }
                )
            } else if (uiState.selectedGroupId == null && uiState.selectedFriendId == null && uiState.selectedTab != NavigationTab.PERSONAL_EXPENSES && uiState.selectedTab != NavigationTab.GROUP_CHAT && uiState.selectedTab != NavigationTab.GROUP_BALANCE_DETAILS) {
                SplitmateHeader(
                    greeting = "${viewModel.getTimeGreeting()}, $currentUserName",
                    isDemoMode = uiState.isDemoMode,
                    onSwitchMode = { viewModel.switchMode(if (it) "DEMO" else "LIVE") },
                    onResetDemoData = { viewModel.resetDemoData() }
                )
            }
        },
        bottomBar = {
            if (!uiState.showAddExpenseDialog && uiState.selectedGroupId == null && uiState.selectedFriendId == null && uiState.selectedTab != NavigationTab.PERSONAL_EXPENSES && uiState.selectedTab != NavigationTab.GROUP_CHAT && uiState.selectedTab != NavigationTab.GROUP_BALANCE_DETAILS) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        Triple(NavigationTab.HOME, Icons.Filled.Home to Icons.Outlined.Home, "Home"),
                        Triple(NavigationTab.EXPENSES, Icons.Filled.Receipt to Icons.Outlined.Receipt, "Expenses"),
                        Triple(NavigationTab.GROUPS, Icons.Filled.Groups to Icons.Outlined.Groups, "Groups"),
                        Triple(NavigationTab.FRIENDS, Icons.Filled.People to Icons.Outlined.People, "Friends"),
                        Triple(NavigationTab.ANALYTICS, Icons.Filled.Insights to Icons.Outlined.Insights, "Insights")
                    )

                    tabs.forEach { (tab, iconPair, label) ->
                        val isSelected = uiState.selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) iconPair.first else iconPair.second,
                                    contentDescription = label,
                                    tint = if (isSelected) SplitEmeraldLight else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SplitEmeraldLight else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SplitEmerald.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedGroupId == null && uiState.selectedFriendId == null && 
                uiState.selectedTab != NavigationTab.ANALYTICS && 
                uiState.selectedTab != NavigationTab.PERSONAL_EXPENSES && 
                uiState.selectedTab != NavigationTab.GROUP_CHAT &&
                uiState.selectedTab != NavigationTab.GROUP_BALANCE_DETAILS &&
                !uiState.showAddExpenseDialog &&
                !isGroupSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showAddExpense() },
                    containerColor = SplitEmerald,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("main_add_expense_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Nested Detail Navigation or Primary Tab Screen
            when {
                uiState.selectedGroupId != null && uiState.selectedTab == NavigationTab.GROUP_BALANCE_DETAILS -> {
                    val group = uiState.groups.find { it.id == uiState.selectedGroupId }
                    if (group != null) GroupBalanceDetailsScreen(
                        group = group,
                        users = uiState.users,
                        currentUserId = uiState.currentUserId,
                        history = balanceHistory.filter { it.groupId == group.id },
                        onBackClick = { viewModel.closeGroupBalanceDetails() }
                    )
                }
                uiState.selectedGroupId != null && uiState.selectedTab != NavigationTab.GROUP_CHAT && uiState.selectedTab != NavigationTab.PERSONAL_EXPENSES -> {
                    val group = uiState.groups.find { it.id == uiState.selectedGroupId }
                    if (group != null) {
                        val groupBalance = viewModel.getGroupUserBalance(group.id)
                        val (_, groupOwed, groupOwe) = viewModel.getGroupBalanceSummary(group.id)
                        val totalSpend = viewModel.getGroupTotalSpend(group.id)
                        val simplifiedDebts = viewModel.simplifyGroupDebts(group.id)

                        GroupDetailScreen(
                            group = group,
                            users = uiState.users,
                            expenses = uiState.expenses,
                            currentUserId = uiState.currentUserId,
                            userBalance = groupBalance,
                            totalSpend = totalSpend,
                            youAreOwed = groupOwed,
                            youOwe = groupOwe,
                            simplifiedDebts = simplifiedDebts,
                            onBackClick = { viewModel.closeGroupDetail() },
                            onChatClick = { viewModel.openGroupChat() },
                            onAddExpenseClick = { viewModel.showAddExpense(group.id) },
                            onSettleUpClick = { grpId, recId -> viewModel.showSettleUp(prefilledReceiverId = recId, prefilledGroupId = grpId) },
                            onExpenseClick = { viewModel.openExpenseDetail(it) },
                            onSetBudget = { viewModel.setGroupBudget(group.id, it) },
                            onAddMember = { viewModel.addMemberToGroup(group.id, it) },
                            onBalanceDetailsClick = { viewModel.openGroupBalanceDetails() },
                            onUnavailableFeature = { viewModel.showUnavailableOnlineFeature() }
                        )
                    }
                }

                uiState.selectedFriendId != null -> {
                    val friend = uiState.users.find { it.id == uiState.selectedFriendId }
                    if (friend != null) {
                        val balance = viewModel.getBalanceWithUser(friend.id)

                        FriendDetailScreen(
                            friend = friend,
                            users = uiState.users,
                            groups = uiState.groups,
                            expenses = uiState.expenses,
                            currentUserId = uiState.currentUserId,
                            balance = balance,
                            onBackClick = { viewModel.closeFriendDetail() },
                            onSettleUpClick = { viewModel.showSettleUp(prefilledReceiverId = friend.id) },
                            onExpenseClick = { viewModel.openExpenseDetail(it) },
                            onSendReminder = { viewModel.showUnavailableOnlineFeature() }
                        )
                    }
                }

                else -> {
                    // Primary Tabs
                    when (uiState.selectedTab) {
                        NavigationTab.HOME -> {
                            HomeScreen(
                                totalOwed = uiState.totalOwed,
                                totalOwe = uiState.totalOwe,
                                netBalance = uiState.netBalance,
                                groups = uiState.groups,
                                expenses = uiState.expenses,
                                settlements = uiState.settlements,
                                users = uiState.users,
                                currentUserId = uiState.currentUserId,
                                onAddExpenseClick = { viewModel.showAddExpense() },
                                onSettleUpClick = { viewModel.showSettleUp() },
                                onCreateGroupClick = { viewModel.showCreateGroup() },
                                onAddFriendClick = { viewModel.showAddFriend() },
                                onGroupClick = { viewModel.openGroupDetail(it) },
                                onExpenseClick = { viewModel.openExpenseDetail(it) },
                                onViewAllExpensesClick = { viewModel.selectTab(NavigationTab.EXPENSES) },
                                onViewAllGroupsClick = { viewModel.selectTab(NavigationTab.GROUPS) },
                                onPersonalExpensesClick = { viewModel.selectTab(NavigationTab.PERSONAL_EXPENSES) },
                                personalExpenseTotalMonth = uiState.personalExpenseTotalMonth,
                                calculateGroupBalance = { viewModel.getGroupUserBalance(it) },
                                calculateGroupTotalSpend = { viewModel.getGroupTotalSpend(it) }
                            )
                        }

                        NavigationTab.EXPENSES -> {
                            ExpensesScreen(
                                expenses = uiState.expenses,
                                users = uiState.users,
                                groups = uiState.groups,
                                currentUserId = uiState.currentUserId,
                                searchQuery = uiState.searchQuery,
                                selectedCategory = uiState.selectedCategoryFilter,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                                onExpenseClick = { viewModel.openExpenseDetail(it) },
                                onAddExpenseClick = { viewModel.showAddExpense() }
                            )
                        }

                        NavigationTab.GROUPS -> {
                            GroupsScreen(
                                groups = uiState.groups,
                                users = uiState.users,
                                onCreateGroupClick = { viewModel.showCreateGroup() },
                                onGroupClick = { viewModel.openGroupDetail(it) },
                                selectedGroupIds = selectedGroupIds,
                                onGroupLongPress = { viewModel.startGroupSelection(it) },
                                onToggleGroupSelection = { viewModel.toggleGroupSelection(it) },
                                calculateGroupBalance = { viewModel.getGroupUserBalance(it) },
                                calculateGroupTotalSpend = { viewModel.getGroupTotalSpend(it) }
                            )
                        }

                        NavigationTab.FRIENDS -> {
                            FriendsScreen(
                            users = uiState.users,
                            currentUserId = uiState.currentUserId,
                            onAddFriendClick = { viewModel.showAddFriend() },
                            onFriendClick = { viewModel.openFriendDetail(it) },
                            onSettleClick = { viewModel.showSettleUp(prefilledReceiverId = it) },
                            calculateBalanceWithUser = { viewModel.getBalanceWithUser(it) }
                        )
                        }

                        NavigationTab.ANALYTICS -> {
                            AnalyticsScreen(
                                expenses = uiState.expenses,
                                users = uiState.users,
                                currentUserId = uiState.currentUserId
                            )
                        }

                        NavigationTab.PERSONAL_EXPENSES -> {
                            PersonalExpensesScreen(
                                expenses = uiState.personalExpenses,
                                sharedExpenses = uiState.expenses,
                                users = uiState.users,
                                groups = uiState.groups,
                                currentUserId = uiState.currentUserId,
                                totalMonth = uiState.personalExpenseTotalMonth,
                                onBackClick = { viewModel.selectTab(NavigationTab.HOME) },
                                onAddExpenseClick = { viewModel.showAddPersonalExpense() },
                                onExpenseClick = { viewModel.openEditPersonalExpense(it) },
                                onSharedExpenseClick = { viewModel.openExpenseDetail(it) }
                            )
                        }

                        NavigationTab.GROUP_CHAT -> {
                            val group = uiState.groups.find { it.id == uiState.selectedGroupId }
                            if (group != null) {
                                GroupChatScreen(
                                    groupName = group.name,
                                    messages = uiState.chatMessages,
                                    users = uiState.users,
                                    currentUserId = uiState.currentUserId,
                                    onBackClick = { viewModel.closeGroupChat() },
                                    onSendMessage = { viewModel.sendMessage(it) }
                                )
                            }
                        }

                        NavigationTab.GROUP_BALANCE_DETAILS -> Unit
                    }
                }
            }
        }
    }

    // --- Active Dialogs ---

    groupActionDialog?.let { action ->
        val selectedGroups = uiState.groups.filter { it.id in action.groupIds }
        if (selectedGroups.isNotEmpty()) {
            GroupActionDialog(
                type = action.type,
                selectedGroups = selectedGroups,
                currentUserId = uiState.currentUserId,
                onDismiss = viewModel::dismissGroupAction,
                onMute = viewModel::confirmMute,
                onLeave = viewModel::confirmLeaveGroups,
                onDelete = viewModel::confirmDeleteGroups,
                onOfferLeave = { viewModel.requestGroupAction(GroupActionType.LEAVE) }
            )
        }
    }

    if (uiState.showAddExpenseDialog) {
        AddExpenseDialog(
            users = uiState.users,
            groups = uiState.groups,
            currentUserId = uiState.currentUserId,
            prefilledGroupId = uiState.prefilledGroupId,
            existingExpense = uiState.editingExpense,
            onDismiss = { viewModel.hideAddExpense() },
            onSaveExpense = { id, title, amount, paidBy, groupId, category, participants, splitType, splits, notes ->
                if (id != null) {
                    viewModel.updateExpense(
                        id = id,
                        title = title,
                        amount = amount,
                        paidByUserId = paidBy,
                        groupId = groupId,
                        category = category,
                        participantIds = participants,
                        splitType = splitType,
                        customSplits = splits,
                        notes = notes
                    )
                } else {
                    viewModel.addExpenseFromQuickAction(
                        title = title,
                        amount = amount,
                        paidByUserId = paidBy,
                        groupId = groupId,
                        category = category,
                        participantIds = participants,
                        splitType = splitType,
                        customSplits = splits,
                        notes = notes
                    )
                }
            }
        )
    }

    if (uiState.showSettleUpDialog) {
        SettleUpDialog(
            users = uiState.users,
            groups = uiState.groups,
            currentUserId = uiState.currentUserId,
            prefilledReceiverId = uiState.prefilledSettleReceiverId,
            prefilledGroupId = uiState.prefilledSettleGroupId,
            onDismiss = { viewModel.hideSettleUp() },
            onRecordSettlement = { payerId, receiverId, amount, groupId, method, notes ->
                viewModel.recordSettlement(
                    payerId = payerId,
                    receiverId = receiverId,
                    amount = amount,
                    groupId = groupId,
                    paymentMethodStr = method,
                    notes = notes
                )
            }
        )
    }

    if (uiState.showCreateGroupDialog) {
        CreateGroupDialog(
            users = uiState.users,
            currentUserId = uiState.currentUserId,
            onDismiss = { viewModel.hideCreateGroup() },
            onCreateGroup = { name, description, emoji, members ->
                viewModel.createGroup(name, description, emoji, members)
            }
        )
    }

    if (uiState.showAddFriendDialog) {
        AddFriendDialog(
            isDemoMode = uiState.isDemoMode,
            existingFriends = uiState.friends,
            onDismiss = { viewModel.hideAddFriend() },
            onAddFriend = { name, email, phone ->
                viewModel.addFriend(name, email, phone)
            }
        )
    }

    if (uiState.showAddPersonalExpenseDialog) {
        AddPersonalExpenseDialog(
            existingExpense = uiState.editingPersonalExpense,
            onDismiss = { viewModel.hideAddPersonalExpense() },
            onSaveExpense = { id, title, amount, category, notes, dateMillis ->
                if (id != null) {
                    viewModel.updatePersonalExpense(id, title, amount, category, notes, dateMillis)
                } else {
                    viewModel.addPersonalExpense(title, amount, category, notes, dateMillis)
                }
            },
            onDeleteExpense = { viewModel.deletePersonalExpense(it) }
        )
    }

    if (uiState.selectedExpenseId != null) {
        val expense = uiState.expenses.find { it.id == uiState.selectedExpenseId }
        if (expense != null) {
            val payer = uiState.users.find { it.id == expense.paidByUserId }
            val group = uiState.groups.find { it.id == expense.groupId }

            ExpenseDetailSheet(
                expense = expense,
                payer = payer,
                group = group,
                users = uiState.users,
                currentUserId = uiState.currentUserId,
                onDismiss = { viewModel.closeExpenseDetail() },
                onEditExpense = { exp -> viewModel.openEditExpense(exp) },
                onDeleteExpense = { id -> viewModel.deleteExpense(id) }
            )
        }
    }
}
