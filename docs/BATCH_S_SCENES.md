# Batch S: the Scenes look (design note)

[English](BATCH_S_SCENES.md) · [العربية](BATCH_S_SCENES.ar.md)

Status: implemented on the `feat/batch-s-scenes` branch after the maintainer's "ok" (2026-11-16); the decisions D1 to D3 are settled (section 10) and the measured numbers are in section 4. Nothing has been run on a TV yet (section 9). No WebView, no new permission, no new service, no new dependency, no `INTERNET` in the offline flavor. The first lines below describe the design as it was written; where the code differs, the later sections say so.
**Scenes**: a full-screen picture, a large clock whose digits are filled with a blurred copy of that
picture, and the scenery drawn over the bottom of the digits. It also adds rotation between scenes, "follow the sun", a
plain black OLED choice, the credits, and two missing docs. Everything is native Java: no WebView, no new permission,
no new service, no new dependency, no `INTERNET` in the offline flavor. The line numbers are those of `main` at
`aff3041`.

The idea is inspired by the home screen of [couch-tv](https://github.com/Seagey/couch-tv) by Seagey (MIT). Its
code is not used and was only read to understand what is drawn; the five scene pictures are used with the credit
section 7 describes.

## 0. What the code does today (read before the plan)

| Thing | Where on `main` | What it does |
|---|---|---|
| Wallpaper | `MainActivity.java:590-630` (`showWallpaper`) | one `ImageView`, `CENTER_CROP`, decoded once at screen size in RGB_565 (about 4 MB at 1080p), added at root index 0; a dark veil (`WallpaperDim`) at index 1 |
| Wallpaper wiring | `MainActivity.java:349-353` | `wallpapers` non-empty: rotation by minute slot (`Wallpapers.indexAt`, `:636-643`); else a single file |
| Cross-fade | `MainActivity.java:626-629` | 700 ms, only with `animations && !reducedMotion` |
| Header clock | `res/layout/activity_main.xml:80-92`, copies in `layout-ar` and `layout-iw` | `TextClock` `clock` (52 sp, Outfit Light, 12/24 h from the system) and `ampm`; the Arabic and Hebrew layouts are hand-mirrored copies, because the launcher does not use `supportsRtl` |
| Clock refresh | `MainActivity.java:818-830`, `ClockRefresh` | only where the system tick is not enough (before Android 12) |
| Digits | `DigitStyle.java`, `MainActivity.java:385-389` | `latin` or `arabic`; a `DigitsTransformation` on the clock and date texts |
| Look choices | `ClockStyle`, `DockStyle`, `TileShape` | pure Java; rule "TV choice, then `config.json`, then default"; keys survive a theme push (`Config.java:437`, `syncThemeChoices`) |
| Settings rows | `LauncherSettingsActivity.java:264-349` (`buildLook`) | chips per choice, saved in the `launcher_settings` preferences, then `changed()` (`:934`) |
| Place | `Config.java:330-331`, `Interests.validPlace` (`:241`) | `prayer_lat` and `prayer_lng` from `config.json`; absent or `0, 0` means no place |
| Sun times | `Prayer.times(...)` (`Prayer.java:100-133`) | returns fajr, sunrise, dhuhr, asr, maghrib, isha as local hours for a place and date; pure Java |
| Small memory | `SmallMemory.java`, `Config.java:258` | `Config.smallMemory` and `totalMemBytes`; batch L wrote it for this batch |
| Wallpaper size | `WallpaperSize.java` | pure `sampleSize` and `coverScale`; reused here |

Not in the code yet: a layer with alpha, a clock whose digits show a picture, a timer shorter than a minute. There is no
About screen; the nearest place is the read-only memory line of Launcher settings (`LauncherSettingsActivity.java:159`).

## 1. The look: three layers, one custom view

One new `SceneView` (extends `View`, no child views) is added at root index 0 exactly where `wallView` goes today, and
draws in this order:

1. **Background**: the scene picture, 1920x1080, drawn with a cover matrix (the same rule as `CENTER_CROP`, but in
   `SceneGeometry`, pure Java).
2. **Clock**: the time as text, size and position per scene, drawn with `Paint.setShader(BitmapShader(glass))`, so the
   digits show the glass picture. The glass picture is the scene blurred, stored at 480x270 and stretched over the whole
   picture, so the digits line up with the scenery behind them. A thin light outline (second pass, stroke) gives the
   edge. No blur is computed on the TV.
3. **Cut-out**: the scenery with a transparent sky, drawn over the bottom of the digits. Only the **band** the clock can
   touch is decoded (section 4); below the clock the background already shows the same pixels.

Checked on the files (copies, read only): where the cut-out is opaque, its pixels equal the background's, with no
difference over 24 levels in any of the four scenes; its first non-transparent row is 154 (Night), 364 (Golden),
491 (Fog) and 479 (Dawn). That is why drawing only the band is correct, and why the table in section 4 is small.

**Waterline** has no cut-out. The waterline sits at 55% of the picture height; the clock is drawn once above it and once
below it, mirrored (`scale(1, -1)` about the line), at 55% opacity with a vertical fade. No blur. It adds no bitmap.

**Header.** With Scenes on, the header `TextClock` and `ampm` become `INVISIBLE` (the header keeps its height; the date,
Hijri date and greeting stay). The big clock draws a small `AM` or `PM` after the digits on a 12 h TV. `clock_style` is
not used under Scenes. The sky and ambient views are hidden; no wallpaper bitmap is created and no dark veil is added
(each scene has its own contrast; section 8 lists the legibility risk).

**Clock text.** `SceneClockText` (pure): `HH:mm` or `h:mm` from the system 12/24 setting, formatted with `Locale.ROOT`,
then `DigitStyle.apply`, so Latin and Arabic-Indic digits follow the existing `digits` choice. Drawn with
`drawTextRun(..., isRtl=false)` so the digits never reorder on the Arabic screen. Refreshed on the minute boundary
(`ClockRefresh.delayToNextMinute`).

**Font.** See decision D1: a digits-only subset of Antonio (SIL OFL), or our Outfit ExtraLight. With `digits = arabic`
the clock uses `plex_arabic_light` (already in the APK). Antonio has no Arabic-Indic digits.

**RTL and Arabic.** The scene is a picture, so it is never mirrored. The view draws in absolute canvas coordinates, so
`layout-ar`, `layout-iw` and `layout` need no new view; the only change in them is none at all, because the view is
added from code and the two hidden clock ids exist in all three files. Clock position is a property of the picture, not
of the reading direction: Night, Golden, Dawn and Waterline are centred; Fog is start-aligned to the left in both
directions because its scenery leaves room on the left. A JVM test pins that the geometry does not depend on direction.

## 2. Scenes, rotation, follow the sun

- **Scenes and order** (`SceneList`, pure): `night`, `fog`, `golden`, `dawn`, `water` (the Waterline), in that fixed
  order. Order cannot be changed by the user (smaller UI; the order the plan set is also the priority).
- **On and off** per scene: `config.json` key `scenes` is a list of ids; the TV choice is stored as a comma string in the
  same key. Unknown ids are dropped, duplicates collapse, and an empty result means `night` alone, so there is always a
  scene to show.
- **Rotation** (`SceneRotation`, pure): `scene_seconds`, 5 to 600, default 20; the TV offers 10, 20, 30, 60, 120. The
  scene is `(epochMillis / (seconds*1000)) % count`, the same slot rule as `Wallpapers.indexAt`, so Home coming back
  mid-slot shows the same scene instead of restarting. A `Handler` posts to the next slot boundary; it stops in
  `onPause` and starts in `onResume`. With one enabled scene nothing is posted.
- **Follow the sun** (`SceneSun`, pure): `scene_follow_sun`, default false. With a valid place
  (`Interests.validPlace`), it uses `Prayer.times` for today at the system time zone and picks the scene by the windows
  below. Without a place it uses the clock. It re-evaluates each minute. Rotation is paused while it is on.

  | Window | Scene |
  |---|---|
  | fajr to sunrise + 45 min | `dawn` |
  | sunrise + 45 min to asr | `water` (Waterline), or `fog` if Waterline is off or not shipped |
  | asr to maghrib + 15 min | `golden` |
  | the rest | `night` |

  No place: 05:00 to 08:00 `dawn`, 08:00 to 16:00 day, 16:00 to 19:00 `golden`, else `night`. If the chosen scene is off,
  the nearest enabled scene in the fixed order is shown (ties go to the earlier one). Fog is a rotation scene only: there
  is no weather input offline and none is added.
- **Small memory** (`Config.smallMemory`): no cross-fade; the old scene is released first, then the next is decoded, so
  at most one scene is in memory. On any other TV the fade is 1,200 ms and only with `animations && !reducedMotion`; the
  previous scene is kept for the length of the fade. A decode that fails or runs out of memory keeps what is on screen;
  if nothing is on screen the plain `background_color` shows, like a refused wallpaper today.

## 3. Settings and keys

New `config.json` keys, all optional. Each is also a TV choice with the same name; the TV's choice wins, then
`config.json`, then the default. They survive a theme push, like `clock_style` (a row is added to the table of
`docs/CONFIG.md`, and to its Arabic twin).

| Key | Values | Default |
|---|---|---|
| `home_look` | `classic`, `scenes`, `oled` | `classic` (today's home, whatever `wallpaper_mode` says) |
| `scenes` | list of `night`, `fog`, `golden`, `dawn`, `water` | all that ship |
| `scene_seconds` | 5 to 600 (a bad value is clamped) | 20 |
| `scene_follow_sun` | `true` / `false` | `false` |
| `scene_scrim` | `off`, `auto`, `on` | `auto` |

**Top scrim** (`SceneScrim`, pure rule; drawn by `SceneView`): a thin top-down gradient (a `Paint` with a
`LinearGradient`, no asset) behind the header band only (greeting, date, Hijri date) and the first tile row. `on` always
draws it, `off` never, `auto` draws it only for scenes flagged `bright` in `SceneList` (Golden and Dawn to start; the flag
is data, changed after the TV check without new code). Its height is a fraction of the picture, computed in picture
space like the clock, and it is off for OLED and Classic.

*Launcher settings > Look* gets five rows: **Home look** (Classic, Scenes, OLED), **Scenes** (five on/off chips, one always stays on), **Seconds per scene**, **Follow the sun** and **Top shade**. They are always shown; the last four only matter while Scenes is selected, and their hints say so. Strings are added in English and Arabic; other languages fall back to English, as the memory line did in batch L. The credit sentence ("Scenes is inspired by couch-tv by Seagey (MIT).") is part of the hint of the Home look row; there is no separate About screen in the app.

**OLED** (`HomeLook`, pure): pure `#000000` background, no wallpaper, no sky, no ambient, no veil, header clock as in
Classic. It is a colour set, no asset and no burn-in shifting (a later idea, not here).

Existing choices are untouched: wallpaper modes, clock styles, dock styles, `digits` and `wallpaper_dim` work as today
under Classic; under Scenes and OLED the wallpaper keys are simply not used.

## 4. Assets and memory

### 4.1 Pipeline
- Sources: couch-tv's `scene-<id>.jpg`, `scene-<id>-fg.webp`, `scene-<id>-glass.jpg` at 1920x1080 (the 4K copies are not
  used), from the clone at commit `31b8f26f445aa023ff1df3cda660740a30a8a8a5`. Only picture files are copied.
- New `scenes/to-scenes.py` (Python 3 and Pillow, like `wallpapers/to-webp.py`, English, no personal path):
  background to WebP q80 (method 6), cut-out to WebP q75 with alpha (method 6), glass resized to 480x270 and saved as
  WebP; it also writes `scenes/manifest.json` with each file's SHA-256, size and, for each cut-out, the first
  non-transparent row. `--check` verifies the manifest and sizes and changes nothing.
- Only `scenes/<id>/{bg,glass,cut}.webp` go into the APK (`Sync` task like `syncWallpaperAssets`), as assets read by
  `AssetManager` directly: nothing is copied to `/data`, so no stale-copy problem.
- Quality is the measured starting point of the plan (copies, 2026-11-16); a scene that bands on a TV gets a higher number
  in the script.

### 4.2 Bytes (measured on copies, 1080p; the real APK is measured in the code phase)
| Scene | Background q80 | Cut-out q75 | Glass 480x270 | Total | As couch-tv ships it |
|---|---|---|---|---|---|
| Night | 118,376 | 98,814 | 5,302 | 222,492 | 459,183 |
| Fog | 93,572 | 77,798 | 2,872 | 174,242 | 352,181 |
| Golden | 105,214 | 79,438 | 3,986 | 188,638 | 409,762 |
| Dawn | 98,098 | 68,390 | 3,356 | 169,844 | 368,641 |
| Waterline | 237,398 | none | 6,506 | 243,904 | 452,602 |
| **All five** | | | | **999,120** | 2,042,369 |

| APK | Before (`main`, built here on 2026-11-16) | After (measured) | Cap |
|---|---|---|---|
| Offline | 1,306,038 B | 2,320,457 B (measured, local release build, debug key) | 4.2 MB |
| Online | 1,314,438 B | 2,328,293 B (measured, same build) | 4.2 MB |

Why the "before" is about 1.31 MB when v3.0.2 shipped at 3.89 MB: batch L (PR #42, already merged) re-encoded the 22
bundled wallpapers from JPEG to WebP and saved about 2.58 MB. Nothing went missing; this batch spends part of that room.

Both are measured on this branch (the estimate before the code was about 2.31 and 2.32 MB). The cap is met with about 1.9 MB to spare. Size of the pictures from `scenes/manifest.json`: the cut-outs and glass copies came out a little larger than the first measurement on copies (alpha at full quality, glass resized by `to-scenes.py`), 999,120 B against 992,306 B.

**Repository size** (decision D2): keeping the five source scenes as byte-identical masters adds 2,042,369 B to the git
history; the WebP outputs add 999,120 B. Total about 3.0 MB, next to the 3.2 MB of wallpaper JPEG masters already there.

### 4.3 Decoded memory, one scene at a time (1080p screen; RGB_565 is 2 B per pixel, ARGB_8888 is 4 B)
The band is the rows the clock box can touch, from the cut-out's first opaque row to the bottom of the clock box
(top + 0.82 x size). The clock numbers are start values, tuned on a TV, and live as data in `SceneList`.

| Scene | Clock top / size (px at 1920 wide) | Cut-out first row | Band rows | Cut-out band (ARGB) | Background (565) | Glass (565) | One scene | Whole cut-out, for comparison |
|---|---|---|---|---|---|---|---|---|
| Night | 150 / 400 | 154 | 154 to 478 = 324 | 2,488,320 | 4,147,200 | 259,200 | **6,894,720** | 7,111,680 |
| Fog | 172 / 470 | 491 | 491 to 557 = 66 | 506,880 | 4,147,200 | 259,200 | **4,913,280** | 4,523,520 |
| Golden | 152 / 390 | 364 | 364 to 472 = 108 | 829,440 | 4,147,200 | 259,200 | **5,235,840** | 5,498,880 |
| Dawn | 230 / 400 | 479 | 479 to 558 = 79 | 606,720 | 4,147,200 | 259,200 | **5,013,120** | 4,615,680 |
| Waterline | 268 / 420 (line at 594) | none | none | 0 | 4,147,200 | 259,200 | **4,406,400** | none |

- Worst single scene: Night, 6.9 MB, which is 2.7 MB more than a wallpaper today (4.1 MB).
- Worst fade pair (not on a small-memory TV): Night plus Golden, 12.1 MB.
- On a small-memory TV one scene is in memory at a time, so the worst case is 6.9 MB.
- Decoding only the band means `BitmapRegionDecoder` on the asset stream (platform class, WebP supported at minSdk 26).
  If a band is empty (the clock box ends above the first opaque row) nothing is decoded.
- A screen that is not 1920x1080 scales the 1080p pictures with the cover matrix; a 4K TV costs no more memory.
- The 20-round-trip settings test of the 3.1 plan must still return to its start values; that test is a device test.

## 5. Definition of done, per item
**S1 Assets and pipeline**
- `scenes/<id>/` has `bg.webp`, `glass.webp` (and `cut.webp` for four scenes) for every shipped scene.
- `SceneAssetsTest` reads each WebP header: background 1920x1080, glass 480x270, cut-out 1920x1080 with the alpha flag.
- The first-row numbers in `SceneList` equal `scenes/manifest.json`; `to-scenes.py --check` exits 0.
- Real offline and online APK bytes are in section 4.2 and are at or under 4.2 MB.

**S2 Scene list, rotation, small memory**
- `SceneListTest`, `SceneRotationTest` pass with their failure cases (section 6).
- On a small TV `SceneFade.useFade` is false whatever `animations` says (a test).
- Scenes off in `scenes` never show; an empty list shows Night.

**S3 Drawing**
- `SceneGeometryTest` covers the cover matrix for 1920x1080, 1280x720, 3840x2160 and a 4:3 screen, the band rows and byte
  counts of the table above, and an empty band.
- The clock text for 12 h and 24 h, midnight, and `digits = arabic` is pinned (`SceneClockTextTest`).
- The unit tests are the proof for the maths; the picture itself is a device check (section 9).
- `SceneScrimTest`: `auto` is on only for scenes flagged bright, `on` and `off` ignore the flag, an unknown value acts as
  `auto`, the scrim height is clamped to the picture, and it is never drawn for OLED or Classic.

**S4 Follow the sun**
- With a valid place, the scene follows the table in section 2; at exact window edges the later window wins (a test).
- With no place, a bad place or a polar value, the clock rule is used and nothing throws.
- A scene that is off is replaced by the nearest enabled one; Waterline off gives Fog by day.

**S5 OLED and settings**
- `HomeLookTest`: `oled` hides sky, ambient, veil and wallpaper; `classic` changes nothing from today.
- The precedence TV choice, then config, then default holds for the four new keys (a test per layer); a bad value falls
  back to the default.
- `docs/CONFIG.md` and its Arabic twin list the four keys and the three-way rule.

**S6 Credits and font**
- `NOTICE.md` (and `NOTICE.ar.md`) credit couch-tv with the full MIT text and the line "Copyright (c) 2026 Seagey", and
  say what was taken (the five scene pictures, re-encoded) and what was not (code, CSS, text, fonts).
- One credit line in `README.md` (English and Arabic parts), and one sentence in the hint of the *Home look* row of Launcher settings.
- If D1 is Antonio: the OFL text and the source commit and SHA-256 are in `NOTICE.md`, the subset is digits and `:` only.

**S7 Docs the study found missing** (separate commits)
- `docs/UNINSTALL.md` and `.ar.md`: built only from what `docs/INSTALL.md` already says about rollback (lines 97-103, 156,
  197); a step that no device proved is marked NOT TESTED.
- A short "What we leave on, and why" section: the facts from the scripts (`Set-TvhomeAsHome.ps1`, `Apply-TvhomeKit.ps1`).

## 6. Failure cases for the JVM tests
- `SceneList`: unknown id, duplicate id, empty list, only unknown ids, case differences, a non-string entry, a comma
  string with spaces and empty parts.
- `SceneRotation`: seconds 0, negative, 3, 601 and `Integer.MAX_VALUE` clamp; count 0 gives -1; the slot edge (the last
  millisecond of a slot and the first of the next); negative time; the wait to the next slot is always 1 to
  seconds x 1000 ms.
- `SceneSun`: `NaN`, `0, 0` and a latitude of 80 give "no place"; `NaN` prayer times give the clock rule; now equal to
  sunrise, to asr and to the end of night; all scenes off except one gives that one; Waterline off gives Fog by day;
  only Night on always gives Night.
- `SceneGeometry`: a clock box that ends above the first opaque row gives no band; a band is clamped to the picture;
  zero or negative screen sizes do not divide by zero; the band bytes equal rows x width x 4.
- `SceneFade`: small memory gives no fade even with `animations` on and `reducedMotion` off.
- `SceneClockText`: 00:05 on 12 h shows `12:05` and `AM`; 12:00 shows `PM`; Arabic digits map every digit; an unknown
  digit style acts as Latin; a null time zone does not throw.
- `HomeLook` and the four keys: an unknown look, a string where a boolean is expected, and a TV choice beating a config
  value, in both directions.

## 7. Credits and licence
- couch-tv is MIT, `Copyright (c) 2026 Seagey`; its scene pictures are covered by its `CREDITS.md`: "Use them however you
  like." The repository licence and that line are quoted in `NOTICE.md`. The pictures were made for couch-tv from text
  only; their copyright status beyond that line is not verified, and the note says so.
- Not taken: any code, CSS, text or font from couch-tv, and the Classic photos (Creative Commons share-alike).
- The look is named Scenes, not by a developer's name or a brand name, so nothing implies that Seagey maintains or
  endorses it.
- Our own wallpaper sets keep our own pictures. The Scenes pictures are used only by the Scenes look.

## 8. Risks
- **Legibility.** A 400 px clock sits behind the header and the first row of tiles. The tiles have their own plates, but
  contrast on a bright scene is a device question, not a calculation. The optional top scrim (`scene_scrim`, section 3)
  is built in, so a bright scene is rescued by changing a flag, not by a second code round after the TV check.
- **Memory** (section 4.3): 6.9 MB for Night is the largest; if the 20-round-trip test or a 1.9 GB TV shows pressure, the
  fallbacks are a smaller band (a smaller clock) or Night's cut-out at q60.
- **Picture quality.** WebP banding on soft gradients shows first on a TV; the quality is one number per file kind.
- **Waterline's mirrored clock** is the part most likely to look wrong. It is last in the order and the first to cut.
- **Region decoding** of a WebP asset stream is documented platform behaviour, but not run on a device here.

## 9. NOT TESTED without a device
- Any pixel of the Scenes look on a TV: alignment of the glass fill with the scenery, the digits behind the header and
  the tiles, the 55% waterline, Fog's left position, the Arabic and Hebrew layouts.
- Decode time and memory of the cut-out band on the Chromecast, the 32-bit TCL and the 1.9 GB Android 12 TV; the
  20-round-trip test with Scenes on; cold start.
- Banding of the re-encoded scenes; 12/24 h on each TV; Arabic-Indic digits in the chosen font.
- Rotation across a real hour, follow the sun at a real sunrise, and the uninstall steps of S7.

## 10. Decisions for the maintainer

Decided on 2026-11-16 (the maintainer chose D1-A in this session; D2-A and D3-A were relayed from the advisor session with the maintainer's hand-over of the lettered choices):

**D1. Clock font: A, decided.** A digits-only subset of Antonio Light (SIL OFL 1.1) from the official `google/fonts`
repository at commit `95f4904fc8bcf26d3420fe315560c96417c6dec7` (`ofl/antonio/Antonio[wght].ttf`, 74,104 B, SHA-256 in
`NOTICE.md`), made by `scenes/make-clock-font.py` (weight fixed at 300, digits 0 to 9 and the colon only). The result,
`res/font/antonio_digits.ttf`, is 2,900 B, under the 8 KB limit set for option A. The OFL text is
`launcher/FONT-LICENSE-ANTONIO-OFL.txt`. With `digits = arabic` the clock uses `plex_arabic_light`, as designed.
Option B (Outfit ExtraLight) stays as the fallback if the digits look wrong on a TV.

**D2. Source scenes in the repository: A, decided.** The byte-identical source pictures are in `scenes/master/` with
their SHA-256 in `scenes/manifest.json` (+2,042,369 B in git history).

**D3. Waterline: A, decided.** It ships with the others behind its own on/off, is last in the order, and is the first to
cut if the TV check shows the mirrored clock looks wrong.

## 11. Order of work after the "ok"
1. `scenes/` pipeline and assets, with the credit in `NOTICE.md` in the same commit as the first picture.
2. Pure classes and their tests (`SceneList`, `SceneRotation`, `SceneSun`, `SceneGeometry`, `SceneFade`, `SceneClockText`,
   `HomeLook`).
3. `SceneView`, wiring in `MainActivity`, the settings group, strings in English and Arabic.
4. Docs: `CONFIG`, `NOTICE`, README (EN and AR), CHANGELOG entry, then `UNINSTALL` in its own commit.
5. Local checks as CI runs them, both flavors built into a folder outside the repo, then bytes in section 4.2.

## 12. Progress

| Item | State | Evidence |
|---|---|---|
| Design note and decisions D1 to D3 | done | this note |
| S1 assets and pipeline | done | `scenes/to-scenes.py --check` OK, `SceneAssetsTest`; 999,120 B of pictures |
| S2 list, rotation, small memory | done | `SceneListTest`, `SceneRotationTest`, `SceneFadeTest`, `SceneSettingsPrecedenceTest` |
| S3 drawing (`SceneView`, geometry, clock text, scrim) | code and maths tests done; the picture is NOT TESTED on a TV | `SceneGeometryTest`, `SceneClockTextTest`, `SceneScrimTest` |
| S4 follow the sun | done | `SceneSunTest` |
| S5 OLED and settings | done | `HomeLookTest`, precedence tests, `docs/CONFIG.md` and its twin |
| S6 credits and font | done | `NOTICE.md` and twin, `README.md`, 2,900 B font |
| S7 docs | done | `docs/UNINSTALL.md` and twin, with NOT TESTED lines |
| Checks | 382 unit tests per flavor, 0 failures; lint 0 errors; script tests 24 passed; hygiene OK | local run, 2026-11-16 |
| APK | offline 2,320,457 B, online 2,328,293 B | section 4.2 |
| TV check, push and PR | not started | needs a TV; push waits for the maintainer's word |
