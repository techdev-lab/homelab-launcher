# Uninstall and rollback

[English](UNINSTALL.md) · [العربية](UNINSTALL.ar.md)

This page collects what [INSTALL.md](INSTALL.md) already says about going back, in the order to do it. It adds no new
command: every command here is one of the install guide's. A step that no device has proved is marked **NOT TESTED**.
If the TV loops on boot, stop and use the [rescue](INSTALL.md#rescue) first.

## What the install changes

| Change | Where INSTALL.md makes it | How to undo |
|---|---|---|
| tvhome is installed (`com.gathin.tvhome`) | step 3 | step 3 of "Undo", below |
| `setupwraith` and `launcherx` are disabled and tvhome is the Home app | step 5 | step 1 of "Undo" |
| The key service `HomeKeyService` is on and `accessibility_enabled` is 1 | step 6 | step 2 of "Undo" |
| The screensaver is set to tvhome's `ClockDream` | step 6 | step 2 of "Undo" |
| `ACCESS_FINE_LOCATION` is granted (optional, for the Wi-Fi name) | step 4 | removed with the app |
| `config.json` and wallpaper files are pushed to the app's folder | step 4 | removed with the app |
| Packages disabled with `pm disable-user` (optional list) | "A quieter TV" | `adb shell pm enable <package>` |

## Undo, in this order

1. **Give Home back to the stock launcher.** Run the rollback of the script you used. It restores what its own rollback
   file recorded (step 5 and the update section):

   ```powershell
   .\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
   .\Apply-TvhomeKit.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
   ```

   By hand, if the TV still works (from the rescue section):

   ```bash
   adb shell pm enable com.google.android.tungsten.setupwraith
   adb shell pm enable com.google.android.apps.tv.launcherx
   adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
   ```

2. **Turn off the key service and put the screensaver back.**

   ```bash
   adb shell settings delete secure enabled_accessibility_services
   adb shell settings put secure accessibility_enabled 0
   ```

   The screensaver value to restore is the one you read before changing it
   (`adb shell settings get secure screensaver_components`, step 6). INSTALL.md does not name a default to go back to.
   Deleting `enabled_accessibility_services` also removes any other accessibility service you had turned on: write
   that list down first.

3. **Remove the app.** Do this only after step 1, so the TV already has a working Home app:

   ```bash
   adb uninstall com.gathin.tvhome
   ```

   The uninstall removes the app's own choices made on the TV (language, wallpaper, columns) and its `config.json`. Copy
   `config.json` first if you want to keep it (`adb pull /sdcard/Android/data/com.gathin.tvhome/files/config.json`).

4. **Re-enable any package you disabled** from the optional list, one at a time, with `adb shell pm enable <package>`.

5. **Turn Wireless debugging off** on the TV (Settings > System > Developer options). It stays on after a reboot until
   you turn it off.

Going back to an older tvhome release instead of removing it is a different path: see "Going back to 3.0.1, 3.0.0,
2.9.0 or older" in INSTALL.md.

## What we leave on, and why

| Left as it was | Why |
|---|---|
| Google Play services, Play Store, TV Settings, the remote service, the keyboard, Cast, the screensaver host and the remote firmware updater | INSTALL.md lists them under "Never disable". Turning them off breaks sign-in, updates, the remote or Cast. |
| Every package not named in the install steps | The scripts disable only `setupwraith` and `launcherx`; the "quieter TV" packages are an optional list you do by hand and can reverse. |
| The key service while you use the remote's YouTube button | It only filters keys (`capabilities=8`) and cannot read the screen. *Launcher settings > Motion > Remote buttons* can set it to the Home screen only, or off. |
| Wireless debugging, after the install | The install guide asks you to turn it off yourself (step 7); nothing turns it off for you. |
| Your own wallpapers and `config.json` on the TV | They are your files. Uninstalling the app removes them. |

## NOT TESTED

- Every step of this page on a real TV: it is assembled from INSTALL.md, whose steps were run on a Chromecast with Google
  TV 4K and a Xiaomi Mi TV (see [Tested devices](TESTED_DEVICES.md)), not from a separate uninstall test.
- The order "rollback first, then `adb uninstall`" is a precaution drawn from INSTALL.md's warning that reinstalling the
  only Home app while the setup wizard can take over is how a boot loop starts. No TV was uninstalled in that order for
  this page.
- Which screensaver value a given TV had before: it differs by device and by Android version.
