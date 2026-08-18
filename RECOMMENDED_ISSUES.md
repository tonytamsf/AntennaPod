# Next High-Impact Issues for Tony Tam (@tonytamsf)

This document outlines the recommended top 5 GitHub issues for `@tonytamsf` to take on next, taking into account past pull request branches, prior issue engagement, skills, community call alignment, and community discussions.

---

## Priority Recommendation Summary Table

| Rank | Issue # | Title / Description | Context / Prior Work | Key Area |
|------|---------|---------------------|----------------------|----------|
| **1** | **#6805** | Trimming silence past timestamp for auto-skip causes skip to be missed | Local branch `issue-6805-auto-skip`, `@tonytamsf` implemented original Auto-Skip feature | Playback / Audio Engine |
| **2** | **#7274** | HTTP request for podcast image doesn't send basic auth credentials | Local branch `issue-7274-http-auth-images`, prior unmerged fix | Networking & Glide / Auth |
| **3** | **#5851** | Jump to last played episode in a podcast feed | Approved in Community Call (2022-05-14), highly discussed in community | Feed UI & Storage |
| **4** | **#7920** | Subscribe button hidden behind podcast title with large system font | Local branch `issue-7920-header-size`, high UX visibility bug | UI Layout / Accessibility |
| **5** | **#5573** | Keep currently playing episode always on top of queue | High impact, top-discussed open issue in AntennaPod | Queue Management / Playback |

---

## Detailed Issue Breakdowns

### 1. Issue #6805: Trimming silence past timestamp for auto-skip causes skip to be missed
* **Status / History**: Directly aligns with your existing local work on branch `issue-6805-auto-skip`. You previously worked on auto-skip and silence trimming mechanisms.
* **Problem**: When both "Skip Silence" and "Auto Skip End" are enabled, skipping silence jumps the playback position past the auto-skip trigger point, causing the end of the episode auto-skip to be bypassed completely.
* **Why it fits**: You authored auto-skip logic in the past and already have a WIP branch for this issue.

---

### 2. Issue #7274: HTTP request for podcast image doesn't send basic auth credentials
* **Status / History**: Directly aligns with your local branch `issue-7274-http-auth-images`.
* **Problem**: When a podcast feed requires basic HTTP authentication, episode media and feed updates use credentials, but loading images via Glide/HTTP client drops basic auth credentials, resulting in broken image loading.
* **Why it fits**: Fixes media/image networking contracts. Re-engaging with this branch resolves an uncompleted fix you started.

---

### 3. Issue #5851: Jump to last played episode in a podcast feed
* **Status / History**: Approved for inclusion during the AntennaPod Monthly Community Call (2022-05-14 topic list) and frequently requested by users.
* **Problem**: Users with long podcast feeds find it difficult to scroll back to where they left off listening. The feature adds a quick action button in the podcast feed view to jump to the last played episode.
* **Why it fits**: Approved community priority, self-contained UI/storage logic, great entry point for re-acclimating to the codebase UI modules (`:ui:episodes`, `:storage:database`).

---

### 4. Issue #7920: Subscribe button hidden behind podcast title with large system font
* **Status / History**: Aligns with your local branch `issue-7920-header-size`.
* **Problem**: On devices with enlarged system text sizes / accessibility font scaling, the podcast title header expands and overlays/hides the "Subscribe" button on the podcast details screen.
* **Why it fits**: Accessibility and layout fix that restores core onboarding UX for vision-impaired users.

---

### 5. Issue #5573: Keep currently playing episode always on top of queue
* **Status / History**: One of the highest discussed open issues in the AntennaPod repository.
* **Problem**: When users sort the queue or use automatic reordering (such as "Clever Mix"), the currently playing episode gets moved from the top of the queue, breaking continuous playback flow.
* **Why it fits**: High user impact, directly addresses queue sorting behavior you have touched in past PRs (e.g. `delete_media_queue`, `android-auto-sort-order`).

---

## Getting Started Workflow

1. **Auto-Skip Fix**:
   ```bash
   git checkout issue-6805-auto-skip
   git rebase develop
   ```
2. **HTTP Basic Auth Images**:
   ```bash
   git checkout issue-7274-http-auth-images
   git rebase develop
   ```
3. **Build & Test**:
   ```bash
   ./gradlew :app:assembleDebug
   ```
