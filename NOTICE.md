# Notices

## Upstream

HomeLab Launcher is a fork of the `tvhome` launcher in [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), MIT License, Copyright (c) 2026 Gathin. Forked from commit `e561b0551c57700397c6862e9513fc23ef601c02`. The Android package name `com.gathin.tvhome` is kept from upstream so existing installs upgrade in place.

## Fonts

| Font | Files | License | Source |
|---|---|---|---|
| Outfit | `launcher/app/src/main/res/font/outfit*.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-OFL.txt`) | Bundled by upstream |
| Readex Pro | `launcher/app/src/main/res/font/readex_pro.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-READEXPRO-OFL.txt`) | [google/fonts](https://github.com/google/fonts) at commit `8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5`, file `ofl/readexpro/ReadexPro[HEXP,wght].ttf`, SHA-256 `268BBA7E1E8F3B14D798B3FB0E40EBAA3FC39308C9AC0020E2FAF6DF181CC30E` |

## Data sources (online build)

- Football: ESPN public site API (`site.api.espn.com`), no key, used for scoreboards only. Not affiliated with ESPN.
- Weather: [Open-Meteo](https://open-meteo.com) forecast API, free, no key.
- News (off by default): any RSS feed the user adds.

## Trademarks

App names and logos shown in the screenshots (YouTube, Netflix, MBC Shahid, Jellyfin, Prime Video, TOD, Google Play and others) belong to their owners. They appear only because those apps were installed on the test TV. This project is not affiliated with Google or any of them.

## Wallpapers

The pictures in `wallpapers/homelab/` are drawn by `wallpapers/gen.html` (canvas code in this repository, no third-party images) and are covered by the MIT License above.
