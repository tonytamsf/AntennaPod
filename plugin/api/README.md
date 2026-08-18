# :plugin:api

Internal extension-point API for augmenting content as it flows through the download pipeline.

This module is intentionally lightweight (it only depends on `:model`) so that pipeline modules such
as `:net:download:service` can call into it without pulling in any implementation. It is the seam that
processors register against; the processors themselves may be in-process or backed by out-of-process
plugin apps (see `:plugin:host`).

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

Processors register at app startup in `ClientConfigurator`. In the current architecture, external
plugin apps are discovered and adapted into `DownloadedMediaProcessor` instances by `:plugin:host`:

```java
PluginManager.discoverAndRegister(context);
```

Each discovered plugin app becomes a `RemoteMediaProcessor` registered on `MediaProcessorRegistry`, so
the download pipeline invokes external plugins through the same in-process seam without knowing they run
in another process.
