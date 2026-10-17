# FlashIQ — Student Flashcard Portal

### Android Application Conceptualization and UI Development
### Preliminary Period Project Documentation

**Course:** Mobile Development (Kotlin and Jetpack Compose)
**Package:** `com.example.prelimproject`
**Platform:** Android, `minSdk 24`, `targetSdk 37`
**UI Toolkit:** Jetpack Compose with Material 3
**Stage:** Preliminary Period

---

## Table of Contents

1. [Project Concept](#1-project-concept)
2. [Project Proposal](#2-project-proposal)
3. [User Interface and Screen Planning](#3-user-interface-and-screen-planning)
4. [Screen Flow](#4-screen-flow)
5. [Wireframes](#5-wireframes)
6. [Prelim Technical Requirements](#6-prelim-technical-requirements)
7. [CRUD Functionality](#7-crud-functionality)
8. [Data Storage](#8-data-storage)
9. [Project Development Process](#9-project-development-process)
10. [Prelim Deliverables](#10-prelim-deliverables)
11. [Project Guidelines Compliance](#11-project-guidelines-compliance)
12. [Continuation of the Project](#12-continuation-of-the-project)
13. [Expected Prelim Output](#13-expected-prelim-output)
14. [Appendix A — Project File Structure](#appendix-a--project-file-structure)
15. [Appendix B — How to Build and Run](#appendix-b--how-to-build-and-run)
16. [Appendix C — Demo Accounts](#appendix-c--demo-accounts)

---

## 1. Project Concept

FlashIQ is a study application where a student can review flashcards and a teacher/administrator can build the flashcard sets. The idea came from the review sessions before examinations: printed question sheets are easy to lose, hard to update, and impossible to personalize per student. Each student also learns at a different pace, so a single printed set does not fit everyone.

FlashIQ answers this by separating the two roles. A student signs in, opens a subject, and flips through only the cards that were assigned to them. An administrator signs in to a separate panel where new cards can be written, assigned to specific students, edited, and deleted. The assignment step is the important part: it is what turns a shared pile of cards into a personal study set.

The application was chosen because it needs the exact set of concepts covered in the Preliminary lessons — several screens, a collection that must be displayed with a lazy list, text input for authoring cards, an identifier that has to travel between screens, and full Create/Read/Update/Delete on a single object type. It is small enough to finish in the period but structured so that later periods can add real persistence, quiz mode, and spaced repetition on top of it.

---

## 2. Project Proposal

### 2.1 Application Title

**FlashIQ — Student Flashcard Portal**

### 2.2 Problem Statement

Students prepare for quizzes using printed or shared flashcard sets, which creates four recurring problems:

1. **No personalization.** A single sheet is given to the whole class, so a student who already mastered a topic still has to review it, and a student who is behind does not get the extra cards they need.
2. **Hard to update.** When a lesson changes or a new topic is covered, the physical sheet has to be reprinted and redistributed.
3. **No ownership.** Whoever holds the paper controls the content, so cards are frequently lost, defaced, or left at home.
4. **No ownership of who studies what.** A teacher cannot tell which cards a specific student has actually worked on.

### 2.3 Proposed Solution

FlashIQ stores flashcards digitally and lets an administrator decide which student receives which card.

- An **administrator** writes a card (question, answer, subject), then ticks the students who should receive it. The administrator can also correct or remove any card at any time, so the content stays current without reprinting anything.
- A **student** signs in, picks a subject, and sees only the cards assigned to them. Each card flips to reveal the answer, and the student moves forward and backward through their set.
- Because assignment is stored per card, every student effectively gets a different study set from the same pool of questions. The same four subjects and twelve seed cards already produce different counts for an administrator and for a student.

This removes the need for printing, makes updates instant, and gives each student a study set built for them.

### 2.4 Target Users

| User | Description | What They Need |
|---|---|---|
| **Student** | A learner enrolled in a subject who is preparing for a quiz or review. | To see only the material assigned to them, and to study it in a simple flip-through manner. |
| **Administrator** | A teacher, instructor, or study group leader who prepares the review material. | To create, assign, correct, and delete cards quickly, and to know how many cards exist. |

### 2.5 Main Features

1. **Login and Logout** — students and administrators sign in with a username and password, and the application routes each one to the correct starting screen.
2. **Student Account Registration** — a new student can create an account, and the application refuses a username that is already taken.
3. **Subject Browsing** — a list of subjects, each showing how many cards the signed-in user actually has for it.
4. **Flashcard Study Mode** — cards are shown one at a time with a flip animation to reveal the answer, plus Previous and Next controls.
5. **Per-Student Card Assignment** — an administrator ticks which students receive each card.
6. **Create Flashcard** — an administrator writes a new question and answer and picks its subject.
7. **Update Flashcard** — an administrator loads an existing card into the form, changes it, and saves.
8. **Delete Flashcard** — an administrator removes a card permanently from the manager list.
9. **Flashcard Manager List** — a scrollable overview of every card showing its subject, question, answer, and assigned students, with the action buttons on each row.
10. **Role-Based Access Control** — non-administrators who reach an administrator screen are blocked with an "Access Denied" message.
11. **In-App Validation** — empty fields and wrong credentials are reported on the screen instead of failing silently.

### 2.6 Data or Information to be Managed

The application manages four objects:

| Object | Fields | Created By | Modified By | Removed By |
|---|---|---|---|---|
| **User** | `id`, `username`, `password`, `role` | Student (registration) | — | — |
| **Subject** | `id`, `title`, `description` | Predefined in the subject data | — | — |
| **Flashcard** | `id`, `subjectId`, `question`, `answer`, `assignedUserIds` | Administrator | Administrator | Administrator |
| **Session (current user)** | the `User` reference that is signed in | Login / registration | Login switches it | Logout clears it |

Two relationships hold the data together:

- A **Flashcard belongs to a Subject** through `subjectId`.
- A **Flashcard is assigned to many Students** through `assignedUserIds`, which stores user ids.

The subject list is fixed for this period, so only `User` and `Flashcard` take part in full CRUD. The session object is the smallest of the four and exists only to route the user and to decide what they are allowed to see.

---

## 3. User Interface and Screen Planning

The application contains **six screens**, which satisfies the minimum of three.

| # | Screen | File | Route | Purpose | Primary User |
|---|---|---|---|---|---|
| 1 | Login | `screens/LoginScreen.kt` | `login` | Authenticate and route by role. | All |
| 2 | Student Registration | `screens/SignUpScreen.kt` | `signup` | Create a new student account. | New student |
| 3 | Subject List | `screens/SubjectListScreen.kt` | `subjects` | Browse subjects and open a study set. | Student, Admin |
| 4 | Flashcards | `screens/FlashcardScreen.kt` | `flashcards/{subjectId}` | Flip through the cards of one subject. | Student, Admin |
| 5 | Admin Panel | `screens/AdminScreen.kt` | `admin` | Create or edit a card and assign students. | Admin only |
| 6 | Flashcard Manager | `screens/AdminCardListScreen.kt` | `admin_card_list` | View, assign, edit, and delete all cards. | Admin only |

**Layout decisions**

- Screens 1 and 2 use a centered `Column` because the content is a short form and vertical centering looks intentional on a phone.
- Screens 3, 4, 5, and 6 use a full-height `Column` with a `Scaffold`, because they hold lists that must use the remaining space.
- Screens 3 and 6 use a `LazyColumn` since their content length is not known in advance.
- Screen 4 uses a `Box` for its empty state so the message can be centered in the leftover area.
- Every screen is wrapped in a `Scaffold` so that the inset padding is handled in one consistent place.

---

## 4. Screen Flow

```
                          ┌───────────────────┐
                          │   1. LOGIN        │   start destination
                          │  (all users)      │
                          └─────────┬─────────┘
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
     tap "Create Student    admin logs in        student logs in
     Account"                │                      │
             │                      │                      │
             v                      v                      v
      ┌─────────────┐      ┌──────────────┐      ┌────────────────┐
      │ 2. SIGN UP  │      │ 5. ADMIN     │      │ 3. SUBJECT LIST│
      │             │      │    PANEL     │      │                │
      └──────┬──────┘      └──────┬───────┘      └───────┬────────┘
             │                    │                      │
   "Back to  │            "View All Flashcards"   tap a subject
    Login"   │                    │                      │
             └────────────┐       v                      v
                          │  ┌──────────────────────┐  ┌─────────────────────┐
                          │  │ 6. FLASHCARD MANAGER│  │ 4. FLASHCARDS       │
                          │  └───────┬────────┬─────┘  │    (one subject)    │
                          │          │        │        └──────────┬──────────┘
                          │  "Edit"  │        │ "Back to Form"     │ "Back"
                          │          v        └────────────────────┼──────────┐
                          │  returns to the Admin Panel           │          │
                          │  with the card loaded for editing     └──────────┘
                          │                                              │
                          │                                        back to 3
                          │
                          └── Logout from 3 or 5 ──> returns to 1. LOGIN
```

**Route table as implemented in `navigation/AppNavigation.kt`**

| Route | Arguments | Navigated to from |
|---|---|---|
| `login` | — | Start destination; also after logout |
| `signup` | — | Login screen |
| `subjects` | — | Student login; successful registration |
| `flashcards/{subjectId}` | `subjectId: String` | Subject List, on card tap |
| `admin` | — | Admin login; "Admin" button on Subject List |
| `admin_card_list` | — | "View All Flashcards" on Admin Panel |

**How the identifier is passed.** Tapping a subject in screen 3 does not pass a whole object. It navigates to the route `flashcards/{subjectId}` with the id in the path, and Navigation Compose delivers it as a `navArgument` of type `NavType.StringType`. The Flashcards screen reads that argument back out and uses it to look up the subject title and to select the cards. This is the only data that crosses between screens; everything else stays in the ViewModel.

**Back-stack behaviour.** Login clears itself from the back stack so the hardware back button cannot return to it after a successful login. Logout clears the whole stack with `popUpTo(0)` so no authenticated screen remains reachable. The Flashcards and Flashcard Manager screens use `popBackStack()` so the hardware back button returns the user to the screen they came from.

---

## 5. Wireframes

### 5.1 Login (`login`)

```
┌──────────────────────────────────┐
│                                  │
│                                  │
│        Flashcard Study App       │   <- Text, 24sp bold
│      Student and Admin Portal    │   <- Text, 14sp secondary
│                                  │
│    ┌────────────────────────┐    │
│    │ Username               │    │   <- OutlinedTextField
│    └────────────────────────┘    │
│                                  │
│    ┌────────────────────────┐    │
│    │ Password         ****  │    │   <- OutlinedTextField, masked
│    └────────────────────────┘    │
│                                  │
│    ! Invalid username or password│   <- only when there is an error
│                                  │
│    ┌────────────────────────┐    │
│    │        Login           │    │   <- Button, full width
│    └────────────────────────┘    │
│                                  │
│    ┌────────────────────────┐    │
│    │  Create Student Account│    │   <- OutlinedButton, full width
│    └────────────────────────┘    │
│                                  │
│  Demo Accounts: Admin/123 Mark/123│   <- Text, 12sp
│                                  │
└──────────────────────────────────┘
```

### 5.2 Student Registration (`signup`)

```
┌──────────────────────────────────┐
│                                  │
│        Student Registration      │   <- Text, 24sp bold
│      Create an account to study  │   <- Text, 14sp secondary
│                                  │
│    ┌────────────────────────┐    │
│    │ Username               │    │   <- OutlinedTextField
│    └────────────────────────┘    │
│                                  │
│    ┌────────────────────────┐    │
│    │ Password         ****  │    │   <- OutlinedTextField, masked
│    └────────────────────────┘    │
│                                  │
│    ! Username already exists     │   <- only when there is an error
│                                  │
│    ┌────────────────────────┐    │
│    │       Register         │    │   <- Button, full width
│    └────────────────────────┘    │
│                                  │
│    ┌────────────────────────┐    │
│    │     Back to Login      │    │   <- OutlinedButton, full width
│    └────────────────────────┘    │
│                                  │
└──────────────────────────────────┘
```

### 5.3 Subject List (`subjects`)

```
┌──────────────────────────────────┐
│ Hello, Mark            [Admin]   │   <- Row, "Admin" only for admin
│ Role: STUDENT         [Logout]   │      OutlinedButton [Logout]
├──────────────────────────────────┤
│                                  │
│   Select a Subject to Study:     │   <- Text, 18sp bold
│                                  │
│  ┌────────────────────────────┐  │   <- LazyColumn
│  │ Project Management         │  │      Card, clickable
│  │ Agile methodologies, ...   │  │
│  │ 3 cards assigned           │  │   <- primary colour, bold
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ Application Development    │  │
│  │ Software architecture,...  │  │
│  │ 3 cards assigned           │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ Mobile Development         │  │
│  │ Android development,...    │  │
│  │ 3 cards assigned           │  │
│  └────────────────────────────┘  │
│         ... scrolls ...          │
└──────────────────────────────────┘
```

The footer count line changes with the role: an **administrator** sees `N total cards`, a **student** sees `N cards assigned`.

### 5.4 Flashcards (`flashcards/{subjectId}`)

```
┌──────────────────────────────────┐
│ ┌────────┐  Project Management  │   <- Row: OutlinedButton + title
│ │ Back   │                      │
│ └────────┘                      │
│                                  │
│  Card 1 of 3                     │   <- Text, 14sp secondary
│                                  │
│ ┌────────────────────────────┐   │
│ │                            │   │
│ │          QUESTION          │   │   <- Card, fills remaining
│ │                            │   │      space with weight(1f)
│ │  What is the main          │   │
│ │  difference between        │   │
│ │  Agile and Waterfall?      │   │
│ │                            │   │
│ └────────────────────────────┘   │
│                                  │
│    ┌────────────────────────┐    │
│    │      Show Answer       │    │   <- Button, full width
│    └────────────────────────┘    │
│                                  │
│  ┌─────────────┐ ┌────────────┐  │
│  │  Previous   │ │    Next    │  │   <- Row, each weight(1f)
│  └─────────────┘ └────────────┘  │      ends disabled at
│                                  │      the first and last card
└──────────────────────────────────┘
```

When the card is flipped, the same `Card` rotates 180° and shows the `ANSWER` label with the answer text. When the student has no cards in the subject, the card area is replaced by a centered `Box`:

```
┌──────────────────────────────────┐
│                                  │
│                                  │
│   No flashcards assigned for     │   <- Box, centered
│   this subject yet.              │
│                                  │
│                                  │
└──────────────────────────────────┘
```

### 5.5 Admin Panel (`admin`)

```
┌──────────────────────────────────┐
│  Admin Panel                     │   <- Text, 24sp bold
│                                  │
│ ┌──────────────────────────────┐ │
│ │ Create New Flashcard         │ │   <- Card holding the form
│ │                              │ │
│ │ Select Subject:              │ │
│ │  (o) Project Management      │ │   <- RadioButton list
│ │  ( ) Application Development │ │
│ │  ( ) Mobile Development      │ │
│ │  ( ) English Proficiency     │ │
│ │                              │ │
│ │ ┌──────────────────────────┐ │ │
│ │ │ Question                 │ │ │   <- OutlinedTextField
│ │ └──────────────────────────┘ │ │
│ │ ┌──────────────────────────┐ │ │
│ │ │ Answer                   │ │ │   <- OutlinedTextField
│ │ └──────────────────────────┘ │ │
│ │                              │ │
│ │ Assign to Students:          │ │
│ │  [x] Mark                    │ │   <- Checkbox list
│ │  [ ] Juan                    │ │
│ │                              │ │
│ │ ┌──────────────────────────┐ │ │
│ │ │      Save Flashcard      │ │ │   <- Button, full width
│ │ └──────────────────────────┘ │ │
│ │                              │ │
│ │ ( Cancel Edit )              │ │   <- only while editing
│ └──────────────────────────────┘ │
│                                  │
│  ┌────────────────────────────┐  │
│  │ View All Flashcards (12)   │  │   <- Button, live count
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │          Logout            │  │   <- OutlinedButton
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

When editing, the card title changes to `Edit Flashcard`, the fields are pre-filled from the selected card, the button changes to `Save Changes`, and the `Cancel Edit` button appears.

Any non-administrator who reaches this screen sees this instead:

```
┌──────────────────────────────────┐
│                                  │
│          Access Denied           │   <- Text, 24sp, error colour
│                                  │
│  Administrator privileges are   │
│  required to access this         │
│  dashboard.                      │
│                                  │
│      ┌────────────────────┐      │
│      │    Go to Login     │      │   <- Button
│      └────────────────────┘      │
│                                  │
└──────────────────────────────────┘
```

### 5.6 Flashcard Manager (`admin_card_list`)

```
┌──────────────────────────────────┐
│ Flashcard Manager    [Back to    │   <- Row, title + Button
│                        Form]     │
│                                  │
│ Total Flashcards: 12             │   <- Text, 15sp secondary
│                                  │
│ ┌──────────────────────────────┐ │
│ │ [Project Management]         │ │   <- LazyColumn of Cards
│ │ Q: What is the main          │ │
│ │    difference between        │ │
│ │    Agile and Waterfall?      │ │
│ │ A: Agile is iterative and    │ │
│ │    flexible, while ...       │ │
│ │ Assigned to: Mark            │ │
│ │   [Assign Mark] [Edit] [Del] │ │   <- row of actions
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ [Application Development]    │ │
│ │ Q: What does the Single ...  │ │
│ │ A: A class should have only  │ │
│ │    one reason to change ...  │ │
│ │ Assigned to: Unassigned      │ │
│ │   [Assign Mark] [Edit] [Del] │ │
│ └──────────────────────────────┘ │
│         ... scrolls ...          │
└──────────────────────────────────┘
```

The assign button toggles its own label — `Assign Mark` when the card is not yet assigned to the first student, `Unassign Mark` when it is — and `Delete` is styled with the error colour to mark it as destructive.

---

## 6. Prelim Technical Requirements

Every concept from the Preliminary lessons is applied in the project.

| # | Required concept | Where it is applied | Status |
|---|---|---|---|
| 1 | Jetpack Compose for the user interface | All six screen files are `@Composable` functions. | Done |
| 2 | `Column`, `Row`, and `Box` layouts | `Column` — Login, SignUp, SubjectList, Flashcard, Admin, AdminCardList. `Row` — SubjectList header, Flashcard header and navigation buttons, Admin student rows and buttons, AdminCardList header and action rows. `Box` — Flashcard empty state. | Done |
| 3 | Alignment, spacing, padding, and sizing | `horizontalAlignment` / `verticalAlignment` / `Alignment.CenterHorizontally`; `Arrangement.Center`, `Arrangement.SpaceBetween`, `Arrangement.spacedBy(...)`; `Spacer(Modifier.height(...))`, `Modifier.padding(...)`, `Scaffold` `innerPadding`; `fillMaxSize()`, `fillMaxWidth()`, `weight(1f)`, `contentPadding`. | Done |
| 4 | `Text`, `Button`, and `TextField` components | `Text` on every screen; `Button` and `OutlinedButton` for actions; `OutlinedTextField` for username, password, question, and answer; plus `RadioButton` and `Checkbox` in the admin form. | Done |
| 5 | `Card` and other appropriate Material components | `Card` for the subject entries, the admin form, and the manager rows; `Scaffold` on all six screens; `MaterialTheme` colours and `MaterialTheme.colorScheme.error` for destructive actions. | Done |
| 6 | `LazyColumn` for displaying a collection of items | `SubjectListScreen` for the subjects, `AdminCardListScreen` for the flashcards. | Done |
| 7 | Multiple screens | Six screens, exceeding the required minimum of three. | Done |
| 8 | Navigation Compose | `navigation/AppNavigation.kt` — `rememberNavController`, `NavHost`, `composable(...)`, `startDestination = "login"`. | Done |
| 9 | Passing an identifier or data between screens | Route `flashcards/{subjectId}` declared with `navArgument("subjectId") { type = NavType.StringType }`, read back from `backStackEntry.arguments`. | Done |
| 10 | Compose state and a state-driven user interface | `mutableStateOf` for the current user, the editing id, and every text field; `mutableStateListOf` for the users and the flashcards. The subject count, the flip angle, the button labels, and the card totals are all computed from that state. | Done |
| 11 | `ViewModel` for managing application or screen state | Four ViewModels: `UserViewModel`, `CurrentUserViewModel`, `SubjectViewModel`, `FlashcardViewModel`. Obtained with `viewModel()` in `AppNavigation` and passed down as parameters. | Done |
| 12 | Basic Create, Read, Update, Delete operations | Full CRUD on `Flashcard`, plus registration on `User`. See [Section 7](#7-crud-functionality). | Done |

### 6.1 State Management Approach

State is split by responsibility, which keeps each ViewModel small:

- **`UserViewModel`** owns the `users` list and everything about credentials — login, duplicate-username checking, and registration.
- **`CurrentUserViewModel`** owns only the `User` reference that is currently signed in. It is the single source of truth for the role checks that every screen performs.
- **`SubjectViewModel`** owns the fixed `subjects` list and the lookup of a subject title by id.
- **`FlashcardViewModel`** owns the `flashcards` list plus `editingCardId`, which is how the manager screen tells the admin form which card to load.

The `UserViewModel` login and registration functions return the matching `User` instead of writing to `CurrentUserViewModel` directly. The screen receives the `User` and calls `setCurrentUser(...)`. This keeps the ViewModels independent of each other, so no factory or shared singleton is needed to construct them.

The four ViewModels are created once in `AppNavigation` with `viewModel()`. Because that call is scoped to the activity, every screen receives the same instance, and a new student who registers is immediately visible to the administrator on the same session.

### 6.2 Reactive Data Without Extra Libraries

`users` and `flashcards` are declared as `mutableStateListOf` rather than plain lists. Compose observes reads of these collections during composition, so when a card is added or deleted, any screen showing the collection recomposes on its own. The Subject List card counts and the `Total Flashcards` counter update with no manual refresh call.

---

## 7. CRUD Functionality

### 7.1 Flashcard — the main CRUD object

| Operation | Function | File | Where it is triggered |
|---|---|---|---|
| **Create** | `addFlashcard(subjectId, question, answer, assignedUserIds)` | `viewmodel/FlashcardViewModel.kt` | Admin Panel, `Save Flashcard` |
| **Read** | `flashcards` list | `viewmodel/FlashcardViewModel.kt` | Subject List (counts), Flashcards (study), Flashcard Manager (full list), Admin Panel (live count) |
| **Update** | `editFlashcard(id, subjectId, question, answer, assignedUserIds)` | `viewmodel/FlashcardViewModel.kt` | Admin Panel, `Save Changes` after `Edit` |
| **Delete** | `deleteFlashcard(id)` | `viewmodel/FlashcardViewModel.kt` | Flashcard Manager, `Delete` |
| **Update (assignment)** | `toggleAssignment(id, studentId)` | `viewmodel/FlashcardViewModel.kt` | Flashcard Manager, `Assign` / `Unassign` |

### 7.2 User

| Operation | Function | File | Where it is triggered |
|---|---|---|---|
| **Create** | `registerStudent(username, password)` | `viewmodel/UserViewModel.kt` | Student Registration, `Register` |
| **Read** | `users` list, `getStudentList()` | `viewmodel/UserViewModel.kt` | Admin Panel and Flashcard Manager (student picker and name resolution) |
| **Update** | — | — | Not required in this period |
| **Delete** | — | — | Not required in this period |

### 7.3 Session

| Operation | Function | File | Where it is triggered |
|---|---|---|---|
| **Create** | `setCurrentUser(user)` | `viewmodel/CurrentUserViewModel.kt` | Login and Registration |
| **Read** | `currentUser` | `viewmodel/CurrentUserViewModel.kt` | Every screen, for the name, the role, and the access checks |
| **Update** | — | — | A new login simply replaces the value |
| **Delete** | `logout()` | `viewmodel/CurrentUserViewModel.kt` | `Logout` on Subject List and Admin Panel |

### 7.4 Worked Example — Full CRUD Cycle

1. Sign in as `Admin` / `123`. The application opens the Admin Panel.
2. **Create.** Type a question and an answer, pick a subject with the radio buttons, tick `Mark`, and press `Save Flashcard`. `addFlashcard` builds a `Flashcard` with a generated id and appends it to the list.
3. **Read.** Press `View All Flashcards (13)`. The new card is in the manager list with its subject, question, answer, and `Assigned to: Mark`.
4. **Update.** Press `Edit` on that row. The manager stores the card id in `editingCardId` and navigates back; the form reads the id, loads the old values, and shows `Edit Flashcard` with the fields filled in. Change the answer and press `Save Changes`, and `editFlashcard` rebuilds the card with the same id and the new values.
5. **Delete.** Press `Delete` on the row. `deleteFlashcard` also clears `editingCardId` if that card was open in the form, so the admin is never left editing something that no longer exists.
6. Sign out with `Logout`, sign in as `Mark` / `123`, and open the subject. Only the cards assigned to `Mark` appear, and the count on the subject matches.

---

## 8. Data Storage

As permitted for this period, data is held in **Compose state inside the ViewModels** and is not written to disk.

- `users` and `flashcards` use `mutableStateListOf`, so the data is observable and updates the UI automatically.
- `currentUser` and `editingCardId` use `mutableStateOf`.
- The `subjects` list is immutable (`listOf`) because it is reference material and is never edited.
- The ViewModels are scoped to the activity, so the data survives configuration changes such as a screen rotation.

**Known limitation, stated deliberately.** Because the data is in memory only, it is lost when the process is killed or the application is force-stopped, and it is not shared between devices. This is expected and acceptable for the Preliminary period. Persistent storage is planned for a succeeding period; the ViewModel split already in place means only a new data source needs to be introduced, without changing any screen.

---

## 9. Project Development Process

The work followed the prescribed order.

| Step | Activity | Result |
|---|---|---|
| 1 | Conceptualised an original application idea | Flashcard portal with per-student assignment, chosen because it needs the full concept set and is small enough for the period |
| 2 | Prepared the project proposal | [Section 2](#2-project-proposal) |
| 3 | Identified the main data objects | `User`, `Subject`, `Flashcard`, and the current-user session |
| 4 | Planned the screen flow and wireframes | [Sections 4 and 5](#4-screen-flow) |
| 5 | Created the Android Studio project in Kotlin and Jetpack Compose | Project created with Compose enabled and the Navigation and Lifecycle dependencies added |
| 6 | Implemented the planned user interface | Six screens built with `Column`, `Row`, `Box`, `Card`, `LazyColumn`, and Material 3 components |
| 7 | Implemented navigation between screens | `NavHost` in `AppNavigation.kt` with six routes, role-based start routing, a path argument for `subjectId`, and back-stack clearing on login and logout |
| 8 | Applied Compose state and ViewModel-based state management | Four ViewModels created and split by responsibility; observable state collections |
| 9 | Implemented the required CRUD operations | [Section 7](#7-crud-functionality) |
| 10 | Prepared the project for demonstration and submission | Builds without errors, demo accounts provided, this documentation written |

---

## 10. Prelim Deliverables

| Deliverable | Status | Where |
|---|---|---|
| Project proposal with title, problem statement, proposed solution, target users, main features, and data to be managed | Complete | [Section 2](#2-project-proposal) |
| Screen flow showing the relationship between the screens | Complete | [Section 4](#4-screen-flow) |
| Basic wireframes or screen designs | Complete — six wireframes plus the empty state and the access denied state | [Section 5](#5-wireframes) |
| Working Android Studio project | Complete — `assembleDebug` succeeds with no errors | [Appendix A](#appendix-a--project-file-structure) |
| Demonstration of the implemented application | Ready to run; walkthrough script in [Section 7.4](#74-worked-example--full-crud-cycle) | [Appendix B](#appendix-b--how-to-build-and-run) |

---

## 11. Project Guidelines Compliance

| Guideline | How the project meets it |
|---|---|
| The application concept must be the student's own work. | The concept, the roles, the per-student assignment model, and all written content are original to this project. The seed questions were written for this application. |
| The project should be realistic and manageable. | Six screens, three data objects, no networking, no third-party libraries, and no feature that goes beyond the Preliminary lessons. |
| The application should reflect the understanding of the concepts taught. | Each required concept is used in a real place rather than demonstrated in isolation — see the mapping table in [Section 6](#6-prelim-technical-requirements). |
| The interface should be functional and organized. | Consistent `Scaffold` and spacing on every screen, one clear purpose per screen, and a `LazyColumn` for both collections. Design is present but deliberately kept simple. |
| Students should keep their project files organized. | Files are grouped into `models`, `viewmodel`, `screens`, `navigation`, and `ui/theme`, with one class per file and a package matching each folder. See [Appendix A](#appendix-a--project-file-structure). |

---

## 12. Continuation of the Project

This is version one, not a finished product. The concept was chosen with room to grow, and the following are planned for the succeeding periods.

| Planned addition | Purpose | Why the current structure supports it |
|---|---|---|
| **Persistent storage** (Room database) | Survive app restarts and keep real accounts | The four ViewModels are the only place data is created or changed, so a database can be introduced behind them without touching a screen. |
| **Real authentication** | Remove the hardcoded demo accounts and plain-text passwords | The credential checks already sit alone inside `UserViewModel`, behind `login` and `registerStudent`. |
| **Quiz mode** | Answer from a multiple-choice list and be scored | The Flashcards screen already has the card, the answer, and an index to advance. |
| **Spaced repetition** | Resurface cards the student is weak on | Each card already knows which students it is assigned to, so per-student progress can be recorded against it. |
| **Progress statistics** | Show a student which subjects they have covered | The per-subject card count on the Subject List is the first version of this. |
| **Instructor and student roles beyond a single admin** | Multiple instructors, per-subject ownership | The `role` field is already a plain string on `User`. |
| **Search and filtering** in the Flashcard Manager | Find a card in a long list | The manager is already a `LazyColumn` over the full collection. |

---

## 13. Expected Prelim Output

By the end of the period the project is a working prototype that demonstrates:

- the planned screens — six of them, each with a single clear purpose
- functional navigation between those screens, including role-based routing and back-stack handling
- appropriate user input, with validation messages shown in the interface
- state management using Compose state and four purpose-built ViewModels
- full Create, Read, Update, and Delete on flashcards, plus registration on users
- an identifier passed between screens through a typed Navigation Compose argument

The application is not a commercial product, and it is not intended to be. Its purpose at this stage is to show that an original concept has been translated into a working Jetpack Compose application using the concepts taught in class.

---

# Appendix A — Project File Structure

```
PrelimProject/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/prelimproject/
│           │   ├── MainActivity.kt              the only Activity; opens the theme and AppNavigation
│           │   ├── models/
│           │   │   ├── User.kt                  id, username, password, role
│           │   │   ├── Subject.kt               id, title, description
│           │   │   └── Flashcard.kt             id, subjectId, question, answer, assignedUserIds
│           │   ├── viewmodel/
│           │   │   ├── UserViewModel.kt         users, login, isUsernameTaken, registerStudent
│           │   │   ├── CurrentUserViewModel.kt  currentUser, setCurrentUser, logout
│           │   │   ├── SubjectViewModel.kt      subjects, getSubjectTitle
│           │   │   └── FlashcardViewModel.kt    flashcards, editingCardId, add, edit, delete, toggle
│           │   ├── screens/
│           │   │   ├── LoginScreen.kt           screen 1 - login
│           │   │   ├── SignUpScreen.kt          screen 2 - registration
│           │   │   ├── SubjectListScreen.kt     screen 3 - subject browsing
│           │   │   ├── FlashcardScreen.kt       screen 4 - study mode
│           │   │   ├── AdminScreen.kt           screen 5 - create and edit a card
│           │   │   └── AdminCardListScreen.kt   screen 6 - manage all cards
│           │   ├── navigation/
│           │   │   └── AppNavigation.kt         NavHost, the six routes, the four view models
│           │   └── ui/theme/
│           │       ├── Color.kt                 template colour values
│           │       ├── Theme.kt                 PrelimProjectTheme
│           │       └── Type.kt                  typography
│           └── res/                             layouts, icons, strings, themes
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

**Design rules followed in the layout**

- One class per file, and the file is named after the class.
- The package declaration always matches the folder the file is in.
- `MainActivity` contains no `@Composable` function; navigation lives in `navigation/`.
- Screens receive their state as parameters and never reach for a ViewModel themselves, which keeps every screen previewable and testable.
- A screen imports only the ViewModels and models it actually uses.

---

# Appendix B — How to Build and Run

**Requirements**

- Android Studio (Ladybug or newer)
- JDK 17 or newer
- Android SDK with API 37 available

**Build from Android Studio**

1. `File > Open` and select the `PrelimProject` folder.
2. Wait for Gradle sync to finish.
3. Select the `app` run configuration.
4. Press **Run**, or `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

**Build from the command line**

```
gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

**Verify only the Kotlin sources compile**

```
gradlew.bat :app:compileDebugKotlin
```

**Run the tests**

Test sources were removed from the project, so there is no `test` task. Use `compileDebugKotlin` or `assembleDebug` to verify the build.

**Install on a connected device**

```
gradlew.bat installDebug
```

---

# Appendix C — Demo Accounts

| Role | Username | Password | Lands on |
|---|---|---|---|
| Administrator | `Admin` | `123` | Admin Panel |
| Student | `Mark` | `123` | Subject List |

The Login screen displays these on screen so they are easy to find during a demonstration. The administrator account is not included in the student picker, so it never appears as an assignment target. A new account created through **Create Student Account** is added to the same list and is immediately assignable.

### Suggested demonstration order

1. Open the app on the Login screen and point out the two demo accounts.
2. Sign in as `Mark` / `123` — observe that the subject list shows `3 cards assigned` per subject.
3. Tap **Project Management** — show `Card 1 of 3`, press **Show Answer** to demonstrate the flip, then use **Next** and **Previous**.
4. Go back, tap **Logout**, and sign in as `Admin` / `123` — observe the **Admin** button that Mark did not have.
5. On the Subject List, show the different count, `3 total cards`, then enter the Admin Panel.
6. Create a card with a new question, assign it to Mark, and save.
7. Open **View All Flashcards (13)** to show the manager list and the live count.
8. **Edit** the card, change the answer, and **Save Changes**.
9. Use **Assign / Unassign Mark** to show assignment changing from the manager.
10. **Delete** the card and show the count return to 12.
11. Sign out, sign back in as `Mark`, and show that the new card is gone and the counts are back to three.
12. Optionally register a brand new student to show that the new account becomes assignable immediately.
