package com.example.prelimproject.data

import androidx.room.TypeConverter

/**
 * Room type converters.
 *
 * [com.example.prelimproject.models.Flashcard.assignedUserIds] is a List<String>,
 * which SQLite cannot store directly, so it is flattened into a single
 * delimited string column.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        // Unit Separator (0x1F) - never appears inside generated ids or usernames.
        const val SEPARATOR = "\u001F"
    }
}
