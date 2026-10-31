# Design notes

[English](DESIGN.md) · [العربية](DESIGN.ar.md)

Status: **DRAFT for review** (describes the code as of version 3.0.0; the notes of each batch of 2.10 and 3.0 are in `docs/LOOK_OPTIONS.md`, `docs/REMOTE_RELIABILITY.md`, `docs/RAMADAN_PRAYER.md`, `docs/DISTRIBUTION.md` and `docs/LAYOUT_OPTIONS.md`).

## Goal

A Home screen for Google TV that an Arabic-speaking, Muslim household can live with every day: their language end to end, prayer times and the Hijri date without any account or location permission, their football, their apps, and nothing else. It must be installable by a non-developer with a written guide, and fully reversible.

## Non-goals

- No root, no bootloader unlock, no system partition changes.
- No accounts, analytics, crash reporting or remote configuration.
- No content: the launcher shows apps; it does not stream anything.
- Not a general Android TV debloater. The optional package list in INSTALL.md is advice, not part of the app.

## Shape

One Java Android app (`launcher/`, package `com.gathin.tvhome`, minSdk 26, targetSdk 34), forked from the `tvhome` launcher of Gathin23/cleanyourtv. Two product flavors from one source:

| Flavor | INTERNET | What the network is used for |
|---|---|---|
| `offline` | no (absent from the merged manifest) | nothing; prayer times, Hijri date and the UI work offline |
| `online` | yes | ESPN public scoreboard (football), Open-Meteo (weather), optional RSS |

`versionCode` stays at 4 across 2.3 to 2.5.x on purpose: the APKs are not debuggable, and Android refuses a downgrade of `versionCode` without `install -d`, so keeping it constant keeps every older build an `install -r` away.

## Main parts

| Part | File | Notes |
|---|---|---|
| Home screen | `MainActivity.java` | Header (clock, Hijri and Gregorian date, Wi-Fi and Settings pills), the interests strip, the app grid with a dock. |
| Settings | `Config.java` | `config.json` in the app's external files dir; every key optional, bad values ignored. |
| Prayer times | `Prayer.java` | Umm al-Qura method, praytimes.org formulas, checked in `PrayerTest` to within 2 minutes of an independent source. |
| Hijri date | `Hijri.java` | Platform ICU `IslamicCalendar` (Umm al-Qura). |
| Strip logic | `Interests.java` | Pure Java (unit tested): which match a card shows, card groups, league fetch list, ESPN day strings. |
| Network | `src/online/.../Feeds.java` | Scoreboard day by day for up to 7 days (ESPN rejects a date range), weather, RSS. HTTPS only, 8 s timeouts, 512 KB cap, no redirects. The offline flavor has a stub. |
| Arabic names | `NamesAr.java`, `assets/names_ar.json` | Apps by package, leagues by ESPN code, teams by ESPN team id. Used only on the Arabic screen. |
| Look and layout options | `DockStyle`, `TileShape`, `ClockStyle`, `DigitStyle`, `WallpaperDim`, `DockPosition`, `SideNav`, `StripSize` | Pure Java, unit tested. The TV's choice wins over `config.json`, then the default; an unknown value counts as unset. `AppAdapter.Slice` gives the side dock its own list over the same tiles. |
| Ramadan and prayer | `RamadanCountdown`, `PrayerAlert`, `HijriShift` | Pure Java, unit tested: the iftar and suhoor card, the on-screen alert at a prayer time, the Hijri date adjustment. |
| Remote keys | `HomeKeyService.java` | Accessibility service that only filters key events (`capabilities=8`, no window content). Needed because the YouTube button is a global key routed to the stock launcher. |
| Screensaver | `ClockDream.java` | DreamService: gliding clock, Hijri date, next prayer, minute hairline, colour-shifting glow. No network. |
| VPN badge | `VpnBadge.java` | Green capsule while a VPN transport is up; red when `vpn_app` is set and no VPN is up. |

## Data sources and privacy

- **ESPN** `site.api.espn.com/apis/site/v2/sports/soccer/<league>/scoreboard[?dates=YYYYMMDD]`: public, no key. Sends the league code and a date. Team names are English only.
- **Open-Meteo** `api.open-meteo.com/v1/forecast`: free, no key. Sends the prayer coordinates rounded to 2 decimals.
- **RSS**: only feeds the user puts in `config.json`.
- Nothing else leaves the TV. The location permission is not requested: the place for prayer and weather is typed into the config.

## Home switch safety

Google TV has two Home owners: `launcherx` (stock home) and `setupwraith` (setup wizard, also a Home fallback). Disabling `launcherx` alone makes `setupwraith` the Home and can loop the device on boot. `scripts/Set-TvhomeAsHome.ps1` disables `setupwraith` first, then `launcherx`, after checking that the Wi-Fi network in use is not owned by `setupwraith` (fail closed when the owner cannot be read). `scripts/Apply-TvhomeKit.ps1` refuses to reinstall unless both are disabled.

## Signing and releases

Releases are built from a tag by `.github/workflows/release.yml` and signed with one long-lived project key (policy and
setup in `docs/RELEASING.md`). Its certificate SHA-256 is pinned in `release.yml` and `scripts/Test-Release.ps1`: a release
whose APKs carry any other certificate is never published. The release job runs without a build cache and with the
narrowest token permissions. Every action in every workflow is pinned to a commit SHA, enforced by zizmor in CI
(`.github/zizmor.yml`). Rotating the key is not planned: Android would refuse the update on every installed TV.

## Known issues

- The on-TV "deadman" rollback timer in `Set-TvhomeAsHome.ps1` did not survive a dropped ADB link on Android 14. Keep the rollback command ready.
- Prayer methods follow api.aladhan.com's definitions; they are not checked against each country's printed calendar.
- The Egyptian, UAE, Qatari and Moroccan leagues (TheSportsDB free API) show the next kick-off only, no live scores.

## Roadmap (not started)

- Live scores for the Arab domestic leagues (needs a paid source or one that allows it).
- More Arabic team names (community contributions to `names_ar.json`).
- "My looks" (save and restore a whole set of look choices), more clock faces, a recent-apps order if the platform allows it.
