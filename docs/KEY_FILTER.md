# Remote buttons switch (`key_filter`): design note

[English](KEY_FILTER.md) · [العربية](KEY_FILTER.ar.md)

Status: the `on` / `off` switch shipped in 3.0.0. The third value, `home` (section 6), is **implemented on the
`fix/held-volume-with-key-filter` branch; the TV check is in section 5**. The default is today's behaviour, so an upgrade
changes nothing until the viewer picks.

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

A second TV showed the same: a TCL Smart TV Pro (G08, Android 14, Google TV, Bluetooth remote `BT_RC813A_B1`). Measured with
the physical remote and `adb logcat` / `getevent`: the remote sends one DOWN and one UP per hold, with no repeats of its own
(`HandlesKeyRepeat` is false), so Android's input dispatcher makes the repeats. With the filter on, a hold of 3 s and one of 8 s
each changed the volume by exactly one step; with it off the volume ramped (3 index units every ~150 ms). The TCL remote's
app buttons arrive as vendor key codes (YouTube 190, Netflix 4062, Prime Video 4099) and only the filter sees them.

Turning the service off in Android's settings fixes the volume but also removes the three buttons above. The cure should
be smaller than that.

## 2. The switch

One runtime choice, `key_filter`:

| Value | Meaning |
|---|---|
| `on` (default) | As before: the service filters keys, so the YouTube button, `app_keys`, the Settings button and the `KEYCODE_ALL_APPS` Home work. |
| `home` | The service filters keys **only while one of this launcher's screens is in front** (home, settings, clubs, hidden apps) and stops filtering while any other app is in front. On the home screen the buttons work as with `on`; inside another app a held Volume key repeats as with `off`, and the buttons stop working there. Section 6. |
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

### The other key service

`InputKeyService` (the optional Input button, enabled only with `INPUT_KEY_SERVICE=1` or `-InputKeyService`) declares the
key filter in its own `res/xml/input_key_service.xml` and this switch does not change it. If you also enable it, Android
keeps filtering keys whatever the choice is (`home` or `off`), so a held Volume key can still step once. Leave it disabled
on a TV that shows the problem.

### Where it is in Launcher settings

