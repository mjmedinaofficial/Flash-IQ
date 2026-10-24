package com.example.prelimproject.models

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubjectRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val flashcards: FlashcardRepository = FlashcardRepository()
) {
    private val collection = db.collection(COLLECTION)

    fun observeAll(): Flow<List<Subject>> = callbackFlow {
        val registration = collection.orderBy(Keys.CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { SubjectFactory.fromDocument(it) })
            }
        awaitClose { registration.remove() }
    }

    suspend fun add(title: String, description: String): Subject {
        val subject = SubjectFactory.create(title = title, description = description)
        collection.document(subject.id).set(SubjectFactory.toDocument(subject)).await()
        return subject
    }

    suspend fun rename(id: String, title: String) {
        collection.document(id).update(Keys.TITLE, title).await()
    }

    suspend fun delete(id: String) {
        flashcards.deleteAllForSubject(id)
        collection.document(id).delete().await()
    }

    private companion object {
        const val COLLECTION = "subjects"
    }
}
