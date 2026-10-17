package com.example.prelimproject.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prelimproject.data.FlashIQRepository
import com.example.prelimproject.models.Flashcard
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FlashcardViewModel(private val repository: FlashIQRepository) : ViewModel() {

    /** Mirrors the flashcards table; kept in sync by the repository Flow. */
    val flashcards = mutableStateListOf<Flashcard>()

    var editingCardId = mutableStateOf("")

    init {
        viewModelScope.launch {
            repository.flashcards.collect { list ->
                flashcards.clear()
                flashcards.addAll(list)
            }
        }
    }

    fun addFlashcard(selectedSubjectId: String, inputQuestion: String, inputAnswer: String, assignedUserIds: List<String>, difficulty: String = "Medium", hint: String = "") {
        val newFlashcard = Flashcard(
            id = System.currentTimeMillis().toString(),
            subjectId = selectedSubjectId,
            question = inputQuestion,
            answer = inputAnswer,
            assignedUserIds = assignedUserIds,
            difficulty = difficulty,
            hint = hint
        )
        viewModelScope.launch { repository.insertFlashcard(newFlashcard) }
    }

    fun editFlashcard(flashcardId: String, newSubjectId: String, newQuestion: String, newAnswer: String, newAssignedUserIds: List<String>, difficulty: String = "Medium", hint: String = "") {
        val old = flashcards.firstOrNull { it.id == flashcardId }
        val createdAt = if (old != null) old.createdAt else System.currentTimeMillis()
        val updatedCard = Flashcard(
            id = flashcardId,
            subjectId = newSubjectId,
            question = newQuestion,
            answer = newAnswer,
            assignedUserIds = newAssignedUserIds,
            difficulty = difficulty,
            hint = hint,
            createdAt = createdAt
        )
        viewModelScope.launch { repository.updateFlashcard(updatedCard) }
    }

    fun deleteFlashcard(flashcardId: String) {
        if (editingCardId.value == flashcardId) {
            editingCardId.value = ""
        }
        viewModelScope.launch { repository.deleteFlashcard(flashcardId) }
    }

    fun toggleAssignment(flashcardId: String, studentUserId: String) {
        viewModelScope.launch { repository.toggleAssignment(flashcardId, studentUserId) }
    }

    /** Keeps the flashcards table consistent when a user account is deleted. */
    fun removeUserFromAssignments(userId: String) {
        viewModelScope.launch { repository.removeUserFromAssignments(userId) }
    }

    fun clearEditing() {
        editingCardId.value = ""
    }
}
