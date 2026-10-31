# Configuration (`config.json`)

tvhome reads `/sdcard/Android/data/com.gathin.tvhome/files/config.json` when it starts. Every key is optional; a missing or badly typed key keeps the default. After editing, push the file and run `adb shell am force-stop com.gathin.tvhome`, then press HOME.

Start from `config/config.example.json` or a city in `config/examples/`.

## Apps

| Key | Example | Notes |
|---|---|---|
| `dock_apps` | `["com.google.android.youtube.tv", "net.mbc.shahidTV"]` | Package names for the top row, in order. Get yours from `adb shell pm list packages -3` or `scripts/Get-DeviceInventory.ps1`. |
| `fill_dock` | `true` | Fill empty dock slots with other apps. |
| `hidden_apps` | `["com.example.app"]` | Never shown. Apps hidden on the TV (Launcher settings > Hidden apps) win over this list and a theme push does not clear them; "Use the theme's list" there goes back to it. The launcher itself cannot be hidden. |
| `columns` | `4` | Grid columns. |
| `app_keys` | `{"190": "com.google.android.youtube.tv"}` | Remote key code to app. Needs the HomeKeyService (see INSTALL.md step 6). 190 is the YouTube button. Use the word `ignore` instead of a package to swallow a button pressed by accident: `{"190": "ignore"}`. Home, Back, the arrows, OK and Power can never be mapped; an invalid entry is dropped and the others still work. |
| `vpn_app` | `"com.wireguard.android"` | Shows a green **VPN** capsule in the Wi-Fi pill while a VPN is up, and a red one when this app is set but no VPN is up. |

## Language

| Key | Values | Notes |
|---|---|---|
| `language` | `system` (default), `ar`, `en` | `system` follows the TV. Arabic is right to left, with Arabic app, league and team names where known (`launcher/app/src/main/assets/names_ar.json`). The screensaver uses the same language as the home screen: this key first, then the launcher's own Android language (see [INSTALL.md](INSTALL.md), step 4), then the TV's. |
| `hijri_date` | `true` / `false` | Umm al-Qura calendar, computed on the TV. |

## Prayer times and weather

| Key | Example | Notes |
|---|---|---|
| `prayer_city` | the city name in Arabic | Name shown on the Arabic screen. The files in `config/` have examples. |
| `prayer_city_en` | `"Makkah"` | Name shown on the English screen. |
| `prayer_lat`, `prayer_lng` | `21.4225`, `39.8262` | Decimal degrees. `0, 0` turns the prayer card off. |
| `prayer_method` | `umm_al_qura` (default), `egypt`, `gulf`, `dubai`, `qatar`, `kuwait`, `jordan`, `morocco`, `algeria`, `tunisia`, `turkey`, `mwl`, `isna`, `karachi` | The calculation method, as api.aladhan.com defines it (angles and its fixed minute offsets); each one is tested against aladhan to within 2 minutes. A name the launcher does not know shows "Unknown prayer method" on the card instead of times. Launcher settings > Prayer times picks it on the TV, and that choice wins. A method gives that method's times: they can differ by a few minutes from a country's printed calendar. |
| `asr_method` | `standard` (default) or `hanafi` | Asr when the shadow is the object's length (standard) or twice it (Hanafi). Also on the TV. |
| `prayer_adjust` | `{"fajr": 2, "isha": -1}` | Your own minutes per time (`fajr`, `sunrise`, `dhuhr`, `asr`, `maghrib`, `isha`), at most 30 either way. |
| `prayer_alert_minutes` | `0` to `30` (default `10`) | For this many minutes after a prayer time (not sunrise) the prayer card says it is time for that prayer, instead of counting down. `0` turns it off. Visual only. The TV's choice (Launcher settings > Prayer alert) wins and a theme push does not clear it. A value outside the range is ignored. |
| `hijri_offset_days` | `-2` to `2` (default `0`) | Days added to the Hijri date, for a country that follows moon sighting. Moves the header date, the screensaver and the start of Ramadan. The TV's choice (Launcher settings > Hijri date) wins and a theme push does not clear it. A value outside the range is ignored, not clamped. |
| `weather` | `true` (default in the online build) | Open-Meteo for the same coordinates (sent at 2 decimals, about 1 km). |

