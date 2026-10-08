# GET public catalog files with the same UA the APK uses.
param(
    [string]$Base = "https://raw.githubusercontent.com/b1nhm1nh/xiaomi-toolbox-public/main/Toolbox-data/"
)

$ErrorActionPreference = "Stop"
$files = @("Pif-props.json", "device-model.json", "app-props.json", "date-update.txt")
$ua = "Xiaomi-Toolbox/1.0"
foreach ($f in $files) {
    $url = $Base + $f
    $r = Invoke-WebRequest -Uri $url -Headers @{ "User-Agent" = $ua } -UseBasicParsing
    if ($r.StatusCode -ne 200 -or $r.RawContentLength -lt 1) {
        throw "FAIL $url status=$($r.StatusCode) len=$($r.RawContentLength)"
    }
    Write-Host ("OK {0,-20} {1,8} bytes" -f $f, $r.RawContentLength)
}
Write-Host "catalog-verify done"
