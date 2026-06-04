# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Inherited Instructions

This module inherits all instructions from the root `AGENTS.md` (via `CLAUDE.md`) at the project root. The root instructions take precedence — read them first. This file adds only net-module-specific detail.

## Module Overview

The `:net` folder contains all modules that directly interact with the network. Every HTTP request goes through the shared OkHttp client in `:net:common`, which applies SSL, proxy, user-agent, and auth interceptors globally.

### Submodules

| Gradle path | Folder | Purpose |
|---|---|---|
| `:net:common` | `common/` | Shared OkHttp client (`AntennapodHttpClient`), interceptors, URL/URI utilities |
| `:net:discovery` | `discovery/` | Podcast search/discovery (iTunes, fyyd, PodcastIndex). Each directory is a separate class implementing `PodcastSearcher` |
| `:net:download:service-interface` | `download/service-interface/` | Interface only — `DownloadServiceInterface`, `FeedUpdateManager`, `AutoDownloadManager` stubs. No implementation here |
| `:net:download:service` | `download/service/` | WorkManager-based download workers: `EpisodeDownloadWorker`, `FeedUpdateWorker`. Also contains auto-download and cleanup algorithms |
| `:net:ssl` | `ssl/` | Wraps Conscrypt to backport modern TLS to older Android versions. Has separate `free/` and `play/` source sets for `SslProviderInstaller` |
| `:net:sync:service-interface` | `sync/service-interface/` | `ISyncService` interface + shared data classes (`EpisodeAction`, `SynchronizationQueue`) |
| `:net:sync:service` | `sync/service/` | `SyncService` Worker that coordinates the active backend; `SynchronizationQueueStorage` persists the pending-changes queue in SharedPreferences |
| `:net:sync:gpoddernet` | `sync/gpoddernet/` | Gpodder.net REST API backend implementing `ISyncService` |
| `:net:sync:model` | `sync/model/` | Shared data model for sync backends |

## Key Architectural Patterns

### Service-Interface / Service Split

Both download and sync use this pattern:
- The `-interface` module declares abstract base classes and stubs (e.g., `DownloadServiceInterface`, `FeedUpdateManager`).
- The concrete implementation lives in the `-service` module and is registered as the singleton at app startup via `ClientConfigurator` in `:app`.
- Consumers depend only on the interface module, keeping compile-time coupling low.

### WorkManager-Based Workers

Downloads and sync run as `Worker` subclasses managed by AndroidX WorkManager (not Android Services). This means scheduling, battery optimization, and network constraints are handled by the OS. New background work in these modules should follow the same pattern.

### Centralized HTTP Client

`AntennapodHttpClient.getHttpClient()` returns the shared OkHttp singleton. All network calls in `:net` modules must use this client (or `AntennapodHttpClient.newBuilder()` if a separate instance with different state is needed). Never create a raw `OkHttpClient` directly — doing so bypasses SSL backports, proxy, and auth interceptors.

### EventBus for Cross-Module Updates

Network events (e.g., `FeedUpdateRunningEvent`, `SyncServiceEvent`, `MessageEvent`) are broadcast via GreenRobot EventBus rather than callbacks or LiveData. Subscribers in the UI layer react to these events.

## Building and Testing

All commands must be run from the project root (`/Users/tonytam/git/AntennaPod`), not from this `net/` folder.

```bash
# Compile only
./gradlew :app:assembleDebug

# Run tests for a specific net submodule
./gradlew --console=plain :net:common:test
./gradlew --console=plain :net:download:service:test
./gradlew --console=plain :net:sync:service:test

# Style checks
./gradlew checkstyle lint spotbugsPlayDebug spotbugsDebug
```

Tests live under `src/test/java/` in each submodule. Most net tests are pure JVM (no instrumentation needed).
