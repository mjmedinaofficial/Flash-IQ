package com.example.prelimproject.models

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val flashcards: FlashcardRepository = FlashcardRepository()
) {
    private val collection = db.collection(COLLECTION)

    fun observeAll(): Flow<List<User>> = callbackFlow {
        val registration = collection.orderBy(Keys.CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { UserFactory.fromDocument(it) })
            }
        awaitClose { registration.remove() }
    }

    suspend fun findByUsername(username: String): User? {
        val documents = collection.whereEqualTo(Keys.USERNAME, username).limit(1).get().await()
        val document = documents.documents.firstOrNull() ?: return null
        return UserFactory.fromDocument(document)
    }

    suspend fun signIn(username: String, password: String): User? {
        val user = findByUsername(username) ?: return null
        return if (user.password == password) user else null
    }

    suspend fun usernameTaken(username: String): Boolean = findByUsername(username) != null

    suspend fun create(user: User) {
        collection.document(user.id).set(UserFactory.toDocument(user)).await()
    }

    suspend fun createStudent(username: String, password: String): User {
        val student = UserFactory.createStudent(username = username, password = password)
        create(student)
        return student
    }

    suspend fun rename(id: String, username: String) {
        collection.document(id)
            .update(mapOf(Keys.USERNAME to username, Keys.DISPLAY_NAME to username))
            .await()
    }

    suspend fun delete(id: String) {
        collection.document(id).delete().await()
        flashcards.unassignEverywhere(id)
    }

    suspend fun seedDemoDataIfNeeded() {
        if (!collection.limit(1).get().await().isEmpty) return

        val now = System.currentTimeMillis()
        val batch = db.batch()

        val admin = UserFactory.create(
            username = "Admin",
            password = "123",
            role = Roles.ADMIN,
            displayName = "Administrator",
            email = "admin@flashiq.app",
            id = "1",
            createdAt = now
        )
        batch.set(collection.document(admin.id), UserFactory.toDocument(admin))

        val subjects = listOf(
            SubjectFactory.create("Project Management", "Agile methodologies, sprint cycles, and project planning.", "#007AFF", "1", now),
            SubjectFactory.create("Application Development", "Software architecture, SOLID principles, and clean code.", "#34C759", "2", now),
            SubjectFactory.create("Mobile Development", "Android development, Jetpack Compose, and Kotlin fundamentals.", "#FF9500", "3", now),
            SubjectFactory.create("English Proficiency", "Grammar, professional writing, and technical communication.", "#FF3B30", "4", now)
        )
        for (subject in subjects) {
            batch.set(db.collection("subjects").document(subject.id), SubjectFactory.toDocument(subject))
        }

        val cards = listOf(
            FlashcardFactory.create("1", "What is the main difference between Agile and Waterfall?", "Agile is iterative and flexible, while Waterfall is sequential and rigid.", id = "1", createdAt = now),
            FlashcardFactory.create("1", "What happens in a Sprint Retrospective?", "The team reviews what went well, what can be improved, and creates action steps.", id = "2", createdAt = now),
            FlashcardFactory.create("1", "What is the Critical Path in a project?", "The longest sequence of dependent tasks determining the shortest total completion time.", id = "3", createdAt = now),
            FlashcardFactory.create("2", "What does the Single Responsibility Principle mean?", "A class should have only one reason to change, handling one specific job.", id = "4", createdAt = now),
            FlashcardFactory.create("2", "Why do we use Dependency Injection?", "It separates object creation from usage to make code easier to test and maintain.", id = "5", createdAt = now),
            FlashcardFactory.create("2", "What is the difference between coupling and cohesion?", "Cohesion is how focused a module is. Coupling is how dependent modules are on each other.", id = "6", createdAt = now),
            FlashcardFactory.create("3", "What is Recomposition in Jetpack Compose?", "Re-running composable functions when their state data changes to update the UI.", id = "7", createdAt = now),
            FlashcardFactory.create("3", "What is the purpose of rememberSaveable?", "It saves UI state across configuration changes like screen rotations.", id = "8", createdAt = now),
            FlashcardFactory.create("3", "What is State Hoisting?", "Moving state up to a parent composable to make child components reusable and stateless.", id = "9", createdAt = now),
            FlashcardFactory.create("4", "What is the difference between active and passive voice?", "In active voice, the subject performs the action. In passive, the subject receives the action.", id = "10", createdAt = now),
            FlashcardFactory.create("4", "What does conciseness mean in writing?", "Expressing thoughts clearly with as few words as necessary without losing meaning.", id = "11", createdAt = now),
            FlashcardFactory.create("4", "Give an example of a transition word.", "Furthermore, However, Consequently, Moreover.", id = "12", createdAt = now)
        )
        for (card in cards) {
            batch.set(db.collection("flashcards").document(card.id), FlashcardFactory.toDocument(card))
        }

        batch.commit().await()
    }

    private companion object {
        const val COLLECTION = "users"
    }
}
