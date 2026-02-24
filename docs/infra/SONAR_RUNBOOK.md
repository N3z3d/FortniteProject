# SonarQube Runbook (Team)

## 1) Start local Sonar
```powershell
docker compose -f docker-compose.sonar.yml up -d
```
Check status:
```powershell
docker compose -f docker-compose.sonar.yml ps
Invoke-WebRequest -UseBasicParsing http://localhost:9000/api/system/status
```

## 2) Generate token
1. Open `http://localhost:9000`.
2. Login (`admin/admin` by default local setup).
3. `My Account` -> `Security` -> `Generate Tokens`.
4. Export token for current shell:
```powershell
$env:SONAR_TOKEN = "<your-token>"
```

## 3) Run backend scan
```powershell
.\scripts\sonar\scan-backend.ps1
```

## 4) Run frontend scan + coverage import
```powershell
.\scripts\sonar\scan-frontend.ps1
```
This command runs the full frontend test suite (`ng test --code-coverage`) before uploading analysis.

Default scanner image is pinned to `sonarsource/sonar-scanner-cli:11.1` (Node 18) to avoid non-recommended Node runtime warnings on SonarQube 9.9.
By default, local scan disables SCM blame import (`-Dsonar.scm.disabled=true`) to avoid non-actionable local warnings on dirty workspaces.
If you need SCM blame data in a clean workspace, run:
```powershell
.\scripts\sonar\scan-frontend.ps1 -EnableScm
```

## 5) Configure quality gate
```powershell
.\scripts\sonar\configure-quality-gate.ps1
```
Default gate: `Fortnite Local Gate` with conditions on new code:
- blocker violations = 0
- critical violations = 0
- reliability rating = A
- security rating = A
- maintainability rating = A
- coverage >= 30%
- duplicated lines density <= 5%

## 6) Verify dashboards
- Backend: `http://localhost:9000/dashboard?id=fortnite-pronos-backend`
- Frontend: `http://localhost:9000/dashboard?id=fortnite-pronos-frontend`

## Troubleshooting
- Token/auth errors (`401/403`):
  - Regenerate token and update `$env:SONAR_TOKEN`.
- `localhost:9000` unavailable:
  - Check container health with `docker compose -f docker-compose.sonar.yml ps`.
- Docker memory issues:
  - Allocate at least `4 GB` RAM in Docker Desktop.
- Frontend scanner coverage warning:
  - Ensure `frontend/coverage/frontend/lcov.info` exists after `scan-frontend.ps1`.
- Slow/failing scanner in Docker:
  - Re-run with clean state: `docker compose -f docker-compose.sonar.yml down -v` then `up -d`.
