param(
  [string]$ProjectRoot,
  [string]$CodexHomePath,
  [switch]$ReinstallPrompts,
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$CodexArgs
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

function Test-WritableDirectory([string]$path) {
  try {
    New-Item -ItemType Directory -Path $path -Force | Out-Null
    $probe = Join-Path $path '.write-test'
    Set-Content -Path $probe -Value 'ok' -Encoding ascii -ErrorAction Stop
    Remove-Item -Path $probe -Force -ErrorAction Stop
    return $true
  }
  catch {
    return $false
  }
}

if ([string]::IsNullOrWhiteSpace($CodexHomePath)) {
  $preferred = Join-Path $ProjectRoot '.codex'
  if (Test-WritableDirectory $preferred) {
    $CodexHomePath = $preferred
  }
  else {
    $fallback = Join-Path $ProjectRoot '.codex-home'
    New-Item -ItemType Directory -Path $fallback -Force | Out-Null
    $CodexHomePath = $fallback
    Write-Host "Falling back to writable CODEX_HOME: $CodexHomePath"
  }
}

$installerScript = Join-Path $PSScriptRoot 'install-bmad-prompts.ps1'
if (-not (Test-Path $installerScript)) {
  throw "Missing installer script: $installerScript"
}

$promptDir = Join-Path $CodexHomePath 'prompts'
$helpPrompt = Join-Path $promptDir 'bmad-help.md'

if ($ReinstallPrompts -or -not (Test-Path $helpPrompt)) {
  & $installerScript -ProjectRoot $ProjectRoot -CodexHomePath $CodexHomePath | Out-Host
}
else {
  Write-Host "Using existing BMAD prompts from $promptDir"
}

$env:CODEX_HOME = $CodexHomePath

$codexCmd = Join-Path $env:APPDATA 'npm\codex.cmd'
if (-not (Test-Path $codexCmd)) {
  throw "Codex CLI not found at: $codexCmd"
}

Write-Host "Launching Codex with CODEX_HOME=$CodexHomePath"
& $codexCmd @CodexArgs
