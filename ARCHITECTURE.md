# CoB Build Log Architecture

## Purpose

CoB Build Log is a local-first Android continuity capture system. Phase 1 provides fast text capture, immutable timeline entries, optional project association, and a persistent floating overlay.

The architecture follows four rules:

1. Capture must remain fast and work offline.
2. Entries are appended, never silently overwritten.
3. Android services and UI depend on a small repository boundary rather than the database directly.
4. Later capture types can join the same timeline without redesigning its core.

## Project tree

```text
CoB BUILD LOG/
├── app/
│   ├── build.gradle.kts              Android application configuration
│   ├── schemas/                      Exported Room schemas (migration history)
│   └── src/main/
│       ├── AndroidManifest.xml       Permissions, activity, and overlay service
│       ├── java/org/childrenofbharat/buildlog/
│       │   ├── BuildLogApplication.kt  Application-scoped dependency container
│       │   ├── MainActivity.kt         Compose host and permission flow
│       │   ├── data/
│       │   │   ├── Models.kt           Entities, entry types, timeline projection
│       │   │   ├── Converters.kt       Room type converters
│       │   │   ├── Daos.kt             Append and observation queries
│       │   │   ├── BuildLogDatabase.kt Room database definition
│       │   │   └── BuildLogRepository.kt Domain-facing persistence boundary
│       │   ├── overlay/
│       │   │   └── CaptureOverlayService.kt
│       │   └── ui/
│       │       ├── BuildLogApp.kt       Timeline, capture, projects, settings
│       │       ├── BuildLogViewModel.kt UI state and user actions
│       │       └── theme/Theme.kt       Compose color system
│       └── res/
│           ├── drawable/              Launcher/notification vector
│           └── values/                Android theme resources
├── docs/PHASE_1.md                    Scope and acceptance criteria
├── gradle/libs.versions.toml          Central dependency versions
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Runtime layers

```text
Compose UI
    │ observes StateFlow / sends user actions
    ▼
BuildLogViewModel
    │ coroutine boundary
    ▼
BuildLogRepository
    │ domain-level capture operations
    ▼
Room DAOs
    │
    ▼
Local SQLite database
```

`BuildLogApplication` currently acts as a deliberately small dependency container. It creates one Room database and one repository for the application process. A dependency-injection framework is unnecessary for Phase 1 but can replace this container without changing screens or entities.

The overlay service does not write entries itself. It opens `MainActivity` with a quick-capture request, keeping validation and persistence on the same path as in-app capture.

## Database schema

Room database: `cob-build-log.db`  
Current schema version: `1`

### `projects`

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | TEXT | Primary key | UUID generated on creation |
| `name` | TEXT | Required | Human-readable project name |
| `description` | TEXT | Required, defaults empty | Optional project context |
| `colorHex` | TEXT | Required | Presentation accent |
| `createdAt` | INTEGER | Required | Unix epoch milliseconds |

### `entries`

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | TEXT | Primary key | UUID generated at capture |
| `createdAt` | INTEGER | Required, indexed | Immutable capture time in epoch milliseconds |
| `type` | TEXT | Required | `NOTE`, `TASK`, `VOICE`, `SCREENSHOT`, `CLIPBOARD`, or `BOOKMARK` |
| `content` | TEXT | Required | Primary searchable/displayable text |
| `projectId` | TEXT | Nullable, indexed, foreign key | Optional association to `projects.id` |
| `tags` | TEXT | Required, defaults empty | Phase 1 comma-separated normalized tags |
| `sourceApplication` | TEXT | Nullable | Originating application when available |
| `metadataJson` | TEXT | Required, defaults `{}` | Type-specific, forward-compatible metadata |

Foreign-key behavior: deleting a project sets `entries.projectId` to `NULL`. It does not delete the historical entry.

### Timeline projection

`TimelineItem` is not a table. It is a read model produced by a left join from `entries` to `projects`, ordered by `entries.createdAt DESC`.

```text
entries.projectId ──────► projects.id
       │                       │
       └──── TimelineItem ◄────┘
```

This projection allows an entry to remain visible if it has no project or if its former project association becomes unavailable.

### Current immutability boundary

`EntryDao` exposes `insert` and observation queries only. There is no update, upsert, or delete method for entries. `OnConflictStrategy.ABORT` prevents a duplicate ID from replacing an existing record.

True audit-grade immutability should later add database triggers or a dedicated event store; the Phase 1 guarantee is enforced at the application data-access boundary.

## Entry lifecycle

```text
User opens capture
        │
        ▼
Compose holds unsaved draft in memory
        │
        ├── dismiss ──► draft discarded; no event created
        │
        ▼
Validate non-blank content
        │
        ▼
Repository normalizes content and tags
        │
        ▼
Create UUID + capture timestamp
        │
        ▼
Room INSERT with conflict strategy ABORT
        │
        ▼
Room Flow emits updated timeline
        │
        ▼
Compose renders the new immutable entry
```

Corrections should be represented by a new entry. A future correction event can refer to an earlier entry through metadata or an explicit relation such as `supersedesEntryId`; the original event must remain intact.

For media types, the database insert should occur only after the local asset has been safely committed, or it should begin with an explicit processing state. Failures must produce a retryable state rather than an apparently complete entry with missing media.

## Overlay lifecycle

The overlay uses `CaptureOverlayService`, a foreground service with the `specialUse` foreground-service type.

```text
Settings screen
    │ explicit user action
    ▼
Check SYSTEM_ALERT_WINDOW permission
    │
    ├── denied ──► open Android permission screen
    │                  │
    │                  └── return + granted ──► continue automatically
    ▼
Request notification permission where applicable
    │
    ▼
