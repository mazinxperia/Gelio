package io.gelio.app.data.local.db

import androidx.room.TypeConverter

class ShowcaseTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = "|||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf { it.isNotBlank() }
            ?.split("|||")
            ?.filter { it.isNotBlank() }
            .orEmpty()
}
