# Set-TvhomeAsHome.ps1
# Makes tvhome the real Home on a Google TV that runs launcherx + setupwraith (Android 11+).
# DRY RUN BY DEFAULT: prints what it would do and runs only read-only checks.
#
#   $env:AL_TV = "<ip:port>"          # the address is never written in this file
#   .\Set-TvhomeAsHome.ps1                                     # dry run (read-only)
#   .\Set-TvhomeAsHome.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM HOME livingroom" [-EnableAppKeys]
#   .\Set-TvhomeAsHome.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
#
# Order: tvhome must already be installed and tested with the remote (see docs/INSTALL.md).
#   1. pre-checks (abort on any failure)   2. rollback file written FIRST   3. on-device deadman timer
#   4. install the pinned APK if it differs   5. disable setupwraith, then launcherx   6. set tvhome as Home
#   7. verify; success cancels the deadman, failure rolls back at once.
# The deadman is meant to re-enable both packages on the TV itself after $DeadmanSeconds unless cancelled.
# KNOWN ISSUE: on a Chromecast with Google TV (Android 14) it did not run once the ADB link dropped (the
# background shell went with it). Do not rely on it: keep the rollback command at hand.
param(
    [string]$Alias = 'tv',          # your name for this TV; it goes into the confirmation text
    [switch]$Execute,
    [switch]$Rollback,
    [string]$ConfirmText = '',
    [switch]$EnableAppKeys,          # also enable HomeKeyService (remote buttons such as YouTube, config app_keys)
    [string]$Apk = '',               # optional: an APK to install first if the TV runs a different one
    [string]$ApkSha256 = '',
    [int]$DeadmanSeconds = 300
)
$ErrorActionPreference = 'Stop'
$Launcherx = 'com.google.android.apps.tv.launcherx'
$LauncherxHome = "$Launcherx/.home.HomeActivity"
$Setup = 'com.google.android.tungsten.setupwraith'
$Tvhome = 'com.gathin.tvhome'
$TvhomeHome = "$Tvhome/.MainActivity"
$HomeKey = "$Tvhome/.HomeKeyService"

if (-not $env:AL_TV) { throw 'Set $env:AL_TV = "<ip:port>" first.' }
$adb = $env:AL_ADB
if (-not $adb) { $adb = (Get-Command adb -ErrorAction SilentlyContinue).Source }
if (-not $adb) { $adb = Join-Path $env:USERPROFILE 'dev\platform-tools\platform-tools\adb.exe' }
if (-not (Test-Path $adb)) { throw "adb not found. Set `$env:AL_ADB to adb.exe." }
function sh([string]$c) { (& $adb -s $env:AL_TV shell $c) -join "`n" }
function say([string]$m) { Write-Host $m }
function Test-Link { (& $adb -s $env:AL_TV get-state 2>$null) -eq 'device' }

$ok = $true
function check([bool]$cond, [string]$what) { say ("  [{0}] {1}" -f $(if ($cond) { 'OK  ' } else { 'FAIL' }), $what); if (-not $cond) { $script:ok = $false } }

