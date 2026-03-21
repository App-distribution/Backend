# Android Client MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Собрать Android MVP-клиент для AppDistribution — авторизация, каталог сборок, скачивание и установка APK, история установок, push-уведомления.

**Architecture:** Kotlin 2.x + Jetpack Compose + MVI + Clean Architecture. Core-first: инфраструктурные модули (`core/*`) создаются до feature-модулей. Каждый feature-модуль содержит domain (UseCases + Repository interface) + data (impl) + ui (ViewModel + Composables) + тесты.

**Tech Stack:** Kotlin 2.1.10, AGP 8.8.0, Compose BOM 2024.12.01, Hilt 2.54, Room 2.7.0, Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3, DataStore 1.1.2, WorkManager 2.10.0, Navigation Compose 2.8.5, Coil 2.7.0, Paging3 3.3.4, Turbine 1.2.0, JUnit5 5.10.2, MockK 1.13.12

**Spec:** `docs/superpowers/specs/2026-03-21-android-client-design.md`

---

## File Structure

```
android/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts                        # root — только плагины, версии
├── settings.gradle.kts
├── gradle.properties
│
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/appdist/app/
│       │   ├── AppApplication.kt           # Hilt + Timber init
│       │   ├── MainActivity.kt             # single Activity
│       │   └── navigation/
│       │       ├── AppNavGraph.kt          # root NavHost, auth/main graph split
│       │       └── BottomNavBar.kt         # 4-tab bar
│       ├── AndroidManifest.xml
│       └── res/xml/file_paths.xml          # FileProvider paths
│
├── core/
│   ├── common/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/com/appdist/core/common/
│   │       │   ├── Result.kt               # sealed Result<T>
│   │       │   ├── AppError.kt             # typed errors
│   │       │   └── ext/
│   │       │       └── FlowExt.kt          # Flow helpers
│   │       └── test/kotlin/com/appdist/core/common/
│   │           └── ResultTest.kt
│   │
│   ├── ui/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/appdist/core/ui/
│   │       ├── theme/
│   │       │   ├── AppTheme.kt             # MaterialTheme wrapper, dynamic color
│   │       │   ├── Color.kt                # seed color + custom tokens
│   │       │   └── Type.kt                 # Typography overrides
│   │       └── components/
│   │           ├── BuildStatusChip.kt      # status/channel chips
│   │           ├── LoadingScreen.kt
│   │           ├── ErrorScreen.kt
│   │           └── EmptyScreen.kt
│   │
│   ├── datastore/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/com/appdist/core/datastore/
│   │       │   ├── TokenManager.kt         # save/get/refresh/clear tokens
│   │       │   ├── UserPreferencesStore.kt # theme, notification settings
│   │       │   └── DataStoreModule.kt      # Hilt @Provides
│   │       └── test/kotlin/com/appdist/core/datastore/
│   │           └── TokenManagerTest.kt
│   │
│   ├── network/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/appdist/core/network/
│   │       ├── ApiService.kt               # все Retrofit endpoints
│   │       ├── AuthInterceptor.kt          # Bearer + 401 refresh
│   │       ├── NetworkModule.kt            # Hilt @Provides OkHttp/Retrofit
│   │       └── dto/
│   │           ├── AuthDtos.kt
│   │           ├── BuildDtos.kt
│   │           ├── ProjectDtos.kt
│   │           ├── WorkspaceDtos.kt
│   │           └── ErrorResponse.kt
│   │
│   └── database/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/com/appdist/core/database/
│           │   ├── AppDatabase.kt          # RoomDatabase
│           │   ├── DatabaseModule.kt       # Hilt @Provides
│           │   ├── entity/
│           │   │   ├── BuildEntity.kt
│           │   │   ├── InstallHistoryEntity.kt
│           │   │   └── DownloadEntity.kt
│           │   └── dao/
│           │       ├── BuildDao.kt
│           │       ├── InstallHistoryDao.kt
│           │       └── DownloadDao.kt
│           └── test/kotlin/com/appdist/core/database/
│               └── BuildDaoTest.kt
│
└── feature/
    ├── auth/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/kotlin/com/appdist/feature/auth/
    │       │   ├── domain/
    │       │   │   ├── AuthRepository.kt
    │       │   │   ├── RequestOtpUseCase.kt
    │       │   │   └── VerifyOtpUseCase.kt
    │       │   ├── data/
    │       │   │   └── AuthRepositoryImpl.kt
    │       │   ├── di/AuthModule.kt
    │       │   └── ui/
    │       │       ├── login/
    │       │       │   ├── LoginScreen.kt
    │       │       │   └── LoginViewModel.kt
    │       │       └── otp/
    │       │           ├── OtpScreen.kt
    │       │           └── OtpViewModel.kt
    │       └── test/kotlin/com/appdist/feature/auth/
    │           ├── RequestOtpUseCaseTest.kt
    │           └── LoginViewModelTest.kt
    │
    ├── home/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/kotlin/com/appdist/feature/home/
    │       │   ├── domain/
    │       │   │   ├── model/AttentionItem.kt
    │       │   │   ├── GetAttentionItemsUseCase.kt
    │       │   │   └── GetRecentBuildsUseCase.kt
    │       │   ├── di/HomeModule.kt
    │       │   └── ui/
    │       │       ├── HomeScreen.kt
    │       │       ├── HomeViewModel.kt
    │       │       └── components/
    │       │           ├── AttentionSection.kt
    │       │           └── RecentBuildsSection.kt
    │       └── test/kotlin/com/appdist/feature/home/
    │           ├── GetAttentionItemsUseCaseTest.kt
    │           └── HomeViewModelTest.kt
    │
    ├── browse/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/kotlin/com/appdist/feature/browse/
    │       │   ├── domain/
    │       │   │   ├── ProjectRepository.kt
    │       │   │   ├── BuildRepository.kt
    │       │   │   ├── GetProjectsUseCase.kt
    │       │   │   └── GetBuildsUseCase.kt
    │       │   ├── data/
    │       │   │   ├── ProjectRepositoryImpl.kt
    │       │   │   └── BuildRepositoryImpl.kt
    │       │   ├── di/BrowseModule.kt
    │       │   └── ui/
    │       │       ├── projects/
    │       │       │   ├── ProjectsScreen.kt
    │       │       │   └── ProjectsViewModel.kt
    │       │       └── builds/
    │       │           ├── BuildsScreen.kt
    │       │           ├── BuildsViewModel.kt
    │       │           ├── BuildFilters.kt
    │       │           └── components/
    │       │               ├── BuildCard.kt
    │       │               └── FilterChipsRow.kt
    │       └── test/kotlin/com/appdist/feature/browse/
    │           └── GetBuildsUseCaseTest.kt
    │
    ├── build-detail/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/kotlin/com/appdist/feature/builddetail/
    │       │   ├── domain/
    │       │   │   ├── model/
    │       │   │   │   ├── BuildUi.kt
    │       │   │   │   ├── InstallStatus.kt
    │       │   │   │   └── DownloadState.kt
    │       │   │   ├── GetBuildDetailUseCase.kt
    │       │   │   ├── GetInstallStatusUseCase.kt
    │       │   │   ├── DownloadBuildUseCase.kt
    │       │   │   ├── VerifyChecksumUseCase.kt
    │       │   │   └── ReportInstallUseCase.kt
    │       │   ├── data/
    │       │   │   └── DownloadWorker.kt
    │       │   ├── di/BuildDetailModule.kt
    │       │   └── ui/
    │       │       ├── BuildDetailScreen.kt
    │       │       ├── BuildDetailViewModel.kt
    │       │       └── components/
    │       │           ├── BuildHeroSection.kt
    │       │           ├── VersionComparisonRow.kt
    │       │           ├── ChangelogSection.kt
    │       │           ├── TechDetailsSection.kt
    │       │           └── StickyInstallBar.kt
    │       └── test/kotlin/com/appdist/feature/builddetail/
    │           ├── GetInstallStatusUseCaseTest.kt
    │           ├── VerifyChecksumUseCaseTest.kt
    │           └── BuildDetailViewModelTest.kt
    │
    ├── upload/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/kotlin/com/appdist/feature/upload/
    │       │   ├── domain/
    │       │   │   ├── ExtractApkMetadataUseCase.kt
    │       │   │   └── UploadBuildUseCase.kt
    │       │   ├── data/UploadWorker.kt
    │       │   ├── di/UploadModule.kt
    │       │   └── ui/
    │       │       ├── UploadScreen.kt
    │       │       └── UploadViewModel.kt
    │       └── test/kotlin/com/appdist/feature/upload/
    │           └── ExtractApkMetadataUseCaseTest.kt
    │
    ├── mine/
    │   ├── build.gradle.kts
    │   └── src/main/kotlin/com/appdist/feature/mine/
    │       ├── di/MineModule.kt
    │       └── ui/
    │           ├── MineScreen.kt
    │           └── MineViewModel.kt
    │
    └── settings/
        ├── build.gradle.kts
        └── src/
            ├── main/kotlin/com/appdist/feature/settings/
            │   ├── di/SettingsModule.kt
            │   ├── data/SyncBuildsWorker.kt
            │   └── ui/
            │       ├── SettingsScreen.kt
            │       ├── SettingsViewModel.kt
            │       └── PermissionsScreen.kt
            └── test/kotlin/com/appdist/feature/settings/
                └── SyncBuildsWorkerTest.kt
```

---

## Task 1: Project Setup — Gradle Multi-Module

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/gradle/libs.versions.toml`
- Create: `android/app/build.gradle.kts`
- Create: `android/core/common/build.gradle.kts`
- Create: `android/core/ui/build.gradle.kts`
- Create: `android/core/datastore/build.gradle.kts`
- Create: `android/core/network/build.gradle.kts`
- Create: `android/core/database/build.gradle.kts`
- Create: `android/feature/auth/build.gradle.kts`
- Create: `android/feature/home/build.gradle.kts`
- Create: `android/feature/browse/build.gradle.kts`
- Create: `android/feature/build-detail/build.gradle.kts`
- Create: `android/feature/upload/build.gradle.kts`
- Create: `android/feature/mine/build.gradle.kts`
- Create: `android/feature/settings/build.gradle.kts`

- [ ] **Step 1: Создать `android/gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.8.0"
kotlin = "2.1.10"
compose-bom = "2024.12.01"
hilt = "2.54"
hilt-navigation-compose = "1.2.0"
room = "2.7.0"
navigation = "2.8.5"
workmanager = "2.10.0"
datastore = "1.1.2"
okhttp = "4.12.0"
retrofit = "2.11.0"
retrofit-kotlinx-serialization = "1.0.0"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"
coil = "2.7.0"
timber = "5.0.1"
paging = "3.3.4"
turbine = "1.2.0"
junit5 = "5.10.2"
mockk = "1.13.12"
coroutines-test = "1.9.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.10.0" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.7" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version = "1.2.0" }
hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Network
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofit-kotlinx-serialization" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# WorkManager
workmanager-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }

# Paging
paging-runtime = { group = "androidx.paging", name = "paging-runtime-ktx", version.ref = "paging" }
paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }

# Coil
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

# Logging
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }

# Firebase (FCM)
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version = "33.7.0" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx" }

# Testing
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.1.10-1.0.29" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

- [ ] **Step 2: Создать `android/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppDistribution"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:datastore")
include(":core:network")
include(":core:database")
include(":feature:auth")
include(":feature:home")
include(":feature:browse")
include(":feature:build-detail")
include(":feature:upload")
include(":feature:mine")
include(":feature:settings")
```

- [ ] **Step 3: Создать `android/build.gradle.kts` (root)**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
```

- [ ] **Step 4: Создать `android/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 5: Создать `android/app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.appdist.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.appdist.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:build-detail"))
    implementation(project(":feature:upload"))
    implementation(project(":feature:mine"))
    implementation(project(":feature:settings"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.workmanager.ktx)
    implementation(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 6: Создать `android/core/common/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.appdist.core.common"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> { useJUnitPlatform() }
