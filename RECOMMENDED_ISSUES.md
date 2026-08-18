# Comprehensive 20-Item Work Backlog for Tony Tam (@tonytamsf)

This document provides an expanded backlog of 20 high-impact GitHub issues and architectural explorations for `@tonytamsf`. It balances quick bug fixes, core UI/UX features, audio engine fixes, and long-term architectural explorations (such as the Plugin System and Media3 migration).

---

## Backlog Overview Table

| # | Issue / Topic | Type / Category | Local Branch / Prior Work | Key Area / Module |
|---|---------------|-----------------|---------------------------|-------------------|
| **1** | **#6805** Trimming silence past timestamp causes auto-skip to be missed | Bug Fix (Audio) | `issue-6805-auto-skip` | Playback Engine / Silence Skip |
| **2** | **#7274** HTTP request for podcast image doesn't send basic auth credentials | Bug Fix (Net/Auth) | `issue-7274-http-auth-images` | `:net:common` / `:ui:glide` |
| **3** | **#5851** Jump to last played episode in a podcast feed | UI Feature | Approved in Community Call | `:ui:episodes` / `:storage:database` |
| **4** | **#7920** Subscribe button hidden behind title with large system font | UI / Accessibility Bug | `issue-7920-header-size` | `:ui:common` / Accessibility |
| **5** | **#5573** Keep currently playing episode always on top of queue | Playback / Queue UX | Prior queue sorting PRs | `:playback:service` / `:storage:database` |
| **6** | **#7917** Extension / Plug-in System Architecture | Architectural Exploration | `claude-android-plugin-system...` | Extensibility / App Architecture |
| **7** | **#6608 / #8323** Media3 Session Callbacks & Skip Intro/Outro | Audio Engine Migration | `f267812d2` Media3 commits | `:playback:service` / Media3 |
| **8** | Podcast2 / WebVTT Transcript Parsing & Display | Feature / Exploration | `podcast2-transcription`, `transcript` | `:parser:transcript`, `:ui:transcript` |
| **9** | **#8657** Excessive repeated full-media GET requests during UI navigation | Performance Bug | Related to media download PRs | `:net:download:service`, `:playback:base` |
| **10** | **#8609** Sync missing queue status, playback position, & settings | Backend / Sync Bug | `cleanup/disk-space-sync` | `:net:sync:service` |
| **11** | Android Auto Resume & Voice Command Inconsistencies | Auto Integration Bug | `android-auto-resume`, `fix-auto-2` | `:playback:service` / Android Auto |
| **12** | **#7443** Discoverability of multi-select menu actions | UX / UI Refinement | Prior multi-select PRs | `:ui:common` / Action Modes |
| **13** | **#7622** Enable playback control buttons when chapter dialog is open | UI / Dialog Bug | `chapter-button-refresh` | `:ui:chapters` / Player UI |
| **14** | PodcastIndex Main Search API Integration | Discovery / Search Feature | `podcastindex-mainsearch` | `:net:discovery`, `:ui:discovery` |
| **15** | Global Filters for Podcast Feeds | Feature / Exploration | Community Call proposal | `:storage:preferences`, `:ui:episodes` |
| **16** | **#6743** Inbox progress indicators for downloading episodes | UI Feature | `delete_downloaded` | `:ui:episodes`, `:net:download` |
| **17** | **#1556** Persistent Queue Sorting / Keep Sorted | Queue Feature | `android-auto-sort-order` | `:storage:database`, `:ui:episodes` |
| **18** | **#5442** Gesture-based Chapter Navigation (Swipe to Chapters) | UI / Gesture UX | `landscape-large-chapter` | `:ui:chapters` / ViewPager |
| **19** | **#7185** Disk Space & Episode Auto-Cleanup Sync | Storage / Service Fix | `integrate-pr-7185` | `:storage:database-maintenance-service` |
| **20** | **#7736** Accept-Encoding HTTP Header Handling for Feed Downloads | Networking Bug | `fix-7736-accept-encoding` | `:net:common`, `:parser:feed` |

---

## Detailed Category Breakdown

### Category A: Immediate Bug Fixes & Resumable WIP Branches (Items 1–5 & 19–20)
1. **#6805: Silence Skipping bypassing Auto-Skip End**:
   - *Branch*: `issue-6805-auto-skip`
   - *Fix*: Ensure silence trimming triggers the auto-skip check if the jumped duration spans past the auto-skip threshold.
2. **#7274: Basic Auth Credentials Dropped for Feed Images**:
   - *Branch*: `issue-7274-http-auth-images`
   - *Fix*: Inject basic auth header interceptor into Glide image fetching pipelines when loading protected feed art.
