# Flash IQ — Application Guide

Everything you need to explain, demo, and defend this app, in the order you'd actually talk about it.
The data schema is at the very bottom.

---

## 1. What it is

**Flash IQ is a student flashcard portal for Android.** Teachers (admins) maintain one shared pool of
flashcards, organized by subject, and assign individual cards to individual students. Students sign in
and only ever see the cards assigned to them. All data lives in **Cloud Firestore**, so two phones
signed into the same Firebase project see the same content in real time.

**The 30-second pitch:**

> Flash IQ replaces scattered review material with a single shared deck. An administrator writes a
> flashcard once, files it under a subject, and assigns it to whichever students need it. Students open
> the app, pick a subject, and study only their own assigned cards in a flip-card study mode. There are
> two roles, six screens, and no setup for the student beyond creating an account.

**What problem it solves:** in a class, the same review questions get retyped per student, and there's no
way to tell who was given what. Flash IQ keeps one authoritative copy per card and makes assignment a
per-card, per-student decision.

---

## 2. Roles

| Role | Stored as | Can do |
|---|---|---|
| **Admin** (teacher) | `users.role = "ADMIN"` | Everything: subjects, cards, users, assignments, and can study any card |
| **Student** | `users.role = "STUDENT"` | Sign up, sign in, browse subjects, study only cards assigned to them |

There is exactly one seeded admin (`Admin` / `123`). Students are created either by that admin in the
Admin Panel or by themselves on the Sign Up screen. Role checks are done in every screen by reading
`Session.currentUser.role`; a student who navigates to an admin route sees an "Access Denied" panel.

---

## 3. Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01), Material 3 |
| Navigation | Navigation Compose 2.8.5, six routes, one string argument |
| State | Compose `mutableStateOf` / `mutableStateListOf` + coroutines |
| Backend | **Cloud Firestore** (`firebase-firestore` 26.0.0, Firebase BOM 34.0.0) |
| Build | AGP 9.4.1, Gradle 9.6, version catalog (`gradle/libs.versions.toml`) |
| SDK | minSdk 24, target/compileSdk 37, Java 11 |
| Font / theme | Geist type scale, Material You dynamic color on Android 12+ |

Two libraries deserve a note because they're often asked about in a defense:

- **No ViewModels, no repository, no Room.** The previous build used Room + Repository + ViewModels.
  Now screens talk to a single `Cloud` object, and Compose state holds the data. See §5.
- **`kotlinx-coroutines-play-services`** is what allows Firestore's `Task` API to be awaited with
  `.await()` inside `launch { }`, so async code reads top-to-bottom instead of callback chains.

---

## 4. Architecture at a glance

```
MainActivity  (applies theme)
      │
      ▼
AppNavigation  ──►  is Firebase configured? ──no──► "Connect Firebase" screen
      │ yes
      ▼
  seedIfNeeded()  ──►  writes demo data only if `users` is empty
      │
      ▼
  NavHost: login → signup / subjects → flashcards/{subjectId} / admin → admin_card_list
      │
      ▼
 Screens ──reads/writes──►  Cloud (collections + helpers)  ──►  Cloud Firestore
```

The three data pieces:

| File | Responsibility |
|---|---|
| [data/Cloud.kt](app/src/main/java/com/example/prelimproject/data/Cloud.kt) | Firestore instance, the three collection references, `newId()`, one-time seeding, the `liveList` snapshot helper, and Firebase error → message translation |
| [models/Models.kt](app/src/main/java/com/example/prelimproject/models/Models.kt) | `User`, `Subject`, `Flashcard` data classes, the field-name constants (`Keys`), and the `toMap()` / `DocumentSnapshot.toX()` mappers |
| [navigation/AppNavigation.kt](app/src/main/java/com/example/prelimproject/navigation/AppNavigation.kt) | Routes, back-stack rules, edit-card state, the Firebase setup screen, seeding on launch |

**Dependency direction:** UI → `Cloud` → Firestore. There is no layer between the screen and the
database, which is a deliberate simplification for this project; it is also the main thing you'd change
if this grew.

---

## 5. How the data actually flows

### Reads are live, not fetched once

`Cloud.liveList()` is a Compose helper that turns a Firestore **snapshot listener** into a list Compose
can read directly:

```kotlin
val subjects = Cloud.subjects.liveList { it.toSubject() }
```

Under the hood it registers `addSnapshotListener`, rewrites a `mutableStateListOf` whenever the server
(or the local cache) reports a change, and removes the listener in `DisposableEffect`'s `onDispose` when
the screen leaves the composition. This is why the dashboard's "12 cards" counter, the subject card
counts, and the manager list all update without any refresh button.

