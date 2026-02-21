param(
    [string]$SonarHostUrl = "http://localhost:9000",
    [string]$ProjectKey = "fortnite-pronos-backend",
    [string]$ProjectName = "Fortnite Pronos Backend"
)

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
