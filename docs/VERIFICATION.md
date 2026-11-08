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

## Release signing

Every CI run used to sign the release APK with a freshly generated debug keystore,
so the certificate changed on each build and Android rejected installing an update
over an existing copy.

`android-ci.yml` now prepares a keystore before the build:

1. `Restore signing key` restores the `.signing/` directory from the Actions cache.
2. `Prepare release signing` writes a keystore from `RELEASE_KEYSTORE_BASE64` if that
   secret exists, otherwise reuses the restored file, otherwise generates one with
   `keytool` and caches it after the job.
3. `Assemble release APK` receives `RELEASE_STORE_FILE`, the password and the alias,
   so `app/build.gradle.kts` signs the release variant with that key.
4. `Verify APK signature` compares the keystore's certificate SHA-256 with the APK's
   and fails the build if they differ, so a silent re-signing cannot ship.

Observed on run [42496984812](https://github.com/xdanther/Navidrome-TV/actions/runs/42496984812):
the key was generated on the first run, its certificate SHA-256 is
`acfab1170c8fd69fac85566fdcd7e96cfc39a6a00664e2f42b226c377ab851a4`, and
`apksigner` reported the same digest for the APK. Later runs restore that key and
must report the identical digest.

Limits: the Actions cache is branch-scoped and can be evicted, and a merge to `main`
starts from that branch's own cache. Configure the `RELEASE_*` repository secrets for
a key that never changes; the cache is a zero-setup default, not a guarantee.

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
