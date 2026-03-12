# Story 7.1: Dashboard pipeline par région

Status: done

## Story

As an admin,
I want to see the pipeline status broken down by region (EU/NAW/BR/ASIA/OCE/NAC/ME) — UNRESOLVED count, total volume, and last ingestion date per region,
so that I can detect regional data incidents quickly without having to scroll through the full player list.

## Acceptance Criteria

1. **Given** the admin is authenticated, **When** they call `GET /api/admin/players/pipeline/regional-status`, **Then** they receive a list of `PipelineRegionalStats` (one per region that has entries), each with: `region`, `unresolvedCount`, `resolvedCount`, `rejectedCount`, `totalCount`, `lastIngestedAt` (ISO datetime of the most recent entry in that region).

2. **Given** no entries exist for a region, **When** the endpoint is called, **Then** that region does NOT appear in the response (only regions with data are returned).

3. **Given** a non-admin user calls the endpoint, **When** Spring Security evaluates, **Then** the server responds 403 Forbidden (existing `@PreAuthorize("hasRole('ADMIN')")` covers this automatically via the existing security configuration).

4. **Given** the admin views the `/admin/pipeline` frontend page, **When** the page loads, **Then** a "Statut par région" tab/section shows a table with columns: Région | Total | Non résolus | Résolus | Rejetés | Dernière ingestion.

5. **Given** data loaded successfully, **When** a region has 0 UNRESOLVED, **Then** its row shows in a neutral color (no alarm); when UNRESOLVED > 0 the row shows an accent/warning color.

6. **Given** the endpoint fails (network/500), **When** the page loads, **Then** the section shows an inline error with a retry button without crashing the rest of the page.

## Technical Context

### FR Coverage
- **FR-41**: Admin visualise le statut du scraping par région — volumes, dernière exécution, UNRESOLVED

### Analyse — ce qui EST DÉJÀ là

#### Backend
- `PlayerIdentityEntity` (@Table `player_identity_pipeline`) — has `playerRegion` (VARCHAR 20), `status` (enum: UNRESOLVED/RESOLVED/REJECTED), `createdAt` (LocalDateTime)
- `PlayerIdentityJpaRepository` extends `JpaRepository<PlayerIdentityEntity, UUID>` — path: `adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java`
- `PlayerIdentityRepositoryPort` — 5 methods: `findByStatus`, `findByStatusPaged`, `findByPlayerId`, `countByStatus`, `save` — NO regional breakdown
- `PlayerIdentityRepositoryAdapter` — implements port, delegates to JpaRepository
- `PlayerIdentityPipelineService` (4 deps — CouplingTest: max 7) — global counts only
- `AdminPlayerPipelineController` at `/api/admin/players` — 5 endpoints (unresolved/resolved/count/resolve/reject)
- `IngestionRun` — has NO region field; source is a string. "Last run per region" is derived from `MAX(createdAt)` in `player_identity_pipeline` per region (no schema migration needed)

#### Frontend
- `AdminService` (`features/admin/services/admin.service.ts`) — HTTP client, `ApiResponse<T>` unwrap via `map(r => r.data)`
- `admin.models.ts` (`features/admin/models/`) — `PipelineCount { unresolvedCount, resolvedCount }` exists. NO regional model
- `AdminPipelinePageComponent` (`features/admin/pipeline/admin-pipeline-page/`) — OnPush, inject() pattern, signals (`loading`, `error`), tab group (En attente / Traités), `forkJoin` load, `takeUntil(destroy$)` pattern
- `AdminPipelineTableComponent` — child component for table rows

### Gap 1 — Backend : `PipelineRegionalStatsDto`

```java
// src/main/java/com/fortnite/pronos/dto/admin/PipelineRegionalStatsDto.java
public record PipelineRegionalStatsDto(
    String region,
    long unresolvedCount,
    long resolvedCount,
    long rejectedCount,
    long totalCount,
    LocalDateTime lastIngestedAt
) {}
```

### Gap 2 — Backend : `PlayerIdentityRepositoryPort` + JPA queries

