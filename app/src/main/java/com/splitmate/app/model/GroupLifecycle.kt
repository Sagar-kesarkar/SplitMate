package com.splitmate.app.model

const val INDEFINITE_MUTE_MILLIS: Long = Long.MAX_VALUE
const val GROUP_DELETE_UNDO_WINDOW_MILLIS: Long = 5_000L

enum class MuteDuration(val displayName: String, val durationMillis: Long?) {
    ONE_HOUR("1 hour", 60L * 60L * 1_000L),
    EIGHT_HOURS("8 hours", 8L * 60L * 60L * 1_000L),
    ONE_WEEK("1 week", 7L * 24L * 60L * 60L * 1_000L),
    INDEFINITE("Until I turn it back on", null);

    fun mutedUntil(nowMillis: Long): Long = durationMillis?.let(nowMillis::plus) ?: INDEFINITE_MUTE_MILLIS

    val confirmationLabel: String
        get() = if (this == INDEFINITE) "until you turn them back on" else "for $displayName"
}

object GroupLifecyclePolicy {
    fun ownerId(group: Group): String? = group.ownerUserId ?: group.memberIds.firstOrNull()

    fun isMuted(group: Group, nowMillis: Long): Boolean {
        val until = group.mutedUntilMillis ?: return false
        return until == INDEFINITE_MUTE_MILLIS || until > nowMillis
    }

    fun canLeave(group: Group, currentUserId: String): Boolean =
        currentUserId in group.memberIds && ownerId(group) != currentUserId

    fun canDelete(group: Group, currentUserId: String): Boolean =
        ownerId(group) == currentUserId

    fun isPendingDeletion(group: Group): Boolean = group.pendingDeletionAtMillis != null
}

data class GroupActionResult(
    val appliedGroups: List<Group>,
    val blockedReasons: Map<String, String>
)
