package org.childrenofbharat.buildlog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.childrenofbharat.buildlog.data.BuildLogRepository

class BuildLogViewModel(private val repository: BuildLogRepository) : ViewModel() {
    val timeline = repository.timeline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun captureNote(content: String, projectId: String?, tags: List<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.captureNote(content, projectId, tags)
            onSaved()
        }
    }

    fun addProject(name: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.addProject(name)
            onSaved()
        }
    }

    class Factory(private val repository: BuildLogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BuildLogViewModel(repository) as T
    }
}
