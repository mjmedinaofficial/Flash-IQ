package com.example.prelimproject.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    var currentUser: User? by mutableStateOf(null)
        private set

    val users: StateFlow<List<User>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun signIn(username: String, password: String): Result<User?> =
        runCatching { repository.signIn(username, password) }
            .onSuccess { user -> if (user != null) currentUser = user }

    suspend fun signUp(username: String, password: String): Result<User> = runCatching {
        if (repository.usernameTaken(username)) {
            throw IllegalStateException("That username is already taken.")
        }
        repository.createStudent(username = username, password = password)
    }.onSuccess { currentUser = it }

    fun signOut() {
        currentUser = null
    }

    suspend fun addStudent(username: String, password: String): Result<Unit> = runCatching {
        repository.createStudent(username = username, password = password)
    }.map { Unit }

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
