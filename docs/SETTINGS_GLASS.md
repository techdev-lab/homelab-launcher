# Glass settings: design note

[English](SETTINGS_GLASS.md) · [العربية](SETTINGS_GLASS.ar.md)

Status: **PHASE 1, IMPLEMENTED** (written before the code; tried on the Chromecast 4K in the eight combinations below, remote only). Launcher settings looked like another app: a flat dark list of 14 rows
and grey chips, while the home screen is glass. This note redesigns the screen only. No new option, no new permission, no
new key in `config.json` and nothing new saved in the TV's choices: every chip writes exactly what it writes today.

A phase 2 (a live preview of the home screen beside the settings) is out of scope and needs its own approval.

## What the screen is today

One long column: a title, then each row is a white heading, grey chips under it and a hint paragraph. Colours,
wallpapers and the dock position are all plain text chips, so the viewer cannot see what a choice does before pressing
it. Focus and the chosen chip are two different flat greys and a pale green.

## The design

### 1. Four section cards

| Section | Rows |
|---|---|
| Look | Language, Wallpapers (three sets), Plain colour, Wallpaper dim, Digits, Clock style |
| Dock and cards | Dock position, Dock style, Tile corners, Apps per row, Text size, Cards on the home screen, Favourite clubs (online build only), Hidden apps |
| Prayer and Ramadan | Prayer method, Asr, Prayer alert, Hijri date |
| Motion | Motion |

The three buttons that are not preferences (Use the theme's layout, Default home app, TV settings) stay in a footer row
under the card, reachable from every section by Down. They are not a fifth section.

Only one card is on screen at a time, in the same glass as the home screen's cards (dark glass, a hairline edge, 20 dp
corners). Above it a strip of four section titles, each with a small icon, shows where you are; the strip also takes
focus, so a section is one or two presses away.

### 2. Show what a choice does

- **Wallpapers**: each set shows a thumbnail of its first picture, taken from the APK itself. The first draft shrank the
  pictures at build time into new files; decoding the existing asset at a quarter of its size (`inSampleSize`, about
  240 by 135 pixels, three of them, on a background thread) adds **zero bytes** to the APK, which is what the council's
  150 KB limit was for. If a thumbnail cannot be read the chip is the plain text chip it is today.
- **Plain colours**: the chip is the colour. (Already so; it gets the new selected and focus look.)
- **Dock position**: a small drawing of the screen with the dock at the bottom, or as a column at the end of the reading
  side. It is **mirrored in Arabic** (the column is on the left), like the real screen. The drawing is decided by a pure
  function, so a test checks the mirror.
- **Digits**: the chip shows a sample, `123` and the Arabic-Indic one.

### 3. Selected and focused

