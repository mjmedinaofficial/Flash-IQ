package com.example.prelimproject.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    var currentUser: User? by mutableStateOf(null)
        private set

    /** True until Firebase has restored (or rejected) the persisted session. */
    var isRestoringSession: Boolean by mutableStateOf(true)
        private set

    val users: StateFlow<List<User>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser == null) {
                    currentUser = null
                } else {
                    val profile = runCatching {
                        withTimeoutOrNull(8_000) {
                            repository.findProfile(firebaseUser.uid)
                        }
                    }.getOrNull()
                    if (profile != null) {
                        currentUser = profile
                    }
                }
                isRestoringSession = false
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> =
        runCatching { repository.signIn(email, password) }
            .onSuccess { currentUser = it }

    suspend fun signUp(username: String, email: String, password: String): Result<User> =
        runCatching { repository.signUp(username, email, password) }
            .onSuccess { currentUser = it }

    fun signOut() {
        repository.signOut()
        currentUser = null
    }

    suspend fun resetPassword(email: String): Result<Unit> =
        runCatching { repository.sendPasswordResetEmail(email) }

    suspend fun addStudent(username: String, email: String, password: String): Result<Unit> =
        runCatching { repository.createStudent(username, email, password) }.map { Unit }

    suspend fun rename(id: String, username: String): Result<Unit> = runCatching {
        repository.rename(id, username)
    }

    suspend fun remove(id: String): Result<Unit> = runCatching {
        repository.delete(id)
    }

    suspend fun seedDemoDataIfNeeded(): Result<Unit> = runCatching {
        repository.seedDemoDataIfNeeded()
    }
}
