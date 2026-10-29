# System HUD: Solo Leveling Focus & Anti-Distraction App (v1.0)

An anime-styled native Android Focus, Goal Tracking, and Anti-Distraction application inspired by the **Solo Leveling "System" HUD**. It transforms deep work and study sessions into Dungeon Quests, rewarding completed focus sessions and daily quests with XP, Rank promotions (`E-RANK` to `S-RANK`), and timed Reward Unlocks while actively sealing away distracting apps, YouTube Shorts, and Instagram Reels via an Android Accessibility Service.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and architectural choices have been confirmed from your preferences and will govern the Version 1.0 build:

- **Confirmed Decision 1 — 100% Local On-Device Persistence**: All Hunter stats (Rank, Level, XP, Streak), Daily Quests, and Penalty Seal configurations are persisted locally on-device with zero login or cloud account required, ensuring instant availability for both the UI and the background `AccessibilityService`.
- **Confirmed Decision 2 — Full Penalty Seals Suite (Shorts/Reels + Installed Apps)**: Alongside dedicated `[ SEAL YOUTUBE SHORTS ]` and `[ SEAL INSTAGRAM REELS ]` toggles, the `[ PENALTY SEALS ]` screen will query installed user-launchable applications (`QUERY_ALL_PACKAGES`) so you can seal or unseal any distracting app on the device.
- **Confirmed Decision 3 — Pre-Loaded Starter System Quests + Custom Quests**: The `[ DAILY QUESTS ]` log initializes with signature Solo Leveling preparation/study quests out of the box and supports adding and deleting custom quests with +50 XP rewards and haptic spring feedback.

---

## 1. Overview & Core Concept

- **What It Does**: Operates as the Player's personal "System" interface with three core modules:
  1. **`[ STATUS & TIMER ]`**: Displays Hunter Rank, Level, Streak, and an animated Cyan/Purple XP bar inside a chamfered System Window, paired with a custom `Canvas` Mana Core countdown dial, Focus (`25m`, `45m`, `60m`, `90m`) and Reward (`10m`, `15m`, `30m`) duration selectors, and automatic transition from Focus Lock (`+100 XP`) into unsealed Reward Time.
  2. **`[ DAILY QUESTS ]`**: Styled after the `[ ! QUEST INFO ]` `[ DAILY QUEST: THE PLAYER'S PREPARATION ]` window, allowing players to create, complete (`+50 XP` with spring animation and haptic pulse), and remove daily study/work objectives while enforcing the bottom system warning that incomplete quests keep distraction seals locked.
  3. **`[ PENALTY SEALS ]`**: Houses `[ SYSTEM AUTHORITY: ACCESSIBILITY SERVICE ]` live status and one-tap deep link to `Settings.ACTION_ACCESSIBILITY_SETTINGS`, system toggles for YouTube Shorts and Instagram Reels, and a searchable/filterable installed-app blocker list.
- **Target Audience / Persona**: Students, developers, and creators seeking a high-immersion, gamified anti-distraction system that replaces bland productivity timers with a sharp dark-fantasy RPG command console.
- **Key Value**: Combines real OS-level distraction enforcement (blocking distracting packages and back-navigating out of Shorts/Reels feeds) with rewarding RPG progression (`[ LEVEL UP! ]` and `[ QUEST CLEARED: REWARD TIME UNLOCKED ]` overlays).

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **System Initialization & Status Check**: Launching the app opens the Abyssal HUD (`#050811`) with subtle background scanlines and a top Player Status Header showing current Rank (`E-RANK` through `S-RANK`), Level, Streak, and animated XP progress.
  2. **Entering a Focus Dungeon (`[ STATUS & TIMER ]`)**: The player selects a Focus duration (`25m`, `45m`, `60m`, `90m`) and Reward duration (`10m`, `15m`, `30m`), then taps `[ ENTER DUNGEON / START FOCUS ]`. The Mana Core dial shifts to Crimson Alert (`#FF1744`) as `[ DUNGEON LOCK: ACTIVE ]` engages, activating app and short-form video seals.
  3. **Quest Cleared & Reward Time**: Reaching `00:00` (or completing a cycle) triggers a glowing `[ QUEST CLEARED: REWARD TIME UNLOCKED ]` banner, grants `+100 XP` (triggering `[ LEVEL UP! ]` if the XP threshold is crossed), switches the Mana Core dial to Sovereign Gold (`#FFD600`), and temporarily unseals blocked apps for the chosen Reward countdown.
  4. **Daily Preparation (`[ DAILY QUESTS ]`)**: Players add new study/work goals or check off active quests. Checking off an item triggers a physical haptic click, spring scale animation, and `+50 XP` gain.
  5. **Configuring Seals (`[ PENALTY SEALS ]`)**: Players verify whether `FocusBlockerService` is enabled in Android Accessibility Settings, toggle `[ SEAL YOUTUBE SHORTS ]` and `[ SEAL INSTAGRAM REELS ]`, and toggle individual installed apps to be sealed when Dungeon Lock is active.