Add to `PlayerIdentityRepositoryPort`:
```java
/** Returns unresolved count per region: key=region, value=count */
Map<String, Long> countUnresolvedByRegion();

/** Returns most recent createdAt per region: key=region, value=lastIngestedAt */
Map<String, java.time.LocalDateTime> findLastIngestedAtByRegion();

/** Returns total count per region for all statuses */
Map<String, Long> countByRegion();

/** Returns count per region for a specific status */
Map<String, Long> countByStatusGroupedByRegion(IdentityStatus status);
```

**Note:** Rather than 4 individual queries (N round-trips), use ONE `@Query` returning `List<Object[]>` with `(region, status, count)` triplets, then aggregate in the adapter. This avoids multiple DB calls.

Add to `PlayerIdentityJpaRepository`:
```java
// One query: fetch (playerRegion, status, count) for GROUP BY
@Query("SELECT e.playerRegion, e.status, COUNT(e) FROM PlayerIdentityEntity e GROUP BY e.playerRegion, e.status")
List<Object[]> countByRegionAndStatus();

// Last ingestion per region
@Query("SELECT e.playerRegion, MAX(e.createdAt) FROM PlayerIdentityEntity e GROUP BY e.playerRegion")
List<Object[]> findLastIngestedAtByRegion();
```

Add to `PlayerIdentityRepositoryPort`:
```java
List<PipelineRegionalStatsDto> getRegionalStats();
```
(Single method returning the assembled DTOs — simplest port API)

### Gap 3 — Backend : `PlayerIdentityRepositoryAdapter`

Implement `getRegionalStats()`:
```java
@Override
public List<PipelineRegionalStatsDto> getRegionalStats() {
    // 1. Query (region, status, count) triplets
    List<Object[]> rows = jpaRepository.countByRegionAndStatus();
    // 2. Query (region, lastIngestedAt) pairs
    List<Object[]> lastDates = jpaRepository.findLastIngestedAtByRegion();
    // 3. Aggregate in Java: Map<region, {UNRESOLVED, RESOLVED, REJECTED, total, lastDate}>
    // 4. Return List<PipelineRegionalStatsDto>
}
```

**IMPORTANT:** `PlayerIdentityRepositoryPort` is in the domain layer. `PipelineRegionalStatsDto` is in the `dto/admin/` package. Domain ports must NOT import from `dto/`. Two options:
- **Option A (recommended)**: Port returns a simple `Map<String, long[]>` or a dedicated domain record, and the adapter + service assemble the DTO. Keeps domain clean.
- **Option B**: Define a nested record inside the port — also acceptable.

**Chosen approach**: Add port method returning `List<RegionalStatRow>` (package-private domain record with region, status, count), plus `findLastIngestedAtByRegion()` returning `Map<String, LocalDateTime>`. The **service** assembles `PipelineRegionalStatsDto` — DTO stays out of domain.

#### Revised port additions:
```java
// In PlayerIdentityRepositoryPort:
/** Raw rows: (region, status, count) triplets */
List<com.fortnite.pronos.domain.player.identity.model.RegionalStatRow> countByRegionAndStatus();

/** Most recent createdAt per region: key=region */
Map<String, java.time.LocalDateTime> findLastIngestedAtByRegion();
```

```java
// New domain record: domain/player/identity/model/RegionalStatRow.java
public record RegionalStatRow(String region, IdentityStatus status, long count) {}
```

### Gap 4 — Backend : `PlayerIdentityPipelineService.getRegionalStats()`

