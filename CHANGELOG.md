# Changelog

All builds keep `versionCode 4` so any of them can replace another with `adb install -r`.

## Unreleased
- **Dock at the side** (Launcher settings > Dock position, or `"dock_position": "side"`): the dock becomes a column at the end of the reading side, on the right on the English screen and on the left on the Arabic one, with the other apps in a grid beside it. The name of the focused dock app shows in a small pill beside its tile. The default is the bottom row of 2.9, so nothing changes until you pick. Arrow keys are decided by pure functions that are unit tested, including that no tile is a dead end and that the top bar is always reachable. The idea of a dock at the side came from looking at other launchers; the code is ours. Design note: `docs/LAYOUT_OPTIONS.md`.
- **Digits and wallpaper dim** (Launcher settings): *Digits* draws every number on the home screen and the screensaver in Arabic-Indic digits on either language, and *Wallpaper dim* sets the veil over a wallpaper picture (15, 35, 55 or 75 percent; 35 is the veil of 2.9, so nothing changes until you pick). Also `"digits"` and `"wallpaper_dim"` in config.json; the TV's choice wins and a theme push does not clear it. Design note: `docs/LAYOUT_OPTIONS.md`.
- **Store metadata and a corrected `SECURITY.md`**: `fastlane/metadata/android/en-US/` (title, short and full description, changelog, icon) so a store such as IzzyOnDroid can list the app; `SECURITY.md` no longer says releases use a debug key (they use the project key) and now shows the certificate fingerprint and how to check an APK. Design note: `docs/DISTRIBUTION.md`.
- **Ramadan card**: only in Ramadan, one more card after the prayer card counts down to the iftar (Fajr to Maghrib) or to the end of suhoor (Maghrib to Fajr), with the day's two times and "Ramadan · day N". It needs the prayer place like the prayer card, has its own chip in the card row, and is gone after Maghrib on the last day. Suhoor ends at Fajr; use `prayer_adjust` if you want a margin.
- **Prayer alert**: for 10 minutes (Off, 5, 10 or 15 in Launcher settings, or `"prayer_alert_minutes"`) after a prayer time the prayer card says "Asr time · Now" instead of counting down. Visual only: no sound, no permission.
- **Hijri date adjustment** (Launcher settings > Hijri date, or `"hijri_offset_days"`, -2 to +2): for countries that follow moon sighting. It moves the header date, the screensaver and the start of Ramadan. Design note: `docs/RAMADAN_PRAYER.md`.
- **Hide apps on the TV** (Launcher settings > Hidden apps): OK on an app hides it or shows it again; a hidden app stays installed and opens from anywhere else. It wins over the theme's `hidden_apps` and a theme push does not clear it; "Use the theme's list" goes back. The launcher itself cannot be hidden.
- **A remote button can be ignored**: `"app_keys": {"190": "ignore"}` swallows a button pressed by accident. Home, Back, the arrows, OK and Power can never be mapped, so a bad config cannot lock you out.
- **Matches and weather come back soon after a start with no network**: while no card has data the next fetch comes after 1, 2, 4 and 8 minutes (instead of 15), and a network that comes back starts one fetch after 3 seconds. With data nothing changes.
- **Look options** (Launcher settings): *Dock style* (the flat band, or floating glass that takes the colour of the focused app), *Tile corners* (normal or soft; the highlight and the dock plate follow) and *Clock style* (plain, or the time on a glass capsule). Every default is the 2.9 look, so an upgrade changes nothing until you pick. Also `"dock_style"`, `"tile_corners"` and `"clock_style"` in config.json; the TV's choice wins and a theme push does not clear it. The ideas came from the public description of tvOS Launcher (see NOTICE.md); no code or image was copied.

