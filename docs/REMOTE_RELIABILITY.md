# Remote and reliability options (design note, batch 2)

Status: **PROPOSED, written before any code**, as `docs/DESIGN.md` asks. Three small changes found by reading the code and by
the TV tests of 2.9 and the look options. Each keeps today's behaviour for anyone who changes nothing.

## 1. Remote buttons can be ignored (`app_keys`)

**Today.** `app_keys` maps a remote key code to an app (default: key 190, the YouTube button). The accessibility service
(`HomeKeyService`) sees every key before any app does, so a button that opens a stock app by accident can only be redirected,
never silenced.

**Change.** A key can be mapped to the word `ignore`: `"app_keys": {"190": "ignore"}`. The key is swallowed, down and up, and
nothing opens. The value is a reserved word, not a package name.

**Safety.** The service sees all keys in all apps, so a bad mapping can lock the owner out. A fixed list of **protected keys**
can never be mapped, to an app or to `ignore`: Home, Back, the four arrows, OK (centre and Enter) and Power. A protected or
invalid entry is dropped and the rest of the map still works. The default map is unchanged.

**Rules in one pure class** `AppKeys` (no Android classes, unit tested): `parse(map)`, `isIgnore(value)`, `protectedKey(code)`.
`Config` hands it the JSON as a plain map. `HomeKeyService` and `MainActivity` call `isIgnore`.

## 2. Matches and weather return soon after a failed start (`FeedRetry`)

**Today.** The online build fetches matches, news and weather every 15 minutes while Home is on screen. After a boot the Wi-Fi
often connects after Home is shown: the first fetch fails, `lastFeedMs` is set anyway, and the cards stay empty for up to 15
minutes. The network callback only repaints the Wi-Fi pill.

**Change.**
- While there is no data yet, the next fetch comes sooner: 1, 2, 4, 8 minutes, never later than the normal 15.
- When a network becomes available and there is still no data, one fetch is started after 3 seconds, if the last attempt was at
  least 15 seconds ago (so a flapping network does not cause a storm).
- Nothing changes when all cards are off, or when there is data: the 15-minute rhythm stays.

**Rules in one pure class** `FeedRetry`: `waitMs(haveData, failures, normalMs)` and `refetchOnNetwork(wanted, haveData, lastMs, nowMs)`.

## 3. Hide apps on the TV (`hidden_apps`)

**Today.** `hidden_apps` exists only in `config.xml` and `config.json`, so hiding an app needs a computer.

**Change.** Launcher settings gets a *Hidden apps* row that opens a list of the TV's apps; OK on one hides or shows it. The
choice is saved on the TV and, like the clubs, wins over the theme's list; *Reset* goes back to the theme's list. The launcher
itself cannot be hidden. A hidden app is only removed from the tiles: it stays installed and opens from anywhere else.

**Rules in one pure class** `HiddenApps` (`parse`, `toCsv`, `toggle`, `valid`, a cap on the list): bad package names are dropped,
duplicates removed, a huge stored value is cut at the cap.

## Tests

JVM: `AppKeysTest`, `FeedRetryTest`, `HiddenAppsTest`, each with failure cases (a protected key, an unknown value, an invalid
package, a flapping network). On the TV: the hidden-apps list with the remote (it can be done over ADB keys); the ignored key
needs the owner's physical remote and a pushed `config.json` (which resets the on-TV column and wallpaper choices, so they are
noted first and set again); the late-Wi-Fi retry needs a reboot, so it is checked in the final reboot test.

## Out of scope

Capturing a key on the TV to pick it (the key code is typed in `config.json`), hiding system settings entries, and any new
permission or dependency.
