# Changelog

All builds keep `versionCode 4` so any of them can replace another with `adb install -r`.

## 2.7.0 (2026-10-28)
- **Ready-to-download APK.** A new `Release APK` workflow builds both flavors from a tag, signs them with the project key and publishes them on the Releases page with `SHA256SUMS.txt` (`docs/RELEASING.md`). File names: `HomeLab-Launcher-<version>.apk` (full) and `-offline.apk`.
- **Two wallpaper sets inside the APK, picked on the TV.** *Launcher settings > Wallpapers*: **HomeLab** (6), **Serene** (10), *My pictures* (only when you pushed some) or *Plain*. New `wallpaper_mode` values `homelab` and `serene`; a fresh install with no config.json starts on Serene. The pictures are copied from `wallpapers/` into the APK at build time, so there is one copy in the repository. A theme push or *Follow theme* clears the on-TV choice, like the other look settings.
- The "general" set from 2.6.0 is now called **Serene** (`wallpapers/serene/`, `wallpapers/gen-serene.py`); the pictures are unchanged.
- The APK grows from about 0.5 MB to about 3 MB (the 16 wallpapers).
- Tests: `WallpapersTest` covers the mode rules (TV choice first, then config, Serene on a fresh install, older themes keep their look).

## 2.6.0 (2026-10-28)
- **Favourite clubs.** Pick up to 10 clubs on the TV (Launcher settings > Favourite clubs) or in `config.json` (`favorite_teams`, ESPN team ids). A club card on the home screen shows each club's live or next match in **any** competition (league, King's Cup, AFC Champions League Elite, UEFA Champions League, domestic cups) and moves to the next club every 5 seconds (`club_rotate_seconds`, 3 to 60). A derby between two of your clubs shows once. The card shows "2/5" so you know more clubs follow. Online build only.
- Data: one ESPN team schedule per club every 12 hours (about 300 KB each), and that day's scoreboard only while one of your clubs is about to play or playing, so the score and minute stay current without heavy traffic. The response cap is now 768 KB (a big club's season schedule is about 320 KB).
- The picker lists 136 clubs: the Saudi Pro League, the five big European leagues, the Gulf clubs in the AFC Champions League Elite, and the other UEFA Champions League clubs. Any other ESPN team id works in `config.json`.
- Arabic names for all Ligue 1 clubs, the AFC Champions League Elite clubs and 15 more competitions (King's Cup, Carabao Cup, Copa del Rey, Europa League and others).
- New wallpaper set `wallpapers/general/`: ten calm, neutral scenes for a TV that is not yours (no text, no people, nothing household), drawn by `wallpapers/gen-general.py` (Pillow and numpy, fixed seeds, rebuildable byte for byte). New example `config/examples/general.json`.
- The screensaver now follows the home screen's language: `config.json` `language` first, then the launcher's own Android language (`cmd locale set-app-locales`), then the TV's. Before, an Arabic home screen still got an English screensaver, because Android starts a screensaver with the TV's system language only. New `Language` class and `LanguageTest`.
- Arabic labels for the quick-settings panel (Display, Network, Apps and its hint), which showed in English on an Arabic TV. `ResourcesTest` now fails when a translatable English string has no Arabic version.
- `scripts/Apply-TvhomeKit.ps1`: the "launcherx and setupwraith disabled" checks now look at user 0 only (a TV with a second user profile was refused by mistake), and the config backup no longer aborts under Windows PowerShell 5.1.
- Docs: `INSTALL.md` explains how to give the launcher its own language (Arabic on an English TV) and how to run the kit script; `JOURNEY.md` has a Day 4 with the errors found by testing the published copy and how they were fixed.
- Tests: `ClubsTest` (9 cases: id rules, any-competition pick, live first, derby once, 21-day window, live refresh window, scoreboard update). 130 unit tests per build flavor.

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