## 2.9.0 (2026-10-28)
- **Your clubs stand out.** Every match card with one of your favourite clubs, on the league cards too, gets a gold star before the competition. The club card now shows each club's **next two** matches, so a continental or cup match two days after a league match is no longer hidden behind it (Al Hilal: Saudi Pro League on Saturday, AFC Champions League Elite on Tuesday). The club card is now the first football card. Matching is by ESPN team id, so it works in both languages.
- **Text size** (Launcher settings > Text size): Small, Medium or Large: the home screen's one row of cards has 5, 4 or 3 places, and when there are more cards the last place shows the rest in turn (every 8 seconds, or OK on it). Every line of every card, the prayer card's included, grows with the size. A second row was tried on the TV and pushed the app dock off the screen. Small is the 2.8 look, so an upgrade changes nothing until you pick. Also `"text_size"` in config.json; the TV's choice wins and a theme push does not clear it.
- **The kick-off line is short and stays in order**: `Thu 29 Oct · 9:17 PM`, and on the Arabic screen the day name, day/month and the time. Before, the long Arabic line was cut off and the time could read backwards ("…:17"). The date and the clock sit in left-to-right isolates; the AM/PM word stays outside. New `WhenTextTest`.
- **New font: IBM Plex Sans Arabic** for the cards, the Arabic header (clock, date, greeting, Hijri date) and the screensaver, Arabic and Latin from one family. Picked by rendering the same cards in seven OFL fonts (Readex Pro, IBM Plex Sans Arabic, Noto Kufi Arabic, Noto Sans Arabic, Tajawal, Cairo, Almarai): it is the narrowest at the same size, so all five prayer times fit a fifth of the screen. Readex Pro is removed. The English header keeps Outfit.
- **Prayer methods.** 14 methods (Umm al-Qura stays the default; Egyptian, Gulf, Dubai, Qatar, Kuwait, Jordan, Morocco, Algeria, Tunisia, Turkey, MWL, ISNA, Karachi) and standard or Hanafi Asr, picked in Launcher settings > Prayer times or with `prayer_method` / `asr_method`; `prayer_adjust` shifts each time by up to 30 minutes. Each method uses api.aladhan.com's angles and minute offsets and is tested against aladhan in two seasons to within 2 minutes (`PrayerMethodsTest`, 28 city/date rows). An unknown method name says so on the card instead of falling back quietly. Tomorrow's Fajr after Isha now uses tomorrow's UTC offset, so a clock change at midnight (Egypt, Morocco) no longer moves the countdown by an hour.
- **Arab domestic leagues:** the Egyptian (`tsdb.4829`), UAE (`tsdb.4678`), Qatari (`tsdb.4663`) and Moroccan (`tsdb.4520`) leagues from TheSportsDB's free API with its public key: the next kick-off (no live scores), fetched at most hourly with requests spaced under the free limit; postponed or cancelled matches are never shown as upcoming. The Cairo and Dubai examples use them and their country's prayer method. New `ArabLeaguesTest`.
- **Removed: Module cards and Move mode.** Nothing in the world declares a module card, so the menu only ever said "No module cards installed"; Move mode (hold OK on a tile) never worked with the physical remote. Holding OK on the Settings button now opens Launcher settings directly. An order saved by Move mode is cleared once, so the dock follows `dock_apps` again. `module_cards` in an old config.json is ignored. The app label cleaner moved to `Labels` with its tests.
- Fixes from the review of 2.6 to 2.8.1: the release workflow's actions are pinned to commit SHAs; the card switches no longer change the shared empty settings object.
- `scripts/Set-TvhomeAsHome.ps1` no longer refuses a TV without a pushed `wallpapers` folder (the built-in sets need none since 2.7), and reads saved networks listed as `* ID: n SSID:` (some Android 14 builds). New fake-TV scenario; 10/10 pass.
- Found by installing 2.9.0 on a Chromecast with Google TV 4K and testing every change: a match between two favourite clubs arrived twice and filled both of a club's places (Al Hilal's AFC match was hidden); a prayer name and its time could break across lines in a narrow card; and the home screen's clock and Gregorian date could turn English on an Arabic screen when Android reset the process language (the home screen now takes its language like the screensaver and puts it back every minute). All fixed and checked again on the TV, with the English screen and the screensaver.
- 2.8.1 was merged but never published as its own release; its fixes ship in 2.9.0.