```

- [ ] **Step 7: Создать `build.gradle.kts` для остальных core-модулей**

Содержимое аналогично `core/common`, меняется только `namespace` и зависимости. Создать:

`core/ui/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.appdist.core.ui"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
}
```

`core/datastore/build.gradle.kts` — namespace `com.appdist.core.datastore`, deps: `datastore-preferences`, `kotlinx-coroutines-android`, `hilt-android`, ksp `hilt-compiler`.

`core/network/build.gradle.kts` — namespace `com.appdist.core.network`, deps: `okhttp`, `okhttp-logging`, `retrofit`, `retrofit-kotlinx-serialization`, `kotlinx-serialization-json`, `hilt-android`, ksp `hilt-compiler`. Добавить plugins `kotlin.serialization`.

`core/database/build.gradle.kts` — namespace `com.appdist.core.database`, deps: `room-runtime`, `room-ktx`, `room-paging`, ksp `room-compiler`, `hilt-android`, ksp `hilt-compiler`. Добавить plugins `ksp`.

- [ ] **Step 8: Создать `build.gradle.kts` для feature-модулей**

Все feature-модули используют одинаковый шаблон. Пример для `feature/auth`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.appdist.feature.auth"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
tasks.withType<Test> { useJUnitPlatform() }
```

Создать аналогичные файлы для `feature/home`, `feature/browse`, `feature/build-detail` (добавить `workmanager-ktx`), `feature/upload` (добавить `workmanager-ktx`), `feature/mine`, `feature/settings`.

- [ ] **Step 9: Проверить синхронизацию Gradle**

```bash
cd android && ./gradlew help
```

Expected: BUILD SUCCESSFUL, все модули разрешены.

- [ ] **Step 10: Commit**

```bash
git add android/
git commit -m "chore: init Android multi-module project with version catalog"
```

---

## Task 2: core/common — Result, AppError, Extensions

**Files:**
- Create: `android/core/common/src/main/kotlin/com/appdist/core/common/Result.kt`
- Create: `android/core/common/src/main/kotlin/com/appdist/core/common/AppError.kt`
- Create: `android/core/common/src/main/kotlin/com/appdist/core/common/ext/FlowExt.kt`
- Create: `android/core/common/src/test/kotlin/com/appdist/core/common/ResultTest.kt`

- [ ] **Step 1: Написать тест для Result**

`android/core/common/src/test/kotlin/com/appdist/core/common/ResultTest.kt`:
```kotlin
package com.appdist.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {
    @Test
    fun `Success holds value`() {
        val result: Result<Int> = Result.Success(42)
        assertTrue(result is Result.Success)
        assertEquals(42, (result as Result.Success).data)
    }

    @Test
    fun `Error holds AppError`() {
        val error = AppError.NoInternet
        val result: Result<Int> = Result.Error(error)
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }

    @Test
    fun `getOrNull returns value on Success`() {
        assertEquals(42, Result.Success(42).getOrNull())
    }

    @Test
    fun `getOrNull returns null on Error`() {
        assertNull(Result.Error<Int>(AppError.NoInternet).getOrNull())
    }

    @Test
    fun `map transforms Success value`() {
        val result = Result.Success(2).map { it * 3 }
        assertEquals(Result.Success(6), result)
    }

    @Test
    fun `map preserves Error`() {
        val error = AppError.NoInternet
        val result = Result.Error<Int>(error).map { it * 3 }
        assertEquals(Result.Error<Int>(error), result)
    }

    private fun <T> assertNull(value: T?) = assertTrue(value == null)
}
```

- [ ] **Step 2: Запустить тест, убедиться что падает (Result не существует)**

```bash
cd android && ./gradlew :core:common:test
```

Expected: FAILED — unresolved reference: Result

- [ ] **Step 3: Создать `Result.kt`**

```kotlin
package com.appdist.core.common

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val error: AppError) : Result<T>()

    fun getOrNull(): T? = if (this is Success) data else null

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
    }

    val isSuccess get() = this is Success
    val isError get() = this is Error
}

inline fun <T> runCatching(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e.toAppError())
}
```

- [ ] **Step 4: Создать `AppError.kt`**

```kotlin
package com.appdist.core.common

import java.io.IOException

sealed interface AppError {
    data class Network(val code: Int?, val message: String) : AppError
    data object Unauthorized : AppError
    data object NoInternet : AppError
    data class ChecksumMismatch(val expected: String, val actual: String) : AppError
    data class IncompatibleDevice(val reason: String) : AppError
    data class SignatureMismatch(val installed: String, val available: String) : AppError
    data class Storage(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}

fun Exception.toAppError(): AppError = when (this) {
    is IOException -> AppError.NoInternet
    else -> AppError.Unknown(message ?: "Unknown error")
}
```

- [ ] **Step 5: Создать `ext/FlowExt.kt`**

```kotlin
package com.appdist.core.common.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.appdist.core.common.AppError
import com.appdist.core.common.Result

fun <T> Flow<T>.asResult(): Flow<Result<T>> = map<T, Result<T>> { Result.Success(it) }
    .catch { e -> emit(Result.Error(AppError.Unknown(e.message ?: "Flow error"))) }
```

- [ ] **Step 6: Запустить тесты**

```bash
cd android && ./gradlew :core:common:test
```

Expected: BUILD SUCCESSFUL, все тесты зелёные.

- [ ] **Step 7: Commit**

```bash
git add android/core/common/
git commit -m "feat(core/common): add Result, AppError, FlowExt"
```

---

## Task 3: core/ui — Material 3 Theme + Shared Composables

**Files:**
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/theme/Color.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/theme/Type.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppTheme.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/components/BuildStatusChip.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/components/LoadingScreen.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/components/ErrorScreen.kt`
- Create: `android/core/ui/src/main/kotlin/com/appdist/core/ui/components/EmptyScreen.kt`

- [ ] **Step 1: Создать `Color.kt`**

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color

// Seed color для Material You dynamic color generation
val AppSeedColor = Color(0xFF1A73E8)

// Semantic status colors (не зависят от темы)
val StatusMandatory = Color(0xFFC5221F)
val StatusMandatoryContainer = Color(0xFFFCE8E6)
val StatusDeprecated = Color(0xFF5F6368)
val StatusDeprecatedContainer = Color(0xFFF1F3F4)
val StatusExpiring = Color(0xFFEA8600)
val StatusExpiringContainer = Color(0xFFFEF7E0)
val StatusLatest = Color(0xFF137333)
val StatusLatestContainer = Color(0xFFE6F4EA)
```

- [ ] **Step 2: Создать `Type.kt`**

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
```

- [ ] **Step 3: Создать `AppTheme.kt`**

```kotlin
package com.appdist.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color: Android 12+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

- [ ] **Step 4: Создать `BuildStatusChip.kt`**

```kotlin
package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.*

enum class BuildChipType {
    MANDATORY, LATEST, DEPRECATED, EXPIRING, CHANNEL, ENVIRONMENT
}

@Composable
fun BuildStatusChip(
    label: String,
    type: BuildChipType,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (type) {
        BuildChipType.MANDATORY -> StatusMandatoryContainer to StatusMandatory
        BuildChipType.LATEST -> StatusLatestContainer to StatusLatest
        BuildChipType.DEPRECATED -> StatusDeprecatedContainer to StatusDeprecated
        BuildChipType.EXPIRING -> StatusExpiringContainer to StatusExpiring
        BuildChipType.CHANNEL, BuildChipType.ENVIRONMENT ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = label.uppercase(),
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
```

- [ ] **Step 5: Создать `LoadingScreen.kt`, `ErrorScreen.kt`, `EmptyScreen.kt`**

```kotlin
// LoadingScreen.kt
package com.appdist.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

```kotlin
// ErrorScreen.kt
package com.appdist.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.common.AppError

@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error.toHumanMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}

private fun AppError.toHumanMessage() = when (this) {
    AppError.NoInternet -> "Нет подключения к сети"
    AppError.Unauthorized -> "Необходима авторизация"
    is AppError.Network -> "Ошибка сервера ($code)"
    is AppError.ChecksumMismatch -> "Файл повреждён — checksum не совпадает"
    is AppError.SignatureMismatch -> "Подпись приложения отличается от установленной"
    is AppError.IncompatibleDevice -> "Устройство не поддерживает эту сборку"
    is AppError.Storage -> "Ошибка хранилища: $message"
    is AppError.Unknown -> message
}
```

```kotlin
// EmptyScreen.kt
package com.appdist.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 6: Убедиться что core/ui компилируется**

```bash
cd android && ./gradlew :core:ui:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add android/core/ui/
git commit -m "feat(core/ui): add Material3 theme, BuildStatusChip, state screens"
```

---

## Task 4: core/datastore — TokenManager + UserPreferences

**Files:**
- Create: `android/core/datastore/src/main/kotlin/com/appdist/core/datastore/TokenManager.kt`
- Create: `android/core/datastore/src/main/kotlin/com/appdist/core/datastore/UserPreferencesStore.kt`
- Create: `android/core/datastore/src/main/kotlin/com/appdist/core/datastore/DataStoreModule.kt`
- Create: `android/core/datastore/src/test/kotlin/com/appdist/core/datastore/TokenManagerTest.kt`

- [ ] **Step 1: Написать тест для TokenManager**

```kotlin
package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenManagerTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setUp() {
        dataStore = mockk(relaxed = true)
        tokenManager = TokenManager(dataStore)
    }

    @Test
    fun `getAccessToken returns null when no token stored`() = runTest {
        val prefs = mutablePreferencesOf()
        every { dataStore.data } returns flowOf(prefs)
        assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun `isAuthenticated emits false when no token`() = runTest {
        val prefs = mutablePreferencesOf()
        every { dataStore.data } returns flowOf(prefs)
        // Collect first value
        val result = mutableListOf<Boolean>()
        tokenManager.isAuthenticated.collect { result.add(it); return@collect }
        assertEquals(false, result.first())
    }
}
```

- [ ] **Step 2: Запустить тест, убедиться что падает**

```bash
cd android && ./gradlew :core:datastore:test
```

Expected: FAILED — unresolved reference: TokenManager

- [ ] **Step 3: Создать `TokenManager.kt`**

```kotlin
package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val isAuthenticated: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESS_TOKEN] != null
    }

    suspend fun getAccessToken(): String? =
        dataStore.data.first()[KEY_ACCESS_TOKEN]

    suspend fun getRefreshToken(): String? =
        dataStore.data.first()[KEY_REFRESH_TOKEN]

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
```

- [ ] **Step 4: Создать `UserPreferencesStore.kt`**

```kotlin
package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }
}
```

- [ ] **Step 5: Создать `DataStoreModule.kt`**

```kotlin
package com.appdist.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appdist_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
```

- [ ] **Step 6: Запустить тесты**

```bash
cd android && ./gradlew :core:datastore:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add android/core/datastore/
git commit -m "feat(core/datastore): add TokenManager, UserPreferencesStore, DataStoreModule"
```

---

---

## Task 5: core/network — Retrofit, DTOs, AuthInterceptor

**Files:**
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/dto/AuthDtos.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/dto/BuildDtos.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/dto/ProjectDtos.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/dto/ErrorResponse.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/ApiService.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/AuthInterceptor.kt`
- Create: `android/core/network/src/main/kotlin/com/appdist/core/network/NetworkModule.kt`

- [ ] **Step 1: Создать DTOs**

`dto/AuthDtos.kt`:
```kotlin
package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequest(@SerialName("email") val email: String)

@Serializable
data class VerifyOtpRequest(
    @SerialName("email") val email: String,
    @SerialName("otp") val otp: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)
```

`dto/ProjectDtos.kt`:
```kotlin
package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectResponse(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("name") val name: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("created_at") val createdAt: Long
)
```

`dto/BuildDtos.kt`:
```kotlin
package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildResponse(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("version_name") val versionName: String,
    @SerialName("version_code") val versionCode: Long,
    @SerialName("build_number") val buildNumber: String? = null,
    @SerialName("flavor") val flavor: String? = null,
    @SerialName("build_type") val buildType: String,
    @SerialName("environment") val environment: String,
    @SerialName("channel") val channel: String,
    @SerialName("branch") val branch: String? = null,
    @SerialName("commit_hash") val commitHash: String? = null,
    @SerialName("uploader_name") val uploaderName: String,
    @SerialName("upload_date") val uploadDate: Long,
    @SerialName("changelog") val changelog: String? = null,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("checksum_sha256") val checksumSha256: String,
    @SerialName("min_sdk") val minSdk: Int,
    @SerialName("target_sdk") val targetSdk: Int,
    @SerialName("cert_fingerprint") val certFingerprint: String? = null,
    @SerialName("abis") val abis: List<String>? = null,
    @SerialName("status") val status: String,
    @SerialName("expiry_date") val expiryDate: Long? = null,
    @SerialName("is_latest_in_channel") val isLatestInChannel: Boolean
)

