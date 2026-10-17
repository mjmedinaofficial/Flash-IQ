package com.example.prelimproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prelimproject.ui.theme.pressScale
import com.example.prelimproject.ui.theme.pressableScale
import com.example.prelimproject.ui.theme.rememberPressInteraction
import com.example.prelimproject.viewmodel.CurrentUserViewModel
import com.example.prelimproject.viewmodel.FlashcardViewModel
import com.example.prelimproject.viewmodel.SubjectViewModel
import com.example.prelimproject.viewmodel.UserViewModel

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCardListScreen(
    userViewModel: UserViewModel,
    currentUserViewModel: CurrentUserViewModel,
    subjectViewModel: SubjectViewModel,
    flashcardViewModel: FlashcardViewModel,
    onNavigateToEdit: () -> Unit,
    onBackToForm: () -> Unit
) {
    val currentUser = currentUserViewModel.currentUser.value

    var isAdmin = false
    if (currentUser != null) {
        if (currentUser.role == "ADMIN") {
            isAdmin = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Flashcard Manager", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onBackToForm) { Text("Back to Form") }
                }
            )
        }
    ) { innerPadding ->
        if (currentUser == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else if (!isAdmin) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Access Denied",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Administrator privileges are required to access this dashboard.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToForm) {
                    Text(text = "Go Back")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = flashcardViewModel.flashcards.size.toString() + " cards total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val tonalBlocks = listOf(
                    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        flashcardViewModel.flashcards,
                        key = { card -> card.id }
                    ) { card ->

                        val cardSubjectTitle = subjectViewModel.getSubjectTitle(card.subjectId)

                        val assignedNames = mutableListOf<String>()
                        for (user in userViewModel.users) {
                            if (card.assignedUserIds.contains(user.id)) {
                                assignedNames.add(user.username)
                            }
                        }

                        var assignedStudentName = "Unassigned"
                        if (assignedNames.size > 0) {
                            val namesText = StringBuilder()
                            for (j in 0 until assignedNames.size) {
                                if (j > 0) {
                                    namesText.append(", ")
                                }
                                namesText.append(assignedNames[j])
                            }
                            assignedStudentName = namesText.toString()
                        }

                        var showUnassign by remember(card.id) { mutableStateOf(false) }

                        val blockColors = tonalBlocks[card.id.hashCode().and(Int.MAX_VALUE) % tonalBlocks.size]
                        val blockContainer = blockColors.first
                        val blockContent = blockColors.second

                        val cardPress = rememberPressInteraction()
                        val cardScale = pressScale(cardPress)

                        Card(
                            onClick = {
                                flashcardViewModel.editingCardId.value = card.id
                                onNavigateToEdit()
                            },
                            interactionSource = cardPress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressableScale(cardScale)
                                .animateItem(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = blockContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(blockContent.copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = cardSubjectTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = blockContent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = card.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = blockContent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = card.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = blockContent.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Assigned to: " + assignedStudentName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = blockContent.copy(alpha = 0.9f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (assignedNames.isNotEmpty()) {
                                        FilledTonalButton(
                                            onClick = { showUnassign = !showUnassign }
                                        ) {
                                            Text(text = "Unassign (" + assignedNames.size + ")", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            flashcardViewModel.editingCardId.value = card.id
                                            onNavigateToEdit()
                                        }
                                    ) {
                                        Text(text = "Edit", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            flashcardViewModel.deleteFlashcard(card.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text(text = "Delete", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showUnassign && assignedNames.isNotEmpty(),
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to unassign:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = blockContent.copy(alpha = 0.8f)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (user in userViewModel.users) {
                                                if (!card.assignedUserIds.contains(user.id)) continue
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Checkbox(
                                                        checked = true,
                                                        onCheckedChange = { checked ->
                                                            if (!checked) {
                                                                flashcardViewModel.toggleAssignment(card.id, user.id)
                                                            }
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = user.username, fontSize = 13.sp, color = blockContent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
