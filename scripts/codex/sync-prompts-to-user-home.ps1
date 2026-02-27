param(
  [string]$ProjectRoot,
  [string]$CodexHomePath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
  $scriptPath = $MyInvocation.MyCommand.Path
  if ([string]::IsNullOrWhiteSpace($scriptPath)) {
    throw 'Cannot resolve script path to infer project root.'
  }

  $scriptDir = Split-Path -Parent $scriptPath
  $ProjectRoot = (Resolve-Path (Join-Path $scriptDir '..\..')).Path
}

if ([string]::IsNullOrWhiteSpace($CodexHomePath)) {
  $candidate = Join-Path $ProjectRoot '.codex'
  $probeOk = $true
  try {
    New-Item -ItemType Directory -Path $candidate -Force | Out-Null
    $probe = Join-Path $candidate '.write-test'
    Set-Content -Path $probe -Value 'ok' -Encoding ascii -ErrorAction Stop
    Remove-Item -Path $probe -Force -ErrorAction Stop
  }
  catch {
    $probeOk = $false
  }

  if ($probeOk) {
    $CodexHomePath = $candidate
  }
  else {
    $CodexHomePath = Join-Path $ProjectRoot '.codex-home'
    New-Item -ItemType Directory -Path $CodexHomePath -Force | Out-Null
  }
}

$sourceDir = Join-Path $CodexHomePath 'prompts'
$helpPrompt = Join-Path $sourceDir 'bmad-help.md'
$installerScript = Join-Path $PSScriptRoot 'install-bmad-prompts.ps1'

if (-not (Test-Path $helpPrompt)) {
  & $installerScript -ProjectRoot $ProjectRoot -CodexHomePath $CodexHomePath | Out-Host
}

$targetDir = Join-Path $env:USERPROFILE '.codex\prompts'
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

$copied = 0
Get-ChildItem -Path $sourceDir -Filter 'bmad-*.md' -File |
  ForEach-Object {
    Copy-Item -Path $_.FullName -Destination (Join-Path $targetDir $_.Name) -Force
    $copied += 1
  }

Write-Output "Copied $copied BMAD prompt files to $targetDir"