Ordering is done by the query, not by the UI: every list is ordered by `createdAt` ascending.

### Writes are direct and awaited

Each mutation is a coroutine in the screen:

```kotlin
scope.launch {
    Cloud.flashcards.document(cardId).set(card.toMap()).await()
}
```

- create → `set()` on a new document id
- update → `set()` with the full map (card edit) or `update()` with one field (rename, unassign)
- delete → `delete()`, or a `WriteBatch` when several documents must go together

### Cascades are the app's job

Firestore has no foreign keys, so the two cascades Room used to do automatically are explicit:

- **Delete a subject** → `WriteBatch` deletes the subject *and* every card whose `subjectId` matches.
- **Delete a student** → `WriteBatch` deletes the user *and* `arrayRemove`s their id from every card's
  `assignedUserIds`.

### The session is in memory

`object Session { var currentUser: User? }` holds the signed-in user. It is intentionally not persisted:
killing the app signs you out. This keeps the demo simple and avoids a token store.

### Startup sequence

1. `MainActivity` applies the theme and calls `AppNavigation()`.
2. If `google-services.json` wasn't compiled in, Firebase never initializes — so the app shows the
   **Connect Firebase** screen instead of crashing on the first Firestore call.
3. Otherwise `seedIfNeeded()` runs once: if `users` is empty, it batch-writes the demo admin, 4 subjects
   and 12 flashcards. Existing data is never overwritten.
4. The login screen appears.

---

## 6. The six screens

| Route | Screen | What it does |
|---|---|---|
| `login` | Login | Username + password; queries `users` by username, compares the password, routes by role. Demo credentials `Admin / 123` are shown on screen. |
| `signup` | Sign Up | Rejects a duplicate username, creates a `STUDENT` document, signs the user in. |
| `subjects` | Subject List | Greeting + role, subject cards with a count. Admins see a total count and an **Admin** button; students see the number of cards assigned to them. |
| `flashcards/{subjectId}` | Flashcards (study) | Card flip study mode for one subject: progress counter, tap-to-flip 3D rotation, Previous/Next. Admins see every card; students only their assigned ones. |
| `admin` | Admin Panel | Hub with four entries (create card, manage subjects, manage users, view all cards) and Logout. Also hosts the create/edit flashcard form with a subject dropdown, question/answer fields, and a searchable student checklist. |
| `admin_card_list` | Flashcard Manager | Every card with its subject, question, answer, assigned students, plus Unassign, Edit and Delete. |

**Back-stack rules:** login is removed after a successful sign-in, logout clears the whole stack, and
study/manager screens use `popBackStack()` so the hardware back button behaves normally.

**Shared edit state:** "Edit" in the manager stores the card id in `AppNavigation` and pops back to the
admin form, which loads that card's question/answer/subject/assignments. "Back to Form" clears it.

---

## 7. Feature list (for the write-up)

- Two roles with role-gated navigation and an in-screen access check
- Full CRUD on flashcards, subjects, and student accounts
- **Per-card, per-student assignment** via a searchable checklist
- **Unassign panel** in the manager that lists exactly who has a card
- **Study mode** with a flip animation and progress counter
- **Live updates** from Firestore snapshot listeners — no refresh anywhere
- **Cascading deletes** done in a batch (subject → its cards, user → their assignments)
- **Seeding** on first launch so a fresh install is demo-ready
- **Readable error handling**: permission-denied, offline/unreachable, and fallback messages
- **Material You** dynamic color with a custom Geist type scale, tonal "expressive" cards, and
  press/spring scale animation on tappable surfaces
- **Configuration guard**: missing Firebase config shows instructions instead of crashing

---

## 8. Limitations to be honest about

These are worth stating before someone else finds them:

1. **Passwords are stored in plaintext in Firestore and compared on the client.** There is no Firebase
   Authentication. Anyone with the app's config can read `users` if the rules allow it. The rules
   shipped in the setup guide are time-boxed for this reason. Adding Firebase Auth is the correct next
   step and would let the rules deny public reads.
2. **Document ids are `System.currentTimeMillis()`.** Simple and sortable, but two writes in the same
   millisecond could collide. Fine at classroom scale.
3. **`createdAt` is a `Long` (epoch millis)**, not a Firestore `Timestamp`. It sorts correctly and maps
   trivially, but reads as a number in the console.
4. **No image/media support, no spaced repetition, no scoring.** Study mode flips cards; it doesn't track
   performance.
5. **Renames write on every keystroke** (subject name, student username). It matches the original Room
   behavior but is chattier against a billed backend.
6. **Unassign state on the very last card in the manager can render partly below the fold**; the panel is
   reachable by scrolling, but it's a rough edge.
