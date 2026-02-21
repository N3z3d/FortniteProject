# SonarQube Backend Baseline (`JIRA-INFRA-003`)

Date: 2026-02-18
Project key: `fortnite-pronos-backend`
Dashboard: `http://localhost:9000/dashboard?id=fortnite-pronos-backend`

## Execution command

```powershell
$env:SONAR_TOKEN = "<token>"
.\scripts\sonar\scan-backend.ps1
```

Equivalent direct command:

```powershell
mvn "-DskipTests" `
  "org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar" `
  "-Dsonar.host.url=http://localhost:9000" `
  "-Dsonar.token=$env:SONAR_TOKEN" `
  "-Dsonar.projectKey=fortnite-pronos-backend" `
  "-Dsonar.projectName=Fortnite Pronos Backend"
```

## Baseline metrics

| Metric | Value |
|---|---:|
| `bugs` | 41 |
| `vulnerabilities` | 2 |
| `code_smells` | 438 |
| `coverage` | 56.1% |
| `duplicated_lines_density` | 2.0% |
| `reliability_rating` | D |
| `security_rating` | E |
| `maintainability_rating` (`sqale_rating`) | A |
| `ncloc` | 22056 |

Quality gate status at baseline: `OK`.

## Notes
- SonarQube must be running locally (`docker compose -f docker-compose.sonar.yml up -d`).
- Baseline is intentionally non-blocking and used as the starting point for `JIRA-INFRA-005` quality gate tuning.
