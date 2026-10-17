package com.example.prelimproject.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.prelimproject.models.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<Flashcard>

    @Query("SELECT * FROM flashcards WHERE id = :flashcardId LIMIT 1")
    suspend fun getById(flashcardId: String): Flashcard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flashcard: Flashcard)

    @Update
    suspend fun update(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE id = :flashcardId")
    suspend fun deleteById(flashcardId: String)

    @Query("DELETE FROM flashcards WHERE subjectId = :subjectId")
    suspend fun deleteBySubject(subjectId: String)
}
