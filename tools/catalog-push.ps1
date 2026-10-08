# Sync Toolbox-data to the public catalog repo and push both remotes.
param(
    [string]$AppRepo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$PublicRepo = "D:\w\git\tools-org\xiaomi-toolbox-public",
    [string]$Message = "Update Toolbox-data catalog."
)

$ErrorActionPreference = "Stop"
$src = Join-Path $AppRepo "Toolbox-data"
if (-not (Test-Path $src)) { throw "Missing $src" }

if (-not (Test-Path (Join-Path $PublicRepo ".git"))) {
    New-Item -ItemType Directory -Force -Path (Split-Path $PublicRepo) | Out-Null
    git clone https://github.com/b1nhm1nh/xiaomi-toolbox-public.git $PublicRepo
}

$dst = Join-Path $PublicRepo "Toolbox-data"
New-Item -ItemType Directory -Force -Path $dst | Out-Null
Copy-Item -Path (Join-Path $src "*") -Destination $dst -Force

function Commit-IfDirty([string]$Repo, [string]$Msg) {
    git -C $Repo add -A
    $st = git -C $Repo status --porcelain
    if (-not $st) {
        Write-Host "$Repo clean"
        return
    }
    git -C $Repo commit -m "$Msg`n`nCo-Authored-By: Claude Code <noreply@anthropic.com>"
    git -C $Repo push
}

Commit-IfDirty $PublicRepo $Message
Commit-IfDirty $AppRepo $Message
Write-Host "catalog-push done"
