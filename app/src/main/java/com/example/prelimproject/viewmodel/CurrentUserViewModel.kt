package com.example.prelimproject.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.prelimproject.models.User

class CurrentUserViewModel : ViewModel() {

    var currentUser = mutableStateOf<User?>(null)

    fun setCurrentUser(user: User) {
        currentUser.value = user
    }

    fun logout() {
        currentUser.value = null
    }
}
