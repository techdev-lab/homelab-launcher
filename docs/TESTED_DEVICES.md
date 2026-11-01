# Tested devices

[English](TESTED_DEVICES.md) · [العربية](TESTED_DEVICES.ar.md)

What 3.0.0 was run on, and what was and was not checked (2026-10-31). Nothing here is a promise for another model: the app is an ordinary Android TV launcher, and the steps that make it the Home screen differ per TV (see [INSTALL.md](INSTALL.md)).

## Chromecast with Google TV 4K (Android 14, Google TV)

The maintainer's living-room TV, used every day since 2.x.

- **Upgrade:** 3.0.0 installed over the previous build with `adb install -r` keeps the TV's own choices (columns, wallpaper set, text size).
- **Checked with the remote, English and Arabic:** the side dock (right on English, left on Arabic), Latin and Arabic-Indic digits everywhere, the four sections of the Launcher settings, the prayer card and the prayer alert at the real prayer time, hidden apps, the screensaver clock.
- **Ramadan card:** it only shows in Ramadan, so it was seen on a throwaway preview build with a fixed Ramadan date and the real clock, never on a release build.
- **Reboot:** Home comes back as tvhome, the key service is bound, the choices are kept and the cards fill within minutes.
- **Remote:** the YouTube button works with the key service on (the default).

## TCL Smart TV Pro, model G08 (Android 14, Google TV, 32-bit)

A family TV set up with the launcher as Home (2026-11-01), with a Bluetooth remote (`BT_RC813A_B1`).

- **Remote app buttons:** YouTube (key code 190), Netflix (4062) and Prime Video (4099) arrive as vendor key codes and work through `app_keys` with the key service on. TCL maps them itself with a fixed system overlay, which cannot be changed without root.
- **Known on this TV:** with the key service on, holding Volume changed the volume by one step (3 s and 8 s holds, measured with `logcat`). The service never receives the Volume key (a test build logged every key it got), so it cannot repeat Volume itself. *Launcher settings > Motion > Remote buttons > Home screen only* made a held Volume key repeat inside other apps while the buttons kept working on the home screen. See [KEY_FILTER.md](KEY_FILTER.md).

## Xiaomi Mi TV MOOR2 (Android 11, Google TV with `launcherx` and `setupwraith`)

A second TV, set up from zero with this guide after an old build was removed.

- **Install:** inventory script, tvhome as a normal app, a config based on `config/examples/general.json`, the Home switch script with its typed confirmation, the key service and the screensaver (steps 2 to 6).
- **Two script bugs found here and fixed in 3.0.0:** the HOME-key check looked for a `dumpsys` line that Android 11 does not print, and a rescue timer left by a failed run could undo the next, successful run. Each has a test that failed before the fix.
- **Reboot:** Home came back as tvhome in about two minutes, Wi-Fi reconnected and the cards filled.
- **Launcher settings** work on Android 11.
- **Known on this TV:** with the key service on, holding the remote's Volume key changed the volume one step per press. *Launcher settings > Motion > Remote buttons > Off* fixes it (confirmed with the physical remote); On keeps the YouTube button. See [KEY_FILTER.md](KEY_FILTER.md).

## Not tested

The Chromecast with Google TV HD, the Google TV Streamer, other Xiaomi models, Fire TV, and Samsung (Tizen) and LG (webOS) TVs, which are not Android and are not supported. If you run it on another device, a bug report with the model and Android version helps.
