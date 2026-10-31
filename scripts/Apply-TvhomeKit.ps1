# Apply-TvhomeKit.ps1 - one shot: tvhome APK + config.json + wallpapers + YouTube key (HomeKeyService)
# + the tvhome ClockDream screensaver, on a Chromecast with Google TV that ALREADY runs tvhome as Home
# (launcherx AND setupwraith both disabled). Every step was first done by hand on a Chromecast 4K.
#
# Dry run by default (read-only). Run it from the kit folder (apk\, config\, wallpapers\ next to it):
#   $env:AL_TV = "<ip:port>"
#   .\Apply-TvhomeKit.ps1 -Alias livingroom
#   .\Apply-TvhomeKit.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM KIT livingroom"
#   .\Apply-TvhomeKit.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
#
# It never disables or enables a package and never touches Home: a TV where launcherx or setupwraith is still
# enabled is refused (that switch is Set-TvhomeAsHome.ps1's job; doing it by hand once put a TV in a boot loop,
# see docs/JOURNEY.md).
param(
    [Parameter(Mandatory = $true)][string]$Alias,
    [switch]$Execute,
    [switch]$Rollback,
    [string]$ConfirmText = '',
    [string]$KitDir = $PSScriptRoot,
    [string]$Adb = $(if ($env:AL_ADB) { $env:AL_ADB } elseif (Get-Command adb -ErrorAction SilentlyContinue) { (Get-Command adb).Source } else { 'adb.exe' }),
    [string]$ApkName = 'tvhome-online.apk',
    [string]$ApkSha256 = '',            # empty: taken from apk\SHA256SUMS.txt
    [string[]]$Codenames = @('sabrina', 'boreal')   # Chromecast with Google TV 4K and HD
)
$ErrorActionPreference = 'Stop'

$Tvhome = 'com.gathin.tvhome'
$Launcherx = 'com.google.android.apps.tv.launcherx'
$Setup = 'com.google.android.tungsten.setupwraith'
$HomeKey = 'com.gathin.tvhome/.HomeKeyService'
$Dream = 'com.gathin.tvhome/.ClockDream'
$Files = "/sdcard/Android/data/$Tvhome/files"
$Apk = Join-Path $KitDir "apk\$ApkName"
if (-not $ApkSha256) {
    $sums = Join-Path $KitDir 'apk\SHA256SUMS.txt'
    if (Test-Path $sums) {
        $line = Get-Content $sums | Where-Object { $_ -match "\s$([regex]::Escape($ApkName))$" } | Select-Object -First 1
        if ($line) { $ApkSha256 = ($line -split '\s+')[0].ToUpper() }
    }
}
$Cfg = Join-Path $KitDir "config\$Alias.config.json"
$Walls = Join-Path $KitDir 'wallpapers'
$StateDir = Join-Path $env:USERPROFILE '.homelab-launcher\state'

if (-not $env:AL_TV) { throw 'Set $env:AL_TV = "<ip:port>" first (the port from Wireless debugging).' }
if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) { throw "adb not found: $Adb (pass -Adb or set `$env:AL_ADB)" }

# Single quotes inside device commands: Windows PowerShell 5.1 drops embedded double quotes on native calls.
function sh([string]$c) { (& $Adb -s $env:AL_TV shell $c) -join "`n" }
function say([string]$m) { Write-Host $m }
$ok = $true
function check([bool]$cond, [string]$what) {
    say ('  [{0}] {1}' -f $(if ($cond) { 'OK  ' } else { 'FAIL' }), $what)
    if (-not $cond) { $script:ok = $false }
}
function Test-Link { (& $Adb -s $env:AL_TV get-state 2>$null) -eq 'device' }
function setting([string]$k) { (sh "settings get secure $k").Trim() }
function put-or-delete([string]$k, [string]$v) {
    if ($v -eq 'null' -or $v -eq '') { sh "settings delete secure $k" | Out-Null } else { sh "settings put secure $k $v" | Out-Null }
}

if (-not (Test-Link)) { & $Adb connect $env:AL_TV | Out-Null; Start-Sleep 2 }

