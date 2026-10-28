# Configuration (`config.json`)

tvhome reads `/sdcard/Android/data/com.gathin.tvhome/files/config.json` when it starts. Every key is optional; a missing or badly typed key keeps the default. After editing, push the file and run `adb shell am force-stop com.gathin.tvhome`, then press HOME.

Start from `config/config.example.json` or a city in `config/examples/`.

## Apps

| Key | Example | Notes |
|---|---|---|
| `dock_apps` | `["com.google.android.youtube.tv", "net.mbc.shahidTV"]` | Package names for the top row, in order. Get yours from `adb shell pm list packages -3` or `scripts/Get-DeviceInventory.ps1`. |
| `fill_dock` | `true` | Fill empty dock slots with other apps. |
| `hidden_apps` | `["com.example.app"]` | Never shown. |
| `columns` | `4` | Grid columns. |
| `app_keys` | `{"190": "com.google.android.youtube.tv"}` | Remote key code to app. Needs the HomeKeyService (see INSTALL.md step 6). 190 is the YouTube button. |
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
| `text_size` | `small` (default), `medium`, `large` | Text size of the home screen's cards: 5, 4 or 3 cards in a row, the rest on the next row. The TV's choice (Launcher settings > Text size) wins and a theme push does not clear it. |

**Finding a team id.** The picker on the TV covers 136 clubs. For any other club, open
`https://site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams` (for example `ksa.1`) and use the `id` next to the
club's name. A few common ones: Al Hilal `929`, Al Nassr `817`, Al Ittihad `2276`, Al Ahli `8346`, Man City `382`,
Real Madrid `86`, Barcelona `83`, Liverpool `364`, Arsenal `359`, Al Ain `7128`, Al Sadd `7135`.

ESPN has no domestic data for the Egyptian, Moroccan, UAE or Qatari leagues, so clubs from those leagues show only their
continental matches (AFC or CAF Champions League), and nothing between tournaments.

League codes are ESPN's. Checked to work:

| Code | League | Code | League |
|---|---|---|---|
| `ksa.1` | Saudi Pro League | `uefa.champions` | UEFA Champions League |
| `ksa.kings.cup` | Saudi King's Cup | `afc.champions` | AFC Champions League Elite |
| `eng.1` | Premier League | `caf.champions` | CAF Champions League |
| `esp.1` | LaLiga | `caf.confed` | CAF Confederation Cup |
| `ita.1` | Serie A | `global.gulf_cup` | Arabian Gulf Cup |
| `ger.1` | Bundesliga | `fifa.worldq.afc` / `fifa.worldq.caf` | World Cup qualifiers |
| `fra.1` | Ligue 1 | | |

Not available from this source (checked): the Egyptian, UAE, Qatari, Moroccan, Kuwaiti, Jordanian, Tunisian and Algerian domestic leagues.

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
