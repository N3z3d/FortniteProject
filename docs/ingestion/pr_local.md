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

Exemple PowerShell:
```powershell
$csv = Get-Content -Raw src\main\resources\data\pr_sample.csv
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/ingestion/pr/csv?source=LOCAL_PR&season=2025&writeScores=true" `
  -Body $csv `
  -ContentType "text/csv"
```

Exemple curl:
```bash
curl -X POST "http://localhost:8080/api/ingestion/pr/csv?source=LOCAL_PR&season=2025&writeScores=true" \
  -H "Content-Type: text/csv" \
  --data-binary @src/main/resources/data/pr_sample.csv
```

## Notes
- Les lignes `GLOBAL` creent un joueur en `UNKNOWN` si besoin.
- Une region classique remplace `UNKNOWN`, `GLOBAL` ne degrade jamais une region connue.
- Le status retourne est `SUCCESS`, `PARTIAL` ou `FAILED` (voir `ingestion_runs`).
- Voir `docs/ingestion/score_model.md` pour le modele de score annuel.
