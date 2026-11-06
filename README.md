# Navidrome TV

A native Android TV client for [Navidrome](https://www.navidrome.org/) and any
Subsonic-compatible server, built for a D-pad remote and a 10-foot screen.

It talks to your server directly over the Subsonic API — streaming, browsing,
searching, favoriting, playlists, lyrics and scrobbling are all real server calls,
not mocked data.

```
Kotlin · Jetpack Compose for TV · Media3 (ExoPlayer + MediaSession) · Hilt
Retrofit/OkHttp · Room · DataStore · Coil · NanoHTTPD (Party Mode)
```

## Features

**Browsing**
- Home with recently played, newest albums, favorites, genres and playlists.
- New releases with recently added albums, artists with new releases and new songs.
- Browse by genre, decade and alphabetical artist index.
- Album, artist, playlist and genre detail screens with real track lists.
- Global search with recent-search history, served by `search3`.
- Library of albums, artists, playlists, genres and a random song sample.

**Playback**
- Media3 `MediaLibraryService` with a real `ExoPlayer`, background audio and
  media notification, controlled from the TV remote and from system media keys.
- Queue with add-to-queue, remove, clear, shuffle and repeat.
- Immersive Now Playing: full-screen artwork, artwork-derived ambient colours,
  seek bar, transport controls, queue drawer, synchronized/plain lyrics, audio
  details (codec, bitrate, sample rate, channels) and server scrobble status.
- Streaming quality presets mapped to Subsonic transcoding parameters.
- Queue persistence across process death, restored paused; resumable playback.
- Local listening history in Room, plus `scrobble` submissions to the server.

**Personalisation**
- Favorites for albums, artists and tracks (`star`/`unstar`).
- Playlist creation, rename, delete and track add/remove.
- Artwork-driven dark theme, focus rings and focus scaling instead of layout jumps,
  reduced-motion and artwork-background preferences.

**Party Mode (guest queue)**
- The TV runs a small HTTP server on the LAN and shows a pairing QR code.
- Guests scan it on a phone, enter a 4-digit code and search the host's library.
- Requests are queued for **host approval** on the TV; nothing plays unapproved.
- Tokens expire after two hours, requests are rate limited (5/min, 20 pending max)
  and the guest page never receives the Navidrome URL or credentials.

## Requirements

| | |
|---|---|
| Server | Navidrome or any Subsonic 1.16.1-compatible server (OpenSubsonic recommended for lyrics) |
| Device | Android TV / Google TV, Android 8.0 (API 26) or newer |
| Build | JDK 17, Android SDK 35, Gradle 8.9 |
| Network | LAN or HTTPS access to the server; cleartext HTTP is allowed for LAN setups |

## Build

The repository pins its toolchain in `gradle/libs.versions.toml`. On a machine with
the Android SDK installed:

```bash
# JDK 17 and ANDROID_HOME must be set
gradle :app:assembleRelease        # release APK (R8 minified and resource shrunk)
gradle :app:testReleaseUnitTest    # JVM unit tests
gradle :app:lintRelease            # Android lint
```

The release build is signed with the debug key unless `RELEASE_STORE_FILE`,
`RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` are set in
the environment, which makes the APK installable for sideloading but not suitable
for store distribution.

`gradlew` is intentionally not committed (no wrapper jar in this repository); CI and
local builds use the Gradle 8.9 distribution directly.

Continuous integration runs the same three release tasks in
[`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) on every push
and pull request, and uploads the release APK, test reports and lint reports as
artifacts.
See [docs/VERIFICATION.md](docs/VERIFICATION.md) for exactly what CI proves and what
it does not.

## First run

1. Install the APK on the TV (`adb install -r app-release.apk`).
2. Enter the server address — `192.168.1.20:4533`, `music.example.com` or a full
   `https://` URL including a reverse-proxy sub-path.
3. Sign in with a Navidrome username and password. Credentials are stored encrypted
   with an AES/GCM key in the Android Keystore; only a token hash is sent per request.

## Party Mode

1. Connect the TV and the guest phones to the same network.
2. Open **Party** in the top bar and press **Start party mode**.
3. Scan the QR code or open the shown URL and enter the pairing code.
4. Approve or reject each request on the TV.

The guest page is served on port `8765` (next free port up to `8776`), bound to all
interfaces so phones on the LAN can reach it. It has no TLS: use it only on a trusted
network, and stop the session when the party ends. The TV needs no extra permission
beyond `INTERNET`; there is no router/NAT configuration and no cloud dependency.

## Project layout

```
app/src/main/java/com/navidrometv/
├── core/         models, errors, formatting, design system
├── data/         Subsonic API, DTOs, mappers, Room cache, DataStore, repositories
├── domain/       repository contracts and playback use cases
├── playback/     Media3 service, controller bridge, scrobble policy, queue store
├── party/        Party Mode host server and guest page
├── presentation/ navigation, screens and view models
└── di/           Hilt modules
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for data flow and design decisions.

## Known limitations

These are properties of the Subsonic API or of Android TV, not hidden placeholders:

- **No all-songs listing.** Subsonic exposes `getRandomSongs`, so the Library "Songs"
  tab is explicitly labelled as a random sample.
- **No playlist reordering.** There is no move endpoint; the UI says so and offers
  remove/re-add instead.
- **No crossfade, gapless or loudness normalisation.** Media3/Subsonic cannot honour
  these toggles, so they are not offered as fake settings.
- **Lyrics depend on the server.** Without OpenSubsonic `getLyricsBySongId` the
  lyrics panel shows that the server has no lyrics.
- **Android TV voice search** resolves through the media session's search callback;
  it depends on the Google TV launcher and is not available on every device.

The full list with reasons is in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## License

[MIT](LICENSE)
