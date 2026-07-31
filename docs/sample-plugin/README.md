# AntennaPod sample plugin (transcription)

This directory is a **template for a standalone plugin app** that integrates with AntennaPod as an
out-of-process plugin. In production it lives in its **own (private) GitHub repository** and ships as
its own APK — it is intentionally *not* part of the AntennaPod Gradle build (not listed in
AntennaPod's `settings.gradle`).

It demonstrates both capabilities: when AntennaPod downloads an audio episode that has no transcript
(and/or no chapters), it binds this app's service, hands over a read-only file descriptor for the
audio, and receives a transcript and/or chapter markers back.

## How integration works

1. **Contract.** The plugin compiles against the AntennaPod "plugin SDK": the AIDL interface
   (`IMediaProcessorPlugin`) and Parcelables (`PluginMediaRequest`, `PluginMediaResult`,
   `PluginChapter`, `PluginContract`) from AntennaPod's `:plugin:host` module. For a real repo you would
   depend on a published SDK artifact; to keep this template self-contained and buildable, those files
   are included directly under `app/src/main/aidl/` and `app/src/main/java/de/danoeh/antennapod/plugin/host/`
   (same package as in AntennaPod).
2. **Declaration.** `AndroidManifest.xml` exposes a `Service` with:
   - an `<intent-filter>` for `de.danoeh.antennapod.plugin.action.MEDIA_PROCESSOR`,
   - `android:permission="de.danoeh.antennapod.permission.PLUGIN"`,
   - `<meta-data>` giving the plugin id and capability bitmask.
3. **Discovery.** On next launch, AntennaPod finds this service via `PackageManager` and registers it.
4. **Invocation.** AntennaPod calls `process(PluginMediaRequest)`; the service reads the audio from the
   supplied `ParcelFileDescriptor`, runs speech-to-text, and returns a `PluginMediaResult` with the
   transcript content and MIME type (`application/srt`, `text/vtt`, or `application/json`).

## Creating the plugin's own repository

```
gh repo create my-antennapod-transcription-plugin --private
# copy the contents of this directory into the new repo, then implement a real
# speech-to-text engine inside TranscriptionPluginService#transcribe(...)
```

`TranscriptionPluginService` here returns a small placeholder WebVTT cue so the end-to-end path can be
exercised without bundling a model. Replace `transcribe(...)` with a real engine (on-device
Whisper/Vosk, or a cloud API with a user-supplied key).

## Security

The service is protected by the `de.danoeh.antennapod.permission.PLUGIN` permission. AntennaPod is
expected to let the user explicitly enable a discovered plugin before it runs. Because the plugin runs
in its own process, the OS sandbox — not AntennaPod — isolates it.