The method is **Umm al-Qura** (Fajr 18.5°, Isha 90 minutes after Maghrib, 120 in Ramadan, Asr shadow factor 1). It was checked against an independent source to within 2 minutes. Where your country's official method differs, expect a few minutes of difference.

## Football (online build only)

| Key | Example | Notes |
|---|---|---|
| `sports_cards` | `[["ksa.1"], ["uefa.champions", "eng.1", "esp.1", "ita.1", "ger.1"]]` | One card per group, side by side. Each card shows a live match of its group, otherwise the next kick-off within 7 days. Up to 4 groups of 8 leagues. |
| `sports_leagues` | `["ksa.1"]` | Without `sports_cards`: one card that rotates through these leagues every minute. |
| `sports_teams` | `["Al Hilal"]` | Only matches with one of these teams (case-insensitive, either side). Empty keeps all. |
| `favorite_teams` | `["929", "817", "382"]` | Your clubs, up to 10, by ESPN team id. One extra card shows each club's live or next two matches (within 21 days) in any competition, one match at a time, with a gold star; any league card with one of these clubs gets the star too. Clubs picked on the TV (Launcher settings > Favourite clubs) take priority; "Clear" there goes back to this list. |
| `club_rotate_seconds` | `5` (default) | Seconds per match on the club card, 3 to 60. |
| `text_size` | `small` (default), `medium`, `large` | Text size of the home screen's cards: 5, 4 or 3 places in the one row; the last place shows the remaining cards in turn. The TV's choice (Launcher settings > Text size) wins and a theme push does not clear it. |
| `dock_style` | `band` (default), `floating` | The plate behind the dock row: the flat band of 2.9, or translucent glass that takes the colour of the focused app. The TV's choice (Launcher settings > Dock style) wins and a theme push does not clear it. |
| `tile_corners` | `normal` (default), `soft` | How round the app tiles are (16 dp or 24 dp). The glass highlight and the dock plate follow. The TV's choice (Launcher settings > Tile corners) wins and a theme push does not clear it. |
| `digits` | `latin` (default), `arabic` | `arabic` draws every number on the home screen and the screensaver in Arabic-Indic digits (the clock, the dates, prayer times, scores, temperatures). The TV's choice (Launcher settings > Digits) wins and a theme push does not clear it. |
| `wallpaper_dim` | `0` to `80` (default `35`) | How dark the veil over a wallpaper picture is, in percent black. 35 is the veil of 2.9. No effect with a plain colour. The TV's choice (Launcher settings > Wallpaper dim: 15, 35, 55, 75) wins. A value outside the range is ignored. |
| `clock_style` | `plain` (default), `glass` | The header clock: plain text, or the time on a small glass capsule. The TV's choice (Launcher settings > Clock style) wins and a theme push does not clear it. |

**Finding a team id.** The picker on the TV covers 136 clubs. For any other club, open
`https://site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams` (for example `ksa.1`) and use the `id` next to the
club's name. A few common ones: Al Hilal `929`, Al Nassr `817`, Al Ittihad `2276`, Al Ahli `8346`, Man City `382`,
Real Madrid `86`, Barcelona `83`, Liverpool `364`, Arsenal `359`, Al Ain `7128`, Al Sadd `7135`.

ESPN has no domestic data for the Egyptian, Moroccan, UAE or Qatari leagues, so the favourite-clubs card shows clubs
from those leagues only in their continental matches. Their **leagues** come from a second source (below).

League codes are ESPN's, except the `tsdb.` ones. Checked to work:

| Code | League | Code | League |
|---|---|---|---|
| `ksa.1` | Saudi Pro League | `uefa.champions` | UEFA Champions League |
| `ksa.kings.cup` | Saudi King's Cup | `afc.champions` | AFC Champions League Elite |
| `eng.1` | Premier League | `caf.champions` | CAF Champions League |
| `esp.1` | LaLiga | `caf.confed` | CAF Confederation Cup |
| `ita.1` | Serie A | `global.gulf_cup` | Arabian Gulf Cup |
| `ger.1` | Bundesliga | `fifa.worldq.afc` / `fifa.worldq.caf` | World Cup qualifiers |
| `fra.1` | Ligue 1 | | |

