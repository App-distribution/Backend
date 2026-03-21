# Android Client Design Spec — AppDistribution

**Date:** 2026-03-21
**Status:** Approved
**Scope:** Android MVP client — plan creation target

---

## 1. Context

PRD and Architecture documents already exist and are approved:
- `docs/PRD.md` — product goals, roles, user flows, functional requirements
- `docs/ARCHITECTURE.md` — system overview, data model, API surface, auth model
- `docs/superpowers/plans/2026-03-21-backend-mvp.md` — backend implementation plan (Ktor)

This spec captures the Android-client-specific design decisions made during brainstorming and serves as the basis for the Android MVP implementation plan.

---

## 2. Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Navigation structure | Home-Centric, 4 bottom tabs | Акцент на actionable info, меньше вкладок |
| Visual theme | Material 3 Light + dynamic dark/light | Нативность Android 12+, system theme support |
| Build Detail layout | Hero + Sticky CTA | Весь контент на одном скролле, CTA всегда видна |
| Implementation order | Core-first | Инфраструктура готова до фич, нет пустых заглушек |

---

## 3. Module Structure

```
android/
├── app/                          # Entry point: DI root, MainActivity, AppNavGraph
├── core/
│   ├── network/                  # Retrofit/OkHttp, ApiService, DTOs, AuthInterceptor
│   ├── database/                 # Room DB, DAOs, entities, migrations
│   ├── datastore/                # DataStore: user prefs, auth tokens, TokenManager
│   ├── common/                   # Result<T>, AppError, extensions, Timber
│   └── ui/                       # Theme, Typography, Colors, shared Composables
└── feature/
    ├── auth/                     # Login (email input → OTP verify)
    ├── home/                     # Home tab: AttentionSection + RecentBuilds
    ├── browse/                   # Browse tab: Projects list → Builds list + filters
    ├── build-detail/             # Build Detail screen + download/install flow
    ├── upload/                   # Upload APK (Uploader/Admin only)
    ├── mine/                     # Mine tab: install history + active downloads
    └── settings/                 # Profile, notifications, permissions onboarding
```

**Dependency rules (strictly one-directional):**
- `feature/*` → `core/common`, `core/ui`, `core/network`, `core/database`, `core/datastore`
- `app` → all modules (DI wire-up only)
- `core/*` → never depend on `feature/*`

> **Note:** This module breakdown supersedes the one in `docs/ARCHITECTURE.md`. The architecture doc lists `projects/`, `builds/` etc. — treat this spec as the authoritative source for the Android module structure.

---

## 4. Navigation Graph

```
NavGraph
│
├── AuthGraph (unauthenticated)
│   ├── LoginScreen          — email input
│   └── OtpScreen            — OTP input + verify
│
└── MainGraph (authenticated) — ScaffoldWithBottomBar
    │
    ├── HomeTab
    │   └── HomeScreen       — AttentionSection + RecentBuildsSection
    │
    ├── BrowseTab
    │   ├── ProjectsScreen   — workspace project list
    │   ├── BuildsScreen     — project builds + filters/search
    │   └── BuildDetailScreen ← shared destination (all tabs + deep links)
    │
    ├── MineTab
    │   └── MineScreen       — install history + active downloads
    │
    └── ProfileTab
        ├── SettingsScreen   — notifications, theme, logout
        └── PermissionsScreen — Unknown Sources onboarding
```

**Bottom tabs:**

| Tab | Icon | Badge | Visibility |
|-----|------|-------|------------|
| Home | `home` | attention count | all roles |
| Browse | `search` | — | all roles |
| Mine | `download_done` | active downloads | all roles |
| Profile | `person` | — | all roles |

**Upload entry point:** FAB on `ProjectsScreen` + `BuildsScreen` (Uploader/Admin only) → `UploadScreen`. Not a standalone tab — upload is an infrequent operation.

**Deep link:** `appdist://builds/{buildId}` → `BuildDetailScreen`

---

## 5. MVI State Management

Each screen owns a `UiState`, `UiAction`, and `UiEffect`.

### Pattern

```kotlin
// State — complete screen state, immutable data class
// Action — all user intents (sealed interface)
// Effect — one-shot events: navigation, snackbar, system intents (Channel-based)

// Data flow:
// User tap → ViewModel.onAction(action)
//     → UseCase (suspend fun)
//         → Repository → Remote/Local
//     → emits new UiState via StateFlow
//     → emits UiEffect via Channel
// Composable collects UiState → recompose
// Composable collects UiEffect → LaunchedEffect side-effect
```

### BuildDetailScreen — reference implementation

