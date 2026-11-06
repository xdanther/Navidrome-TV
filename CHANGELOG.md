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
