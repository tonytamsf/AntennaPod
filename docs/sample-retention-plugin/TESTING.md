# End-to-end test: AntennaPod retention plugin

This walks through installing AntennaPod (debug) and the retention plugin, enabling it, and verifying
that only the newest N downloaded episodes of a podcast survive the automatic cleanup.

Requires a machine with the Android SDK and a device/emulator (`adb`). AntennaPod's debug build has
applicationId `de.danoeh.antennapod.debug`.

## 1. Build & install AntennaPod (debug)

From the AntennaPod repo root:

```
./gradlew :app:installPlayDebug
```

## 2. Build & install the retention plugin

The plugin is a standalone project (its own Gradle build). Easiest path is Android Studio:
**Open** `docs/sample-retention-plugin/` as a project, let it sync, then Run the `app` config.

Or from the command line, in `docs/sample-retention-plugin/` (the bundled Gradle wrapper needs only a
JDK):

```
./gradlew :app:installDebug
```

CI also builds the debug APK on every push/PR via `.github/workflows/build.yml` and uploads it as a
build artifact, so you can download a prebuilt APK from the Actions run instead of building locally.

The plugin app installs as `com.example.antennapodretentionplugin` and, unlike the transcript and
chapter samples, has a launcher icon: that is where the retention settings live.

## 3. Enable the plugin in AntennaPod

Open AntennaPod → **Settings → Plugins**. "AntennaPod Retention Plugin" appears in the list. Toggle it
**on**. (Plugins are disabled by default; discovery also happens live, so the entry appears without
restarting the app.)

## 4. Configure how many episodes to keep

Open the "AntennaPod Retention Plugin" app. Set **All podcasts** to e.g. `2`. Podcasts show up under
**Per podcast** once AntennaPod has asked about them, so you can override individual ones after the
first feed refresh or cleanup run.

## 5. Verify the feed is truncated

1. Subscribe to a podcast with many episodes (or refresh one you already have).
2. Open the podcast: only the newest 2 episodes are listed. The rest are not "hidden" — AntennaPod
   never parsed them, so they are absent from the database entirely.
3. Set the value back to `0` (keep all) and refresh again: the full episode list returns.

Confirm via logcat:

```
adb logcat -d | grep -E "RetentionFeedPlugin|RemoteFeedContent|FeedContentRegistry"
```

Expected lines include `Registering feed content plugin 'keep-newest-episodes'`, `<url> has N items,
keeping newest 2`, and `Applied feed rewritten by 'keep-newest-episodes': X item(s) removed`.

## 6. Verify cleanup of already-downloaded episodes

The feed hook only affects what is fetched from now on; episodes downloaded earlier are cleaned up by
the retention half of the plugin.

1. With the value set high (or `0`), download 4 or more episodes of a podcast.
2. Make sure they are neither in the queue nor marked as favorite — those are never deleted.
3. Lower the value to 2, then trigger AntennaPod's automatic cleanup (it runs with the automatic
   download job; on a test device the quickest way is Settings → Automatic Download, enable it, and let
   the job run).
4. The two newest episodes are still downloaded, the older ones are not.

```
adb logcat -d | grep -E "RetentionPlugin|RemoteRetentionPolicy|PluginRetentionCleanup|PluginManager"
```

Expected lines include `Registering retention plugin 'keep-newest-episodes'`, `'<podcast>' has N
downloaded episodes, keeping 2`, and `Retention policies deleted X episodes`.

## Toggling off

Disabling the plugin in Settings makes `RemoteEpisodeRetentionPolicy.shouldApply` return `false`, so
cleanups no longer consult it. Setting the keep count to `0` keeps all episodes. Uninstalling the
plugin app removes it from the list (live, via the package monitor).