- **Selected**: a filled base in the accent green at low alpha plus a 2 dp accent border (today's green, nothing new).
- **Focused**: the glass ring of the home screen's highlight: a light sheen fill and a bright 2 dp rim.
- A chip that is both shows the ring over the green base, so the chosen value is visible while moving over it.
- The colour chips keep their colour under both states.

### 4. One explanation line

The hint paragraph under every row is gone. At the bottom of the card there is one line (two when it must) that shows the
hint of the row that has focus and changes as focus moves. The texts are today's hints; Language and Apps per row, which
had none, get a short one.

## Navigation

Pure Java, no Android types, in `SettingsNav`, unit tested on the JVM like `SideNav` and `GridNavigation`.

The screen is a stack of **zones**: the section strip, the rows of the visible section, and the footer. A position is
`(section, row, chip)`; chips are numbered in **reading order**, so the first chip is on the right on the Arabic screen
and Left and Right swap (`rtl`), as everywhere else in the launcher.

| Key | Rule |
|---|---|
| Up | Row to the row above (the chip index is kept, or clamped to the shorter row). The first row goes to its section's strip tab. The tab stays. The footer goes to the last row of the visible section (to the tab when it has no rows). |
| Down | The tab goes to the first chip of the first row (or to the footer when the section has no rows). A row goes to the row below; the last row goes to the footer. The footer stays. |
| Toward the end (Right on English, Left on Arabic) | The next chip. At the last chip of a row: the next section, same row index (clamped), first chip. In the last section: stays. On the strip: the next tab. In the footer: the next button, and it stays at the last. |
| Toward the start | The mirror image: the previous chip; at the first chip, the previous section's last chip; stays in the first section. |

Rules that make it safe, each with a test that fails without it:

- **No wrap and no jump.** An edge keeps the focus where it is; it never wraps and never lands on a view far away.
- **A one-chip row** crosses to the next section in both directions at once (it is the first and the last chip).
- **A section with no rows** (a build or a future change can empty one) is entered at its tab and passed through, never a dead end.
- **Arabic**: the same positions with the two horizontal keys swapped.
- **A position outside the model** (a stale position after a section changed) is clamped to a real one, never an
  exception, so focus can always be found again.
- **Every position reaches the strip and the footer by Up and Down presses alone**, and the strip reaches every section
  by one horizontal key per section. A test walks all positions and checks it, as `SideNavTest` does for the dock.

### Polish after the first TV test (2026-10-31)

Three cosmetic findings from the first test on the TV, fixed without changing what the model answers:

1. **Crossing a section needs a second press.** The model still answers the same (the end of a row goes to the next
   section), but the Activity asks `SettingsNav.crossesSection` first: a Left or Right that would cross from a row to
   another section only arms on the first press, shows "Press again to open <section>" in the explanation line, and
   crosses on a second press of the same key within 1.5 seconds (`EdgeBump`, pure Java, tested). A key held down never
   crosses, and any other key disarms it. The section tabs stay the fast way across. Moves inside a row, Up, Down and the
   tabs are unchanged.
2. **The focus ring is never clipped.** The focused chip grows by at most about 5 dp whatever its width (a wide one-chip row
   used to grow 4% of its width and hit the card edge), and the scroll area is wider than the content by 8 dp on each side.
3. **Long sections scroll by whole rows.** The scroll area shows whole rows only (`RowWindow`, pure Java, tested): the
   focused row decides the first row shown, no row is cut at the top or bottom, and a small arrow under the card says
   there are more rows below (or, at the end, above). Focus moves exactly as before; only where the rows sit changes.

The Activity feeds the model the chips that are actually visible (the offline build has no clubs and no weather or
football cards) and moves focus itself. If the model has no view for a position, the key falls through to Android's own
focus search, so the old behaviour is the fallback and the screen cannot be locked. Back is not touched: it closes the
screen and Home is under it. The keys that open dialogs and screens (prayer method, clubs, hidden apps, the Lively motion
warning) behave as today, and focus returns to the chip that opened them.

## What does not change

- Every chip saves the same key with the same value as now; nothing new in `config.json` or the TV's choices.
- No permission, no network call, no new font, no new dependency, no new image file.
- `chip()` and `fill()` carry the mirroring logic for the old rows; the new code keeps their rule (views added in reading
  order, reversed on Arabic and Hebrew screens) and the tests cover it through the navigation model.
- The version stays 3.0.0 in this change.

## Strings

New: four section titles and the two missing hints, in `values` and `values-ar` (`ResourcesTest` already fails if the
Arabic one is missing). The old hints are reused. Other locales fall back to English as before.

## The four sections on the TV

Taken on a Chromecast with Google TV 4K, with the demo city (Riyadh) and the remote only. Each screenshot has the focus on
the first choice of its section.

| Look | Dock and cards |
|---|---|
| ![Look, English](screenshots/settings-glass-look-english.png) | ![Dock and cards, English](screenshots/settings-glass-dock-and-cards-english.png) |
| **Prayer and Ramadan** | **Motion** |
| ![Prayer and Ramadan, English](screenshots/settings-glass-prayer-and-ramadan-english.png) | ![Motion, English](screenshots/settings-glass-motion-english.png) |

The same four on the Arabic screen (the tabs, rows and the dock sketch are mirrored):

| Look | Dock and cards |
|---|---|
| ![Look, Arabic](screenshots/settings-glass-look-arabic.png) | ![Dock and cards, Arabic](screenshots/settings-glass-dock-and-cards-arabic.png) |
| **Prayer and Ramadan** | **Motion** |
| ![Prayer and Ramadan, Arabic](screenshots/settings-glass-prayer-and-ramadan-arabic.png) | ![Motion, Arabic](screenshots/settings-glass-motion-arabic.png) |

## Conditions (from the council, 2026-10-31)

1. This note first, in English with an Arabic twin.
2. The navigation is a pure class with a failure test per rule (section edges, Arabic order, a one-chip row).
3. Eight combinations on the Chromecast 4K with the remote only: English and Arabic, dock at the bottom and at the side, small
   and large strip text; and Back returns to Home. If one fails, the redesign is not merged.
4. No later phase without a new approval.

## Test plan

JVM, written first and shown failing: `SettingsNavTest` (every rule above, and `crossesSection`), `EdgeBumpTest`, `RowWindowTest`, `DockSketchTest` (bottom and side, mirrored
in Arabic, cells inside the frame and not overlapping), the new strings in `ResourcesTest`. Then lint and both flavors.
On the TV: the eight combinations, remote only, and every chip still saves what it did.