**Arab domestic leagues** come from [TheSportsDB](https://www.thesportsdb.com)'s free API (its documented public key, so
nothing secret is in the app). They show the **next kick-off only, no live scores** (live scores are a paid feature
there), refreshed at most once an hour and spaced so a TV stays far below the free limit of 30 requests a minute. A
postponed or cancelled match is never shown as upcoming. Checked on 2026-10-28:

| Code | League |
|---|---|
| `tsdb.4829` | Egyptian Premier League |
| `tsdb.4678` | UAE Pro League |
| `tsdb.4663` | Qatar Stars League |
| `tsdb.4520` | Moroccan Botola Pro |

Example: `"sports_cards": [["ksa.1"], ["tsdb.4829", "tsdb.4678", "tsdb.4663", "tsdb.4520"]]` gives the Saudi league and
one card for the four Arab leagues. Still not available (no free source checked yet): Kuwait, Jordan, Tunisia, Algeria.

## News (online build only, off in the examples)

| Key | Example | Notes |
|---|---|---|
| `news_feeds` | `["https://feeds.bbci.co.uk/arabic/rss.xml"]` | HTTPS RSS or Atom, for the Arabic screen. `[]` turns the card off. |
| `news_feeds_en` | `["https://feeds.bbci.co.uk/news/rss.xml"]` | For the English screen. |

## Look

| Key | Values | Notes |
|---|---|---|
| `wallpaper_mode` | `serene`, `homelab`, `kids`, `rotate`, `fixed`, `plain` | `serene`, `homelab` and `kids` rotate the sets built into the APK; `plain` shows `background_color`. `rotate` uses every picture you pushed to `files/wallpapers/`, `fixed` the single `files/wallpaper.jpg`. Without a config.json a fresh install shows `serene`. A choice made on the TV (*Launcher settings > Wallpapers*) wins until the next theme push or *Follow theme*. |
| `wallpaper_interval_min` | `1` to `240` (default 5) | Minutes per picture, for `rotate` and for the built-in sets. |
| `background_color` | `"#0B0B0C"` | Behind everything. |
| `animations`, `reduced_motion` | `true` / `false` | Turn effects down on slow TVs. |
| `sky_intensity` | `0` to `100` | Optional time-of-day sky behind the screen; 0 hides it. |
| `shamsi_date`, `festival` | | Inherited from the upstream launcher; off in the examples. |

## Choices made on the TV

*Launcher settings* (hold OK on the Settings button) saves these on the TV. They win over `config.json`:

| Setting | Overrides | Cleared by |
|---|---|---|
| Language (TV language, Arabic, English) | `language` | choosing *TV language* |
| Apps per row, wallpapers and plain colour, motion | `columns`, `wallpaper_mode`, `background_color`, `animations`, `advanced_effects` | a new theme push, or *Follow theme* |
| Cards: prayer times, weather, football, my clubs | a card that is off never shows, whatever the config says | switching it on again |
| Favourite clubs | `favorite_teams` | *Clear* on the clubs screen |

## Wallpapers

Two generated sets, no photos and no third-party images. Both are copied into the APK at build time
(`syncWallpaperAssets` in `launcher/app/build.gradle`), so `"wallpaper_mode": "homelab"`, `"serene"` or `"kids"` needs no push:

| Folder | Pictures | Made by | For |
|---|---|---|---|
| `wallpapers/homelab/` | 6 | `wallpapers/gen.html` (open in a browser, export) | A home-lab look: racks, terminals, circuits. |
| `wallpapers/kids/` | 6 | `wallpapers/gen-kids.html` (open in a browser, `?s=1` to `?s=6`, export) | A children's TV: a rocket in space, under the sea, a rainbow castle, dinosaurs, balloons, a night garden. |
| `wallpapers/serene/` | 10 | `wallpapers/gen-serene.py` (Python, Pillow and numpy) | Any TV: dusk, ridges, still water, dunes, ribbons, circles, night stars, aurora, sea at dawn, a faint geometric lattice. Nothing that points to a household. (Called "general" in 2.6.0.) |

To use your own pictures instead, push them to `files/wallpapers/` and set `"wallpaper_mode": "rotate"`.
`config/examples/general.json` is a neutral starting point for a TV you set up for someone else (Serene, no leagues).
