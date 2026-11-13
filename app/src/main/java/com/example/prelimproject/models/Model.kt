package com.example.prelimproject.models

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object Keys {
    const val USERNAME = "username"
    const val ROLE = "role"
    const val DISPLAY_NAME = "displayName"
    const val EMAIL = "email"

    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val COLOR = "color"

    const val SUBJECT_ID = "subjectId"
    const val QUESTION = "question"
    const val ANSWER = "answer"
    const val ASSIGNED_USER_IDS = "assignedUserIds"
    const val HINT = "hint"

    const val CREATED_AT = "createdAt"
}

object Roles {
    const val ADMIN = "ADMIN"
    const val STUDENT = "STUDENT"
}

data class User(
    val id: String,
    val username: String,
    val role: String = Roles.STUDENT,
    val displayName: String = username,
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Subject(
    val id: String,
    val title: String,
    val description: String,
    val color: String = "#007AFF",
    val createdAt: Long = System.currentTimeMillis()
)

data class Flashcard(
    val id: String,
    val subjectId: String,
    val question: String,
    val answer: String,
    val assignedUserIds: List<String> = emptyList(),
    val hint: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
fun newId(): String = System.currentTimeMillis().toString()
fun firebaseErrorMessage(error: Throwable): String {
    return when (error) {
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                "Incorrect email or password."
            "ERROR_USER_NOT_FOUND" -> "No account found for that email."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Email/password sign-in is not enabled in Firebase."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Cannot reach Firebase. Check your internet connection."
            else -> error.message ?: "Authentication failed."
        }
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore denied this request. Check the security rules."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Cannot reach Firebase. Check your internet connection."
            else -> error.message ?: "Firestore error: ${error.code.name}"
        }
        is FirebaseNetworkException -> "Cannot reach Firebase. Check your internet connection."
        else -> error.message ?: "Something went wrong."
    }
}
