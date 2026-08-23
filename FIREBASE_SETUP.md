# Firebase development setup

WordDeck uses the Firebase Authentication and Firestore Emulators during
development. The tracked configuration uses the demo project ID
`demo-worddeck`, which has no production Firebase resources.

## Local Android configuration

`app/google-services.json` is intentionally ignored by Git. For local emulator
development, copy the tracked placeholder:

```powershell
Copy-Item .\app\google-services.example.json .\app\google-services.json
```

When a separate Firebase test or production project is created later, download
its `google-services.json` from Firebase Console and place it at the same local
path. Do not commit that file, passwords, tokens, service-account files or
private keys.

## Start the Firebase Emulators

Install Node.js using a version supported by the current Firebase CLI. If npm
reports an `unsupported engine` warning, switch to a supported Node.js LTS
release. From the project root run:

```powershell
npx firebase-tools emulators:start --only "auth,firestore" --project demo-worddeck
```

The Auth Emulator listens on `localhost:9099`, Firestore on `localhost:8080`,
and the Emulator UI is available at `http://localhost:4000`. Debug Android
builds use `10.0.2.2`, which maps the Android emulator to the development
computer. Release builds do not call `useEmulator`. Cleartext HTTP is allowed
only by the debug manifest because the local emulators do not use HTTPS;
release builds keep Android's secure default.

The tracked `firestore.rules` file allows access only to documents below the
current authenticated user's UID. Do not add public read rules unless the P2
public catalog is intentionally implemented.

## Verify the connection

Keep the Auth Emulator running, start an Android emulator and execute:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

`FirebaseAuthRepositoryTest` exercises registration, login, logout, profile
display-name updates and session observation. `FirestoreSecurityRulesTest`
verifies owner access and rejects foreign or unauthenticated access for every
synchronized collection. `SyncCoordinatorFirestoreTest` verifies Room →
Firestore → second Room transfer, including tombstone deletes without duplicate
review events. These tests use emulator-only users and never target production
data.
