# Batch F: five fixes for the home screen and the key service (design note)

[English](BATCH_F_FIXES.md) · [العربية](BATCH_F_FIXES.ar.md)

Status: implemented on the `fix/batch-f-inherited-fixes` branch for the next release (listed under "Unreleased" in the
changelog). Nothing here adds a dependency, a permission or a service, and every change is generic: no device name is used
in a rule. Where a device quirk is involved, the code asks the system whether the thing exists (a capability check) and
otherwise keeps today's behaviour.

The five problems are known from another Android TV launcher that shares part of our code history. We only took the
**ideas** (what breaks and where); the code below is written here. The line numbers are those of `main` at `8b33e2b`, each
one re-read before this note was written. Nothing was run on a TV: section 7 lists what only a TV can show.

## 1. F1: a settings change leaves the old Home screen in memory

**Problem.** Changing a Launcher setting calls `recreate()`. Anything that outlives the old activity and points to it keeps
the whole screen (views, bitmaps) in memory. Two things do.

**Where (`MainActivity.java` on `main`).**
- `:519` adds a global focus listener to the decor view as an inline lambda; `onDestroy` (`:749-759`) never removes it.
  Our **hypothesis** (not proved) is that `recreate()` keeps the window and its `ViewTreeObserver`, so every old activity
  stays on the listener list. The proof is the `dumpsys meminfo` loop on a TV described below; until it is run, this
  is an educated guess from the code, not a measured leak.
- `ConnectivityManager` and `TvInputManager` are taken from the activity at `:529`, `:530`, `:752`, `:753`, `:1955`, `:2094`
  and `:2165`. On Android 12 a manager obtained from an Activity can keep that Activity reachable. For `TvInputManager`
  this is not known; taking it from the application context is harmless either way.
- The triggers are `recreate()` at `:642` (a festival started or ended) and `:672` (Launcher settings closed).

**Fix.** The listener becomes a field (`focusListener`) removed in `onDestroy`, guarded by `ViewTreeObserver.isAlive()`.
Two private helpers, `connectivity()` and `tvInputs()`, take the managers from `getApplicationContext()` at every call
site. The network and input callbacks were already fields and were already unregistered.

**Failure-case test.** `HomeLifecycleSourceTest` reads `MainActivity.java` and fails if a global focus listener is added
without a matching removal, or if `ConnectivityManager` or `TvInputManager` is taken from the activity. On `main` it fails
(1 listener added, 0 removed; 7 services taken from the activity); after the fix it passes. The checker is also tested on
small samples of good and bad code. This is a source-level guard, not a leak test: the leak needs the Android framework.

**Cannot be tested without a TV.** That the leak exists at all and that the memory really stays flat after the fix. The test on a device (not run here): `adb shell dumpsys
meminfo com.gathin.tvhome`, note `Activities:` and TOTAL PSS, change a Launcher setting 8 times, read again. Expected after
the fix: `Activities` 1 to 2 and PSS within a few MB. This is also the acceptance test of the memory work planned for 3.1.

## 2. F2: a remote button can stop the key service

**Problem.** If the app a remote button should open refuses to start, `startActivity` throws and the exception leaves
`onKeyEvent`, which can stop the accessibility service (and with it the remote buttons).

**Where.** `HomeKeyService.java:104` (Settings button), `:114` (`app_keys` button), `:124` (Home button): no try/catch.
`InputKeyService.java:23-26` catches `ActivityNotFoundException` only; a `SecurityException` still escapes.

**Fix.** A small class `SafeStart` runs a start action, catches `ActivityNotFoundException` and `SecurityException`,
returns whether it started, and reports each failing target **once** to a log sink (class name only, no package names or
extras). The two services call it for every `startActivity`. Any other exception still propagates, so real bugs are not
hidden.

**Failure-case test.** `SafeStartTest`: an action that throws `SecurityException` returns false and does not throw; the
same target fails twice and is reported once; a different target is reported again; an unrelated exception still
propagates; a successful start is not reported.

**Cannot be tested without a TV.** Which real target refuses to start, and what the system does to a service that throws.

## 3. F3: the home clock after standby on older Android

**Problem.** Before Android 12 a `TextClock` moves only when the system sends the minute tick. Some Android 9 TVs stop
sending it after the power button's standby, so the clock freezes until a reboot. This came from a report about another
launcher; it is **not reproduced** here.

**Where.** The home clock and date are `TextClock`s (`MainActivity.java` `keepLanguage()`, `:771-783`). `minuteTick`
(`:247-256`) does not touch them, and `keepLanguage()` re-formats them only when the locale changed.

**Fix.** A pure class `ClockRefresh` with two rules: `needed(sdk)` is true below Android 12 (API 31), where the
`TextClock` depends on the system tick, and `delayToNextMinute(now)` gives the milliseconds to the next minute plus a short
margin. On a device that needs it, Home re-sets the clocks' formats (which makes a `TextClock` read the time again) at
every minute boundary while it is resumed, on `onResume`, and when the screen turns on (`ACTION_SCREEN_ON`, registered
only on such a device and only while resumed). On Android 12 and later none of this is registered or scheduled, so it is
harmless there.

