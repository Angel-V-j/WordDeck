# WordDeck

Firebase Authentication development and Auth Emulator setup are described in
[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md).

WordDeck е Android приложение за изучаване на чужди езици чрез spaced
repetition. Проектът се разработва постепенно като дипломна работа.

## Текущ статус

Проектът е един Gradle application module (`:app`) с Kotlin packages за
presentation, domain и data слоевете. В момента работят:

- регистрация, вход, възстановяване на Firebase сесия, профил и изход;
- локално създаване, редактиране, изтриване, търсене и филтриране на тестета;
- локално създаване, редактиране, изтриване и търсене на карти;
- flashcard study flow с reveal и `Again / Hard / Good / Easy` оценяване;
- typed-answer режим с точно сравнение след `trim` и игнориране на главни/малки букви;
- SM-2 обновяване с атомарен Room запис на review state и отделен history event;
- history по карта и summary при край на учебна сесия;
- реактивно показване на Room данните чрез `Flow`, включително след restart;
- адаптивни Compose екрани и автоматизирани unit/Room/Compose тестове.

Firebase Authentication и Room са скрити зад малки repository interfaces, а
production зависимостите се създават в manual `AppContainer`. На този етап
умишлено няма:

- Firestore, production Firebase configuration или credentials;
- statistics;
- synchronization между устройства;
- dependency injection framework.

## Структура

```text
app/src/main/java/com/worddeck/
├── common/                 # AppResult, Clock, IdGenerator, OperationStatus
├── core/
│   └── AppContainer.kt
├── data/
│   ├── local/
│   │   ├── database/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── mapper/
│   ├── remote/
│   │   └── firebase/
│   │       └── Firebase Auth adapter
│   └── repository/
├── domain/
│   ├── model/
│   │   └── User, Deck, Flashcard и review модели/value classes
│   └── repository/
│       ├── AuthenticationRepository.kt
│       ├── DeckRepository.kt
│       └── FlashcardRepository.kt
├── feature/
│   ├── auth/
│   ├── home/               # owner deck list и deck details
│   ├── decks/              # deck/card forms и ViewModel-и
│   └── study/              # study modes, SM-2 review flow и history UI
├── navigation/
│   └── AppNavigation.kt
├── ui/
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
├── MainActivity.kt
├── WordDeckApp.kt
└── WordDeckApplication.kt
```

Нови packages се добавят едва когато имат реален consumer; не използваме
`.gitkeep` или фиктивни класове за планирана функционалност.

## Отговорности

- `feature/` съдържа Compose UI, feature-specific UiState и ViewModel-и.
- `domain/model/` съдържа platform-independent модели.
- `domain/repository/` съдържа само абстракциите, които feature слоят може да
  използва. Те не знаят за Room или Firebase.
- `data/local/` съдържа текущия Room source of truth.
- `data/remote/firebase/` съдържа Firebase Authentication adapter-а; Firestore
  ще бъде добавен във фазата за synchronization.
- `data/repository/` съдържа тънките Room repository implementations.
- `common/` съдържа само малки общи типове; `core/` съдържа composition root-а.
- `navigation/` съдържа централния Navigation Compose graph.
- `ui/` съдържа Material 3 theme и малък брой reusable UI components.

## Посока на зависимостите

Текущият CRUD поток е:

```text
Compose UI
    ↓
ViewModel
    ↓
Repository abstraction
    ↓
Room source of truth
```

UseCase се добавя само за координирана бизнес операция. Review flow минава през
`Compose → ViewModel → ReviewFlashcardUseCase`, който координира SM-2 и
атомарното записване на state/event през repository в Room. Firestore по-късно ще
синхронизира Room данните.

`domain` не трябва да зависи от Android, Compose, Room или Firebase. При
липса на интернет UI трябва да продължи да работи през repository и Room.
Firebase по-късно ще синхронизира cloud данните, без UI да го използва
директно.

## Spaced repetition

Scheduler-ът е чист Kotlin код, отделен от Android, Room и Firebase, и се
unit-test-ва независимо. Старият custom scheduler не е пренесен. Domain слоят
дефинира валидирана SM-2 quality `0..5`,
начални `repetition = 0`, `easeFactor = 2.5` и `intervalDays = 0`, minimum ease
factor `1.3`, reset при quality под `3`, първи интервали `1 / 6` дни и mapping
`Again / Hard / Good / Easy` към `0 / 3 / 4 / 5`. Алгоритъмът получава review
timestamp отвън и връща следващите repetition, ease factor, interval и review
дата. При всеки review се актуализират success/failure counters и се определя
`NEW`, `LEARNING`, `MASTERED` или `PROBLEMATIC` ниво. Чиста domain функция
избира new и due картите за текущ user/deck към подаден timestamp. Reveal/rate
UI flow-ът работи и пази текущата сесия във ViewModel. Review state и history
се записват атомарно в Room, а history екранът показва събитията по карта.

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

## Следващи основни стъпки

1. статистика и прогрес;
2. минимална Firestore синхронизация върху Room source of truth;
3. приемателно тестване и дипломна документация.
