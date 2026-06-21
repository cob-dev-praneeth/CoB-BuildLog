# CoB Build Log — Phase 1

An Android-first continuity capture app for Children of Bharat. This repository contains the first working product slice and its UI prototype in one native Jetpack Compose application.

## Included

- Fast text-note capture with timestamps, optional tags, and optional project association
- Immutable, newest-first activity timeline backed by Room
- Project creation and association
- Persistent foreground overlay with drag and edge docking
- Local-only storage; no account, analytics, or network access
- Purpose-built dark visual system with high-visibility capture controls

Voice, screenshot/OCR, clipboard capture, search, reports, and sync are intentionally marked as later phases rather than represented as working Phase 1 features.

## Run it

1. Open this folder in a current Android Studio release.
2. Allow Android Studio to install Android SDK 35 and use JDK 17 if prompted.
3. Sync the project and run the `app` configuration on an Android 8.0+ device or emulator.
4. Open **Settings → Floating capture → Allow & Start**. Android will ask for “display over other apps” permission.

## UI mock screenshots

Open `BuildLogApp.kt` in Android Studio and switch to **Split** or **Design** view. The Preview panel contains the group **CoB Build Log · Mock Screens** with four Pixel 7 renders:

- Timeline
- Quick Capture
- Projects
- Settings

The previews use shared realistic sample entries and projects and do not modify the Room database.

The source is configured for Gradle 8.11.1 / Android Gradle Plugin 8.9.1. A command-line compile could not be completed in the creation environment because its Windows sandbox denied Gradle access to its own runtime JARs; Android Studio can perform the normal initial sync and build.

## Product decisions

- Capturing requires only content. Project and tag fields are secondary.
- Entries have no update or delete operation in the Phase 1 data-access layer. Corrections become new timeline events.
- Overlay permission is requested only after an explicit user action.
- The foreground service has a visible, ongoing notification and declares its special-use purpose.
- Files remain private to the app sandbox.

## Structure

- `data/` — Room entities, DAOs, database, and repository
- `ui/` — Compose prototype and view model
- `overlay/` — draggable foreground capture bubble
- `docs/PHASE_1.md` — scope, flows, acceptance criteria, and next steps
