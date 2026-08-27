package com.ochag.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract


data class PhoneContact(
    val contactId: Long,
    val name: String,
    val phone: String,
    val normalizedPhone: String
)


object PhoneContactsRepository {

    fun normalizePhone(
        value: String
    ): String {

        val digits =
            value.filter {
                it.isDigit()
            }

        if (
            digits.isBlank()
        ) {
            return ""
        }

        return when {

            digits.length == 11 &&
                    digits.startsWith("8") ->
                "7" +
                        digits.drop(1)

            digits.length == 10 ->
                "7$digits"

            else ->
                digits
        }
    }


    fun loadContacts(
        context: Context
    ): List<PhoneContact> {

        val result =
            mutableListOf<PhoneContact>()

        val projection =
            arrayOf(
                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .CONTACT_ID,

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .DISPLAY_NAME,

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .NUMBER
            )

        val cursor =
            context
                .contentResolver
                .query(
                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .DISPLAY_NAME +
                            " COLLATE NOCASE ASC"
                )
                ?: return emptyList()

        cursor.use {

            val idIndex =
                cursor.getColumnIndex(
                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .CONTACT_ID
                )

            val nameIndex =
                cursor.getColumnIndex(
                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .DISPLAY_NAME
                )

            val phoneIndex =
                cursor.getColumnIndex(
                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .NUMBER
                )

            while (
                cursor.moveToNext()
            ) {

                val id =
                    if (
                        idIndex >= 0
                    ) {
                        cursor.getLong(
                            idIndex
                        )
                    } else {
                        0L
                    }

                val name =
                    if (
                        nameIndex >= 0
                    ) {
                        cursor
                            .getString(
                                nameIndex
                            )
                            .orEmpty()
                            .trim()
                    } else {
                        ""
                    }

                val phone =
                    if (
                        phoneIndex >= 0
                    ) {
                        cursor
                            .getString(
                                phoneIndex
                            )
                            .orEmpty()
                            .trim()
                    } else {
                        ""
                    }

                val normalized =
                    normalizePhone(
                        phone
                    )

                if (
                    name.isBlank() ||
                    phone.isBlank() ||
                    normalized.isBlank()
                ) {
                    continue
                }

                if (
                    result.none {
                        it.normalizedPhone ==
                                normalized
                    }
                ) {

                    result.add(
                        PhoneContact(
                            contactId =
                                id,
                            name =
                                name,
                            phone =
                                phone,
                            normalizedPhone =
                                normalized
                        )
                    )
                }
            }
        }

        return result
    }


    fun inviteBySms(
        context: Context,
        contact: PhoneContact
    ) {

        val uri =
            Uri.parse(
                "smsto:${contact.phone}"
            )

        val intent =
            Intent(
                Intent.ACTION_SENDTO,
                uri
            )
                .putExtra(
                    "sms_body",
                    "Присоединяйся к нашему семейному мессенджеру «Очаг»."
                )
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

        try {

            context.startActivity(
                intent
            )

        } catch (_: Exception) {

            val fallback =
                Intent(
                    Intent.ACTION_SEND
                )
                    .setType(
                        "text/plain"
                    )
                    .putExtra(
                        Intent.EXTRA_TEXT,
                        "Присоединяйся к нашему семейному мессенджеру «Очаг»."
                    )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

            context.startActivity(
                fallback
            )
        }
    }
}
