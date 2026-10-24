package com.example.prelimproject.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prelimproject.models.Flashcard
import com.example.prelimproject.models.FlashcardViewModel
import com.example.prelimproject.models.SubjectViewModel
import com.example.prelimproject.models.UserViewModel
import com.example.prelimproject.ui.theme.pressScale
import com.example.prelimproject.ui.theme.pressableScale
import com.example.prelimproject.ui.theme.rememberPressInteraction

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    subjectId: String,
    userViewModel: UserViewModel,
    subjectViewModel: SubjectViewModel,
    flashcardViewModel: FlashcardViewModel,
    onBack: () -> Unit
) {
    val currentUser = userViewModel.currentUser
    val subjects by subjectViewModel.subjects.collectAsState()
    val flashcards by flashcardViewModel.flashcards.collectAsState()

    var isAdmin = false
    if (currentUser != null) {
        if (currentUser.role == "ADMIN") {
            isAdmin = true
        }
    }

    val subjectTitle = subjects.firstOrNull { it.id == subjectId }?.title ?: "Subject"

    val visibleCards = mutableListOf<Flashcard>()
    for (card in flashcards) {
        if (card.subjectId == subjectId) {
            if (isAdmin) {
                visibleCards.add(card)
            } else {
                if (currentUser != null) {
                    if (card.assignedUserIds.contains(currentUser.id)) {
                        visibleCards.add(card)
                    }
                }
            }
        }
    }

    var currentIndex: Int by remember { mutableIntStateOf(0) }
    var showAnswer: Boolean by remember(currentIndex) { mutableStateOf(false) }

    val rotationY by animateFloatAsState(
        targetValue = if (showAnswer) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardTurn"
    )

    val studyPress = rememberPressInteraction()
    val studyScale = pressScale(studyPress, pressedScale = 0.985f)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(subjectTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (visibleCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No flashcards assigned for this subject yet.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (currentIndex >= visibleCards.size) {
                    currentIndex = 0
                    showAnswer = false
                }
                val currentCard = visibleCards[currentIndex]

                AnimatedContent(
                    targetState = currentIndex,
                    label = "cardCounter"
                ) { index ->
                    Text(
                        text = "Card " + (index + 1) + " of " + visibleCards.size,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    onClick = { showAnswer = !showAnswer },
                    interactionSource = studyPress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pressableScale(studyScale)
                        .graphicsLayer { this.rotationY = rotationY },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (rotationY <= 90f) {
                            Text(
                                text = "QUESTION",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentCard.question,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Column(
                                modifier = Modifier.graphicsLayer { this.rotationY = 180f },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ANSWER",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = currentCard.answer,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showAnswer = !showAnswer },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    var buttonText = "Show Answer"
                    if (showAnswer) {
                        buttonText = "Show Question"
                    }
                    Text(text = buttonText, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex = currentIndex - 1
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Previous")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < visibleCards.size - 1) {
                                currentIndex = currentIndex + 1
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex < visibleCards.size - 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Next")
                    }
                }
            }
        }
    }
}
