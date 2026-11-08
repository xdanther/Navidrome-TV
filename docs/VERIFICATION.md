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

Run [42494393139](https://github.com/xdanther/Navidrome-TV/actions/runs/42494393139)
(2026-11-06, branch `freebuff/ce4e351aa72f4e6085fe8f89`, commit `8d873e8`) finished
green after the 10-foot redesign: `testReleaseUnitTest` passed, `lintRelease` reported
no errors, and `assembleRelease` produced the R8-minified APK.

That run proves the redesigned composables compile against Compose 1.7 and tv-material
1.0.0 and that lint accepts them. It does **not** prove anything about how the result
looks or feels: focus contrast, the navigation collapse, artwork proportions, lyric
layout and background stability all need a device or emulator to assess.

Earlier release run [42300761466](https://github.com/xdanther/Navidrome-TV/actions/runs/42300761466)
(commit `7f38aa4`) first exercised the release variant, and debug run
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
