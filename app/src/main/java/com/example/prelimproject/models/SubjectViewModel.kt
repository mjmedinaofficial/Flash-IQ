package com.example.prelimproject.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SubjectViewModel(
    private val repository: SubjectRepository = SubjectRepository()
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun add(title: String, description: String): Result<Unit> = runCatching {
        repository.add(title = title, description = description)
    }.map { Unit }

    suspend fun rename(id: String, title: String): Result<Unit> = runCatching {
        repository.rename(id, title)
    }
    suspend fun remove(id: String): Result<Unit> = runCatching {
        repository.delete(id)
    }
}