4. **#7920: Subscribe Button Obscured by Scaled System Fonts**:
   - *Branch*: `issue-7920-header-size`
   - *Fix*: Constrain podcast header title TextView height/lines and enable dynamic text auto-sizing or wrapping.
19. **#7185: Disk Space & Maintenance Auto-Cleanup Sync**:
   - *Branch*: `integrate-pr-7185`
   - *Fix*: Finish integrating PR #7185 for periodic disk space cleanup tasks in `:storage:database-maintenance-service`.
20. **#7736: Accept-Encoding Header Handling**:
   - *Branch*: `fix-7736-accept-encoding`
   - *Fix*: Prevent feed parser crashes when podcast servers return unexpected compression formats.

---

### Category B: High-Impact UI & Queue Features (Items 3, 5, 12, 16, 17, 18)
3. **#5851: Jump to Last Played Episode in Feed**:
   - *Approved Priority*: Approved in Community Call (2022-05-14).
   - *Task*: Add quick-scroll FAB / menu action in podcast feed view jumping to the most recently listened item.
5. **#5573: Pin Currently Playing Episode to Top of Queue**:
   - *Task*: Prevent automated queue sorts (e.g. Clever Mix) from re-ordering the currently playing episode.
12. **#7443: Multi-Select UX Discoverability**:
   - *Task*: Add visual scroll indicators / cues to the multi-select contextual action bar.
16. **#6743: Download Progress Circle in Inbox Views**:
   - *Task*: Show active circular progress indicator in the Inbox list for items currently downloading.
17. **#1556: Permanent "Keep Sorted" Option for Queue**:
   - *Task*: Add persistent preference setting for queue sort order so new additions auto-arrange according to chosen rules.
18. **#5442: Swipe Gesture for Chapter List View**:
   - *Task*: Integrate chapter listing as a swipeable panel alongside show notes.

---

### Category C: Audio Engine & System Integrations (Items 7, 9, 11, 13)
7. **#6608 / #8323: Media3 Library Migration & Skip Intro/Outro Callback**:
   - *Task*: Address Media3 session callbacks (`:playback:service`) and restore "skip intro/outro" functionality under Android's Media3 playback engine.
9. **#8657: Redundant Full-Media GET Requests on Navigation**:
   - *Task*: Audit media player header pre-fetching during UI navigation to prevent unintended mobile data use.
11. **Android Auto Playback Resume & Voice Control**:
   - *Branches*: `android-auto-resume`, `fix-auto-2`
   - *Task*: Fix auto-resume failures when reconnecting to car head units.
13. **#7622: Accessible Playback Controls in Chapter Modal**:
   - *Branch*: `chapter-button-refresh`
   - *Task*: Expose play/pause/skip buttons inside the chapter selection dialog.

---

### Category D: Long-Term Architectural Explorations (Items 6, 8, 10, 14, 15)
6. **#7917: Plugin & Extension System Architecture**:
   - *Branches*: `claude-android-plugin-system-dle2n5`, `claude-antennapod-plugin-architecture-o2lee3`, `claude-antennapod-plugin-point-2077-3ly2yh`
   - *Exploration*: Design a secure plugin API mechanism (e.g. extension interfaces for custom sync backends, transcript providers, or SponsorBlock integration).
8. **Podcast 2.0 / WebVTT Transcript Parsing & RecyclerView Engine**:
   - *Branches*: `podcast2-transcription`, `transcript-step3-semantic`, `transcript-recycleview`
   - *Exploration*: Extend transcript support in `:parser:transcript` and `:ui:transcript` to handle synchronized scrolling and multiple subtitle formats (WebVTT, JSON, SRTT).
10. **#8609: Full Sync Engine Upgrade (Queue Position & Preferences)**:
   - *Task*: Expand sync service beyond basic subscriptions to cover queue state, playback positions, and user settings across devices.
14. **PodcastIndex Search API Integration**:
   - *Branch*: `podcastindex-mainsearch`
   - *Exploration*: Enhance `:net:discovery` to utilize PodcastIndex APIs for global podcast discovery and chapter metadata enrichment.
15. **Global Feed Filters Architecture**:
   - *Community Proposal*: Implement global filtering rules across podcast feeds (e.g. globally hiding played episodes with per-podcast override settings).

---

## Recommended Next Steps

1. Start by rebasing and reviewing your existing branch for **Issue #6805** or **Issue #7274**.
2. Run compilation to ensure clean build environment:
   ```bash
   ./gradlew :app:assembleDebug
   ```
