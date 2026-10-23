# Security

## What the app can do

- **offline build:** no INTERNET permission. It cannot reach the network.
- **online build:** HTTPS requests to the ESPN scoreboard, Open-Meteo, and RSS feeds you configure. 8-second timeouts, a 512 KB response cap, no redirects followed.
- No location lookup: the place for prayer times and weather is typed into `config.json`, and no code in the app calls a location API. The manifest does declare `ACCESS_FINE_LOCATION`, because Android 10 and later only let an app read the Wi-Fi network name (shown in the Wi-Fi pill) with it.
- `HomeKeyService` and `InputKeyService` are accessibility services limited to key events (`canRequestFilterKeyEvents`, no `canRetrieveWindowContent`). They do not read window content, and every key they do not handle passes through untouched. Both are opt-in: they stay off until you enable them.
- No analytics, accounts, crash reporting or remote configuration.

## What the install does to the TV

- Packages are only disabled (`pm disable-user`), never uninstalled. Every change has a written rollback.
- Wireless debugging is needed during the install. **Turn it off when you are done**: anyone on your network who can pair could otherwise control the TV.
- Release APKs built from this repository are signed with the builder's local debug key. Verify the SHA-256 of any APK you did not build yourself.

## Reporting a problem

Open a GitHub issue for bugs. For a security issue, use GitHub's private vulnerability reporting on this repository instead of a public issue.
