## What and why

<!-- One or two sentences. Link the issue if there is one. -->

## Checklist

- [ ] Code, comments, docs and commit messages are in English. Arabic appears only in `*.ar.md` docs and localization data.
- [ ] `cd launcher && ./gradlew testOfflineReleaseUnitTest testOnlineReleaseUnitTest` passes.
- [ ] New behavior has at least one test of a failure case.
- [ ] No new permission, or the reason is written below. The offline build still has no INTERNET permission.
- [ ] No IP addresses, Wi-Fi names, pairing codes, ADB keys or `local.properties`.
- [ ] `python3 scripts/ci/check_hygiene.py` passes.
