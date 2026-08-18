# End-to-end test: AntennaPod chapter plugin

This walks through installing AntennaPod (debug) and the chapter plugin, enabling it, and verifying
that a downloaded episode gets plugin-generated chapter markers.

Requires a machine with the Android SDK and a device/emulator (`adb`). AntennaPod's debug build has
applicationId `de.danoeh.antennapod.debug`.

## 1. Build & install AntennaPod (debug)

From the AntennaPod repo root:

```
./gradlew :app:installPlayDebug
```

## 2. Build & install the chapter plugin

The plugin is a standalone project (its own Gradle build). Easiest path is Android Studio:
**Open** `docs/sample-chapter-plugin/` as a project, let it sync, then Run the `app` config.

Or from the command line, in `docs/sample-chapter-plugin/` (the bundled Gradle wrapper needs only a
JDK):

```
./gradlew :app:installDebug
```

CI also builds the debug APK on every push/PR via `.github/workflows/build.yml` and uploads it as a
build artifact, so you can download a prebuilt APK from the Actions run instead of building locally.

The plugin app installs as `com.example.antennapodchapterplugin`. It has no launcher activity — it
only exposes the plugin service, so you will not see an icon; that is expected.

## 3. Enable the plugin in AntennaPod

Open AntennaPod → **Settings → Plugins**. "AntennaPod Chapter Plugin" appears in the list. Toggle it
**on**. (Plugins are disabled by default; discovery also happens live, so the entry appears without
restarting the app. If you also installed the transcription sample, both are listed independently.)

## 4. Trigger processing

1. Subscribe to a podcast whose episodes have **no** chapters.
2. Download an episode.
3. When the download completes, `MediaDownloadedHandler` runs the registered processors. The remote
   processor binds the plugin, passes the audio file descriptor, and applies the returned chapters.

## 5. Verify

- Open the downloaded episode → the **chapters** show "Intro", "Part 1", and "Part 2".

Confirm the plugin ran via logcat:

```
adb logcat -d | grep -E "SampleChapterPlugin|RemoteMediaProcessor|PluginManager"
```

Expected lines include `Registering plugin 'sample-chapters'`, `Generating chapters for '<title>'`,
and `Applied 3 chapters from plugin sample-chapters`.

## Toggling off

Disabling the plugin in Settings makes `RemoteMediaProcessor.shouldProcess` return `false`, so future
downloads are not sent to it. Uninstalling the plugin app removes it from the list (live, via the
package monitor).
