# Batch L: lighter on small TVs (design note)

[English](BATCH_L_LIGHT.md) · [العربية](BATCH_L_LIGHT.ar.md)

Status: implemented on the `feat/batch-l-light` branch (D1-a chosen by Ahmad); the measured numbers are in section 6. Four items: a small-memory default for the heavy effects (L1), a check that R8 and
resource shrinking really apply (L2), the bundled wallpapers re-encoded to WebP (L2b) and a size and memory table in the
release notes (L3). Nothing here adds a dependency, a permission or a service, and no rule uses a device name: the gate
asks the system how much memory the TV has. The line numbers are those of `main` at `47c07d0`.

## 0. What the code already does (read before the plan)

- `advanced_effects` is the "Lively" motion choice: lifted tile, icon zoom, smooth colour transitions (`AppAdapter`). Its
  default is already `false` (`res/values/config.xml:80`), so on a fresh install nothing heavy runs in the tiles.
- There is **no glass blur** in the app: `Glass.java` and `GlassView.java` use plain colour arithmetic and translucency.
  The first blurred picture will come with the Scenes batch, which must read the gate of L1 (see section 1.5).
- The one moving wallpaper effect is the 700 ms cross-fade when the picture changes (`MainActivity.showWallpaper`,
  `animations && !reducedMotion`). It composes two full-screen layers for the length of the fade.
- Wallpapers are decoded once at screen size in RGB_565 (`showWallpaper`, `WallpaperSize`), so a decoded picture costs
  the same whatever its file format is. The format only changes the APK size and the one-off decode time.

So L1 changes little today on its own: it is the rule the heavier work that follows (Scenes) will rest on. Section 1.4
says exactly what it changes now.

## 1. L1: small-memory gate

### 1.1 Definition of done
- `SmallMemory` (pure Java, no Android import) decides "small" from two inputs and has JVM tests; the Android calls are
  a few lines in `Config.load`; the read-only memory line of D1-a (or the fade of D1-b) is the only UI change.
- A TV with 2 GB and no choice gets the effects off; a choice made on the TV, then a key in `config.json`, always wins
  over the gate (a test per layer).
- A TV with 4 GB behaves exactly as today (a test), and a zero, negative or missing `totalMem` falls back to "not small"
  unless `isLowRamDevice()` says so (a test).
- `docs/CONFIG.md` (and the Arabic twin) say what the default is and how to turn the effects on.

### 1.2 The threshold and why
Small = `ActivityManager.isLowRamDevice()` **or** `MemoryInfo.totalMem` at or under **2,200 MiB** (2,306,867,200 bytes).
- `isLowRamDevice()` alone is not enough: the Chromecast with Google TV 4K has 2 GB, an empty `ro.config.low_ram` and
  reports normal, so the flag is false there.
- `totalMem` is what the kernel hands to Android, below the physical RAM (a "2 GB" TV reports well under 2,048 MiB; a
  "3 GB" one reports about 2.6 GB or more). 2,200 MiB therefore catches the 2 GB class (and the 1 GB class) and leaves
  3 GB and larger untouched, with room on both sides. One measured data point exists: another 2 GB-class TV (Android TV 12, 32-bit) reports `MemTotal` 1,984,396 kB, about
  1,938 MiB, with `ro.config.low_ram` unset, so it is under the threshold with 262 MiB to spare and `isLowRamDevice()`
  is false there too. **The real `totalMem` of the Chromecast is not measured yet**: it is
  read once, read-only, in the device phase (`cat /proc/meminfo`, `MemTotal`) and written into
  `docs/TESTED_DEVICES.md`; if it is above 2,200 MiB the threshold is revisited before release, not tuned to fit.

### 1.3 Precedence (the same rule as every other setting)
1. A choice made on the TV (`choices` preferences, key `advanced_effects`) wins.
2. Otherwise a key in `config.json` (`advanced_effects`) wins.
3. Otherwise the gate: small gives `false`; not small gives the resource default (`R.bool.advanced_effects`, `false`).
The "choice" test is "the key is present", not "the value differs from the default", so a user who picked Calm or Lively
is never overridden. The resolution is one pure method, `SmallMemory.resolve(Boolean tv, Boolean config, boolean small,
boolean resourceDefault)`; `Config.java:195`, `:222` and `:239` become its callers.

