# Look options (design note)

Status: **IMPLEMENTED in the unreleased build, waiting for the TV test.** Written before any code, as `docs/DESIGN.md` asks. It
describes three small settings that let a viewer choose between today's look and a more "glass" look, on the TV, from Launcher
settings. Decisions taken: option 3 is the glass clock (not the clock size), built last and to be dropped if the Arabic or
English screenshots show any shift; the order is 1, 2, 3.

## Goal

Some households like the flat dark home screen of 2.9, others prefer a lighter, floating, glass look. Both should be one
choice away on the TV, and **nobody who does nothing sees any change**.

## Rules every option must follow

- The default of every option is today's look: same sizes, same positions, same colours. After an upgrade a TV that was
  not touched looks exactly as before. This is checked with before and after screenshots of the same TV.
- No new permission, no new dependency. The offline flavor stays without INTERNET.
- The strip stays one row and the dock stays fully visible on a 1080p TV (a second row pushed the dock off the screen in
  2.9.0, see `StripSize`).
- Focus does not change: dock, strip cards and top bar are reached with the same keys as today.
- Where the choice lives: the same pattern as `text_size`. The TV's own choice wins, then a `config.json` key, then the
  default. An unknown value counts as unset and is ignored. A theme push does not clear the choice made on the TV.
- Every option has a failure-case test (a bad value falls back to the default) and a JVM test for its pure logic.

## What the home screen draws today

- A pinned dock row. `DockDecoration` paints a flat lighter band (radius 26 dp, fill `#141416`, one hairline) behind it.
- One glass highlight (`GlassView`, radius 20 dp) that springs between tiles, and the name of the focused app above the dock.
- App tiles with a 16 dp corner (`tile_bg.xml`). Interest cards use the tinted glass colours of `Glass`.
- A header clock of 52 sp (`TextClock`) with the date lines beside it. The header uses `gravity="left"` on purpose so Arabic
  stays under the clock (see the comment in `activity_main.xml`).

## The three options

| # | Key | Values (default first) | What changes | Main touch points |
|---|---|---|---|---|
| 1 | `dock_style` | `band`, `floating` | `floating` replaces the flat band with a rounded glass slab that floats a little above the bottom edge: translucent fill tinted with the focused app's colour (the `Glass` arithmetic), a brighter hairline, a larger radius. No blur, so weak GPUs are not affected. | `DockDecoration`, a new pure class `DockStyle` (`pick`, `valid`, geometry), `Config`, `LauncherSettingsActivity` and its layout, `MainActivity` (pass the style, redraw on change), strings |
| 2 | `tile_corners` | `normal`, `soft` | `soft` rounds the tiles more (16 dp to 24 dp). The highlight and the dock radius follow, so the three shapes stay concentric. | `AppAdapter` (card background), `GlassView`, `DockDecoration`, a new pure class `TileShape` (radius table), settings, strings |
| 3 | `clock_style` | `plain`, `glass` | `glass` puts the time on a small translucent rounded capsule. The text, its size and the date lines do not move. | header in `activity_main.xml` (and the `-ar` and `-iw` copies), `MainActivity`, a new pure class `ClockStyle`, settings, strings |

Option 3 touches the header, which is the most fragile layout for right-to-left text. It is built **last**, and dropped if the
Arabic and English screenshots show any shift. A safer variant, if wanted instead: `clock_size` (`small`, `medium`, `large`
scales of the header clock), which only changes a text size.

## Tests

JVM (run on every change, both flavors): `DockStyleTest`, `TileShapeTest`, `ClockStyleTest` cover the value tables, the
fallback of an unknown value, the order TV choice then `config.json` then default, and the geometry of the floating slab
(inside the screen, never over the dock tiles). `ResourcesTest` already fails when an English string has no Arabic one.

On the TV (one device step at a time, each with the owner's typed confirmation, the remote in hand):
1. Screenshot of the current build, then install the candidate over it (`install -r`, same signing key).
2. Screenshot of the default look: it must match step 1.
3. Each option, in Arabic and in English, with a screenshot; walk dock, strip and top bar with the D-pad.
4. Back to the defaults.

## Rollback

`install -r` of the frozen 2.9.0 APK. The three keys are ignored by older builds. Picking the first value of each option again
(Band, Normal, Plain) restores today's look; *Follow theme* does not touch these choices, like the text size.

## Inspiration and credit

The glass dock and glass clock looks are inspired by the public description of
[tvOS Launcher](https://github.com/techgenius1/tvOS-Launcher) (Apache-2.0) by techgenius. Only its README was read. No code,
image or wallpaper is copied: the drawing is our own and uses our `Glass` colour arithmetic. `NOTICE.md` and the README carry a
thank-you line. If a later change does copy code from it, that file keeps the Apache-2.0 header and the licence text, and
`NOTICE.md` says what was changed.

## Out of scope

Depth wallpapers (their images have no declared licence), a control panel, a recent-apps switcher, profiles, an icon changer, a
button mapper, any extra permission (device admin, secure settings, notification listener), and any self-update.

## Decisions

1. Option 3: the glass clock (decided by the owner), with `clock_size` kept as the fallback if it shifts the header.
2. The visual comparison with the reference launcher was not done (its page did not open in the in-app browser); the options
   are judged on our own screenshots instead.
3. The order of the options inside the batch: 1, then 2, then 3.

## What was built

`DockStyle`, `TileShape` and `ClockStyle` (pure classes, 16 JVM tests), `DockDecoration` (band or floating plate),
`AppAdapter.cornerRadius` (soft corners), `MainActivity` (clock capsule, ambient colour for the plate, highlight radius),
`Config` (three keys), three rows in Launcher settings with English and Arabic strings. The defaults draw with exactly the
numbers of 2.9 (`DockStyleTest.bandKeepsTodaysNumbers`, `TileShapeTest.normalIsExactlyTodaysRadius`).
