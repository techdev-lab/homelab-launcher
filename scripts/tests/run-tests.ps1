# run-tests.ps1: runs Set-TvhomeAsHome.ps1 against a FAKE Mi TV (fakeadb.py), no device and no network.
# Scenarios: dry run, missing confirmation, success with app keys, set-home ignored (must roll back),
# network owned by setupwraith or owner unreadable (must abort with no change), rollback dry run and executed,
# HomeKeyService never binds (must roll back packages and accessibility), built-in wallpapers with no pushed folder.
#   powershell -ExecutionPolicy Bypass -File .\tests\run-tests.ps1
# Needs: python on PATH. No APK and no device.
# Test-Release.ps1 scenarios make a fake APK in TEMP and use fakeapksigner.cmd.
$here = $PSScriptRoot
$script = Join-Path (Split-Path $here -Parent) 'Set-TvhomeAsHome.ps1'
$state = Join-Path $env:TEMP 'fake-tv-state.json'; $log = Join-Path $env:TEMP 'fake-tv-calls.log'
$env:AL_ADB = Join-Path $here 'fakeadb.cmd'; $env:AL_TV = 'tv-test:5555'
$env:FAKE_STATE = $state; $env:FAKE_LOG = $log
$pass = 0; $fail = 0

function Run([string]$name, [string]$mode, [string[]]$argList, [scriptblock]$expect) {
    Remove-Item $state, $log -ErrorAction SilentlyContinue
    $env:FAKE_MODE = $mode
    $o = (& powershell -NoProfile -ExecutionPolicy Bypass -File $script @argList 2>&1 | Out-String)
    $calls = if (Test-Path $log) { Get-Content $log } else { @() }
    $writes = @($calls | Where-Object { $_ -match 'disable-user|pm enable|set-home-activity|install|push|nohup|settings (put|delete)|touch' })
    $s = if (Test-Path $state) { Get-Content $state -Raw | ConvertFrom-Json } else { $null }
    $okk = & $expect $o $writes $s
    if ($okk) { $script:pass++ } else { $script:fail++ }
    "{0}  {1}" -f $(if ($okk) { 'PASS' } else { 'FAIL' }), $name
}

$relScript = Join-Path (Split-Path $here -Parent) 'Test-Release.ps1'
$relSigner = Join-Path $here 'fakeapksigner.cmd'
$relDir = Join-Path $env:TEMP 'test-release'
$relSums = Join-Path $relDir 'SHA256SUMS.txt'
$relSig = 'ab' * 32   # test digest, not a real certificate
$env:FAKE_SIGNER_DIGEST = $relSig

function MakeApk {
    Remove-Item $relDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $relDir | Out-Null
    $apk = Join-Path $relDir 'app.apk'
    [IO.File]::WriteAllBytes($apk, (New-Object byte[] 2048))
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLower()
    Set-Content -LiteralPath $relSums -Value "$hash  app.apk" -Encoding ascii
    return $apk
}

function RelArgs([string]$apk, [string]$signer) {
    return @('-Apk', $apk, '-Sums', $relSums, '-ExpectedSigner', $relSig, '-ApkSigner', $signer)
}

function RunRel([string]$name, [string]$mode, [string[]]$argList, [scriptblock]$expect) {
    $env:FAKE_SIGNER_MODE = $mode
    $o = (& powershell -NoProfile -ExecutionPolicy Bypass -File $relScript @argList 2>&1 | Out-String)
    $code = $LASTEXITCODE
    $okk = & $expect $o $code
    if ($okk) { $script:pass++ } else { $script:fail++ }
    "{0}  {1}" -f $(if ($okk) { 'PASS' } else { 'FAIL' }), $name
}

Run 'dry run changes nothing' 'normal' @() { param($o, $w, $s) $w.Count -eq 0 -and $o -match 'DRY RUN' }
Run 'execute without the confirmation text changes nothing' 'normal' @('-Execute') { param($o, $w, $s) $w.Count -eq 0 -and $o -match 'Missing exact confirmation' }
Run 'success: tvhome Home, launcherx and setupwraith disabled, deadman cancelled' 'normal' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv', '-EnableAppKeys') {
    param($o, $w, $s) $s.home -eq 'com.gathin.tvhome' -and $s.enabled.Count -eq 0 -and $s.cancel -and $s.a11y -match 'HomeKeyService' -and $o -match 'DONE' }
