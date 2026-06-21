package org.childrenofbharat.buildlog

import android.app.Application
import org.childrenofbharat.buildlog.data.BuildLogDatabase
import org.childrenofbharat.buildlog.data.BuildLogRepository

class BuildLogApplication : Application() {
    val database by lazy { BuildLogDatabase.create(this) }
    val repository by lazy { BuildLogRepository(database.entryDao(), database.projectDao()) }
}
