# System HUD: Solo Leveling Focus & Anti-Distraction App (v1.2 Upgrade)

Version 1.2 overhauls the reward loop into a **Dopamine-Tuned Reward Token Economy**, introduces **Timer-Quest Binding**, adds a full-screen **`WindowManager` Crimson System Penalty Overlay** (`SYSTEM_ALERT_WINDOW`), fixes the **Quest Checkbox XP Exploit** with de-leveling support, and transitions `[ DAILY QUESTS ]` to a **midnight-resetting zero-state log**—while strictly preserving your `.github/workflows/build.yml` and `app/build.gradle.kts` debug signing configuration.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following mechanics have been locked in from your clarifications for Version 1.2:

- **Confirmed Decision 1 — Scientifically Tuned Focus Threshold & Token Yield**:
  - **5-Minute Activation Threshold**: Focusing for under **5 minutes** yields `0 Tokens` (preventing micro-session farming). Once the Player passes the **5-minute threshold** (or completes any focus preset: `15m`, `25m`, `35m`, `50m`, `70m`), tokens are awarded.
  - **Fixed Token Ratio (`35m Focus = 100 Tokens = 10m App Usability`)**: Every **3.5 minutes** of Focus grants **10 Reward Tokens** (`~2.86 Tokens/min`), where **10 Tokens = 1 minute of app usability**.
- **Confirmed Decision 2 — Fixed-Price Per-App Token Exchange (`10m` to `60m` Multiples, Stackable)**:
  - Removed automatic global break time. Instead, in `[ PENALTY SEALS ]`, each blocked app can be independently unsealed using fixed-price Token Passes in **10-minute multiples up to 1 hour** per transaction:
    - `10m` = **100 Tokens** | `20m` = **200 Tokens** | `30m` = **300 Tokens** | `40m` = **400 Tokens** | `50m` = **500 Tokens** | `60m (1 Hr)` = **600 Tokens**
  - **Independent & Stackable**: Unsealing one app (e.g., Instagram) leaves all other blocked apps strictly sealed. Repeat transactions on the same app stack additional time with no cap.
- **Confirmed Decision 3 — Timer-Quest Binding, XP Reversal, & Empty Midnight Reset**:
  - Players can bind an active Daily Quest directly to the Focus Timer in `[ STATUS & TIMER ]`; reaching `00:00` auto-completes the bound quest, grants its XP + Focus XP, and deposits Reward Tokens.
  - Unchecking a completed quest **deducts** its exact XP reward (and de-levels the Player if XP drops below the current level floor).
  - `[ DAILY QUESTS ]` starts at **0 quests** (no pre-loaded starter quests) and automatically clears/resets at midnight (`00:00` local date transition).

---

## 1. Overview & Core Concept