# Saved Wi-Fi networks are read block by block: the creator of the network IN USE is what matters, not a grep
# over every cname= line (that mixed networks and gave a false alarm on a Mi TV).
function Get-CurrentWifiOwner {
    $lines = (sh 'dumpsys wifi') -split "`n"
    $cur = $null
    foreach ($l in $lines) { if ($l -match 'mWifiInfo SSID: ([^,]+),') { $cur = $Matches[1].Trim('"'); break } }
    if (-not $cur) { return @{ Ssid = $null; Owner = $null } }
    $ssid = $null; $owner = $null
    foreach ($l in $lines) {
        if ($l -match '^\s*mPerId=') { break }
        # Android 14 lists a saved network as "- <Type> ID: n SSID:" or "* ID: n SSID:", depending on the build.
        if ($l -match '^\s*(?:(?:- \w+|\*) )?ID: \d+ SSID: "(.*?)" ') { $ssid = $Matches[1]; continue }
        if ($ssid -and $l -match 'cname=(\S+)') { if ($ssid -eq $cur -and -not $owner) { $owner = $Matches[1] }; $ssid = $null }
    }
    return @{ Ssid = $cur; Owner = $owner }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmm'
$stateDir = Join-Path $env:USERPROFILE '.homelab-launcher\state'
New-Item -ItemType Directory -Force $stateDir | Out-Null
$rollbackCmds = @(
    "pm enable --user 0 $Launcherx",
    "pm enable --user 0 $Setup",
    "cmd package set-home-activity --user 0 $LauncherxHome"
)

if ($Rollback) {
    say "ROLLBACK on $($env:AL_TV): re-enable launcherx and setupwraith, Home back to launcherx"
    $rollbackCmds | ForEach-Object { say "  adb shell $_" }
    if ($ConfirmText -cne "CONFIRM ROLLBACK $Alias") { say "`nDry run. Add -ConfirmText 'CONFIRM ROLLBACK $Alias' to run it."; return }
    if (-not (Test-Link)) { & $adb connect $env:AL_TV | Out-Null }
    $rollbackCmds | ForEach-Object { sh $_ | Out-Null }
    sh 'rm -f /data/local/tmp/tvhome-deadman.cancel' | Out-Null
    say ("Home now: " + ((sh 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1')))
    return
}

say "== 1. pre-checks (read-only) on $($env:AL_TV)"
if (-not (Test-Link)) { & $adb connect $env:AL_TV | Out-Null }
check (Test-Link) 'adb link up'
$pkgs = sh 'pm list packages'
check ($pkgs -match "package:$([regex]::Escape($Tvhome))\b") 'tvhome installed'
check ((sh "pm list packages -e $Launcherx") -match 'launcherx') 'launcherx enabled (nothing disabled yet)'
$setupEnabled = (sh "pm list packages -e $Setup") -match 'setupwraith'
say ("  [INFO] setupwraith enabled: $setupEnabled")
check ((sh "ls /sdcard/Android/data/$Tvhome/files/config.json 2>&1") -notmatch 'No such file') 'config.json present on the TV'
# Since 2.7 the HomeLab, Serene and Kids sets are built into the APK: pushed pictures are only for "rotate".
$pushedWalls = (sh "ls /sdcard/Android/data/$Tvhome/files/wallpapers 2>&1") -notmatch 'No such file'
say ("  [INFO] pushed wallpapers folder: $pushedWalls (not needed for the built-in sets)")
$wifi = Get-CurrentWifiOwner
check ($null -ne $wifi.Ssid) 'TV is on Wi-Fi'
if ($wifi.Ssid) {
    $own = $wifi.Owner
    say ("  [INFO] network in use is owned by: $own")
    # Fail closed: a blank owner means the parse found nothing, not that the network is safe.
    # (On a Chromecast the owner once read blank, the check passed, and disabling setupwraith deleted the saved Wi-Fi.)
    check ([bool]$own) 'owner of the network in use was read (blank = unknown, stop)'
    check ($own -ne $Setup) 'network in use is NOT owned by setupwraith (disabling it would drop the TV off Wi-Fi)'
}
$useApk = [bool]$Apk
if ($useApk) {
    check (Test-Path $Apk) 'pinned APK file exists'
    check ($ApkSha256 -match '^[0-9A-Fa-f]{64}$') 'pinned SHA-256 given with -ApkSha256'
    if ((Test-Path $Apk) -and $ApkSha256) { check ((Get-FileHash $Apk).Hash -eq $ApkSha256.ToUpper()) 'pinned APK SHA-256 matches' }
}
$devHash = ((sh "p=`$(pm path $Tvhome); sha256sum `${p#package:}") -split ' ')[0].ToUpper()
say ("  [INFO] APK on the TV: $($devHash.Substring(0, [Math]::Min(12, $devHash.Length)))..." + $(if ($useApk) { "  pinned: $($ApkSha256.Substring(0, [Math]::Min(12, $ApkSha256.Length)))..." } else { "  (no -Apk: the installed tvhome is kept)" }))
$a11yBefore = (sh 'settings get secure enabled_accessibility_services') + ' | ' + (sh 'settings get secure accessibility_enabled')
say ("  [INFO] accessibility now: $a11yBefore")
say ("  [INFO] Home now: " + (sh 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1'))

$plan = @()
if ($useApk -and $devHash -ne $ApkSha256.ToUpper()) { $plan += "adb install -r <pinned apk>   (an upgrade in place, never a downgrade)" }
if ($setupEnabled) { $plan += "adb shell pm disable-user --user 0 $Setup" }
$plan += "adb shell pm disable-user --user 0 $Launcherx"
$plan += "adb shell cmd package set-home-activity --user 0 $TvhomeHome"
if ($EnableAppKeys) { $plan += "adb shell settings put secure enabled_accessibility_services $HomeKey ; settings put secure accessibility_enabled 1" }
say "`n== plan (rollback file written first, deadman $DeadmanSeconds s on the TV)"
$plan | ForEach-Object { say "  $_" }
say "== rollback"
$rollbackCmds | ForEach-Object { say "  adb shell $_" }
if ($EnableAppKeys) { say "  adb shell settings put secure enabled_accessibility_services $($a11yBefore.Split('|')[0].Trim()) ; accessibility_enabled $($a11yBefore.Split('|')[1].Trim())" }

if (-not $ok) { throw 'Pre-checks failed: nothing was changed.' }
if (-not $Execute) { say "`nDRY RUN: nothing changed. Run with -Execute -ConfirmText 'CONFIRM HOME $Alias'."; return }
if ($ConfirmText -cne "CONFIRM HOME $Alias") { throw "Missing exact confirmation text: -ConfirmText 'CONFIRM HOME $Alias'" }

say "`n== 2. rollback file"
$rbFile = Join-Path $stateDir "rollback_home_$stamp.txt"
$rollbackCmds | Set-Content -Path $rbFile -Encoding ascii
say "  $rbFile"

say "== 3. deadman timer on the TV ($DeadmanSeconds s)"
$dm = @"
#!/system/bin/sh
sleep $DeadmanSeconds
if [ ! -f /data/local/tmp/tvhome-deadman.cancel ]; then
  pm enable --user 0 $Launcherx
  pm enable --user 0 $Setup
  cmd package set-home-activity --user 0 $LauncherxHome
  echo restored > /data/local/tmp/tvhome-deadman.fired
fi
rm -f /data/local/tmp/tvhome-deadman.sh
"@ -replace "`r`n", "`n"
$tmp = Join-Path $env:TEMP "tvhome-deadman-$stamp.sh"
[IO.File]::WriteAllText($tmp, $dm, (New-Object Text.UTF8Encoding $false))
# A timer left by an earlier run (a failed run retried within the timer) must not fire later and undo this run:
# it checks the cancel file, which the line below removes.
sh 'ps -A -o PID,ARGS | grep tvhome-deadman.sh | grep -v grep | while read p rest; do kill $p; done' | Out-Null
sh 'rm -f /data/local/tmp/tvhome-deadman.cancel /data/local/tmp/tvhome-deadman.fired' | Out-Null
& $adb -s $env:AL_TV push $tmp /data/local/tmp/tvhome-deadman.sh | Out-Null
& $adb -s $env:AL_TV shell 'nohup sh /data/local/tmp/tvhome-deadman.sh >/dev/null 2>&1 &' | Out-Null
Remove-Item $tmp -Force

function Restore-Now {
    say 'ROLLING BACK'
    foreach ($c in $rollbackCmds) { try { sh $c | Out-Null } catch { } }
    if ($EnableAppKeys) {
        # Put the accessibility settings back to what they were before this run.
        $svc = $a11yBefore.Split('|')[0].Trim(); $on = $a11yBefore.Split('|')[1].Trim()
        try {
            if ($svc -eq 'null' -or $svc -eq '') { sh 'settings delete secure enabled_accessibility_services' | Out-Null }
            else { sh "settings put secure enabled_accessibility_services $svc" | Out-Null }
            sh "settings put secure accessibility_enabled $(if ($on -eq 'null' -or $on -eq '') { 0 } else { $on })" | Out-Null
        } catch { }
    }
}

try {
    if ($useApk -and $devHash -ne $ApkSha256.ToUpper()) {
        say '== 4. install pinned APK'
        & $adb -s $env:AL_TV install -r $Apk
        if ($LASTEXITCODE -ne 0) { throw 'adb install failed' }
    }
    say '== 5. disable setupwraith, then launcherx'
    if ($setupEnabled) { sh "pm disable-user --user 0 $Setup" | Out-Null }
    sh "pm disable-user --user 0 $Launcherx" | Out-Null
    say '== 6. tvhome as Home'
    sh "cmd package set-home-activity --user 0 $TvhomeHome" | Out-Null
    Start-Sleep 8

    say '== 7. verify'
    $ok = $true
    if (-not (Test-Link)) { & $adb connect $env:AL_TV | Out-Null; Start-Sleep 3 }
    check (Test-Link) 'adb link still up'
    check ((sh 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1') -match [regex]::Escape($Tvhome)) 'HOME resolves to tvhome'
    sh 'input keyevent KEYCODE_HOME' | Out-Null; Start-Sleep 4
    # Android 12 and later print topResumedActivity; Android 11 (Mi TV MOOR2) prints only mResumedActivity.
    check ((sh "dumpsys activity activities 2>/dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'") -match [regex]::Escape($Tvhome)) 'HOME key opens tvhome'
    # Single quotes inside the device command: Windows PowerShell 5.1 drops embedded double quotes when it
    # passes an argument to a native program (adb), which would split "Supplicant state" in two.
    check ((sh "dumpsys wifi | grep -m1 'Supplicant state'") -match 'COMPLETED') 'Wi-Fi still connected'
    if (-not $ok) { throw 'verification failed' }

    if ($EnableAppKeys) {
        say '== app keys: enable HomeKeyService'
        sh "settings put secure enabled_accessibility_services $HomeKey" | Out-Null
        sh 'settings put secure accessibility_enabled 1' | Out-Null
        Start-Sleep 3
        check ((sh "dumpsys accessibility | grep -m1 'Bound services'") -match 'Home') 'HomeKeyService bound'
        if (-not $ok) { throw 'HomeKeyService did not bind' }
    }
    sh 'touch /data/local/tmp/tvhome-deadman.cancel' | Out-Null
    say "`nDONE: tvhome is Home, deadman cancelled. Rollback: .\Set-TvhomeAsHome.ps1 -Alias $Alias -Rollback -ConfirmText 'CONFIRM ROLLBACK $Alias'"
    say "Reboot test is the next step (Home must come back as tvhome)."
} catch {
    say "FAILED: $($_.Exception.Message)"
    if (-not (Test-Link)) { & $adb connect $env:AL_TV | Out-Null }
    Restore-Now
    sh 'touch /data/local/tmp/tvhome-deadman.cancel' | Out-Null
    throw
}
