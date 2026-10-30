# System HUD: Solo Leveling Focus & Anti-Distraction App (v1.1 Upgrade)

Version 1.1 upgrades **System Focus HUD** with a **Quota-Safe Single-Request Gemini "System Arbiter"** for evaluating Daily Quests, a **supercharged OS-level Accessibility App & Shorts/Reels Blocker** that works even when the main UI is closed, and the **GitHub Actions Gradle debug signing fix**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions have been confirmed for Version 1.1:

- **Confirmed Decision 1 — Auto-Add `COMPLY` & `MODIFY` Quests with Verdict Popup**: When `[ SUBMIT QUEST TO SYSTEM ]` is pressed, the System Arbiter evaluates the quest in a single request. If the verdict is `COMPLY` or `MODIFY`, the approved/rewritten quest (`finalQuestTitle`) and its dynamic difficulty reward (`50`–`150 XP`) are automatically saved to the Daily Quests log while displaying the glowing `[ ! SYSTEM ARBITER VERDICT ]` popup window (`DECLINE` quests are rejected and not added).
- **Confirmed Decision 2 — Gradle Debug Signing Fix Only**: We will update `app/build.gradle.kts` so the `debug` buildType does not reference `debugConfig` or require `debug.keystore` (allowing GitHub Actions to build with Android's default debug key), without creating a new `.github/workflows/build.yml` file (and preserving any existing workflow if present).
- **Confirmed Decision 3 — Offline Heuristic Arbiter Fallback**: If no Gemini API key is configured in `[ API KEY CONFIG ]` or the device is offline, the System Arbiter seamlessly falls back to a local Solo Leveling heuristic judge so quest submission never fails or crashes.

---

## 1. Overview & Core Concept

- **What Version 1.1 Delivers**:
  1. **Supercharged Real-Time Accessibility Blocker (`FocusBlockerService`)**:
     - Calls `FocusRepository.init(applicationContext)` inside both `onServiceConnected()` and `onAccessibilityEvent()` so the service always has live state (`isBlockingActive`, `blockedPackages`, `sealYoutubeShorts`, `sealInstagramReels`) even if the app process was restarted in the background.
     - **Multi-Window & Multi-Signal Node Scanner**: Inspects `rootInActiveWindow` and interactive `windows` recursively across `viewIdResourceName`, `text`, and `contentDescription` with an **800ms debounce cooldown** (`lastBlockActionTime`) to detect YouTube Shorts (`reel_player`, `shorts_`, `reel_recycler`, or selected `"Shorts"` tab) and Instagram Reels (`clips_viewer`, `reel_viewer`, `clips_video`, or active `"Reels"` tab) and trigger `performGlobalAction(GLOBAL_ACTION_BACK)`.
     - **Real App Blocking + System Penalty Toast**: When `isBlockingActive` is true (via Focus Timer or `[ FORCE DUNGEON LOCK (TEST MODE) ]`), opening any package in `blockedPackages` (excluding our app, `com.android.systemui`, `com.android.settings`, and the default home launcher) triggers `performGlobalAction(GLOBAL_ACTION_HOME)` and displays a Toast warning: `"[ ! SYSTEM PENALTY: ACCESS DENIED UNTIL FOCUS COMPLETE ]"`.
  2. **Quota-Safe Single-Request Gemini "System Arbiter" (`[ DAILY QUESTS ]`)**:
     - Adds a sleek collapsible `[ API KEY CONFIG ]` panel at the top of `[ DAILY QUESTS ]` to paste and save a Gemini API key in `SharedPreferences`.
     - Executes strictly **one** HTTPS POST request via `HttpURLConnection` on `Dispatchers.IO` to `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` (`responseMimeType = "application/json"`) **only** when the Player taps `[ SUBMIT QUEST TO SYSTEM ]`.
     - Parses structured JSON (`verdict`: `"COMPLY"` | `"MODIFY"` | `"DECLINE"`, `finalQuestTitle`, `xpReward`: `50..150`, `systemMessage`) and displays a glowing `[ ! SYSTEM ARBITER VERDICT ]` popup window styled in **Electric Cyan** (`COMPLY`), **Monarch Purple** (`MODIFY`), or **Crimson Red** (`DECLINE`).
  3. **GitHub Actions Gradle Compatibility**:
     - Ensures `INTERNET` permission is declared in `AndroidManifest.xml` for the System Arbiter request.
     - Removes/comments out `signingConfig = signingConfigs.getByName("debugConfig")` inside `buildTypes.debug` in `app/build.gradle.kts`.

---

## 2. User Experience & Visual Design

- **Key User Flows in v1.1**:
  1. **Configuring & Using the System Arbiter (`[ DAILY QUESTS ]`)**:
     - Tapping `[ API KEY CONFIG ]` expands a chamfered HUD drawer where the Player can paste and save their Gemini API key locally.
     - Typing a goal (e.g., *"study math"* or *"watch anime for 3 hours"*) and tapping `[ SUBMIT QUEST TO SYSTEM ]` shows an animated `[ ARBITER EVALUATING... ]` state, performs a single request, auto-adds `COMPLY`/`MODIFY` quests with their assigned `50`–`150 XP` reward, and opens the glowing `[ ! SYSTEM ARBITER VERDICT ]` modal window.
  2. **Immediate Blocking Verification (`[ PENALTY SEALS ]`)**:
     - Toggling `[ FORCE DUNGEON LOCK (TEST MODE) ]` on the `[ PENALTY SEALS ]` screen immediately activates `isBlockingActive = true` in `SharedPreferences` and `StateFlow`, allowing instant testing of app blocking (`GLOBAL_ACTION_HOME` + Penalty Toast) and Shorts/Reels blocking (`GLOBAL_ACTION_BACK`) without waiting for the timer.

- **Visual Identity & Theme**:
  - **Verdict Color Coding**:
    - `COMPLY`: Electric Cyan-Blue (`#00E5FF`) border & header glow
    - `MODIFY`: Monarch Shadow Purple (`#9D4EDD`) border & header glow
    - `DECLINE`: Penalty Crimson Red (`#FF1744`) border & header glow

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Zero-Dependency `HttpURLConnection` + `org.json` for Gemini 2.5 Flash**
  - *Chosen Approach*: Perform the single POST request on `Dispatchers.IO` using Android's built-in `HttpURLConnection` and `org.json.JSONObject` with `generationConfig.responseMimeType = "application/json"`.
  - *Why*: Eliminates any risk of background SDK retries, polling, or extra dependency bloat, guaranteeing strictly 1 HTTP call per button tap and preserving the user's free Gemini API quota.
- **Decision 2: Synchronous `SharedPreferences` Sync Between UI and `FocusBlockerService`**
  - *Chosen Approach*: `FocusRepository.init(context)` loads and synchronizes state from `SharedPreferences`, and `FocusBlockerService.onAccessibilityEvent()` calls `FocusRepository.init(applicationContext)` and reads the latest preferences/flows before evaluating package and node rules.
  - *Why*: Ensures 100% reliability even when the Android OS kills or backgrounds the UI activity while `FocusBlockerService` remains active.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM & OS LAYER                             │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ FocusBlockerService (AccessibilityService)                             │  │
│  │ • onServiceConnected() & onAccessibilityEvent() -> FocusRepository.init│  │
│  │ • 800ms debounce check (lastBlockActionTime)                           │  │
│  │ • Blocked Package -> GLOBAL_ACTION_HOME + System Penalty Toast         │  │
│  │ • Multi-Window Node Scanner (rootInActiveWindow + windows):            │  │
│  │   - YouTube Shorts (reel_player / shorts_ / "Shorts") -> GLOBAL_BACK   │  │
│  │   - Instagram Reels (clips_viewer / reel_viewer / "Reels") -> BACK     │  │
│  └───────────────────────────────────┬────────────────────────────────────┘  │
└──────────────────────────────────────┼───────────────────────────────────────┘
                                       │ SharedPreferences + StateFlow
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                 DATA & ARBITER LAYER (FocusRepository + SystemArbiter)       │
│  • Single-Request Gemini 2.5 Flash Client (HttpURLConnection, Dispatchers.IO)│
│  • Offline Heuristic Arbiter Fallback (when key is blank or offline)         │
│  • Persists Gemini API Key, Quests (50-150 XP), Force Lock & Seals locally   │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER (FocusViewModel + Compose HUD)            │
│  • [ DAILY QUESTS ]: Collapsible [ API KEY CONFIG ], [ SUBMIT QUEST TO       │
│    SYSTEM ], and glowing [ ! SYSTEM ARBITER VERDICT ] modal popup            │
│  • [ PENALTY SEALS ]: [ FORCE DUNGEON LOCK (TEST MODE) ] + App/Shorts Seals  │
└──────────────────────────────────────────────────────────────────────────────┘
```
