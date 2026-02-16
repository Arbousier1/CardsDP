param(
    [Parameter(Mandatory = $false)]
    [string]$Repo = $env:GITHUB_REPOSITORY,

    [Parameter(Mandatory = $false)]
    [string]$Token = $env:GITHUB_TOKEN,

    [Parameter(Mandatory = $false)]
    [string]$Message = "docs: sync wiki"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Repo)) {
    throw "Missing repo. Use -Repo 'owner/repo' or set GITHUB_REPOSITORY."
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "Missing token. Use -Token '<PAT>' or set GITHUB_TOKEN."
}

$source = Join-Path $PSScriptRoot "..\docs\wiki"
$source = (Resolve-Path $source).Path
if (-not (Test-Path $source)) {
    throw "Missing wiki source folder: $source"
}

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("cardsdp-wiki-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    $wikiRepo = "https://x-access-token:$Token@github.com/$Repo.wiki.git"
    git clone $wikiRepo $tempRoot | Out-Null

    Get-ChildItem -Path $tempRoot -Force |
        Where-Object { $_.Name -ne ".git" } |
        Remove-Item -Recurse -Force

    Copy-Item -Path (Join-Path $source "*") -Destination $tempRoot -Recurse -Force

    $status = git -C $tempRoot status --porcelain
    if ([string]::IsNullOrWhiteSpace($status)) {
        Write-Host "No wiki changes to publish."
        exit 0
    }

    git -C $tempRoot config user.name "cardsdp-bot" | Out-Null
    git -C $tempRoot config user.email "cardsdp-bot@users.noreply.github.com" | Out-Null

    git -C $tempRoot add . | Out-Null
    git -C $tempRoot commit -m $Message | Out-Null
    git -C $tempRoot push origin master | Out-Null

    Write-Host "Wiki published: $Repo.wiki.git"
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
