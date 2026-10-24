# Running Flash IQ against your own Firebase project

The app now stores everything in **Cloud Firestore** (users, subjects, flashcards).
Until it finds a `google-services.json`, it shows a "Connect Firebase" screen instead of
crashing, so the setup below is the only thing standing between the APK and a working app.

**Package name — use this exactly:** `com.example.prelimproject`
If you change the application id in `app/build.gradle.kts`, you must register the new
package name in Firebase and download the config again.

---

## 1. Create the Firebase project

1. Open <https://console.firebase.google.com> and sign in with your Google account.
2. **Add project** → name it `Flash IQ` (any name works) → continue.
3. Google Analytics can be turned off; the app does not use it.
4. Wait for the project to finish creating, then **Continue**.

## 2. Register the Android app

1. On the project overview page, click the **Android** icon ("Add app").
2. **Android package name:** `com.example.prelimproject`
3. App nickname: `Flash IQ` (optional).
4. Debug signing certificate SHA-1: **leave empty**. It is only needed for Google
   Sign-In, which this app does not use yet.
5. **Register app**.

## 3. Download and place `google-services.json`

1. On the next screen click **Download google-services.json**.
2. Move that file to:

   ```
   app/google-services.json
   ```

   It must sit next to `app/build.gradle.kts`, not in the repository root.
3. `app/google-services.json` is already listed in `.gitignore`, so it will not be
   committed. Every developer/environment downloads their own copy.
4. If you want to skip the console wizard, you can also add the app later from
   **Project settings → Your apps → Add app**.

The Gradle plugin is applied conditionally, so the project builds with or without the
file; it just shows the setup screen when the file is missing.

## 4. Create the Firestore database

1. Left sidebar → **Build → Firestore Database** → **Create database**.
2. Choose **Standard edition**.
3. Pick the location closest to you (for the Philippines, `asia-southeast1`).
4. Start in **Production mode** (rules are set in the next step).

The app creates three collections by itself — `users`, `subjects`, `flashcards` — so you
do not need to create them manually.

## 5. Set the security rules

The app still checks passwords on the client and has no Firebase Authentication, so the
rules have to allow the client to read/write the three collections. For a class project,
time-box it:

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

Paste it in **Firestore → Rules → Publish**.

> Be aware of what this means: anyone who extracts the app's config can read the `users`
> collection, which currently contains plaintext passwords. That is acceptable for a
> demo/school build but not for a real product. The permanent fix is Firebase
> Authentication plus per-user rules, which is a separate change.

If the rules are wrong, the app shows **"Firestore denied this request. Check the
security rules."** instead of failing silently.

## 6. Build the APK

From the project root (Android Studio terminal or Git Bash):

```bash
./gradlew assembleDebug
```

Output:

```
app/build/outputs/apk/debug/app-debug.apk
```

In Android Studio you can also use **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
**Rebuild after adding `google-services.json`** — the file is baked into the APK at build
time, so an APK built before the download will still show the setup screen.

## 7. Install it on your phone

1. On the phone: **Settings → About phone → tap Build number 7×** to enable Developer
   options, then **Developer options → USB debugging → On**.
2. Connect the phone by USB, allow the debugging prompt, and confirm it is visible:

   ```bash
   adb devices
   ```

3. Install:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Or, with the phone connected in Android Studio, just press **Run ▶**. Copying the APK
   to the phone and opening it also works (allow "install unknown apps" for your file
   manager).

The phone needs internet access (Wi-Fi or mobile data) on first launch.

## 8. First launch

The app seeds demo data the first time the `users` collection is empty:

- admin account — username `Admin`, password `123`
- 4 subjects (Project Management, Application Development, Mobile Development,
  English Proficiency)
- 12 flashcards

Sign in as `Admin / 123` to reach the Admin Panel, or create a student account from the
login screen. Everything you do appears in **Firestore → Data** within a second, and the
screens update live because they subscribe to Firestore snapshot listeners.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| "Connect Firebase" screen | `google-services.json` missing from `app/`, or the APK was built before you added it. Add the file and rebuild. |
| "Firestore denied this request. Check the security rules." | Rules not published, or the time-box date has passed. |
| "Cannot reach Firebase. Check your internet connection." | Phone offline, or Firestore not created yet in the console. |
| Login says invalid username/password right after install | Seeding needs a moment on first launch; pull to re-open the app or wait a second and retry. |
| App installed but nothing syncs between two phones | Both must use the same `google-services.json` / Firebase project. |

## Notes for later

- Passwords are stored and compared client-side. Passwords are visible to anyone who can
  read the `users` collection.
- No composite indexes are required by the current queries (each screen reads a whole
  collection ordered by `createdAt`).
- Firestore keeps a local cache, so a brief offline period still shows the last data.
