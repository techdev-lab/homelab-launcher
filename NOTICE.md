# Notices

## Upstream

HomeLab Launcher is a fork of the `tvhome` launcher in [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), MIT License, Copyright (c) 2026 Gathin. Forked from commit `e561b0551c57700397c6862e9513fc23ef601c02`. The Android package name `com.gathin.tvhome` is kept from upstream so existing installs upgrade in place.

## Design inspiration

The *Dock style* (floating glass) and *Clock style* (glass) options in Launcher settings were inspired by the public description of [tvOS Launcher](https://github.com/techgenius1/tvOS-Launcher) by techgenius, Apache License 2.0. Only its README was read. No code, image or wallpaper from it is in this project: the drawing is our own and uses the `Glass` colour arithmetic. If code from it is ever copied, that file will keep the Apache License 2.0 header, the licence text will be added, and this section will say what was changed.

## Fonts

The fonts are not covered by the MIT License in [LICENSE](LICENSE). Outfit is copyright The Outfit Project Authors and IBM Plex Sans Arabic is copyright IBM Corp., both under the SIL Open Font License 1.1.

| Font | Files | License | Source |
|---|---|---|---|
| Outfit | `launcher/app/src/main/res/font/outfit*.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-OFL.txt`) | Bundled by upstream |
| IBM Plex Sans Arabic | `launcher/app/src/main/res/font/plex_arabic.ttf` (Regular), `plex_arabic_light.ttf` (Light), `plex_arabic_medium.ttf` (Medium) | SIL OFL 1.1 (`launcher/FONT-LICENSE-IBMPLEXSANSARABIC-OFL.txt`) | [google/fonts](https://github.com/google/fonts) at commit `9862abc066c9b83bf84c3e8ed1fc2d8d48f9bbf0`, files `ofl/ibmplexsansarabic/IBMPlexSansArabic-{Regular,Light,Medium}.ttf`, SHA-256 Regular `6F611412270A132BBAC838DA9259D4C68569B4175F3B3B8FA3FA36A30B56DAB9`, Light `0AF9D1EFFBB36FF74D13C64F752C771ECB9162A451CF151C5425F4D00D4DAA7C`, Medium `B8363AB9F733DFA4F8E96B8B2102C24B5CF4110FB96D1D3D9A9412F6FB49CF74` |

Readex Pro (used for the cards up to 2.8) was removed in 2.9.

## Data sources (online build)

- Football: ESPN public site API (`site.api.espn.com`), no key, used for scoreboards only. Not affiliated with ESPN.
- Arab domestic leagues: [TheSportsDB](https://www.thesportsdb.com) free API v1 with its documented public key, next fixtures only. Not affiliated with TheSportsDB.
- Prayer methods: the angles and minute offsets of each method follow [api.aladhan.com](https://aladhan.com/calculation-methods); times are computed on the TV. Not affiliated with Aladhan.
- Weather: [Open-Meteo](https://open-meteo.com) forecast API, free, no key.
- News (off by default): any RSS feed the user adds.

## Trademarks

App names and logos shown in the screenshots (YouTube, Netflix, MBC Shahid, Jellyfin, Prime Video, TOD, Google Play and others) belong to their owners. They appear only because those apps were installed on the test TV. This project is not affiliated with Google or any of them.

## Wallpapers

The pictures in `wallpapers/homelab/` are drawn by `wallpapers/gen.html` (canvas code in this repository, no third-party images) and are covered by the MIT License above.
