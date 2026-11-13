package com.example.prelimproject.models

import com.google.firebase.firestore.DocumentSnapshot

object UserFactory {

    fun create(
        username: String,
        role: String = Roles.STUDENT,
        displayName: String = username,
        email: String = "",
        id: String = newId(),
        createdAt: Long = System.currentTimeMillis()
    ): User = User(
        id = id,
        username = username,
        role = role,
        displayName = displayName,
        email = email,
        createdAt = createdAt
    )

    fun createStudent(
        username: String,
        email: String,
        id: String = newId(),
        createdAt: Long = System.currentTimeMillis()
    ): User = create(
        username = username,
        displayName = username,
        email = email,
        id = id,
        createdAt = createdAt
    )

    fun fromDocument(document: DocumentSnapshot): User? {
        if (!document.exists()) return null
        return User(
            id = document.id,
            username = document.getString(Keys.USERNAME) ?: return null,
            role = document.getString(Keys.ROLE) ?: Roles.STUDENT,
            displayName = document.getString(Keys.DISPLAY_NAME) ?: "",
            email = document.getString(Keys.EMAIL) ?: "",
            createdAt = document.getLong(Keys.CREATED_AT) ?: 0L
        )
    }

    fun toDocument(user: User): Map<String, Any?> = mapOf(
        Keys.USERNAME to user.username,
        Keys.ROLE to user.role,
        Keys.DISPLAY_NAME to user.displayName,
        Keys.EMAIL to user.email,
        Keys.CREATED_AT to user.createdAt
    )
}
