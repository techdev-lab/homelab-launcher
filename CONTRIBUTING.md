# Contributing

[English](CONTRIBUTING.md) · [العربية](CONTRIBUTING.ar.md)

Thank you for helping. The most useful contributions right now:

1. **Arabic team names for more leagues.** Add entries to `launcher/app/src/main/assets/names_ar.json` under `"teams"`, keyed by the ESPN team id from `https://site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams`, and the league under `"leagues"`. Do not change existing entries without a reason in the PR.
2. **A free football source for the Arab leagues still missing** (Kuwait, Jordan, Tunisia, Algeria) or for live scores of Egypt, UAE, Qatar and Morocco (TheSportsDB's free API gives the next kick-off only). It must be HTTPS, keyless or free, and allowed for this use.
3. **A prayer method that is missing** (see `Prayer.METHODS`). Add it with a row in `PrayerMethodsTest` from an independent source.
4. **Testing on other devices** (Chromecast HD, Google TV Streamer, other Android TV boxes). Report what worked and what did not.

## Rules

- Code, comments, docs and commit messages are in English. User-facing strings go in the app's resources (`values-ar/`, `values/`), and Arabic data in `names_ar.json`. Every user-facing doc has an Arabic twin next to it named `*.ar.md` (for example `docs/INSTALL.ar.md`): when you change a doc, change its twin too, or say in the pull request that you could not and it will be completed.
- Run the tests before a pull request:
  ```bash
  cd launcher && ./gradlew testOfflineReleaseUnitTest testOnlineReleaseUnitTest
  ```
  and, on Windows, the fake-TV tests for the scripts:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/tests/run-tests.ps1
  ```
- Run the repository checks (Arabic only where it belongs, no keys or `local.properties` tracked); CI runs the same:
  ```bash
  python3 scripts/ci/check_hygiene.py
  python3 -m unittest discover -s scripts/ci
  ```
- Every new feature comes with at least one test of a failure case.
- No new permissions without a reason in the PR. The offline build must stay without INTERNET.
- Never commit IP addresses, Wi-Fi names, pairing codes, ADB keys or `local.properties`.