@Serializable
data class DownloadUrlResponse(
    @SerialName("url") val url: String,
    @SerialName("expires_at") val expiresAt: Long
)

@Serializable
data class InstallEventRequest(
    @SerialName("device_model") val deviceModel: String,
    @SerialName("android_version") val androidVersion: String,
    @SerialName("sdk_version") val sdkVersion: Int
)
```

`dto/ErrorResponse.kt`:
```kotlin
package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String
)
```

- [ ] **Step 2: Создать `ApiService.kt`**

```kotlin
package com.appdist.core.network

import com.appdist.core.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/v1/auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): Response<Unit>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // Projects
    @GET("api/v1/workspaces/{workspaceId}/projects")
    suspend fun getProjects(@Path("workspaceId") workspaceId: String): Response<List<ProjectResponse>>

    @GET("api/v1/projects/{id}")
    suspend fun getProject(@Path("id") id: String): Response<ProjectResponse>

    // Builds
    @GET("api/v1/projects/{projectId}/builds")
    suspend fun getBuilds(
        @Path("projectId") projectId: String,
        @Query("channel") channel: String? = null,
        @Query("env") env: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<BuildResponse>>

    @GET("api/v1/builds/{id}")
    suspend fun getBuild(@Path("id") id: String): Response<BuildResponse>

    @GET("api/v1/builds/{id}/download-url")
    suspend fun getDownloadUrl(@Path("id") id: String): Response<DownloadUrlResponse>

    @GET("api/v1/builds/recent")
    suspend fun getRecentBuilds(@Query("limit") limit: Int = 20): Response<List<BuildResponse>>

    // Events
    @POST("api/v1/builds/{id}/install-event")
    suspend fun reportInstall(
        @Path("id") id: String,
        @Body request: InstallEventRequest
    ): Response<Unit>

    @POST("api/v1/builds/{id}/download-event")
    suspend fun reportDownload(@Path("id") id: String): Response<Unit>

    // User
    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserResponse>

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): Response<UserResponse>
}

// Дополнительные DTOs для user
@kotlinx.serialization.Serializable
data class UserResponse(
    @kotlinx.serialization.SerialName("id") val id: String,
    @kotlinx.serialization.SerialName("email") val email: String,
    @kotlinx.serialization.SerialName("name") val name: String,
    @kotlinx.serialization.SerialName("role") val role: String,
    @kotlinx.serialization.SerialName("workspace_id") val workspaceId: String
)

@kotlinx.serialization.Serializable
data class UpdateMeRequest(
    @kotlinx.serialization.SerialName("name") val name: String? = null,
    @kotlinx.serialization.SerialName("fcm_token") val fcmToken: String? = null
)
```

- [ ] **Step 3: Создать `AuthInterceptor.kt`**

AuthInterceptor добавляет Bearer-токен ко всем запросам и обновляет его при 401.

```kotlin
package com.appdist.core.network

import com.appdist.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }

        val request = chain.request().newBuilder()
            .apply { if (accessToken != null) addHeader("Authorization", "Bearer $accessToken") }
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            // Попытка обновить токен
            val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return response
            val refreshed = runBlocking {
                try {
                    // Прямой OkHttp-запрос без перехватчика, чтобы избежать рекурсии
                    chain.proceed(
                        chain.request().newBuilder()
                            .url(chain.request().url.toString().replace(
                                chain.request().url.encodedPath,
                                "/api/v1/auth/refresh"
                            ))
                            .post(okhttp3.RequestBody.create(
                                okhttp3.MediaType.parse("application/json"),
                                """{"refresh_token":"$refreshToken"}"""
                            ))
                            .build()
                    )
                } catch (e: Exception) { null }
            }

            if (refreshed != null && refreshed.code == 200) {
                // Парсим новый токен и сохраняем
                val newToken = refreshed.body?.string()
                    ?.let { org.json.JSONObject(it).getString("access_token") }
                if (newToken != null) {
                    runBlocking { tokenManager.updateAccessToken(newToken) }
                    return chain.proceed(
                        request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    )
                }
            }
            // refresh не удался — вернуть 401, UI обработает
            return refreshed ?: chain.proceed(request)
        }

        return response
    }
}
```

- [ ] **Step 4: Создать `NetworkModule.kt`**

```kotlin
package com.appdist.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                            else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

> **Примечание:** `BuildConfig.API_BASE_URL` и `BuildConfig.DEBUG` берутся из `app/build.gradle.kts`. Добавить в `app/build.gradle.kts`:
> ```kotlin
> android {
>     defaultConfig {
>         buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
>     }
>     buildTypes {
>         release { buildConfigField("String", "API_BASE_URL", "\"https://your-server.com/\"") }
>     }
>     buildFeatures { buildConfig = true }
> }
> ```

- [ ] **Step 5: Убедиться что core/network компилируется**

```bash
cd android && ./gradlew :core:network:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add android/core/network/
git commit -m "feat(core/network): add ApiService, AuthInterceptor, NetworkModule, DTOs"
```

---

## Task 6: core/database — Room Entities, DAOs, AppDatabase

**Files:**
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/entity/BuildEntity.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/entity/InstallHistoryEntity.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/entity/DownloadEntity.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/dao/BuildDao.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/dao/InstallHistoryDao.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/dao/DownloadDao.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt`
- Create: `android/core/database/src/main/kotlin/com/appdist/core/database/DatabaseModule.kt`
- Create: `android/core/database/src/test/kotlin/com/appdist/core/database/BuildDaoTest.kt`

- [ ] **Step 1: Написать тест для BuildDao**

```kotlin
package com.appdist.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuildDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BuildDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.buildDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `upsert and query builds for project`() = runTest {
        val build = buildEntity(id = "b1", projectId = "p1")
        dao.upsertBuilds(listOf(build))

        dao.getBuildsForProject("p1").test {
            val items = awaitItem()
            assert(items.size == 1)
            assert(items[0].id == "b1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteStale removes old entries`() = runTest {
        val old = buildEntity(id = "old", projectId = "p1", cachedAt = 1000L)
        val fresh = buildEntity(id = "fresh", projectId = "p1", cachedAt = System.currentTimeMillis())
        dao.upsertBuilds(listOf(old, fresh))
        dao.deleteStale(olderThan = System.currentTimeMillis() - 60_000)

        dao.getBuildsForProject("p1").test {
            val items = awaitItem()
            assert(items.size == 1)
            assert(items[0].id == "fresh")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildEntity(id: String, projectId: String, cachedAt: Long = System.currentTimeMillis()) =
        BuildEntity(
            id = id, projectId = projectId, versionName = "1.0.0", versionCode = 1,
            channel = "alpha", environment = "qa", changelog = null, fileSize = 1024,
            checksumSha256 = "abc", status = "active", isLatestInChannel = true,
            uploadDate = System.currentTimeMillis(), uploaderName = "dev", cachedAt = cachedAt
        )
}
```

> Для Robolectric добавить в `core/database/build.gradle.kts`:
> ```kotlin
> testImplementation("org.robolectric:robolectric:4.13")
> testOptions { unitTests.isIncludeAndroidResources = true }
> ```

- [ ] **Step 2: Создать Entities**

`entity/BuildEntity.kt`:
```kotlin
package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val cachedAt: Long
)
```

`entity/InstallHistoryEntity.kt`:
```kotlin
package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "install_history")
data class InstallHistoryEntity(
    @PrimaryKey val id: String,
    val buildId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installedAt: Long
)
```

`entity/DownloadEntity.kt`:
```kotlin
package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val buildId: String,
    val localPath: String?,
    val state: String,   // downloading / completed / failed
    val progress: Float,
    val bytesLoaded: Long,
    val totalBytes: Long,
    val startedAt: Long
)
```

- [ ] **Step 3: Создать DAOs**

`dao/BuildDao.kt`:
```kotlin
package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.BuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildDao {
    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY uploadDate DESC")
    fun getBuildsForProject(projectId: String): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds ORDER BY uploadDate DESC LIMIT :limit")
    fun getRecentBuilds(limit: Int): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds WHERE id = :id")
    suspend fun getBuildById(id: String): BuildEntity?

    @Upsert
    suspend fun upsertBuilds(builds: List<BuildEntity>)

    @Query("DELETE FROM builds WHERE cachedAt < :olderThan")
    suspend fun deleteStale(olderThan: Long)
}
```

`dao/InstallHistoryDao.kt`:
```kotlin
package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.InstallHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallHistoryDao {
    @Query("SELECT * FROM install_history ORDER BY installedAt DESC")
    fun getAll(): Flow<List<InstallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: InstallHistoryEntity)

    @Query("SELECT * FROM install_history WHERE packageName = :packageName ORDER BY installedAt DESC LIMIT 1")
    suspend fun getLatestForPackage(packageName: String): InstallHistoryEntity?
}
```

`dao/DownloadDao.kt`:
```kotlin
package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE state = 'downloading' OR state = 'completed'")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE buildId = :buildId")
    suspend fun getByBuildId(buildId: String): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE buildId = :buildId")
    suspend fun delete(buildId: String)
}
```

- [ ] **Step 4: Создать `AppDatabase.kt`**

```kotlin
package com.appdist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appdist.core.database.dao.*
import com.appdist.core.database.entity.*

@Database(
    entities = [BuildEntity::class, InstallHistoryEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buildDao(): BuildDao
    abstract fun installHistoryDao(): InstallHistoryDao
    abstract fun downloadDao(): DownloadDao
}
```

- [ ] **Step 5: Создать `DatabaseModule.kt`**

```kotlin
package com.appdist.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "appdist.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBuildDao(db: AppDatabase) = db.buildDao()
    @Provides fun provideInstallHistoryDao(db: AppDatabase) = db.installHistoryDao()
    @Provides fun provideDownloadDao(db: AppDatabase) = db.downloadDao()
}
```

- [ ] **Step 6: Запустить тесты**

```bash
cd android && ./gradlew :core:database:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add android/core/database/
git commit -m "feat(core/database): add Room entities, DAOs, AppDatabase"
```

---

## Task 7: app — DI Root, MainActivity, AppNavGraph

**Files:**
- Create: `android/app/src/main/kotlin/com/appdist/app/AppApplication.kt`
- Create: `android/app/src/main/kotlin/com/appdist/app/MainActivity.kt`
- Create: `android/app/src/main/kotlin/com/appdist/app/navigation/AppNavGraph.kt`
- Create: `android/app/src/main/kotlin/com/appdist/app/navigation/BottomNavBar.kt`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/res/xml/file_paths.xml`

- [ ] **Step 1: Создать `AppApplication.kt`**