7. **Two devices editing the same card** will last-write-wins; there's no conflict resolution.

---

## 9. Running it

**Requirements:** Android Studio (with its bundled JDK), an Android device or emulator on API 24+,
and — for real data — your own Firebase project.

1. Follow [FIREBASE_SETUP.md](FIREBASE_SETUP.md): create the project, register the Android app with
   package `com.example.prelimproject`, drop `google-services.json` into `app/`, create Firestore, and
   publish the rules.
2. Build:

   ```bash
   ./gradlew assembleDebug
   ```

   Output: `app/build/outputs/apk/debug/app-debug.apk`

   The config is compiled into the APK, so **rebuild after adding the file**.
3. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or press **Run ▶** in Android
   Studio with a device selected.
4. Sign in as `Admin / 123`, or create a student account.

Without `google-services.json` the app still builds and opens — it shows the Connect Firebase screen.

**Local testing without a real project:** the app was verified against the standalone Firestore emulator
(`cloud-firestore-emulator`) with a throwaway config. Two things are needed there: a config whose
`package_name` matches, and reaching the host. `10.0.2.2` did not negotiate gRPC on this setup, but
`adb reverse tcp:8080 tcp:8080` plus `useEmulator("127.0.0.1", 8080)` did. Those hooks were removed
afterwards and must not ship.

---

## 10. Demo script (5 minutes)

1. **Admin sign-in** — `Admin / 123` lands on the Admin Panel. Point out the live card count on
   "View All Flashcards".
2. **Create a subject** — Manage Subjects → title + description → Add. It appears immediately; the
   write is already in Firestore.
3. **Create a card and assign it** — Create Flashcard → pick the subject → question/answer → tick a
   student → Save.
4. **Manage it** — View All Flashcards → show subject chip, question, answer, "Assigned to: …" → Edit to
   prove the form repopulates → Save Changes → Unassign → Delete.
5. **Cascade** — delete the subject and note its cards disappear with it.
6. **Student side** — Logout → create an account → note "0 cards assigned" → sign in as a student who
   has assignments (or assign one as admin from a second device) and open study mode: flip, next,
   previous.
7. **Show the console** — Firestore → Data → `users`, `subjects`, `flashcards`, and the
   `assignedUserIds` array.

---

## 11. File map

```
app/src/main/java/com/example/prelimproject/
├── MainActivity.kt              theme + AppNavigation
├── data/Cloud.kt                Firestore instance, collections, seeding, liveList, errors
├── models/Models.kt             User / Subject / Flashcard + Keys + toMap / toX mappers
├── navigation/AppNavigation.kt  six routes, guards, edit state, setup screen
├── screens/
│   ├── LoginScreen.kt           query users by username, compare password, route by role
│   ├── SignUpScreen.kt          duplicate check, create STUDENT, sign in
│   ├── SubjectListScreen.kt     subject cards + per-role counts
│   ├── FlashcardScreen.kt       study mode (flip, progress, prev/next)
│   ├── AdminScreen.kt           hub + flashcard form + subject/user management
│   └── AdminCardListScreen.kt   full card list, unassign, edit, delete
└── ui/theme/                    Color, Type (Geist), Shape, Motion, Theme (dynamic color)
```

Supporting docs: [FIREBASE_SETUP.md](FIREBASE_SETUP.md) (setup + troubleshooting),
[README.md](README.md) and [DOCUMENTATION.md](DOCUMENTATION.md) (older write-ups that still describe the
Room version — treat this file as authoritative), [SCHEMA.md](SCHEMA.md) (old Room schema).

---

# Data schema

Firestore is a document store: three **collections** at the root, each holding documents keyed by a
string id. The document id **is** the object's `id` — it is not duplicated as a field.

```
(database)
├── users/{userId}
│     username*, password*, role*, displayName, email, createdAt*
├── subjects/{subjectId}
│     title*, description, color, createdAt*
└── flashcards/{cardId}
      subjectId*, question*, answer*, assignedUserIds[], hint, createdAt*
```

`*` = always written by the app.

## 12.1 `users`

The account record. Also the login lookup table (`whereEqualTo("username", …) limit 1`).

| Field | Type | Required | Notes |
|---|---|---|---|
| *(document id)* | string | yes | `id`; generated with `System.currentTimeMillis().toString()` |
| `username` | string | yes | Unique by convention — signup checks before creating; not enforced by the database |
| `password` | string | yes | **Plaintext.** Compared client-side |
| `role` | string | yes | `"ADMIN"` or `"STUDENT"`; the only authorization switch in the app |
| `displayName` | string | no | Defaults to the username; updated together with the username by admin rename |
| `email` | string | no | Written as `""`; not collected by any screen |
| `createdAt` | number | yes | Epoch milliseconds; the list order key |

