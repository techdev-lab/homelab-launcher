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
| `wallpaper_mode` | `plain`, `fixed`, `rotate` | `fixed` uses `files/wallpaper.jpg`, `rotate` every picture in `files/wallpapers/`. |
| `wallpaper_interval_min` | `1` to `240` (default 5) | Minutes per picture in `rotate`. |
| `background_color` | `"#0B0B0C"` | Behind everything. |
| `animations`, `reduced_motion` | `true` / `false` | Turn effects down on slow TVs. |
| `module_cards` | `true` | Allow app cards. |
| `sky_intensity` | `0` to `100` | Optional time-of-day sky behind the screen; 0 hides it. |
| `shamsi_date`, `festival` | | Inherited from the upstream launcher; off in the examples. |

## Wallpapers

`wallpapers/homelab/` has six 1920x1080 pictures generated in the browser by `wallpapers/gen.html` (no photos, no third-party images). Open the HTML file, export, and push the JPGs to `files/wallpapers/`.
