# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Run single test class
./gradlew test --tests "com.dsa.thebigtrip.ExampleUnitTest"
```

Maps API key must be in `local.properties` as `MAPS_API_KEY=...` — the Secrets Gradle plugin injects it into the manifest.

## Architecture

MVVM-adjacent with Repository pattern and dual storage (Firebase + Room).

**App flow:**
- `AuthActivity` is the launcher. Checks Firebase Auth state on start; redirects to `MainActivity` if already logged in.
- `MainActivity` hosts bottom navigation with three fragments: `MapFragment`, `CreatePostFragment`, `ProfileFragment`.

**Layers:**
- `Auth/` — login/register/forgot-password fragments, all inside `AuthActivity`
- `data/models/` — Firebase operation wrappers (`FirebasePostModel`, `FirebaseUserModel`)
- `data/repository/` — singleton repositories (`PostRepository.shared`, `UserRepository.shared`) that coordinate Firebase Firestore + Room writes
- `dao/` — Room database (`AppLocalDb.db` singleton), DAOs for `User` and `Post`
- `model/Post.kt`, `data/user/User.kt` — Room entities with `toJson`/`fromJson` for Firestore serialization
- `base/TheBigTrip.kt` — Application class, holds global `context`

**Storage pattern:** Every write goes to both Firestore (remote) and Room (local cache). Repositories use `suspend` functions; callers use `lifecycleScope` + `Dispatchers.IO`.

**View binding** is enabled project-wide — all fragments/activities use binding, not `findViewById`.

## Key Tech

- Kotlin 2.0.21, compileSdk 36, minSdk 33
- Room 2.6.1 (KAPT for annotation processing)
- Firebase BOM 34.9.0 (Auth, Firestore, Analytics)
- Google Maps 18.2.0 + Play Services Location 21.0.1
- Navigation Component 2.9.7 (single-activity nav graph)
- Kotlin Coroutines 1.8.0
- Dependency versions managed via `gradle/libs.versions.toml`
