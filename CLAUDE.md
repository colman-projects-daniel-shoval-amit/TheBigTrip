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
- `PostDetailsFragment` is a non-tab nav destination pushed from `MapFragment` when a map marker is tapped. Bottom nav hides via `addOnDestinationChangedListener` in `MainActivity`.
- `ClusterPostsBottomSheetFragment` (`map/`) is a `BottomSheetDialogFragment` shown via `MapFragment.childFragmentManager` when a cluster marker is tapped. It lists all posts at the clustered location. Navigation from it uses `requireParentFragment().findNavController()` — not its own NavController — because the dialog is attached to `childFragmentManager` and its `parentFragmentManager` differs from the one `setFragmentResultListener` subscribes to. Posts are passed via a `companion object pendingPosts: MutableMap<String, List<Post>>` keyed by UUID (because `Post` contains `LatLng`, which is not `Parcelable`).

**Layers:**
- `Auth/` — login/register/forgot-password fragments, all inside `AuthActivity`
- `data/models/` — Firebase operation wrappers (`FirebasePostModel`, `FirebaseUserModel`)
- `data/repository/` — singleton repositories (`PostRepository.shared`, `UserRepository.shared`) that coordinate Firebase Firestore + Room writes
- `dao/` — Room database (`AppLocalDb.db` singleton), DAOs for `User` and `Post`
- `model/Post.kt`, `data/user/User.kt` — Room entities with `toJson`/`fromJson` for Firestore serialization
- `utils/ImageUtil.kt` — Firebase Storage upload helpers; resizes to 1024px max before upload
- `base/TheBigTrip.kt` — Application class, holds global `context`

**Storage pattern:** Every write goes to both Firestore (remote) and Room (local cache) inside a single `withContext(Dispatchers.IO)` block. Repositories use `suspend` functions; callers use `lifecycleScope`.

**Read pattern:** For single-item lookups (`getPostById`, `getUserById`), check Room first and return immediately if found; fetch Firestore only on cache miss and back-fill Room. For list queries (`getAllPosts`), always fetch Firestore (fresh data required for map markers) and update Room cache; fall back to Room only on network failure.

**Image picking:** All image selection uses `ActivityResultContracts.PickVisualMedia` (modern system photo picker, no runtime permission needed). Images are uploaded to Firebase Storage via `ImageUtil.uploadImage` before the post/user record is saved.

**View binding** is enabled project-wide — all fragments/activities use binding, not `findViewById`. Activities use `XxxBinding.inflate(layoutInflater)`; fragments use the `_binding` nullable pattern with null in `onDestroyView`.

**Location:** `FusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)` is used instead of `lastLocation` to avoid null on cold starts. Permission is requested via `ActivityResultContracts.RequestPermission`. Two-stage fallback: `getCurrentLocation` → on null result call `lastLocation` → on null result show Snackbar. All async callbacks guard with `!isAdded` / `_binding == null` checks. Never use `setOnMapLoadedCallback` to center the camera — it fires in ~1s and always wins the race against `getCurrentLocation` (several seconds), causing a hardcoded-coordinate flash.

**Map camera persistence:** `MapFragment` holds `private var savedCameraPosition: CameraPosition?`. Before navigating to `PostDetailsFragment` (single item click or cluster bottom sheet item click), the current `googleMap.cameraPosition` is captured. In `onMapReady`, if `savedCameraPosition != null` the camera is restored instantly via `moveCamera`; `enableUserLocation()` is skipped. This survives the `onDestroyView`/`onCreateView` cycle because the fragment instance stays on the back stack. On fresh launch (`savedCameraPosition == null`) the normal GPS-centering flow runs.

## Key Tech

- Kotlin 2.0.21, compileSdk 36, minSdk 33
- Room 2.6.1 (KAPT for annotation processing)
- Firebase BOM 34.9.0 (Auth, Firestore, Storage, Analytics)
- Google Maps 18.2.0 + Maps Utils 3.8.0 (`ClusterManager`, `ClusterItem`) + Play Services Location 21.0.1
- Navigation Component 2.9.7 (single-activity nav graph; no Safe Args plugin — args passed as Bundle)
- Kotlin Coroutines 1.8.0
- Picasso (image loading; `CircleTransform` for profile pictures)
- Dependency versions managed via `gradle/libs.versions.toml`
