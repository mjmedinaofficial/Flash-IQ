package com.example.prelimproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prelimproject.models.User
import com.example.prelimproject.models.UserViewModel
import com.example.prelimproject.models.firebaseErrorMessage
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userViewModel: UserViewModel,
    onLoginSuccess: (User) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var inputEmail: String by remember { mutableStateOf("") }
    var inputPass: String by remember { mutableStateOf("") }
    var errorMessage: String by remember { mutableStateOf("") }
    var statusMessage: String by remember { mutableStateOf("") }
    var isBusy: Boolean by remember { mutableStateOf(false) }
    var isResetting: Boolean by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Flash IQ",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "A student flashcard portal for reviewing assigned cards",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { newValue ->
                            inputEmail = newValue
                            errorMessage = ""
                            statusMessage = ""
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = { newValue ->
                            inputPass = newValue
                            errorMessage = ""
                            statusMessage = ""
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = errorMessage != "",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = statusMessage != "",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = statusMessage,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (inputEmail == "" || inputPass == "") {
                                errorMessage = "Please enter both fields"
                            } else if (!inputEmail.contains("@")) {
                                errorMessage = "Enter a valid email address"
                            } else {
                                isBusy = true
                                errorMessage = ""
                                statusMessage = ""
                                scope.launch {
                                    try {
                                        userViewModel.signIn(inputEmail, inputPass)
                                            .onSuccess { user ->
                                                onLoginSuccess(user)
                                            }
                                            .onFailure { error ->
                                                errorMessage = firebaseErrorMessage(error)
                                            }
                                    } catch (error: Throwable) {
                                        errorMessage = firebaseErrorMessage(error)
                                    } finally {
                                        isBusy = false
                                    }
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (isBusy) "Signing in..." else "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            if (inputEmail == "" || !inputEmail.contains("@")) {
                                statusMessage = ""
                                errorMessage = "Enter your email address above first"
                            } else {
                                isResetting = true
                                errorMessage = ""
                                statusMessage = ""
                                scope.launch {
                                    try {
                                        userViewModel.resetPassword(inputEmail)
                                            .onSuccess {
                                                statusMessage =
                                                    "Password reset email sent to ${inputEmail.trim()}"
                                            }
                                            .onFailure { error ->
                                                errorMessage = firebaseErrorMessage(error)
                                            }
                                    } catch (error: Throwable) {
                                        errorMessage = firebaseErrorMessage(error)
                                    } finally {
                                        isResetting = false
                                    }
                                }
                            }
                        },
                        enabled = !isBusy && !isResetting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isResetting) "Sending reset email..." else "Forgot password?",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FilledTonalButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(text = "Create Student Account")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Demo Admin: admin@flashiq.app / 123456",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