```java
// Add to PlayerIdentityPipelineService (currently 4 deps — stays at 4)
public List<PipelineRegionalStatsDto> getRegionalStats() {
    List<RegionalStatRow> rows = identityRepository.countByRegionAndStatus();
    Map<String, LocalDateTime> lastDates = identityRepository.findLastIngestedAtByRegion();
    return assembleRegionalStats(rows, lastDates);
}

private List<PipelineRegionalStatsDto> assembleRegionalStats(
        List<RegionalStatRow> rows, Map<String, LocalDateTime> lastDates) {
    // Group by region, sum per status, build DTO
    Map<String, Map<IdentityStatus, Long>> grouped = rows.stream()
        .collect(Collectors.groupingBy(RegionalStatRow::region,
            Collectors.groupingBy(RegionalStatRow::status,
                Collectors.summingLong(RegionalStatRow::count))));

    return grouped.entrySet().stream()
        .map(e -> {
            String region = e.getKey();
            Map<IdentityStatus, Long> counts = e.getValue();
            long unresolved = counts.getOrDefault(IdentityStatus.UNRESOLVED, 0L);
            long resolved = counts.getOrDefault(IdentityStatus.RESOLVED, 0L);
            long rejected = counts.getOrDefault(IdentityStatus.REJECTED, 0L);
            long total = unresolved + resolved + rejected;
            LocalDateTime lastAt = lastDates.get(region);
            return new PipelineRegionalStatsDto(region, unresolved, resolved, rejected, total, lastAt);
        })
        .sorted(Comparator.comparing(PipelineRegionalStatsDto::region))
        .toList();
}
```

**CouplingTest:** `PlayerIdentityPipelineService` stays at 4 deps (no new dep injected) ✓

### Gap 5 — Backend : `AdminPlayerPipelineController` endpoint

```java
// Add to AdminPlayerPipelineController
@GetMapping("/pipeline/regional-status")
public ResponseEntity<List<PipelineRegionalStatsDto>> getRegionalStatus() {
    return ResponseEntity.ok(pipelineService.getRegionalStats());
}
```

No new auth annotation needed — `/api/admin/**` is already protected by `AdminGuard` / existing security config.

### Gap 6 — Frontend : `admin.models.ts`

```typescript
// Add to frontend/src/app/features/admin/models/admin.models.ts
export interface PipelineRegionalStats {
  region: string;
  unresolvedCount: number;
  resolvedCount: number;
  rejectedCount: number;
  totalCount: number;
  lastIngestedAt: string | null; // ISO datetime
}
```

### Gap 7 — Frontend : `AdminService.getPipelineRegionalStatus()`

```typescript
// Add to frontend/src/app/features/admin/services/admin.service.ts
getPipelineRegionalStatus(): Observable<PipelineRegionalStats[]> {
  return this.http
    .get<PipelineRegionalStats[]>(`${this.baseUrl}/players/pipeline/regional-status`);
}
```

**Note:** This endpoint returns `List<PipelineRegionalStatsDto>` directly (no `ApiResponse<T>` wrapper) — aligned with `AdminPlayerPipelineController` which returns `ResponseEntity<List<...>>` directly (NOT wrapped). DO NOT use `.pipe(map(r => r.data))` here.

### Gap 8 — Frontend : Section régionale dans `AdminPipelinePageComponent`

Ajouter un **3ème onglet** "Statut par région" dans le tab group existant.

**Données à afficher :**
```
| Région | Total | Non résolus | Résolus | Rejetés | Dernière ingestion |
|--------|-------|-------------|---------|---------|-------------------|
| EU     | 1250  | 42          | 1180    | 28      | 28/02/2026 08:00  |
| NAW    | 980   | 5           | 960     | 15      | 28/02/2026 07:30  |
```

**État warning:** ligne surlignée (CSS class `has-unresolved`) si `unresolvedCount > 0`.
**État vide:** message "Aucune donnée d'ingestion disponible" si liste vide.

**Chargement :** ajouter `getPipelineRegionalStatus()` au `forkJoin` existant dans `loadData()`.

```typescript
// Dans AdminPipelinePageComponent
regionalStats: PipelineRegionalStats[] = [];

// Dans loadData() — étendre le forkJoin existant
this.adminService.getPipelineRegionalStatus().subscribe({
  next: (stats) => {
    this.regionalStats = stats;
    this.cdr.markForCheck();
  },
  error: () => { this.regionalError = true; this.cdr.markForCheck(); }
});
```

