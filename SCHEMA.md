================================================================================
                    FLASHIQ - STUDENT FLASHCARD PORTAL
                    APPLICATION SCHEMA (ASCII)
================================================================================
Package : com.example.prelimproject
Platform: Android, minSdk 24, targetSdk 37
Stack   : Kotlin + Jetpack Compose + Material 3 + Navigation Compose
Storage : Room (SQLite) - androidx.room 2.8.5 + KSP
Stage   : Preliminary (data persisted locally in Room/SQLite)

================================================================================
1. DATA SCHEMA (ENTITIES & RELATIONSHIPS)
================================================================================

+----------------------------------+        +----------------------------------+
|              User                |        |            Subject               |
+----------------------------------+        +----------------------------------+
| id          : String             |        | id          : String             |
| username    : String             |        | title       : String             |
| password    : String             |        | description : String             |
| role        : String             |        | color       : String             |
|   "ADMIN" | "STUDENT"           |        | createdAt   : Long               |
| displayName : String             |        +----------------------------------+
| email       : String             |                   ^ 1
| createdAt   : Long               |                   |
+----------------------------------+                   | many-to-one
        | 1                                             |
        |                                               |
        | assignedUserIds (List<String>)                 |
        v many                                          |
+--------------------------------------------------------------------------+
|                               Flashcard                                  |
+--------------------------------------------------------------------------+
| id               : String                                                |
| subjectId        : String   --> Subject.id                                |
| question         : String                                                |
| answer           : String                                                |
| assignedUserIds  : List<String>  --> User.id (many-to-many, denormalized) |
| difficulty       : String   ("Easy"|"Medium"|"Hard")                     |
| hint             : String                                                |
| createdAt        : Long                                                  |
+--------------------------------------------------------------------------+

+----------------------+
|   Session (current)  |
+----------------------+
| currentUser : User?  |   <- held in CurrentUserViewModel
| (in-memory only)     |   <- created on login/signup, cleared on logout
+----------------------+

RELATIONSHIPS
  Flashcard.subjectId  --> Subject.id          (many Flashcards : 1 Subject)
  Flashcard.assignedUserIds[] --> User.id      (many Flashcards : many Users)
  Session.currentUser --> User                  (1 Session : 1 User or null)

GRANULARITY
  User       = @Entity "users"
  Subject    = @Entity "subjects"
  Flashcard  = @Entity "flashcards"
  assignedUserIds is stored inside the flashcards row (see Converters).

SEED DATA (written once, when the Room database is first created)
  Users     : Admin(id="1", ADMIN)             <- "Mark" was removed
  Subjects  : 4 fixed (ids "1".."4")
  Flashcards: 12 (3 per subject, all assignedUserIds = [])

PERSISTENCE
  Every mutation goes through FlashIQRepository to the Room DAOs, so data
  survives app restarts and process death. Reads are exposed as Flow<List<..>>
  and collected by the ViewModels, which mirror them into Compose state.

================================================================================
2. LAYER / ARCHITECTURE SCHEMA
================================================================================

+---------------------------------------------------------------------+
|                          UI LAYER (Compose)                         |
|  screens/ : LoginScreen, SignUpScreen, SubjectListScreen,            |
|             FlashcardScreen, AdminScreen, AdminCardListScreen        |
|  (pure @Composable; receive ViewModels + callbacks as params)        |
+----------------------------------+----------------------------------+
                                   | reads state / calls functions
                                   v
+---------------------------------------------------------------------+
|                       VIEWMODEL LAYER (activity-scoped)             |
|  +----------------+  +-------------------+  +-------------------+    |
|  | UserViewModel  |  | CurrentUserVM     |  | SubjectViewModel  |    |
|  | users list     |  | currentUser       |  | subjects          |    |
|  | (mirrors Flow) |  | (session only)    |  | (mirrors Flow)    |    |
|  | login()        |  | setCurrentUser()  |  | getSubjectTitle()  |    |
|  | isUsernameTaken|  | logout()          |  +-------------------+    |
|  | registerStudent|  +-------------------+                           |
|  | getStudentList |                                                  |
|  +----------------+  +-------------------------------------------+  |
|                      | FlashcardViewModel                        |  |
|                      | flashcards (mirrors Flow), editingCardId  |  |
|                      | add / edit / delete / toggle              |  |
|                      +-------------------------------------------+  |
|  Built by *ViewModelFactory(repository); CurrentUserViewModel has no repo|
+----------------------------------+----------------------------------+
                                   | Compose state (mutableStateListOf /
                                   | mutableStateOf) mirrored from the
                                   | repository Flow inside viewModelScope
                                   v
