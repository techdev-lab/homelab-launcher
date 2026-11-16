
---

### Which file?

- **`HomeLab-Launcher-<version>.apk`**: the full app (prayer times, Hijri date, weather, football, favourite clubs, all three wallpaper sets and the plain colours). Most people want this one.
- **`HomeLab-Launcher-<version>-offline.apk`**: the same without network access.
- **`SHA256SUMS.txt`**: checksums of both.

### APK size and memory

Filled in for every release with numbers that were measured; a row that was not measured says so.

| Item | This release | Previous release |
|---|---|---|
| `HomeLab-Launcher-<version>.apk` (online) | `<bytes>` bytes | `<bytes>` bytes |
| `HomeLab-Launcher-<version>-offline.apk` | `<bytes>` bytes | `<bytes>` bytes |
| Bundled wallpapers inside the APK | `<bytes>` bytes | `<bytes>` bytes |
| Memory on the home screen (device, Android version, front activity, how long idle) | `<MB PSS>` or "not measured" | `<MB PSS>` or "not measured" |
| Objects after 20 Launcher settings round trips and 5 minutes idle (Activities, AppContexts, Views) | `<before> / <after>` or "not measured" | |

The cap for the online APK is 4.2 MB. The memory rows are for information; the figure that decides is that the counters return to their starting values.

### Install

`adb install HomeLab-Launcher-<version>.apk`, or copy it to the TV with *Send files to TV* or *Downloader*. It installs next to the stock home screen. Read [INSTALL.md](https://github.com/techdev-lab/homelab-launcher/blob/main/docs/INSTALL.md) before making it the Home screen.

Arabic: see the Arabic section of the [README](https://github.com/techdev-lab/homelab-launcher#%D8%A7%D9%84%D8%B9%D8%B1%D8%A8%D9%8A%D8%A9).
