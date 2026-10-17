package com.example.prelimproject.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val color: String = "#007AFF",
    val createdAt: Long = System.currentTimeMillis()
)
