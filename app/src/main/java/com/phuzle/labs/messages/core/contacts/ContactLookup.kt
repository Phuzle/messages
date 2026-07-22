package com.phuzle.labs.messages.core.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactMatch(val name: String, val number: String, val photoUri: String? = null)

/** Resolves a phone number to a saved contact's display name/photo, when READ_CONTACTS is granted. */
class ContactLookup(private val context: Context) {

    /** Autocomplete for Compose's "To" field, via the same filter URI the system dialer uses. */
    fun searchContacts(query: String, limit: Int = 5): List<ContactMatch> {
        if (query.isBlank()) return emptyList()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            android.net.Uri.encode(query),
        )
        val results = mutableListOf<ContactMatch>()
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ),
            null, null, null,
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            if (nameIdx < 0 || numberIdx < 0) return@use
            while (cursor.moveToNext() && results.size < limit) {
                val name = cursor.getString(nameIdx) ?: continue
                val number = cursor.getString(numberIdx) ?: continue
                val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                results += ContactMatch(name, number, photoUri)
            }
        }
        return results.distinctBy { it.number }
    }

    fun displayNameFor(number: String): String? = lookupPhone(number)?.name

    /** Contact avatar for [ThreadRow]/thread headers — null when the sender isn't a saved contact. */
    fun photoUriFor(number: String): String? = lookupPhone(number)?.photoUri

    private fun lookupPhone(number: String): ContactMatch? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number),
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                if (nameIndex < 0) return null
                val name = cursor.getString(nameIndex) ?: return null
                val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null
                return ContactMatch(name, number, photoUri)
            }
        }
        return null
    }

    fun isKnownContact(number: String): Boolean = displayNameFor(number) != null
}
