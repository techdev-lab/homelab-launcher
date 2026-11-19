# Tested devices

[English](TESTED_DEVICES.md) · [العربية](TESTED_DEVICES.ar.md)

What 3.0.0 and 3.1.1 were run on, and what was and was not checked (3.0.0 on 2026-10-31; the Chromecast section also covers 3.0.2 on 2026-11-15 and 3.1.1 on 2026-11-18; the TCL section also covers 3.1.1 on 2026-11-19). Nothing here is a promise for another model: the app is an ordinary Android TV launcher, and the steps that make it the Home screen differ per TV (see [INSTALL.md](INSTALL.md)).

## Chromecast with Google TV 4K (Android 14, Google TV)

The owner's living-room TV, used every day since 2.x.

- **Upgrade:** 3.0.0 installed over the previous build with `adb install -r` keeps the TV's own choices (columns, wallpaper set, text size).
- **Checked with the remote, English and Arabic:** the side dock (right on English, left on Arabic), Latin and Arabic-Indic digits everywhere, the four sections of the Launcher settings, the prayer card and the prayer alert at the real prayer time, hidden apps, the screensaver clock.
- **Ramadan card:** it only shows in Ramadan, so it was seen on a throwaway preview build with a fixed Ramadan date and the real clock, never on a release build.
- **Reboot:** Home comes back as tvhome, the key service is bound, the choices are kept and the cards fill within minutes.
- **Remote:** the YouTube button works with the key service on (the default).
- **3.0.2 (batch F, [BATCH_F_FIXES.md](BATCH_F_FIXES.md)):** 20 round trips through Launcher settings left the object counters at their starting values (no leak seen on this build; the earlier build was not measured the same way). The remote's YouTube, Settings and Home buttons work (reported by the owner). The Default home picker shows a solid dark backdrop (seen by the owner on the TV).
- **3.1.1 (2026-11-18):** installed with `adb install -r`; the APK pulled back from the TV has the same SHA-256 as the candidate build. The owner checked the remote by hand: YouTube, the Netflix button (it opens Shahid in the owner's setup) and Home all work, and the Launcher settings applied from Home look right. Cold start, read from the system log's "Displayed" line (four samples: one after the install and three after force-stop), was 705 to 782 ms. The key service had to be bound again by hand after the install and after force-stop.

## Second Chromecast with Google TV (32-bit, Android 14)

A second Chromecast with Google TV, a 32-bit build of Android 14, with a Bluetooth remote.

- **3.1.1 (2026-11-18):** upgraded from 2.5.5 (versionCode 4) with `adb install -r`. The install reported `Success`, and the installed version was read back as 3.1.1 (versionCode 9).
- **Profile:** a device profile (`config.json`) was pushed after the upgrade. The push reset the TV's own choices (columns, motion, effects, wallpaper mode, colour and text size), which the owner set again by hand on the TV. The maintainer did not verify those.
- **Remote:** the owner reports that YouTube, Netflix (mapped to Shahid, see the TCL section) and Home work with the physical remote. The key service was bound by hand after the install.
- **Not checked:** the Netflix button's key code was not read on this remote (with `getevent`), so that mapping rests on the owner's report only.

## TCL Smart TV Pro, model G08 (Android 14, Google TV, 32-bit)

A family TV set up with the launcher as Home (2026-11-01), with a Bluetooth remote (`BT_RC813A_B1`).

- **Remote app buttons:** YouTube (key code 190), Netflix (4062) and Prime Video (4099) arrive as vendor key codes and work through `app_keys` with the key service on. TCL maps them itself with a fixed system overlay, which cannot be changed without root.
- **Netflix button set to Shahid (owner-reported, 2026-11-19):** the device profile maps key code 4062 to `net.mbc.shahidTV` (Shahid) through `app_keys`, and the owner reports that the button opens Shahid. Map a button only to an app that is installed on the TV.
- **3.1.1 (2026-11-19):** installed over the 3.0.0 candidate build (versionCode 5) with `adb install -r`, now 3.1.1 (versionCode 9). An install cannot go back to versionCode 5; the only way is an uninstall, and an uninstall clears the TV's choices. The key service was bound again by hand after the install. The owner reports that the remote buttons and volume work. The maintainer did not measure this on 3.1.1.
- **Known on this TV (measured on the 3.0.0 candidate build):** with the key service on, holding Volume changed the volume by one step (3 s and 8 s holds, measured with `logcat`). The service never receives the Volume key (a test build logged every key it got), so it cannot repeat Volume itself. *Launcher settings > Motion > Remote buttons > Home screen only* made a held Volume key repeat inside other apps while the buttons kept working on the home screen. See [KEY_FILTER.md](KEY_FILTER.md).
- **A config push resets the TV's choices:** a config push resets columns, motion, effects, wallpaper mode and background colour. Set them again on the TV after each push.

## Xiaomi Mi TV MOOR2 (Android 11, Google TV with `launcherx` and `setupwraith`)

A second TV, set up from zero with this guide after an old build was removed.

- **Install:** inventory script, tvhome as a normal app, a config based on `config/examples/general.json`, the Home switch script with its typed confirmation, the key service and the screensaver (steps 2 to 6).
- **Two script bugs found here and fixed in 3.0.0:** the HOME-key check looked for a `dumpsys` line that Android 11 does not print, and a rescue timer left by a failed run could undo the next, successful run. Each has a test that failed before the fix.
- **Reboot:** Home came back as tvhome in about two minutes, Wi-Fi reconnected and the cards filled.
- **Launcher settings** work on Android 11.
- **Known on this TV:** with the key service on, holding the remote's Volume key changed the volume one step per press. *Launcher settings > Motion > Remote buttons > Off* fixes it (confirmed with the physical remote); On keeps the YouTube button. See [KEY_FILTER.md](KEY_FILTER.md).

## Not tested

On 3.0.2: the clock fix on Android 9 and older (no such TV), the quick settings Picture row on Xiaomi TVs, and the Xiaomi Mi TV and the TCL on this build.

On 3.1.1: the Xiaomi Mi TV MOOR2 (not run on 3.1.1); the clock fix on Android 9 and older; the remote's "Home screen only" setting (`key_filter=home`), which is not verified in the 3.1.1 code; and a low-priority Scenes detail: after start, the grid sits about 44 px higher until the strip's data arrives, then drops once. It was found after the candidate build and is not fixed in 3.1.1.

The Chromecast with Google TV HD, the Google TV Streamer, other Xiaomi models, Fire TV, and Samsung (Tizen) and LG (webOS) TVs, which are not Android and are not supported. If you run it on another device, a bug report with the model and Android version helps.