# ---------------------------------------------------------------- rollback
if ($Rollback) {
    $last = Get-ChildItem $StateDir -Directory -Filter "backup_kit_$Alias`_*" -ErrorAction SilentlyContinue |
            Sort-Object Name | Select-Object -Last 1
    if (-not $last) { throw "no backup_kit_$Alias`_* folder in $StateDir" }
    $before = @{}
    Get-Content (Join-Path $last.FullName 'before.txt') | ForEach-Object { $k, $v = $_ -split '=', 2; $before[$k] = $v }
    say "ROLLBACK on $($env:AL_TV) from $($last.Name):"
    say "  install -r base.apk (sha256 $($before['apk_sha256'].Substring(0,12))...), config.json back,"
    say "  screensaver_components=$($before['screensaver_components']), accessibility=$($before['enabled_accessibility_services'])"
    if ($ConfirmText -cne "CONFIRM ROLLBACK $Alias") { say "`nDry run. Add -ConfirmText 'CONFIRM ROLLBACK $Alias' to run it."; return }
    check ((sh "pm list packages --user 0 -e $Launcherx") -notmatch 'launcherx') 'launcherx still disabled (Home stays tvhome)'
    check ((sh "pm list packages --user 0 -e $Setup") -notmatch 'setupwraith') 'setupwraith still disabled'
    if (-not $ok) { throw 'Refused: the safe install -r needs both stock Home packages disabled.' }
    & $Adb -s $env:AL_TV install -r (Join-Path $last.FullName 'base.apk')
    $oldCfg = Join-Path $last.FullName 'config.json'
    if (Test-Path $oldCfg) { & $Adb -s $env:AL_TV push $oldCfg "$Files/config.json" | Out-Null }
    sh "am force-stop $Tvhome" | Out-Null
    sh 'input keyevent KEYCODE_HOME' | Out-Null
    Start-Sleep 4
    put-or-delete 'screensaver_components' $before['screensaver_components']
    put-or-delete 'enabled_accessibility_services' $before['enabled_accessibility_services']
    put-or-delete 'accessibility_enabled' $before['accessibility_enabled']
    say ('Done. Home role: ' + (sh 'cmd role get-role-holders android.app.role.HOME'))
    return
}

# ---------------------------------------------------------------- 1. read-only checks
say "== 1. checks (read-only) on $($env:AL_TV), alias $Alias"
check (Test-Link) 'adb link up'
check ($Codenames -contains (sh 'getprop ro.product.device').Trim()) "Chromecast with Google TV ($($Codenames -join ' / '))"
check ((sh "pm list packages $Tvhome") -match [regex]::Escape("package:$Tvhome")) 'tvhome installed'
check ((sh "pm list packages --user 0 -e $Launcherx") -notmatch 'launcherx') 'launcherx disabled (else use Set-TvhomeAsHome.ps1 first)'
check ((sh "pm list packages --user 0 -e $Setup") -notmatch 'setupwraith') 'setupwraith disabled (else use Set-TvhomeAsHome.ps1 first)'
check ((sh 'cmd role get-role-holders android.app.role.HOME').Trim() -eq $Tvhome) 'Home role is tvhome'
check (Test-Path $Apk) "APK in kit: $Apk"
check ($ApkSha256 -match '^[0-9A-F]{64}$') 'APK SHA-256 known (-ApkSha256 or apk\SHA256SUMS.txt)'
if ((Test-Path $Apk) -and $ApkSha256) { check ((Get-FileHash $Apk -Algorithm SHA256).Hash -eq $ApkSha256) "APK SHA-256 = $($ApkSha256.Substring(0, [Math]::Min(12, $ApkSha256.Length)))..." }
check (Test-Path $Cfg) "config for this TV: $Cfg"
if (Test-Path $Cfg) {
    try { Get-Content $Cfg -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null; check $true 'config.json parses' } catch { check $false "config.json parses: $_" }
}
$wallCount = @(Get-ChildItem $Walls -Filter *.jpg -ErrorAction SilentlyContinue).Count
say "  [INFO] wallpapers in kit: $wallCount"
$apkPath = ((sh "pm path $Tvhome") -replace '^package:', '').Trim()
$devHash = ((sh "sha256sum $apkPath") -split ' ')[0].ToUpper()
$now = @{
    apk_sha256                     = $devHash
    screensaver_components         = (setting 'screensaver_components')
    enabled_accessibility_services = (setting 'enabled_accessibility_services')
    accessibility_enabled          = (setting 'accessibility_enabled')
}
say "  [INFO] tvhome on the TV: $($devHash.Substring(0, [Math]::Min(12, $devHash.Length)))... ($(((sh "dumpsys package $Tvhome") -split "`n" | Select-String -Pattern 'versionName=' | Select-Object -First 1).ToString().Trim()))"
say "  [INFO] screensaver now: $($now['screensaver_components'])"
say "  [INFO] accessibility now: $($now['enabled_accessibility_services']) | $($now['accessibility_enabled'])"

say "`n== plan"
if ($devHash -ne $ApkSha256) { say "  adb install -r $ApkName (an upgrade in place; the pinned APK must not have a lower versionCode than the one on the TV)" } else { say '  the TV already runs this APK: no install' }
say "  push config.json + $wallCount wallpapers, force-stop tvhome, HOME"
say "  settings: enabled_accessibility_services=$HomeKey, accessibility_enabled=1 (AFTER force-stop)"
say "  settings: screensaver_components=$Dream"
say '  backup first: base.apk + config.json + settings -> state\backup_kit_<alias>_<time>\'

