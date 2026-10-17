# FlashIQ — AI Agent Baseline

> Single source of truth for any AI agent working on this repo.
> Read this file first before inspecting code, refactoring, or adding features.

## 1. What this app is

**FlashIQ — Student Flashcard Portal.** Android study app with two roles:

- **Student:** signs in, browses subjects, flips through only the flashcards assigned to them.
- **Admin (teacher):** writes flashcards (question + answer + subject), ticks which students receive each card, edits/deletes cards, toggles assignment per card.

Core idea: one shared pool of cards, but **per-card `assignedUserIds` turns it into a personal study set per student.**

Package: `com.example.prelimproject`
Entry point: `MainActivity.kt` → `PrelimProjectTheme` → `navigation/AppNavigation.kt`

Demo accounts (seeded in `UserViewModel`):
| Role | Username | Password | Lands on |
|---|---|---|---|
| ADMIN | `Admin` | `123` | `admin` |
| STUDENT | `Mark` | `123` | `subjects` |

## 2. Tech stack

- Kotlin, Jetpack Compose + Material 3, single `ComponentActivity`
- Navigation Compose (`rememberNavController`, `NavHost`, typed `navArgument`)
- Lifecycle ViewModels obtained via `viewModel()` (activity-scoped, shared across screens)
- Compose state only: `mutableStateOf` + `mutableStateListOf`. **No Room, no DataStore, no network, no DI, no third-party libs.**
- Build: `minSdk 24`, `targetSdk 37`, `compileSdk 37`, Java 11, Compose enabled
- Key deps (`app/build.gradle.kts`): `androidx.compose.bom`, `activity-compose`, `navigation-compose`, `lifecycle-viewmodel-compose`, `material3`, `material-icons-extended`, `core-ktx`, `lifecycle-runtime-ktx`

## 3. File map (where to look)

```
app/src/main/java/com/example/prelimproject/
├── MainActivity.kt                  # only Activity; theme + AppNavigation, no @Composable here
├── models/
│   ├── User.kt                      # id, username, password, role ("ADMIN" | "STUDENT")
│   ├── Subject.kt                   # id, title, description
│   └── Flashcard.kt                 # id, subjectId, question, answer, assignedUserIds: List<String>
├── viewmodel/
│   ├── UserViewModel.kt             # users list, login(), isUsernameTaken(), registerStudent(), getStudentList()
│   ├── CurrentUserViewModel.kt      # currentUser: MutableState<User?>, setCurrentUser(), logout()
│   ├── SubjectViewModel.kt          # fixed subjects list (4 items), getSubjectTitle()
│   └── FlashcardViewModel.kt        # flashcards list (12 seeds), editingCardId, add/edit/delete/toggle/clearEditing()
├── screens/                         # pure @Composable, receive ViewModels + lambdas as params
│   ├── LoginScreen.kt               # route "login"
│   ├── SignUpScreen.kt              # route "signup"
│   ├── SubjectListScreen.kt         # route "subjects"
│   ├── FlashcardScreen.kt           # route "flashcards/{subjectId}"
│   ├── AdminScreen.kt               # route "admin" (admin-only, Access Denied otherwise)
│   └── AdminCardListScreen.kt       # route "admin_card_list" (admin-only)
├── navigation/
│   └── AppNavigation.kt             # NavHost, all 6 routes, creates the 4 ViewModels ONCE, passes them down
└── ui/theme/                        # Color.kt, Theme.kt (PrelimProjectTheme), Type.kt (template defaults)
```

Other relevant files: `DOCUMENTATION.md` (school prelim submission doc with wireframes — background reading, not agent instructions), `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`.

## 4. Data model & relationships

```kotlin
User(id: String, username: String, password: String, role: String) // role = "ADMIN" or "STUDENT"
Subject(id: String, title: String, description: String)            // fixed, ids "1".."4"
Flashcard(id: String, subjectId: String, question: String, answer: String, assignedUserIds: List<String>)
```

- `Flashcard.subjectId` → `Subject.id` (many-to-one)
- `Flashcard.assignedUserIds` → list of `User.id` (many-to-many, stored denormalized on the card)
- Session = `CurrentUserViewModel.currentUser` (nullable `User`, in-memory only)