```kotlin
package com.appdist.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

- [ ] **Step 2: Создать `MainActivity.kt`**

```kotlin
package com.appdist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.appdist.app.navigation.AppNavGraph
import com.appdist.core.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppNavGraph()
            }
        }
    }
}
```

- [ ] **Step 3: Создать `navigation/BottomNavBar.kt`**

```kotlin
package com.appdist.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavDestination("home", "Home", Icons.Default.Home)
    data object Browse : BottomNavDestination("browse", "Browse", Icons.Default.Search)
    data object Mine : BottomNavDestination("mine", "Mine", Icons.Default.DownloadDone)
    data object Profile : BottomNavDestination("profile", "Profile", Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavDestination.Home,
    BottomNavDestination.Browse,
    BottomNavDestination.Mine,
    BottomNavDestination.Profile
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
```

- [ ] **Step 4: Создать `navigation/AppNavGraph.kt`**

```kotlin
package com.appdist.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.appdist.core.datastore.TokenManager
import com.appdist.feature.auth.ui.login.LoginScreen
import com.appdist.feature.auth.ui.otp.OtpScreen
import com.appdist.feature.browse.ui.builds.BuildsScreen
import com.appdist.feature.browse.ui.projects.ProjectsScreen
import com.appdist.feature.builddetail.ui.BuildDetailScreen
import com.appdist.feature.home.ui.HomeScreen
import com.appdist.feature.mine.ui.MineScreen
import com.appdist.feature.settings.ui.PermissionsScreen
import com.appdist.feature.settings.ui.SettingsScreen
import com.appdist.feature.upload.ui.UploadScreen
import javax.inject.Inject

@Composable
fun AppNavGraph(tokenManager: TokenManager = hiltViewModel<AppNavViewModel>().tokenManager) {
    val navController = rememberNavController()
    val isAuthenticated by tokenManager.isAuthenticated.collectAsState(initial = false)

    // Auth state gate — redirect unauthenticated users
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) navController.navigate("auth/login") {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val showBar = currentRoute in bottomNavItems.map { it.route }
            if (showBar) BottomNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) "home" else "auth/login",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth graph
            composable("auth/login") {
                LoginScreen(onNavigateToOtp = { email ->
                    navController.navigate("auth/otp/$email")
                })
            }
            composable(
                "auth/otp/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStack ->
                OtpScreen(
                    email = backStack.arguments?.getString("email") ?: "",
                    onSuccess = {
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // Main graph
            composable("home") { HomeScreen(onBuildClick = { navController.navigate("builds/detail/$it") }) }

            composable("browse") {
                ProjectsScreen(
                    onProjectClick = { navController.navigate("browse/projects/$it/builds") },
                    onUploadClick = { navController.navigate("upload") }
                )
            }
            composable("browse/projects/{projectId}/builds",
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStack ->
                BuildsScreen(
                    projectId = backStack.arguments?.getString("projectId") ?: "",
                    onBuildClick = { navController.navigate("builds/detail/$it") },
                    onUploadClick = { navController.navigate("upload") }
                )
            }

            // Shared destination — Build Detail
            composable(
                "builds/detail/{buildId}",
                arguments = listOf(navArgument("buildId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "appdist://builds/{buildId}" })
            ) { backStack ->
                BuildDetailScreen(
                    buildId = backStack.arguments?.getString("buildId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable("upload") {
                UploadScreen(onSuccess = { navController.popBackStack() })
            }
            composable("mine") { MineScreen(onBuildClick = { navController.navigate("builds/detail/$it") }) }
            composable("profile") {
                SettingsScreen(
                    onPermissionsClick = { navController.navigate("profile/permissions") },
                    onLogout = {
                        navController.navigate("auth/login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("profile/permissions") { PermissionsScreen() }
        }
    }
}
```

> `AppNavViewModel` — простой HiltViewModel для получения `TokenManager`:
> ```kotlin
> @HiltViewModel
> class AppNavViewModel @Inject constructor(val tokenManager: TokenManager) : ViewModel()
> ```
> Создать в `app/navigation/AppNavViewModel.kt`.

- [ ] **Step 5: Создать `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".AppApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppDistribution">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Deep link: appdist://builds/{buildId} -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="appdist" android:host="builds" />
            </intent-filter>
        </activity>

        <!-- FileProvider для безопасной передачи APK системному installer -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <!-- WorkManager — custom WorkerFactory через HiltWorkerFactory -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>

    </application>
</manifest>
```

- [ ] **Step 6: Создать `res/xml/file_paths.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="apk_downloads" path="apk_downloads/" />
</paths>
```

- [ ] **Step 7: Проверить сборку**

```bash
cd android && ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL (возможны ошибки unresolved для feature-экранов — они будут заглушками до Task 8+).

- [ ] **Step 8: Commit**

```bash
git add android/app/
git commit -m "feat(app): add Application, MainActivity, NavGraph, BottomNavBar, Manifest"
```

---

---

## Task 8: feature/auth — Login, OTP, AuthRepository

**Files:**
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/AuthRepository.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/RequestOtpUseCase.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/VerifyOtpUseCase.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/data/AuthRepositoryImpl.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/di/AuthModule.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginViewModel.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginScreen.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpViewModel.kt`
- Create: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpScreen.kt`
- Create: `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/RequestOtpUseCaseTest.kt`
- Create: `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/LoginViewModelTest.kt`

- [ ] **Step 1: Написать тест для RequestOtpUseCase**

```kotlin
package com.appdist.feature.auth

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.AuthRepository
import com.appdist.feature.auth.domain.RequestOtpUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequestOtpUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = RequestOtpUseCase(repository)

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { repository.requestOtp("test@example.com") } returns Result.Success(Unit)
        val result = useCase("test@example.com")
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `returns Error when email is blank`() = runTest {
        val result = useCase("  ")
        assert(result is Result.Error)
        assert((result as Result.Error).error is AppError.Unknown)
    }

    @Test
    fun `propagates repository error`() = runTest {
        val error = AppError.NoInternet
        coEvery { repository.requestOtp(any()) } returns Result.Error(error)
        val result = useCase("test@example.com")
        assertEquals(Result.Error<Unit>(error), result)
    }
}
```

- [ ] **Step 2: Запустить тест, убедиться что падает**

```bash
cd android && ./gradlew :feature:auth:test
```

Expected: FAILED — unresolved reference.

- [ ] **Step 3: Создать `AuthRepository.kt`**

```kotlin
package com.appdist.feature.auth.domain

import com.appdist.core.common.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun requestOtp(email: String): Result<Unit>
    suspend fun verifyOtp(email: String, otp: String): Result<Unit>
    suspend fun logout()
    val isAuthenticated: Flow<Boolean>
}
```

- [ ] **Step 4: Создать `RequestOtpUseCase.kt` и `VerifyOtpUseCase.kt`**

```kotlin
package com.appdist.feature.auth.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.Error(AppError.Unknown("Email cannot be blank"))
        return repository.requestOtp(email.trim())
    }
}

class VerifyOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Unit> {
        if (otp.length != 6) return Result.Error(AppError.Unknown("OTP must be 6 digits"))
        return repository.verifyOtp(email, otp)
    }
}
```

- [ ] **Step 5: Создать `AuthRepositoryImpl.kt`**

```kotlin
package com.appdist.feature.auth.data

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.datastore.TokenManager
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.RequestOtpRequest
import com.appdist.core.network.dto.VerifyOtpRequest
import com.appdist.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = tokenManager.isAuthenticated

    override suspend fun requestOtp(email: String): Result<Unit> = try {
        val response = api.requestOtp(RequestOtpRequest(email))
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = try {
        val response = api.verifyOtp(VerifyOtpRequest(email, otp))
        if (response.isSuccessful) {
            val body = response.body()!!
            tokenManager.saveTokens(body.accessToken, body.refreshToken)
            Result.Success(Unit)
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    override suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clear()
    }

    private fun Exception.toAppError(): AppError =
        if (this is java.io.IOException) AppError.NoInternet
        else AppError.Unknown(message ?: "Unknown error")
}
```

- [ ] **Step 6: Создать `AuthModule.kt`**

```kotlin
package com.appdist.feature.auth.di

import com.appdist.feature.auth.data.AuthRepositoryImpl
import com.appdist.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
```

- [ ] **Step 7: Создать `LoginViewModel.kt`**

```kotlin
package com.appdist.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.RequestOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data object SubmitClicked : LoginAction
}

sealed interface LoginEffect {
    data class NavigateToOtp(val email: String) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _state.update { it.copy(email = action.value, error = null) }
            LoginAction.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val email = _state.value.email.trim()
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = requestOtp(email)) {
                is Result.Success -> _effects.send(LoginEffect.NavigateToOtp(email))
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
```

- [ ] **Step 8: Написать тест для LoginViewModel**

```kotlin
package com.appdist.feature.auth

import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.RequestOtpUseCase
import com.appdist.feature.auth.ui.login.LoginAction
import com.appdist.feature.auth.ui.login.LoginEffect
import com.appdist.feature.auth.ui.login.LoginViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val requestOtp = mockk<RequestOtpUseCase>()
    private lateinit var vm: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = LoginViewModel(requestOtp)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `email change updates state`() = runTest {
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        assertEquals("test@example.com", vm.state.value.email)
    }

    @Test
    fun `successful submit emits NavigateToOtp effect`() = runTest {
        coEvery { requestOtp("test@example.com") } returns Result.Success(Unit)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))

        vm.effects.test {
            vm.onAction(LoginAction.SubmitClicked)
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToOtp)
            assertEquals("test@example.com", (effect as LoginEffect.NavigateToOtp).email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed submit sets error in state`() = runTest {
        coEvery { requestOtp(any()) } returns Result.Error(AppError.NoInternet)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        vm.onAction(LoginAction.SubmitClicked)
        assertEquals(AppError.NoInternet, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }
}
```

- [ ] **Step 9: Создать `LoginScreen.kt`**

```kotlin
package com.appdist.feature.auth.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.ErrorScreen

@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToOtp -> onNavigateToOtp(effect.email)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AppDistribution", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(LoginAction.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.onAction(LoginAction.SubmitClicked) }),
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (error) {
                    is com.appdist.core.common.AppError.NoInternet -> "Нет подключения к сети"
                    is com.appdist.core.common.AppError.Network -> "Ошибка сервера"
                    else -> "Ошибка. Попробуйте снова."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onAction(LoginAction.SubmitClicked) },
            enabled = state.email.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Получить код")
        }
    }
}
```

- [ ] **Step 10: Создать `OtpViewModel.kt` и `OtpScreen.kt`**

`OtpViewModel.kt` — аналогичная структура с `OtpUiState`, `OtpAction`, `OtpEffect`:
```kotlin
package com.appdist.feature.auth.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpUiState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface OtpAction {
    data class OtpChanged(val value: String) : OtpAction
    data object VerifyClicked : OtpAction
}

sealed interface OtpEffect {
    data object NavigateToHome : OtpEffect
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyOtp: VerifyOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OtpUiState())
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private val _effects = Channel<OtpEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    var email: String = ""

    fun onAction(action: OtpAction) = when (action) {
        is OtpAction.OtpChanged -> _state.update {
            it.copy(otp = action.value.take(6), error = null)
        }
        OtpAction.VerifyClicked -> verify()
    }

    private fun verify() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = verifyOtp(email, _state.value.otp)) {
                is Result.Success -> _effects.send(OtpEffect.NavigateToHome)
                is Result.Error -> _state.update { it.copy(isLoading = false, error = r.error) }
            }
        }
    }
}
```

`OtpScreen.kt` — аналогичная структура LoginScreen: поле для 6-значного OTP + кнопка "Войти". Используй `KeyboardType.NumberPassword`. При вводе 6 символов — автоматически вызывай verify.

- [ ] **Step 11: Запустить тесты**

```bash
cd android && ./gradlew :feature:auth:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Commit**

```bash
git add android/feature/auth/
git commit -m "feat(auth): add OTP login flow with MVI ViewModel, Repository, UseCases"
```

---

## Task 9: feature/home — AttentionItems, RecentBuilds, HomeScreen

**Files:**
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/domain/model/AttentionItem.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/domain/GetAttentionItemsUseCase.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/domain/GetRecentBuildsUseCase.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/di/HomeModule.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/ui/HomeViewModel.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/ui/HomeScreen.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/ui/components/AttentionSection.kt`
- Create: `android/feature/home/src/main/kotlin/com/appdist/feature/home/ui/components/RecentBuildsSection.kt`
- Create: `android/feature/home/src/test/kotlin/com/appdist/feature/home/GetAttentionItemsUseCaseTest.kt`

> `feature/home` зависит от `feature/browse` для типа `BuildUi`. Альтернатива — перенести `BuildUi` в `core/common`. **Решение:** создать `BuildUi` в `core/common` чтобы его переиспользовали `home`, `browse`, `mine`.

- [ ] **Step 1: Создать `BuildUi` в `core/common`**

`android/core/common/src/main/kotlin/com/appdist/core/common/model/BuildUi.kt`:
```kotlin
package com.appdist.core.common.model

data class BuildUi(
    val id: String,
    val projectId: String,
    val projectName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val environment: String,
    val buildType: String,
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val status: String,              // "active" | "mandatory" | "deprecated" | "archived"
    val isLatestInChannel: Boolean,
    val uploadDate: Long,
    val uploaderName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val expiryDate: Long?,
    val branch: String?,
    val commitHash: String?,
    // Computed on client, not from server:
    val installStatus: InstallStatus = InstallStatus.NotInstalled
)

sealed interface InstallStatus {
    data object NotInstalled : InstallStatus
    data class Installed(val versionCode: Long) : InstallStatus
    data class UpdateAvailable(val installed: Long, val available: Long) : InstallStatus
    data class InstalledNewer(val installed: Long, val available: Long) : InstallStatus
    data class SignatureMismatch(val installedFingerprint: String) : InstallStatus
    data object Incompatible : InstallStatus
}
```

- [ ] **Step 2: Написать тест для GetAttentionItemsUseCase**

```kotlin
package com.appdist.feature.home

import com.appdist.core.common.model.*
import com.appdist.feature.home.domain.GetAttentionItemsUseCase
import com.appdist.feature.home.domain.model.AttentionItem
import io.mockk.every
import io.mockk.mockk
import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetAttentionItemsUseCaseTest {

    // HomeRepository — интерфейс, предоставляющий builds
    private val repository = mockk<com.appdist.feature.home.domain.HomeRepository>()
    private val useCase = GetAttentionItemsUseCase(repository)

    @Test
    fun `mandatory build appears as MandatoryUpdate`() = runTest {
        val mandatory = buildUi(status = "mandatory")
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(mandatory))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.any { it is AttentionItem.MandatoryUpdate })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expiring build within 3 days appears as ExpiringBuild`() = runTest {
        val expiringIn2Days = buildUi(expiryDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(expiringIn2Days))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.any { it is AttentionItem.ExpiringBuild })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `normal build does not appear in attention`() = runTest {
        val normal = buildUi(status = "active", expiryDate = null)
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(normal))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildUi(status: String = "active", expiryDate: Long? = null) = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0.0", versionCode = 1, channel = "alpha", environment = "qa",
        buildType = "debug", changelog = null, fileSize = 1024, checksumSha256 = "abc",
        status = status, isLatestInChannel = false, uploadDate = System.currentTimeMillis(),
        uploaderName = "dev", minSdk = 24, targetSdk = 35, certFingerprint = null,
        abis = emptyList(), expiryDate = expiryDate, branch = null, commitHash = null
    )
}
```

- [ ] **Step 3: Создать `AttentionItem.kt` и `HomeRepository.kt`**

```kotlin
package com.appdist.feature.home.domain.model

import com.appdist.core.common.model.BuildUi

sealed interface AttentionItem {
    data class MandatoryUpdate(val build: BuildUi) : AttentionItem
    data class ExpiringBuild(val build: BuildUi, val daysLeft: Int) : AttentionItem
    data class NewBuildInSubscribedChannel(val build: BuildUi) : AttentionItem
}
```

```kotlin
package com.appdist.feature.home.domain

import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getRecentBuilds(limit: Int = 20): Flow<List<BuildUi>>
}
```

- [ ] **Step 4: Создать `GetAttentionItemsUseCase.kt`**

```kotlin
package com.appdist.feature.home.domain

import com.appdist.feature.home.domain.model.AttentionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetAttentionItemsUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    operator fun invoke(): Flow<List<AttentionItem>> =
        repository.getRecentBuilds(50).map { builds ->
            val items = mutableListOf<AttentionItem>()
            val nowMs = System.currentTimeMillis()
            val threeDaysMs = TimeUnit.DAYS.toMillis(3)

            // Priority 1: Mandatory
            builds.filter { it.status == "mandatory" }
                .forEach { items.add(AttentionItem.MandatoryUpdate(it)) }

            // Priority 2: Expiring within 3 days
            builds.filter { build ->
                build.expiryDate != null && build.expiryDate > nowMs &&
                build.expiryDate - nowMs <= threeDaysMs
            }.forEach { build ->
                val daysLeft = TimeUnit.MILLISECONDS.toDays(build.expiryDate!! - nowMs).toInt()
                items.add(AttentionItem.ExpiringBuild(build, daysLeft))
            }

            items
        }
}
```

- [ ] **Step 5: Создать `GetRecentBuildsUseCase.kt`**

```kotlin
package com.appdist.feature.home.domain

import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentBuildsUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<BuildUi>> =
        repository.getRecentBuilds(limit)
}
```

- [ ] **Step 6: Создать `HomeViewModel.kt`**

```kotlin
package com.appdist.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.home.domain.GetAttentionItemsUseCase
import com.appdist.feature.home.domain.GetRecentBuildsUseCase
import com.appdist.feature.home.domain.model.AttentionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val attentionItems: List<AttentionItem> = emptyList(),
    val recentBuilds: List<BuildUi> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HomeAction {
    data object Refresh : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAttentionItems: GetAttentionItemsUseCase,
    private val getRecentBuilds: GetRecentBuildsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun onAction(action: HomeAction) = when (action) {
        HomeAction.Refresh -> load()
    }

    private fun load() {
        viewModelScope.launch {
            combine(
                getAttentionItems(),
                getRecentBuilds()
            ) { attention, recent ->
                HomeUiState(attentionItems = attention, recentBuilds = recent, isLoading = false)
            }.catch { _state.update { it.copy(isLoading = false) } }
             .collect { _state.value = it }
        }
    }
}
```

- [ ] **Step 7: Создать `HomeScreen.kt` и компоненты**

`HomeScreen.kt`:
```kotlin
package com.appdist.feature.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.home.ui.components.AttentionSection
import com.appdist.feature.home.ui.components.RecentBuildsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBuildClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text("AppDistribution") })
    }) { padding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(padding))
            state.attentionItems.isEmpty() && state.recentBuilds.isEmpty() ->
                EmptyScreen("Нет доступных сборок", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.attentionItems.isNotEmpty()) {
                    item { AttentionSection(state.attentionItems, onBuildClick) }
                }
                if (state.recentBuilds.isNotEmpty()) {
                    item { RecentBuildsSection(state.recentBuilds, onBuildClick) }
                }
            }
        }
    }
}
```

`AttentionSection.kt` — список `AttentionItem`, показывает MandatoryUpdate (красная карточка), ExpiringBuild (оранжевая карточка). Каждая карточка — `ElevatedCard` с кнопкой "Обновить"/"Посмотреть".

`RecentBuildsSection.kt` — горизонтальный `LazyRow` или вертикальный список последних `BuildUi` с кратким отображением (название, версия, channel, дата).

- [ ] **Step 8: Создать `HomeRepositoryImpl` (получает данные из Room + API)**

Создать в `feature/home/data/HomeRepositoryImpl.kt`. Реализация: берёт данные из `BuildDao.getRecentBuilds()` как основной источник, запускает фоновое обновление через `ApiService.getRecentBuilds()` → upsert в Room.

- [ ] **Step 9: Создать `HomeModule.kt`**

```kotlin
package com.appdist.feature.home.di

import com.appdist.feature.home.data.HomeRepositoryImpl
import com.appdist.feature.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
```

- [ ] **Step 10: Запустить тесты**

```bash
cd android && ./gradlew :feature:home:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add android/core/common/src/main/kotlin/com/appdist/core/common/model/
git add android/feature/home/
git commit -m "feat(home): add HomeScreen, AttentionItems, RecentBuilds with MVI"
```

---

## Task 10: feature/browse — Projects, Builds List, Filters

**Files:**
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/domain/ProjectRepository.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/domain/BuildRepository.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/domain/GetProjectsUseCase.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/domain/GetBuildsUseCase.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/BuildFilters.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/projects/ProjectsViewModel.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/projects/ProjectsScreen.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/BuildsViewModel.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/BuildsScreen.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/components/BuildCard.kt`
- Create: `android/feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/components/FilterChipsRow.kt`

- [ ] **Step 1: Создать `BuildFilters.kt`**

```kotlin
package com.appdist.feature.browse.ui.builds

data class BuildFilters(
    val channel: String? = null,       // "alpha" | "beta" | "rc" | etc.
    val environment: String? = null,   // "dev" | "qa" | "staging"
    val searchQuery: String = ""
) {
    val isEmpty get() = channel == null && environment == null && searchQuery.isBlank()
}
```

- [ ] **Step 2: Создать Repository interfaces**

```kotlin
// ProjectRepository.kt
package com.appdist.feature.browse.domain

import com.appdist.core.network.dto.ProjectResponse
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<ProjectResponse>>
}

// BuildRepository.kt
package com.appdist.feature.browse.domain

import androidx.paging.PagingData
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.browse.ui.builds.BuildFilters
import kotlinx.coroutines.flow.Flow

interface BuildRepository {
    fun getBuilds(projectId: String, filters: BuildFilters): Flow<PagingData<BuildUi>>
    suspend fun getBuild(buildId: String): com.appdist.core.common.Result<BuildUi>
    suspend fun getDownloadUrl(buildId: String): com.appdist.core.common.Result<String>
}
```

- [ ] **Step 3: Создать `GetProjectsUseCase.kt` и `GetBuildsUseCase.kt`**

```kotlin
package com.appdist.feature.browse.domain

import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(private val repo: ProjectRepository) {
    operator fun invoke() = repo.getProjects()
}

class GetBuildsUseCase @Inject constructor(private val repo: BuildRepository) {
    operator fun invoke(projectId: String, filters: BuildFilters) =
        repo.getBuilds(projectId, filters)
}
```

- [ ] **Step 4: Создать `BuildsViewModel.kt`**

```kotlin
package com.appdist.feature.browse.ui.builds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.appdist.feature.browse.domain.GetBuildsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val getBuilds: GetBuildsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _filters = MutableStateFlow(BuildFilters())
    val filters: StateFlow<BuildFilters> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val builds = _filters.flatMapLatest { filters ->
        getBuilds(projectId, filters)
    }.cachedIn(viewModelScope)

    fun setChannel(channel: String?) = _filters.update { it.copy(channel = channel) }
    fun setEnvironment(env: String?) = _filters.update { it.copy(environment = env) }
    fun setSearch(query: String) = _filters.update { it.copy(searchQuery = query) }
    fun clearFilters() = _filters.update { BuildFilters() }
}
```

- [ ] **Step 5: Создать `BuildCard.kt`**

```kotlin
package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.BuildChipType
import com.appdist.core.ui.components.BuildStatusChip
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuildCard(
    build: BuildUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(build.uploadDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(build.uploadDate))
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${build.projectName} v${build.versionName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "(${build.versionCode}) · $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Install status indicator
                when (build.installStatus) {
                    is InstallStatus.UpdateAvailable -> Badge { Text("↑") }
                    is InstallStatus.Installed -> Icon(
                        androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Unit
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BuildStatusChip(build.channel, BuildChipType.CHANNEL)
                if (build.status == "mandatory") BuildStatusChip("MANDATORY", BuildChipType.MANDATORY)
                if (build.isLatestInChannel) BuildStatusChip("LATEST", BuildChipType.LATEST)
                if (build.status == "deprecated") BuildStatusChip("DEPRECATED", BuildChipType.DEPRECATED)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "${build.fileSize / 1024 / 1024} MB · ${build.uploaderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 6: Создать `FilterChipsRow.kt`**

```kotlin
package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.feature.browse.ui.builds.BuildFilters

val CHANNELS = listOf("alpha", "beta", "rc", "internal", "nightly")
val ENVIRONMENTS = listOf("dev", "qa", "staging")

@Composable
fun FilterChipsRow(
    filters: BuildFilters,
    onChannelSelected: (String?) -> Unit,
    onEnvironmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.horizontalScroll(rememberScrollState())) {
        CHANNELS.forEach { channel ->
            FilterChip(
                selected = filters.channel == channel,
                onClick = { onChannelSelected(if (filters.channel == channel) null else channel) },
                label = { Text(channel) }
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(8.dp))
        ENVIRONMENTS.forEach { env ->
            FilterChip(
                selected = filters.environment == env,
                onClick = { onEnvironmentSelected(if (filters.environment == env) null else env) },
                label = { Text(env) }
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}
```

- [ ] **Step 7: Создать `BuildsScreen.kt`**

```kotlin
package com.appdist.feature.browse.ui.builds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.browse.ui.builds.components.BuildCard
import com.appdist.feature.browse.ui.builds.components.FilterChipsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    projectId: String,
    onBuildClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    viewModel: BuildsViewModel = hiltViewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val builds = viewModel.builds.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сборки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onUploadClick) {
                Icon(Icons.Default.Add, "Загрузить APK")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search
            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; viewModel.setSearch(it) },
                placeholder = { Text("Поиск по версии, ветке...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Filters
            FilterChipsRow(
                filters = filters,
                onChannelSelected = viewModel::setChannel,
                onEnvironmentSelected = viewModel::setEnvironment,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            when {
                builds.loadState.refresh is LoadState.Loading -> LoadingScreen()
                builds.itemCount == 0 -> EmptyScreen("Сборок не найдено")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(builds.itemCount) { index ->
                        builds[index]?.let { build ->
                            BuildCard(build = build, onClick = { onBuildClick(build.id) })
                        }
                    }
                    if (builds.loadState.append is LoadState.Loading) {
                        item { CircularProgressIndicator(Modifier.fillMaxWidth().wrapContentWidth()) }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 8: Создать `ProjectsScreen.kt` и `ProjectsViewModel.kt`**

Аналогично `HomeScreen`. `ProjectsViewModel` загружает `GetProjectsUseCase()`, показывает список проектов как `ElevatedCard` с названием, package name и иконкой. FAB — если роль пользователя `uploader` или `admin` (роль получать из DataStore UserPreferences).

- [ ] **Step 9: Создать `BrowseModule.kt`** — биндинг `ProjectRepositoryImpl` и `BuildRepositoryImpl`. Обе реализации используют `ApiService` + `BuildDao` (Room-first, API-triggered refresh).

- [ ] **Step 10: Запустить тесты**

```bash
cd android && ./gradlew :feature:browse:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add android/feature/browse/
git commit -m "feat(browse): add ProjectsScreen, BuildsScreen with Paging3, filters, search"
```

---

---

## Task 11: feature/build-detail — Detail Screen, Download, Install Flow

**Files:**
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/GetBuildDetailUseCase.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/GetInstallStatusUseCase.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/DownloadBuildUseCase.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/VerifyChecksumUseCase.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/ReportInstallUseCase.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/data/DownloadWorker.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/di/BuildDetailModule.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt`
- Create: `android/feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailScreen.kt`
- Create: `android/feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/GetInstallStatusUseCaseTest.kt`
- Create: `android/feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/VerifyChecksumUseCaseTest.kt`
- Create: `android/feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/BuildDetailViewModelTest.kt`

- [ ] **Step 1: Написать тест для GetInstallStatusUseCase**

```kotlin
package com.appdist.feature.builddetail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.builddetail.domain.GetInstallStatusUseCase
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetInstallStatusUseCaseTest {

    private val pm = mockk<PackageManager>()
    private val useCase = GetInstallStatusUseCase(pm)

    @Test
    fun `returns NotInstalled when package not found`() {
        every { pm.getPackageInfo("com.test", any<Int>()) } throws
            PackageManager.NameNotFoundException()
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 24)
        assertEquals(InstallStatus.NotInstalled, status)
    }

    @Test
    fun `returns Installed when versionCodes match`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 100L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 24)
        assertEquals(InstallStatus.Installed(100L), status)
    }

    @Test
    fun `returns UpdateAvailable when available versionCode is higher`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 99L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 24)
        assertEquals(InstallStatus.UpdateAvailable(99L, 100L), status)
    }

    @Test
    fun `returns InstalledNewer when installed versionCode is higher`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 101L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 24)
        assertEquals(InstallStatus.InstalledNewer(101L, 100L), status)
    }

    @Test
    fun `returns Incompatible when minSdk exceeds device SDK`() {
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 999)
        assertEquals(InstallStatus.Incompatible, status)
    }
}
```

- [ ] **Step 2: Запустить тест, убедиться что падает**

```bash
cd android && ./gradlew :feature:build-detail:test
```

- [ ] **Step 3: Создать `GetInstallStatusUseCase.kt`**

```kotlin
package com.appdist.feature.builddetail.domain

import android.content.pm.PackageManager
import android.os.Build
import com.appdist.core.common.model.InstallStatus
import javax.inject.Inject

class GetInstallStatusUseCase @Inject constructor(
    private val packageManager: PackageManager
) {
    operator fun invoke(
        packageName: String,
        versionCode: Long,
        certFingerprint: String?,
        minSdk: Int
    ): InstallStatus {
        // Device SDK check
        if (minSdk > Build.VERSION.SDK_INT) {
            return InstallStatus.Incompatible
        }

        val installedInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } catch (e: PackageManager.NameNotFoundException) {
            return InstallStatus.NotInstalled
        }

        val installedVersionCode = installedInfo.longVersionCode

        // Signature check (if fingerprint provided)
        if (certFingerprint != null) {
            val signingInfo = installedInfo.signingInfo
            val installedFingerprint = signingInfo?.apkContentsSigners
                ?.firstOrNull()
                ?.let { sig ->
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(sig.toByteArray())
                        .joinToString(":") { "%02X".format(it) }
                }
            if (installedFingerprint != null && installedFingerprint != certFingerprint) {
                return InstallStatus.SignatureMismatch(installedFingerprint)
            }
        }

        return when {
            installedVersionCode == versionCode -> InstallStatus.Installed(installedVersionCode)
            installedVersionCode < versionCode -> InstallStatus.UpdateAvailable(installedVersionCode, versionCode)
            else -> InstallStatus.InstalledNewer(installedVersionCode, versionCode)
        }
    }
}
```

- [ ] **Step 4: Написать тест для VerifyChecksumUseCase**

```kotlin
package com.appdist.feature.builddetail

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.builddetail.domain.VerifyChecksumUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VerifyChecksumUseCaseTest {

    private val useCase = VerifyChecksumUseCase()

    @Test
    fun `returns Success when SHA256 matches`(@TempDir tempDir: File) {
        val file = File(tempDir, "test.apk").also { it.writeText("hello world") }
        // Precomputed SHA-256 of "hello world"
        val expected = "b94d27b9934d3e08a52e52d7da7dabfac484efe04294e576b536db7c7b4c8d7f"
        // Actually compute it:
        val actual = file.computeSha256()
        val result = useCase(file, actual)
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `returns ChecksumMismatch on wrong hash`(@TempDir tempDir: File) {
        val file = File(tempDir, "test.apk").also { it.writeText("hello world") }
        val result = useCase(file, "wrong_hash")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ChecksumMismatch)
    }

    private fun File.computeSha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytes = stream.read(buffer)
            while (bytes != -1) { digest.update(buffer, 0, bytes); bytes = stream.read(buffer) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 5: Создать `VerifyChecksumUseCase.kt`**

```kotlin
package com.appdist.feature.builddetail.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class VerifyChecksumUseCase @Inject constructor() {
    operator fun invoke(file: File, expectedSha256: String): Result<Unit> {
        val actual = file.computeSha256()
        return if (actual.equals(expectedSha256, ignoreCase = true)) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ChecksumMismatch(expectedSha256, actual))
        }
    }

    private fun File.computeSha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 6: Создать `DownloadWorker.kt`**

```kotlin
package com.appdist.feature.builddetail.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_BUILD_ID = "build_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_EXPECTED_CHECKSUM = "expected_checksum"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_LOADED = "bytes_loaded"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val buildId = inputData.getString(KEY_BUILD_ID) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val totalBytes = inputData.getLong(KEY_FILE_SIZE, 0L)

        val outputFile = File(applicationContext.cacheDir, "apk_downloads/$buildId.apk")
            .also { it.parentFile?.mkdirs() }

        try {
            downloadDao.upsert(DownloadEntity(buildId, null, "downloading", 0f, 0L, totalBytes, System.currentTimeMillis()))

            val request = Request.Builder().url(downloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.retry()
                val body = response.body ?: return@withContext Result.retry()

                outputFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                            setProgress(workDataOf(KEY_PROGRESS to progress, KEY_BYTES_LOADED to totalRead))
                            downloadDao.upsert(DownloadEntity(buildId, null, "downloading", progress, totalRead, totalBytes, System.currentTimeMillis()))
                        }
                    }
                }
            }

            downloadDao.upsert(DownloadEntity(buildId, outputFile.absolutePath, "completed", 1f, totalBytes, totalBytes, System.currentTimeMillis()))
            Result.success(workDataOf(KEY_OUTPUT_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            downloadDao.upsert(DownloadEntity(buildId, null, "failed", 0f, 0L, totalBytes, System.currentTimeMillis()))
            Result.retry()
        }
    }
}
```

- [ ] **Step 7: Создать `DownloadBuildUseCase.kt`**

```kotlin
package com.appdist.feature.builddetail.domain

import androidx.work.*
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.builddetail.data.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val bytesLoaded: Long) : DownloadState
    data object Verifying : DownloadState
    data class ReadyToInstall(val filePath: String) : DownloadState
    data class Failed(val reason: com.appdist.core.common.AppError) : DownloadState
}

class DownloadBuildUseCase @Inject constructor(
    private val workManager: WorkManager,
    private val verifyChecksum: VerifyChecksumUseCase
) {
    operator fun invoke(build: BuildUi, downloadUrl: String): Flow<DownloadState> {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                DownloadWorker.KEY_BUILD_ID to build.id,
                DownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
                DownloadWorker.KEY_EXPECTED_CHECKSUM to build.checksumSha256,
                DownloadWorker.KEY_FILE_SIZE to build.fileSize
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork(
            "download_${build.id}",
            ExistingWorkPolicy.KEEP,
            request
        )

        return workManager.getWorkInfoByIdFlow(request.id).map { info ->
            when (info?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                    val bytes = info.progress.getLong(DownloadWorker.KEY_BYTES_LOADED, 0L)
                    DownloadState.Downloading(progress, bytes)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val path = info.outputData.getString(DownloadWorker.KEY_OUTPUT_PATH)
                    if (path != null) {
                        val file = java.io.File(path)
                        val verified = verifyChecksum(file, build.checksumSha256)
                        if (verified.isSuccess) DownloadState.ReadyToInstall(path)
                        else DownloadState.Failed(com.appdist.core.common.AppError.ChecksumMismatch(build.checksumSha256, "mismatch"))
                    } else DownloadState.Failed(com.appdist.core.common.AppError.Unknown("No output path"))
                }
                WorkInfo.State.FAILED -> DownloadState.Failed(com.appdist.core.common.AppError.Unknown("Download failed"))
                else -> DownloadState.Idle
            }
        }
    }
}
```

- [ ] **Step 8: Создать `BuildDetailViewModel.kt`**

```kotlin
package com.appdist.feature.builddetail.ui

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.builddetail.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BuildDetailUiState(
    val build: BuildUi? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

sealed interface BuildDetailAction {
    data object DownloadOrInstallClicked : BuildDetailAction
    data object CancelDownload : BuildDetailAction
    data object CopyLinkClicked : BuildDetailAction
    data object RetryClicked : BuildDetailAction
}

sealed interface BuildDetailEffect {
    data class LaunchInstaller(val apkUri: Uri) : BuildDetailEffect
    data class ShowSnackbar(val message: String) : BuildDetailEffect
    data class CopyToClipboard(val text: String) : BuildDetailEffect
    data class ShowSignatureMismatchDialog(val installedFp: String) : BuildDetailEffect
    data class ShowDowngradeWarning(val installedCode: Long, val newCode: Long) : BuildDetailEffect
}

@HiltViewModel
class BuildDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBuildDetail: GetBuildDetailUseCase,
    private val getInstallStatus: GetInstallStatusUseCase,
    private val downloadBuild: DownloadBuildUseCase,
    private val reportInstall: ReportInstallUseCase
) : ViewModel() {

    val buildId: String = checkNotNull(savedStateHandle["buildId"])

    private val _state = MutableStateFlow(BuildDetailUiState())
    val state: StateFlow<BuildDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<BuildDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadBuild() }

    fun onAction(action: BuildDetailAction) = when (action) {
        BuildDetailAction.DownloadOrInstallClicked -> handleInstallAction()
        BuildDetailAction.CancelDownload -> cancelDownload()
        BuildDetailAction.CopyLinkClicked -> copyLink()
        BuildDetailAction.RetryClicked -> loadBuild()
    }

    private fun loadBuild() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getBuildDetail(buildId).collect { build ->
                val status = getInstallStatus(
                    build.packageName, build.versionCode, build.certFingerprint, build.minSdk
                )
                _state.update { it.copy(build = build.copy(installStatus = status), isLoading = false) }
            }
        }
    }

    private fun handleInstallAction() {
        val build = _state.value.build ?: return
        val downloadState = _state.value.downloadState

        if (downloadState is DownloadState.ReadyToInstall) {
            launchInstaller(downloadState.filePath, build)
            return
        }

        // Signature mismatch warning before download
        val status = build.installStatus
        if (status is InstallStatus.SignatureMismatch) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowSignatureMismatchDialog(status.installedFingerprint))
            }
            return
        }

        // Downgrade warning
        if (status is InstallStatus.InstalledNewer) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowDowngradeWarning(status.installed, status.available))
            }
            return
        }

        startDownload(build)
    }

    private fun startDownload(build: BuildUi) {
        viewModelScope.launch {
            // Get signed download URL
            val urlResult = getBuildDetail.getDownloadUrl(buildId)
            if (urlResult is Result.Error) {
                _effects.send(BuildDetailEffect.ShowSnackbar("Не удалось получить ссылку на скачивание"))
                return@launch
            }
            val url = (urlResult as Result.Success).data

            downloadBuild(build, url).collect { state ->
                _state.update { it.copy(downloadState = state) }
                if (state is DownloadState.ReadyToInstall) {
                    launchInstaller(state.filePath, build)
                }
                if (state is DownloadState.Failed) {
                    _effects.send(BuildDetailEffect.ShowSnackbar("Ошибка загрузки"))
                }
            }
        }
    }

    private fun launchInstaller(filePath: String, build: BuildUi) {
        viewModelScope.launch {
            // Fire-and-forget: report install on intent launch (see spec note)
            reportInstall(buildId)
            // Use FileProvider URI — Uri.fromFile() throws FileUriExposedException on API 24+
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,                         // inject ApplicationContext via @Inject
                "${context.packageName}.fileprovider",
                java.io.File(filePath)
            )
            _effects.send(BuildDetailEffect.LaunchInstaller(apkUri))
        }
    }

    private fun cancelDownload() { /* WorkManager cancel via buildId */ }

    private fun copyLink() {
        viewModelScope.launch {
            _effects.send(BuildDetailEffect.CopyToClipboard("appdist://builds/$buildId"))
        }
    }
}
```

- [ ] **Step 9: Создать `BuildDetailScreen.kt`**

```kotlin
package com.appdist.feature.builddetail.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildDetailScreen(
    buildId: String,
    onBack: () -> Unit,
    viewModel: BuildDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Permission launcher for REQUEST_INSTALL_PACKAGES
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Re-check permission after returning */ }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BuildDetailEffect.LaunchInstaller -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                is BuildDetailEffect.CopyToClipboard ->
                    clipboard.setText(AnnotatedString(effect.text))
                is BuildDetailEffect.ShowSnackbar -> { /* SnackbarHostState */ }
                is BuildDetailEffect.ShowSignatureMismatchDialog -> { /* show dialog */ }
                is BuildDetailEffect.ShowDowngradeWarning -> { /* show dialog */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.build?.let { "${it.projectName} v${it.versionName}" } ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { viewModel.onAction(BuildDetailAction.CopyLinkClicked) }) {
                    Icon(Icons.Default.Share, "Copy link")
                }}
            )
        },
        bottomBar = {
            // Sticky CTA
            state.build?.let { build ->
                StickyInstallBar(
                    build = build,
                    downloadState = state.downloadState,
                    onAction = { viewModel.onAction(it) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(padding))
            state.error != null -> ErrorScreen(state.error!!, onRetry = { viewModel.onAction(BuildDetailAction.RetryClicked) }, Modifier.padding(padding))
            state.build != null -> {
                val build = state.build!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    // Hero: app icon + name + version chips + version comparison
                    BuildHeroSection(build, Modifier.padding(16.dp))
                    // Changelog
                    if (!build.changelog.isNullOrBlank()) {
                        ChangelogSection(build.changelog, Modifier.padding(horizontal = 16.dp))
                    }
                    // Technical details (collapsible)
                    TechDetailsSection(build, Modifier.padding(16.dp))
                    Spacer(Modifier.height(80.dp)) // bottom bar space
                }
            }
        }
    }
}

@Composable
private fun StickyInstallBar(
    build: com.appdist.core.common.model.BuildUi,
    downloadState: DownloadState,
    onAction: (BuildDetailAction) -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            // Progress bar during download
            if (downloadState is DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Text(
                    "${downloadState.bytesLoaded / 1024 / 1024} MB скачано",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
            }

            val (buttonText, enabled) = when {
                downloadState is DownloadState.Downloading -> "Отмена" to true
                downloadState is DownloadState.Verifying -> "Проверка..." to false
                downloadState is DownloadState.ReadyToInstall -> "Установить" to true
                build.installStatus is InstallStatus.NotInstalled -> "Скачать и установить" to true
                build.installStatus is InstallStatus.UpdateAvailable -> "Обновить до v${build.versionName}" to true
                build.installStatus is InstallStatus.Installed -> "Установлена актуальная версия" to false
                build.installStatus is InstallStatus.InstalledNewer -> "Откатить до v${build.versionName}" to true
                build.installStatus is InstallStatus.Incompatible -> "Несовместимо с устройством" to false
                build.installStatus is InstallStatus.SignatureMismatch -> "Другая подпись — установить?" to true
                else -> "Скачать" to true
            }

            Button(
                onClick = {
                    if (downloadState is DownloadState.Downloading)
                        onAction(BuildDetailAction.CancelDownload)
                    else
                        onAction(BuildDetailAction.DownloadOrInstallClicked)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text(buttonText) }
        }
    }
}
```

- [ ] **Step 10: Создать `BuildHeroSection.kt`, `ChangelogSection.kt`, `TechDetailsSection.kt`**

`BuildHeroSection.kt` — иконка приложения (Coil), название, package name, status chips, блок сравнения версий (установлена → доступна). Если приложение не установлено — показать только доступную версию.

`ChangelogSection.kt` — заголовок "Что нового" + текст changelog. Markdown не нужен — просто `Text`.

`TechDetailsSection.kt` — сворачиваемая секция (`AnimatedVisibility`). Показывает: versionCode, branch, commitHash, fileSize, minSdk/targetSdk, ABIs, SHA-256 (первые 16 символов + "..."), uploader, дата загрузки.

- [ ] **Step 11: Создать `GetBuildDetailUseCase.kt` и `ReportInstallUseCase.kt`**

```kotlin
// GetBuildDetailUseCase.kt
package com.appdist.feature.builddetail.domain

import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBuildDetailUseCase @Inject constructor(
    private val repository: BuildDetailRepository
) {
    operator fun invoke(buildId: String): Flow<BuildUi> = repository.getBuildDetail(buildId)
    suspend fun getDownloadUrl(buildId: String): Result<String> = repository.getDownloadUrl(buildId)
}

// ReportInstallUseCase.kt
class ReportInstallUseCase @Inject constructor(
    private val repository: BuildDetailRepository,
    private val installHistoryDao: com.appdist.core.database.dao.InstallHistoryDao
) {
    suspend operator fun invoke(buildId: String): Result<Unit> {
        // Fire & forget — report on system installer launch, not on completion
        return repository.reportInstall(buildId)
    }
}
```

Create `BuildDetailRepository.kt` interface and `BuildDetailRepositoryImpl.kt` data class.

- [ ] **Step 12: Создать `BuildDetailModule.kt`**

```kotlin
package com.appdist.feature.builddetail.di

import android.content.pm.PackageManager
import com.appdist.feature.builddetail.data.BuildDetailRepositoryImpl
import com.appdist.feature.builddetail.domain.BuildDetailRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

@Module @InstallIn(ViewModelComponent::class)
abstract class BuildDetailModule {
    @Binds
    abstract fun bindBuildDetailRepository(impl: BuildDetailRepositoryImpl): BuildDetailRepository
}

@Module @InstallIn(ViewModelComponent::class)
object BuildDetailProviders {
    @Provides
    fun providePackageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager
}
```

- [ ] **Step 13: Запустить тесты**

```bash
cd android && ./gradlew :feature:build-detail:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 14: Commit**

```bash
git add android/feature/build-detail/
git commit -m "feat(build-detail): add BuildDetailScreen, DownloadWorker, install flow, checksum verification"
```

---

## Task 12: feature/upload — Upload APK

**Files:**
- Create: `android/feature/upload/src/main/kotlin/com/appdist/feature/upload/domain/ExtractApkMetadataUseCase.kt`
- Create: `android/feature/upload/src/main/kotlin/com/appdist/feature/upload/domain/UploadBuildUseCase.kt`
- Create: `android/feature/upload/src/main/kotlin/com/appdist/feature/upload/data/UploadWorker.kt`
- Create: `android/feature/upload/src/main/kotlin/com/appdist/feature/upload/ui/UploadViewModel.kt`
- Create: `android/feature/upload/src/main/kotlin/com/appdist/feature/upload/ui/UploadScreen.kt`

- [ ] **Step 1: Создать `ExtractApkMetadataUseCase.kt`**

```kotlin
package com.appdist.feature.upload.domain

import android.content.Context
import android.content.pm.PackageManager
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import java.io.File
import javax.inject.Inject

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int
)

class ExtractApkMetadataUseCase @Inject constructor(private val context: Context) {
    operator fun invoke(apkFile: File): Result<ApkMetadata> = try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_META_DATA
        ) ?: return Result.Error(AppError.Unknown("Cannot parse APK"))

        info.applicationInfo?.sourceDir = apkFile.absolutePath
        info.applicationInfo?.publicSourceDir = apkFile.absolutePath

        Result.Success(ApkMetadata(
            packageName = info.packageName,
            versionName = info.versionName ?: "unknown",
            versionCode = info.longVersionCode,
            minSdk = info.applicationInfo?.minSdkVersion ?: 1,
            targetSdk = info.applicationInfo?.targetSdkVersion ?: 1
        ))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown("APK parse error: ${e.message}"))
    }
}
```

- [ ] **Step 2: Создать `UploadBuildUseCase.kt`**

```kotlin
package com.appdist.feature.upload.domain

