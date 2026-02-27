# Story 1.2: Résolution pseudo FT vers Epic ID via adapter swappable

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an admin,
I want les pseudos FortniteTracker stagés automatiquement résolus vers un Epic Account ID via un port hexagonal swappable,
so that l'implémentation du fournisseur externe (Fortnite-API.com) reste remplaçable sans toucher le domaine.

## Acceptance Criteria

1. **Given** la table `player_identity_entries` contient des entrées en statut `UNRESOLVED`, **When** le `ResolutionQueueService` traite le lot, **Then** les pseudos résolus via `ResolutionPort` passent en statut `RESOLVED` avec l'`epicId` et le `confidenceScore` renseignés, **And** `PlayerIdentityRepositoryPort.save()` persiste chaque entrée résolue.

2. **Given** `ResolutionPort.resolveFortniteId()` retourne `Optional.empty()` pour un pseudo, **When** le service traite cette entrée, **Then** l'entrée reste `UNRESOLVED` sans lancer d'exception, **And** le reste du lot continue à être traité (comportement non-bloquant).

3. **Given** un adapter `ResolutionPort` est remplacé par un autre (swap), **When** les tests s'exécutent avec le mock, **Then** aucun code domaine ou service n'a changé — seul l'adapter est substitué (NFR-I02 swappable).

4. **Given** une entrée `PlayerIdentityEntry` invalide (playerId null, pseudo blank), **When** le service tente de la traiter, **Then** l'entrée est ignorée avec un log WARN explicite, **And** aucun état invalide n'est persisté.

5. **Given** `ResolutionPort` lève une exception technique (timeout, 429, réseau), **When** le service l'attrape, **Then** l'exception est loggée avec contexte (pseudo, region), **And** l'entrée reste UNRESOLVED et le lot continue.

## Tasks / Subtasks

