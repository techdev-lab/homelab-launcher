# Notices

[English](NOTICE.md) · [العربية](NOTICE.ar.md)

## Upstream

HomeLab Launcher is a fork of the `tvhome` launcher in [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), MIT License, Copyright (c) 2026 Gathin. Forked from commit `e561b0551c57700397c6862e9513fc23ef601c02`. The Android package name `com.gathin.tvhome` is kept from upstream so existing installs upgrade in place.

## Design inspiration

The *Dock style* (floating glass) and *Clock style* (glass) options in Launcher settings were inspired by the public description of [tvOS Launcher](https://github.com/techgenius1/tvOS-Launcher) by techgenius, Apache License 2.0. Only its README was read. No code, image or wallpaper from it is in this project: the drawing is our own and uses the `Glass` colour arithmetic. If code from it is ever copied, that file will keep the Apache License 2.0 header, the licence text will be added, and this section will say what was changed.

The *ignore* action for a remote button (`app_keys`) and the option to hide apps from the grid were inspired by features in the public 3.x releases of CleanYourTV (see Upstream, MIT License). The code for both is written here, not copied.

The five fixes of batch F (`docs/BATCH_F_FIXES.md`: the leaked focus listener, guarded `startActivity` calls in the key services, the clock refresh before Android 12, the Xiaomi picture settings route, the banner icon and the Default home backdrop) were prompted by problems found and fixed in later CleanYourTV commits. Thanks to its author for finding them. Only the ideas were taken: the later commits carry no clear licence of their own, none of their code is in this project, and the code is written here.

The ideas of the *side dock*, the *wallpaper dim* and *Arabic digits for the clock* were inspired by the public README of [sukoon-tv](https://github.com/MultiX0/sukoon-tv) by MultiX0. It states no licence, so only the ideas were taken: its README was read, no code or image from it is in this project, and the implementation is ours.

## Scenes look (couch-tv)

The *Scenes* home look (`docs/BATCH_S_SCENES.md`) is inspired by the home screen of [couch-tv](https://github.com/Seagey/couch-tv) by Seagey, MIT License. What was taken: the five scene pictures (`scene-night`, `scene-fog`, `scene-golden`, `scene-dawn`, `scene-water`, each with its cut-out and blurred copy), from commit `31b8f26f445aa023ff1df3cda660740a30a8a8a5`. Their `CREDITS.md` says of them: "Use them however you like." They were made for couch-tv from text only, with no source photo; their copyright status beyond that line is not verified here. The originals are kept byte for byte in `scenes/master/` (SHA-256 of each in `scenes/manifest.json`); the APK carries re-encoded WebP copies made by `scenes/to-scenes.py`. What was not taken: any code, CSS, text or font. The drawing code is written here, from reading what the screen shows. The Classic photos of couch-tv (Creative Commons share-alike) are not used. The name *Scenes* is descriptive and does not imply that its author maintains or endorses this project.

The couch-tv licence, as published:

```
MIT License

Copyright (c) 2026 Seagey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Fonts

The fonts are not covered by the MIT License in [LICENSE](LICENSE). Outfit is copyright The Outfit Project Authors, Antonio (the clock digits of the Scenes look) is copyright The Antonio Project Authors and IBM Plex Sans Arabic is copyright IBM Corp., all under the SIL Open Font License 1.1.

| Font | Files | License | Source |
|---|---|---|---|
| Outfit | `launcher/app/src/main/res/font/outfit*.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-OFL.txt`) | Bundled by upstream |
| IBM Plex Sans Arabic | `launcher/app/src/main/res/font/plex_arabic.ttf` (Regular), `plex_arabic_light.ttf` (Light), `plex_arabic_medium.ttf` (Medium) | SIL OFL 1.1 (`launcher/FONT-LICENSE-IBMPLEXSANSARABIC-OFL.txt`) | [google/fonts](https://github.com/google/fonts) at commit `9862abc066c9b83bf84c3e8ed1fc2d8d48f9bbf0`, files `ofl/ibmplexsansarabic/IBMPlexSansArabic-{Regular,Light,Medium}.ttf`, SHA-256 Regular `6F611412270A132BBAC838DA9259D4C68569B4175F3B3B8FA3FA36A30B56DAB9`, Light `0AF9D1EFFBB36FF74D13C64F752C771ECB9162A451CF151C5425F4D00D4DAA7C`, Medium `B8363AB9F733DFA4F8E96B8B2102C24B5CF4110FB96D1D3D9A9412F6FB49CF74` |
| Antonio (digits and colon only, weight 300) | `launcher/app/src/main/res/font/antonio_digits.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-ANTONIO-OFL.txt`), copyright The Antonio Project Authors | [google/fonts](https://github.com/google/fonts) at commit `95f4904fc8bcf26d3420fe315560c96417c6dec7`, file `ofl/antonio/Antonio[wght].ttf` (74,104 bytes), SHA-256 `9E95A2258ECDF3E45C72C5BBEA1C4CD350E8F7BEBC87C9DBA53B29B1890B8903`; subset made by `scenes/make-clock-font.py` (2,900 bytes, SHA-256 `FD9A72BD993566F8BE20549C588D1A77735B50D78E88E84C3DF0D8377F9CD22B`) |

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
