package com.example.prelimproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prelimproject.models.Flashcard
import com.example.prelimproject.models.FlashcardFactory
import com.example.prelimproject.models.FlashcardViewModel
import com.example.prelimproject.models.SubjectViewModel
import com.example.prelimproject.models.User
import com.example.prelimproject.models.UserViewModel
import com.example.prelimproject.models.firebaseErrorMessage
import com.example.prelimproject.models.newId
import com.example.prelimproject.ui.theme.pressScale
import com.example.prelimproject.ui.theme.pressableScale
import com.example.prelimproject.ui.theme.rememberPressInteraction
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    editingCardId: String,
    userViewModel: UserViewModel,
    subjectViewModel: SubjectViewModel,
    flashcardViewModel: FlashcardViewModel,
    onNavigateToCardList: () -> Unit,
    onClearEditing: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = userViewModel.currentUser
    val users by userViewModel.users.collectAsState()
    val subjects by subjectViewModel.subjects.collectAsState()
    val flashcards by flashcardViewModel.flashcards.collectAsState()
    val scope = rememberCoroutineScope()
    var errorText by remember { mutableStateOf("") }

    var isAdmin = false
    if (currentUser != null) {
        if (currentUser.role == "ADMIN") {
            isAdmin = true
        }
    }

    val studentList = mutableListOf<User>()
    for (user in users) {
        if (user.role != "ADMIN") {
            studentList.add(user)
        }
    }

    val editingCard = flashcards.firstOrNull { it.id == editingCardId }

    var inputQuestion by remember(editingCardId) { mutableStateOf("") }
    var inputAnswer by remember(editingCardId) { mutableStateOf("") }
    var selectedSubjectId by remember(editingCardId) { mutableStateOf("") }
    val selectedStudentIds = remember(editingCardId) { mutableStateListOf<String>() }

    // The card arrives asynchronously from Firestore, so fill the form when it loads.
    LaunchedEffect(editingCard) {
        val card = editingCard
        if (card != null) {
            inputQuestion = card.question
            inputAnswer = card.answer
            selectedSubjectId = card.subjectId
            selectedStudentIds.clear()
            for (id in card.assignedUserIds) {
                selectedStudentIds.add(id)
            }
        }
    }

    val effectiveSubjectId = if (selectedSubjectId != "") {
        selectedSubjectId
    } else {
        subjects.firstOrNull()?.id ?: ""
    }
    val selectedSubjectTitle = subjects.firstOrNull { it.id == effectiveSubjectId }?.title ?: "Select subject"

    var newSubjectTitle by remember { mutableStateOf("") }
    var newSubjectDesc by remember { mutableStateOf("") }
    var newUserUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }

    var selectedSection by remember { mutableStateOf(if (editingCardId != "") "flashcard" else null) }
    LaunchedEffect(editingCardId) {
        if (editingCardId != "") {
            selectedSection = "flashcard"
        }
    }

    fun saveFlashcard() {
        if (inputQuestion == "" || inputAnswer == "") {
            errorText = "A question and an answer are required."
            return
        }
        if (effectiveSubjectId == "") {
            errorText = "Create a subject before saving a card."
            return
        }
        val card = FlashcardFactory.create(
            subjectId = effectiveSubjectId,
            question = inputQuestion,
            answer = inputAnswer,
            assignedUserIds = selectedStudentIds.toList(),
            hint = editingCard?.hint ?: "",
            id = if (editingCardId != "") editingCardId else newId(),
            createdAt = editingCard?.createdAt ?: System.currentTimeMillis()
        )
        scope.launch {
            flashcardViewModel.save(card)
                .onSuccess {
                    inputQuestion = ""
                    inputAnswer = ""
                    selectedSubjectId = ""
                    selectedStudentIds.clear()
                    errorText = ""
                    if (editingCardId != "") onClearEditing()
                }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    fun addSubject(title: String, description: String) {
        scope.launch {
            subjectViewModel.add(title, description)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    fun renameSubject(id: String, title: String) {
        scope.launch {
            subjectViewModel.rename(id, title)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    /** Deleting a subject also deletes the cards that belong to it. */
    fun removeSubject(id: String) {
        scope.launch {
            subjectViewModel.remove(id)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    fun addUser(username: String, password: String) {
        scope.launch {
            userViewModel.addStudent(username, password)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    fun renameUser(id: String, username: String) {
        scope.launch {
            userViewModel.rename(id, username)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    /** Deleting a student also removes them from every card they were assigned. */
    fun removeUser(id: String) {
        scope.launch {
            userViewModel.remove(id)
                .onSuccess { errorText = "" }
                .onFailure { error -> errorText = firebaseErrorMessage(error) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        if (currentUser == null) {
            // Logging out; avoid flashing "Access Denied".
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
                Button(onClick = onLogout) {
                    Text(text = "Go to Login")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (errorText != "") {
                    Text(
                        text = errorText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Header with back when in a section
                if (selectedSection != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (editingCardId != "") {
                                    onClearEditing()
                                    inputQuestion = ""
                                    inputAnswer = ""
                                    selectedStudentIds.clear()
                                    onNavigateToCardList()
                                } else {
                                    selectedSection = null
                                }
                            }
                        ) {
                            Text(text = "Back")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (selectedSection) {
                                "flashcard" -> if (editingCardId == "") "Create Flashcard" else "Edit Flashcard"
                                "subjects" -> "Manage Subjects"
                                "users" -> "Manage Users"
                                else -> "Admin Panel"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Text(
                        text = "Choose what you want to manage",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = selectedSection == null,
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 6 }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 6 })
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Hub cards in expressive tonal blocks
                        val hubBlocks = listOf(
                            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
                        )
                        AdminHubCard(
                            icon = Icons.Filled.Add,
                            title = "Create Flashcard",
                            description = "Write a new question and answer, then assign it to students.",
                            containerColor = hubBlocks[0].first,
                            contentColor = hubBlocks[0].second,
                            onClick = { selectedSection = "flashcard" }
                        )
                        AdminHubCard(
                            icon = Icons.Filled.Book,
                            title = "Manage Subjects",
                            description = "Add, rename, or remove subjects.",
                            containerColor = hubBlocks[1].first,
                            contentColor = hubBlocks[1].second,
                            onClick = { selectedSection = "subjects" }
                        )
                        AdminHubCard(
                            icon = Icons.Filled.Group,
                            title = "Manage Users",
                            description = "Add, rename, or remove student accounts.",
                            containerColor = hubBlocks[2].first,
                            contentColor = hubBlocks[2].second,
                            onClick = { selectedSection = "users" }
                        )
                        AdminHubCard(
                            icon = Icons.AutoMirrored.Filled.ViewList,
                            title = "View All Flashcards",
                            description = "Browse, assign, edit, and delete every card. (" + flashcards.size + ")",
                            containerColor = hubBlocks[3].first,
                            contentColor = hubBlocks[3].second,
                            onClick = onNavigateToCardList
                        )
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(text = "Logout", fontSize = 15.sp)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSection == "flashcard",
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 6 }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 6 })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Subject",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                var subjectExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = subjectExpanded,
                                    onExpandedChange = { subjectExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedSubjectTitle,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select subject") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = subjectExpanded,
                                        onDismissRequest = { subjectExpanded = false }
                                    ) {
                                        for (subject in subjects) {
                                            DropdownMenuItem(
                                                text = { Text(subject.title) },
                                                onClick = {
                                                    selectedSubjectId = subject.id
                                                    subjectExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            OutlinedTextField(
                                value = inputQuestion,
                                onValueChange = { newValue -> inputQuestion = newValue },
                                label = { Text("Question") },
                                shape = MaterialTheme.shapes.large,
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = inputAnswer,
                                onValueChange = { newValue -> inputAnswer = newValue },
                                label = { Text("Answer") },
                                shape = MaterialTheme.shapes.large,
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Assign to students",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (studentList.isEmpty()) {
                                    Text(
                                        text = "No students registered yet.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    var studentSearch by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = studentSearch,
                                        onValueChange = { studentSearch = it },
                                        label = { Text("Search students") },
                                        shape = MaterialTheme.shapes.large,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val filteredStudents = studentList.filter {
                                        it.username.contains(studentSearch, ignoreCase = true)
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())
                                    ) {
                                        for (student in filteredStudents) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = selectedStudentIds.contains(student.id),
                                                    onCheckedChange = { isChecked ->
                                                        if (isChecked) {
                                                            if (!selectedStudentIds.contains(student.id)) {
                                                                selectedStudentIds.add(student.id)
                                                            }
                                                        } else {
                                                            selectedStudentIds.remove(student.id)
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = student.username, fontSize = 14.sp)
                                            }
                                        }
                                        if (filteredStudents.isEmpty()) {
                                            Text(
                                                text = "No matching students.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { saveFlashcard() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = if (editingCardId == "") "Save Flashcard" else "Save Changes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (editingCardId != "") {
                                OutlinedButton(
                                    onClick = {
                                        onClearEditing()
                                        inputQuestion = ""
                                        inputAnswer = ""
                                        selectedStudentIds.clear()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Cancel Edit", fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSection == "subjects",
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 6 }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 6 })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Subjects",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            for (subject in subjects) {
                                var title by remember(subject.id) { mutableStateOf(subject.title) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { newValue ->
                                            title = newValue
                                            renameSubject(subject.id, newValue)
                                        },
                                        label = { Text("Subject name") },
                                        shape = MaterialTheme.shapes.large,
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { removeSubject(subject.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text("Remove", fontSize = 12.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            Text(
                                text = "Add new subject",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newSubjectTitle,
                                onValueChange = { newSubjectTitle = it },
                                label = { Text("Title") },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newSubjectDesc,
                                onValueChange = { newSubjectDesc = it },
                                label = { Text("Description") },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newSubjectTitle != "") {
                                        addSubject(newSubjectTitle, newSubjectDesc)
                                        newSubjectTitle = ""
                                        newSubjectDesc = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text("Add Subject", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSection == "users",
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 6 }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 6 })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Students",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (studentList.isEmpty()) {
                                Text(
                                    text = "No students registered yet.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            for (user in studentList) {
                                var username by remember(user.id) { mutableStateOf(user.username) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { newValue ->
                                            username = newValue
                                            renameUser(user.id, newValue)
                                        },
                                        label = { Text("Username") },
                                        shape = MaterialTheme.shapes.large,
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { removeUser(user.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text("Remove", fontSize = 12.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            Text(
                                text = "Add new student",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newUserUsername,
                                onValueChange = { newUserUsername = it },
                                label = { Text("Username") },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newUserPassword,
                                onValueChange = { newUserPassword = it },
                                label = { Text("Password") },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newUserUsername != "" && newUserPassword != "") {
                                        addUser(newUserUsername, newUserPassword)
                                        newUserUsername = ""
                                        newUserPassword = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text("Add User", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AdminHubCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val hubPress = rememberPressInteraction()
    val hubScale = pressScale(hubPress)
    Card(
        onClick = onClick,
        interactionSource = hubPress,
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(hubScale),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(contentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
