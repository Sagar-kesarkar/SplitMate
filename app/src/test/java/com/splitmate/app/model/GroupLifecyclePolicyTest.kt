package com.splitmate.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupLifecyclePolicyTest {
    private val now = 1_000_000L
    private val group = Group(
        id = "g1",
        name = "Test Group",
        description = "",
        icon = "T",
        memberIds = listOf("owner", "member"),
        createdAt = now,
        ownerUserId = "owner"
    )

    @Test
    fun muteDurationsExpireDeterministically() {
        val until = MuteDuration.EIGHT_HOURS.mutedUntil(now)
        assertEquals(now + 8L * 60L * 60L * 1_000L, until)
        assertTrue(GroupLifecyclePolicy.isMuted(group.copy(mutedUntilMillis = until), until - 1L))
        assertFalse(GroupLifecyclePolicy.isMuted(group.copy(mutedUntilMillis = until), until))
    }

    @Test
    fun indefiniteMuteRequiresExplicitUnmute() {
        val muted = group.copy(mutedUntilMillis = MuteDuration.INDEFINITE.mutedUntil(now))
        assertEquals(Long.MAX_VALUE, muted.mutedUntilMillis)
        assertTrue(GroupLifecyclePolicy.isMuted(muted, Long.MAX_VALUE - 1L))
        assertFalse(GroupLifecyclePolicy.isMuted(muted.copy(mutedUntilMillis = null), now))
    }

    @Test
    fun ownerCanDeleteButCannotLeaveAndMemberCanLeaveButCannotDelete() {
        assertFalse(GroupLifecyclePolicy.canLeave(group, "owner"))
        assertTrue(GroupLifecyclePolicy.canDelete(group, "owner"))
        assertTrue(GroupLifecyclePolicy.canLeave(group, "member"))
        assertFalse(GroupLifecyclePolicy.canDelete(group, "member"))
    }
}
