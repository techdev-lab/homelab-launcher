# Install guide

This is the exact order used on a Chromecast with Google TV 4K (Android 14). Read it once before you start.
Every step that changes the TV can be undone, and the [rescue](#rescue) at the end works even if everything goes wrong.

**Time:** about 30 minutes. **You need:** a computer on the same Wi-Fi as the TV, the TV remote, and your Wi-Fi password written down.

---

## 0. Before anything

1. **Write down your Wi-Fi password.** One step below can make the TV forget a saved network.
2. **Update the TV:** Settings → System → About → System update. Install everything it offers first.
3. **Developer options:** Settings → System → About → click **Android TV OS build** 7 times.
4. **Wireless debugging:** Settings → System → Developer options → **Wireless debugging** → On.
5. On the computer, install the official [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) and check:
   ```bash
   adb version
   ```

## 1. Pair and connect (once)

On the TV: Wireless debugging → **Pair device with pairing code**. Keep that window open and run:

```bash
adb pair <TV-IP>:<PAIR-PORT> <CODE>
adb connect <TV-IP>:<PORT>
adb devices
```

`<PAIR-PORT>` is the port inside the pairing window, `<PORT>` is the one on the main Wireless debugging screen. They are different. The code works once; if pairing fails, open a new pairing window and use the new code.

The port changes after every reboot. `adb mdns services` shows the current one (the `_adb-tls-connect` line). Pairing is kept, so later you only need `adb connect`.

## 2. Look before you touch (read-only)

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\scripts\Get-DeviceInventory.ps1
```

It prints the model, Android version, the current Home and your installed apps (use them for `dock_apps`). It changes nothing.

## 3. Install the launcher as a normal app

```bash
adb install tvhome-online.apk
adb shell am start -n com.gathin.tvhome/.MainActivity
```

The stock home is still the Home screen at this point. Use the remote on tvhome: arrows, open an app, Back. Continue only if it all works.

## 4. Your settings

Copy `config/config.example.json` (or one of `config/examples/`), set your city coordinates, leagues and dock apps ([CONFIG.md](CONFIG.md)), then:

```bash
adb push config.json /sdcard/Android/data/com.gathin.tvhome/files/config.json
adb shell mkdir -p /sdcard/Android/data/com.gathin.tvhome/files/wallpapers
adb push wallpapers/homelab/. /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/
adb shell am force-stop com.gathin.tvhome
```

Press HOME, then open tvhome again to see the result.

The launcher follows the TV's language. To show it in Arabic on a TV whose system language is English, without changing the TV language for every app, give the launcher its own language (Android 13 and later; `adb shell cmd locale help` also explains how to clear it):

```bash
adb shell cmd locale set-app-locales com.gathin.tvhome --locales ar
```

Optional, to show the Wi-Fi network name in the Wi-Fi pill: Android 10 and later only hand the name to an app that holds the location permission, and nothing in this guide grants it for you. Without it the pill still works and just does not show the name. The app never asks for your location; this permission is declared only for the name (see [SECURITY.md](../SECURITY.md)).

```bash
adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION
```

## 5. Make it the Home screen

Google TV keeps two packages that own Home: the stock launcher `launcherx` and the setup wizard `setupwraith`. **Both** must be disabled, **setupwraith first**. If only `launcherx` is disabled, the setup wizard becomes the Home screen and the TV can loop on boot.

Disabling `setupwraith` can also make the TV forget a Wi-Fi network that the setup wizard saved. The script checks the network you are on and stops if it cannot tell who owns it.

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom                    # dry run: read-only checks and the plan
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM HOME livingroom" -EnableAppKeys
```

It writes a rollback file first, disables `setupwraith` then `launcherx`, sets tvhome as Home and verifies HOME, Wi-Fi and ADB. If a check fails it rolls back immediately.

If the TV drops off Wi-Fi during this step, re-enter the Wi-Fi password with the remote, then reconnect adb.

Undo at any time:
```powershell
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
```

## 6. YouTube button and screensaver

The YouTube button on the remote is a global key that only the stock launcher listened to. tvhome hears it through a small accessibility service that only filters keys (`capabilities=8`, it cannot read the screen).

```bash
adb shell am force-stop com.gathin.tvhome
adb shell settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
adb shell settings put secure accessibility_enabled 1
adb shell settings put secure screensaver_components com.gathin.tvhome/.ClockDream
```

Order matters: set the accessibility service **after** any `force-stop` (a force-stop afterwards clears it), and again after every reinstall of tvhome. Test the YouTube button with the real remote; keys sent with `adb shell input` do not pass through the service.

The screensaver follows the TV's system language. Your old screensaver value can be read first with `adb shell settings get secure screensaver_components` if you want to go back to it.

## 7. Finish

1. Reboot (`adb reboot`) and check that tvhome comes back as Home and Wi-Fi reconnects.
2. **Turn Wireless debugging off** on the TV. It stays on after a reboot until you turn it off.

---

## Updating later (one shot)

For a TV that already runs tvhome as Home, `scripts/Apply-TvhomeKit.ps1` installs a new APK, pushes the config and wallpapers, re-enables the YouTube service and the screensaver, and backs everything up first. Put the files next to it like this:

```
kit/
  Apply-TvhomeKit.ps1
  apk/tvhome-online.apk
  apk/SHA256SUMS.txt          "<sha256>  tvhome-online.apk"
  config/livingroom.config.json
  wallpapers/*.jpg
```

Run it from the kit folder, with the TV address set (the connect port from Wireless debugging):

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\Apply-TvhomeKit.ps1 -Alias livingroom                                          # dry run
.\Apply-TvhomeKit.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM KIT livingroom"
.\Apply-TvhomeKit.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
```

The backup goes to `%USERPROFILE%\.homelab-launcher\state\`; the rollback puts back the previous APK, config, screensaver and accessibility setting. The script also sets the screensaver to tvhome's own clock, so note your old value first if you want it back.

It refuses to run unless `launcherx` and `setupwraith` are both disabled for user 0, because reinstalling the only Home app while the setup wizard can take over is exactly how a boot loop starts.

## Optional: a quieter TV

These 12 packages were disabled on the test Chromecast with no side effect on the remote, sound, Cast, keyboard or screensaver. `disable-user` is reversible with `adb shell pm enable <package>`. Do them in two batches and test the remote, sound and Cast between batches.

```
com.google.android.feedback                    com.android.tv.feedbackconsent
com.google.android.tv.remotecontrol.logging    com.google.android.apps.tv.netoscope
com.google.android.federatedcompute            com.google.android.ondevicepersonalization.services
com.google.android.adservices.api              com.google.android.ext.adservices.api
com.google.android.play.games                  com.google.android.videos
com.android.printspooler                       com.google.android.backdrop
```

```bash
adb shell pm disable-user --user 0 <package>
```

**Never disable:** Google Play services (`com.google.android.gms`, `com.google.android.gsf`), Play Store (`com.android.vending`), Settings (`com.android.tv.settings`), the remote service (`com.google.android.tv.remote.service`), the keyboard (`com.google.android.inputmethod.latin`), Cast (`com.google.android.apps.mediashell`), the screensaver host `com.google.android.apps.tv.dreamx` while it is your screensaver, or the remote firmware updater (`com.google.android.tv.dfuservice`). Never `pm uninstall` a system package.

---

## Rescue

**If the TV loops on boot** (Google logo, Google TV colours, black, again):

1. Leave it plugged into the TV and power.
2. Press and hold the button on the back of the Chromecast. The light blinks **yellow**.
3. Keep holding until it turns **solid white**, then release. The Chromecast resets to factory settings.
4. Set it up again, then **System update**.

A factory reset wipes only user data: everything this project changes lives there, the system itself is not touched, so the TV comes back on the same Android build it had. (Source: Google support, "How to factory reset your Chromecast".)

What does **not** work: adb over a USB cable (USB debugging is off by default and was never needed here), `cmd dreams` (needs root), unlocking the bootloader (wipes the TV anyway and is not needed). Google does not publish downloadable factory images for this device.

**If something looks wrong but the TV still works:** run the rollback of the script you used, or by hand:

```bash
adb shell pm enable com.google.android.tungsten.setupwraith
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
```
