# Security

[English](SECURITY.md) · [العربية](SECURITY.ar.md)

## What the app can do

- **offline build:** no INTERNET permission. It cannot reach the network.
- **online build:** HTTPS requests to the ESPN scoreboard, Open-Meteo, and RSS feeds you configure. 8-second timeouts, a 512 KB response cap, no redirects followed.
- No location lookup: the place for prayer times and weather is typed into `config.json`, and no code in the app calls a location API. The manifest does declare `ACCESS_FINE_LOCATION`, because Android 10 and later only let an app read the Wi-Fi network name (shown in the Wi-Fi pill) with it.
- `HomeKeyService` and `InputKeyService` are accessibility services limited to key events (`canRequestFilterKeyEvents`, no `canRetrieveWindowContent`). They do not read window content, and every key they do not handle passes through untouched. Both are opt-in: they stay off until you enable them.
- No analytics, accounts, crash reporting or remote configuration.

## What the install does to the TV

- Packages are only disabled (`pm disable-user`), never uninstalled. Every change has a written rollback.
- Wireless debugging is needed during the install. **Turn it off when you are done**: anyone on your network who can pair could otherwise control the TV.
- Release APKs are built and signed by GitHub Actions from the tagged source with the project key (see [docs/RELEASING.md](docs/RELEASING.md)). An APK you build yourself on your own computer is signed with your own debug key instead, so its fingerprint will differ. Verify any APK you did not build yourself, as below.

## Checking an APK

Every release publishes `SHA256SUMS.txt` next to the APKs. Check the file, then check who signed it:

```
sha256sum --ignore-missing -c SHA256SUMS.txt
apksigner verify --print-certs HomeLab-Launcher-<version>.apk
```

The line `Signer #1 certificate SHA-256 digest` must be:

```
14849bdb3ed13523f75eede24e6b8cc36270d3a6f8f4bcb86f454311c616affe
```

This fingerprint is public. The release workflow and `scripts/Test-Release.ps1` both pin it, and the workflow stops before publishing an APK that carries any other certificate. The project signs every release with one long-lived key, so a different fingerprint on a file that claims to be a release means the file did not come from this project: do not install it, and open an issue or use the private reporting below. If the project ever has to change its key, the change will be announced in `CHANGELOG.md` and on the Releases page before any APK signed with the new key is published.

## Reporting a problem

Open a GitHub issue for bugs. For a security issue, use GitHub's private vulnerability reporting on this repository instead of a public issue.
