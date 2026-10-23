# How it was built (and what went wrong)

Three days, two Chromecasts with Google TV 4K, one boot loop. Written down so the next person skips the painful parts.

## Day 1: clean up, then a launcher

- Started from the community **cleanyourtv** project: package lists per TV model and a small launcher called `tvhome`. Read the code before running anything, pinned the exact commit, and built outside the synced folder because its tool creates a private ADB key.
- Paired over **Wireless debugging** (`adb pair`, then `adb connect`). Port 5555 is closed on Android 14; the pairing port and the connect port are different and change on every reboot.
- Disabled 13 tracking and bloat packages from the list for the same model. **Mistake:** one of them, `dreamx`, was the active screensaver host, so the screensaver silently broke. Re-enabled it. Lesson: check `screensaver_components` before disabling anything that draws.
- Built `tvhome` with a "Move Mode" for reordering apps. A code review found four real bugs (a saved order that was never reloaded, a refresh that dropped key presses). Fixed, rebuilt, installed. On the physical remote it still did not work, so it was **dropped**, not patched forever.

## Day 2: make it ours

- **Arabic theme**: a right-to-left layout, a tinted-glass look, the Hijri date (Umm al-Qura, from the platform ICU) and **prayer times computed on the TV**. The prayer maths was checked against an independent source to within 2 minutes.
- **Two builds**: `offline` with no INTERNET permission at all, and `online` for football and news. Verified with `aapt2` that the offline APK really has no INTERNET.
- **Trap: versionCode.** A build with a higher versionCode cannot be rolled back on a non-debuggable APK. Kept versionCode 4 for every build since.
- **Trap: the YouTube button.** It is a global key that only the stock launcher receives. The fix is a tiny accessibility service that filters keys only. Every reinstall turns it off, and `adb shell input keyevent` does not go through it, so it must be tested with the real remote.
- Rotating wallpapers drawn in the browser (no photos), a weather card, then the first Chromecast ran it for good: reboot test passed, Wireless debugging closed.

## Day 3: the second Chromecast, the hard way

- Installed tvhome and set it as Home **by hand**. Disabled the stock launcher `launcherx`, but looked for the setup wizard under a wrong package name and believed it was absent. It was `com.google.android.tungsten.setupwraith`, and it was **enabled**.
- Reinstalled tvhome while it was the Home app. For a moment the setup wizard was the only Home candidate. **The TV went into a boot loop**: Google logo, colours, black, again. No ADB.
- The way out was the official one: **hold the button on the back until the light goes from yellow to white** (factory reset). Everything we change lives in user data, so the TV came back clean on the same Android build. USB debugging, fastboot and factory images were all checked and are not options on this device.
- Rebuilt it properly: cleanup in two batches with tests in between, tvhome installed as a normal app first, then the Home switch **with the script, setupwraith first**.
- **Surprise: the Wi-Fi password disappeared.** Disabling the setup wizard deleted a network it had saved. The script's ownership check had read a blank owner and treated it as safe. Fixed to fail closed, with a test. The script's on-TV rollback timer also did not survive the dropped link; documented as a known issue.
- Reboot test passed. Then the polish:
  - the football card went empty after the day's matches ended, because the scoreboard only returns today. ESPN rejects a date range, so the app now asks **day by day, up to 7 days**;
  - **two football cards** (the Saudi league and the big European leagues), news removed;
  - the focused app name sat under the prayer card: smaller, with its own space;
  - cards of one height, a 4-line match card (date, league, teams, score);
  - the **Readex Pro** font for Arabic and English together;
  - an **all-Arabic screen** on an Arabic TV: app names, league names and 114 team names, since ESPN has no Arabic;
  - a **gliding clock screensaver** with the Hijri date and the next prayer, instead of Google's;
  - a **one-shot kit script** to apply all of it to another TV, with a backup and a rollback.

## Lessons

1. Read before you run, pin what you run, and keep private keys out of synced folders.
2. On Google TV, **`setupwraith` before `launcherx`**. Always. Check package names exactly.
3. Never reinstall the only Home app while another Home fallback can take over.
4. Write down the Wi-Fi password before touching the setup wizard.
5. A check that cannot read its input must fail, not pass.
6. Test remote features with the real remote.
7. One change at a time, a test after each, and a rollback written before the change.
8. The factory reset button is your friend: know it before you need it.