### 1.4 What the gate touches now
| Setting | Key in `Config.java` | Effect of the gate |
|---|---|---|
| Lively tile effects | `advanced_effects` (`:195`, `:222`, `:239`) | default false on a small TV; no visible change today, because the resource default is already false (it protects the default if a later batch flips it) |
| Wallpaper cross-fade | `animations`, `reduced_motion` (`MainActivity.showWallpaper`) | **only under D1-b:** on a small TV the fade shows only when Lively is on (`SmallMemory.wallpaperFade`); on any other TV nothing changes |
| For the Scenes batch | new field `Config.smallMemory` | the blurred glass picture and the cross-fade of scenes read it; no scene code is written here |
Nothing else is gated: the `animations` setting itself (tile focus scale) keeps its default, so a small TV still feels
alive; only the full-screen work is traded away. The user can turn Lively on in Launcher settings > Motion.

**Decision for Ahmad (D1).** Two options; Ahmad chooses.
- **D1-a (chosen by Ahmad on 2026-11-16):** ship L1 as the pure rule plus `Config.smallMemory` and **no visible
  change**: the cross-fade stays as it is, because it works on the 2 GB Chromecast with no measured problem and the 3.1
  rule is "measure first, change only if it helps". To make the gate checkable on a TV without changing behaviour, one
  read-only line is added to the explanation line of the Motion row in Launcher settings, for example "Memory: small (1938 MiB)" (EN and AR strings;
  it reads `SmallMemory` and shows the class and the value read). The Scenes batch is the first real consumer.
- **D1-b (not chosen, not built):** also apply the fade rule above (`SmallMemory.wallpaperFade`): on a small TV the 700 ms fade shows only when
  Lively is on. Visible on the Chromecast; not measured to help.
The sections below list the tests of both; only the fade tests drop out under D1-a.

### 1.5 Failure cases (JVM tests, `SmallMemoryTest`)
2 GB device and no choice: effects off. `lowRam` true with 8 GB: small. TV choice on beats the gate; TV choice off beats
a config that says on; config on beats the gate; 4 GB and no choice: the resource default; `totalMem` 0, negative or
`Long.MAX_VALUE` with `lowRam` false: not small; exactly 2,200 MiB: small; one byte over: not small; the fade is
off on a small TV with Lively off and on with Lively on, and unchanged on a large TV.

## 2. L2: do R8 full mode and resource shrinking really apply?

### 2.1 Definition of done
- A written answer, with bytes: `minifyEnabled true` and `shrinkResources true` are set
  (`launcher/app/build.gradle:51-52`); AGP is 8.13.2 and `gradle.properties` has no R8 flag. Full mode has been the
  default since AGP 8.0, so the claim is checked by building once with `android.enableR8.fullMode=false` and comparing.
- Release APK bytes (offline and online) before and after every experiment, in the table of section 6.
- A change is kept only if it shrinks the APK **and** the unit tests still pass; otherwise nothing changes and the note
  says "already in effect".
- `-dontobfuscate` (`proguard-rules.pro`) is measured but **not** removed without Ahmad's word: it is a deliberate choice
  (readable crash traces) and this batch does not reverse it for a few kilobytes.

### 2.2 Method
Build `assembleOfflineRelease assembleOnlineRelease` on unchanged `main` code (baseline), then with the flag off, then with
the experiment, each into its own output folder outside the repo (`-PoutDir`). The release type falls back to the debug
signing key when `TVHOME_KEYSTORE` is unset, so no secret is needed and the sizes are the release sizes (the signature
block differs by a few hundred bytes from a project-key build; the table says so).

## 3. L2b: bundled wallpapers as lossy WebP

### 3.1 Definition of done
- Every picture of `wallpapers/{homelab,kids,serene}` has a `.webp` of the **same pixel size**; none is dropped (a JVM
  test compares the two lists and the dimensions, reading both headers).
