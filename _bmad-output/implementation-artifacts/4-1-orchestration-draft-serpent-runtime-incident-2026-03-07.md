# Story 4.1 Runtime Incident Addendum

Story: `4-1-orchestration-draft-serpent`
Date: `2026-03-07`
Status: `closed`

## Incident

- Signalement local UI: `Une erreur technique est survenue. Reessayez dans quelques instants.`
- Reproduction backend: `POST /api/games/{gameId}/start-draft` retournait `500`.

## Root Cause

- La table `drafts` impose `season INTEGER NOT NULL`.
- L'entite JPA `com.fortnite.pronos.model.Draft` ne mappait pas `season`.
- Lors du `startDraft`, Hibernate tentait un insert avec `season = null`, provoquant une `ConstraintViolationException`.

## Resolution

- `src/main/java/com/fortnite/pronos/model/Draft.java`
  - ajout du mapping `season`
  - synchronisation automatique depuis `Game.currentSeason`
  - fallback sur l'annee courante si la saison de la game est absente ou invalide
- `src/test/java/com/fortnite/pronos/model/DraftModelTest.java`
  - couverture des cas nominal + fallbacks
- `src/test/java/com/fortnite/pronos/service/draft/DraftServiceTddTest.java`
  - assertions de propagation de `season` sur create/start draft

## Validation

- `mvn -q -Dtest="DraftModelTest,DraftServiceTddTest,GameDraftServiceDomainMigrationTest" test` -> OK
- `docker compose -f docker-compose.local.yml up -d --build app` -> OK
- `POST /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/start-draft?user=thibaut` -> `200 OK`

## BMAD Handling

- Incident traite comme follow-up runtime sur une story `done`, car le bug casse un comportement central deja livre.
- Pas de nouvelle story d'hebergement creee.
- La story 4.1 reste `done` apres correction et revalidation locale.
