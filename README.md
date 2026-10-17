<div align="center">

# Flash IQ

**A student flashcard portal for Android — teachers build and assign cards, students study exactly what they've been given.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.8.5-3DDC84)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)

</div>

---

## About

Flash IQ is an offline-first Android study app with two roles. Teachers keep one shared pool of flashcards, then **assign each card to specific students**. Students sign in and flip through only the cards assigned to them — so everyone studies from the same material, but each student gets a personal study set.

Everything is persisted locally with **Room (SQLite)**, so data survives app restarts and process death.

| Role | Can do |
| --- | --- |
| **Admin (teacher)** | Create, edit, and delete flashcards; create/rename/delete subjects; add/rename/remove students; assign or unassign a card per student; browse every card |
| **Student** | Browse subjects, see how many cards are assigned, and study them with a flip-card UI |

---

## Features

- **Role-based access** — login routes admins to the admin panel and students to the subject list; admin screens are gated behind a role check.
- **Flashcard CRUD** — create, edit, and delete cards, with a subject dropdown and a searchable student picker (built for ~50+ students).
- **Per-card assignment** — an "Unassign (N)" button expands to show the students currently assigned to a card, with checkboxes to de-select them.
- **Subject & user management** — full create / rename / delete for subjects (deleting a subject cascades to its cards) and users (deleting a user unassigns them everywhere).
- **Study mode** — spring-animated card flip with an animated progress counter, showing only the current student's assigned cards.
- **Material 3 Expressive UI** — dynamic (Material You) color, a Geist-based type scale, tonal color-blocked cards, stadium buttons, and press/enter transitions throughout.
- **Offline-first storage** — Room + Repository + Flow keeps every screen in sync with the database automatically.

---

## Screens

| Screen | Purpose |
| --- | --- |
| `LoginScreen` | Sign in, or jump to sign-up. Demo credentials shown on screen. |
| `SignUpScreen` | Students self-register a new account. |
| `SubjectListScreen` | Subject cards with per-subject card counts (assigned vs. total). |
| `FlashcardScreen` | Study one subject: flip through assigned cards, previous/next. |
| `AdminScreen` | Admin hub — create a flashcard, manage subjects, manage users, or open the card manager. |
| `AdminCardListScreen` | Flashcard Manager — every card, with assignment toggles, edit, and delete. |

---

## Tech Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM `2026.02.01`) + Material 3 |
| Navigation | Navigation Compose 2.8.5 |
| State | AndroidX ViewModel + Kotlin Coroutines / Flow |
| Storage | Room 2.8.5 (SQLite) with KSP |
| Build | AGP 9.4.1, Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |
| SDK | `minSdk 24`, `targetSdk 37`, Java 11 |

---

## Architecture

A clean, one-directional layered architecture. The UI never touches Room; it only ever talks to a ViewModel, which only ever talks to the repository.

```
┌──────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                        │
│  screens/ — pure @Composable, receive ViewModels + callbacks │
└───────────────────────────┬──────────────────────────────────┘
                            │ observes Compose state, calls functions
┌───────────────────────────▼──────────────────────────────────┐
│  ViewModel                                                   │
│  UserViewModel · CurrentUserViewModel · SubjectViewModel ·   │
│  FlashcardViewModel                                          │
│  Collects repository Flows into state; launched from the     │
│  shared repository via *ViewModelFactory.                    │
└───────────────────────────┬──────────────────────────────────┘
                            │ suspend CRUD + Flow<List<…>>
┌───────────────────────────▼──────────────────────────────────┐
│  Repository — data/FlashIQRepository.kt                      │
│  The single access point to the data layer.                  │
└───────────────────────────┬──────────────────────────────────┘
                            │ Room DAO calls
┌───────────────────────────▼──────────────────────────────────┐
│  Data — DAOs + @Entity models + Converters                   │
│  UserDao · SubjectDao · FlashcardDao                         │
└───────────────────────────┬──────────────────────────────────┘
                            │ Room-generated SQL
┌───────────────────────────▼──────────────────────────────────┐
│  Persistence — AppDatabase (RoomDatabase)                    │
│  SQLite file: flashiq_room.db                                │
└──────────────────────────────────────────────────────────────┘
```