Run 'set-home ignored: rolls back to launcherx, deadman cancelled' 'sethome_ignored' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv') {
    param($o, $w, $s) $s.home -like '*launcherx' -and $s.enabled.Count -eq 2 -and $s.cancel }
Run 'network owned by setupwraith: aborts before any change' 'setup_owns_wifi' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv') {
    param($o, $w, $s) $w.Count -eq 0 -and $o -match 'Pre-checks failed' }
Run 'owner of the network in use unreadable: aborts before any change' 'owner_unknown' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv') {
    param($o, $w, $s) $w.Count -eq 0 -and $o -match 'Pre-checks failed' }
Run 'built-in wallpapers (no pushed folder) and "* ID:" network lines: dry run passes the checks' 'builtin_walls' @() {
    param($o, $w, $s) $w.Count -eq 0 -and $o -match 'DRY RUN' -and $o -notmatch 'Pre-checks failed' -and $o -match 'owned by: android.uid.system' }
Run 'rollback dry run changes nothing' 'normal' @('-Rollback') { param($o, $w, $s) $w.Count -eq 0 }
Run 'rollback executed re-enables both packages' 'normal' @('-Rollback', '-ConfirmText', 'CONFIRM ROLLBACK tv') {
    param($o, $w, $s) $s.enabled.Count -eq 2 -and $s.home -like '*launcherx' }
Run 'service never binds: packages and accessibility both rolled back' 'bind_fails' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv', '-EnableAppKeys') {
    param($o, $w, $s) $s.enabled.Count -eq 2 -and $s.home -like '*launcherx' -and $s.a11y -eq 'null' -and $s.a11y_on -eq '0' }

$apk = MakeApk
$before = (Get-FileHash -LiteralPath $apk).Hash + (Get-FileHash -LiteralPath $relSums).Hash
RunRel 'S1 all good' 'good' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 0 -and $o -match 'RESULT: OK' -and $o -match 'PASS sha256' -and $o -match 'PASS signer' -and $before -eq ((Get-FileHash -LiteralPath $apk).Hash + (Get-FileHash -LiteralPath $relSums).Hash) }
$apk = MakeApk
$bytes = [IO.File]::ReadAllBytes($apk); $bytes[0] = $bytes[0] -bxor 1; [IO.File]::WriteAllBytes($apk, $bytes)
RunRel 'S2 APK changed after sums made' 'good' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 1 -and $o -match 'FAIL sha256' }
$apk = MakeApk
$h = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLower()
Set-Content -LiteralPath $relSums -Value "$h  other.apk" -Encoding ascii
RunRel 'S3 sums lists another name' 'good' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 2 -and $o -match 'FAIL listed' }
$apk = MakeApk
Set-Content -LiteralPath $relSums -Value 'zzzz  app.apk' -Encoding ascii
RunRel 'S4 sums has only a malformed line' 'good' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 2 -and $o -match 'FAIL listed' }
$apk = MakeApk
RunRel 'S5 other signer' 'other' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 3 -and $o -match 'FAIL signer' }
$apk = MakeApk
RunRel 'S6 two signers' 'two' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 3 -and $o -match 'FAIL signer' }
$apk = MakeApk
RunRel 'S7 apksigner rejects the APK' 'reject' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 3 -and $o -match 'FAIL signer' }
$apk = MakeApk
RunRel 'S8 apksigner broken (no Java)' 'broken' (RelArgs $apk $relSigner) { param($o, $c) $c -eq 4 -and $o -match 'FAIL tool' }
$apk = MakeApk
RunRel 'S9 apksigner path does not exist' 'good' (RelArgs $apk (Join-Path $relDir 'missing-apksigner.bat')) { param($o, $c) $c -eq 4 -and $o -match 'FAIL tool' }
$apk = MakeApk
RunRel 'S10 -Apk is a directory' 'good' (RelArgs $relDir $relSigner) { param($o, $c) $c -eq 5 -and $o -match 'FAIL input' }

"`n$pass passed, $fail failed"
if ($fail -gt 0) { exit 1 }