- The APK carries the `.webp` files and no bundled `.jpg` (a `Sync` include change in `launcher/app/build.gradle`).
- Wallpapers still show on the TV path: built-in sets are listed, copied and decoded (`Wallpapers.accepted`,
  `BuiltInWallpapers.copy`, the settings thumbnail) with the new extension, and a user's own `.jpg`, `.jpeg` and `.png`
  in `files/wallpapers/` and `files/wallpaper.jpg` work exactly as before (tests for each).
- **Upgrade path:** after an upgrade from 3.0.2 each built-in picture exists once, as WebP, and no old `.jpg` copy stays on
  /data; a user's own pictures are never deleted (a JVM test on a temporary folder, see 3.5).
- The conversion is one documented script, reproducible, English, no personal path; quality is one value per folder.
- Bytes before and after in section 6, measured on the real APK.

### 3.2 How the pictures travel (read on `main`)
`syncWallpaperAssets` copies `wallpapers/<set>/*.jpg` into `assets/wallpapers/<set>/` (`build.gradle:76-85`).
`BuiltInWallpapers.copy` copies each asset whose name passes `Wallpapers.accepted` (jpg, jpeg, png) to the app's files
once per version; `Wallpapers.list` then feeds `MainActivity.showWallpaper`, which reads the file with
`BitmapFactory.decodeFile` and does not care about the format. `LauncherSettingsActivity.thumbnail` looks for a name ending
in `.jpg` (`:852`). minSdk is 26: lossy WebP decodes natively since API 14, so no library is added.

### 3.3 Plan
- Keep the JPEG files where they are as the **masters** (they are the source for any later, better re-encode; the repo
  grows by the WebP size, about 0.6 MB). Only the `.webp` files go into the APK (`include '*/*.webp'`).
- `wallpapers/to-webp.py` (Python 3, Pillow, the same dependency as `gen-serene.py`): one WebP per JPEG, same size, method
  6; quality per folder from a dictionary at the top of the file, overridable with `--quality serene=90`. `--check`
  verifies that every JPEG has a WebP of the same dimensions and prints the byte totals. Raising quality is a one-line
  change plus a re-run.
- `Wallpapers.accepted` also accepts `.webp` (user pictures too: a lossy WebP is fine for a user). The thumbnail picks the
  first accepted picture instead of the first `.jpg`. Nothing is removed from the accepted list.
- `docs/CONFIG.md` and its Arabic twin say that `.webp` is accepted in `files/wallpapers/`.
- Quality 80 to start (measured on copies: 3,181,869 B as JPEG, 598,374 B as WebP q80, 452,834 B at q70, same pixels).

### 3.4 Upgrade: stale built-in copies (read on `main`, `BuiltInWallpapers.java`)
Built-in pictures are copied to `files/builtin-wallpapers/<set>/`, a folder of their own; a user's pictures live in
`files/wallpapers/` and `files/wallpaper.jpg`, which this class never touches. `copy` deletes everything in the set's
folder before copying, but only when the stamp file `.version` differs from `BuildConfig.VERSION_NAME`. So:
- A release upgrade (3.0.2 to a new versionName) clears the old `.jpg` copies: each picture appears once, no double entries.
- **Gap:** a candidate build with the *same* versionName as the installed one would keep the old `.jpg` copies and skip
  the new `.webp` ones (the stamp matches). The stamp therefore becomes `versionName` plus a constant `FORMAT` ("webp1"),
  so the format change itself forces one clean copy, whatever the versionName.
- The folder clean-up moves into a small pure helper (`clearSet(File)`) so the test can run it on a temporary folder:
  it removes only inside the set folder, and a sibling `wallpapers/` folder with a user's `.jpg` is left intact.

### 3.5 Keeping the two sets from drifting
`WallpaperAssetsTest` (JVM) reads `wallpapers/` and fails if a JPEG master has no WebP of the same name, if a WebP has a
different pixel size (JPEG size from `ImageIO`, WebP size from the RIFF header), or if a bundled folder has none. The same
check is `to-webp.py --check`, run locally; CI needs only the JVM test (no Pillow on the runner).

### 3.6 Not decided by numbers
Picture quality on a TV (banding on soft gradients, the serene set most of all) is **not verified**. Ahmad compares before
and after on the Chromecast for each of the three folders; a folder that bands gets a higher quality in the dictionary, or
stays JPEG (then its masters are the shipped files, one include line).

