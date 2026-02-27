# Story 1.5: Région principale, dédoublonnage et alertes pipeline

Status: done

## Story

As an admin,
I want une région principale auto, une détection de doublons et une alerte UNRESOLVED,
so that la qualité des données reste exploitable sans revue manuelle permanente (FR-07, FR-08, FR-09).

## Acceptance Criteria

1. **Given** les snapshots des 12 derniers mois sont disponibles pour un joueur, **When** `PlayerQualityService.runDailyQualityJob()` est exécuté, **Then** la région principale est calculée (région la plus fréquente hors GLOBAL) et `Player.updateRegion()` est appelé si elle change (FR-07).

2. **Given** deux `PlayerIdentityEntry(RESOLVED)` partagent le même `epicId`, **When** le job qualité s'exécute, **Then** un log WARN `[ALERT] Duplicate epicId detected` est émis avec les playerIds concernés (FR-08).

3. **Given** un `PlayerIdentityEntry(UNRESOLVED)` a une `createdAt` antérieure à `now - 24h`, **When** le job qualité s'exécute, **Then** un log WARN `[ALERT] Player UNRESOLVED > 24h` est émis avec le pseudo et le playerId (FR-09).

4. **Given** le résultat du job, **When** `runDailyQualityJob()` retourne, **Then** un `PlayerQualityJobResult(mainRegionsUpdated, duplicateEpicIdsDetected, staleUnresolvedAlerted)` est retourné.

## Dev Notes

### Infrastructure créée

| Artefact | Chemin | Usage |
|---|---|---|
| `PrSnapshotQueryPort` | `domain/port/out/PrSnapshotQueryPort.java` | Port hexagonal pour requêtes snapshot — `findMainRegionNameForPlayer()` |
| `PrSnapshotQueryAdapter` | `adapter/out/persistence/snapshot/PrSnapshotQueryAdapter.java` | Implémentation: stream + groupingBy sur `PrSnapshotRepository.findByPlayerIdSince()` |
| `PlayerQualityService` | `service/ingestion/PlayerQualityService.java` | `@Service`, 3 deps (PlayerDomainRepositoryPort, PlayerIdentityRepositoryPort, PrSnapshotQueryPort) |

### Méthode ajoutée à PrSnapshotRepository

```java
@Query("SELECT s FROM PrSnapshot s WHERE s.player.id = :playerId AND s.snapshotDate >= :since")
List<PrSnapshot> findByPlayerIdSince(@Param("playerId") UUID, @Param("since") LocalDate);
```

### Architecture compliance

- `PlayerQualityService` uses `PlayerDomainRepositoryPort` (domain port, not JPA repo) ✓
- 3 dependencies injected (≤ 7 CouplingTest limit) ✓
- `PrSnapshotQueryPort` — interface pure dans `domain/port/out/` ✓

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### File List

- `_bmad-output/implementation-artifacts/1-5-region-principale-dedoublonnage-et-alertes-pipeline.md`
- `src/main/java/com/fortnite/pronos/domain/port/out/PrSnapshotQueryPort.java` (nouveau)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/snapshot/PrSnapshotQueryAdapter.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/ingestion/PlayerQualityService.java` (nouveau)
- `src/main/java/com/fortnite/pronos/repository/PrSnapshotRepository.java` (modifié — ajout findByPlayerIdSince)
- `src/test/java/com/fortnite/pronos/service/ingestion/PlayerQualityServiceTest.java` (nouveau — 7 tests)
