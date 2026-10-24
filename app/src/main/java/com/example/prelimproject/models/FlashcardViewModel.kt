package com.example.prelimproject.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FlashcardViewModel(
    private val repository: FlashcardRepository = FlashcardRepository()
) : ViewModel() {

    val flashcards: StateFlow<List<Flashcard>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun save(card: Flashcard): Result<Unit> = runCatching {
        repository.save(card)
    }

    suspend fun delete(cardId: String): Result<Unit> = runCatching {
        repository.delete(cardId)
    }

    suspend fun unassign(cardId: String, userId: String): Result<Unit> = runCatching {
        repository.unassign(cardId, userId)
    }
}