## 4. L3: size and memory table in the release notes

### 4.1 Definition of done
- `docs/RELEASE_NOTES_FOOTER.md` and `.ar.md` carry an "APK size and memory" table with the release's own numbers.
- `CHANGELOG.md` and `.ar.md` get an "Unreleased" entry for batch L whose numbers are the measured ones of section 6.
- Memory rows hold only numbers read on a device with the front activity named; a row never measured is written "not
  measured", never estimated.

## 5. Light budget (3.1 plan, section 3)
APK at or under 4.2 MB, no new dependency, permission or service, no `INTERNET` in the offline flavor, cold start no more
than 10% slower. L can only shrink the APK; the cold-start and memory-loop lines are device measurements, listed below.

## 6. Bytes (measured 2026-11-16, release type, debug-key fallback, same machine and same build command)
| Item | Before (`main` at `47c07d0`) | After (this branch) | Change |
|---|---|---|---|
| 22 wallpapers inside the APK | 3,181,869 B (JPEG) | 598,374 B (WebP q80) | -2,583,495 B (-81%) |
| Offline APK | 3,888,880 B | 1,306,038 B | -2,582,842 B |
| Online APK | 3,897,304 B | 1,314,438 B | -2,582,866 B |
| Built-in copies in `files/builtin-wallpapers/` on the TV, after an upgrade | not measured (device phase) | not measured (device phase) | |

The "before" files rebuilt here are byte-identical in size to the released v3.0.2 files (3,888,880 and 3,897,304), so the
two columns compare like with like. Both are far under the 4.2 MB cap of the light budget.

### L2 result: R8 full mode and resource shrinking
| Build of this branch | Offline APK | Online APK |
|---|---|---|
| As it is (AGP 8.13.2 default, minify and shrink on) | 1,306,038 B | 1,314,438 B |
| `-Pandroid.enableR8.fullMode=false` | 1,320,826 B | 1,328,950 B |
| `-dontobfuscate` removed (experiment only) | 1,293,982 B | 1,302,886 B |

- Full mode **is in effect**: turning it off makes the APK 14.8 KB (offline) and 14.5 KB (online) bigger, so nothing is
  added to `gradle.properties` and nothing changes in the build for L2. Resource shrinking is on (`shrinkResources true`);
  the biggest entries left are `classes.dex` (197 KB) and the three fonts (about 100 KB each).
- Dropping `-dontobfuscate` would save about 12 KB (0.9%) and make crash traces unreadable. Not done; it is Ahmad's call
  to revisit.

## 7. Risks
- A WebP that bands on a TV: quality per folder, or JPEG kept (section 3.6).
- A user pushing `.webp` expecting it to be ignored: it is now accepted; a bad file is skipped by the decoder as before.
- The gate hides the fade on a 2 GB TV that would have coped: the user can turn Lively on; the choice always wins.
- The threshold is a guess until the Chromecast's `MemTotal` is read (section 1.2).

## 8. NOT TESTED without a device
- Real `totalMem` and `isLowRamDevice()` values on the Chromecast, the Xiaomi and the TCL TV (only one other 2 GB TV was read).
- The upgrade copy on a real TV (old `.jpg` gone, WebP present).
- That the memory line shows the right class on each TV (D1-a); under D1-b also that the fade really goes and Lively brings it back.
- Picture quality of the three WebP sets on a TV; decode and rotation of WebP assets on API 26 to 34.
- Cold start and the settings-loop memory test after the batch (device phase, typed CONFIRM).

## 9. Progress (2026-11-16)
| Item | State | Evidence |
|---|---|---|
| L1 small-memory gate (D1-a) | done, committed | `SmallMemoryTest`; 328 unit tests per flavor, 0 failures |
| L2 R8 full mode and shrinking | done: already in effect, no build change | section 6 table |
| L2b WebP wallpapers | done, committed; picture quality on a TV not verified | 3,181,869 B to 598,374 B |
| L3 size and memory table | done (footer, changelog EN and AR); memory rows say "not measured" | |
| Device phase (totalMem, cold start, memory loop, upgrade copy, picture check) | not started | needs a TV and a typed CONFIRM |
| Push and PR | not started, waits for Ahmad's word | |