- **Visual Identity & Theme ("Solo Leveling System HUD")**:
  - *Aesthetic Direction*: Sharp, translucent dark-fantasy RPG System Windows floating over an abyssal void, using geometric HUD ticks, corner brackets, and glowing mana borders—strictly avoiding soft rounded Material 3 cards or pastel tones.
  - *Color Palette & Mood*:
    - **Abyssal Background**: Deep Void Black-Blue (`#050811`)
    - **System Window Surface**: Translucent Dark Navy (`#0A1224`)
    - **System Mana Blue (Primary Glow & Borders)**: Electric Cyan-Blue (`#00E5FF`) to Royal System Blue (`#1E88E5`) linear gradients
    - **Monarch Shadow Purple (Level Up & Rank Accent)**: (`#9D4EDD`)
    - **Penalty / Danger Red (Dungeon Lock & Active Restriction)**: Crimson Alert (`#FF1744`)
    - **Quest Cleared / Reward Gold (Reward Time Unlocked)**: Sovereign Gold (`#FFD600`)
  - *Typography & Hierarchy*: High-contrast `FontFamily.Monospace` with `FontWeight.ExtraBold` / `FontWeight.Black` and wide `2.sp` letter spacing for all System headers (`[ ! SYSTEM NOTIFICATION ]`, `[ DAILY QUEST: THE PLAYER'S PREPARATION ]`, `[ DUNGEON LOCK: ACTIVE ]`).
  - *Component Styling & Layout*:
    - **`SystemWindowCard`**: Reusable frame built with `CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)`, a `1.5.dp` `Brush.linearGradient` border, and `Canvas`-drawn L-shaped glowing corner brackets.
    - **Adaptive Multi-Screen Layout**: Uses `enableEdgeToEdge()` and `safeDrawingPadding()`. Via `BoxWithConstraints`, compact screens (`< 600.dp`) display a single-column System Window stack with a chamfered bottom HUD dock, while medium/expanded screens (`>= 600.dp`) render a 2-pane Hunter Command Console with a persistent left status/navigation rail and dual-panel workspace.

- **Interactive Feedback & Motion**:
  - Continuous subtle rotation/pulse on the `Canvas` Mana Core outer geometric ring when the timer is running.
  - Spring-animated (`spring(dampingRatio = Spring.DampingRatioMediumBouncy)`) quest completion rows paired with `LocalHapticFeedback`.
  - Animated `[ LEVEL UP! ]` and `[ QUEST CLEARED: REWARD TIME UNLOCKED ]` HUD alert banners when XP thresholds or timer milestones are reached.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Singleton Reactive `FocusRepository` with Synchronous Local Persistence**
  - *Chosen Approach*: A process-wide singleton `FocusRepository` exposing `StateFlow` streams backed by persistent local storage (`SharedPreferences` + JSON serialization for quests and blocked package sets).
  - *Why*: `FocusBlockerService` (`AccessibilityService`) runs independently of `MainActivity` lifecycle. A lightweight singleton repository backed by instant local persistence allows both the Compose UI (`FocusViewModel`) and the background `AccessibilityService` to read and react to `isBlockingActive`, `sealYoutubeShorts`, `sealInstagramReels`, and `blockedPackages` in real time with zero latency.
  - *Alternatives Considered*: Asynchronous-only database queries on every accessibility window event were rejected because `onAccessibilityEvent` fires at high frequency during scrolling and must evaluate blocking rules in sub-millisecond memory reads via `StateFlow.value`.

- **Decision 2: Dual-Layer Accessibility Enforcement (Package Home-Redirect + Node Tree Inspection)**
  - *Chosen Approach*: `FocusBlockerService` inspects `event.packageName` against `blockedPackages` when `isBlockingActive` is true (and not in Reward Time) and calls `performGlobalAction(GLOBAL_ACTION_HOME)`. Simultaneously, when inside YouTube or Instagram with Shorts/Reels seals enabled, it traverses `rootInActiveWindow` (`AccessibilityNodeInfo`) for view IDs and node descriptions matching YouTube Shorts (`reel_player`, `"Shorts"`) and Instagram Reels (`clips_viewer`, `"Reels"`) and triggers `performGlobalAction(GLOBAL_ACTION_BACK)`.
  - *Why*: Gives users granular control—allowing them to keep YouTube or Instagram accessible for educational videos or direct messages while strictly sealing short-form infinite scroll feeds, or locking the entire app during a Focus Dungeon.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM & OS LAYER                             │
