# System HUD: Block Kick-Out Popup & 60/120 FPS Smoothness Fix Plan

This plan resolves the four issues with the **Block Kick-Out Popup** (unresponsive buttons, stuck 3-second countdown timer, frozen screen gestures) and eliminates **UI lag** across the app while keeping the exact same **Solo Leveling System HUD** design and UX.

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> Your preferences from the clarification step are locked in:

- **Confirmed Decision 1 — 3-Second Overlay Countdown Then Kick to Home**:
  - Opening a blocked app immediately covers it with the full-screen Crimson System Penalty popup.
  - The popup's **3-second countdown timer** (`3s` → `2s` → `1s`) ticks smoothly without resetting, and automatically kicks the user to the **Android Home screen** as soon as the 3 seconds finish.
  - Tapping `[ RETURN TO SAFE ZONE (HOME) ]` or `[ VIEW DAILY QUESTS IN SYSTEM HUD ]` responds immediately on the first tap, cancels the timer, dismisses the popup, and navigates directly to Home or Daily Quests.
- **Confirmed Decision 2 — Zero-Lag 60/120 FPS Performance with the Same Solo Leveling HUD UI**:
  - Keep the exact same Solo Leveling System HUD aesthetic (chamfered glowing borders, Mana Core Timer Dial, scanlines, and color palette) while eliminating background service thrashing, redundant disk/JSON reads, and unnecessary per-frame redraws.

---

## 1. Root Causes Identified & How We Will Fix Them

### Issue 1, 2 & 3: Popup Buttons Not Working, 3s Timer Stuck, and Gestures Freezing
- **Why it happens**:
  - When the `WindowManager` overlay covers a blocked app (e.g., YouTube), Android still reports the blocked app as the foreground window behind the overlay.
  - Both `FocusShieldForegroundService` (polling every 350ms) and `FocusBlockerService.onAccessibilityEvent` (throttled at 450ms) detect the blocked app again and call `displaySystemPenaltyOverlayInsideApp()` **nearly 3 times per second**.
  - Each call **destroys and recreates** the overlay view and **cancels and restarts** `CountDownTimer(3000L, 1000L)` back to `3s`! Because the view is destroyed every ~350ms before a finger tap (`ACTION_DOWN` → `ACTION_UP`) or a 1-second timer tick can complete, buttons never fire, the timer stays frozen at `3s`, and `WindowManager` touch/gesture dispatch gets flooded.
- **The Fix**:
  1. **Single-Instance Overlay Guard**: Track active overlay state (`isOverlayShowing`) across both `FocusBlockerService` and `FocusShieldForegroundService`. Once the penalty overlay is displayed for a blocked app, all subsequent polls/events are ignored until the 3-second countdown completes or the user taps a button.
  2. **Smooth 3-Second Countdown → Auto-Kick to Home**: Run a single, uninterrupted `CountDownTimer(3000L, 250L)` that updates the countdown badge (`3s` → `2s` → `1s`) and, in `onFinish()`, cleanly removes the overlay and kicks the user to the Android Home screen (`GLOBAL_ACTION_HOME` + `CATEGORY_HOME` intent) with a brief post-kick transition guard.
  3. **Instant First-Tap Button & Back-Gesture Handling**: Wire `[ RETURN TO SAFE ZONE (HOME) ]` and `[ VIEW DAILY QUESTS IN SYSTEM HUD ]` (plus system Back gestures in both the `WindowManager` overlay and `BlockerOverlayActivity`) to immediately cancel the timer, remove the overlay once, and route to Home or the Daily Quests tab.

### Issue 4: App Running Laggy on High-Refresh-Rate Devices
- **Why it happens**:
  - `FocusBlockerService.onAccessibilityEvent()` calls `FocusRepository.init(applicationContext)` on **every accessibility event**, which re-reads `SharedPreferences`, parses JSON strings, and calls `startForegroundService()` dozens of times per second on the main UI thread.
  - `FocusBlockerService` listens to `TYPES_ALL_MASK` with a `30ms` timeout and runs a 24-level recursive view-tree scan across all apps on every scroll frame.
  - `ManaCoreTimerDial` runs two infinite Canvas animations (`rotationDegrees` and `pulseAlpha`) at 60–120 FPS even when the timer is idle/stopped, and `MainActivity` collects a 1-second `currentTimeMillis` ticker at the root composable, forcing full-screen recomposition every second.