Seed data: 4 subjects (Project Management, Application Development, Mobile Development, English Proficiency), 12 flashcards (3 per subject, all assigned to user id `"2"` = Mark), 2 users (Admin id `"1"`, Mark id `"2"`).

## 5. State management (rules)

1. **ViewModels are created once in `AppNavigation`** with `viewModel()` and passed as parameters. Screens never call `viewModel()` themselves. Keep this pattern — it keeps screens previewable/testable.
2. **`UserViewModel.login/register` return a `User`; they do NOT touch `CurrentUserViewModel`.** The screen calls `currentUserViewModel.setCurrentUser(user)`. Do not couple the ViewModels.
3. `users` and `flashcards` are `mutableStateListOf` → any mutation auto-recomposes every reader. No manual refresh.
4. `editingCardId: MutableState<String>` (`""` = create mode) is how `AdminCardListScreen` (Edit button) hands a card to `AdminScreen` (form). `deleteFlashcard()` clears it if the open card is deleted. `clearEditing()` is called on logout.
5. New ids = `System.currentTimeMillis().toString()`. No UUID, no auto-increment counter.

## 6. Navigation & auth flow

Routes in `AppNavigation.kt`:

| Route | Args | From | Notes |
|---|---|---|---|
| `login` | — | startDestination, logout | `popUpTo("login"){inclusive=true}` after login so back can't return |
| `signup` | — | Login "Create Student Account" | `popBackStack()` to go back; success → `subjects` with `popUpTo("login"){inclusive=true}` |
| `subjects` | — | student login, signup success | subject tap → `flashcards/{subjectId}` (id in path, NOT whole object) |
| `flashcards/{subjectId}` | `subjectId: String` (NavType.StringType) | Subject List | read via `backStackEntry.arguments`; `onBack = popBackStack()` |
| `admin` | — | admin login, "Admin" button on Subject List | gate: `currentUser.role != "ADMIN"` → Access Denied UI |
| `admin_card_list` | — | "View All Flashcards (N)" on Admin Panel | Edit → sets `editingCardId`, `popBackStack()` to form; Back → `popBackStack()` |

- Role routing happens in `LoginScreen` callbacks: `onLoginAdmin` → `admin`, `onLoginStudent` → `subjects`.
- Logout (Subject List + Admin Panel): `logout()` + `clearEditing()` + `navigate("login"){ popUpTo(0) }` — clears entire back stack.
- Admin button on Subject List is only rendered for `role == "ADMIN"`.
- Admin gets `N total cards` per subject; student gets `N cards assigned` (filtered by `assignedUserIds.contains(currentUser.id)`).

## 7. CRUD matrix (exact functions)

`FlashcardViewModel` (`viewmodel/FlashcardViewModel.kt`):
- Create: `addFlashcard(subjectId, question, answer, assignedUserIds)` ← Admin Panel "Save Flashcard"
- Read: `flashcards` list ← Subject counts, Flashcard study screen, Manager list, Admin live count
- Update: `editFlashcard(id, subjectId, question, answer, assignedUserIds)` ← Admin Panel "Save Changes" (rebuilds `Flashcard` with same id, replaces by index)
- Delete: `deleteFlashcard(id)` ← Manager "Delete" (also resets `editingCardId`)
- Assignment toggle: `toggleAssignment(cardId, studentId)` ← Manager "Assign/Unassign" (copies list, adds/removes, rebuilds card)

`UserViewModel`: `login(u, p): User?` (exact string match, case-sensitive — `Admin` ≠ `admin`), `isUsernameTaken(u): Boolean`, `registerStudent(u, p): User` (always role `STUDENT`), `getStudentList(): List<User>` (filters `role == "STUDENT"`, so Admin never appears in picker).

`CurrentUserViewModel`: `setCurrentUser(user)`, `logout()` (sets null). Re-login just overwrites.

`SubjectViewModel`: immutable `listOf(...)`; `getSubjectTitle(id)` returns `"Subject"` fallback.

## 8. Screen behaviors & validation (don't break)

