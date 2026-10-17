package com.example.prelimproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prelimproject.ui.theme.pressScale
import com.example.prelimproject.ui.theme.pressableScale
import com.example.prelimproject.ui.theme.rememberPressInteraction
import com.example.prelimproject.viewmodel.CurrentUserViewModel
import com.example.prelimproject.viewmodel.FlashcardViewModel
import com.example.prelimproject.viewmodel.SubjectViewModel

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    currentUserViewModel: CurrentUserViewModel,
    subjectViewModel: SubjectViewModel,
    flashcardViewModel: FlashcardViewModel,
    onSubjectClick: (String) -> Unit,
    onAdminClick: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = currentUserViewModel.currentUser.value

    var isAdmin = false
    var displayName = "User"
    var displayRole = "STUDENT"
    if (currentUser != null) {
        if (currentUser.role == "ADMIN") {
            isAdmin = true
        }
        displayName = currentUser.username
        displayRole = currentUser.role
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Flash IQ", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isAdmin) {
                        TextButton(onClick = onAdminClick) { Text("Admin") }
                    }
                    TextButton(onClick = onLogout) { Text("Logout", color = MaterialTheme.colorScheme.error) }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hello, " + displayName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Role: " + displayRole,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select a subject to study",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expressive tonal card blocks, cycling container colors like M3 Expressive.
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
                itemsIndexed(
                    subjectViewModel.subjects,
                    key = { _, subject -> subject.id }
                ) { index, subject ->

                    var cardCount = 0
                    for (card in flashcardViewModel.flashcards) {
                        if (card.subjectId == subject.id) {
                            if (isAdmin) {
                                cardCount = cardCount + 1
                            } else {
                                if (currentUser != null) {
                                    if (card.assignedUserIds.contains(currentUser.id)) {
                                        cardCount = cardCount + 1
                                    }
                                }
                            }
                        }
                    }

                    val blockColors = tonalBlocks[index % tonalBlocks.size]
                    val blockContainer = blockColors.first
                    val blockContent = blockColors.second

                    val cardPress = rememberPressInteraction()
                    val cardScale = pressScale(cardPress)

                    Card(
                        onClick = { onSubjectClick(subject.id) },
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
                            Text(
                                text = subject.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = blockContent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subject.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = blockContent.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            var labelText = cardCount.toString() + " cards assigned"
                            if (isAdmin) {
                                labelText = cardCount.toString() + " total cards"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(blockContent.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = blockContent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