import android.net.Uri
import androidx.work.*
import com.appdist.feature.upload.data.UploadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface UploadState {
    data class Uploading(val progress: Float) : UploadState
    data object Success : UploadState
    data class Failed(val error: com.appdist.core.common.AppError) : UploadState
}

class UploadBuildUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    operator fun invoke(
        apkUri: Uri,
        channel: String,
        changelog: String,
        metadata: ApkMetadata
    ): Flow<UploadState> {
        // Copy content URI to cache before enqueueing (Worker needs a real file path)
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                UploadWorker.KEY_APK_PATH to apkUri.path,   // caller must resolve to File path first
                UploadWorker.KEY_PROJECT_ID to "",           // filled by UploadViewModel from selected project
                UploadWorker.KEY_CHANNEL to channel,
                UploadWorker.KEY_CHANGELOG to changelog,
                UploadWorker.KEY_BASE_URL to com.appdist.app.BuildConfig.API_BASE_URL
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork("upload_apk", ExistingWorkPolicy.REPLACE, request)

        return workManager.getWorkInfoByIdFlow(request.id).map { info ->
            when (info?.state) {
                WorkInfo.State.RUNNING -> UploadState.Uploading(
                    info.progress.getFloat(UploadWorker.KEY_PROGRESS, 0f)
                )
                WorkInfo.State.SUCCEEDED -> UploadState.Success
                WorkInfo.State.FAILED -> UploadState.Failed(
                    com.appdist.core.common.AppError.Unknown("Upload failed")
                )
                else -> UploadState.Uploading(0f)
            }
        }
    }
}
```

- [ ] **Step 3: Создать `UploadWorker.kt`**

```kotlin
package com.appdist.feature.upload.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_APK_PATH = "apk_path"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_CHANNEL = "channel"
        const val KEY_CHANGELOG = "changelog"
        const val KEY_PROGRESS = "progress"
        const val KEY_BASE_URL = "base_url"  // passed by UploadBuildUseCase from BuildConfig.API_BASE_URL
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkPath = inputData.getString(KEY_APK_PATH) ?: return@withContext Result.failure()
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return@withContext Result.failure()
        val channel = inputData.getString(KEY_CHANNEL) ?: "internal"
        val changelog = inputData.getString(KEY_CHANGELOG) ?: ""

        val apkFile = File(apkPath)
        if (!apkFile.exists()) return@withContext Result.failure()

        try {
            // Multipart upload — OkHttp directly (Retrofit не поддерживает progress)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("project_id", projectId)
                .addFormDataPart("channel", channel)
                .addFormDataPart("changelog", changelog)
                .addFormDataPart(
                    "apk", apkFile.name,
                    apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType())
                )
                .build()

            // BASE_URL injected via @Named qualifier from NetworkModule, not via inputData
            val baseUrl = inputData.getString(KEY_BASE_URL)
                ?: return@withContext Result.failure()

            val request = Request.Builder()
                .url("${baseUrl}api/v1/builds/upload")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) Result.success()
            else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

