# Get-DeviceInventory.ps1
# READ-ONLY inventory of one Android TV, to fill its config.json before the launcher goes on it.
# Changes nothing on the device. Prints no serial, MAC or full getprop.
# The address comes from an env var, never from this file:
#   $env:AL_TV = "<ip:port>"      (the Wireless debugging port shown on the TV)
#   powershell -ExecutionPolicy Bypass -File .\scripts\Get-DeviceInventory.ps1
# Output also goes to %USERPROFILE%\.homelab-launcher\inventory\<model>_<date>.txt.

$ErrorActionPreference = 'Stop'
if (-not $env:AL_TV) { throw 'Set $env:AL_TV = "<ip:port>" first.' }
$adb = $env:AL_ADB
if (-not $adb) { $adb = (Get-Command adb -ErrorAction SilentlyContinue).Source }
if (-not $adb) { $adb = Join-Path $env:USERPROFILE 'dev\platform-tools\platform-tools\adb.exe' }
if (-not (Test-Path $adb)) { throw "adb not found. Set `$env:AL_ADB to adb.exe." }

& $adb connect $env:AL_TV | Out-Null
function sh([string]$c) { (& $adb -s $env:AL_TV shell $c) -join "`n" }

$model = sh 'getprop ro.product.model'
$lines = @(
  "date        : $(Get-Date -Format s)",
  "model       : $model",
  "codename    : $(sh 'getprop ro.product.device')",
  "android     : $(sh 'getprop ro.build.version.release') (sdk $(sh 'getprop ro.build.version.sdk'))",
  "build       : $(sh 'getprop ro.build.display.id')",
  "patch       : $(sh 'getprop ro.build.version.security_patch')",
  "timezone    : $(sh 'getprop persist.sys.timezone')",
  "ui size     : $((sh 'wm size') -replace "`n", ' | ')",
  "home now    : $(sh 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1')",
  "tvhome      : $(sh 'dumpsys package com.gathin.tvhome | grep -m1 versionName')",
  "",
  "== HOME candidates",
  (sh 'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME'),
  "",
  "== third-party apps (pick dock_apps from here)",
  (sh 'pm list packages -3'),
  "",
  "== disabled packages",
  (sh 'pm list packages -d')
)
$lines | ForEach-Object { $_ }

$outDir = Join-Path $env:USERPROFILE '.homelab-launcher\inventory'
New-Item -ItemType Directory -Force $outDir | Out-Null
$safe = ($model -replace '[^A-Za-z0-9_-]', '_')
$out = Join-Path $outDir ("{0}_{1}.txt" -f $safe, (Get-Date -Format 'yyyyMMdd-HHmm'))
$lines | Set-Content -Path $out -Encoding utf8
Write-Host "`nSaved: $out"
