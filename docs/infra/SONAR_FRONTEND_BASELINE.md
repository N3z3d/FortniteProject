# SonarQube Frontend Baseline (`JIRA-INFRA-004`)

Date: 2026-02-18
Project key: `fortnite-pronos-frontend`
Dashboard: `http://localhost:9000/dashboard?id=fortnite-pronos-frontend`

## Execution command

```powershell
$env:SONAR_TOKEN = "<token>"
.\scripts\sonar\scan-frontend.ps1
```

The script does:
1. Run targeted Angular tests with coverage (`ChromeHeadless`).
2. Normalize `lcov.info` paths for Linux scanner container.
3. Launch `sonarsource/sonar-scanner-cli:11.1` in Docker (Node 18 runtime).

## Baseline metrics

| Metric | Value |
|---|---:|
| `bugs` | 4 |
| `vulnerabilities` | 0 |
| `code_smells` | 13 |
| `coverage` | 34.2% |
| `duplicated_lines_density` | 0.3% |
| `reliability_rating` | D |
| `security_rating` | A |
| `maintainability_rating` (`sqale_rating`) | A |
| `ncloc` | 25045 |

Quality gate status at baseline: `OK`.

## Known warnings (non-blocking)
- `JIRA-INFRA-006` a retire les warnings `module: preserve` et LCOV unresolved-path.
- `JIRA-INFRA-007` a retire les warnings Node runtime non recommande et les deprecations Sass `@import`.
- Le mode local par defaut desactive SCM (`sonar.scm.disabled=true`), donc pas de warnings blame/timestamp attendus.
- Si vous lancez `-EnableScm`, un workspace dirty peut encore produire des warnings SCM.
