\# CoB Build Log Architecture



\## Core Philosophy



\* Continuity over memory

\* Capture first, organize later

\* Preserve history

\* Local-first ownership



\---



\## Current Architecture



\### UI Layer



Jetpack Compose



Screens:



\* Timeline

\* Quick Capture

\* Projects

\* Settings



\---



\### Overlay Layer



CaptureOverlayService



Responsibilities:



\* Floating bubble

\* Edge docking

\* Quick capture access



\---



\### Data Layer



Room Database



Current Entities:



\* Entry

\* Project



Characteristics:



\* Append-only

\* Immutable history

\* Local-first storage



\---



\## Entry Lifecycle



Capture



↓



Entry Created



↓



Stored in Room



↓



Visible in Timeline



↓



Included in Reports



↓



Included in Project History



\---



\## Future Architecture



\### Compilation Layer



Responsible for:



\* Daily reports

\* Summaries

\* Idea extraction

\* Task extraction



\---



\### Context Layer



Responsible for:



\* Active topics

\* Open threads

\* Project memory

\* Historical continuity



\---



\### Sync Layer



Future only.



Responsibilities:



\* Authentication

\* Backup

\* Synchronization



Local storage remains the source of truth.



