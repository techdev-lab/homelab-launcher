
---

### Which file?

- **The APK without `-offline` in its name**: the full app (prayer times, Hijri date, weather, football, favourite clubs, all three wallpaper sets and the plain colours). Most people want this one.
- **The APK ending in `-offline.apk`**: the same without network access.
- **`SHA256SUMS.txt`**: checksums of both.

### APK size and memory

The size of each file is listed beside it in the assets below. The cap for the online APK is 4.2 MB. Memory is measured for a release only when it was really measured, on a Chromecast with Google TV 4K, and then it is written in the notes above together with the device and how long it idled; a release whose notes have no memory figure did not measure it. The figure that decides is that the objects (Activities, AppContexts, Views) return to their starting values after 20 Launcher settings round trips and 5 minutes idle.

### Install

`adb install` with the APK file name, or copy it to the TV with *Send files to TV* or *Downloader*. It installs next to the stock home screen. Read [INSTALL.md](https://github.com/techdev-lab/homelab-launcher/blob/main/docs/INSTALL.md) before making it the Home screen.

Arabic: see the Arabic section of the [README](https://github.com/techdev-lab/homelab-launcher#%D8%A7%D9%84%D8%B9%D8%B1%D8%A8%D9%8A%D8%A9).
