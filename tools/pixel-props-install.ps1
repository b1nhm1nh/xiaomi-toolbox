# Download a Pixel Build Prop zip (GitHub), inject ADB-safe sensitive flags, ksud install.
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$Codename = "Comet",
    [string]$Tag = "",
    [string]$WorkDir = ""
)

$ErrorActionPreference = "Stop"
if (-not $WorkDir) {
    $WorkDir = Join-Path $env:TEMP ("pbp-" + [guid]::NewGuid().ToString("n").Substring(0, 8))
}
New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

$api = "https://api.github.com/repos/Elcapitanoe/Build-Prop-BETA/releases"
$releases = Invoke-RestMethod -Uri $api -Headers @{ "User-Agent" = "Xiaomi-Toolbox/1.0" }
$asset = $null
$usedTag = $Tag
foreach ($rel in $releases) {
    if ($Tag -and $rel.tag_name -ne $Tag) { continue }
    $hit = $rel.assets | Where-Object { $_.name -like "${Codename}_*" -and $_.name -like "*.zip" } | Select-Object -First 1
    if ($hit) {
        $asset = $hit
        $usedTag = $rel.tag_name
        break
    }
    if ($Tag) { break }
}
if (-not $asset) { throw "No zip for codename=$Codename tag=$Tag" }

$zipPath = Join-Path $WorkDir $asset.name
Write-Host "Downloading $($asset.browser_download_url) (tag $usedTag)"
curl.exe -L --fail -A "Mozilla/5.0" -o $zipPath $asset.browser_download_url
if (-not (Test-Path $zipPath)) { throw "download failed" }

$extract = Join-Path $WorkDir "extract"
if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }
Expand-Archive -Path $zipPath -DestinationPath $extract -Force
$sys = Join-Path $extract "system.prop"
if (-not (Test-Path $sys)) { throw "zip missing system.prop" }
$flags = @(
    "pixelprops.sensitive.device=true",
    "pixelprops.sensitive.security_patch=true",
    "pixelprops.sensitive.soc=true",
    "pixelprops.sensitive.sdk=true",
    "pixelprops.sensitive.props=true",
    "pixelprops.sensitive.pihooks=true"
)
$raw = Get-Content $sys -Raw
foreach ($line in $flags) {
    $key = $line.Split("=")[0]
    if ($raw -notmatch [regex]::Escape($key)) {
        $raw = "$line`n$raw"
    }
}
Set-Content -Path $sys -Value $raw -NoNewline -Encoding utf8

$cfg = Join-Path $extract "config.prop"
if (Test-Path $cfg) {
    $c = Get-Content $cfg -Raw
    foreach ($line in $flags) {
        $key = $line.Split("=")[0]
        if ($c -match [regex]::Escape($key)) {
            $c = [regex]::Replace($c, "(?m)^[#[\s]*" + [regex]::Escape($key) + "=.*$", $line)
        } else {
            $c = "$c`n$line"
        }
    }
    Set-Content -Path $cfg -Value $c -NoNewline -Encoding utf8
}

$outZip = Join-Path $WorkDir ("patched-" + $asset.name)
if (Test-Path $outZip) { Remove-Item -Force $outZip }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($extract, $outZip)

$remote = "/data/local/tmp/" + $asset.name
adb -s $Serial push $outZip $remote
if ($LASTEXITCODE -ne 0) { throw "adb push failed" }
adb -s $Serial shell "/data/adb/ksu/bin/ksud module install $remote"
Write-Host "ksud exit=$LASTEXITCODE (255 after 'Module installed successfully!' is often leftover resetprop)"
adb -s $Serial shell "/data/adb/ksu/bin/ksud module list"
Write-Host "Reboot required. Work dir: $WorkDir"