## 2.8.1 (2026-10-28, not released separately)
- Found by a second full test on the TV after the 2.8.0 release: on the English screen the club card's league line cut off the position ("5/8") and the weather card cut the city ("Weather · Al-A..."). The position now comes first ("5/8 · Premier League") and the weather card shows the city on its own line.
- **Example configs show a wallpaper out of the box.** Every file in `config/` said `"wallpaper_mode": "rotate"`, which needs pictures pushed to the TV: someone who copied an example and skipped that step got a plain screen. They now use `"serene"` (built into the APK); `docs/INSTALL.md` says wallpapers need no pushing and shows how to use your own.
- Wording: the download table and release notes said "both wallpaper sets"; there are three. `wallpaper_interval_min` also applies to the built-in sets (CONFIG.md). Arabic grammar fix in the README.
- The README's install examples no longer name a version.

## 2.8.0 (2026-10-28)
- **Launcher settings has everything a viewer needs** (hold OK on the Settings button): Language (TV language, Arabic, English: saved for the launcher and handed to Android as the app's own language), Apps per row, Wallpapers, Cards, Favourite clubs and Motion.
- **Kids wallpapers**: six generated children's scenes (`wallpapers/kids/`, `wallpapers/gen-kids.html`), built into the APK; `wallpaper_mode` `kids`.
- **Plain colours** instead of "no wallpaper": black, navy, teal, forest, burgundy, graphite (saved as `background_color`). The "My pictures" choice left the TV screen (it duplicated the pushed HomeLab set); `"wallpaper_mode": "rotate"` in config.json still shows your own pictures.
- **Card switches**: prayer times, weather, football and my clubs can each be turned off on the TV. Weather keeps working with the prayer card off. The strip's widths follow the cards that are on.
- **Motion** is one choice of three: Calm (soft fades), Lively (tiles lift, icons zoom) or Off.
- **The strip cards take focus**: Up from the dock reaches them, Up again the top bar, Down returns. A focused card has a white edge that survives the clubs card's 5-second swap. OK on the clubs card shows the next club, on the rotating league or news card the next item, on the others it opens Launcher settings. Found on the TV: the strip's layout blocked focus on its children (`blocksDescendants`).
- README in both languages: a table of what works right after installing the APK and what needs a computer once (Home button, YouTube button, screensaver), the Kids look, what's new in 2.8.

## 2.7.0 (2026-10-28)
- **Ready-to-download APK.** A new `Release APK` workflow builds both flavors from a tag, signs them with the project key and publishes them on the Releases page with `SHA256SUMS.txt` (`docs/RELEASING.md`). File names: `HomeLab-Launcher-<version>.apk` (full) and `-offline.apk`.
- **Two wallpaper sets inside the APK, picked on the TV.** *Launcher settings > Wallpapers*: **HomeLab** (6), **Serene** (10), *My pictures* (only when you pushed some) or *Plain*. New `wallpaper_mode` values `homelab` and `serene`; a fresh install with no config.json starts on Serene. The pictures are copied from `wallpapers/` into the APK at build time, so there is one copy in the repository. A theme push or *Follow theme* clears the on-TV choice, like the other look settings.
- The "general" set from 2.6.0 is now called **Serene** (`wallpapers/serene/`, `wallpapers/gen-serene.py`); the pictures are unchanged.
- The APK grows from about 0.5 MB to about 3 MB (the 16 wallpapers).
- Tests: `WallpapersTest` covers the mode rules (TV choice first, then config, Serene on a fresh install, older themes keep their look).
- Found on the TV: the Wallpapers row now reads right to left on an Arabic screen, like the columns row.

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
