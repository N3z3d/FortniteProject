# Story 1.3: Gestion UNRESOLVED non-bloquante

Status: done

## Story

As an admin,
I want les joueurs non résolus marqués UNRESOLVED sans casser le lot,
so that l'ingestion continue même en cas d'échec partiel de résolution (NFR-R03).

## Acceptance Criteria

1. **Given** un nouveau joueur est créé lors de l'ingestion CSV, **When** `PrIngestionRowProcessor.createPlayer()` est appelé, **Then** une `PlayerIdentityEntry(UNRESOLVED)` est créée et persistée via `PlayerIdentityRepositoryPort.save()`.

2. **Given** `PlayerIdentityRepositoryPort.save()` lève une exception technique, **When** la ligne est traitée, **Then** l'exception est capturée avec un log WARN, **And** le joueur est quand même créé (`playersCreated++` non impacté), **And** le reste du lot continue.

3. **Given** un joueur existant est mis à jour (région/tranche/saison), **When** `applyPlayerUpdates()` persiste la mise à jour, **Then** aucune nouvelle `PlayerIdentityEntry` n'est créée (uniquement pour les nouveaux joueurs).

## Dev Notes

### Infrastructure existante réutilisée

| Artefact | Chemin | Usage |
|---|---|---|
| `PlayerIdentityEntry` | `domain/player/identity/model/PlayerIdentityEntry.java` | Constructeur public `(UUID playerId, String playerUsername, String playerRegion, LocalDateTime createdAt)` → status=UNRESOLVED automatique |
| `PlayerIdentityRepositoryPort` | `domain/port/out/PlayerIdentityRepositoryPort.java` | `save(PlayerIdentityEntry)` — méthode existante |
| `PlayerIdentityRepositoryAdapter` | `adapter/out/persistence/player/identity/` | Implémentation JPA existante |
| `PrIngestionRowProcessor` | `service/ingestion/PrIngestionRowProcessor.java` | Ajout 4ème dep + `queueForResolution()` privé |

### Contrainte @DataJpaTest

`PrIngestionServiceTddTest` est un `@DataJpaTest` qui `@Import(PrIngestionRowProcessor.class)`. L'ajout de `PlayerIdentityRepositoryPort` comme 4ème dep nécessite d'ajouter `PlayerIdentityRepositoryAdapter.class` et `PlayerIdentityEntityMapper.class` au `@Import`.

### Autre test impacté

`PrIngestionServiceRuntimePortsTest` construit `PrIngestionRowProcessor` directement — mis à jour pour passer le 4ème mock `PlayerIdentityRepositoryPort`.

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### File List

- `_bmad-output/implementation-artifacts/1-3-gestion-unresolved-non-bloquante.md`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessor.java` (modifié — 4ème dep + @Slf4j + queueForResolution)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceTddTest.java` (modifié — @Import + PlayerIdentityRepositoryAdapter + PlayerIdentityEntityMapper)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceRuntimePortsTest.java` (modifié — 4ème mock + constructeur mis à jour)
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessorUnresolvedTest.java` (nouveau — 5 tests)
