package com.example.prelimproject.models

import com.google.firebase.firestore.DocumentSnapshot

object SubjectFactory {

    fun create(
        title: String,
        description: String,
        color: String = "#007AFF",
        id: String = newId(),
        createdAt: Long = System.currentTimeMillis()
    ): Subject = Subject(
        id = id,
        title = title,
        description = description,
        color = color,
        createdAt = createdAt
    )

    fun fromDocument(document: DocumentSnapshot): Subject? {
        if (!document.exists()) return null
        return Subject(
            id = document.id,
            title = document.getString(Keys.TITLE) ?: return null,
            description = document.getString(Keys.DESCRIPTION) ?: "",
            color = document.getString(Keys.COLOR) ?: "#007AFF",
            createdAt = document.getLong(Keys.CREATED_AT) ?: 0L
        )
    }

    fun toDocument(subject: Subject): Map<String, Any?> = mapOf(
        Keys.TITLE to subject.title,
        Keys.DESCRIPTION to subject.description,
        Keys.COLOR to subject.color,
        Keys.CREATED_AT to subject.createdAt
    )
}
