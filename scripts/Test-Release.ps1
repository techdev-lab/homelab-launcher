# Read-only check of a downloaded release APK: its SHA-256 against SHA256SUMS.txt, then its signing certificate.
#   powershell -ExecutionPolicy Bypass -File .\Test-Release.ps1 -Apk .\HomeLab-Launcher-3.0.0.apk
# Exit codes: 0 OK, 1 sha256, 2 listed, 3 signer, 4 tool, 5 input. Writes no file, makes no network call.
param(
    [string]$Apk,
    [string]$Sums,
    # SHA-256 of the project release certificate. Public information, not a secret.
    [string]$ExpectedSigner = '14849bdb3ed13523f75eede24e6b8cc36270d3a6f8f4bcb86f454311c616affe',
    [string]$ApkSigner
)
Set-StrictMode -Version 2

$maxBytes = 67108864

function Fail([int]$code, [string]$check, [string]$reason) {
    Write-Host "FAIL ${check}: $reason"
    Write-Host "RESULT: FAILED ($code)"
    exit $code
}

function Find-ApkSigner {
    if ($env:AL_APKSIGNER) { return $env:AL_APKSIGNER }
    if ($env:ANDROID_HOME) {
        $newest = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_HOME 'build-tools') -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'apksigner.bat') -PathType Leaf } |
            Sort-Object { [version]($_.Name -replace '-.*$', '') } -Descending |
            Select-Object -First 1
        if ($newest) { return (Join-Path $newest.FullName 'apksigner.bat') }
    }
    $cmd = Get-Command 'apksigner.bat' -CommandType Application -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

$name = if ($Apk) { Split-Path -Leaf $Apk } else { '(none)' }
Write-Host "Test-Release: $name"

# input
if (-not $Apk -or -not (Test-Path -LiteralPath $Apk)) { Fail 5 'input' 'APK file not found' }
$item = Get-Item -LiteralPath $Apk
if ($item.PSIsContainer) { Fail 5 'input' 'APK path is a directory' }
if ($item.Extension -ne '.apk') { Fail 5 'input' 'file name must end in .apk' }
if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { Fail 5 'input' 'reparse point (symlink) refused' }
if ($item.Length -gt $maxBytes) { Fail 5 'input' 'file larger than 64 MiB' }
if (-not $Sums) { $Sums = Join-Path (Split-Path -Parent $item.FullName) 'SHA256SUMS.txt' }
if (-not (Test-Path -LiteralPath $Sums -PathType Leaf)) { Fail 5 'input' 'sums file not found' }

# listed
$listed = $null
foreach ($line in Get-Content -LiteralPath $Sums) {
    $t = $line.TrimEnd([char]13)
    if ($t -match '^([0-9A-Fa-f]{64}) [ *](.+)$') {
        $listName = Split-Path -Leaf $Matches[2]
        if ($listName -ieq $item.Name) { $listed = $Matches[1].ToLower(); break }
    }
}
if (-not $listed) { Fail 2 'listed' "no valid line in $(Split-Path -Leaf $Sums) names this APK" }

# sha256
$actual = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLower()
if ($actual -ne $listed) { Fail 1 'sha256' 'hash differs from the listed value' }
Write-Host "PASS sha256 $($actual.Substring(0, 12))"

# tool
if (-not $ApkSigner) { $ApkSigner = Find-ApkSigner }
if (-not $ApkSigner -or -not (Test-Path -LiteralPath $ApkSigner -PathType Leaf)) { Fail 4 'tool' 'apksigner not found' }
$errorPref = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$out = & $ApkSigner 'verify' '--print-certs' $item.FullName 2>&1 | Out-String
$code = $LASTEXITCODE
$ErrorActionPreference = $errorPref
if ($code -ne 0) {
    if ($out -match 'DOES NOT VERIFY') { Fail 3 'signer' 'apksigner says the APK does not verify' }
    Fail 4 'tool' "apksigner exited with $code"
}

# signer
$digests = @()
foreach ($line in ($out -split "`r?`n")) {
    # "Signer #1 certificate SHA-256 digest: ..." (older apksigner) or "V2 Signer: certificate SHA-256 digest: ..." (newer).
    if ($line -match 'certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$') { $digests += $Matches[1].ToLower() }
}
# The same certificate listed under several signature schemes (V2, V3) counts once.
$digests = @($digests | Select-Object -Unique)
if ($digests.Count -ne 1) { Fail 3 'signer' "expected 1 signer certificate, found $($digests.Count)" }
$expected = ($ExpectedSigner -replace '[: ]', '').ToLower()
if ($digests[0] -ne $expected) { Fail 3 'signer' 'certificate differs from the project certificate' }
Write-Host "PASS signer $($digests[0].Substring(0, 12))"
Write-Host 'RESULT: OK'
exit 0