- **What Version 1.2 Delivers**:
  1. **The Reward Token Economy (`FocusRepository` + `[ STATUS & TIMER ]` + `[ PENALTY SEALS ]`)**:
     - Removes the old Reward Time selector from `[ STATUS & TIMER ]`.
     - Displays the Player's live **Reward Token Vault** (`[ REWARD TOKENS: X ]`) in the HUD header and `[ PENALTY SEALS ]`.
     - Calculates and deposits Reward Tokens whenever a focus session completes (`00:00`) or is claimed after passing the **5-minute minimum focus threshold** (`tokens = floor((focusedMinutes / 3.5) * 10)`).
     - Adds the **Token Exchange Window** in `[ PENALTY SEALS ]`: select any currently sealed app, pick a fixed-price pass (`10m` for `100T`, `20m` for `200T`, `30m` for `300T`, `40m` for `400T`, `50m` for `500T`, `60m` for `600T`), and unseal **only** that specific package with a live countdown badge (`UNSEALED: MM:SS REMAINING`).
  2. **Timer-Quest Binding (`[ STATUS & TIMER ]`)**:
     - Adds a chamfered `[ BIND ACTIVE DAILY QUEST ]` selector on `[ STATUS & TIMER ]` listing all incomplete quests from `[ DAILY QUESTS ]` (plus `[ NONE — FREE FOCUS DUNGEON ]`).
     - When the Focus Timer reaches `00:00`, if a quest is bound, it automatically marks that quest completed, awards the quest's XP (`50`–`150 XP`) + `+100 XP` Dungeon clear bonus, and deposits the earned Reward Tokens.
  3. **System Overlay Kick-Out & `SYSTEM_ALERT_WINDOW` (`FocusBlockerService`)**:
     - Declares `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />` in `AndroidManifest.xml`.
     - Checks `Settings.canDrawOverlays(context)` on resume and renders a glowing `[ GRANT OVERLAY SYSTEM AUTHORITY ]` button (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`) when missing.
     - When `FocusBlockerService` intercepts a sealed app (whose package is not currently unsealed in the Token Exchange) or a sealed Shorts/Reels feed, it triggers `GLOBAL_ACTION_HOME` / `GLOBAL_ACTION_BACK` AND uses `WindowManager` (`LayoutParams.TYPE_APPLICATION_OVERLAY`) to display a full-screen, un-dismissible **Crimson Red Solo Leveling Penalty Overlay** (`[ ! SYSTEM PENALTY: ACCESS DENIED ]` / `[ ! SYSTEM PENALTY: DUNGEON LOCK ACTIVE ]`) for **3 seconds**.
  4. **Quest XP Exploit Fix & Midnight Empty Reset (`[ DAILY QUESTS ]`)**:
     - Toggling a quest from `completed -> incomplete` subtracts `quest.xpReward` from `PlayerStatsState` (and steps down `level` and `HunterRank` if `currentXp < 0`).
     - Removes all starter quests so `[ DAILY QUESTS ]` initializes with an empty list (`0` quests) and checks the current calendar date (`yyyy-MM-dd`) on init/resume to automatically clear the daily quest log at midnight.

---

## 2. User Experience & Visual Design

- **Key User Flows in v1.2**:
  1. **Binding a Quest & Earning Tokens (`[ STATUS & TIMER ]`)**:
     - Select a Focus duration (`5m`, `15m`, `25m`, `35m`, `50m`, `70m` — showing the exact Token payout on each chip, e.g., `35m (+100T)`), optionally bind an active Daily Quest, and tap `[ ENTER DUNGEON ]`.
     - Reaching `00:00` (or claiming after `>= 5m` focused) deposits the Reward Tokens, auto-completes the bound quest, and triggers `[ QUEST CLEARED: +100 TOKENS DEPOSITED ]`.
  2. **Redeeming Tokens Per App (`[ PENALTY SEALS ]`)**:
     - Open the `[ TOKEN EXCHANGE: TEMPORARY UNSEAL ]` card, pick a sealed app, choose a fixed-price multiple (`10m / 100T` up to `60m / 600T`), and tap `[ REDEEM PASS ]`. Only that app is unsealed for the redeemed duration; tapping it again stacks more time.
  3. **Aggressive System Penalty Overlay Kick-Out**:
     - Attempting to open a locked app or scroll YouTube Shorts / Instagram Reels while sealed immediately kicks the user out and flashes a full-screen Crimson HUD overlay drawn directly via `WindowManager` for 3 seconds.

---

## 3. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM & OS LAYER                             │
│  ┌────────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │ FocusBlockerService                │  │ System Overlay Authority       │  │
│  │ • Checks per-app unseal expiry     │  │ • SYSTEM_ALERT_WINDOW          │  │
│  │ • GLOBAL_HOME / GLOBAL_BACK        │  │ • Settings.canDrawOverlays()   │  │
│  │ • WindowManager Crimson Overlay    │  │ • ACTION_MANAGE_OVERLAY_PERM   │  │
│  │   (3s un-dismissible kick-out)     │  └────────────────────────────────┘  │
└──┴─────────────────┬──────────────────┴──────────────────────────────────────┘
                     │ SharedPreferences + StateFlow
                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     DATA & ECONOMY LAYER (FocusRepository)                   │
│  • Reward Tokens Balance (35m Focus = 100 Tokens; 5m min threshold)          │
│  • Per-App Unseal Expiry Map (packageName -> expiryEpochMillis, stackable)   │
│  • Bound Quest ID -> Auto-complete + XP + Token deposit at 00:00             │
│  • Bidirectional XP Engine (addXp on check, deductXp + de-level on uncheck)  │
│  • Midnight Date Guard (yyyy-MM-dd check clears Daily Quests at 00:00)       │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER (FocusViewModel + Compose HUD)           │
│  ┌───────────────────────┬────────────────────────┬───────────────────────┐  │
│  │ [ STATUS & TIMER ]    │ [ DAILY QUESTS ]       │ [ PENALTY SEALS ]     │  │
│  │ • Token Balance Badge │ • Starts Empty (0)     │ • Overlay Perm Button │  │
│  │ • Bind Quest Selector │ • Gemini System Arbiter│ • Token Exchange UI   │  │
│  │ • Focus Presets w/    │ • Check (+XP) /        │   (10m..60m passes    │  │
│  │   5m Min Threshold    │   Uncheck (-XP Fix)    │   per individual app) │  │
│  └───────────────────────┴────────────────────────┴───────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```
