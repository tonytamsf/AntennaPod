# :plugin:chaptermarkers

Reference `:plugin:api` implementation that generates chapter markers for downloaded audio episodes
that do not already provide any.

`ChapterMarkerMediaProcessor` is a `DownloadedMediaProcessor`. It processes an episode only when an
engine is available, the media is audio, the file is downloaded, and the item has no chapters from
any existing source (embedded/simple chapters or a Podcast Index chapter URL). When the engine
produces chapters, they are stored via `DBWriter.setFeedItem`, so the rest of the app treats them
exactly like embedded or Podcast Index chapters.

## Engines

The actual chapter detection is abstracted behind `ChapterMarkerEngine`, so the back end is swappable:

- `DisabledChapterMarkerEngine` — the default no-op engine (`isAvailable()` returns `false`). It lets
  the extension point ship and be exercised without bundling a chapter-detection model.

To make chapter generation actually run, implement `ChapterMarkerEngine` (e.g. silence/segment
detection, or a cloud API) and register the processor with it in `ClientConfigurator`.

## Relationship to other plugins

This module is completely independent of `:plugin:transcription`. Both depend only on the shared
`:plugin:api` contract and are registered separately. Adding this second plugin required no changes to
`:plugin:api`, to the download pipeline, or to the transcription plugin.
