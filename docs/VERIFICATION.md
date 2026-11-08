# Verification

## What CI does

`.github/workflows/android-ci.yml` runs on every push to `main`/`freebuff/**` and on
pull requests:

1. Sets up JDK 17 and the Android SDK.
2. Installs Gradle 8.9 through `gradle/actions/setup-gradle`.
3. Runs `:app:testReleaseUnitTest` (JVM unit tests).
4. Runs `:app:lintRelease`.
5. Builds `:app:assembleRelease`, which also runs R8 minification, resource
   shrinking and `lintVitalRelease`.
6. Uploads the release APK, test reports and lint reports as workflow artifacts.
7. On failure, republishes the Gradle and lint findings as workflow annotations, so
   a failed build is readable without access to the raw job logs.

A green run proves that the Kotlin sources compile against the pinned Android,
Compose, Media3 and Hilt versions, that the unit tests pass, and that lint reports no
errors. Lint warnings do not fail the build; check the uploaded report.

## Last verified run

Run [42495969792](https://github.com/xdanther/Navidrome-TV/actions/runs/42495969792)
(2026-11-06, branch `freebuff/ce4e351aa72f4e6085fe8f89`, commit `32e7deb`) finished
green for the Android TV design rewrite: `testReleaseUnitTest` passed, `lintRelease`
reported no errors, and `assembleRelease` produced the R8-minified APK. Two earlier
runs on the same branch failed and were fixed rather than bypassed — a smart cast on a
delegated `track`, and a trailing comma left behind by a palette replacement.

That run proves the new palette, type scale, tab navigation and focus components
compile and lint. It does **not** demonstrate how they look: colour contrast on a real
panel, focus legibility at three metres, overscan behaviour and tab underline
animation still need a device or emulator.

Earlier runs: [42494393139](https://github.com/xdanther/Navidrome-TV/actions/runs/42494393139)
(commit `8d873e8`) covered the 10-foot layout pass, and
[42297506307](https://github.com/xdanther/Navidrome-TV/actions/runs/42297506307)
covered the Subsonic envelope, cover-art fallback and Media3 callback fixes.

## Running the same checks locally

```bash
gradle :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
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
