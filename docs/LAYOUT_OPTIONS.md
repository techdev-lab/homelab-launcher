# Layout options: batch 5 design note

Status: written before the code, as the other batches. Every default is today's look, so an upgrade changes nothing
until the viewer picks.

Three options in Launcher settings, each also a key in `config.json`. As for every look option, the TV's choice wins
over the theme, a theme push does not clear it, and an unknown or out-of-range value counts as unset.

| Option | Key | Values | Default |
|---|---|---|---|
| Digits | `digits` | `latin`, `arabic` | `latin` |
| Wallpaper dim | `wallpaper_dim` | whole percent, 0 to 80 (chips: 15, 35, 55, 75) | 35 |
| Dock position | `dock_position` | `bottom`, `side` | `bottom` |

The ideas of a dock at the side, a dimmed wallpaper and Arabic digits came from looking at other launchers (see the
logbook and NOTICE.md); the code is ours.

## 1. Digits (`digits`)

`arabic` shows every number on the home screen in Arabic-Indic digits (U+0660 to U+0669): the clock, the Gregorian and Hijri
dates, and the numbers on the strip cards (prayer times, countdowns, kick-off times, scores, temperatures). The Arabic
screen already writes the Hijri date that way; this makes the rest follow when the viewer wants it, and gives the same
choice on the English screen.

- It changes how a number is drawn, never the number: a `TransformationMethod` on the text views, so the clock keeps
  formatting itself and the strip cards build their text as before. The mapping is the existing `Hijri.arabicDigits`.
- The screensaver clock follows the same choice.
- The English screen draws the clock in Outfit, which has no Arabic-Indic digits, so Android supplies them from its own
  font. This is noted in the setting's hint.
- Settings screens, dialogs and the top-bar pills are not changed.

## 2. Wallpaper dim (`wallpaper_dim`)

A wallpaper picture already sits under a dark veil (35% black) so the clock and the cards keep their contrast. The veil
becomes a setting: 15 for bright pictures to show through, 55 or 75 for a calmer screen. The default is the current 35,
so nothing moves. It only exists where a picture is shown; the plain colours and a TV with no wallpaper have no veil.

## 3. Dock position (`dock_position`)

`side` puts the dock in a column at the end of the reading side, under the top-bar buttons: on the right on the
English screen and on the left on the Arabic one, which is mirrored like the rest of that layout (the header, the strip
and the app grid). The other apps are in a grid beside it, and in both languages the end of a row is the one next to the
dock. (The first draft called the value `right`; it is `side` because it is not on the right in Arabic.) `bottom` is
today's layout and the default.

### Why this is not just a style

The dock is today the first row of the one app grid: `RecyclerView` positions 0 to `COLUMNS - 1`. The plate behind it,
the focused app's name above it, the glass highlight, the arrow-key logic, `rows_below_dock` and the number of strip
places all rest on that. A column cannot be made from a row by styling, so the side layout is a second list.
`bottom` keeps the code path it has today.

### Design

- **One data set, two lists.** `AppAdapter` stays the owner of the tiles. In the side layout two thin adapters show
  two slices of it: positions 0 to `COLUMNS - 1` (the dock, empty slots included, as today) in a vertical list on the
  right, and the rest in a grid on the left. Both bind and create tiles through `AppAdapter`, so a tile looks and
  behaves as it does now. Positions seen by the rest of `MainActivity` stay absolute.
- **Sizes.** The column is 108 dp of tile plus the plate padding. It runs from the top-bar buttons down to the bottom
  margin and scrolls when there are more dock apps than fit (about four to five on 1080p). The grid has
  `max(3, COLUMNS - 1)` columns, and the strip has one place fewer than its text size gives (`StripSize.perRowSide`),
  because both are narrower.
- **Plate.** A vertical version of `DockDecoration` draws the same two styles (band, floating glass) behind the
  column, with the same corner rule.
- **Name of the focused dock app.** There is no free band above a column, so the name goes in a small glass pill to the
  left of the focused tile, drawn over the grid for as long as the tile has focus. The glow in the app's colour works as
  today.
- **Glass highlight.** It follows the tile in whichever list it is, and the scroll of both lists.
- **Arrow keys** are decided by pure functions (`SideNav`) so they are tested on the JVM:
  - Dock: Up on the first tile goes to the top-bar buttons; Down on the last filled tile stays; Left goes to the grid
    tile nearest in height; Right stays.
  - Grid: Left at the first column stays; Right at the last column of a row goes to the nearest dock tile in height;
    Up on the first row goes to the strip, or the top bar when the strip is empty; Down on the last row stays.
  - Strip: Down from the last strip row goes to the grid, or to the dock when there is no grid.
  - From the top-bar buttons, Down goes to the strip, else the grid, else the dock. Settings stays reachable from every
    tile in at most three key presses.
- **Mirrored on the Arabic screen.** The owner decided, after seeing the first version on the TV, that the Arabic
  screen puts the dock on the left, the mirror image of English, as the Arabic header, strip and grid already are. The
  navigation functions take that as a flag (`rtl`), the margins and the name pill follow it, and the keys toward the dock
  are Right on English and Left on Arabic.
- **No new permission, no network.**

### Conditions (from the council, 2026-10-31)

1. The default is `bottom` and nothing changes for anyone who does not choose.
2. The top-bar buttons stay reachable by arrow keys in every layout: a JVM test for the navigation functions.
3. Eight combinations are tried on the Chromecast 4K with the remote: English and Arabic, band and floating dock, small
   and large strip text, and the lowest and highest column counts. If one fails, the dock position is left out of the
   release (it moves to 2.11) and nothing else in this batch is held back.
4. Tried first on the TV after a backup, and installed only with the owner's word.

## Test plan

JVM, written first and shown failing: `DigitStyleTest`, `WallpaperDimTest`, `DockPositionTest`, `SideNavTest` (every edge
of both lists, an empty grid, one dock app, more dock apps than fit), `StripSize.perRowSide`, and the settings and
config rules for the three keys. Then lint and both flavors. On the TV: the settings chips, the digits on both
screens, the veil at 15 and 75, and the eight dock combinations.