**Failure-case test.** `ClockRefreshTest`: Android 12 and newer need no refresh; Android 8 to 11 do; the delay is always
between 1 ms and 60 s plus the margin, including exactly on a boundary and just before one; a negative time cannot give a
negative delay.

**Cannot be tested without a TV.** **NOT TESTED on Android 9.** We own no Android 9 TV, so neither the freeze nor the fix
was seen. The change is written to do no harm if the guess is wrong: it re-reads the time, nothing else.

## 4. F4: the Picture row of the quick settings panel on some Xiaomi TVs

**Problem.** The Picture row sends `Settings.ACTION_DISPLAY_SETTINGS`. On a Xiaomi Android TV that action is reported to
open an empty stub, so the row looks dead. The action resolves, so a plain "does it resolve" check cannot detect this.

**Where.** `QuickSettingsActivity.java:29`.

**Fix.** A pure class `SettingsRoute` holds the rules for the candidates, most specific first. For the Picture row the
candidates are the TV settings app's own picture screen (component
`com.android.tv.settings/com.xiaomi.mitv.settings.picture.PictureActivity`) and then `ACTION_DISPLAY_SETTINGS`. The own
screen is a candidate only if `PackageManager.resolveActivity` finds it **and its `ResolveInfo.activityInfo.exported` is
true** (`SettingsRoute.usable`): a screen that resolves but that we may not start would otherwise end in the "Not
available" toast, which is worse than today's stub. When the row is pressed the candidates are started in order
(`SettingsRoute.startInOrder`); if the own screen still throws `SecurityException` or `ActivityNotFoundException`, the row
falls back once to `ACTION_DISPLAY_SETTINGS` before any toast. On a TV without the component the row behaves exactly as
before. No `Build.MANUFACTURER` or model check. The component's package is added to `<queries>` in the manifest so the
check works on Android 11 and later (package visibility); this is not a permission. If nothing starts, the existing "Not
available on this TV" toast stays; the row is not hidden, because a visibility filter could hide a row that works.

**Failure-case test.** `SettingsRouteTest`: a screen that resolves but is not exported is
not usable (and neither is one that does not resolve); a first screen that refuses to start falls back to the standard action exactly once, in order; a screen that
starts is not followed by the next; if every screen refuses, nothing starts and the caller shows the toast.

**Cannot be tested without a TV.** **NOT TESTED on a Xiaomi TV** (no reachable one). The component name comes from another
project's report and is unverified here; if it is wrong, `resolveActivity` returns null and nothing changes.

## 5. F5: a hidden banner icon, and the Default home picker on Android 14

**Problem (a).** A tile that shows a banner hides its icon view but keeps the icon's drawable, so a recycled tile holds a
second bitmap it never draws.

**Where.** `AppAdapter.java:281-285` (the banner branch of `bind`); the icon is cleared only for empty slots (`:264`) and
the no-banner branch (`:289`).

**Fix.** The banner branch also calls `h.icon.setImageDrawable(null)`. The rule "a tile shows exactly one of banner or
icon, and the other holds no drawable" is a pure class, `TileFace`, so it can be tested.

**Problem (b).** The Default home entry opens `HomeSettingsActivity`, declared `Theme.NoDisplay` (`AndroidManifest.xml:84-88`),
which finishes at once. On Android 14 the system's Default home picker is translucent and the home screen's tiles show
through its text.

**Fix.** `HomeSettingsActivity` gets a theme with a solid black window background and stays on screen (behind the picker)
until the picker closes, then finishes on its next resume. Plainly: the solid black backdrop now shows behind the picker
on **every** Android version, not only on 14 (the theme is not version-specific; on a picker that is already opaque it is
simply never seen). It can be tested on an Android 14 Google TV such as the Chromecast with Google TV 4K. A pure class `HomePickerGate` holds that rule: finish at once if
the picker could not be started; otherwise finish on the second resume. `noHistory` is not used: the translucent picker only
pauses the activity, so it would not be finished at the right time.

**Failure-case test.** `TileFaceTest` (banner clears the icon, icon clears the banner, never both) and
`HomePickerGateTest` (a failed start finishes at once; the first resume keeps it; the second resume finishes; a resume
before the start was tried does not finish).

**Cannot be tested without a TV.** How the picker looks on Android 14 (the black backdrop and the text) and that Back from
the picker returns to Home. **NOT TESTED on Android 14 here.**

## 6. The two-layer rule

Everything above is in the public, generic build. A TV that needs different behaviour gets it as a setting in the clean
build, turned on by that TV's own `config.json`; no device has its own copy of the code.

## 7. What was and was not checked

| Item | JVM tests | On a TV |
|---|---|---|
| F1 | source guard fails on `main`, passes after | NOT TESTED (`dumpsys meminfo` loop above) |
| F2 | `SafeStartTest` | NOT TESTED |
| F3 | `ClockRefreshTest` | NOT TESTED on Android 9 |
| F4 | `SettingsRouteTest` | NOT TESTED on Xiaomi |
| F5 | `TileFaceTest`, `HomePickerGateTest` | NOT TESTED on Android 14 |

Size and budget: no new dependency; the APK size before and after is in the pull request.

Credit: the problems F1, F2, F3, F4 and F5 were first reported in the issue tracker and release notes of another Android
TV launcher (CleanYourTV). Their licence is unclear, so only the ideas are used; see `NOTICE.md`.
