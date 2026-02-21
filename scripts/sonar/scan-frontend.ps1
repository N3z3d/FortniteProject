param(
    [string]$SonarHostUrl = "http://host.docker.internal:9000",
    [string]$ScannerImage = "sonarsource/sonar-scanner-cli:11.1",
    [switch]$EnableScm,
    [switch]$SkipCoverage
)

if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    Write-Error "SONAR_TOKEN is required. Generate a token in SonarQube and export it first."
    exit 1
}

if (-not $SkipCoverage) {
    npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --code-coverage `
        --include="src/app/core/services/translation.service.spec.ts" `
        --include="src/app/core/services/ui-error-feedback.service.spec.ts" `
        --include="src/app/shared/components/main-layout/main-layout.component.spec.ts"

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
