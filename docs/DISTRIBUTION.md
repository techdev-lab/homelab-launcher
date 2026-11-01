# Distribution and trust: batch 4 design note

[English](DISTRIBUTION.md) · [العربية](DISTRIBUTION.ar.md)

Status: written before the files it describes, as the other batches. Nothing here changes the app, its permissions or
its version.

Three small jobs that make the project easier to find and easier to trust:

1. Store metadata in the Fastlane layout, so a store such as IzzyOnDroid can list the app.
2. Make `SECURITY.md` say what is true about signing, and give a way to check an APK.
3. Credit where the ideas came from, and check that every link in the docs still works.

## 1. Store metadata (`fastlane/metadata/android/en-US/`)

IzzyOnDroid asks for the store text and graphics to live in the app repository in the Fastlane layout (policy and FAQ
read on 2026-10-31; neither page carries a date, so the rules are read again when the request is filed).

| File | Content |
|---|---|
| `title.txt` | `HomeLab Launcher` |
| `short_description.txt` | One line, at most 80 characters. |
| `full_description.txt` | What it does, the two builds, privacy, how an APK is built and signed. Plain text. |
| `changelogs/6.txt` | At most 500 bytes. The file name is the `versionCode` (6 in 3.0.1), so the file is renamed with each release. |
| `images/icon.png` | 512 by 512, made from the app's own launcher icon. |

- English in `en-US` and Arabic in `ar`. The first plan had English only, because the repository keeps Arabic in
  `*.ar.md`, the bilingual README, `values-ar` and data files. The owner then decided that everything on GitHub should
  also be readable in Arabic, since the people this is for are Arabic-speaking Muslims and some do not read English, so
  `fastlane/metadata/android/ar/` is an allowed place for Arabic in `scripts/ci/check_hygiene.py` (a unit test says so).
- Screenshots (`images/tvScreenshots/`) are not part of this batch. The current README screenshots were taken on the
  maintainer's own TV and show a city name and third-party apps, and they will change again with batch 5. They are
  redone once, at the end, from the demo config with a neutral city, and then added here.
- No claim in these files goes beyond what the code does: the offline build has no INTERNET permission, the
  accessibility services only see key events and stay off until enabled, there is no analytics.
- Which APK a store picks (`online` or `offline`) is decided when the request is filed, with the maintainer.

## 2. `SECURITY.md`

- The line "signed with the builder's local debug key" is wrong for releases: `docs/RELEASING.md` and the README say
  releases are built and signed by GitHub Actions with the project key. Only a build you make yourself on your own
  computer uses your own debug key. The line is rewritten to say so.
- A new section "Checking an APK" gives the certificate SHA-256 (public information, also pinned in `release.yml` and
  `scripts/Test-Release.ps1`), the `apksigner verify --print-certs` command, and the `SHA256SUMS.txt` check.
- It states the key policy in one paragraph: one long-lived project key, kept outside the Android tools folder with a
  strong password and two encrypted copies, and what a user should do if the fingerprint ever differs. No password, no
  path and no backup location is written anywhere.

## 3. Credits and links

- `NOTICE.md` already credits the fork source and tvOS Launcher. It gets one more paragraph: the idea of an *ignore*
  action for a remote key (`app_keys`) and of hiding apps from the grid came from features in the public 3.x releases
  of CleanYourTV; the code is ours.
- Ideas taken from other launchers are credited when the feature ships, not before.
- Every external link in the README, `docs/` and the root documents is requested once and its status is recorded in the
  logbook with the date. The ESPN address in the docs is a template with a league placeholder, so it is expected not to
  answer.

## Decision for the release: `versionCode` (decided 2026-10-31)

Until 2.9 every build kept `versionCode 4` so that any build installed over any other with `adb install -r`. A store
(IzzyOnDroid, F-Droid) decides that an update exists when the `versionCode` goes up, so a user who installed from a store
would never be offered a new release. The maintainer decided to raise it: 3.0.0 has `versionCode 5`.

- Before the first store-listed release the `versionCode` has to start rising with each release. The changelog file
  then follows it (`5.txt`, `6.txt`, and so on).
- The cost: an older build, 2.9.0 included, then no longer installs over a newer one. Going back means uninstalling,
  which removes `config.json` from the TV. The rollback APKs and the rollback text in `docs/INSTALL.md` must say so.
- Decided by the maintainer at the final upgrade, together with the release name (3.0). The next release is 6, and so on.

## Definition of done

- The `fastlane` files exist in `en-US` and `ar`, the short description is at most 80 characters, the changelog is at
  most 500 bytes, and `icon.png` is 512 by 512.
- `SECURITY.md` no longer says releases use a debug key, shows the fingerprint and the verification command, and
  contains no secret.
- `NOTICE.md` has the CleanYourTV paragraph and no duplicate of the tvOS one.
- Every external link answered 200 on the day of the check (the template address excepted).
- `python scripts/ci/check_hygiene.py` passes, unit tests and lint pass for both flavors, and `aapt2 dump badging`
  shows the same permissions as before. `versionName` and `versionCode` are unchanged.