- [ ] **Step 3: Создать `UploadViewModel.kt`**

```kotlin
package com.appdist.feature.upload.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.feature.upload.domain.ApkMetadata
import com.appdist.feature.upload.domain.ExtractApkMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val selectedApkUri: Uri? = null,
    val extractedMetadata: ApkMetadata? = null,
    val channel: String = "internal",
    val changelog: String = "",
    val uploadProgress: Float? = null,   // null = not uploading, 0..1 = progress
    val isSuccess: Boolean = false,
    val error: AppError? = null
)

sealed interface UploadAction {
    data class ApkSelected(val uri: Uri) : UploadAction
    data class ChannelChanged(val channel: String) : UploadAction
    data class ChangelogChanged(val text: String) : UploadAction
    data object UploadClicked : UploadAction
}

sealed interface UploadEffect {
    data object UploadSuccess : UploadEffect
    data class ShowError(val message: String) : UploadEffect
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val extractMetadata: ExtractApkMetadataUseCase,
    private val uploadBuild: com.appdist.feature.upload.domain.UploadBuildUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    private val _effects = Channel<UploadEffect>()
    val effects = _effects.receiveAsFlow()

    fun onAction(action: UploadAction) = when (action) {
        is UploadAction.ApkSelected -> extractAndSetMetadata(action.uri)
        is UploadAction.ChannelChanged -> _state.update { it.copy(channel = action.channel) }
        is UploadAction.ChangelogChanged -> _state.update { it.copy(changelog = action.text) }
        UploadAction.UploadClicked -> upload()
    }

    private fun extractAndSetMetadata(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(selectedApkUri = uri, extractedMetadata = null, error = null) }
            // Step 1: copy content:// URI to a temp file (PackageManager can't read content URIs directly)
            val tempFile = withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "upload_preview.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }
            // Step 2: extract metadata from temp file
            when (val result = extractMetadata(tempFile)) {
                is com.appdist.core.common.Result.Success ->
                    _state.update { it.copy(extractedMetadata = result.data) }
                is com.appdist.core.common.Result.Error ->
                    _state.update { it.copy(error = result.error) }
            }
        }
    }

    private fun upload() {
        val uri = _state.value.selectedApkUri ?: return
        val metadata = _state.value.extractedMetadata ?: return
        _state.update { it.copy(uploadProgress = 0f) }
        viewModelScope.launch {
            uploadBuild(uri, _state.value.channel, _state.value.changelog, metadata)
                .collect { state ->
                    when (state) {
                        is UploadState.Uploading -> _state.update { it.copy(uploadProgress = state.progress) }
                        is UploadState.Success -> {
                            _state.update { it.copy(isSuccess = true, uploadProgress = null) }
                            _effects.send(UploadEffect.UploadSuccess)
                        }
                        is UploadState.Failed -> {
                            _state.update { it.copy(uploadProgress = null, error = state.error) }
                            _effects.send(UploadEffect.ShowError("Ошибка загрузки"))
                        }
                    }
                }
        }
    }
}
```