Example:

```json
{
  "username": "Admin",
  "password": "123",
  "role": "ADMIN",
  "displayName": "Administrator",
  "email": "admin@flashiq.app",
  "createdAt": 1792826662981
}
```

## 12.2 `subjects`

A grouping of flashcards. Deleting one deletes its cards.

| Field | Type | Required | Notes |
|---|---|---|---|
| *(document id)* | string | yes | `id` |
| `title` | string | yes | Shown everywhere; renamed per keystroke |
| `description` | string | no | One line on the subject card |
| `color` | string | no | Hex string, default `"#007AFF"`; stored but the UI currently uses theme colors |
| `createdAt` | number | yes | Epoch millis; list order key |

Example:

```json
{
  "title": "Project Management",
  "description": "Agile methodologies, sprint cycles, and project planning.",
  "color": "#007AFF",
  "createdAt": 1792826662981
}
```

## 12.3 `flashcards`

The main object. One document per question/answer pair.

| Field | Type | Required | Notes |
|---|---|---|---|
| *(document id)* | string | yes | `id` |
| `subjectId` | string | yes | Reference to `subjects/{id}`. Not enforced; a missing subject renders as "Uncategorized" |
| `question` | string | yes | Front of the card |
| `answer` | string | yes | Back of the card |
| `assignedUserIds` | array&lt;string&gt; | yes | References to `users/{id}`. Empty array = unassigned. Read by the student filter and written by assignment/unassignment |
| `hint` | string | no | Default `""`; carried through edits, not yet surfaced |
| `createdAt` | number | yes | Epoch millis; list order key; preserved on edit |

Example:

```json
{
  "subjectId": "3",
  "question": "What is Recomposition in Jetpack Compose?",
  "answer": "Re-running composable functions when their state data changes to update the UI.",
  "assignedUserIds": ["1792827000559", "1792827047803"],
  "hint": "",
  "createdAt": 1792826662990
}
```

## 12.4 Relationships

```
subjects 1 ──── * flashcards          via flashcards.subjectId
users    * ──── * flashcards          via flashcards.assignedUserIds[]
```

- A student sees `flashcards` where `subjectId == selected` **and** `assignedUserIds` contains their id.
- An admin sees every card in a subject.
- **Referential integrity is the app's responsibility** (no foreign keys):
  - delete subject → batch deletes its cards
  - delete user → batch `arrayRemove`s them from every card
  - unassign → `arrayRemove` on that card only

## 12.5 Queries used

| Screen | Query |
|---|---|
| Login | `users.whereEqualTo("username", x).limit(1)` |
| Signup duplicate check | `users.whereEqualTo("username", x).limit(1)` |
| Every list | `<collection>.orderBy("createdAt", ASCENDING)` via `liveList` (one live listener per screen) |
| Study mode / subject counts | client-side filter of the live `flashcards` list by `subjectId` and `assignedUserIds` |
| Seeding | `users.limit(1).get()`; no-op if non-empty |

No composite indexes are required: each query is a single collection with at most one equality filter or
one order-by, which Firestore serves from automatic single-field indexes. If a filtered query ever adds
an `orderBy` on a different field, a composite index will be needed and the console will print a direct
link to create it.

## 12.6 Seeded demo data

Written once by `Cloud.seedIfNeeded()` when `users` is empty. Seed documents use short literal ids:
`users/1`, `subjects/1`–`subjects/4`, `flashcards/1`–`flashcards/12`.

| Collection | Count | Content |
|---|---|---|
| `users` | 1 | `Admin` / `123`, role `ADMIN` |
| `subjects` | 4 | 1 Project Management, 2 Application Development, 3 Mobile Development, 4 English Proficiency |
| `flashcards` | 12 | 3 per subject, `assignedUserIds: []`, subject ids `"1"`–`"4"` |

## 12.7 Security rules (as configured in the setup guide)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}       { allow read, write: if request.time < timestamp.date(2027, 1, 1); }
    match /subjects/{subjectId} { allow read, write: if request.time < timestamp.date(2027, 1, 1); }
    match /flashcards/{cardId}  { allow read, write: if request.time < timestamp.date(2027, 1, 1); }
  }
}
```

Read this as: *anyone holding the app's config can read and write all three collections until
2027-01-01*. That is sufficient for a classroom demo and is **not** a production authorization model —
`users` contains plaintext passwords. The proper fix is Firebase Authentication with rules keyed to
`request.auth.uid`, plus per-student assignment checks.
