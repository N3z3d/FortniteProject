param(
    [string]$SonarHostUrl = "http://host.docker.internal:9000",
    [string]$ScannerImage = "sonarsource/sonar-scanner-cli:11.1",
    [switch]$EnableScm,
    [switch]$SkipCoverage
)

function Initialize-SonarTokenFromDotEnv {
    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return
    }

    $repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
    $envFile = Join-Path $repoRoot ".env"
    if (-not (Test-Path $envFile)) {
        return
    }

    $tokenLine = Get-Content $envFile | Where-Object { $_ -match '^\s*SONAR_TOKEN\s*=' } | Select-Object -First 1
    if (-not $tokenLine) {
        return
    }

    $tokenValue = ($tokenLine -replace '^\s*SONAR_TOKEN\s*=\s*', '').Trim()
    if ($tokenValue.StartsWith('"') -and $tokenValue.EndsWith('"') -and $tokenValue.Length -ge 2) {
        $tokenValue = $tokenValue.Substring(1, $tokenValue.Length - 2)
    } elseif ($tokenValue.StartsWith("'") -and $tokenValue.EndsWith("'") -and $tokenValue.Length -ge 2) {
        $tokenValue = $tokenValue.Substring(1, $tokenValue.Length - 2)
    }

    if (-not [string]::IsNullOrWhiteSpace($tokenValue)) {
        $env:SONAR_TOKEN = $tokenValue
    }
}

Initialize-SonarTokenFromDotEnv

if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    Write-Error "SONAR_TOKEN is required. Generate a token in SonarQube and export it first."
    exit 1
}

if (-not $SkipCoverage) {
    npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --code-coverage

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $lcovPath = Join-Path "frontend" "coverage/frontend/lcov.info"
    if (Test-Path $lcovPath) {
        $lcovContent = Get-Content -Raw $lcovPath
        $lcovContent = $lcovContent -replace 'SF:frontend\\', 'SF:'
        $lcovContent = $lcovContent -replace '\\', '/'
        $records = $lcovContent -split "end_of_record`r?`n"
        $filteredRecords = $records | Where-Object {
            $_ -notmatch 'SF:src/environments/'
        }
        $normalized = ($filteredRecords -join "end_of_record`n").Trim()
        if ($normalized.Length -gt 0 -and -not $normalized.EndsWith("end_of_record")) {
            $normalized = "$normalized`nend_of_record`n"
        }
        Set-Content -Path $lcovPath -Value $normalized -Encoding UTF8
    }
}

$repoPath = (Resolve-Path ".").Path
$scannerArgs = @()
if (-not $EnableScm) {
    $scannerArgs += "-Dsonar.scm.disabled=true"
}

docker run --rm `
    -e SONAR_HOST_URL=$SonarHostUrl `
    -e SONAR_TOKEN=$env:SONAR_TOKEN `
    -v "${repoPath}:/workspace" `
    -w /workspace/frontend `
    $ScannerImage `
    @scannerArgs

exit $LASTEXITCODE