- [ ] **Step 4: Создать `UploadScreen.kt`**

Экран содержит:
- Кнопка выбора APK (`ActivityResultContracts.GetContent("application/vnd.android.package-archive")`)
- После выбора — отображение метаданных (`packageName`, `versionName`, `versionCode`)
- Выбор channel (dropdown: `internal`, `alpha`, `beta`, `rc`)
- Поле changelog (`OutlinedTextField`, multiline)
- Кнопка "Загрузить" — активна только если APK выбран
- Progress bar во время загрузки

- [ ] **Step 5: Commit**

```bash
git add android/feature/upload/
git commit -m "feat(upload): add UploadScreen, ExtractApkMetadataUseCase, UploadWorker"
```

---

## Task 13: feature/mine — Install History + Active Downloads

**Files:**
- Create: `android/feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineViewModel.kt`
- Create: `android/feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineScreen.kt`

- [ ] **Step 1: Создать `MineViewModel.kt`**

```kotlin
package com.appdist.feature.mine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MineUiState(
    val installHistory: List<InstallHistoryEntity> = emptyList(),
    val activeDownloads: List<DownloadEntity> = emptyList()
)

@HiltViewModel
class MineViewModel @Inject constructor(
    installHistoryDao: InstallHistoryDao,
    downloadDao: DownloadDao
) : ViewModel() {

    val state: StateFlow<MineUiState> = combine(
        installHistoryDao.getAll(),
        downloadDao.getActiveDownloads()
    ) { history, downloads ->
        MineUiState(installHistory = history, activeDownloads = downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MineUiState())
}
```

