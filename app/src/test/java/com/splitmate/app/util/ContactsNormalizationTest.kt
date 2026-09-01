package com.splitmate.app.util

import com.splitmate.app.model.Friend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsNormalizationTest {

    @Test
    fun testPhoneNumberNormalization() {
        assertEquals("+15552345678", ContactsUtils.normalizePhoneNumber("+1 (555) 234-5678"))
        assertEquals("5553456789", ContactsUtils.normalizePhoneNumber(" (555) 345-6789 "))
        assertEquals("+919876543210", ContactsUtils.normalizePhoneNumber("+91 98765-43210"))
        assertEquals("", ContactsUtils.normalizePhoneNumber("  "))
    }

    @Test
    fun testFilterDuplicatesAgainstExistingFriends() {
        val existingFriends = listOf(
            Friend(id = "f1", name = "Alex", email = "alex@example.com", avatar = "", phone = "+1 (555) 234-5678"),
            Friend(id = "f2", name = "Sarah", email = "sarah@example.com", avatar = "", phone = "5553456789")
        )

        val deviceContacts = listOf(
            DeviceContact(id = "c1", name = "Alex", phoneNumbers = listOf("+15552345678")),
            DeviceContact(id = "c2", name = "New Friend Bob", phoneNumbers = listOf("+1 (555) 999-8888")),
            DeviceContact(id = "c3", name = "Multi Number Contact", phoneNumbers = listOf("5553456789", "+1 (555) 777-6666"))
        )

        val filtered = ContactsUtils.filterDuplicates(deviceContacts, existingFriends)

        assertEquals(2, filtered.size)
        // c1 (Alex) is completely filtered out
        assertTrue(filtered.none { it.id == "c1" })
        // c2 is retained
        assertTrue(filtered.any { it.id == "c2" })
        // c3 has its duplicate number removed, retaining only the non-duplicate number
        val c3 = filtered.find { it.id == "c3" }
        assertEquals(1, c3?.phoneNumbers?.size)
        assertEquals("+1 (555) 777-6666", c3?.phoneNumbers?.first())
    }
}
