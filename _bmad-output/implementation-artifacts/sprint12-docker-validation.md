# Story Sprint 12 — F: Docker Rebuild + Validation des 7 Fixes

Status: ready-for-dev

<!-- METADATA
  story_key: sprint12-docker-validation
  sprint: Sprint 12
  priority: P0 — valider avant tout le reste
-->

## Story

As Thibaut (développeur + premier utilisateur),
I want to rebuild the Docker image with all Sprint 12 fixes and validate them in real conditions,
so that I know the stabilisation actually works before testing features.

## Acceptance Criteria

1. Docker image rebuilt avec les commits Sprint 12 (ws-lifecycle, delete-archive, timer-server-sync, loading-states, error-boundary, create-game-fixes, nav-cleanup)
2. App accessible sur http://localhost:8080 sans erreur de démarrage
3. Login/logout fonctionne sans freeze ni erreur console
4. Delete d'une partie sans picks → succès
5. Archive d'une partie CREATING → succès
6. Démarrer un draft → timer s'affiche correctement (valeur serveur)
7. Naviguer vers une page draft puis se déconnecter → redirect login propre, pas de freeze
8. Header affiche uniquement : Logo | Catalogue | Profil — sans bouton "Parties" ni "Draft"
9. Backend: 2383 tests, 0 failures (baseline sprint 12)
10. Frontend: 0 nouvelles régressions vitest

## Tasks / Subtasks

- [ ] Task 1: Rebuild image Docker et redémarrer le stack (AC: #1, #2)
  - [ ] 1.1: `docker compose -f docker-compose.local.yml build --no-cache app`
  - [ ] 1.2: `docker compose -f docker-compose.local.yml up -d`
  - [ ] 1.3: Attendre que `GET /actuator/health` retourne `{"status":"UP"}`
  - [ ] 1.4: Vérifier les logs: `docker logs fortnite-app-local | grep -E "(Started|ERROR)"`
  - [ ] 1.5: Vérifier que Flyway V46/V47/V48 sont appliqués (out-of-order=true)

- [ ] Task 2: Validation manuelle des fixes critiques via curl/API (AC: #3 à #8)
  - [ ] 2.1: Login admin → obtenir JWT token
  - [ ] 2.2: Créer une partie test → vérifier bouton disabled pendant requête
  - [ ] 2.3: Archiver la partie → vérifier HTTP 200 et soft delete (deletedAt set)
  - [ ] 2.4: Tenter de supprimer une partie avec picks → vérifier HTTP 409 + message clair
  - [ ] 2.5: Déclencher un draft → vérifier que `expiresAt` est présent dans l'event STOMP
  - [ ] 2.6: Vérifier le header Angular: confirmer absence du bouton "Parties" dans le HTML servi

- [ ] Task 3: Validation automatisée backend (AC: #9)
  - [x] 3.1: `mvn spotless:apply -q --no-transfer-progress` — OK
  - [x] 3.2: `mvn test --no-transfer-progress` — **2383 tests, 0 failures, 9 skipped** ✅ (2026-03-20)

- [ ] Task 4: Validation automatisée frontend (AC: #10)
  - [x] 4.1: `cd frontend && npm run test:vitest` — **2254 tests, 1720 passing, 534 pre-existing failures** (localStorage/Zone.js baseline — inchangé) ✅ (2026-03-20)
  - [x] 4.2: Counts reportés ci-dessus

- [ ] Task 5: Mettre à jour sprint-status.yaml
  - [ ] 5.1: `sprint12-docker-validation: done`

## Dev Notes

### Commandes clés

```bash
# Rebuild
cd "C:/Users/Thibaut/Desktop/FortniteProject"
docker compose -f docker-compose.local.yml build --no-cache app
docker compose -f docker-compose.local.yml up -d

# Health check
docker exec fortnite-app-local curl -sf http://localhost:8080/actuator/health

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@fortnite-pronos.com","password":"Admin1234"}'

# Vérifier Flyway
docker exec fortnite-postgres-local psql -U fortnite_user -d fortnite_pronos \
  -c "SELECT version, description, success FROM flyway_schema_history WHERE version IN ('46','47','48') ORDER BY installed_rank;"

# Vérifier pr_snapshots.region est bien varchar
docker exec fortnite-postgres-local psql -U fortnite_user -d fortnite_pronos \
  -c "SELECT column_name, data_type FROM information_schema.columns WHERE table_name='pr_snapshots' AND column_name='region';"
```

### Fixes Sprint 12 à valider

| Fix | Fichier modifié | Ce qu'on valide |
|-----|----------------|-----------------|
| ws-lifecycle | AppComponent + Draft pages | Logout sans freeze |
| delete-archive | GlobalExceptionHandler | 409 CONFLICT sur FK violation |
| timer-server-sync | DraftTimerComponent + SnakeDraftPage | expiresAt dans events STOMP |
| loading-states | UserGamesStore.reset() | Store vide après logout |
| error-boundary | GlobalErrorHandlerService | Enregistré dans app.config.ts |
| create-game-fixes | create-game.component.ts | Bouton disabled pendant requête |
| nav-cleanup | main-layout.component.html | Pas de bouton "Parties" |

### Pre-existing Gaps / Known Issues

- [KNOWN] 534 pre-existing frontend test failures (localStorage/Zone.js) — inchangés depuis Sprint 12
- [KNOWN] 19 pre-existing backend failures (FortniteTrackerServiceTddTest, GameDataIntegrationTest, etc.)
- [KNOWN] Flyway out-of-order=true requis dans application-dev.yml (V46-48 < V1002)

### Ordre de suppression pour delete avec cascade

Si la suppression hard d'une partie est nécessaire (hors soft delete) :
1. DraftRegionCursor (si existe)
2. DraftPick
3. DraftAsyncSelection
4. DraftAsyncWindow
5. Draft
6. Trade (liés à la partie)
7. GameParticipant
8. Game

### Project Structure Notes

- Dockerfile: `C:/Users/Thibaut/Desktop/FortniteProject/Dockerfile`
- docker-compose.local.yml: `C:/Users/Thibaut/Desktop/FortniteProject/docker-compose.local.yml`
- application-dev.yml: `src/main/resources/application-dev.yml` — contient `flyway.out-of-order: true`
- GlobalExceptionHandler: `src/main/java/com/fortnite/pronos/config/GlobalExceptionHandler.java`

### References

- Sprint 12 commit: `2023982` — `feat(sprint12): implement 7 stabilisation stories`
- V48 migration: `src/main/resources/db/migration/V48__fix_pr_snapshots_region_type.sql`
- pr_region fix commit: `e262c32` — `fix(pipeline): resolve pr_region enum type mismatch`

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Completion Notes List

### File List