**Ne pas ajouter** `getPipelineRegionalStatus()` au `forkJoin` principal (erreur réseau sur un seul endpoint ne doit pas bloquer les autres onglets) — charger séparément avec gestion d'erreur indépendante.

### Contraintes architecture

- **CouplingTest** : `PlayerIdentityPipelineService` reste à 4 deps ✓ (ajout de méthode, pas de deps)
- **NamingConventionTest** : pas de nouveau service ✓
- **DomainIsolationTest** : `RegionalStatRow` est dans `domain/player/identity/model/` — domaine pur, aucune dépendance JPA/Spring ✓
- **LayeredArchitectureTest** : Controller → Service → Port → Adapter (aucun accès JPA direct depuis Controller/Service) ✓
- **DependencyInversionTest** : Controller ne dépend que du service ✓
- **Spotless** : `mvn spotless:apply` avant `mvn test`

### Pattern de tests backend

```java
// PlayerIdentityPipelineServiceTest (extension du fichier existant)
@ExtendWith(MockitoExtension.class)
class PlayerIdentityPipelineServiceTest {
  // Tests à ajouter :
  // whenMultipleRegions_returnsStatsGroupedByRegion()
  // whenNoEntries_returnsEmptyList()
  // whenRegionHasOnlyResolved_unresolvedIsZero()
  // whenLastIngestedAtAvailable_populatedInDto()
}

// AdminPlayerPipelineControllerTest (extension)
// whenRegionalStatusCalled_returns200WithList()
// whenNoData_returns200WithEmptyList()
```

### Pattern de tests frontend

```typescript
// AdminPipelinePageComponent spec (extension)
// 'affiche la section régionale avec stats'
// 'surligne les régions avec des non-résolus'
// 'affiche un état d\'erreur si le chargement régional échoue'

// AdminService spec
// 'getPipelineRegionalStatus retourne la liste des stats'
```

### Project Structure Notes

- DTO admin : `src/main/java/com/fortnite/pronos/dto/admin/`
- Nouveau domain record : `src/main/java/com/fortnite/pronos/domain/player/identity/model/RegionalStatRow.java`
- JPA query ajout : `adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java`
- Adapter ajout : `adapter/out/persistence/player/identity/PlayerIdentityRepositoryAdapter.java`
- Port ajout : `domain/port/out/PlayerIdentityRepositoryPort.java`
- Service ajout : `service/admin/PlayerIdentityPipelineService.java`
- Controller ajout : `controller/AdminPlayerPipelineController.java`
- Frontend model : `features/admin/models/admin.models.ts`
- Frontend service : `features/admin/services/admin.service.ts`
- Frontend component : `features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` + `.html`
- Frontend spec : `features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts`

### References

- `PlayerIdentityEntity` : `adapter/out/persistence/player/identity/PlayerIdentityEntity.java`
- `PlayerIdentityJpaRepository` : `adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java`
- `PlayerIdentityRepositoryPort` : `domain/port/out/PlayerIdentityRepositoryPort.java`
- `PlayerIdentityPipelineService` : `service/admin/PlayerIdentityPipelineService.java`
- `AdminPlayerPipelineController` : `controller/AdminPlayerPipelineController.java`
- `AdminService` (frontend) : `features/admin/services/admin.service.ts`
- `admin.models.ts` : `features/admin/models/admin.models.ts`
- `AdminPipelinePageComponent` : `features/admin/pipeline/admin-pipeline-page/`

## Tasks / Subtasks