- Login: empty fields and wrong credentials show inline error text; never navigate on failure. Demo-account hint text is rendered on screen.
- SignUp: rejects empty fields and taken usernames (`isUsernameTaken`); on success sets current user and navigates to `subjects`.
- Subject List: header `Hello, {username}` + `Role: {role}`; `LazyColumn` of `Card`s; each card shows title, description, count line (role-dependent); Admin-only "Admin" button; Logout button.
- Flashcards: header Back button + subject title; `Card X of N`; tappable/`Show Answer` flip (180° rotation); Previous/Next with disabled ends; empty state centered `Box`: "No flashcards assigned for this subject yet." Students see filtered list; admin sees all cards in subject.
- Admin Panel: form inside `Card`; subject via `RadioButton` list; question/answer via `OutlinedTextField`; students via `Checkbox` list (from `getStudentList()`); Save validates non-empty question/answer; edit mode changes title to "Edit Flashcard", prefills fields, shows "Save Changes" + "Cancel Edit".
- Manager: `LazyColumn` of cards showing `[Subject]` tag, Q/A, `Assigned to: names or "Unassigned"` (names resolved via users list); per-row Assign-toggle / Edit / Delete (Delete uses error color). Header: title + "Back to Form" + `Total Flashcards: N`.

All screens wrapped in `Scaffold` (inset padding). Login/SignUp use centered `Column`; list screens use full-height `Column` + `LazyColumn`.

## 9. Build / run / verify

```powershell
# from project root (PrelimProject/)
.\gradlew.bat :app:compileDebugKotlin   # compile Kotlin only (fast check)
.\gradlew.bat assembleDebug             # full APK → app\build\outputs\apk\debug\
.\gradlew.bat installDebug              # install on connected device/emulator
```

Note: test sources and test deps were removed (no `src/test`, no `src/androidTest`), so there is no `test` task to run. Template Auto-Backup XMLs were also removed; the manifest keeps only `allowBackup="true"`.

Android Studio: open `PrelimProject/` folder → wait for Gradle sync → Run `app` config. Requires JDK 17+, Android SDK with API 37.

## 10. Conventions for new code

- One class per file, filename = class name, package = folder.
- Screens: `@Composable fun XScreen(viewModels..., callbacks...)` — state in via params, events out via lambdas. No ViewModel instantiation inside screens.
- New shared state → new function in the owning ViewModel, not in the screen. Ownership: users→`UserViewModel`, session→`CurrentUserViewModel`, subjects→`SubjectViewModel`, cards→`FlashcardViewModel`.
- Roles are raw strings `"ADMIN"` / `"STUDENT"` (exact match). If adding roles, centralize to constants/enum — currently scattered as string literals.
- Passwords are plain-text in memory. Known prelim simplification, not a pattern to copy into real auth.
- Keep `Scaffold` + Material 3 components (`Card`, `OutlinedTextField`, `Button`/`OutlinedButton`, `RadioButton`, `Checkbox`).

## 11. Known limitations (intentional, prelim-stage)

- In-memory only: data lost on process kill; no persistence, no multi-device sync. ViewModels survive rotation but not death.
- No real auth: hardcoded seeds, plain-text passwords, no sessions tokens.
- No test sources or test dependencies (removed; template `ExampleUnitTest` / `ExampleInstrumentedTest` deleted).
- Assignment toggle in Manager targets one student at a time; bulk assign lives only in the Admin form checkboxes.
- No search/filter, no quiz scoring, no progress tracking.

## 12. Extension points (where future work goes)

| Goal | Touch only |
|---|---|
| Room persistence | Add DAO/Entity + Repository behind the 4 ViewModels; screens unchanged |
| Real auth | Replace `login`/`registerStudent` internals in `UserViewModel` |
| Quiz mode | Extend `FlashcardScreen` (has card + answer + index already) |
| Spaced repetition / progress | New per-(user,card) progress store keyed off `assignedUserIds`; count line on Subject List is the v1 |
| Search/filter | Filter `flashcards` in `AdminCardListScreen` (already a `LazyColumn`) |
| More roles | Replace `role: String` with enum; update login routing + the two admin gates + `getStudentList()` |

## 13. Suggested smoke test (after any change)

1. Login `Mark/123` → subjects show `3 cards assigned` each → open subject → flip + Next/Previous work.
2. Logout → login `Admin/123` → Admin Panel → create card assigned to Mark → count becomes 13.
3. Manager → Edit → change answer → Save Changes → Assign/Unassign → Delete → count back to 12.
4. Logout → login `Mark/123` → counts back to 3. Register a new student → appears in admin student picker.
