# :plugin:host

Host-side machinery for **out-of-process plugins**: plugins are separate Android apps (their own APKs,
their own repos) that AntennaPod discovers on the device and talks to over Android IPC. There is no
dynamic code loading — the plugin runs in its own process, sandboxed by the OS.

## Cross-process contract (the "plugin SDK")

The contract that both AntennaPod and every plugin app compile against lives here:

- `aidl/.../IMediaProcessorPlugin.aidl` — the bound-service interface: `getPluginId()`,
  `getCapabilities()`, and `process(PluginMediaRequest)`.
- `PluginMediaRequest` / `PluginMediaResult` — the Parcelable request/response. The downloaded media is
  passed as a **read-only `ParcelFileDescriptor`** over the binder, so no file sharing, `content://`
  URIs, or storage permissions are involved. The result carries generated content (e.g. a transcript)
  and its MIME type.
- `aidl/.../IEpisodeRetentionPlugin.aidl` — the bound-service interface of the retention extension
  point: `getPluginId()`, `getCapabilities()`, and `selectForDeletion(PluginRetentionRequest)`.
- `PluginRetentionRequest` / `PluginRetentionResult` / `PluginEpisodeInfo` — the Parcelables for that
  call. The request describes one feed and the downloaded episodes AntennaPod considers deletable; the
  result carries the ids of the episodes that may be deleted.
- `aidl/.../IFeedContentPlugin.aidl` — the bound-service interface of the feed content extension point:
  `getPluginId()`, `getCapabilities()`, and `processFeed(PluginFeedContentRequest)`.
- `PluginFeedContentRequest` / `PluginFeedContentResult` — the Parcelables for that call. The request
  carries the feed id and url plus **two `ParcelFileDescriptor`s**: the downloaded document read-only,
  and a file to write a rewritten document to. The result says whether anything was rewritten.
- `PluginContract` — the well-known actions, permission name, metadata keys, and capability/result flags.

In a real deployment this package would be published as a small "plugin SDK" artifact that plugin repos
depend on. The sample plugin (`docs/sample-plugin/`) copies these files for illustration.

## Discovery

`PluginManager.discover(context)` queries `PackageManager.queryIntentServices` for services declaring
the `ACTION_MEDIA_PROCESSOR` action, reading each plugin's id and capabilities from its service
`<meta-data>`. `discoverRetentionPlugins(context)` and `discoverFeedContentPlugins(context)` do the same
for `ACTION_EPISODE_RETENTION` and `ACTION_FEED_CONTENT`, and `discoverAll(context)` returns all three
deduplicated by plugin id (that is what the settings screen lists). One app may expose several of these
services under a single plugin id, so the user enables it once. Android 11+ package visibility requires
the matching `<queries>` entries in the host manifest (declared in `:app`).

## Bridge to the download pipeline

`PluginManager.discoverAndRegister(context)` (called from `ClientConfigurator` at startup) wraps each
discovered plugin in a `RemoteMediaProcessor` and registers it with `MediaProcessorRegistry` from
`:plugin:api`. That means an external plugin app plugs into the **same** in-process extension point the
download pipeline already calls (`MediaDownloadedHandler` → `MediaProcessorRegistry.runAll`). The
pipeline is unaware that a given processor is backed by another app.

For each applicable episode, `RemoteMediaProcessor`:
1. binds to the plugin service (`BIND_AUTO_CREATE`, with a timeout),
2. opens the downloaded file as a read-only `ParcelFileDescriptor`,
3. calls `process(...)` synchronously over the binder,
4. applies the result through the existing pipeline (e.g. `TranscriptUtils.storeTranscript` +
   `DBWriter.setFeedItem`), so generated content is indistinguishable from publisher-provided content,
5. unbinds.

## Security / trust model

- Plugin services are gated behind the `de.danoeh.antennapod.permission.PLUGIN` permission declared by
  the host.
- **Explicit user consent:** discovered plugins are disabled by default. `RemoteMediaProcessor` refuses
  to run (`shouldProcess` returns `false`) unless the user has enabled the plugin, persisted via
  `PluginPreferences`. The "Plugins" settings screen lists installed plugins with a per-plugin switch.
- For a curated first-party ecosystem the permission could be raised to `signature` so only co-signed
  plugins bind.

## Bridge to the cleanup pipeline

`RemoteEpisodeRetentionPolicy` is the same idea for the `EpisodeRetentionPolicy` extension point of
`:plugin:api`. During automatic cleanup, `PluginRetentionCleanup` asks the registry per feed which
downloaded episodes may be deleted; the remote policy binds the plugin's `ACTION_EPISODE_RETENTION`
service, sends the feed and its deletable episodes, and maps the returned episode ids back to
`FeedItem`s. Ids that were not part of the request are ignored, so a plugin cannot reach beyond the
episodes it was offered. This is how per-podcast rules like "keep the newest N episodes" are
implemented outside the app.

## Bridge to the feed update pipeline

`RemoteFeedContentProcessor` implements the `FeedContentProcessor` extension point of `:plugin:api`.
After a feed is downloaded and before `FeedParserTask` reads it, it binds the plugin's
`ACTION_FEED_CONTENT` service, passes the downloaded document as a read-only `ParcelFileDescriptor`
plus a second descriptor for the plugin's output, and replaces the downloaded file only if the plugin
reports it rewrote one. The replacement is rejected unless the output is non-empty and starts with `<`,
so a broken plugin cannot make a subscription unparseable. This is what lets a plugin cut a feed down
to the newest N items: the removed items are never parsed, so AntennaPod never sees them.

## Capabilities

Plugins declare a capability bitmask (`CAPABILITY_TRANSCRIPTION`, `CAPABILITY_CHAPTERS`,
`CAPABILITY_EPISODE_RETENTION`, `CAPABILITY_FEED_CONTENT`). For each download, `RemoteMediaProcessor` requests every capability the
plugin supports that the episode still needs, one binder call each, and applies the results: transcripts
via `TranscriptUtils.storeTranscript` and chapters via `item.setChapters` + `DBWriter.setFeedItem`.

## Live discovery

`discoverAndRegister` registers a runtime `BroadcastReceiver` for `PACKAGE_ADDED` / `REMOVED` /
`REPLACED`, so installing or uninstalling a plugin app while AntennaPod is running re-syncs the
registry (`syncRegistrations`) without a restart.

## Known limitations (prototype)

- Binding is synchronous on the download worker thread; heavy plugins argue for a dedicated
  `WorkManager` job (the extension-point contract is unaffected).
- The package monitor is registered for the app process lifetime and not explicitly unregistered.
