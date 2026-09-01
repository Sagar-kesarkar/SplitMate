package com.splitmate.app.util

import android.content.Context
import android.provider.ContactsContract
import com.splitmate.app.model.Friend

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>
)

object ContactsUtils {
    /**
     * Normalizes a phone number string into a clean digit string (preserving leading +).
     * Example: "+1 (555) 234-5678" -> "+15552345678"
     * Example: "09876 54321" -> "0987654321"
     */
    fun normalizePhoneNumber(phone: String): String {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return ""
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Queries device contacts with phone numbers using ContactsContract.
     */
    fun queryDeviceContacts(context: Context): List<DeviceContact> {
        val contactsMap = mutableMapOf<String, Pair<String, MutableList<String>>>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                    val normalized = normalizePhoneNumber(rawNumber)

                    if (normalized.isNotEmpty()) {
                        val entry = contactsMap.getOrPut(id) { name to mutableListOf() }
                        if (!entry.second.contains(normalized)) {
                            entry.second.add(normalized)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return contactsMap.map { (id, pair) ->
            DeviceContact(id = id, name = pair.first, phoneNumbers = pair.second)
        }
    }

    /**
     * Filters out device contacts whose phone numbers match existing friends.
     */
    fun filterDuplicates(
        contacts: List<DeviceContact>,
        existingFriends: List<Friend>
    ): List<DeviceContact> {
        val existingNormalizedPhones = existingFriends.map { normalizePhoneNumber(it.phone) }.filter { it.isNotEmpty() }.toSet()

        return contacts.mapNotNull { contact ->
            val nonDuplicatePhones = contact.phoneNumbers.filter { !existingNormalizedPhones.contains(normalizePhoneNumber(it)) }
            if (nonDuplicatePhones.isNotEmpty()) {
                contact.copy(phoneNumbers = nonDuplicatePhones)
            } else null
        }
    }
}
