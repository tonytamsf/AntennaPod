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

### `EpisodeRetentionPolicy`

A per-subscription retention hook, used during automatic cleanup. `PluginRetentionCleanup` (in
`:net:download:service`) groups the downloaded episodes AntennaPod considers deletable by feed and
calls `EpisodeRetentionRegistry.selectForDeletion(...)` for each feed; every episode a policy selects
is deleted. This is the extension point behind per-podcast retention rules such as "keep the newest N
episodes of this podcast" (AntennaPod/AntennaPod#2077), which can then be configured by the plugin
that implements them instead of in AntennaPod itself.

The registry only accepts episodes the caller offered as candidates, so a policy can never widen the
set of deletable episodes: queued and favorite episodes are never candidates.

### `FeedContentProcessor`

A hook on the **raw feed document**, before it is parsed. `FeedUpdateWorker` downloads a feed and calls
`FeedContentRegistry.runAll(...)` with the file on disk, then hands the same file to `FeedParserTask`.
A processor may rewrite the document, for example dropping all but the newest N items — episodes it
removes are never parsed, so the app never learns they exist at all. That is stronger than
`EpisodeRetentionPolicy`, which only deletes downloaded files of episodes the app already knows about.

Processors must leave a parseable document behind, or leave the file untouched; a feed that no longer
parses fails the whole update for that subscription.

## Registration

Processors register at app startup in `ClientConfigurator`. In the current architecture, external
plugin apps are discovered and adapted into `DownloadedMediaProcessor` instances by `:plugin:host`:

```java
PluginManager.discoverAndRegister(context);
```

Each discovered plugin app becomes a `RemoteMediaProcessor` registered on `MediaProcessorRegistry`, or a
`RemoteEpisodeRetentionPolicy` registered on `EpisodeRetentionRegistry`, so the pipelines invoke external
plugins through the same in-process seams without knowing they run in another process.
