# Flash IQ

A student flashcard portal for Android. Teachers keep one shared pool of flashcards and assign each card to specific students; students sign in and study only the cards assigned to them. Everything is stored locally with Room, so the data survives app restarts.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)
![Room](https://img.shields.io/badge/Room-2.8.5-3DDC84)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)

## Features

- **Two roles** — admins manage content, students study it.
- **Flashcard CRUD** — create, edit, and delete cards, with a subject dropdown and a searchable student picker.
- **Per-student assignment** — an "Unassign (N)" button expands to show who a card is assigned to, with checkboxes to remove them.
- **Subject management** — create, rename, and delete subjects. Deleting a subject deletes its cards.
- **User management** — create, rename, and remove student accounts. Deleting a user unassigns them from every card.
- **Study mode** — an animated card flip with a progress counter, showing only the current student's cards.
- **Material 3** — dynamic (Material You) color, a Geist-based type scale, tonal cards, and press/enter transitions.
- **Offline-first** — Room + Repository + Flow keeps every screen in sync with the database.

## Screens

| Screen | Purpose |
| --- | --- |
| Login | Sign in, or open sign-up. Demo credentials are shown on screen. |
| Sign Up | A student registers a new account. |
| Subject List | Subject cards with assigned / total card counts. |
| Flashcards | Study one subject: flip through the assigned cards. |
| Admin Panel | Hub for creating flashcards, managing subjects, and managing users. |
| Flashcard Manager | Every card, with assignment toggles, edit, and delete. |

## Tech Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01) + Material 3 |
| Navigation | Navigation Compose 2.8.5 |
| State | AndroidX ViewModel + Coroutines / Flow |
| Storage | Room 2.8.5 (SQLite) with KSP |
| Build | AGP 9.4.1, Gradle Kotlin DSL, version catalog |
| SDK | minSdk 24, targetSdk 37, Java 11 |

## Architecture

The app is split into four layers, and dependencies only point one way: the UI never touches the database, it only talks to a ViewModel, and the ViewModels only talk to the repository.

```
UI (screens/)  ->  ViewModel  ->  FlashIQRepository  ->  DAOs  ->  Room (SQLite)
```

- `screens/` — pure composables. They receive their ViewModels and navigation callbacks as parameters.
- `viewmodel/` — one ViewModel per concern (`User`, `Subject`, `Flashcard`, plus `CurrentUser` for the session). Each collects a repository `Flow` and mirrors it into Compose state; each is built by a `*ViewModelFactory` that injects the shared repository.
- `data/FlashIQRepository.kt` — the single access point to the data layer. Exposes read `Flow`s and `suspend` CRUD functions.
- `data/*Dao.kt` + `models/` — Room DAOs and `@Entity` data classes.

`FlashIQApplication` creates the database and repository lazily and seeds them on first launch. `MainActivity` applies the theme and hands off to `AppNavigation`, which creates the ViewModels once and wires up the routes.

## Data Model

Three Room entities, stored in `flashiq_room.db`:

- **User** — `id`, `username`, `password`, `role` (`ADMIN` or `STUDENT`), `displayName`, `email`, `createdAt`.
- **Subject** — `id`, `title`, `description`, `color`, `createdAt`.
- **Flashcard** — `id`, `subjectId` (points to a subject), `question`, `answer`, `assignedUserIds`, `difficulty`, `hint`, `createdAt`.

`assignedUserIds` is a `List<String>` stored inside the card's own row and de/serialized by `data/Converters.kt`, so no join table is needed. Deleting a subject deletes its cards, and deleting a user strips their id from every card.

See [SCHEMA.md](SCHEMA.md) for the full schema, SQL tables, and CRUD matrix.

## Getting Started

Requirements: Android Studio (with its bundled JDK, or JDK 11+) and Android SDK Platform 37.

Build a debug APK:

```bash
./gradlew assembleDebug
```

Or install straight to a connected device or emulator:

```bash
./gradlew installDebug
```

You can also open the project in Android Studio and run the `app` configuration.

The first build needs network access to resolve the Compose and Room artifacts; later builds work offline with `--offline`.

### Demo account

The database seeds one admin account on first launch:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `Admin` | `123` |

No students are seeded. Create one via **Create Student Account** on the login screen, or from **Manage Users** in the admin panel. Four subjects and twelve example flashcards are also seeded.

## Project Structure

```
app/src/main/java/com/example/prelimproject/
├── MainActivity.kt          entry point -> theme -> AppNavigation
├── FlashIQApplication.kt    lazy AppDatabase + repository, seeds once
├── models/                  Room entities: User, Subject, Flashcard
├── data/                    AppDatabase, DAOs, Converters, FlashIQRepository
├── viewmodel/               ViewModels + factories
├── screens/                 Login, SignUp, SubjectList, Flashcard, Admin, AdminCardList
├── navigation/              AppNavigation: routes and ViewModel wiring
└── ui/theme/                Color, Theme, Type (Geist), Shape, Motion
```

## Limitations

- **Plaintext passwords.** Credentials are stored as-is in the local database — fine for a classroom demo, not for production.
- **No migrations.** The schema is at version 1; bumping the version currently drops and recreates the database.
- **Local-only.** There is no server or sync; data lives on the device.
- `assignedUserIds` is denormalized into the `flashcards` row rather than modeled as a join table.

## Documentation

- [DOCUMENTATION.md](DOCUMENTATION.md) — project concept, proposal, screen flow, and wireframes.
- [SCHEMA.md](SCHEMA.md) — data schema, architecture, navigation flow, and CRUD matrix.

## License

This is an academic course project. Add a `LICENSE` file before publishing or open-sourcing it.
