package com.example.prelimproject.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import com.example.prelimproject.models.FlashcardViewModel
import com.example.prelimproject.models.SubjectViewModel
import com.example.prelimproject.models.UserViewModel
import com.example.prelimproject.screens.AdminCardListScreen
import com.example.prelimproject.screens.AdminScreen
import com.example.prelimproject.screens.FlashcardScreen
import com.example.prelimproject.screens.LoginScreen
import com.example.prelimproject.screens.SignUpScreen
import com.example.prelimproject.screens.SubjectListScreen

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val SUBJECTS = "subjects"
    const val FLASHCARDS = "flashcards/{subjectId}"
    const val ADMIN = "admin"
    const val ADMIN_CARD_LIST = "admin_card_list"

    fun flashcards(subjectId: String): String = "flashcards/$subjectId"
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current

    // Without google-services.json Firebase never initializes, and every
    // Firestore call would crash. Show the setup instructions instead.
    if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseSetupScreen()
        return
    }

    val navController = rememberNavController()
    var editingCardId by remember { mutableStateOf("") }

    val userViewModel: UserViewModel = viewModel()
    val subjectViewModel: SubjectViewModel = viewModel()
    val flashcardViewModel: FlashcardViewModel = viewModel()

    LaunchedEffect(Unit) {
        // Seeding is best effort: the screens surface their own errors.
        userViewModel.seedDemoDataIfNeeded()
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                userViewModel = userViewModel,
                onLoginSuccess = { user ->
                    val destination = if (user.role == "ADMIN") Routes.ADMIN else Routes.SUBJECTS
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                userViewModel = userViewModel,
                onSignUpSuccess = {
                    navController.navigate(Routes.SUBJECTS) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.SUBJECTS) {
            SubjectListScreen(
                userViewModel = userViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onSubjectClick = { subjectId ->
                    navController.navigate(Routes.flashcards(subjectId))
                },
                onAdminClick = { navController.navigate(Routes.ADMIN) },
                onLogout = {
                    userViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.FLASHCARDS,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { entry ->
            FlashcardScreen(
                subjectId = entry.arguments?.getString("subjectId") ?: "",
                userViewModel = userViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN) {
            AdminScreen(
                editingCardId = editingCardId,
                userViewModel = userViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onNavigateToCardList = { navController.navigate(Routes.ADMIN_CARD_LIST) },
                onClearEditing = { editingCardId = "" },
                onLogout = {
                    userViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADMIN_CARD_LIST) {
            AdminCardListScreen(
                userViewModel = userViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onEditCard = { cardId ->
                    editingCardId = cardId
                    navController.popBackStack()
                },
                onBackToForm = {
                    editingCardId = ""
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun FirebaseSetupScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect Firebase",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This build has no Firebase configuration, so the app cannot reach Firestore yet.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Create a Firebase project, add an Android app with the package name " +
                    "com.example.prelimproject, download google-services.json into the app/ " +
                    "folder, enable Cloud Firestore, then rebuild and reinstall.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
