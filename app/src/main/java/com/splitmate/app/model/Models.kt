package com.splitmate.app.model

import androidx.compose.ui.graphics.Color

enum class Category(val label: String, val icon: String, val color: Color) {
    FOOD("Food & Dining", "🍽️", Color(0xFFF59E0B)),
    GROCERIES("Groceries", "🛒", Color(0xFF10B981)),
    TRAVEL("Travel & Transport", "✈️", Color(0xFF3B82F6)),
    ENTERTAINMENT("Entertainment", "🍿", Color(0xFF8B5CF6)),
    SHOPPING("Shopping", "🛍️", Color(0xFFEC4899)),
    RENT("Rent & Stay", "🏠", Color(0xFF6366F1)),
    UTILITIES("Utilities & Bills", "⚡", Color(0xFFEAB308)),
    GENERAL("General", "💳", Color(0xFF64748B))
}

enum class SplitType(val label: String) {
    EQUAL("Equal"),
    EXACT("Exact ($)"),
    PERCENT("Percent (%)"),
    SHARES("Shares")
}

enum class PaymentMethod(val label: String, val icon: String) {
    UPI("UPI / Bank Transfer", "🏦"),
    CASH("Cash", "💵"),
    PAYPAL("PayPal", "🅿️"),
    VENMO("Venmo", "✌️")
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val isCurrentUser: Boolean = false
)

data class Friend(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val phone: String = ""
)

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val memberIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val budget: Double? = null,
    val ownerUserId: String? = null,
    val mutedUntilMillis: Long? = null,
    val pendingDeletionAtMillis: Long? = null,
    val deletionDeadlineMillis: Long? = null
)

enum class BalanceEventType {
    EXPENSE,
    EDIT_REVERSAL,
    EDIT_APPLIED,
    DELETION_ADJUSTMENT,
    SETTLEMENT,
    SETTLEMENT_REVERSAL
}

data class BalanceHistoryEvent(
    val id: String,
    val groupId: String,
    val sourceId: String,
    val title: String,
    val eventType: BalanceEventType,
    val paidByUserId: String,
    val otherUserId: String,
    val fullAmount: Double,
    val currentUserShare: Double,
    val signedChange: Double,
    val dateMillis: Long = System.currentTimeMillis()
)

data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val paidByUserId: String,
    val groupId: String? = null,
    val category: Category = Category.GENERAL,
    val dateMillis: Long = System.currentTimeMillis(),
    val participantIds: List<String> = emptyList(),
    val splitType: SplitType = SplitType.EQUAL,
    val customSplits: Map<String, Double> = emptyMap(),
    val notes: String = ""
)

data class Settlement(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val groupId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val note: String = ""
) {
    val payerId: String get() = fromUserId
    val receiverId: String get() = toUserId
}

data class SimplifiedDebt(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)

typealias DebtTransfer = SimplifiedDebt

data class CategorySpending(
    val category: Category,
    val totalAmount: Double,
    val percentage: Double
)

data class FriendBalance(
    val friend: User,
    val netBalance: Double
)

data class PersonalExpense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: Category = Category.GENERAL,
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val groupId: String,
    val senderId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
