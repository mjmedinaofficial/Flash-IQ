package com.example.prelimproject.models

import com.google.firebase.firestore.DocumentSnapshot

object FlashcardFactory {

    fun create(
        subjectId: String,
        question: String,
        answer: String,
        assignedUserIds: List<String> = emptyList(),
        hint: String = "",
        id: String = newId(),
        createdAt: Long = System.currentTimeMillis()
    ): Flashcard = Flashcard(
        id = id,
        subjectId = subjectId,
        question = question,
        answer = answer,
        assignedUserIds = assignedUserIds,
        hint = hint,
        createdAt = createdAt
    )

    fun fromDocument(document: DocumentSnapshot): Flashcard? {
        if (!document.exists()) return null
        return Flashcard(
            id = document.id,
            subjectId = document.getString(Keys.SUBJECT_ID) ?: return null,
            question = document.getString(Keys.QUESTION) ?: "",
            answer = document.getString(Keys.ANSWER) ?: "",
            assignedUserIds = (document.get(Keys.ASSIGNED_USER_IDS) as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList(),
            hint = document.getString(Keys.HINT) ?: "",
            createdAt = document.getLong(Keys.CREATED_AT) ?: 0L
        )
    }

    fun toDocument(card: Flashcard): Map<String, Any?> = mapOf(
        Keys.SUBJECT_ID to card.subjectId,
        Keys.QUESTION to card.question,
        Keys.ANSWER to card.answer,
        Keys.ASSIGNED_USER_IDS to card.assignedUserIds,
        Keys.HINT to card.hint,
        Keys.CREATED_AT to card.createdAt
    )
}
