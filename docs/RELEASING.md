# Releasing

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

Both keep `versionCode 4` (see `CHANGELOG.md`), so any release installs over another with `adb install -r`.

## One-time setup: the signing key

The workflow needs the project keystore as a repository secret. Without it the job stops on purpose: an APK signed
with a throwaway key could never be updated by the next release.

1. On the maintainer's computer, turn the keystore into one line of base64 (PowerShell):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")) | Set-Content -NoNewline keystore.b64
   ```
2. GitHub: *Settings > Secrets and variables > Actions > New repository secret*. Name `TVHOME_KEYSTORE_B64`,
   value: the content of `keystore.b64`. Delete `keystore.b64` afterwards.
3. Only if the keystore does not use the Android debug defaults, also add `TVHOME_KEYSTORE_PASSWORD`,
   `TVHOME_KEY_ALIAS` and `TVHOME_KEY_PASSWORD`.

Using the same keystore as the maintainer's local builds means a TV that already runs a local build can take a
release APK with `adb install -r`, and the other way round. A different key would need an uninstall first, which
also removes `config.json` from the TV.

## Making a release

1. Bump `versionName` in `launcher/app/build.gradle` and add a `## <version> (<date>)` section to `CHANGELOG.md`.
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

The workflow log prints the certificate's SHA-256 for every release, so you can compare it with the one on your
download.
