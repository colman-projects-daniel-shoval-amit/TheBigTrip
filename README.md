# TheBigTrip

An Android app for sharing and discovering travel locations. Users create posts pinned to GPS coordinates; posts appear as map markers visible to all users.

## Features

- **Authentication** — register, login, forgot password via Firebase Auth
- **Map View** — Google Maps with clustered markers for all trip posts; centers on device location on launch; tap a single marker or cluster to browse posts; camera position preserved when returning from post details
- **Post Details** — full-screen detail view with image, title, description, and location; back-navigates to map
- **Create Post** — publish a trip post with title, description, and optional image; location picked via Places Autocomplete search (Places API New) or GPS auto-fill button
- **Profile** — view account info, upload/change profile picture, logout

## Requirements

- Android SDK 33+ (minSdk 33)
- Google Maps API key in `local.properties`:
  ```
  MAPS_API_KEY=your_key_here
  ```
- `google-services.json` in `app/` (Firebase project config)

## Setup

1. Clone repo
2. Add `local.properties` with `MAPS_API_KEY`
3. Add `app/google-services.json` from Firebase Console
4. Run `./gradlew assembleDebug`

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Fragments + View Binding + Navigation Component |
| Maps | Google Maps SDK 18.2.0 + Maps Utils 3.8.0 (clustering) |
| Places | Google Places SDK 3.5.0 (Places API New — Autocomplete) |
| Auth | Firebase Authentication |
| Remote DB | Firebase Firestore |
| Image Storage | Firebase Storage |
| Local DB | Room 2.6.1 |
| Async | Kotlin Coroutines 1.8.0 |
| Image Loading | Picasso |

## Architecture

MVVM-adjacent with Repository pattern. Every write syncs to both Firestore (remote) and Room (local cache).

```
AuthActivity  ──►  MainActivity
                      ├── MapFragment                      (clustered map; tap single → PostDetailsFragment; tap cluster → bottom sheet)
                      │     ├── ClusterPostsBottomSheetFragment  (list of posts at same location; tap → PostDetailsFragment)
                      │     └── PostDetailsFragment             (full post view; not a bottom-nav tab; back → map restores camera)
                      ├── CreatePostFragment     (new post with image + GPS auto-fill)
                      └── ProfileFragment        (account, profile picture upload, logout)
```

Repositories (`PostRepository.shared`, `UserRepository.shared`) are singletons that coordinate Firestore + Room. Fragments call repositories via `lifecycleScope` + `Dispatchers.IO`.

**Read pattern:** local Room cache checked first for instant load; Firestore fetched as fallback (or always, for list queries that must be fresh).
