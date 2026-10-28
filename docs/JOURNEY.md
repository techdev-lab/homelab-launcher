# How it was built (and what went wrong)

Four days, two Chromecasts with Google TV 4K, one boot loop. Written down so the next person skips the painful parts.

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

## Day 4: publishing, and testing the published copy

The project went to GitHub, and then the **published** copy was tested the way a stranger would use it: a fresh clone, a build, an install on a real Chromecast. That found what the working folder never showed:

- **The apply script refused a correct TV.** It asked whether `launcherx` and `setupwraith` were enabled for any user, and this TV has a second user profile where `launcherx` is still enabled. Fixed: it now checks user 0 only.
- **The same script stopped at its backup step on Windows PowerShell 5.1**, because adb writes progress to stderr and PowerShell treated that as an error. Fixed.
- **Four Arabic strings were missing** (the quick-settings labels), so an Arabic TV would have shown English there. Fixed, and a test now fails when an English string has no Arabic version.
- **The README said things that were not true:** it offered a download that did not exist, and it said "no location permission" while the manifest declares one so that Android lets the app read the Wi-Fi name. Both now say what is true.
- **GitHub did not recognise the license as MIT**, because a font note had been appended to the license text. The note moved to `NOTICE.md`.
- **The first commits carried a placeholder author**, because the Git identity was not set. The history was rebuilt as one clean commit before publishing.
- **The TV's `config.json` had drifted from its saved profile** (news switched on, one football card instead of two). Not a code bug: the kit script puts the profile back, with a backup first.

## Day 5: your clubs, two looks, and a real download

People do not follow a league, they follow a club. So the strip gained a **favourite clubs** card: pick up to ten
clubs with the remote, and the card shows each one's next or live match, whatever the competition (a Saudi club in
the AFC Champions League Elite, an English one in the Carabao Cup), one club every five seconds. ESPN's
all-competition team schedule made that one request per club; it is large, so it is fetched twice a day, and only a
match that is about to start or running is refreshed from that day's scoreboard. One catch found by reading the
data, not the docs: ESPN files a day by US Eastern time, so a kick-off just after midnight UTC sits on the day before.

The same app now has **two looks**. The tech wallpapers stay as *HomeLab*; a second, calm set called *Serene* is for
any home (a parent's TV, a guest room). Both are inside the APK and picked on the TV, and a fresh install starts on
Serene, because a downloaded app should look finished before anyone writes a config file.

And there is finally a **download**: a release workflow builds and signs both flavors from a tag, so a person can
fetch a complete APK from the Releases page instead of trusting a file someone sent them.

Later the same day, a full test on the TV turned up what a checklist had missed: the cards could not be reached with
the arrows (their layout blocked focus), and two wallpaper choices showed the same pictures. So the settings screen
grew into the one place a viewer needs: language, wallpapers (now with a **Kids** set and plain colours), which cards
show, and the motion. Nothing there needs a computer.

## Lessons

1. Read before you run, pin what you run, and keep private keys out of synced folders.
2. On Google TV, **`setupwraith` before `launcherx`**. Always. Check package names exactly.
3. Never reinstall the only Home app while another Home fallback can take over.
4. Write down the Wi-Fi password before touching the setup wizard.
5. A check that cannot read its input must fail, not pass.
6. Test remote features with the real remote.
7. One change at a time, a test after each, and a rollback written before the change.
8. The factory reset button is your friend: know it before you need it.
9. Test the published copy from a fresh clone, not the folder you wrote it in.
10. A safety check must ask exactly the right question: "disabled for the user that matters", not "disabled somewhere".
11. Write only what you verified. A promise in the README (a download, "no location permission") is a claim to test.
12. Look at the real data before trusting the API's shape: the date a match is filed under is not always its UTC date.
