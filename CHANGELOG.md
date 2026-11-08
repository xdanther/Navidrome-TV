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

- Focus is now unmistakable on the light stage: a 6dp two-tone ring (white band,
  dark edge) is drawn inside the element's own rounded shape, so nothing clips it
  away, and a focused item also darkens to an ink pill in the navigation and chips.
  Previously the ring was a thin 3dp outline outside the bounds, where scroll rows and
  the navigation capsule cut it off.
- The ambient backdrop no longer carries a white highlight: the gradient is fully
  tinted from sand to sage to sky with warm and cool blooms, so the screen edges no
  longer read as white margins.
- Home carousels bleed to the screen edge while their shelf titles keep the
  overscan margin, so covers run off the edge like a TV shelf instead of stopping
  short inside a narrow column.
- Replaced the dark charcoal chrome with a light, ambient stage: a soft
  sand-to-sky gradient behind every screen, translucent white glass for the
  navigation capsule and mini player, ink text with bold shelf titles, pastel
  genre tiles and a dark 3dp focus ring that stays readable on light surfaces.
  Navigation is now a centered floating capsule whose current destination is a
  solid white pill, and the utility icons (search, settings, party) close it.
  Now Playing keeps its dark, artwork-driven screen so playback reads as its own
  space.
- Focus now grows the element to 1.10x (the top of the TV guidance range) and draws
  its outline outside the element, instead of a 1.05x scale with an inset outline
  that read like a shadow. The translucent white veil over focused covers is gone and
  grid gutters were widened so scaled cards do not collide.

- Reworked the visual language to the Android TV design guidance and retired the
  purple/red art direction: neutral charcoal Material surfaces, one Google-blue
  accent reserved for state, Roboto TV type scale, 48dp/27dp overscan-safe margins,
  and 12dp card corners. Focus is now a 1.05x scale plus a 2dp white outline and a
  surface change, and the top navigation is TV tabs with an accent underline.
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

- The album screen overflowed a 960x540dp television layout: a fixed 360dp cover
  column pushed the title, artist and metadata past the bottom edge, and the
  five-button action row was squeezed into a tall empty pill. The cover now sizes
  itself from the available height, album actions are two labelled buttons plus icon
  controls, and the track list scrolls with room for the mini player.
- Release APKs no longer change signing certificate on every CI run, which made
  Android refuse an update over an already installed build. CI now restores a
  persisted keystore, and the pipeline fails if the APK's certificate does not match
  it. Repository secrets can supply a permanent key instead.
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