│  ┌────────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │ FocusBlockerService                │  │ PackageManager & Settings      │  │
│  │ (AccessibilityService)             │  │ • QUERY_ALL_PACKAGES           │  │
│  │ • Package check -> GLOBAL_HOME     │  │ • ACTION_ACCESSIBILITY_SETTINGS│  │
│  │ • Node scan (Shorts/Reels)->BACK   │  └───────────────┬────────────────┘  │
└──┴─────────────────┬──────────────────┴──────────────────┼───────────────────┘
                     │ observes StateFlow                  │ queries apps & status
                     ▼                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     DATA & DOMAIN LAYER (FocusRepository)                    │
│  • PlayerStats StateFlow (level, xp, xpToNextLevel, rankTitle, streak)       │
│  • TimerState StateFlow (mode: IDLE / FOCUS_LOCK / REWARD_UNLOCK, remaining) │
│  • DailyQuests StateFlow (starter & custom quests, completion, +50 XP)       │
│  • PenaltySeals StateFlow (isBlockingActive, sealShorts, sealReels, pkgs)    │
│  • Local On-Device Storage Sync (SharedPreferences persistence)              │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │ collects & dispatches intents
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER (FocusViewModel + Compose HUD)           │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ Adaptive System HUD Shell (BoxWithConstraints: <600dp vs >=600dp)      │  │
│  │  • Persistent Top Player Status Header + Level-Up / Quest-Cleared HUD  │  │
│  ├───────────────────────┬────────────────────────┬───────────────────────┤  │
│  │ [ STATUS & TIMER ]    │ [ DAILY QUESTS ]       │ [ PENALTY SEALS ]     │  │
│  │ • Canvas Mana Core    │ • [ ! QUEST INFO ] HUD │ • Accessibility Card  │  │
│  │ • Focus (25/45/60/90) │ • Add/Delete Quest     │ • Shorts/Reels Toggles│  │
│  │ • Reward (10/15/30)   │ • Checkoff (+50 XP)    │ • Installed App List  │  │
│  │ • Dungeon Lock Toggle │ • Penalty Warning Box  │   with Search & Seal  │  │
│  └───────────────────────┴────────────────────────┴───────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **`HunterRank` Progression**:
  - Level 1–2: `E-RANK HUNTER`
  - Level 3–4: `D-RANK HUNTER`
  - Level 5–7: `C-RANK HUNTER`
  - Level 8–10: `B-RANK HUNTER`
  - Level 11–14: `A-RANK HUNTER`
  - Level 15+: `S-RANK SHADOW MONARCH`
- **`DailyQuest` Entity**: `id`, `title`, `targetDetail`, `xpReward` (`50`), `isCompleted`, `isStarter`.
- **`TimerPhase` State**:
  - `IDLE`: Ready to configure Focus (`25m`, `45m`, `60m`, `90m`) and Reward (`10m`, `15m`, `30m`) durations.
  - `FOCUS_LOCK`: Countdown active, `isBlockingActive = true`, Crimson Alert HUD state (`[ DUNGEON LOCK: ACTIVE ]`).
  - `REWARD_UNLOCK`: Triggered automatically when Focus hits `00:00` (`+100 XP` awarded), `isBlockingActive = false` (temporary unseal), Sovereign Gold HUD state (`[ QUEST CLEARED: REWARD TIME UNLOCKED ]`).

### Interactive Component & State Mapping

- **Mana Core Timer Controls**:
  - Selecting Focus (`25m`, `45m`, `60m`, `90m`) or Reward (`10m`, `15m`, `30m`) pills updates the target countdown duration when in `IDLE` mode.
  - Starting the Focus Timer activates a coroutine ticker in `FocusViewModel`/`FocusRepository`, sets `isBlockingActive = true`, and transitions the `Canvas` Mana Core to Crimson Red.
  - Reaching `00:00` on Focus automatically calls `completeFocusSession()`, awards `+100 XP`, increments Streak, displays the `[ QUEST CLEARED: REWARD TIME UNLOCKED ]` alert, and starts the Sovereign Gold Reward countdown with `isBlockingActive = false`.
- **Daily Quests Tracker**:
  - Input bar validates non-empty quest titles and appends a new `DailyQuest` to `FocusRepository`.
  - Toggling a quest checkbox from incomplete to complete awards `+50 XP`, triggers haptic feedback, and evaluates level-up progression (`[ LEVEL UP! ]` banner).
  - Deleting a quest removes it from the list and persists the updated state.
- **Penalty Seals & Accessibility Authority**:
  - Lifecycle-aware check (`LifecycleEventObserver` on `ON_RESUME`) refreshes whether `FocusBlockerService` is enabled in `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.
  - Tapping `[ GRANT SYSTEM AUTHORITY ]` launches `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)`.
  - Toggling `[ SEAL YOUTUBE SHORTS ]`, `[ SEAL INSTAGRAM REELS ]`, or any app in the installed apps list updates `FocusRepository` immediately so `FocusBlockerService` enforces the updated rules in real time.