- **The Fix**:
  1. **One-Time Repository Init**: Guard `FocusRepository.init()` with an `@Volatile var isInitialized` check so it initializes once at startup and never re-parses JSON or restarts the foreground service on accessibility events.
  2. **Targeted Accessibility Event Filtering**: Restrict `FocusBlockerService` to `TYPE_WINDOW_STATE_CHANGED | TYPE_WINDOW_CONTENT_CHANGED` (`120ms` notification timeout) and only run the Shorts/Reels node-tree scan when the active package is actually YouTube or Instagram.
  3. **Cached HUD Drawing & Idle Animation Pause**: Use `Modifier.drawWithCache` for `SystemWindowCard`, `SystemBackgroundScanlines`, and static `ManaCoreTimerDial` geometry so paths and gradients are cached instead of allocated every frame, and pause the infinite dial rotation when the timer is not running.
  4. **Eliminate Root-Level 1s Recomposition**: Scope timestamp updates so the root `MainActivity` HUD does not recompose the entire screen tree every second.

---

## 2. Technical Architecture & Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   BLOCKED APP INTERCEPTION FLOW (FIXED)                     │
│                                                                             │
│  Blocked App Opened (e.g., YouTube / Instagram)                             │
│         │                                                                   │
│         ▼                                                                   │
│  [ isOverlayShowing == true? ] ──(YES)──► Ignore duplicate poll/event       │
│         │ (NO)                            (No view destroy/recreate loop!)  │
│         ▼                                                                   │
│  Show Single Crimson Penalty Overlay (isOverlayShowing = true)              │
│  Start Single 3.0s CountDownTimer (ticks 3s -> 2s -> 1s smoothly)           │
│         │                                                                   │
│         ├──► User taps [ RETURN TO SAFE ZONE ] ──► Cancel timer, remove     │
│         │    or System Back Gesture                overlay, launch Home     │
│         │                                                                   │
│         ├──► User taps [ VIEW DAILY QUESTS ] ────► Cancel timer, remove     │
│         │                                          overlay, open Quests Tab │
│         │                                                                   │
│         └──► 3.0s Timer Reaches 00:00 ───────────► Remove overlay & kick    │
│                                                    blocked app to Home      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Files to Modify

- `app/src/main/java/com/example/service/FocusBlockerService.kt`:
  - Unify overlay state management (`isOverlayShowing`, single `activeOverlayView`, single `activeCountdownTimer`) so the popup is never destroyed/recreated mid-countdown.
  - Fix the 3-second countdown timer (`3s` → `2s` → `1s` → remove overlay + kick to Home screen) and make both popup buttons and Back key/gesture work on the first tap.
  - Remove per-event `FocusRepository.init()` calls and restrict deep node scanning strictly to YouTube/Instagram when Shorts/Reels seals are active.
- `app/src/main/java/com/example/service/FocusShieldForegroundService.kt`:
  - Skip triggering `enforceExternalAppBlock` while the penalty overlay is already on screen or during the brief post-kick Home transition.
- `app/src/main/java/com/example/ui/BlockerOverlayActivity.kt`:
  - Add a matching 3-second countdown timer with auto-kick to Home and update Back gesture handling so swiping/pressing Back exits to Home cleanly.
- `app/src/main/java/com/example/data/FocusRepository.kt`:
  - Make `FocusRepository.init()` idempotent (`isInitialized` guard) so `SharedPreferences` and JSON parsing only run once.
- `app/src/main/java/com/example/ui/components/SystemHudComponents.kt`, `app/src/main/java/com/example/ui/FocusViewModel.kt`, & `app/src/main/java/com/example/MainActivity.kt`:
  - Cache HUD Canvas drawing (`drawWithCache`), pause idle infinite transitions when the timer is stopped, and stop root-level 1-second full-screen recompositions.
