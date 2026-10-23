# run-tests.ps1: runs Set-TvhomeAsHome.ps1 against a FAKE Mi TV (fakeadb.py), no device and no network.
# Scenarios: dry run, missing confirmation, success with app keys, set-home ignored (must roll back),
# network owned by setupwraith or owner unreadable (must abort with no change), rollback dry run and executed,
# HomeKeyService never binds (must roll back packages and accessibility).
#   powershell -ExecutionPolicy Bypass -File .\tests\run-tests.ps1
# Needs: python on PATH. No APK and no device.
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
Run 'rollback dry run changes nothing' 'normal' @('-Rollback') { param($o, $w, $s) $w.Count -eq 0 }
Run 'rollback executed re-enables both packages' 'normal' @('-Rollback', '-ConfirmText', 'CONFIRM ROLLBACK tv') {
    param($o, $w, $s) $s.enabled.Count -eq 2 -and $s.home -like '*launcherx' }
Run 'service never binds: packages and accessibility both rolled back' 'bind_fails' @('-Execute', '-ConfirmText', 'CONFIRM HOME tv', '-EnableAppKeys') {
    param($o, $w, $s) $s.enabled.Count -eq 2 -and $s.home -like '*launcherx' -and $s.a11y -eq 'null' -and $s.a11y_on -eq '0' }

"`n$pass passed, $fail failed"
if ($fail -gt 0) { exit 1 }
