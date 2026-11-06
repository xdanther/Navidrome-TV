# Verification

## What CI does

`.github/workflows/android-ci.yml` runs on every push to `main`/`freebuff/**` and on
pull requests:

1. Sets up JDK 17 and the Android SDK.
2. Installs Gradle 8.9 through `gradle/actions/setup-gradle`.
3. Runs `:app:testDebugUnitTest` (JVM unit tests).
4. Runs `:app:lintDebug`.
5. Builds `:app:assembleDebug`.
6. Uploads the debug APK, test reports and lint reports as workflow artifacts.

A green run proves that the Kotlin sources compile against the pinned Android,
Compose, Media3 and Hilt versions, that the unit tests pass, and that lint reports no
errors. Lint warnings do not fail the build; check the uploaded report.

## Running the same checks locally

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

A JDK 17 and an Android SDK with platform 35 are required; the repository does not
commit `gradlew`, so use the Gradle 8.9 distribution.

## What is not verified automatically

- **No device or emulator tests.** Focus traversal, D-pad behaviour, playback,
  media-session integration and the Compose layouts have not been exercised by an
  automated test.
- **No real server in CI.** Authentication, streaming, scrobbling, favorites,
  playlists and lyrics are implemented against the Subsonic 1.16.1 contract but have
  not been run against a live Navidrome instance in CI.
- **Party Mode** needs two devices on one network (host TV + guest browser); this is
  a manual test.
- **Voice search** depends on the Google TV launcher.

## Manual test checklist

Before calling a release usable:

1. Sign in against a real server; kill and relaunch the app to confirm session
   restore; turn Wi-Fi off and confirm errors are readable and retryable.
2. Browse every tab with only the D-pad; confirm focus is visible and returns after
   Back.
3. Play an album, then exercise pause, next/previous, seek, shuffle, repeat, queue
   removal and app restart during playback.
4. Open Now Playing on a track with synced lyrics and confirm line highlighting.
5. Favorite an album and a track, then verify the stars on the server.
6. Create, rename and delete a playlist; add and remove tracks.
7. Start Party Mode, join from a phone, request a song and approve/reject it on the
   TV; stop the session and confirm the guest page stops working.
