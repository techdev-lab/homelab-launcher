# Remote buttons switch (`key_filter`): design note

[English](KEY_FILTER.md) · [العربية](KEY_FILTER.ar.md)

Status: **implemented on the `fix/key-filter-switch` branch, waiting for the TV check** (written before the code; the TV result is in section 5 once done). The default
is today's behaviour, so an upgrade changes nothing until the viewer picks.

## 1. The problem

`HomeKeyService` is an accessibility service that exists only to filter keys (`canRequestFilterKeyEvents`, flag
`FLAG_REQUEST_FILTER_KEY_EVENTS`; it cannot read the screen). It needs the filter for three things: the remote's YouTube
button (keycode 190, or whatever `app_keys` maps), a Settings button, and the Home button on remotes that send
`KEYCODE_ALL_APPS`.

Android's key filter is all or nothing: once one service asks for it, **every** key the remote sends goes through
`AccessibilityManagerService` first, and a service can only answer "handled" or "not handled". On some TVs a key that went
through the filter loses its auto repeat. Seen on a real TV with the physical remote: on a Xiaomi Mi TV (MOOR2,
Android 11) holding Volume up or down changes the volume **one step per press** while the service is enabled; with the
service removed from `enabled_accessibility_services` the held key repeats normally. The Chromecast with Google TV 4K does
not show it.

Turning the service off in Android's settings fixes the volume but also removes the three buttons above. The cure should
be smaller than that.

## 2. The switch

One runtime choice, `key_filter`:

| Value | Meaning |
|---|---|
| `on` (default) | As before: the service filters keys, so the YouTube button, `app_keys`, the Settings button and the `KEYCODE_ALL_APPS` Home work. |
| `off` | The service stays enabled and bound but asks Android not to filter keys. Every key reaches the system untouched, so a held key repeats as on a TV without the service. The three buttons above stop working (a remote whose Home sends `KEYCODE_ALL_APPS` loses its Home button too). |

The rule for the value is the one of every look option: the TV's own choice (Launcher settings) wins over `config.json`,
then the default `on`; an unknown value counts as unset. It is the pure class `KeyFilter` (`pick`, `valid`, and `flags`,
which turns the choice into `AccessibilityServiceInfo.flags`), unit tested on the JVM like `DigitStyle`.

### How the service applies it

- `onServiceConnected` reads `Config` and calls `setServiceInfo` with the flags `KeyFilter.flags(choice, current)`:
  the request-filter bit is set for `on` and cleared for `off`; every other flag is kept.
- The service listens to the launcher's own preferences (`SharedPreferences.OnSharedPreferenceChangeListener`, held in a
  field because Android keeps listeners weakly) and re-applies when `key_filter` changes, so the chip works at once, with
  no restart and without turning the service off and on. A theme push restarts the service as before and it reads the
  value again.
- The `res/xml/home_key_service.xml` flag stays: a fresh install filters until the service has read the choice, which is
  the 2.9 behaviour.
- No new permission, no new file, no new component. The accessibility service was already declared with
  `canRequestFilterKeyEvents`.

### Where it is in Launcher settings

One row, **Remote buttons: On / Off**, in the Motion section (it is the section with one row; the Dock and cards section
already scrolls). The section's own hint grows by a few words. The row is one more row for `SettingsNav` (two chips), so
its arrow-key rules and tests do not change. The explanation line says what turns off (the YouTube button, app buttons and
the Settings button) and that Off fixes held volume keys on some TVs.

### What this does not do

It does not make filtering and repeat work together; it lets the viewer choose which one they need. Whether something
smarter is possible is section 3.

## 3. Alternatives looked at

From the AOSP source (`KeyboardInterceptor`, `KeyEventDispatcher`, Android 11):

- With the flag set, every key is taken out of the normal input path, offered to the service (it has 500 ms to answer),
  and, if no service handles it, **re-injected** into the input pipeline once every filtering service has answered. There is
  no "return false faster" that changes this: the event is held until the answer or the timeout either way, and the
  re-injected key is a new event, not the original one. This is the likely reason a held key loses its repeat on a TV whose
  remote relies on the dispatcher's own repeat, but we could not confirm the exact mechanism from the source alone.
- `FLAG_REQUEST_FILTER_KEY_EVENTS` is the only flag involved; no other `AccessibilityServiceInfo` flag lets a service see
  some keys and not others. The filter is per service and cannot be limited to keycodes.
- Possible, **not built**: the service could repeat Volume itself while the key is held (a handler calling
  `AudioManager.adjustStreamVolume` until the up event). That would keep filtering and give repeat for Volume only, but it
  replaces the system's volume handling (volume panel, a TV that sends volume to an amplifier over CEC), so it may behave
  differently on each TV. It needs a decision before anyone builds it.
- Possible, **not built**: switching the filter on only while the home screen or a chosen app is in front. Android has no
  event for "the YouTube button is about to be pressed", so the button would stop working elsewhere; the viewer's switch is
  simpler and honest.

## 4. Tests

JVM tests (`KeyFilterTest`), written first and shown failing: the TV's choice wins over the theme; the theme is used when
the TV has no choice; the default is `on`; unknown, empty and null values count as unset; case matters; a config of `off`
with a TV choice of `on` gives `on`; `flags` sets and clears only the request-filter bit and keeps every other bit.

## 5. TV check

Not done yet. The test TV (a Xiaomi Mi TV, Android 11) was off the network when the build was ready. To do, with the physical
remote: with *On*, hold Volume up (expect one step), press the YouTube button (expect the app); with *Off*, hold Volume up
(expect repeat) and confirm `dumpsys accessibility` lists the service without the filter-key-events flag.
