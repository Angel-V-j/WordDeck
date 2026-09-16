# WordDeck visual identity

Original vector mark: two learning cards with a W. The same geometry is used in the interface and launcher.

## Editable files

- `worddeck-logo.svg`: compact, transparent mark for the interface.
- `worddeck-icon.svg`: full 108 × 108 adaptive artwork with a solid teal background.
- `worddeck-monochrome.svg`: one-color silhouette with a W cutout.

Android counterparts are in `app/src/main/res/drawable`: `worddeck_logo.xml`, `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, and `ic_launcher_monochrome.xml`.
The existing launcher and round launcher resource names are retained. The app still supports API 26+; its manifest and application ID are unchanged. Density-specific WebP icons are supplied for mdpi through xxxhdpi.

## Design

- Primary teal: light `#146B5A`, dark `#8BD6BF`.
- Quiet green surfaces with blue secondary and amber accents.
- System sans-serif typography, with no downloaded fonts or added dependencies.
- Text labels remain on actions; rating colors supplement the existing Again / Hard / Good / Easy labels.
- Home has one search field and dropdown filters derived from all owned decks. Deck details and cards share a scrolling list, with Edit deck / Delete deck in a separate footer. Action rows wrap as required. Inputs retain their existing values, validation and enabled conditions.
- Home, Statistics and Profile share bottom navigation. The active icon is white on the original teal; tab state is saved and restored. Detail and study screens keep their existing Back flow.
- Revealing a typed answer clears focus and closes the keyboard so the result can be read. The callback and grading logic remain unchanged.

## Verification

Initial redesign: 50 unit tests and 22 UI scenarios passed. The 16 September layout follow-up passes 50 unit tests and 24 UI scenarios, including combined dropdown filters, reset after no matches, and repeated tab switching with state restoration. `assembleDebug`, `assembleDebugAndroidTest`, and `lintDebug` succeeded. Lint retained the same 18 pre-existing version/SDK warnings, with no errors.
The follow-up changes HomeScreen, FlashcardScreen and the navigation container, adds a bottom navigation component, vector icons and five string resources. Other existing source files and existing string values were byte-compared with the follow-up baseline and are unchanged. ViewModels, SM-2, database, Firebase and synchronization code were not edited.

Manual checks used an isolated API 37 emulator with fictitious data: normal and small portrait, landscape, light and dark themes, font scales 1.5 and 2.0, keyboard input, dialogs and the launcher icon. Standard icon masks and the monochrome silhouette were visually checked.

The local UI tests in `app/src/androidTest/` retain the repository's existing ignored status. Viewport-dependent checks scroll to controls before clicking/asserting. The navigation suite also covers combined filters and tab state; existing functional assertions were retained.

Not verified: live Firebase operations, physical devices, every supported OS version, and enabling themed icons in the system launcher. No production credentials or user data were used.

Adaptive icon reference: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

## Navigation and filter icons

Original editable SVG icons: [Home](ic_home.svg), [Statistics](ic_statistics.svg), [Profile](ic_profile.svg), [dropdown arrow](ic_chevron_down.svg), [selection check](ic_check.svg).
Matching Android VectorDrawables use the same filenames in `app/src/main/res/drawable/`.

The implementation uses the existing Compose dependencies: [NavigationBar](https://developer.android.com/develop/ui/compose/components/navigation-bar) and [DropdownMenu](https://developer.android.com/develop/ui/compose/components/menu). No new modules or libraries were added.
