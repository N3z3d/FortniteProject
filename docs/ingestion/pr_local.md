# PR ingestion (local)

Objectif: charger des snapshots PR dans la base locale (H2 ou Postgres local).

## Pre-requis
- Profil `dev`, `h2` ou `test`
- App demarree (ex: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`)
- `ingestion.local.enabled=true` dans le profil local

## Dataset sample
- Fichier: `src/main/resources/data/pr_sample.csv`
- Format CSV:
  - `nickname,region,points,rank,snapshot_date`
  - `snapshot_date` au format `YYYY-MM-DD`

## Lancer l ingestion via endpoint (dev/h2/test uniquement)
Endpoint:
`POST /api/ingestion/pr/csv`

Authentification:
- En runtime local actuel, tout `/api/**` est protege.
- Utiliser un JWT admin valide avant l'appel d'ingestion.
- Un appel anonyme retournera `403`.

Exemple PowerShell:
```powershell
$loginBody = @{ username = "admin"; password = "Admin1234" } | ConvertTo-Json
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }
$csv = Get-Content -Raw src\main\resources\data\pr_sample.csv
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/ingestion/pr/csv?source=LOCAL_PR&season=2025&writeScores=true" `
  -Headers $headers `
  -Body $csv `
  -ContentType "text/csv"
```

Exemple curl:
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234"}' | jq -r '.token')

curl -X POST "http://localhost:8080/api/ingestion/pr/csv?source=LOCAL_PR&season=2025&writeScores=true" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: text/csv" \
  --data-binary @src/main/resources/data/pr_sample.csv
```

## Notes
- Les lignes `GLOBAL` creent un joueur en `UNKNOWN` si besoin.
- Une region classique remplace `UNKNOWN`, `GLOBAL` ne degrade jamais une region connue.
- Le status retourne est `SUCCESS`, `PARTIAL` ou `FAILED` (voir `ingestion_runs`).
- Voir `docs/ingestion/score_model.md` pour le modele de score annuel.