```kotlin
data class BuildDetailUiState(
    val build: BuildUi? = null,
    val installedVersion: InstalledVersionUi? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val bytesLoaded: Long) : DownloadState
    data object Verifying : DownloadState
    data object ReadyToInstall : DownloadState
    data class Failed(val reason: AppError) : DownloadState
}

sealed interface BuildDetailAction {
    data object DownloadClicked : BuildDetailAction
    data object InstallClicked : BuildDetailAction
    data object CancelDownload : BuildDetailAction
    data object RetryClicked : BuildDetailAction
    data object CopyLinkClicked : BuildDetailAction
    data object ShareClicked : BuildDetailAction
}

sealed interface BuildDetailEffect {
    data class LaunchInstaller(val apkUri: Uri) : BuildDetailEffect
    data class ShowSnackbar(val message: String) : BuildDetailEffect
    data class CopyToClipboard(val text: String) : BuildDetailEffect
}
```

### AppError — typed errors

```kotlin
sealed interface AppError {
    data class Network(val code: Int?, val message: String) : AppError
    data object Unauthorized : AppError
    data object NoInternet : AppError
    data class ChecksumMismatch(val expected: String, val actual: String) : AppError
    data class IncompatibleDevice(val reason: String) : AppError
    data class SignatureMismatch(val installed: String, val new: String) : AppError
    data class Storage(val message: String) : AppError
}
```

### Key UiState fields by screen

| Screen | Key state fields |
|--------|-----------------|
| HomeScreen | attentionItems, recentBuilds, isLoading |
| BuildsScreen | builds (PagingData), filters, searchQuery, isLoading |
| MineScreen | installHistory, activeDownloads |
| UploadScreen | uploadProgress, extractedMetadata, formState |

---

## 6. Data Layer

### Room entities

```kotlin
@Entity(tableName = "builds")
data class BuildEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val environment: String,
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val status: String,
    val isLatestInChannel: Boolean,
    val uploadDate: Long,
    val uploaderName: String,
    val cachedAt: Long               // for TTL invalidation
)

@Entity(tableName = "install_history")
data class InstallHistoryEntity(
    @PrimaryKey val id: String,
    val buildId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installedAt: Long
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val buildId: String,
    val localPath: String?,
    val state: String,
    val progress: Float,
    val bytesLoaded: Long,
    val totalBytes: Long,
    val startedAt: Long
)
```

### Mapping chain

```
BuildResponse (DTO) → Build (Domain) → BuildUi (UI model)
```

`BuildUi` adds `installStatus: InstallStatus` computed by `GetInstallStatusUseCase` via `PackageManager` — never stored server-side.

### InstallStatus

```kotlin
sealed interface InstallStatus {
    data object NotInstalled : InstallStatus
    data class Installed(val versionCode: Long) : InstallStatus
    data class UpdateAvailable(val installed: Long, val available: Long) : InstallStatus
    data class InstalledNewer(val installed: Long, val available: Long) : InstallStatus
    data class SignatureMismatch(val installedFingerprint: String) : InstallStatus
    data object Incompatible : InstallStatus   // minSdk > device SDK
}
```

---

## 7. Use Cases

```
auth/
  RequestOtpUseCase(email)                    → Result<Unit>
  VerifyOtpUseCase(email, otp)                → Result<Unit>

home/
  GetAttentionItemsUseCase()                  → Flow<List<AttentionItem>>
  GetRecentBuildsUseCase(limit)               → Flow<List<BuildUi>>

browse/
  GetProjectsUseCase()                        → Flow<List<Project>>
  GetBuildsUseCase(projectId, filters)        → Flow<PagingData<BuildUi>>

build-detail/
  GetBuildDetailUseCase(buildId)              → Flow<BuildUi>
  GetInstallStatusUseCase(pkg, versionCode,
      certFingerprint)                        → InstallStatus
  DownloadBuildUseCase(buildId)               → Flow<DownloadState>
  VerifyChecksumUseCase(file, expected)       → Result<Unit>
  ReportInstallUseCase(buildId)               → Result<Unit>

upload/
  UploadBuildUseCase(apkFile, metadata)       → Flow<UploadState>
  ExtractApkMetadataUseCase(file)             → Result<ApkMetadata>
```

### AttentionItem aggregation

```kotlin
sealed interface AttentionItem {
    data class MandatoryUpdate(val build: BuildUi) : AttentionItem
    // ExpiringBuild: supported in MVP only if build.expiry_date != null
    // (the backend data model includes expiry_date, but retention policy UI is Phase 2)
    // Include the type and logic, but it will rarely appear until uploaders start setting expiry dates.
    data class ExpiringBuild(val build: BuildUi, val daysLeft: Int) : AttentionItem
    data class NewBuildInSubscribedChannel(val build: BuildUi) : AttentionItem
}
// Priority order: Mandatory → Expiring → New
```