+---------------------------------------------------------------------+
|                    REPOSITORY LAYER (data/FlashIQRepository.kt)     |
|  users / subjects / flashcards : Flow<List<..>>                     |
|  suspend insert / update / delete / toggleAssignment / seed()       |
|  (the only class the ViewModels talk to)                            |
+----------------------------------+----------------------------------+
                                   | calls DAO suspend funs
                                   v
+---------------------------------------------------------------------+
|                    DATA LAYER (Room DAOs + entities)                |
|  data/UserDao.kt, SubjectDao.kt, FlashcardDao.kt                    |
|  models/User.kt, Subject.kt, Flashcard.kt  (@Entity data classes)   |
|  data/Converters.kt  (List<String> <-> delimited TEXT)              |
+----------------------------------+----------------------------------+
                                   | Room generates the SQL at build
                                   v
+---------------------------------------------------------------------+
|               PERSISTENCE LAYER (AppDatabase : RoomDatabase)        |
|  data/AppDatabase.kt  -> SQLite file "flashiq_room.db"              |
|  Built once in FlashIQApplication (lazy); seeds on first launch     |
+---------------------------------------------------------------------+

ENTRY POINT
  AndroidManifest sets android:name=".FlashIQApplication".
  FlashIQApplication -> lazy AppDatabase + lazy FlashIQRepository.
  MainActivity.kt -> PrelimProjectTheme -> navigation/AppNavigation.kt
  AppNavigation reads the repository from the Application and creates the
  ViewModels ONCE via viewModel(factory = ..) and passes them to each screen.

================================================================================
3. NAVIGATION / SCREEN FLOW SCHEMA
================================================================================

                          +-------------------+
                          |   1. LOGIN        |  <- start destination
                          |   (all users)     |
                          +---------+---------+
                                    |
            +-----------------------+-----------------------+
            |                       |                       |
   "Create Student Account"  admin logs in         student logs in
            |                       |                       |
            v                       v                       v
     +-------------+        +--------------+        +----------------+
     | 2. SIGN UP  |        | 5. ADMIN     |        | 3. SUBJECT     |
     |             |        |    PANEL     |        |    LIST        |
     +------+------+        +------+-------+        +--------+-------+
            |                      |                        |
   "Back to Login"        "View All Flashcards"     tap a subject
            |                      |                        |
            +-----------+          v                        v
                        |   +----------------------+  +------------------+
                        |   | 6. FLASHCARD MANAGER  |  | 4. FLASHCARDS    |
                        |   +----------+-----------+  | (one subject)    |
                        |              |               +--------+---------+
                        |   "Edit"     |  "Back to Form"           |
                        |       v      +---------------------------+----+
                        |  returns to Admin Panel                     |
                        |  with card loaded for editing                |
                        |                                             |
                        +--- Logout from 3 or 5 -> returns to 1. LOGIN-+

ROUTES (AppNavigation.kt)
  login            -> (start; cleared from back stack after login)
  signup           -> from Login
  subjects         -> student login / signup success
  flashcards/{subjectId}  -> Subject List (subjectId: String, NavType.StringType)
  admin            -> admin login / "Admin" button on Subject List
  admin_card_list  -> "View All Flashcards (N)" on Admin Panel

BACK-STACK RULES
  - Login clears itself: popUpTo("login"){inclusive=true}
  - Logout clears whole stack: popUpTo(0)
  - Flashcards / Manager use popBackStack()

================================================================================
4. STATE MANAGEMENT SCHEMA
================================================================================