if (-not $ok) { throw 'Checks failed: nothing was changed.' }
if (-not $Execute) { say "`nDRY RUN: nothing changed. Run with -Execute -ConfirmText 'CONFIRM KIT $Alias'."; return }
if ($ConfirmText -cne "CONFIRM KIT $Alias") { throw "Missing exact confirmation text: -ConfirmText 'CONFIRM KIT $Alias'" }

# ---------------------------------------------------------------- 2. backup
$bk = Join-Path $StateDir ("backup_kit_{0}_{1}" -f $Alias, (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $bk | Out-Null
say "`n== 2. backup -> $bk"
& $Adb -s $env:AL_TV pull $apkPath (Join-Path $bk 'base.apk') | Out-Null
& $Adb -s $env:AL_TV pull "$Files/config.json" (Join-Path $bk 'config.json') | Out-Null
$now.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } | Set-Content (Join-Path $bk 'before.txt') -Encoding ascii
if (-not (Test-Path (Join-Path $bk 'base.apk'))) { throw 'Backup of base.apk failed: nothing changed.' }

# ---------------------------------------------------------------- 3. apply
say '== 3. apply'
if ($devHash -ne $ApkSha256) {
    & $Adb -s $env:AL_TV install -r $Apk
    if ($LASTEXITCODE -ne 0) { throw 'adb install failed (the TV still runs the old tvhome; nothing else changed).' }
    $p2 = ((sh "pm path $Tvhome") -replace '^package:', '').Trim()
    $devHash = ((sh "sha256sum $p2") -split ' ')[0].ToUpper()
}
& $Adb -s $env:AL_TV push $Cfg "$Files/config.json" | Out-Null
sh "mkdir -p $Files/wallpapers" | Out-Null
if ($wallCount -gt 0) { & $Adb -s $env:AL_TV push "$Walls\." "$Files/wallpapers/" | Out-Null }
sh "am force-stop $Tvhome" | Out-Null
sh 'input keyevent KEYCODE_WAKEUP' | Out-Null
sh 'input keyevent KEYCODE_HOME' | Out-Null
Start-Sleep 5
# Accessibility AFTER force-stop: set before it, a force-stop cleared the setting to null.
sh "settings put secure enabled_accessibility_services $HomeKey" | Out-Null
sh 'settings put secure accessibility_enabled 1' | Out-Null
sh "settings put secure screensaver_components $Dream" | Out-Null
Start-Sleep 4

# ---------------------------------------------------------------- 4. verify
say '== 4. verify'
$ok = $true
check ($devHash -eq $ApkSha256) 'tvhome on the TV = the kit APK (SHA-256)'
check ((sh 'cmd role get-role-holders android.app.role.HOME').Trim() -eq $Tvhome) 'Home role still tvhome'
check ((sh "dumpsys activity activities | grep -m1 topResumedActivity") -match [regex]::Escape($Tvhome)) 'HOME shows tvhome'
check ((sh "dumpsys accessibility | grep -m1 -o 'capabilities=[0-9]*'") -match 'capabilities=8') 'HomeKeyService bound (capabilities=8)'
check ((setting 'screensaver_components') -eq $Dream) 'screensaver = tvhome ClockDream'
if ($ok) {
    say "`nDONE. Next: YouTube button on the real remote, a reboot test, then turn Wireless debugging off."
} else {
    say "`nSOME CHECKS FAILED (Home is untouched). Undo: .\Apply-TvhomeKit.ps1 -Alias $Alias -Rollback -ConfirmText 'CONFIRM ROLLBACK $Alias'"
}
say "Backup: $bk"
