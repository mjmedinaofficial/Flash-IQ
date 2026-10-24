package com.example.prelimproject.models

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FlashcardRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection(COLLECTION)

    fun observeAll(): Flow<List<Flashcard>> = callbackFlow {
        val registration = collection.orderBy(Keys.CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { FlashcardFactory.fromDocument(it) })
            }
        awaitClose { registration.remove() }
    }

    suspend fun save(card: Flashcard) {
        collection.document(card.id).set(FlashcardFactory.toDocument(card)).await()
    }

    suspend fun delete(cardId: String) {
        collection.document(cardId).delete().await()
    }

    suspend fun unassign(cardId: String, userId: String) {
        collection.document(cardId)
            .update(Keys.ASSIGNED_USER_IDS, FieldValue.arrayRemove(userId))
            .await()
    }

    /** Used when a student account is deleted: strips them from every card. */
    suspend fun unassignEverywhere(userId: String) {
        val assigned = collection.whereArrayContains(Keys.ASSIGNED_USER_IDS, userId).get().await()
        if (assigned.isEmpty) return
        val batch = db.batch()
        for (document in assigned.documents) {
            batch.update(
                collection.document(document.id),
                Keys.ASSIGNED_USER_IDS,
                FieldValue.arrayRemove(userId)
            )
        }
        batch.commit().await()
    }

    /** Used when a subject is deleted: its cards go with it. */
    suspend fun deleteAllForSubject(subjectId: String) {
        val cards = collection.whereEqualTo(Keys.SUBJECT_ID, subjectId).get().await()
        if (cards.isEmpty) return
        val batch = db.batch()
        for (document in cards.documents) {
            batch.delete(collection.document(document.id))
        }
        batch.commit().await()
    }

    private companion object {
        const val COLLECTION = "flashcards"
    }
}
