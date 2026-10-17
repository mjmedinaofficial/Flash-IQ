package com.example.prelimproject

import android.app.Application
import com.example.prelimproject.data.AppDatabase
import com.example.prelimproject.data.FlashIQRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application-scoped holder for the Room database and the repository,
 * following the same pattern as the lecture's EmployeeApplication.
 *
 * The database and repository are created lazily and shared by every
 * ViewModel factory.
 */
class FlashIQApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: FlashIQRepository by lazy {
        FlashIQRepository(
            userDao = database.userDao(),
            subjectDao = database.subjectDao(),
            flashcardDao = database.flashcardDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Seed the database on first launch (no-op once it already has data).
        applicationScope.launch {
            if (repository.isDatabaseEmpty()) {
                repository.seed()
            }
        }
    }
}
