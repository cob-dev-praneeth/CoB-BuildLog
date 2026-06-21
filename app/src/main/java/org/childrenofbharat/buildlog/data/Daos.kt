package org.childrenofbharat.buildlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity)

    @Query("""
        SELECT e.id, e.createdAt, e.type, e.content, e.tags,
               p.name AS projectName, p.colorHex AS projectColor
        FROM entries e LEFT JOIN projects p ON e.projectId = p.id
        ORDER BY e.createdAt DESC
    """)
    fun observeTimeline(): Flow<List<TimelineItem>>

    @Query("SELECT COUNT(*) FROM entries WHERE createdAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProjectEntity>>
}
