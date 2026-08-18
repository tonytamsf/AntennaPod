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
- `PluginContract` — the well-known action, permission name, metadata keys, and capability/result flags.

In a real deployment this package would be published as a small "plugin SDK" artifact that plugin repos
depend on. The sample plugin (`docs/sample-plugin/`) copies these files for illustration.

## Discovery

`PluginManager.discover(context)` queries `PackageManager.queryIntentServices` for services declaring
the `ACTION_MEDIA_PROCESSOR` action, reading each plugin's id and capabilities from its service
`<meta-data>`. Android 11+ package visibility requires the matching `<queries>` entry in the host
manifest (declared in `:app`).

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

## Capabilities

Plugins declare a capability bitmask (`CAPABILITY_TRANSCRIPTION`, `CAPABILITY_CHAPTERS`,
`CAPABILITY_AD_CHAPTERS`). For each download, `RemoteMediaProcessor` requests every capability the
plugin supports that the episode still needs, one binder call each, and applies the results:
transcripts via `TranscriptUtils.storeTranscript`, chapters via `item.setChapters` +
`DBWriter.setFeedItem`, and ad-chapter markers via a merge-and-dedup strategy that inserts ad markers
into existing chapter lists without duplicating near-identical timestamps (1s tolerance).

## Live discovery

`discoverAndRegister` registers a runtime `BroadcastReceiver` for `PACKAGE_ADDED` / `REMOVED` /
`REPLACED`, so installing or uninstalling a plugin app while AntennaPod is running re-syncs the
registry (`syncRegistrations`) without a restart.

## Known limitations (prototype)

- Binding is synchronous on the download worker thread; heavy plugins argue for a dedicated
  `WorkManager` job (the extension-point contract is unaffected).
- The package monitor is registered for the app process lifetime and not explicitly unregistered.
