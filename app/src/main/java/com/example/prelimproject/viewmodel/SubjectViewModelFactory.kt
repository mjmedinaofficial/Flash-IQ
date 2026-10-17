package com.example.prelimproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.prelimproject.data.FlashIQRepository

/** Builds a [SubjectViewModel] with the application-wide repository. */
class SubjectViewModelFactory(private val repository: FlashIQRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubjectViewModel(repository) as T
    }
}
