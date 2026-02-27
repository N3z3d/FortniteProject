# Story 1.4: Historisation alias et snapshots PR append-only

Status: done

## Story

As an admin,
I want conserver l'historique des pseudos et snapshots PR par région,
so that les évolutions joueurs restent auditables dans le temps (FR-05, FR-06, NFR-R04).

## Acceptance Criteria

1. **Given** un nouveau joueur est créé lors de l'ingestion CSV, **When** `createPlayer()` est appelé, **Then** une `PlayerAliasEntry(source="FT_INGESTION", current=true)` est sauvegardée via `PlayerAliasRepositoryPort.save()`.

2. **Given** un joueur existant est mis à jour (région/tranche/saison), **When** `applyPlayerUpdates()` est appelé, **Then** aucune nouvelle `PlayerAliasEntry` n'est créée (append-only — on ne duplique pas les alias existants).

3. **Given** `PlayerAliasRepositoryPort.save()` lève une exception, **When** l'alias est enregistré, **Then** l'exception est capturée avec un log WARN, **And** le joueur et son snapshot/score continuent d'être traités (comportement non-bloquant).

4. **Given** les snapshots PR sont persistés par `PrIngestionRowProcessor.upsertSnapshot()`, **When** plusieurs ingestions couvrent la même (player, region, date), **Then** chaque combinaison unique est conservée en DB (composite key = append-only par date/région).

## Dev Notes

### Infrastructure créée

| Artefact | Chemin | Usage |
|---|---|---|
| `PlayerAliasEntry` | `domain/player/alias/model/PlayerAliasEntry.java` | Modèle domaine (`final`, `restore()`, `current=true` par défaut) |
| `PlayerAliasRepositoryPort` | `domain/port/out/PlayerAliasRepositoryPort.java` | Port persistence: `save()`, `findByPlayerId()` |
| `PlayerAliasEntity` | `adapter/out/persistence/player/alias/` | Mapping table `player_aliases` (V29 migration existante) |
| `PlayerAliasEntityMapper` | `adapter/out/persistence/player/alias/` | toDomain/toEntity |
| `PlayerAliasJpaRepository` | `adapter/out/persistence/player/alias/` | Spring Data JPA |
| `PlayerAliasRepositoryAdapter` | `adapter/out/persistence/player/alias/` | Implémentation port |

### Note migration Flyway

Aucune migration nécessaire — la table `player_aliases` existait déjà depuis **V29__create_player_aliases.sql**.

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### File List

- `_bmad-output/implementation-artifacts/1-4-historisation-alias-et-snapshots-pr-append-only.md`
- `src/main/java/com/fortnite/pronos/domain/player/alias/model/PlayerAliasEntry.java` (nouveau)
- `src/main/java/com/fortnite/pronos/domain/port/out/PlayerAliasRepositoryPort.java` (nouveau)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/alias/PlayerAliasEntity.java` (nouveau)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/alias/PlayerAliasJpaRepository.java` (nouveau)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/alias/PlayerAliasEntityMapper.java` (nouveau)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/alias/PlayerAliasRepositoryAdapter.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessor.java` (modifié — 5ème dep + recordAlias)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceTddTest.java` (modifié — @Import alias adapter)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceRuntimePortsTest.java` (modifié — 5ème mock)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessorUnresolvedTest.java` (modifié — 5ème arg)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessorAliasTest.java` (nouveau — 4 tests)
