package com.dsa.thebigtrip.data

import androidx.room.TypeConverter

class StringListConverter {

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toString(value: List<String>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.joinToString(SEPARATOR)
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}
