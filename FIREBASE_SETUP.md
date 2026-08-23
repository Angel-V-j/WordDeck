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

## Reproduce the two-device acceptance test

Use two Android emulators, called device A and device B, and one shared Auth /
Firestore Emulator instance. Install the same current debug APK on both devices
and clear only WordDeck's app data before starting.

1. On A, register a new test user. Create one deck and two cards. Review the
   first card, open Profile, choose Sync data, log in again and confirm.
2. On B, log in with the same test user. Login refreshes the local Room data;
   verify the deck, both cards and the first card's review history.
3. To test a conflict, disconnect both devices. Edit the same deck on A, then
   edit it again on B with a visibly different title. Reconnect A first and B
   second, then reopen A. The B title is expected because its `updatedAt` is
   later. Explicitly synchronize B and then A. Device clocks must be reasonably aligned; clock skew is a documented
   limitation of this simple last-write-wins policy.
4. On B, edit one card and review the second, still-new card, then explicitly
   synchronize. On A, log in or explicitly synchronize and
   app and verify the card edit plus both separate review events.
5. On A, delete the card and deck, then confirm synchronization. On B, refresh
   through login or synchronization and verify that neither
   is visible. Reopen once more and confirm that review history was not
   duplicated by retry.

Acceptance run on 2026-08-23:

| Check | Result |
|---|---|
| A create deck/card/review → B | PASS |
| concurrent deck edit with fixed timestamps | PASS — newer B edit won on both devices |
| B card edit and second review → A | PASS |
| A tombstone delete → B | PASS |
| repeated sync | PASS — exactly two review events remained |

The run used two independent emulator processes (API 37 and API 35) and their
separate production Room databases against the same Firebase Emulator user. No
production Firebase data or credentials were used.