State split by responsibility (one ViewModel per concern):

  UserViewModel  (built by UserViewModelFactory(repository))
    +-- users : mutableStateListOf<User>   (mirrors repository.users Flow)
    +-- login(u,p) : User?
    +-- isUsernameTaken(u) : Boolean
    +-- registerStudent(u,p) : User
    +-- addUser(u,p) : User?
    +-- renameUser(id,name) / updateUser(...)
    +-- removeUser(id)                     (guard: never id == "1")
    +-- getStudentList() : List<User>      (role == "STUDENT" only)

  CurrentUserViewModel  (no database)
    +-- currentUser : MutableState<User?>   (mutableStateOf, session only)
    +-- setCurrentUser(user)
    +-- logout()                            (sets null)

  SubjectViewModel  (built by SubjectViewModelFactory(repository))
    +-- subjects : mutableStateListOf<Subject>   (mirrors repository Flow)
    +-- getSubjectTitle(id) : String
    +-- addSubject / renameSubject / updateSubject
    +-- removeSubject(id)                  (cascades to its flashcards)

  FlashcardViewModel  (built by FlashcardViewModelFactory(repository))
    +-- flashcards : mutableStateListOf<Flashcard>   (mirrors repository Flow)
    +-- editingCardId : MutableState<String>        ("" = create mode)
    +-- addFlashcard(subjectId, question, answer, assignedUserIds)
    +-- editFlashcard(id, subjectId, question, answer, assignedUserIds)
    +-- deleteFlashcard(id)                 (also clears editingCardId)
    +-- toggleAssignment(cardId, studentId)
    +-- removeUserFromAssignments(userId)   (sync after user delete)
    +-- clearEditing()

RULES
  - ViewModels created once in AppNavigation via viewModel(factory = ...)
    (activity-scoped); the factory injects the shared FlashIQRepository.
  - ViewModels talk only to FlashIQRepository, never to DAOs/AppDatabase.
  - Repository reads are Flow<List<..>>; each ViewModel collects its Flow in
    viewModelScope and mirrors it into a mutableStateListOf, which the screens
    observe (so screens never need to know about Room).
  - Mutations are suspend repository calls launched in viewModelScope; the UI
    updates when Room re-emits the changed table.
  - login/register return a User immediately; screens call setCurrentUser(...).
  - New ids = System.currentTimeMillis().toString().

================================================================================
5. CRUD MATRIX
================================================================================

Flashcard (main CRUD object)
  Create  -> FlashcardViewModel.addFlashcard(...)        (Admin Panel, Save)
  Read    -> flashcards list                             (Subject counts, Study,
                                                         Manager, Admin live count)
  Update  -> FlashcardViewModel.editFlashcard(...)       (Admin Panel, Save Changes)
  Delete  -> FlashcardViewModel.deleteFlashcard(...)     (Manager, Delete)
  Assign  -> FlashcardViewModel.toggleAssignment(...)    (Manager, Assign/Unassign)

User
  Create  -> UserViewModel.addUser(...) / registerStudent(...)   (Admin Panel, SignUp)
  Read    -> users list, getStudentList()                        (Admin Panel, Manager)
  Update  -> UserViewModel.renameUser(...) / updateUser(...)     (Admin Panel)
  Delete  -> UserViewModel.removeUser(...)                      (Admin Panel)

Subject
  Create  -> SubjectViewModel.addSubject(...)                   (Admin Panel)
  Read    -> subjects list, getSubjectTitle(...)                (Subject List, Admin)
  Update  -> SubjectViewModel.renameSubject(...) / updateSubject(...) (Admin Panel)
  Delete  -> SubjectViewModel.removeSubject(...)                (Admin Panel)

Session (current user)
  Create  -> CurrentUserViewModel.setCurrentUser(...)   (Login, SignUp)
  Read    -> currentUser                               (every screen)
  Update  -> replaced on new login
  Delete  -> CurrentUserViewModel.logout()              (Logout buttons)

================================================================================
5b. SQL SCHEMA (Room / SQLite: flashiq_room.db, version 1)
================================================================================
Tables are generated by Room from the @Entity data classes.

users
  id          TEXT PRIMARY KEY
  username    TEXT NOT NULL
  password    TEXT NOT NULL
  role        TEXT NOT NULL            -- "ADMIN" | "STUDENT"
  displayName TEXT NOT NULL
  email       TEXT NOT NULL
  createdAt   INTEGER NOT NULL

