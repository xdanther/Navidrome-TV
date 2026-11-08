# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Initial Android TV client for Navidrome/Subsonic: onboarding with local server
  discovery by URL, encrypted credential storage and session restore.
- Home, New, Browse, Library, Search, Album, Artist, Playlist, Genre, Settings and
  immersive Now Playing screens with D-pad-first focus handling.
- Media3 playback service with queue management, shuffle, repeat, streaming-quality
  presets, persisted queue and media-session search.
- Lyrics (synchronized and plain) with a clear message when the server has none.
- Local listening history, real server scrobbles and favorites.
- Playlist creation, rename, deletion and track add/remove.
- Party Mode: LAN guest page with pairing code, expiring tokens, rate limits and
  host approval before tracks reach the queue.
- Room metadata cache with offline fallback, DataStore settings and cache controls.
- JVM unit tests for auth, mapping, scrobble policy and formatting.
- GitHub Actions workflow building, testing and linting the app.

### Changed

- Redesigned the 10-foot presentation: focus is now a filled, high-contrast
  treatment with an animated ring and 1.06x scale instead of a thin outline, and the
  top navigation collapses with an animation once Home is scrolled past its first
  row (Back reveals it again).
- Home and every carousel now use square 200dp artwork cards with 24dp gaps; the
  oversized landscape hero row was removed so 5-7 covers fit at once. Playlist tiles
  are square too.
- Now Playing uses a balanced two-column layout with square artwork, a prominent
  play/pause control and RTL-aware synchronised lyrics. Playback progress is now read
  through `derivedStateOf`, so position ticks no longer recompose the artwork
  background; artwork image requests and extracted palettes are cached per cover,
  which removes the background flicker.
- CI now builds, tests and lints the release variant only, and uploads the
  R8-minified release APK. Release signing uses the debug key unless the
  `RELEASE_*` environment variables supply a real keystore.

### Fixed

- Subsonic responses are unwrapped once in `SubsonicService`, so every repository
  call sees the payload instead of the envelope.
- Cover-art fallbacks only add the `al-`/`ar-` prefix when the id does not carry it,
  instead of producing unusable `al-al-…` artwork ids.
- Media3 1.4.1 library callbacks match the real signatures (`onSearch` returns
  `LibraryResult<Void>`, `onGetSearchResult` takes page and page size before
  params), and the opt-in track-format calls are annotated for lint.
- Onboarding exposes `canSubmit` on its state instead of an extension the screen
  could not see.
- CI republishes Gradle and lint findings as annotations so failures are readable.
