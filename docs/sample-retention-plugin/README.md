# AntennaPod sample plugin (episode retention)

This directory is a **template for a standalone plugin app** that implements per-podcast episode
retention — "keep the newest N episodes of each subscribed podcast"
([AntennaPod/AntennaPod#2077](https://github.com/AntennaPod/AntennaPod/issues/2077)). In production it
lives in its **own GitHub repository** and ships as its own APK — it is intentionally *not* part of the
AntennaPod Gradle build.

It uses two extension points, not the media processor one used by `docs/sample-plugin/` (transcripts)
and `docs/sample-chapter-plugin/` (chapters). AntennaPod does not gain a "keep newest N" setting; the
rule *and* its per-podcast configuration live in this app.

- **`FEED_CONTENT`** — the primary mechanism. AntennaPod hands over each downloaded feed document
  before parsing it, and `FeedContentPluginService` returns the same feed cut down to the newest N
  items. Everything older is gone before the app reads the feed, so those episodes are never listed,
  never counted and never downloaded — AntennaPod does not know they exist.
- **`EPISODE_RETENTION`** — the cleanup half. `RetentionPluginService` tells AntennaPod which
  *already downloaded* episodes are past N, so files that were fetched before the limit was lowered
  get removed during the next automatic cleanup.

Both services share one plugin id (`keep-newest-episodes`) and one N per podcast, so the user enables
the plugin once and sets one number.

## How integration works

1. **Contract.** The plugin compiles against the AntennaPod "plugin SDK": the AIDL interfaces
   (`IFeedContentPlugin`, `IEpisodeRetentionPlugin`) and Parcelables (`PluginFeedContentRequest`,
   `PluginFeedContentResult`, `PluginRetentionRequest`, `PluginRetentionResult`, `PluginEpisodeInfo`,
   `PluginContract`) from AntennaPod's `:plugin:host` module. For a real repo you would depend on a
   published SDK artifact; to keep this template self-contained and buildable, those files are included
   directly under `app/src/main/aidl/` and `app/src/main/java/de/danoeh/antennapod/plugin/host/`.
2. **Declaration.** `AndroidManifest.xml` exposes two `Service`s, each with
   `android:permission="de.danoeh.antennapod.permission.PLUGIN"`, the same `<meta-data>` plugin id
   (`keep-newest-episodes`), and:
   - `FeedContentPluginService` — `<intent-filter>` for
     `de.danoeh.antennapod.plugin.action.FEED_CONTENT`, capability bitmask `8`,
   - `RetentionPluginService` — `<intent-filter>` for
     `de.danoeh.antennapod.plugin.action.EPISODE_RETENTION`, capability bitmask `4`.
3. **Discovery.** AntennaPod finds both services via `PackageManager` and registers them as a
   `FeedContentProcessor` and an `EpisodeRetentionPolicy` (live, via the package monitor, or on next
   launch). They stay disabled until the user enables the plugin in **Settings → Plugins**.
4. **Invocation, feed content.** After downloading a feed and before parsing it, AntennaPod calls
   `processFeed(PluginFeedContentRequest)`. The request carries the feed id and url plus a read-only
   descriptor for the downloaded document and a descriptor to write to. `FeedContentPluginService`
   parses the XML, drops every item past the newest N, and writes the result. AntennaPod parses the
   rewritten feed, so the dropped episodes never enter the database.
5. **Invocation, retention.** During automatic cleanup, AntennaPod calls
   `selectForDeletion(PluginRetentionRequest)` once per subscription. The request carries the feed and
   its downloaded episodes; the service returns the ids of the episodes beyond the newest N. AntennaPod
   deletes those media files and ignores ids it did not offer.

## Settings

The launcher activity (`RetentionSettingsActivity`) configures how many episodes to keep for all
podcasts and, per podcast, an override. Podcasts appear there as AntennaPod asks about them. `0` keeps
all episodes, which effectively disables the rule for that podcast.

The same N drives both services. Note that a podcast the app has never fetched has feed id `0` at feed
content time, so a brand-new subscription uses the "all podcasts" value on its first fetch.

Queued and favorite episodes are never deleted by AntennaPod; they are still part of the retention
request (flagged via `PluginEpisodeInfo#isDeletable()`) so the plugin counts them towards the newest N.

Feed truncation keeps document order when items have no parseable dates, since feeds are conventionally
newest-first; when every item carries a `pubDate` / `published` / `updated`, the newest N are kept
regardless of the order the feed lists them in.

## Building

A Gradle wrapper is bundled, so `./gradlew :app:assembleDebug` works with just a JDK. CI
(`.github/workflows/build.yml`) also builds the debug APK on every push/PR and uploads it as an
artifact.

## Creating the plugin's own repository

```
gh repo create my-antennapod-retention-plugin --private
# copy the contents of this directory into the new repo
```

## Security

The service is protected by the `de.danoeh.antennapod.permission.PLUGIN` permission, and AntennaPod
runs it only after the user enables it in **Settings → Plugins**. It runs in its own OS-sandboxed
process and never sees any episode file: the request contains only metadata, and the plugin can only
ask for episodes of the feed AntennaPod asked about.
