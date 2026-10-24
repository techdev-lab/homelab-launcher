# Changelog

All builds keep `versionCode 4` so any of them can replace another with `adb install -r`.

## Unreleased
- Arabic labels for the quick-settings panel (Display, Network, Apps and its hint), which showed in English on an Arabic TV. `ResourcesTest` now fails when a translatable English string has no Arabic version.
- `scripts/Apply-TvhomeKit.ps1`: the "launcherx and setupwraith disabled" checks now look at user 0 only (a TV with a second user profile was refused by mistake), and the config backup no longer aborts under Windows PowerShell 5.1.

## 2.5.5 (2026-10-22)
- All user-facing text moved out of the Java code into resources: prayer names, Hijri months and weekdays, "Prayer times", "LIVE", "News" and the "in" phrasing now live in `res/values/strings.xml` and `res/values-ar/strings.xml`. The code itself is English only.
- `ResourcesTest` checks that both languages have complete name lists. No visible change.

## 2.5.4 (2026-10-22)
- Screensaver (`ClockDream`) redesigned: the clock glides to a new place every 20 seconds, with a minute hairline, the Hijri date, the next prayer and a colour-shifting glow. Readex Pro on Arabic screens.
- `scripts/Apply-TvhomeKit.ps1`: one-shot update for a TV that already runs tvhome as Home, with backup and rollback.

## 2.5.3
- Fully Arabic home screen on an Arabic TV: app labels, league names and 114 team names (`assets/names_ar.json`, teams by ESPN id).

## 2.5.2
- Focused app name no longer overlaps the strip: smaller (34sp) and given its own band above the dock.
- Cards share one height; smaller type; the match card has four lines (date and time, league, teams, score).
- Readex Pro font for the cards and the Hijri date.

## 2.5.1
- Football: the scoreboard is asked day by day for up to 7 days (ESPN rejects date ranges), so the card no longer goes empty once today's matches end.
- New `sports_cards` key: one card per league group, side by side.
- News can be turned off with empty `news_feeds`.

## 2.5.0
- Weather card (Open-Meteo), online build only.

## 2.4.x (2026-10-21)
- Arabic theme with a right-to-left layout, Hijri date, offline prayer times (Umm al-Qura).
- `offline` and `online` build flavors; matches (ESPN) and news (RSS) in the online build.
- Rotating generated wallpapers; YouTube remote button through `HomeKeyService`.

## 2.3 (2026-10-20)
- First build on a Chromecast with Google TV from the upstream `tvhome` launcher. Move Mode (long-press reordering) added, then dropped: it does not work with the physical remote.
