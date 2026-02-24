param(
    [string]$SonarHostUrl = "http://localhost:9000",
    [string]$ProjectKey = "fortnite-pronos-backend",
    [string]$ProjectName = "Fortnite Pronos Backend"
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

mvn `
    "-DskipTests" `
    "org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar" `
    "-Dsonar.host.url=$SonarHostUrl" `
    "-Dsonar.token=$env:SONAR_TOKEN" `
    "-Dsonar.projectKey=$ProjectKey" `
    "-Dsonar.projectName=$ProjectName"

exit $LASTEXITCODE
