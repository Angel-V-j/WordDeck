# WordDeck

Firebase Authentication development and Auth Emulator setup are described in
[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md).

WordDeck е Android приложение за изучаване на чужди езици чрез spaced
repetition. В момента repository-то съдържа само архитектурен skeleton за
дипломната работа, а не завършена функционалност.

## Текущ статус

Проектът е един Gradle application module (`:app`) с Kotlin packages за
feature, domain и data слоевете. Минималният Compose entry point отваря само
placeholder home екран, за да валидира Compose, Material 3 и Navigation
Compose конфигурацията.

Подготвени са базов ViewModel, Room local слой, manual `AppContainer` и
development връзка към Firebase Auth Emulator. На този етап умишлено няма:

- login или registration flow;
- Firebase authentication repository или реални потребители;
- Firestore, production Firebase configuration или credentials;
- SM-2 алгоритъм, study session или statistics;
- synchronization между устройства;
- dependency injection framework.

## Структура

```text
app/src/main/java/com/worddeck/
├── core/
│   └── AppResult.kt
├── data/
│   ├── local/
│   │   ├── database/
│   │   ├── dao/
│   │   └── entity/
│   ├── remote/
│   │   └── firebase/
│   │       ├── auth/
│   │       └── firestore/
│   └── repository/
├── domain/
│   ├── model/
│   │   ├── User.kt
│   │   ├── Deck.kt
│   │   └── Flashcard.kt
│   └── repository/
│       ├── AuthenticationRepository.kt
│       ├── DeckRepository.kt
│       └── FlashcardRepository.kt
├── feature/
│   ├── auth/
│   ├── home/
│   │   └── HomeScreen.kt
│   ├── decks/
│   ├── study/
│   └── statistics/
├── navigation/
│   └── AppNavigation.kt
├── ui/
│   ├── components/
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
├── MainActivity.kt
└── WordDeckApp.kt
```

Папките без файлове в схемата показват планирани packages. Git ще ги добави
реално, когато в тях се появи първата необходима имплементация; не използваме
`.gitkeep` или фиктивни Kotlin класове само за да пазим празни директории.

## Отговорности

- `feature/` съдържа Compose UI и бъдещата presentation логика по
  функционалности. ViewModel и UiState се добавят само при реална нужда.
- `domain/model/` съдържа platform-independent модели.
- `domain/repository/` съдържа само абстракциите, които feature слоят може да
  използва. Те не знаят за Room или Firebase.
- `data/local/` е мястото за бъдещия Room source of truth.
- `data/remote/firebase/` е мястото за бъдещите Authentication и Firestore
  adapters.
- `data/repository/` ще съдържа concrete repository implementations, които
  координират local и remote data sources.
- `core/` съдържа само малки, действително общи типове. Засега това е
  `AppResult`.
- `navigation/` съдържа централния Navigation Compose graph.
- `ui/` съдържа Material 3 theme и малък брой reusable UI components.

## Посока на зависимостите

Планираният поток е:

```text
Compose UI
    ↓
ViewModel
    ↓
UseCase (само за реална бизнес операция)
    ↓
Repository abstraction
    ↓
Room source of truth
    ↕
Firebase synchronization
```

`domain` не трябва да зависи от Android, Compose, Room или Firebase. При
липса на интернет UI трябва да продължи да работи през repository и Room.
Firebase по-късно ще синхронизира cloud данните, без UI да го използва
директно.

## Spaced repetition

Бъдещият scheduler ще бъде чист Kotlin код, отделен от Android, Room и
Firebase, за да може да се unit-test-ва независимо. Старият custom scheduler
не е пренесен. `ReviewEvent` и `ReviewState` вече могат да се съхраняват
локално, но SM-2 update логиката и mapping-ът от UI оценка към quality `0..5`
още не са имплементирани.

## Локална конфигурация и secrets

`local.properties` се създава локално от Android Studio и е игнориран.
`app/google-services.json` също се добавя само локално според
[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md). Не записвайте API keys, private keys,
passwords, tokens, signing stores или service-account credentials в source
code или README.

## Build

От root директорията на проекта:

```powershell
.\gradlew.bat test assembleDebug --no-configuration-cache
```

## Решения преди реалната имплементация

Преди Room и SM-2 трябва да се уточнят:

1. как UI оценките се преобразуват към SM-2 quality `0..5`;
2. точните полета на `ReviewState` и immutable `ReviewEvent`;
3. дали локалната база пази данни за един или за няколко Firebase users;
4. минималната synchronization/conflict стратегия, без преждевременен
   outbox или сложен sync framework.
