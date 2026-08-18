# AntennaPod sample plugin (chapters)

This directory is a **template for a standalone plugin app** that integrates with AntennaPod as an
out-of-process plugin. In production it lives in its **own (private) GitHub repository** and ships as
its own APK — it is intentionally *not* part of the AntennaPod Gradle build.

It is deliberately independent of the transcription sample (`docs/sample-plugin/`): a separate app,
a separate repo, a separate plugin id (`sample-chapters`), declaring only the **chapters** capability.
Both plugins can be installed at once, and AntennaPod discovers and drives each on its own.

It demonstrates the chapters capability: when AntennaPod downloads an audio episode that has no
chapters, it binds this app's service, hands over a read-only file descriptor for the audio, and
receives chapter markers back.

## How integration works

1. **Contract.** The plugin compiles against the AntennaPod "plugin SDK": the AIDL interface
   (`IMediaProcessorPlugin`) and Parcelables (`PluginMediaRequest`, `PluginMediaResult`,
   `PluginChapter`, `PluginContract`) from AntennaPod's `:plugin:host` module. For a real repo you would
   depend on a published SDK artifact; to keep this template self-contained and buildable, those files
   are included directly under `app/src/main/aidl/` and `app/src/main/java/de/danoeh/antennapod/plugin/host/`.
2. **Declaration.** `AndroidManifest.xml` exposes a `Service` with:
   - an `<intent-filter>` for `de.danoeh.antennapod.plugin.action.MEDIA_PROCESSOR`,
   - `android:permission="de.danoeh.antennapod.permission.PLUGIN"`,
   - `<meta-data>` giving the plugin id (`sample-chapters`) and capability bitmask (`2` = chapters).
3. **Discovery.** AntennaPod finds this service via `PackageManager` and registers it (live, via the
   package monitor, or on next launch).
4. **Invocation.** AntennaPod calls `process(PluginMediaRequest)` with `capability == CAPABILITY_CHAPTERS`;
   the service returns a `PluginMediaResult` whose `chapters` list carries the generated markers.

## Building

A Gradle wrapper is bundled, so `./gradlew :app:assembleDebug` works with just a JDK. CI
(`.github/workflows/build.yml`) also builds the debug APK on every push/PR and uploads it as an
artifact.

## Creating the plugin's own repository

```
gh repo create my-antennapod-chapter-plugin --private
# copy the contents of this directory into the new repo, then implement real
# chapter detection inside ChapterPluginService#generateChapters(...)
```

`ChapterPluginService` here returns a few placeholder markers so the end-to-end path can be exercised
without bundling a model. Replace `generateChapters(...)` with real detection (e.g. silence/segment
analysis reading `request.getMediaFd()`).

## Security

The service is protected by the `de.danoeh.antennapod.permission.PLUGIN` permission, and AntennaPod
runs it only after the user enables it in **Settings → Plugins**. It runs in its own OS-sandboxed
process.