- [x] Task 1: Domain record + Port extension (AC: #1)
  - [x] Créer `domain/player/identity/model/RegionalStatRow.java` (record: region, status, count)
  - [x] Ajouter `countByRegionAndStatus()` + `findLastIngestedAtByRegion()` dans `PlayerIdentityRepositoryPort`

- [x] Task 2: JPA queries + Adapter (AC: #1, #2)
  - [x] Ajouter `@Query countByRegionAndStatus()` + `findLastIngestedAtByRegion()` dans `PlayerIdentityJpaRepository`
  - [x] Implémenter les 2 méthodes dans `PlayerIdentityRepositoryAdapter`

- [x] Task 3: DTO + Service `getRegionalStats()` + tests (AC: #1, #2)
  - [x] Créer `dto/admin/PipelineRegionalStatsDto.java` (record Java)
  - [x] Ajouter `getRegionalStats()` + `assembleRegionalStats()` dans `PlayerIdentityPipelineService`
  - [x] 4 tests dans `PlayerIdentityPipelineServiceTest`

- [x] Task 4: Controller endpoint + tests (AC: #1, #3)
  - [x] Ajouter `GET /pipeline/regional-status` dans `AdminPlayerPipelineController`
  - [x] 2 tests dans `AdminPlayerPipelineControllerTest`

- [x] Task 5: Frontend model + service + tests (AC: #4, #6)
  - [x] Ajouter `PipelineRegionalStats` dans `admin.models.ts`
  - [x] Ajouter `getPipelineRegionalStatus()` dans `AdminService` + `getRegionalStatus()` dans `PipelineService`
  - [x] 1 test dans `admin.service.spec.ts`

- [x] Task 6: Frontend composant — section régionale + tests (AC: #4, #5, #6)
  - [x] Ajouter `regionalStats`, `regionalError`, chargement indépendant dans `AdminPipelinePageComponent`
  - [x] 3ème onglet "Statut par région" dans le template HTML
  - [x] CSS: classe `has-unresolved` sur les lignes avec UNRESOLVED > 0
  - [x] 3 tests dans `AdminPipelinePageComponent` spec

- [x] Task 7: i18n (AC: #4)
  - [x] N/A — composant utilise des chaînes françaises codées en dur (cohérent avec le reste de la page)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None.

### Completion Notes List

- `PipelineService.getRegionalStatus()` added (component uses PipelineService, not AdminService)
- Regional stats loaded independently from main forkJoin to isolate failures per AC #6
- 2206 backend tests (19 pre-existing failures + 1 pre-existing error, 0 regressions)
- 2086 frontend tests SUCCESS (+3 regional stats tests)

### File List

- `src/main/java/com/fortnite/pronos/domain/player/identity/model/RegionalStatRow.java` (created)
- `src/main/java/com/fortnite/pronos/domain/port/out/PlayerIdentityRepositoryPort.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityRepositoryAdapter.java` (modified)
- `src/main/java/com/fortnite/pronos/dto/admin/PipelineRegionalStatsDto.java` (created)
- `src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java` (modified)
- `src/main/java/com/fortnite/pronos/controller/AdminPlayerPipelineController.java` (modified)
- `src/test/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineServiceTest.java` (modified — 4 tests)
- `src/test/java/com/fortnite/pronos/controller/AdminPlayerPipelineControllerTest.java` (modified — 2 tests)
- `frontend/src/app/features/admin/models/admin.models.ts` (modified)
- `frontend/src/app/features/admin/services/admin.service.ts` (modified)
- `frontend/src/app/features/admin/services/pipeline.service.ts` (modified)
- `frontend/src/app/features/admin/services/admin.service.spec.ts` (modified — 1 test)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.html` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.scss` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts` (modified — 4 tests)

## Review Follow-ups (AI — post-code-review fixes)

### No HIGH or MEDIUM findings

All ACs verified: regional stats endpoint, grouping logic, service/controller/frontend tests all passing.

### Action items

- [ ] **[AI-Review][Low][L1]**: No dedicated `SecurityConfigAdminPlayerPipelineAuthorizationTest` for the new `GET /api/admin/players/pipeline/regional-status` endpoint. The `/api/admin/**` path pattern is already tested by `SecurityConfigAdminScrapeAuthorizationTest` and `SecurityConfigAdminDraftRosterAuthorizationTest`, so risk is minimal.
- [ ] **[AI-Review][Low][L2]**: Frontend spec documents 3 tests in the story file but 4 tests are actually present (the isolation test `regional stats tab is visible even when main data load fails` was added). Minor doc discrepancy.
