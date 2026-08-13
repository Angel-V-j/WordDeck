# Firebase Authentication development setup

WordDeck uses the Firebase Authentication Emulator during development. The
tracked configuration uses the demo project ID `demo-worddeck`, which has no
production Firebase resources.

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

## Start the Auth Emulator

Install Node.js using a version supported by the current Firebase CLI. If npm
reports an `unsupported engine` warning, switch to a supported Node.js LTS
release. From the project root run:

```powershell
npx firebase-tools emulators:start --only auth --project demo-worddeck
```

The Auth Emulator listens on `localhost:9099`; its UI is available at
`http://localhost:4000`. Debug Android builds use `10.0.2.2:9099`, which maps
the Android emulator to the development computer. Release builds do not call
`useEmulator`. Cleartext HTTP is allowed only by the debug manifest because the
local emulator does not use HTTPS; release builds keep Android's secure default.

## Verify the connection

Keep the Auth Emulator running, start an Android emulator and execute:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

`FirebaseAuthRepositoryTest` exercises registration, login, logout
and session observation using emulator-only users. It never targets production
user data.
