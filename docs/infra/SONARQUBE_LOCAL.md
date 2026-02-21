# SonarQube Local Bootstrap (Docker)

## Prerequisites
- Docker Desktop running.
- Free local port `9000`.
- At least `4 GB` RAM available for Docker.
- Admin token creation right in SonarQube UI (default admin account at first start).

## Start
```bash
docker compose -f docker-compose.sonar.yml up -d
```

## Verify
1. Check containers:
```bash
docker compose -f docker-compose.sonar.yml ps
```
2. Check system status:
```bash
curl http://localhost:9000/api/system/status
```
Expected result: JSON with `"status":"UP"` (it can be `"STARTING"` for a few minutes after first boot).
3. Open UI:
- URL: `http://localhost:9000`
- Default login: `admin` / `admin` (password change required on first login).

## Generate Admin Token
1. Login to SonarQube UI.
2. Go to `My Account` -> `Security` -> `Generate Tokens`.
3. Create a token named `fortnite-local-ci`.
4. Store it in your local shell/session for next tickets (`JIRA-INFRA-003` and `JIRA-INFRA-004`).

## Stop
```bash
docker compose -f docker-compose.sonar.yml down
```

## Reset (remove all Sonar data)
```bash
docker compose -f docker-compose.sonar.yml down -v
```