Start foreground service
    │
    ├── create low-importance notification channel
    ├── publish ongoing notification
    └── attach overlay view through WindowManager
                         │
                         ├── drag ──► update x/y position
                         ├── release ──► dock to nearest horizontal edge
                         └── tap ──► open MainActivity with quick-capture request
                                                    │
                                                    └── show note composer
```

The overlay never reads screen content and does not require an accessibility service. Android owns permission revocation. If overlay permission is revoked while the service runs, the service should be stopped or recreate its view only after permission is restored.

The current bubble position exists only for the service lifetime. A future preference store can persist its edge and vertical offset.

## Future migration strategy

### Schema discipline

- Increment the Room database version for every structural change.
- Commit every exported JSON schema under `app/schemas/`.
- Add an explicit `Migration(oldVersion, newVersion)` for each supported upgrade path.
- Never use destructive migration in production builds.
- Test migrations from every publicly released schema to the newest schema.
- Back up or copy user-owned media independently from the SQLite file.

### Recommended sequence

| Version | Candidate change | Migration notes |
|---|---|---|
| v1 | Entries and projects | Current Phase 1 schema |
| v2 | Normalized tags and entry-tag join table | Backfill by splitting `entries.tags`; retain old column until validation completes |
| v3 | Asset table for audio/images | Store relative private paths, MIME type, checksum, size, and processing state |
| v4 | Full-text search index | Backfill FTS from note content, transcripts, and OCR text |
| v5 | Reports and report-entry links | Store generated artifacts separately from source entries |
| v6 | Sync identity and outbox | Add server IDs, revision/vector metadata, tombstones, and queued operations without changing local IDs |

Versions are illustrative; changes should be grouped according to release needs rather than forced into this numbering.

### Safe migration pattern

For destructive-looking transformations, prefer expand → backfill → verify → contract:

1. Add the new table or nullable column.
2. Copy and validate existing data inside a transaction.
3. Make application reads understand both old and new representations during transition if needed.
4. Remove obsolete storage only in a later version after successful field use is established.

Entry history must survive migrations byte-for-byte where feasible. If content is transformed, store the original representation or a migration provenance record.

### File migrations

Room migrations do not cover audio, screenshot, OCR, or report files. Maintain a separate versioned file-layout migrator that:

- Uses stable asset IDs instead of user-visible filenames.
- Writes to a temporary path and verifies checksums before switching references.
- Is restart-safe and idempotent.
- Never deletes the previous asset until the database transaction commits.

## Extension points

### Voice

Add a recording domain service behind an interface such as `VoiceCaptureManager`. The foreground recording service owns microphone lifecycle and writes audio to app-private storage.

Recommended additions:

- `assets` row for the audio file and checksum.
- `entries.type = VOICE`.
- `content` containing the best available transcript or a concise placeholder while processing.
- Typed metadata for duration, codec, sample rate, language, and transcription model.
- A processing state such as `CAPTURING`, `QUEUED`, `TRANSCRIBING`, `READY`, or `FAILED`.

Transcription should be a replaceable worker (`TranscriptionEngine`) with on-device Whisper as the local-first default. Transcript revisions should be versioned rather than erasing the original machine output.

### OCR

Screenshot capture should be isolated behind `ScreenCaptureManager`, using Android MediaProjection after explicit user consent. An `OcrEngine` interface can support ML Kit first and Tesseract later.

Recommended flow:

```text
MediaProjection capture
    └── private image asset
            └── SCREENSHOT entry
                    └── background OCR job
                            └── searchable OCR result/version
```

Store the image separately from extracted text. OCR output should include engine/version, language, confidence where available, and processing status. Re-running OCR creates a new result version; it must not alter the original image or capture timestamp.

### Reports

Add a `ReportGenerator` domain boundary that consumes immutable timeline projections for an explicit local date range. Renderers should be independent:

- `MarkdownReportRenderer`
- `JsonReportRenderer`
- `PdfReportRenderer`

The source entry IDs used by a report should be stored in a join table so every statement can be traced back to its inputs. Generated files belong in the asset store, while the `reports` table holds date range, generator version, summary, creation time, and asset references.

Regeneration creates a new report version. It does not overwrite a previously exported historical report.

### CoB sync

Sync should attach outside the repository through an outbox/inbox boundary, not be embedded in UI or DAOs.

```text
Local capture
    └── Room transaction
            ├── immutable entry
            └── outbox operation
                    └── SyncWorker ──► CoB API
                                           │
                                           └── acknowledgement / remote events
```

Recommended components:

- `SyncCoordinator` for scheduling and connectivity policy.
- `CoBApiClient` for authenticated transport.
- `SyncOutboxDao` for retryable, ordered local operations.
- `RemoteEventApplier` for idempotent incoming changes.
- Mapping layer between local entities and versioned API DTOs.

Local UUIDs must remain stable. Remote IDs are additional identifiers, not replacements. Each operation needs an idempotency key. Conflicts should produce explicit timeline events or user-visible resolution states—never silent last-write-wins replacement.

Sync remains opt-in. Network transfer, project publication, and public visibility each require explicit user intent, and local capture must continue when authentication or connectivity fails.

## Cross-cutting extension rules

- New capture types enter through repository/domain commands, not direct DAO access from UI or services.
- Long-running work uses foreground services or WorkManager according to Android execution rules.
- Every processor records its engine and version for reproducibility.
- Binary assets live in private file storage; Room stores metadata and stable references.
- Search indexes, reports, and sync payloads are derived data and can be rebuilt from canonical entries and assets.
- Permission prompts occur at the moment a user invokes the related feature.
- No extension may make basic text capture depend on network access.
