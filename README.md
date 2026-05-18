# TheBigTrip

An Android app for sharing and discovering travel locations. Users create posts pinned to GPS coordinates; posts appear as map markers visible to all users.

## Features

- **Authentication** — register, login, forgot password via Firebase Auth
- **Map View** — Google Maps with markers for all trip posts; taps on user location on launch
- **Create Post** — publish a trip post with title, description, GPS coordinates, and optional image
- **Profile** — view account info, logout

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
| Maps | Google Maps SDK 18.2.0 |
| Auth | Firebase Authentication |
| Remote DB | Firebase Firestore |
| Local DB | Room 2.6.1 |
| Async | Kotlin Coroutines 1.8.0 |

## Architecture

MVVM-adjacent with Repository pattern. Every write syncs to both Firestore (remote) and Room (local cache).

```
AuthActivity  ──►  MainActivity
                      ├── MapFragment        (browse posts on map)
                      ├── CreatePostFragment  (new post)
                      └── ProfileFragment    (account + logout)
```

Repositories (`PostRepository.shared`, `UserRepository.shared`) are singletons that coordinate Firestore + Room. Fragments call repositories via `lifecycleScope` + `Dispatchers.IO`.
