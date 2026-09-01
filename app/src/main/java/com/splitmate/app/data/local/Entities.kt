package com.splitmate.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.splitmate.app.model.Category
import com.splitmate.app.model.ChatMessage
import com.splitmate.app.model.Expense
import com.splitmate.app.model.Friend
import com.splitmate.app.model.Group
import com.splitmate.app.model.PaymentMethod
import com.splitmate.app.model.PersonalExpense
import com.splitmate.app.model.Settlement
import com.splitmate.app.model.SplitType
import com.splitmate.app.model.User
import com.splitmate.app.model.BalanceEventType
import com.splitmate.app.model.BalanceHistoryEvent

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val isCurrentUser: Boolean = false
) {
    fun toDomainModel(): User = User(
        id = id,
        name = name,
        email = email,
        avatar = avatar,
        isCurrentUser = isCurrentUser
    )

    companion object {
        fun fromDomainModel(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            avatar = user.avatar,
            isCurrentUser = user.isCurrentUser
        )
    }
}

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val phone: String = ""
) {
    fun toDomainModel(): Friend = Friend(
        id = id,
        name = name,
        email = email,
        avatar = avatar,
        phone = phone
    )

    fun toUser(): User = User(
        id = id,
        name = name,
        email = email,
        avatar = avatar,
        isCurrentUser = false
    )

    companion object {
        fun fromDomainModel(friend: Friend): FriendEntity = FriendEntity(
            id = friend.id,
            name = friend.name,
            email = friend.email,
            avatar = friend.avatar,
            phone = friend.phone
        )
    }
}

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomainModel(): Group = Group(
        id = id,
        name = name,
        description = description,
        icon = icon,
        memberIds = memberIds,
        createdAt = createdAt,
        budget = budget,
        ownerUserId = ownerUserId ?: memberIds.firstOrNull(),
        mutedUntilMillis = mutedUntilMillis,
        pendingDeletionAtMillis = pendingDeletionAtMillis,
        deletionDeadlineMillis = deletionDeadlineMillis
    )

    companion object {
        fun fromDomainModel(group: Group): GroupEntity = GroupEntity(
            id = group.id,
            name = group.name,
            description = group.description,
            icon = group.icon,
            memberIds = group.memberIds,
            createdAt = group.createdAt,
            budget = group.budget,
            ownerUserId = group.ownerUserId ?: group.memberIds.firstOrNull(),
            mutedUntilMillis = group.mutedUntilMillis,
            pendingDeletionAtMillis = group.pendingDeletionAtMillis,
            deletionDeadlineMillis = group.deletionDeadlineMillis
        )
    }
}

@Entity(tableName = "balance_history")
data class BalanceHistoryEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val sourceId: String,
    val title: String,
    val eventTypeName: String,
    val paidByUserId: String,
    val otherUserId: String,
    val fullAmount: Double,
    val currentUserShare: Double,
    val signedChange: Double,
    val dateMillis: Long
) {
    fun toDomainModel() = BalanceHistoryEvent(id, groupId, sourceId, title,
        runCatching { BalanceEventType.valueOf(eventTypeName) }.getOrDefault(BalanceEventType.EXPENSE),
        paidByUserId, otherUserId, fullAmount, currentUserShare, signedChange, dateMillis)
    companion object {
        fun fromDomainModel(event: BalanceHistoryEvent) = BalanceHistoryEntity(
            event.id, event.groupId, event.sourceId, event.title, event.eventType.name,
            event.paidByUserId, event.otherUserId, event.fullAmount, event.currentUserShare,
            event.signedChange, event.dateMillis)
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val paidByUserId: String,
    val groupId: String? = null,
    val categoryName: String = "GENERAL",
    val dateMillis: Long = System.currentTimeMillis(),
    val participantIds: List<String> = emptyList(),
    val splitTypeName: String = "EQUAL",
    val customSplits: Map<String, Double> = emptyMap(),
    val notes: String = ""
) {
    fun toDomainModel(): Expense {
        val category = try {
            Category.valueOf(categoryName)
        } catch (e: Exception) {
            Category.GENERAL
        }
        val splitType = try {
            SplitType.valueOf(splitTypeName)
        } catch (e: Exception) {
            SplitType.EQUAL
        }
        return Expense(
            id = id,
            title = title,
            amount = amount,
            paidByUserId = paidByUserId,
            groupId = groupId,
            category = category,
            dateMillis = dateMillis,
            participantIds = participantIds,
            splitType = splitType,
            customSplits = customSplits,
            notes = notes
        )
    }

    companion object {
        fun fromDomainModel(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            paidByUserId = expense.paidByUserId,
            groupId = expense.groupId,
            categoryName = expense.category.name,
            dateMillis = expense.dateMillis,
            participantIds = expense.participantIds,
            splitTypeName = expense.splitType.name,
            customSplits = expense.customSplits,
            notes = expense.notes
        )
    }
}

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val groupId: String? = null,
    val paymentMethodName: String = "UPI",
    val note: String = ""
) {
    fun toDomainModel(): Settlement {
        val method = try {
            PaymentMethod.valueOf(paymentMethodName)
        } catch (e: Exception) {
            PaymentMethod.UPI
        }
        return Settlement(
            id = id,
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            dateMillis = dateMillis,
            groupId = groupId,
            paymentMethod = method,
            note = note
        )
    }

    companion object {
        fun fromDomainModel(settlement: Settlement): SettlementEntity = SettlementEntity(
            id = settlement.id,
            fromUserId = settlement.fromUserId,
            toUserId = settlement.toUserId,
            amount = settlement.amount,
            dateMillis = settlement.dateMillis,
            groupId = settlement.groupId,
            paymentMethodName = settlement.paymentMethod.name,
            note = settlement.note
        )
    }
}

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "personal_expenses")
data class PersonalExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val categoryName: String = "GENERAL",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): PersonalExpense {
        val category = try {
            Category.valueOf(categoryName)
        } catch (e: Exception) {
            Category.GENERAL
        }
        return PersonalExpense(
            id = id,
            title = title,
            amount = amount,
            category = category,
            notes = notes,
            dateMillis = dateMillis
        )
    }

    companion object {
        fun fromDomainModel(expense: PersonalExpense): PersonalExpenseEntity = PersonalExpenseEntity(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            categoryName = expense.category.name,
            notes = expense.notes,
            dateMillis = expense.dateMillis
        )
    }
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val senderId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): ChatMessage = ChatMessage(
        id = id,
        groupId = groupId,
        senderId = senderId,
        message = message,
        timestamp = timestamp
    )

    companion object {
        fun fromDomainModel(chat: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = chat.id,
            groupId = chat.groupId,
            senderId = chat.senderId,
            message = chat.message,
            timestamp = chat.timestamp
        )
    }
}