- [ ] **Step 2: Создать `MineScreen.kt`**

Экран с двумя секциями:
1. **Активные загрузки** — список `DownloadEntity` со статусом `downloading`. Показывать прогресс (`LinearProgressIndicator`). Если загрузок нет — скрыть секцию.
2. **История установок** — `LazyColumn` из `InstallHistoryEntity`: название пакета, версия, дата установки. Тап — навигация на `BuildDetailScreen` по `buildId`.

- [ ] **Step 3: Commit**

```bash
git add android/feature/mine/
git commit -m "feat(mine): add MineScreen with install history and active downloads"
```

---

## Task 14: feature/settings — Settings, Permissions, FCM + SyncWorker

**Files:**
- Create: `android/feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/SettingsViewModel.kt`
- Create: `android/feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/SettingsScreen.kt`
- Create: `android/feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/PermissionsScreen.kt`
- Create: `android/feature/settings/src/main/kotlin/com/appdist/feature/settings/data/SyncBuildsWorker.kt`
- Create: `android/feature/settings/src/main/kotlin/com/appdist/feature/settings/data/AppDistFirebaseMessagingService.kt`

- [ ] **Step 1: Создать `SyncBuildsWorker.kt`**

```kotlin
package com.appdist.feature.settings.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncBuildsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ApiService,
    private val buildDao: BuildDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "sync_builds_periodic"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncBuildsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val response = api.getRecentBuilds(50)
            if (response.isSuccessful) {
                val builds = response.body()?.map { dto ->
                    BuildEntity(
                        id = dto.id, projectId = dto.projectId,
                        versionName = dto.versionName, versionCode = dto.versionCode,
                        channel = dto.channel, environment = dto.environment,
                        changelog = dto.changelog, fileSize = dto.fileSize,
                        checksumSha256 = dto.checksumSha256, status = dto.status,
                        isLatestInChannel = dto.isLatestInChannel,
                        uploadDate = dto.uploadDate, uploaderName = dto.uploaderName,
                        cachedAt = System.currentTimeMillis()
                    )
                } ?: emptyList()
                buildDao.upsertBuilds(builds)
                // Stale cache cleanup: remove entries older than 7 days
                buildDao.deleteStale(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

- [ ] **Step 2: Создать `AppDistFirebaseMessagingService.kt`**

```kotlin
package com.appdist.feature.settings.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppDistFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var workManager: WorkManager

    override fun onNewToken(token: String) {
        // TODO: send token to backend via UpdateMe API
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Data message — trigger sync
        if (message.data.containsKey("new_build")) {
            SyncBuildsWorker.schedule(workManager)
        }

        // Notification message — show push
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "AppDistribution",
                body = notification.body ?: "Новая сборка доступна",
                buildId = message.data["build_id"]
            )
        }
    }

    private fun showNotification(title: String, body: String, buildId: String?) {
        val channelId = "appdist_builds"
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Новые сборки", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            buildId?.let { putExtra("build_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
```

Добавить в `AndroidManifest.xml` внутри `<application>`:
```xml
<service
    android:name="com.appdist.feature.settings.data.AppDistFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

- [ ] **Step 3: Создать `SettingsScreen.kt` и `SettingsViewModel.kt`**

`SettingsViewModel.kt` — управляет: отображением user info (email, role из DataStore), переключателем уведомлений (`UserPreferencesStore.notificationsEnabled`), выход из аккаунта (`AuthRepository.logout()`).

`SettingsScreen.kt` содержит:
- Блок профиля: email, роль
- Switch "Push-уведомления"
- Кнопка "Разрешения установки" → `onPermissionsClick()`
- Кнопка "Выйти" (с подтверждением через `AlertDialog`)

- [ ] **Step 4: Создать `PermissionsScreen.kt`**

```kotlin
package com.appdist.feature.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Разрешения для установки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Для установки тестовых APK Android требует разрешение " +
            "«Установка из неизвестных источников» для AppDistribution.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "1. Нажмите кнопку ниже\n" +
            "2. Найдите AppDistribution в списке\n" +
            "3. Включите «Разрешить установку приложений»\n" +
            "4. Вернитесь назад",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Открыть настройки") }
    }
}
```

- [ ] **Step 5: Запланировать SyncBuildsWorker при старте приложения**

В `AppApplication.onCreate()` добавить после Timber.plant:
```kotlin
// Start periodic sync after app init
val workManager = WorkManager.getInstance(this)
SyncBuildsWorker.schedule(workManager)
```

- [ ] **Step 6: Запустить все тесты**

```bash
cd android && ./gradlew test
```

Expected: BUILD SUCCESSFUL, все unit-тесты зелёные.

- [ ] **Step 7: Финальная сборка**

```bash
cd android && ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL, APK создан в `app/build/outputs/apk/debug/`.

- [ ] **Step 8: Commit**

```bash
git add android/feature/settings/ android/app/src/main/kotlin/com/appdist/app/AppApplication.kt
git commit -m "feat(settings): add SettingsScreen, PermissionsScreen, FCM service, SyncBuildsWorker"
```

---

## Финальный чеклист MVP

После выполнения всех задач убедиться:

- [ ] `./gradlew test` — все unit-тесты зелёные
- [ ] `./gradlew :app:assembleDebug` — APK собирается
- [ ] Login flow: email → OTP → Home (проверить с запущенным backend)
- [ ] Build list: видны сборки из `ProjectsScreen` → `BuildsScreen`
- [ ] Build Detail: метаданные, download progress, install flow
- [ ] Install status: правильно определяется для установленных приложений
- [ ] Checksum: проверка SHA-256 после скачивания
- [ ] Push notification: FCM-токен регистрируется, приходит уведомление
- [ ] Offline: кешированные данные видны без сети
- [ ] Deep link: `appdist://builds/{id}` открывает BuildDetailScreen
- [ ] FileProvider: APK передаётся системному installer'у без SecurityException

---

## Компромиссы и следующие шаги

### В MVP не вошло (Phase 2)
- Download resume через Range header (сейчас retry с начала)
- Invite links / tester groups
- Build comparison screen
- Pinned builds / favorite projects
- Analytics dashboard
- Paging в build detail history
- Notification settings per-project

### Известные ограничения
- `AuthInterceptor` использует `runBlocking` — это приемлемо для однократного refresh, но при высокой частоте запросов может вызвать contention. В Phase 2 заменить на mutex-based token refresh.
- `ReportInstallUseCase` репортит на запуск system installer, не на успешную установку — это стандартное ограничение Android.
- `SyncBuildsWorker` синхронизирует recent builds глобально. В Phase 2 добавить per-project подписки.
