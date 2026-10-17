package com.example.prelimproject.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val password: String,
    val role: String,
    val displayName: String = username,
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
