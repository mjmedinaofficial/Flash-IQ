package com.example.prelimproject.models

import com.google.firebase.firestore.DocumentSnapshot

object UserFactory {

    fun create(
        username: String,
        password: String,
        role: String = Roles.STUDENT,
        displayName: String = username,
        email: String = "",
        id: String = newId(),
        createdAt: Long = System.currentTimeMillis()
    ): User = User(
        id = id,
        username = username,
        password = password,
        role = role,
        displayName = displayName,
        email = email,
        createdAt = createdAt
    )

    fun createStudent(username: String, password: String): User =
        create(username = username, password = password)

    fun fromDocument(document: DocumentSnapshot): User? {
        if (!document.exists()) return null
        return User(
            id = document.id,
            username = document.getString(Keys.USERNAME) ?: return null,
            password = document.getString(Keys.PASSWORD) ?: "",
            role = document.getString(Keys.ROLE) ?: Roles.STUDENT,
            displayName = document.getString(Keys.DISPLAY_NAME) ?: "",
            email = document.getString(Keys.EMAIL) ?: "",
            createdAt = document.getLong(Keys.CREATED_AT) ?: 0L
        )
    }

    fun toDocument(user: User): Map<String, Any?> = mapOf(
        Keys.USERNAME to user.username,
        Keys.PASSWORD to user.password,
        Keys.ROLE to user.role,
        Keys.DISPLAY_NAME to user.displayName,
        Keys.EMAIL to user.email,
        Keys.CREATED_AT to user.createdAt
    )
}
