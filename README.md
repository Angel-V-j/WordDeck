# WordDeck

WordDeck е Android приложение за изучаване на чужди езици с учебни карти и
spaced repetition. Данните се пазят първо локално, така че основните функции
остават достъпни и без интернет.

## Основни функции

- локален offline профил без парола;
- Firebase регистрация, вход, изход и редактиране на display name;
- създаване, редактиране, изтриване, търсене и филтриране на тестета;
- създаване, редактиране, изтриване и търсене на учебни карти;
- flashcard режим с `Again`, `Hard`, `Good` и `Easy`;
- typed-answer режим с проверка без значение на главни/малки букви и околни
  интервали;
- SM-2 планиране на следващия преговор;
- история на преговорите и обобщение на учебната сесия;
- обща статистика и статистика по тесте;
- изрично стартирана синхронизация с Firestore между устройства.

## Технологии

- Kotlin и Kotlin Coroutines/Flow;
- Jetpack Compose, Material 3 и Navigation Compose;
- MVVM и Repository pattern;
- Room като локален source of truth;
- Firebase Authentication и Cloud Firestore;
- Gradle Kotlin DSL и KSP;
- JUnit за unit tests.

Проектът е един Gradle application module (`:app`). Обичайният CRUD поток е:

```text
Compose UI → ViewModel → Repository → Room
```

SM-2 логиката е отделена от Android и получава текущото време отвън, което я
прави детерминистична и лесна за unit testing. Firebase не се използва директно
от UI; синхронизацията обменя данни с Room, а интерфейсът продължава да наблюдава
локалната база.

## Изисквания

- Android Studio с JDK 17 или по-нова поддържана версия;
- Android SDK 36.1;
- Android emulator или устройство с Android 8.0 (API 26) или по-нова версия;
- Node.js и Firebase CLI само за online authentication/sync през локалните
  Firebase Emulators.

## Basic build/run

1. Клонирайте repository-то и отворете root директорията в Android Studio.
2. Създайте локалния Firebase configuration файл от tracked placeholder-а:

   ```powershell
   Copy-Item .\app\google-services.example.json .\app\google-services.json
   ```

3. Изчакайте Gradle sync. Android Studio ще създаде локалния `local.properties`
   с пътя към Android SDK.
4. Изберете emulator/device с API 26+ и стартирайте `app` configuration.

Debug APK може да бъде построен и от PowerShell в root директорията:

```powershell
.\gradlew.bat assembleDebug --no-configuration-cache
```

Clean clone не може да се build-не, преди локално да бъде добавен
`app/google-services.json`. Файлът и `local.properties` са игнорирани от Git.
Tracked example конфигурацията съдържа само placeholder стойности за локалния
demo проект.

## Firebase configuration

Debug build-ът използва Firebase Authentication и Firestore Emulators на
development компютъра. От root директорията стартирайте:

```powershell
npx firebase-tools emulators:start --only "auth,firestore" --project demo-worddeck
```

След това стартирайте приложението на Android emulator. Debug кодът използва
`10.0.2.2:9099` за Authentication и `10.0.2.2:8080` за Firestore; `10.0.2.2`
сочи към development компютъра само от Android emulator.

За собствен Firebase project изтеглете неговия `google-services.json` от
Firebase Console и го поставете локално в `app/`. Не commit-вайте този файл,
пароли, tokens, service-account файлове, private keys или signing material.
Допълнителни emulator инструкции има във
[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md).

Основните локални функции работят и без стартирани Firebase Emulators чрез
offline profile flow-а.

## Базова употреба

1. Създайте offline профил или се регистрирайте/влезте чрез Firebase.
2. Създайте тесте и добавете карти с лицева и обратна страна.
3. Стартирайте учебна сесия във flashcard или typed-answer режим.
4. Преглеждайте history и statistics от съответните екрани.
5. При linked profile стартирайте синхронизацията изрично от Profile екрана.

## Unit tests

Unit test suite-ът се намира в `app/src/test/` и се изпълнява с:

```powershell
.\gradlew.bat testDebugUnitTest --no-configuration-cache
```

За допълнителна локална проверка:

```powershell
.\gradlew.bat lintDebug assembleDebug --no-configuration-cache
```

## Release

Текущата Gradle конфигурация е с `versionName = 1.0` и `versionCode = 1`.

Локално генерираният `WordDeck-1.0-release.apk` е междинен release candidate и
не представя окончателния source state.
от окончателния commit.