- [ ] Task 1: Créer le port hexagonal `ResolutionPort` dans `domain/port/out/` (AC: #3)
  - [ ] Subtask 1.1: Interface `ResolutionPort` avec méthode `Optional<String> resolveFortniteId(String pseudo, String region)`
  - [ ] Subtask 1.2: Vérifier que le port est `final`-safe (interface pur, zéro annotation Spring/JPA)
  - [ ] Subtask 1.3: Mettre à jour `DependencyInversionTest` si nécessaire (les ports dans `domain/port/out` doivent être des interfaces — déjà enforced)

- [ ] Task 2: Créer l'adapter mock `StubResolutionAdapter` dans `adapter/out/resolution/` (AC: #3)
  - [ ] Subtask 2.1: Classe `StubResolutionAdapter implements ResolutionPort` avec retour configurable (RESOLVED / empty) — utilisée en dev/test tant que l'API key Fortnite-API.com n'est pas disponible
  - [ ] Subtask 2.2: Annotation `@Primary @ConditionalOnProperty(name="resolution.adapter", havingValue="stub", matchIfMissing=true)` pour activation par défaut en tests/dev
  - [ ] Subtask 2.3: Structurer le package `adapter/out/resolution/` pour accueillir plus tard `FortniteApiResolutionAdapter` (emplacement défini dans architecture.md)

- [ ] Task 3: Créer `ResolutionQueueService` dans `service/ingestion/` (AC: #1, #2, #4, #5)
  - [ ] Subtask 3.1: Méthode `processUnresolvedBatch()` — charge toutes les entrées `UNRESOLVED` via `PlayerIdentityRepositoryPort.findByStatus(IdentityStatus.UNRESOLVED)`
  - [ ] Subtask 3.2: Pour chaque entrée, appeler `ResolutionPort.resolveFortniteId(entry.getPlayerUsername(), entry.getPlayerRegion())`
  - [ ] Subtask 3.3: Si résolu → appeler `entry.resolve(epicId, confidenceScore, "AUTO")` puis `PlayerIdentityRepositoryPort.save(entry)`
  - [ ] Subtask 3.4: Si non résolu (`Optional.empty()`) → log WARN, continuer sans persistance
  - [ ] Subtask 3.5: Capturer toute exception technique → log ERROR avec contexte, continuer le lot (non-bloquant)
  - [ ] Subtask 3.6: Respecter ≤ 7 dépendances injectées (`ResolutionPort`, `PlayerIdentityRepositoryPort`, éventuellement `Clock` — max 3)

- [ ] Task 4: Couvrir en tests TDD (AC: #1, #2, #3, #4, #5)
  - [ ] Subtask 4.1: Test nominal — batch de 3 UNRESOLVED, ResolutionPort mock retourne epicId → tous passent RESOLVED, `save()` appelé 3 fois
  - [ ] Subtask 4.2: Test partiel — 2 UNRESOLVED, 1 résolu / 1 non résolu → 1 save, 1 non-persisté, pas d'exception
  - [ ] Subtask 4.3: Test exception technique — ResolutionPort lève RuntimeException → log capturé, lot continue, entrée reste UNRESOLVED
  - [ ] Subtask 4.4: Test entrée invalide — pseudo blank → ignorée avec WARN, pas de call ResolutionPort
  - [ ] Subtask 4.5: Test swappabilité — deux adapters implémentent `ResolutionPort`, service fonctionne identiquement (DIP vérifié)

## Dev Notes

### Contexte critique — Infrastructure DÉJÀ EXISTANTE à réutiliser

**Ne PAS recréer ces éléments — ils existent et sont opérationnels :**

| Artefact | Chemin | Usage |
|---|---|---|
| `PlayerIdentityEntry` | `domain/player/identity/model/PlayerIdentityEntry.java` | Modèle domaine (UNRESOLVED→RESOLVED) — `final`, static factory `restore()`, méthode `resolve(epicId, score, resolvedBy)` |
| `IdentityStatus` | `domain/player/identity/model/IdentityStatus.java` | Enum `UNRESOLVED / RESOLVED / REJECTED` |
| `PlayerIdentityRepositoryPort` | `domain/port/out/PlayerIdentityRepositoryPort.java` | Port persistence — `findByStatus()`, `findByPlayerId()`, `save()`, `countByStatus()` |
| `PlayerIdentityRepositoryAdapter` | `adapter/out/persistence/player/identity/` | Implémentation JPA complète (Entity + Mapper + JpaRepo + Adapter) |
| `EpicIdValidatorPort` | `domain/port/out/EpicIdValidatorPort.java` | Valide si un Epic ID est joignable — **différent** de ResolutionPort (ne résout pas pseudo → ID) |
| `MockEpicIdValidatorAdapter` | `adapter/out/epicid/MockEpicIdValidatorAdapter.java` | Pattern de référence pour les adapters mock — reproduire ce pattern pour `StubResolutionAdapter` |

### Architecture compliance (NON-NÉGOCIABLE)

```
NOUVEAU ResolutionPort → domain/port/out/ResolutionPort.java
NOUVEAU StubResolutionAdapter → adapter/out/resolution/StubResolutionAdapter.java
NOUVEAU ResolutionQueueService → service/ingestion/ResolutionQueueService.java
```

- `ResolutionPort` = interface pure, ZÉRO annotation, ZÉRO import Spring/JPA (enforce par `DomainIsolationTest`)
- `StubResolutionAdapter` = `@Service` + `@Primary` conditionnel — jamais dans `domain/`
- `ResolutionQueueService` = `@Service` — dépend de `ResolutionPort` + `PlayerIdentityRepositoryPort` uniquement (≤ 7 deps — `CouplingTest`)
- Pattern `@Scheduled` dans `PrIngestionOrchestrationService` montre comment chaîner les services — `ResolutionQueueService` peut être appelé en fin de `processRegion()` ou en job séparé

### POC bloquant — Fortnite-API.com

> ⚠️ La clé API `FORTNITE_API_KEY` n'est pas encore disponible. Le vrai `FortniteApiResolutionAdapter` est **hors-scope** de cette story. Implémenter uniquement `StubResolutionAdapter` (retourne un Epic ID synthétique configurable). Le vrai adapter sera implémenté après validation du POC.

Pattern prévu pour le vrai adapter (à créer plus tard) :
```java
// adapter/out/resolution/FortniteApiResolutionAdapter.java
@Service
@ConditionalOnProperty(name="resolution.adapter", havingValue="fortnite-api")
public class FortniteApiResolutionAdapter implements ResolutionPort {
    @Override
    public Optional<String> resolveFortniteId(String pseudo, String region) {
        // GET https://fortnite-api.com/v2/stats/br/v2?name={pseudo}
        // timeout: ${resolution.timeout.seconds:10}
    }
}
```

### Patterns obligatoires

**ResolutionPort (domain/port/out/) :**
```java
public interface ResolutionPort {
    /**
     * Resolves a FortniteTracker display name to a Fortnite Epic Account ID.
     * @return Optional.of(epicId) if resolved, Optional.empty() if not found
     * @throws RuntimeException (caller must catch) if external call fails
     */
    Optional<String> resolveFortniteId(String pseudo, String region);
}
```

**StubResolutionAdapter :**
```java
@Service
@Primary
@ConditionalOnProperty(name="resolution.adapter", havingValue="stub", matchIfMissing=true)
public class StubResolutionAdapter implements ResolutionPort {
    @Override
    public Optional<String> resolveFortniteId(String pseudo, String region) {
        if (pseudo == null || pseudo.isBlank()) return Optional.empty();
        return Optional.of("STUB-EPIC-" + pseudo.toUpperCase(Locale.ROOT));
    }
}
```

**ResolutionQueueService — logique non-bloquante :**
```java
public void processUnresolvedBatch() {
    List<PlayerIdentityEntry> unresolved =
        identityRepository.findByStatus(IdentityStatus.UNRESOLVED);
    for (PlayerIdentityEntry entry : unresolved) {
        if (entry.getPlayerUsername() == null || entry.getPlayerUsername().isBlank()) {
            log.warn("Skipping invalid entry id={}", entry.getId());
            continue;
        }
        try {
            resolveEntry(entry);
        } catch (Exception e) {
            log.error("Resolution failed for pseudo={} region={}: {}",
                entry.getPlayerUsername(), entry.getPlayerRegion(), e.getMessage());
        }
    }
}
private void resolveEntry(PlayerIdentityEntry entry) {
    resolutionPort.resolveFortniteId(entry.getPlayerUsername(), entry.getPlayerRegion())
        .ifPresent(epicId -> {
            entry.resolve(epicId, 100, "AUTO");
            identityRepository.save(entry);
            log.info("Resolved: pseudo={} epicId={}", entry.getPlayerUsername(), epicId);
        });
}
```

### Constraints Spotless & ArchUnit

- Lancer `mvn spotless:apply` AVANT `mvn test` — obligatoire
- `DomainIsolationTest` scanne `domain/player/identity/..` automatiquement — `ResolutionPort` dans `domain/port/out/` est autorisé (interfaces, pas de final)
- `DependencyInversionTest` vérifie que les services n'importent pas JPA directement — respecté ici
- `CouplingTest` : `ResolutionQueueService` doit avoir ≤ 7 dépendances injectées (ici 2)

### Naming Tests

| Composant | Fichier test attendu |
|---|---|
| `ResolutionQueueService` | `ResolutionQueueServiceTest.java` |
| `StubResolutionAdapter` | Test inclus dans `ResolutionQueueServiceTest` via mock direct |

### Project Structure Notes

- Pattern `@ConditionalOnProperty` déjà utilisé dans `PrIngestionOrchestrationService` (property `ingestion.pr.scheduled.enabled`) — reproduire pour le choix d'adapter
- Le package `adapter/out/epicid/` contient `MockEpicIdValidatorAdapter` — créer `adapter/out/resolution/` en parallèle (pas dans `epicid/`)
- `PlayerIdentityEntity` existe déjà avec sa table DB — **pas de nouvelle migration Flyway nécessaire pour cette story**
- Vérifier le nom exact de la table `PlayerIdentityEntity` (`@Table`) avant de créer une migration inutile

### References

- `domain/player/identity/model/PlayerIdentityEntry.java` — modèle domaine existant (resolve(), reject())
- `domain/port/out/PlayerIdentityRepositoryPort.java` — port persistence existant (findByStatus)
- `adapter/out/persistence/player/identity/` — adapter JPA existant (pattern à reproduire pour resolution)
- `adapter/out/epicid/MockEpicIdValidatorAdapter.java` — pattern mock adapter existant
- `service/ingestion/PrIngestionOrchestrationService.java` — orchestration @Scheduled existante (contexte Story 1.1)
- `_bmad-output/planning-artifacts/architecture.md` — section "Pipeline de données", patterns ResolutionPort, adapter/out/resolution/
- `_bmad-output/planning-artifacts/prd.md` — FR-03, NFR-I02 (swappable)
- `_bmad-output/planning-artifacts/epics.md` — Story 1.2 AC BDD

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Analysis: PlayerIdentityEntry + PlayerIdentityRepositoryPort already exist — no new domain model needed
- Analysis: EpicIdValidatorPort ≠ ResolutionPort — validator validates existence, resolver maps pseudo → ID
- Analysis: Flyway V12-V29 are taken — no new migration needed (PlayerIdentityEntity table already created by existing migration)
- Analysis: MockEpicIdValidatorAdapter at adapter/out/epicid/ — reference pattern for StubResolutionAdapter
- Analysis: Real FortniteApiResolutionAdapter blocked by missing API key — stub only in scope
- Story context generated from BMAD artifacts (epics.md, prd.md, architecture.md, project-context.md)

### Completion Notes List

### File List

- `_bmad-output/implementation-artifacts/1-2-resolution-pseudo-ft-vers-epic-id-via-adapter-swappable.md`
- `src/main/java/com/fortnite/pronos/domain/port/out/ResolutionPort.java`
- `src/main/java/com/fortnite/pronos/adapter/out/resolution/StubResolutionAdapter.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/ResolutionQueueServiceTest.java`
