package org.childrenofbharat.buildlog.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromEntryType(value: EntryType): String = value.name
    @TypeConverter fun toEntryType(value: String): EntryType = EntryType.valueOf(value)
}
