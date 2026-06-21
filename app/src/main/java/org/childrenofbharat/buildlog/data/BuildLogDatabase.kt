package org.childrenofbharat.buildlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EntryEntity::class, ProjectEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BuildLogDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun projectDao(): ProjectDao

    companion object {
        fun create(context: Context): BuildLogDatabase = Room.databaseBuilder(
            context.applicationContext,
            BuildLogDatabase::class.java,
            "cob-build-log.db"
        ).build()
    }
}
