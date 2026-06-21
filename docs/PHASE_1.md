# Phase 1 product slice

## Outcome

A contributor can open Build Log or tap its overlay from another app, record a note in seconds, and immediately find it in an immutable local timeline.

## Core flow

1. Tap the in-app `+` or floating overlay.
2. Type the capture. Project and tags may be added, but are never required.
3. Tap **Capture now**.
4. The note is timestamped and appended to the timeline.

## Screens

### Timeline

The landing screen. It shows the total captured count, chronological cards, capture type, time, project, and tags. Its empty state teaches the single most important action.

### Quick capture

A bottom sheet optimized for one-handed use. The cursor begins in the content area; metadata is visually subordinate. A local/immutable reassurance sits beside the save action.

### Projects

Creates lightweight project records and makes them available during capture. Notes do not require a project.

### Settings

Explains and starts the overlay with explicit permission. Later-phase capabilities are labeled honestly.

## Acceptance criteria

- A note can be saved with only body text.
- New entries appear first and survive process restarts.
- No DAO method can mutate or silently overwrite an entry.
- Project and tags are optional.
- Overlay permission is requested contextually, not at launch.
- The overlay can be dragged, docks to the nearest horizontal edge, and opens capture.
- The foreground service remains visible to the user through an ongoing notification.
- The app performs all Phase 1 behavior offline.

## Deliberately deferred

- Full-text search and filters
- Voice recording and transcription
- Screenshot capture and OCR
- Clipboard and bookmark capture
- Daily Markdown/JSON/PDF reports
- Export, backup, synchronization, and CoB account integration
- Accessibility-based capture

## Recommended next engineering pass

1. Compile on Android Studio and add instrumented Room migration tests.
2. Test overlay behavior on Samsung, Xiaomi, Pixel, and OnePlus devices.
3. Add onboarding and permission education.
4. Add export/delete controls before external beta distribution.
5. Measure median time-to-capture against the three-second success target.