One row, **Remote buttons: On / Home screen only / Off**, in the Motion section (it is the section with one row; the Dock and cards section
already scrolls). The section's own hint grows by a few words. The row is one more row for `SettingsNav` (two chips, three
with `home`), so its arrow-key rules and tests do not change. The explanation line says what turns off (the YouTube button, app buttons and
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
- **The service repeating Volume itself: impossible on the TVs measured.** It was the idea of this note's first version
  (a handler calling `AudioManager.adjustStreamVolume` until the up event). A temporary build that logged every
  `KeyEvent` in `onKeyEvent` showed on the TCL that the service receives the YouTube button (190) and Home, press and
  release, but **no Volume key at all**: the log shows `WindowManager` changing the volume when the key is
  queued, and the likely reason (not confirmed from the TV's source) is that the key is then not passed on to the user, so
  the accessibility filter never offers it to a service. A service that cannot see the press cannot know when to start or when to stop. This also rules
  out the CEC and volume-panel questions the idea raised, because it cannot be built at all.
- **TCL's own mechanism: not usable.** The remote's app buttons are mapped by `com.tcl.globalkeyoverlay`, a static,
  non-mutable overlay in `/product` that sends `KEYCODE_BUTTON_3` (190), `KEYCODE_GUIDE`, `KEYCODE_PROFILE_SWITCH`,
  `KEYCODE_FEATURED_APP_1` and `KEYCODE_MACRO_1` to the stock launcher's global key receiver. It cannot be changed without
  root. The settings `tcl_sys_input_key_intercept` and `InterceptApp` are empty, and no string with those names or with
  `tcl_app_settings_key_customization` exists in the dex or resources of Settings, the TCL system server, the config
  provider, the quick panel, the dashboard, the TCL home or the stock launcher, so nothing readable was found that maps a
  button to an app.
- **Switching the filter on and off by volume activity: would not help the first hold.** From the AOSP source (not
  measured here): the dispatcher starts repeating a key when it enqueues the DOWN, and a filtered DOWN is never enqueued, so
  turning the filter off after the first step cannot make that hold repeat, only the next press.
- **Switching the filter by what is in front: built as `home`** (section 6).

## 4. Tests

JVM tests (`KeyFilterTest`), written first and shown failing: the TV's choice wins over the theme; the theme is used when
the TV has no choice; the default is `on`; unknown, empty and null values count as unset; case matters; a config of `off`
with a TV choice of `on` gives `on`; `flags` sets and clears only the request-filter bit and keeps every other bit.
For `home` (`KeyFilterTest`, `LauncherFrontTest`): `home` is valid and picked like the others; `home` filters exactly when a
launcher screen is in front; an unknown choice still filters whatever is in front; and the front counter, including the
failure cases: a fresh process has nothing in front; a screen that stops without a start never takes the count below zero,
so the next screen still counts as in front; and two screens that overlap while the user moves between them never leave the
front.

## 5. TV check

Measured on the TCL Smart TV Pro (G08, Android 14) with the physical remote, recording `logcat` and `getevent`:

- Filter on (3.0.0): hold Volume up 3.4 s and 8 s, one `volume_changed` each. The temporary probe build logged the YouTube
  button (190) and Home reaching the service and no Volume key at all.
- The `home` value, same TV and remote, one recording (`logcat` and `getevent`): inside SmartTube a Volume up hold of 1.6 s
  ramped the volume (27 to 54) and a Volume down hold ramped it back (54 to 23); `dumpsys accessibility` showed no key
  filter while SmartTube was in front. After Home, with the launcher in front, the filter was back and two Volume up
  holds of 1.8 s and 1.7 s gave one step each (23 to 24, 24 to 25), as expected. The YouTube button on the home screen
  started YouTube (`Displayed com.google.android.youtube.tv` 1.2 s after the key). Inside that app a hold ramped again
  (25 to 47). No crash in the log, and the service stayed bound. What was not checked: the buttons inside another app
  (they are meant not to work there), a TV with a remote whose Home sends `KEYCODE_ALL_APPS`, and the screensaver.

## 6. The `home` value

Idea: the app buttons are needed mostly on the home screen, and a held Volume key is needed mostly inside a video app. The
filter is a service flag that can be changed while the service runs (`setServiceInfo`, as `off` already does), so the
service asks for it only while a screen of this launcher is in front.

- **What is "in front":** a small `Application` subclass (`TvHomeApp`) registers `ActivityLifecycleCallbacks` when the process
  starts and counts the launcher's started activities (`LauncherFront`, a pure class). The count is above zero while any
  launcher screen is started; moving from the home screen to Settings starts the new screen before the old one stops, so the
  count never touches zero in between. When the user opens another app, the launcher's last screen stops and the count
  reaches zero, and `HomeKeyService` is told to ask for the filter again. The count lives in the application, not in the
  service, so no screen is missed whenever the service connects, and a restarted process starts from zero with nothing in
  front, which is true.
- **Nothing in front after a restart:** if the process is restarted while another app is in front, the filter is off until
  a launcher screen starts, which is the meaning of the choice. A fresh install filters until the service reads the choice
  (the flag in `home_key_service.xml` stays).
- **Rule:** `KeyFilter.filtering(choice, launcherInFront)`: `on` and any unknown value filter; `off` never; `home` filters
  exactly when the launcher is in front. The flag bit is set or cleared by the existing `KeyFilter.flags`.
- **What it costs:** inside another app the YouTube button, `app_keys`, the Settings button and a `KEYCODE_ALL_APPS` Home do
  not work (the stock `KEYCODE_HOME` still does). A remote whose Home sends `KEYCODE_ALL_APPS` (Xiaomi) should keep `on`.
  It is the viewer's choice and never the default.
- **Worst case:** a count stuck at zero would leave the filter off while the launcher is shown, so the buttons would be
  dead until the viewer picks *On*; the default and `on` are untouched, and the stop callback never takes the count below
  zero. No new permission and no new service; the one new component is the `Application` subclass.
