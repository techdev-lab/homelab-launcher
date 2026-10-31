# Releasing

[English](RELEASING.md) · [العربية](RELEASING.ar.md)

Every release on the [Releases page](https://github.com/techdev-lab/homelab-launcher/releases) is built by the
`Release APK` workflow (`.github/workflows/release.yml`) from a tag, not uploaded by hand. It runs the unit tests,
builds both flavors, signs them with the project key, writes `SHA256SUMS.txt` and publishes the release with the
notes for that version from `CHANGELOG.md`.

## Files in a release

| File | Flavor |
|---|---|
| `HomeLab-Launcher-<version>.apk` | `online`: everything, including weather, football and favourite clubs |
| `HomeLab-Launcher-<version>-offline.apk` | `offline`: no INTERNET permission at all |
| `SHA256SUMS.txt` | SHA-256 of both APKs |

Both keep `versionCode 4` (see `CHANGELOG.md`), so any release installs over another with `adb install -r`. A store
needs the `versionCode` to rise with each release: see "Open decision for the release" in `docs/DISTRIBUTION.md`.

The release page carries the notes in English and then in Arabic: the workflow takes the section of the version from
`CHANGELOG.md` and from `CHANGELOG.ar.md`, so both files need a `## <version> (...)` heading before a tag.

## The signing key

Every release is signed with one long-lived project key. Its certificate SHA-256 is
`14849bdb3ed13523f75eede24e6b8cc36270d3a6f8f4bcb86f454311c616affe` (public information). `release.yml` and
`scripts/Test-Release.ps1` both pin it, and the workflow stops before publishing when an APK carries any other
certificate.

The key was created by the Android tools on the maintainer's computer (`debug.keystore`), and the first TVs were
installed with it. Android refuses an update signed by a different key unless the app is uninstalled first, which also
removes `config.json` from the TV, so the project keeps this key. A debug keystore is unique to the computer that made
it, but it is easy to lose and has a well-known default password, so treat it as a release key:

- Keep the key in a project key file with its own strong password, outside the Android tools folder.
  `keytool -importkeystore` copies the key and its certificate unchanged, so the digest above stays the same.
- Keep two encrypted copies in different places. Check a copy with `keytool -list -v`: the SHA-256 must match.
- If the key is lost, no installed TV can be updated without an uninstall. If it leaks, anyone can sign updates that
  TVs accept.

### One-time setup: the repository secrets

The workflow needs the key as repository secrets. Without them the job stops on purpose: an APK signed with a
throwaway key could never be updated by the next release.

1. On the maintainer's computer, turn the key file into one line of base64 (PowerShell):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("<path to the key file>")) | Set-Content -NoNewline keystore.b64
   ```
2. GitHub: *Settings > Secrets and variables > Actions > New repository secret*. Name `TVHOME_KEYSTORE_B64`,
   value: the content of `keystore.b64`. Delete `keystore.b64` afterwards.
3. Also add `TVHOME_KEYSTORE_PASSWORD`, `TVHOME_KEY_ALIAS` and `TVHOME_KEY_PASSWORD`. The defaults (`android` and
   `androiddebugkey`) apply only when these are not set, which is the case for the original debug keystore.

Using the same key for the maintainer's local builds means a TV that already runs a local build can take a release APK
with `adb install -r`, and the other way round.

## Making a release

1. Bump `versionName` in `launcher/app/build.gradle` and add a `## <version> (<date>)` section to `CHANGELOG.md` and to
   `CHANGELOG.ar.md`.
2. Merge that to `main` through a pull request (the three checks must pass).
3. Tag the merge commit and push the tag:
   ```bash
   git tag v<version> <commit>
   git push origin v<version>
   ```
4. Watch *Actions > Release APK*. When it is green the release is public.

To re-run for an existing tag: *Actions > Release APK > Run workflow*, and type the tag.

## Checking a download

```bash
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs HomeLab-Launcher-<version>.apk
```

On Windows you can run both checks at once (needs the Android build-tools and Java):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/Test-Release.ps1 -Apk .\HomeLab-Launcher-<version>.apk
```

It prints `RESULT: OK` only when the file matches `SHA256SUMS.txt` and is signed with the project certificate.

The workflow log prints the certificate's SHA-256 for every release, so you can compare it with the one on your
download.
