package com.example.prelimproject.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey val id: String,
    val subjectId: String,
    val question: String,
    val answer: String,
    val assignedUserIds: List<String> = emptyList(),
    val difficulty: String = "Medium",
    val hint: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
