# End-to-end test: AntennaPod plugin system

This walks through installing AntennaPod (debug) and the sample plugin, enabling the plugin, and
verifying that a downloaded episode gets a plugin-generated transcript and chapters.

Requires a machine with the Android SDK and a device/emulator (`adb`). AntennaPod's debug build has
applicationId `de.danoeh.antennapod.debug`.

## 1. Build & install AntennaPod (debug)

From the AntennaPod repo root:

```
./gradlew :app:installPlayDebug
```

## 2. Build & install the sample plugin

The sample is a standalone project (its own Gradle build). Easiest path is Android Studio:
**Open** `docs/sample-plugin/` as a project, let it sync, then Run the `app` config.

Or from the command line, in `docs/sample-plugin/` with a local Gradle 8.13:

```
gradle wrapper        # once, to generate ./gradlew (or use Android Studio's Gradle)
./gradlew :app:installDebug
```

The plugin app installs as `com.example.antennapodplugin`. It has no launcher activity — it only
exposes the plugin service, so you will not see an icon; that is expected.

## 3. Enable the plugin in AntennaPod

Open AntennaPod → **Settings → Plugins**. The sample plugin ("AntennaPod Transcription Plugin")
appears in the list. Toggle it **on**. (Plugins are disabled by default; discovery also happens live,
so the entry appears without restarting the app.)

## 4. Trigger processing

1. Subscribe to a podcast whose episodes have **no** publisher transcript and **no** chapters.
2. Download an episode.
3. When the download completes, `MediaDownloadedHandler` runs the registered processors. The remote
   processor binds the plugin, passes the audio file descriptor, and applies the returned transcript
   and chapters.

## 5. Verify

- Open the downloaded episode → the **transcript** view shows the sample plugin's WebVTT output,
  including the episode title and the audio byte count that crossed the binder.
- The episode's **chapters** show "Sample chapter: Intro" and "Sample chapter: Middle".

Confirm the plugin ran via logcat:

```
adb logcat -d | grep -E "SamplePlugin|RemoteMediaProcessor|PluginManager"
```

Expected lines include `Registering plugin 'sample-transcription'`, `Transcribing '<title>'`, and
`Applied transcript from plugin sample-transcription` / `Applied 2 chapters ...`.

## Toggling off

Disabling the plugin in Settings makes `RemoteMediaProcessor.shouldProcess` return `false`, so future
downloads are not sent to it. Uninstalling the plugin app removes it from the list (live, via the
package monitor).
