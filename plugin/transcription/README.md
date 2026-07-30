# :plugin:transcription

Reference `:plugin:api` implementation that generates a transcript for downloaded audio episodes
that do not already provide one.

`TranscriptionMediaProcessor` is a `DownloadedMediaProcessor`. It processes an episode only when an
engine is available, the media is audio, the file is downloaded, and the item has no transcript yet.
When the engine produces a transcript, it is stored next to the media file (via
`TranscriptUtils.storeTranscript`) and the item's transcript URL/type are set, so the rest of the app
(parser and transcript UI) treats it exactly like a publisher-provided Podcast Index transcript.

## Engines

The actual speech-to-text is abstracted behind `TranscriptionEngine`, so the model/back end is
swappable:

- `DisabledTranscriptionEngine` — the default no-op engine (`isAvailable()` returns `false`). It lets
  the extension point ship and be exercised without bundling a speech-to-text model.

To make transcription actually run, implement `TranscriptionEngine` (e.g. an on-device Whisper/Vosk
engine, or a cloud API using a user-supplied key) and register the processor with it in
`ClientConfigurator`. The engine returns a `TranscriptionResult` carrying the transcript content and
its MIME type (`application/srt`, `text/vtt`, or `application/json`).
