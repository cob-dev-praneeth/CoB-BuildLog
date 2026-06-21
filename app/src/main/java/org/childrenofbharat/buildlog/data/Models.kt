package org.childrenofbharat.buildlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class EntryType { NOTE, TASK, VOICE, SCREENSHOT, CLIPBOARD, BOOKMARK }

@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("projectId"), Index("createdAt")]
)
data class EntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val type: EntryType = EntryType.NOTE,
    val content: String,
    val projectId: String? = null,
    val tags: String = "",
    val sourceApplication: String? = null,
    val metadataJson: String = "{}"
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val colorHex: String = "#E9FF70",
    val createdAt: Long = System.currentTimeMillis()
)

data class TimelineItem(
    val id: String,
    val createdAt: Long,
    val type: EntryType,
    val content: String,
    val tags: String,
    val projectName: String?,
    val projectColor: String?
)
