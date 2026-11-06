# Architecture

## Shape of the app

The app ships as a single Gradle module (`:app`) with strict package layering. A
multi-module build would add ceremony without a second consumer of these APIs; the
package boundaries are enforced by review and by dependency rules below rather than
by Gradle.

```
presentation ──▶ domain ◀── data
      │            ▲         │
      │            └── playback
      └── core (models, errors, design system, formatting) ◀── everyone
```

Rules:

- `core` depends on nothing app-specific and holds pure models, error mapping,
  formatting and the Compose design system.
- `domain` holds repository contracts and use cases; it has no Android or Retrofit
  types.
- `data` implements the contracts: Subsonic API, DTOs, mappers, Room, DataStore.
- `playback` owns Media3 and implements the player-facing part of the domain.
- `presentation` only talks to `domain`/`playback` interfaces and `core` models.

## Connection and credentials

- `SubsonicAuth` implements token authentication: a random salt per request and
  `token = md5(password + salt)`, plus `v`, `c` and `f=json` parameters.
- `SubsonicClientProvider` keeps the active session, builds the Retrofit client and
  exposes credential-aware URL builders for streaming and cover art. Stream and
  artwork URLs carry the token, and are never logged.
- `SecureCredentialStore` encrypts the base URL, username and password with AES/GCM
  using a key generated in the Android Keystore. Nothing is stored in plain
  preferences, and no secret is written to logs.
- `restoreSession()` verifies a stored session on launch but keeps it when the server
  is merely unreachable, so history and cached artwork still work offline. An
  authentication failure clears it.

## Data and caching

- `SubsonicService` wraps every call in the shared envelope, maps Subsonic error codes
  to `AppError`/`ErrorKind` and keeps cancellation intact.
- `SubsonicMapper` converts DTOs to domain models. It is free of Android and network
  types, which is why it is directly unit tested.
- `MusicRepositoryImpl` caches album lists in Room and falls back to the cache when a
  request fails, marking the data as cached. Artist details are enriched with top
  songs and biography when the server supports it; failures there are non-fatal.
- `CacheManager` exposes cache sizes/clearing for Settings.

## Playback

```
Player (ExoPlayer) ◀── MediaLibraryService ◀── MediaController ◀── PlaybackRepository
                                                                    │
                                                        PlaybackState (StateFlow)
                                                                    │
                                                               Compose UI
```

- `PlaybackService` is a `MediaLibraryService` so the app also works as a media
  browser for the system and for Assistant-style search.
- `PlaybackRepository` is the single source of truth for the UI: it connects a
  `MediaController`, mirrors queue/index/position/state into an immutable
  `PlaybackState`, restores the persisted queue paused, and triggers scrobbles.
- `ScrobblePolicy` decides when a play counts (half the track or four minutes,
  whichever is first, with sane floors), then history and `scrobble` are written.
- `QueueStore` persists the queue as JSON in DataStore so process death does not lose
  the session.

## Presentation

- One `NavHost`, top-level destinations with saved state, and a persistent top bar
  and mini player outside the nav graph.
- Every screen has a view model exposing a single immutable UI state plus one-shot
  events for navigation and toasts.
- TV interaction goes through the design system: `TvClickable`, `FocusableRow`,
  `ActionButton`, `CircleIconButton`, `FilterChip`. Focus is expressed with a border,
  a small scale and elevation instead of size changes, so rows never jump.
- Screens reserve a safe content bottom inset for the mini player.

## Party Mode

- `PartyModeManager` owns the session: pairing code, guest sessions with expiring
  tokens, rate limits, pending requests and approval.
- `PartyGuestServer` (NanoHTTPD) serves one static page and four JSON endpoints on
  the LAN. It can only call `PartyApi`; it never sees Subsonic credentials.
- Approval adds the track through `PlaybackRepository.addToQueue`, so Party Mode has
  no separate playback path.
- Guest devices only ever receive track metadata the host resolved itself.

## Error handling

`DataResult<T>` wraps success or an `AppError` with an `ErrorKind` (network, auth,
not found, server, not configured, unknown). `toAppError()` maps HTTP and Subsonic
codes once, at the data layer, and the UI renders messages from `ErrorKind` plus a
retry action. No screen swallows an error silently.

## Testing strategy

Pure logic is unit tested on the JVM: auth token generation, URL handling, DTO
mapping, lyrics selection, scrobble thresholds and formatting. UI, playback and
networking require a device/emulator and a real server; the CI workflow builds the APK
and runs lint, but does not run instrumentation tests.