`FlashIQApplication` builds the database and repository lazily and seeds the database on first launch. `MainActivity` sets the Compose theme, and `AppNavigation` reads the repository from the Application and creates the ViewModels once, activity-scoped.

---

## Data Model

Three Room entities:

```
User                                    Subject
├─ id          (PK)                     ├─ id          (PK)
├─ username                             ├─ title
├─ password                             ├─ description
├─ role        ADMIN | STUDENT          ├─ color
├─ displayName                          └─ createdAt
├─ email
└─ createdAt

                       Flashcard
                       ├─ id                (PK)
                       ├─ subjectId         → Subject.id
                       ├─ question
                       ├─ answer
                       ├─ assignedUserIds   → User.id  (many-to-many, denormalized)
                       ├─ difficulty
                       ├─ hint
                       └─ createdAt
```

- `Flashcard.subjectId` links many cards to one subject.
- `assignedUserIds` is a `List<String>` stored in the card's own row and (de)serialized by `data/Converters.kt` with a unit-separator delimiter — no join table needed.
- Deleting a subject deletes its cards; deleting a user strips their id from every card.

Full details, including the SQL schema and CRUD matrix, live in [`SCHEMA.md`](SCHEMA.md).

---

## Getting Started

### Requirements

- Android Studio (with the bundled JDK, or JDK 11+)
- Android SDK Platform 37

### Build & run

```bash
# from the project root
./gradlew assembleDebug          # build the debug APK

# or install straight to a connected device / emulator
./gradlew installDebug
```

Then open the project in Android Studio and run the `app` configuration, or launch the installed app from your device.

> The first build needs network access to resolve the Compose and Room artifacts. Later builds work offline with `--offline`.

### Demo account

The database seeds a single admin account on first launch:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `Admin` | `123` |

No students are seeded — create one from **Create Student Account** on the login screen, or from **Manage Users** in the admin panel. The database also seeds 4 subjects and 12 example flashcards.

---

## Project Structure

```
app/src/main/java/com/example/prelimproject/
├─ MainActivity.kt                 # entry point → theme → AppNavigation
├─ FlashIQApplication.kt           # lazy AppDatabase + FlashIQRepository, seeds once
├─ models/                         # Room @Entity data classes
│  ├─ User.kt
│  ├─ Subject.kt
│  └─ Flashcard.kt
├─ data/
│  ├─ AppDatabase.kt               # @Database (entities + DAOs)
│  ├─ UserDao.kt
│  ├─ SubjectDao.kt
│  ├─ FlashcardDao.kt
│  ├─ Converters.kt                # List<String> ↔ delimited TEXT
│  └─ FlashIQRepository.kt         # single access point for the ViewModels
├─ viewmodel/                      # UserViewModel, CurrentUserViewModel,
│                                  # SubjectViewModel, FlashcardViewModel + factories
├─ screens/                        # Login, SignUp, SubjectList, Flashcard,
│                                  # Admin, AdminCardList
├─ navigation/
│  └─ AppNavigation.kt             # routes, transitions, ViewModel wiring
└─ ui/theme/                       # Color, Theme, Type (Geist), Shape, Motion
```

---

## Notes & Limitations

- **Plaintext passwords.** Accounts are stored as-is in the local database — fine for a classroom demo, not for production. A real release would hash credentials and authenticate against a backend.
- **No migrations yet.** The Room schema is at `version = 1`; bumping the version currently drops and recreates the database.
- **Local-only.** There is no server or sync — data lives on the device.
- `assignedUserIds` is denormalized into the `flashcards` row rather than modeled as a join table.

---

## Documentation

- [`DOCUMENTATION.md`](DOCUMENTATION.md) — full project documentation: concept, proposal, screen flow, wireframes, and deliverables.
- [`SCHEMA.md`](SCHEMA.md) — data schema, layered architecture, navigation flow, CRUD matrix, and SQL schema.

---

## License

This is an academic course project. Add a `LICENSE` file if you intend to publish or open-source it.
