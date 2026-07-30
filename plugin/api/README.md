# :plugin:api

Extension-point API for optional, in-tree plugins that augment content as it flows through the app.

This module is intentionally lightweight (it only depends on `:model`) so that pipeline modules such
as `:net:download:service` can call into it without pulling in plugin implementations.

## Extension points

### `DownloadedMediaProcessor`

A post-download processing hook. After an episode has been downloaded and its file is on disk, the
download pipeline calls `MediaProcessorRegistry.runAll(...)`, which invokes every registered
processor whose `shouldProcess(FeedMedia)` returns `true`. A processor may augment the episode, for
example by generating a transcript for audio that does not ship one.

Processors run on the download worker thread. Heavy work (e.g. speech-to-text) should be kept
resumable and tolerate interruption when the worker is stopped. A future iteration may move heavy
processors onto their own `WorkManager` job while keeping this contract unchanged.

## Registration

Plugins register their processors at app startup in `ClientConfigurator`, following the same
service-interface/impl registration pattern used elsewhere in the app:

```java
MediaProcessorRegistry.register(new TranscriptionMediaProcessor(new DisabledTranscriptionEngine()));
```

Implementations live in their own `:plugin:*` modules and are wired in from `:app`.
