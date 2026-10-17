package com.example.prelimproject.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prelimproject.data.FlashIQRepository
import com.example.prelimproject.models.Subject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SubjectViewModel(private val repository: FlashIQRepository) : ViewModel() {

    /** Mirrors the subjects table; kept in sync by the repository Flow. */
    val subjects = mutableStateListOf<Subject>()

    init {
        viewModelScope.launch {
            repository.subjects.collect { list ->
                subjects.clear()
                subjects.addAll(list)
            }
        }
    }

    fun getSubjectTitle(subjectId: String): String {
        for (subject in subjects) {
            if (subject.id == subjectId) {
                return subject.title
            }
        }
        return "Subject"
    }

    fun addSubject(title: String, description: String = ""): Subject {
        val newSubject = Subject(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description
        )
        viewModelScope.launch { repository.insertSubject(newSubject) }
        return newSubject
    }

    fun removeSubject(subjectId: String) {
        viewModelScope.launch { repository.deleteSubjectWithCards(subjectId) }
    }

    fun renameSubject(subjectId: String, newTitle: String) {
        val target = subjects.firstOrNull { it.id == subjectId } ?: return
        viewModelScope.launch { repository.updateSubject(target.copy(title = newTitle)) }
    }
}