---

## 8. Background Work (WorkManager)

### Workers

```kotlin
// Download APK
class DownloadWorker : CoroutineWorker() {
    // Input:  buildId, downloadUrl, expectedChecksum, expectedSize
    // Output: localFilePath
    // Progress: setProgress(workDataOf("progress" to 0.42f, "bytesLoaded" to N))
    // Retry: RetryPolicy.EXPONENTIAL, maxAttempts = 3
    // Constraint: NetworkType.CONNECTED
    // Resume: Range header if partial file + matching ETag  ← Phase 2, not MVP
}

// Periodic sync (new builds check)
class SyncBuildsWorker : CoroutineWorker() {
    // Trigger: periodic (15 min) + FCM data message
    // Action: check new builds in subscribed channels
    //         → update Room cache
    //         → show notification if mandatory build found
}

// Upload APK
class UploadWorker : CoroutineWorker() {
    // Input:  localApkPath, metadata (JSON)
    // Progress: setProgress(workDataOf("progress" to 0.67f))
    // Constraint: NetworkType.CONNECTED
    // Retry: auto on network error
}
```

### Install Flow (step by step)

```
1. User taps "Установить"
2. File already downloaded + checksum matches? → YES: skip to 5
3. Enqueue DownloadWorker → show progress (collect WorkInfo)
4. VerifyChecksumUseCase → FAIL: AppError.ChecksumMismatch + delete file
5. Check REQUEST_INSTALL_PACKAGES permission
   → DENIED: show rationale dialog → ACTION_MANAGE_UNKNOWN_APP_SOURCES
6. Check SignatureMismatch → warn + offer choice (cancel / continue)
7. Check Downgrade (new versionCode < installed) → warn user
8. startActivity(Intent(ACTION_VIEW, apkUri, FLAG_GRANT_READ_URI_PERMISSION))
9. ReportInstallUseCase(buildId) — fire & forget immediately after launching the
   system installer (step 8). The app cannot detect whether the user actually
   completed the system installer dialog, so we report on intent launch — consistent
   with Firebase App Distribution and other distribution tools.
   → update InstallHistory in Room
```

FileProvider required for secure APK URI sharing (Android 7+).

---

## 9. DI (Hilt)

```kotlin
@Module @InstallIn(SingletonComponent::class)
object NetworkModule        // OkHttpClient + Retrofit + ApiService

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule       // AppDatabase, all DAOs

@Module @InstallIn(SingletonComponent::class)
object DataStoreModule      // DataStore<Preferences>, TokenManager

@Module @InstallIn(ViewModelComponent::class)
object BuildDetailModule    // Download/Verify/InstallStatus UseCases
```

**AuthInterceptor:** automatically injects Bearer token; on 401 calls `TokenManager.refresh()` and retries; on refresh failure emits `LoggedOut` event → navigate to Auth graph.

---

## 10. Testing Strategy

| Layer | Type | Tool | Focus |
|-------|------|------|-------|
| UseCase | Unit | JUnit5 + Turbine | business logic, edge cases |
| ViewModel | Unit | JUnit5 + Turbine | state transitions, effects |
| Repository | Integration | Robolectric + MockWebServer | DTO→Domain mapping, cache |
| Room DAOs | Integration | in-memory Room | CRUD, Flow emissions |
| Install Flow | Unit | PackageManager mock | compatibility status logic |
| UI | UI | Compose Testing | key screens snapshot |

**MVP coverage priorities:**
1. `GetInstallStatusUseCase` — critical version comparison logic
2. `VerifyChecksumUseCase` — security
3. `BuildDetailViewModel` — complex state machine
4. `GetAttentionItemsUseCase` — aggregation logic

---

## 11. Tech Stack Summary

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI + Clean Architecture |
| DI | Hilt |
| Navigation | Navigation Compose |
| Network | Retrofit + OkHttp + KotlinxSerialization |
| Local DB | Room |
| Preferences | DataStore |
| Background | WorkManager |
| Images | Coil |
| Logging | Timber |
| Build system | Gradle Kotlin DSL |

---

## 12. Out of Scope (MVP → Phase 2+)

- Invite links, tester groups, QR codes
- Download resume via Range headers (WorkManager retry covers most cases)
- Pinned builds, favorite projects
- Build comparison screen
- Analytics dashboard
- Offline-first with full sync queue
- Web admin panel
