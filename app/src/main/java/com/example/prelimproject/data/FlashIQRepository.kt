package com.example.prelimproject.data

import com.example.prelimproject.models.Flashcard
import com.example.prelimproject.models.Subject
import com.example.prelimproject.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to the Room DAOs for the whole app.
 *
 * The ViewModels only ever talk to this repository, never to the DAOs or the
 * database directly. Read paths expose cold [Flow]s so the UI stays in sync
 * with the database automatically.
 */
class FlashIQRepository(
    private val userDao: UserDao,
    private val subjectDao: SubjectDao,
    private val flashcardDao: FlashcardDao
) {

    val users: Flow<List<User>> = userDao.getAll()
    val subjects: Flow<List<Subject>> = subjectDao.getAll()
    val flashcards: Flow<List<Flashcard>> = flashcardDao.getAll()

    // ----------------------------------------------------------------- users

    suspend fun insertUser(user: User) = userDao.insert(user)

    suspend fun updateUser(user: User) = userDao.update(user)

    /** Deletes an account and unassigns any flashcards that referenced it. */
    suspend fun deleteUser(userId: String) {
        userDao.deleteById(userId)
        removeUserFromAssignments(userId)
    }

    // -------------------------------------------------------------- subjects

    suspend fun insertSubject(subject: Subject) = subjectDao.insert(subject)

    suspend fun updateSubject(subject: Subject) = subjectDao.update(subject)

    /** Deletes a subject together with every flashcard that belongs to it. */
    suspend fun deleteSubjectWithCards(subjectId: String) {
        flashcardDao.deleteBySubject(subjectId)
        subjectDao.deleteById(subjectId)
    }

    // ------------------------------------------------------------ flashcards

    suspend fun insertFlashcard(flashcard: Flashcard) = flashcardDao.insert(flashcard)

    suspend fun updateFlashcard(flashcard: Flashcard) = flashcardDao.update(flashcard)

    suspend fun deleteFlashcard(flashcardId: String) = flashcardDao.deleteById(flashcardId)

    suspend fun toggleAssignment(flashcardId: String, userId: String) {
        val card = flashcardDao.getById(flashcardId) ?: return
        val updated = if (card.assignedUserIds.contains(userId)) {
            card.copy(assignedUserIds = card.assignedUserIds.filter { it != userId })
        } else {
            card.copy(assignedUserIds = card.assignedUserIds + userId)
        }
        flashcardDao.update(updated)
    }

    suspend fun removeUserFromAssignments(userId: String) {
        val cards = flashcardDao.getAllOnce()
        for (card in cards) {
            if (card.assignedUserIds.contains(userId)) {
                flashcardDao.update(card.copy(assignedUserIds = card.assignedUserIds.filter { it != userId }))
            }
        }
    }

    // ------------------------------------------------------------------ seed

    suspend fun isDatabaseEmpty(): Boolean = userDao.count() == 0

    /** Writes the initial Admin account, subjects and flashcards. Called once. */
    suspend fun seed() {
        val now = System.currentTimeMillis()

        userDao.insert(
            User(
                id = "1",
                username = "Admin",
                password = "123",
                role = "ADMIN",
                displayName = "Administrator",
                email = "admin@flashiq.app",
                createdAt = now
            )
        )

        val subjects = listOf(
            Subject("1", "Project Management", "Agile methodologies, sprint cycles, and project planning.", "#007AFF", now),
            Subject("2", "Application Development", "Software architecture, SOLID principles, and clean code.", "#34C759", now),
            Subject("3", "Mobile Development", "Android development, Jetpack Compose, and Kotlin fundamentals.", "#FF9500", now),
            Subject("4", "English Proficiency", "Grammar, professional writing, and technical communication.", "#FF3B30", now)
        )
        for (subject in subjects) {
            subjectDao.insert(subject)
        }

        val flashcards = listOf(
            Flashcard("1", "1", "What is the main difference between Agile and Waterfall?", "Agile is iterative and flexible, while Waterfall is sequential and rigid.", emptyList(), createdAt = now),
            Flashcard("2", "1", "What happens in a Sprint Retrospective?", "The team reviews what went well, what can be improved, and creates action steps.", emptyList(), createdAt = now),
            Flashcard("3", "1", "What is the Critical Path in a project?", "The longest sequence of dependent tasks determining the shortest total completion time.", emptyList(), createdAt = now),
            Flashcard("4", "2", "What does the Single Responsibility Principle mean?", "A class should have only one reason to change, handling one specific job.", emptyList(), createdAt = now),
            Flashcard("5", "2", "Why do we use Dependency Injection?", "It separates object creation from usage to make code easier to test and maintain.", emptyList(), createdAt = now),
            Flashcard("6", "2", "What is the difference between coupling and cohesion?", "Cohesion is how focused a module is. Coupling is how dependent modules are on each other.", emptyList(), createdAt = now),
            Flashcard("7", "3", "What is Recomposition in Jetpack Compose?", "Re-running composable functions when their state data changes to update the UI.", emptyList(), createdAt = now),
            Flashcard("8", "3", "What is the purpose of rememberSaveable?", "It saves UI state across configuration changes like screen rotations.", emptyList(), createdAt = now),
            Flashcard("9", "3", "What is State Hoisting?", "Moving state up to a parent composable to make child components reusable and stateless.", emptyList(), createdAt = now),
            Flashcard("10", "4", "What is the difference between active and passive voice?", "In active voice, the subject performs the action. In passive, the subject receives the action.", emptyList(), createdAt = now),
            Flashcard("11", "4", "What does conciseness mean in writing?", "Expressing thoughts clearly with as few words as necessary without losing meaning.", emptyList(), createdAt = now),
            Flashcard("12", "4", "Give an example of a transition word.", "Furthermore, However, Consequently, Moreover.", emptyList(), createdAt = now)
        )
        for (card in flashcards) {
            flashcardDao.insert(card)
        }
    }
}