subjects
  id          TEXT PRIMARY KEY
  title       TEXT NOT NULL
  description TEXT NOT NULL
  color       TEXT NOT NULL
  createdAt   INTEGER NOT NULL

flashcards
  id              TEXT PRIMARY KEY
  subjectId       TEXT NOT NULL        -- -> subjects.id
  question        TEXT NOT NULL
  answer          TEXT NOT NULL
  assignedUserIds TEXT NOT NULL        -- List<String> via Converters,
                                       -- unit-separator (0x1F) delimited
  difficulty      TEXT NOT NULL        -- "Easy" | "Medium" | "Hard"
  hint            TEXT NOT NULL
  createdAt       INTEGER NOT NULL

INTEGRITY
  - No SQL foreign keys / join table: assignedUserIds is denormalized into the
    flashcards row and (de)serialized by data/Converters.kt.
  - Deleting a subject deletes its flashcards in FlashIQRepository
    (deleteSubjectWithCards) and the flashcard Flow re-emits.
  - Deleting a user strips that id from every card (removeUserFromAssignments).
  - Version changes drop + recreate the DB (dev-stage; no migrations yet).

================================================================================
6. FILE / PROJECT STRUCTURE SCHEMA
================================================================================

PrelimProject/
+-- app/
|   +-- src/main/
|   |   +-- java/com/example/prelimproject/
|   |   |   +-- MainActivity.kt
|   |   |   +-- FlashIQApplication.kt    (lazy AppDatabase + Repository)
|   |   |   +-- models/                  (Room @Entity data classes)
|   |   |   |   +-- User.kt
|   |   |   |   +-- Subject.kt
|   |   |   |   +-- Flashcard.kt
|   |   |   +-- data/
|   |   |   |   +-- AppDatabase.kt       (RoomDatabase: entities + DAOs)
|   |   |   |   +-- UserDao.kt
|   |   |   |   +-- SubjectDao.kt
|   |   |   |   +-- FlashcardDao.kt
|   |   |   |   +-- Converters.kt        (List<String> <-> TEXT)
|   |   |   |   +-- FlashIQRepository.kt (single access point for VMs)
|   |   |   +-- viewmodel/
|   |   |   |   +-- UserViewModel.kt
|   |   |   |   +-- UserViewModelFactory.kt
|   |   |   |   +-- CurrentUserViewModel.kt
|   |   |   |   +-- SubjectViewModel.kt
|   |   |   |   +-- SubjectViewModelFactory.kt
|   |   |   |   +-- FlashcardViewModel.kt
|   |   |   |   +-- FlashcardViewModelFactory.kt
|   |   |   +-- screens/
|   |   |   |   +-- LoginScreen.kt
|   |   |   |   +-- SignUpScreen.kt
|   |   |   |   +-- SubjectListScreen.kt
|   |   |   |   +-- FlashcardScreen.kt
|   |   |   |   +-- AdminScreen.kt
|   |   |   |   +-- AdminCardListScreen.kt
|   |   |   +-- navigation/
|   |   |   |   +-- AppNavigation.kt
|   |   |   +-- ui/theme/
|   |   |       +-- Color.kt
|   |   |       +-- Theme.kt
|   |   |       +-- Type.kt
|   |   +-- res/          (layouts, icons, strings, themes)
|   +-- build.gradle.kts         (applies KSP; Room deps)
+-- settings.gradle.kts
+-- gradle.properties            (android.disallowKotlinSourceSets=false)
+-- gradle/libs.versions.toml    (room 2.8.5, ksp 2.2.10-2.0.2)

================================================================================
7. ACCESS CONTROL SCHEMA
================================================================================

  Role "ADMIN"  -> Login routes to "admin"; sees Admin button on Subject List;
                   sees "N total cards" per subject; can create/edit/delete/assign.
  Role "STUDENT"-> Login routes to "subjects"; sees "N cards assigned" per subject;
                   sees only cards where assignedUserIds contains currentUser.id.

  Gate: AdminScreen / AdminCardListScreen check currentUser.role != "ADMIN"
        -> render "Access Denied" UI with "Go to Login" button.

================================================================================
                              END OF SCHEMA
================================================================================
