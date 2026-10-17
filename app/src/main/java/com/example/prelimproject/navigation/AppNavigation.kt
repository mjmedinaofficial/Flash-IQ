package com.example.prelimproject.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prelimproject.FlashIQApplication
import com.example.prelimproject.screens.AdminCardListScreen
import com.example.prelimproject.screens.AdminScreen
import com.example.prelimproject.screens.FlashcardScreen
import com.example.prelimproject.screens.LoginScreen
import com.example.prelimproject.screens.SignUpScreen
import com.example.prelimproject.screens.SubjectListScreen
import com.example.prelimproject.viewmodel.CurrentUserViewModel
import com.example.prelimproject.viewmodel.FlashcardViewModel
import com.example.prelimproject.viewmodel.FlashcardViewModelFactory
import com.example.prelimproject.viewmodel.SubjectViewModel
import com.example.prelimproject.viewmodel.SubjectViewModelFactory
import com.example.prelimproject.viewmodel.UserViewModel
import com.example.prelimproject.viewmodel.UserViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val application = LocalContext.current.applicationContext as FlashIQApplication
    val repository = application.repository

    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(repository))
    val currentUserViewModel: CurrentUserViewModel = viewModel()
    val subjectViewModel: SubjectViewModel = viewModel(factory = SubjectViewModelFactory(repository))
    val flashcardViewModel: FlashcardViewModel = viewModel(factory = FlashcardViewModelFactory(repository))

    NavHost(
        navController = navController,
        startDestination = "login",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("login") {
            LoginScreen(
                userViewModel = userViewModel,
                currentUserViewModel = currentUserViewModel,
                onLoginAdmin = {
                    navController.navigate("admin") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onLoginStudent = {
                    navController.navigate("subjects") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                userViewModel = userViewModel,
                currentUserViewModel = currentUserViewModel,
                onSignUpSuccess = {
                    navController.navigate("subjects") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("subjects") {
            SubjectListScreen(
                currentUserViewModel = currentUserViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onSubjectClick = { subjectId ->
                    navController.navigate("flashcards/$subjectId")
                },
                onAdminClick = {
                    navController.navigate("admin")
                },
                onLogout = {
                    currentUserViewModel.logout()
                    flashcardViewModel.clearEditing()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(
            route = "flashcards/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            var subjectId = ""
            val arguments = backStackEntry.arguments
            if (arguments != null) {
                val takenSubjectId = arguments.getString("subjectId")
                if (takenSubjectId != null) {
                    subjectId = takenSubjectId
                }
            }
            FlashcardScreen(
                subjectId = subjectId,
                currentUserViewModel = currentUserViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("admin") {
            AdminScreen(
                userViewModel = userViewModel,
                currentUserViewModel = currentUserViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onNavigateToCardList = {
                    navController.navigate("admin_card_list")
                },
                onLogout = {
                    currentUserViewModel.logout()
                    flashcardViewModel.clearEditing()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        composable("admin_card_list") {
            AdminCardListScreen(
                userViewModel = userViewModel,
                currentUserViewModel = currentUserViewModel,
                subjectViewModel = subjectViewModel,
                flashcardViewModel = flashcardViewModel,
                onNavigateToEdit = {
                    if (!navController.popBackStack()) {
                        navController.navigate("admin")
                    }
                },
                onBackToForm = {
                    if (!navController.popBackStack()) {
                        navController.navigate("admin")
                    }
                }
            )
        }
    }
}
