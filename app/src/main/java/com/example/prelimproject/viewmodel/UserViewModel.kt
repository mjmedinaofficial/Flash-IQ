package com.example.prelimproject.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prelimproject.data.FlashIQRepository
import com.example.prelimproject.models.User
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class UserViewModel(private val repository: FlashIQRepository) : ViewModel() {

    /** Mirrors the users table; kept in sync by the repository Flow. */
    val users = mutableStateListOf<User>()

    init {
        viewModelScope.launch {
            repository.users.collect { list ->
                users.clear()
                users.addAll(list)
            }
        }
    }

    fun login(inputUser: String, inputPass: String): User? {
        for (user in users) {
            if (user.username == inputUser) {
                if (user.password == inputPass) {
                    return user
                }
            }
        }
        return null
    }

    fun isUsernameTaken(inputUser: String): Boolean {
        for (user in users) {
            if (user.username == inputUser) {
                return true
            }
        }
        return false
    }

    fun registerStudent(inputUser: String, inputPass: String, displayName: String = inputUser, email: String = ""): User {
        val newStudent = User(
            id = System.currentTimeMillis().toString(),
            username = inputUser,
            password = inputPass,
            role = "STUDENT",
            displayName = displayName,
            email = email
        )
        viewModelScope.launch { repository.insertUser(newStudent) }
        return newStudent
    }

    fun addUser(username: String, password: String, role: String = "STUDENT", displayName: String = username, email: String = ""): User? {
        if (isUsernameTaken(username)) return null
        val newUser = User(
            id = System.currentTimeMillis().toString(),
            username = username,
            password = password,
            role = role,
            displayName = displayName,
            email = email
        )
        viewModelScope.launch { repository.insertUser(newUser) }
        return newUser
    }

    fun removeUser(userId: String) {
        // Never remove the seeded admin account.
        if (userId == "1") return
        viewModelScope.launch { repository.deleteUser(userId) }
    }

    fun renameUser(userId: String, newUsername: String) {
        for (user in users) {
            if (user.id != userId && user.username == newUsername) return
        }
        val target = users.firstOrNull { it.id == userId } ?: return
        val updated = target.copy(username = newUsername, displayName = newUsername)
        viewModelScope.launch { repository.updateUser(updated) }
    }

    fun getStudentList(): List<User> {
        val studentList = mutableListOf<User>()
        for (user in users) {
            if (user.role == "STUDENT") {
                studentList.add(user)
            }
        }
        return studentList
    }
}
