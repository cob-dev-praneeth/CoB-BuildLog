package org.childrenofbharat.buildlog.data

class BuildLogRepository(
    private val entryDao: EntryDao,
    private val projectDao: ProjectDao
) {
    val timeline = entryDao.observeTimeline()
    val projects = projectDao.observeAll()

    suspend fun captureNote(content: String, projectId: String?, tags: List<String>) {
        require(content.isNotBlank())
        entryDao.insert(
            EntryEntity(
                content = content.trim(),
                projectId = projectId,
                tags = tags.map { it.trim().removePrefix("#") }
                    .filter { it.isNotBlank() }.distinct().joinToString(",")
            )
        )
    }

    suspend fun addProject(name: String, description: String = "") {
        require(name.isNotBlank())
        projectDao.insert(ProjectEntity(name = name.trim(), description = description.trim()))
    }
}
