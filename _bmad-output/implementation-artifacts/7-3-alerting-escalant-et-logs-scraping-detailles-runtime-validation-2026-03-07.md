# Story 7.3 Runtime Validation

Story: `7-3-alerting-escalant-et-logs-scraping-detailles`
Date: `2026-03-07`
Status: `closed`

## Observation

- Le flow admin authentifie retournait d'abord `403` sur `GET /api/admin/scraping/logs` et `GET /api/admin/scraping/alert`.
- Apres correction auth/bean wiring, les endpoints repondaient bien `200`, mais sans run visible dans les logs.
- Le declencheur local `POST /api/ingestion/pr/csv` retournait alors `500`, ce qui empechait de prouver la story 7.3 avec un vrai log de scraping non vide.

## Root Cause

- `CustomUserDetailsService` construisait le `UserDetails.username` avec l'email alors que le JWT admin etait emis avec le username canonique.
- `UnresolvedAlertService` exposait deux constructeurs et Spring ne selectionnait pas explicitement le bon en runtime.
- La persistance `PrSnapshot` cassait en PostgreSQL sur le type enum `pr_region` :
  - lookup composite Hibernate 6.6 compare `pr_region = varchar`
  - insertion JPA bindait `region` comme `varchar` sans cast

## Resolution

- `src/main/java/com/fortnite/pronos/service/CustomUserDetailsService.java`
  - alignement du username Spring Security sur le username canonique
- `src/main/java/com/fortnite/pronos/service/admin/UnresolvedAlertService.java`
  - constructeur runtime principal annote `@Autowired`
- `src/main/java/com/fortnite/pronos/repository/PrSnapshotRepository.java`
  - ajout d'un lookup de business key compatible PostgreSQL (`CAST(:region AS pr_region)`)
- `src/main/java/com/fortnite/pronos/repository/PrSnapshotRepositoryCustom.java`
  - ajout d'un point d'extension de persistance cible
- `src/main/java/com/fortnite/pronos/repository/PrSnapshotRepositoryImpl.java`
  - insertion native `INSERT ... CAST(:region AS pr_region)` pour eviter le bind JPA invalide
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessor.java`
  - upsert `PrSnapshot` bascule sur lookup metier + insert natif pour les nouveaux snapshots
- `docs/ingestion/pr_local.md`
  - la procedure locale documente maintenant le JWT admin requis

## Validation

- `mvn -q -Dtest="CustomUserDetailsServiceTest,UnresolvedAlertServiceTest,UnresolvedAlertServiceContextTest,AdminScrapeControllerTest,SecurityConfigAdminScrapeAuthorizationTest" test` -> OK
- `mvn -q -Dtest="PrSnapshotRepositoryTest,PrIngestionServiceTddTest,PrIngestionServiceRuntimePortsTest,PrIngestionRowProcessorUnresolvedTest,PrIngestionRowProcessorAliasTest" test` -> OK
- `POST /api/auth/login` avec `username=admin`, `password=Admin1234` -> `200 OK`
- `GET /api/admin/scraping/logs?limit=5` -> `200 OK`
- `GET /api/admin/scraping/alert` -> `200 OK`
- `POST /api/ingestion/pr/csv?source=LOCAL_PR&season=2025&writeScores=true` avec JWT admin -> `200 OK`
- Reponse ingestion observee:
  - `status=SUCCESS`
  - `playersCreated=4`
  - `playersUpdated=1`
  - `snapshotsWritten=6`
  - `scoresWritten=6`
- `GET /api/admin/scraping/logs?limit=5` apres ingestion -> `200 OK`
  - premiere entree: `source=LOCAL_PR`, `status=SUCCESS`, `totalRowsWritten=6`
- `GET /api/admin/scraping/alert` apres ingestion -> `200 OK`
  - `level=NONE`
  - `unresolvedCount=4`

## BMAD Handling

- La story 7.3 reste `done`, mais elle est maintenant revalidee en runtime local avec un log non vide issu d'un vrai run.
- Le guide d'ingestion locale est aligne sur la securite reelle de l'application.
- Aucun chantier hebergement/staging n'est rouvert.
