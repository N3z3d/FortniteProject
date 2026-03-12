# Story 5.1: Swap solo vers joueur libre valide

Status: done

## Story

As a participant,
I want remplacer l'un de mes joueurs par un joueur libre de rang strictement pire dans la même région,
so that je puisse ajuster mon équipe sans blocage de tranche ni acceptation tierce.

## Acceptance Criteria

1. **Given** un participant authentifié possède un joueur dans son équipe pour un draft actif, **When** il soumet `POST /api/games/{gameId}/draft/swap-solo` avec `{playerOutId, playerInId}` et que playerIn est libre + même région + rang strictement pire, **Then** le swap est exécuté (playerOut retiré, playerIn ajouté) et la réponse 200 contient les détails du swap.

2. **Given** le joueur cible (playerIn) est déjà assigné dans ce draft, **When** le swap est soumis, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

3. **Given** le joueur cible est d'une région différente de playerOut, **When** le swap est soumis, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

4. **Given** le joueur cible a un rang égal ou meilleur (tranche ≤ tranche de playerOut), **When** le swap est soumis, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

5. **Given** playerOut n'est pas dans l'équipe du participant appelant, **When** le swap est soumis, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

## Technical Context

### Infrastructure déjà en place

#### Exceptions + handler (READY)
- **`InvalidSwapException`** → 400 `INVALID_SWAP` dans `DomainExceptionHandler` ✓
- **`GameNotFoundException`** → 404 ✓
- **`InvalidDraftStateException`** → 409 ✓

#### Repository (READY)
- **`DraftPickRepositoryPort`** :
  - `save(DraftPick)` ✓
  - `findPickedPlayerIdsByDraftId(UUID draftId)` ✓ (playerIn libre)
  - `deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId)` ✓ (Story 4.6)
  - **MANQUANT** : `boolean existsByDraftIdAndParticipantIdAndPlayerId(UUID, UUID, UUID)` → vérifier que playerOut appartient au participant

#### Domain Player (READY)
- `Player.getRegion()` → `PlayerRegion` ✓
- `Player.getTranche()` → String numérique (ex: "1", "2", "3") ; rang pire = tranche plus grande ✓
- `PlayerDomainRepositoryPort.findById(UUID)` ✓

#### Lookups (READY)
- `GameParticipantRepositoryPort.findByUserIdAndGameId(UUID, UUID)` ✓
- `DraftDomainRepositoryPort.findActiveByGameId(UUID)` ✓
- `DraftRepositoryPort.findById(UUID)` ✓
- `GameDomainRepositoryPort.findById(UUID)` ✓

#### UserResolver (READY)
- Pattern `SnakeDraftController` : `userResolver.getCurrentUser(request).getId()` ✓

### Gap identifié pour Story 5.1

**1. DraftPickRepositoryPort** : manque de méthode pour vérifier l'appartenance au participant.
**Fix cible** :
```java
// Port
boolean existsByDraftIdAndParticipantIdAndPlayerId(UUID draftId, UUID participantId, UUID playerId);

// Repository @Query
@Query("SELECT COUNT(dp) > 0 FROM DraftPick dp WHERE dp.draft.id = :draftId AND dp.participant.id = :participantId AND dp.player.id = :playerId")
boolean existsByDraftIdAndParticipantIdAndPlayerId(...)
```

**2. `SwapSoloService`** : service inexistant.
**Fix cible** : `src/main/java/com/fortnite/pronos/service/draft/SwapSoloService.java`
- `executeSoloSwap(UUID gameId, UUID userId, UUID playerOutId, UUID playerInId)` → `SwapSoloResponse`
- Validations : playerOut dans équipe, playerIn libre, même région, tranche strictement pire
- Exécution : delete old pick + save new pick
- 6 dépendances (≤ 7 CouplingTest)

**3. `SwapSoloController`** : controller inexistant.
**Fix cible** : `src/main/java/com/fortnite/pronos/controller/SwapSoloController.java`
- `POST /api/games/{gameId}/draft/swap-solo` → 200 + `SwapSoloResponse`
- Résout userId via `UserResolver.getCurrentUser(request).getId()`
- 2 dépendances : `SwapSoloService`, `UserResolver`

**4. DTO** : `SwapSoloRequest(UUID playerOutId, UUID playerInId)` + `SwapSoloResponse(UUID draftId, UUID participantId, UUID playerOutId, UUID playerInId)` records.

## Tasks / Subtasks

- [x] Task 1: Étendre `DraftPickRepositoryPort` + `DraftPickRepository` (AC: #5)
  - [x] Ajouter `existsByDraftIdAndParticipantIdAndPlayerId(UUID, UUID, UUID)` dans port + repo

- [x] Task 2: Créer DTOs `SwapSoloRequest` + `SwapSoloResponse` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/dto/SwapSoloRequest.java`
  - [x] `src/main/java/com/fortnite/pronos/dto/SwapSoloResponse.java`

- [x] Task 3: Créer `SwapSoloService` (AC: #1–5)
  - [x] `src/main/java/com/fortnite/pronos/service/draft/SwapSoloService.java`
  - [x] `executeSoloSwap(UUID gameId, UUID userId, UUID playerOutId, UUID playerInId)` → `SwapSoloResponse`
  - [x] Validation playerOut dans équipe → `InvalidSwapException`
  - [x] Validation playerIn libre → `InvalidSwapException`
  - [x] Validation même région → `InvalidSwapException`
  - [x] Validation rang strictement pire (`parseTranche(playerIn) > parseTranche(playerOut)`) → `InvalidSwapException`

- [x] Task 4: Créer `SwapSoloController` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/controller/SwapSoloController.java`
  - [x] `POST /api/games/{gameId}/draft/swap-solo` → 200 + `SwapSoloResponse`

- [x] Task 5: Tests unitaires `SwapSoloServiceTest` (AC: #1–5)
  - [x] `src/test/java/com/fortnite/pronos/service/draft/SwapSoloServiceTest.java`
  - [x] `whenValid_executesSwapAndReturnsResponse`
  - [x] `whenPlayerOutNotInTeam_throwsInvalidSwapException`
  - [x] `whenPlayerInAlreadyPicked_throwsInvalidSwapException`
  - [x] `whenDifferentRegion_throwsInvalidSwapException`
  - [x] `whenRankNotStrictlyWorse_trancheEqual_throwsInvalidSwapException`
  - [x] `whenRankBetter_trancheInLowerThanOut_throwsInvalidSwapException`
  - [x] `whenGameNotFound_throwsGameNotFoundException`
  - [x] `whenNoDraftActive_throwsInvalidDraftStateException`

- [x] Task 6: Tests unitaires `SwapSoloControllerTest` (AC: #1)
  - [x] `src/test/java/com/fortnite/pronos/controller/SwapSoloControllerTest.java`
  - [x] `whenValid_returns200WithResponse`
  - [x] `whenNotAuthenticated_returns401`

## Dev Notes

### Logique de comparaison de rang
```java
// "Rang strictement pire" = tranche numérique strictement supérieure (1=meilleur, 5=pire)
private int parseTranche(com.fortnite.pronos.domain.player.model.Player player) {
  try {
    return Integer.parseInt(player.getTranche().trim());
  } catch (NumberFormatException e) {
    throw new InvalidSwapException("Invalid tranche format for player: " + player.getId());
  }
}
// Condition : parseTranche(playerIn) > parseTranche(playerOut) → sinon InvalidSwapException
```

### Pattern findActiveDraftEntity (depuis GameDraftService / AdminDraftRosterService)
```java
private com.fortnite.pronos.model.Draft findDraftEntity(UUID gameId) {
  com.fortnite.pronos.domain.draft.model.Draft domainDraft =
      draftDomainRepository.findActiveByGameId(gameId)
          .orElseThrow(() -> new InvalidDraftStateException("No active draft for game: " + gameId));
  return draftRepository.findById(domainDraft.getId())
      .orElseThrow(() -> new InvalidDraftStateException("Draft entity not found"));
}
```

### Pattern toLegacyPlayer (depuis GameDraftService / AdminDraftRosterService)
```java
// Réutiliser le pattern identique à AdminDraftRosterService.toLegacyPlayer()
```

### SwapSoloService — structure complète
```java
@Slf4j @Service @RequiredArgsConstructor @Transactional
public class SwapSoloService {
  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;

  public SwapSoloResponse executeSoloSwap(UUID gameId, UUID userId, UUID playerOutId, UUID playerInId) {
    gameDomainRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    com.fortnite.pronos.model.GameParticipant participant =
        gameParticipantRepository.findByUserIdAndGameId(userId, gameId)
            .orElseThrow(() -> new InvalidSwapException("Participant not found for this game"));

    requirePlayerInParticipantTeam(draft.getId(), participant.getId(), playerOutId);

    List<UUID> pickedIds = draftPickRepository.findPickedPlayerIdsByDraftId(draft.getId());
    requirePlayerInFree(pickedIds, playerInId);

    com.fortnite.pronos.domain.player.model.Player playerOut = findDomainPlayer(playerOutId);
    com.fortnite.pronos.domain.player.model.Player playerIn = findDomainPlayer(playerInId);

    requireSameRegion(playerOut, playerIn);
    requireStrictlyWorseRank(playerOut, playerIn);

    draftPickRepository.deleteByDraftIdAndPlayerId(draft.getId(), playerOutId);
    com.fortnite.pronos.model.Player legacyPlayerIn = toLegacyPlayer(playerIn);
    com.fortnite.pronos.model.DraftPick newPick = new com.fortnite.pronos.model.DraftPick(draft, participant, legacyPlayerIn, 0, 0);
    draftPickRepository.save(newPick);

    log.info("Swap solo: game={} user={} out={} in={}", gameId, userId, playerOutId, playerInId);
    return new SwapSoloResponse(draft.getId(), participant.getId(), playerOutId, playerInId);
  }
}
```

### Contraintes architecture
- `CouplingTest` : 6 dépendances dans `SwapSoloService` (max 7) ✓
- `NamingConventionTest` : `SwapSoloService` ✓, `SwapSoloController` ✓
- Spotless : `mvn spotless:apply` avant `mvn test`

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- 10/10 tests green (8 service + 2 controller). Full backend suite: 2155 run, 19 failures + 1 error (all pre-existing). No regressions.
- `UnnecessaryStubbingException` fix: `stubPlayers()` uses `lenient()` for `toLegacyPlayer` stubs (only consumed in happy path). Removed `playerOut.getId()` stub (never consumed — service uses UUID param directly).
- Controller follows `SnakeDraftController` pattern: `userResolver.resolve(username, httpRequest)` + 401 guard.
- Runtime follow-up (2026-03-07): `swap-solo` revalide localement avec JWT reel, persistence PostgreSQL et verification `draft audit`; le contrat `400 INVALID_SWAP` a aussi ete rejoue sur un cas de rang invalide. Voir `5-1-swap-solo-vers-joueur-libre-valide-runtime-validation-2026-03-07.md`.

### File List

- `src/main/java/com/fortnite/pronos/domain/port/out/DraftPickRepositoryPort.java` (modified: +existsByDraftIdAndParticipantIdAndPlayerId)
- `src/main/java/com/fortnite/pronos/repository/DraftPickRepository.java` (modified: +@Query implementation)
- `src/main/java/com/fortnite/pronos/dto/SwapSoloRequest.java` (created)
- `src/main/java/com/fortnite/pronos/dto/SwapSoloResponse.java` (created)
- `src/main/java/com/fortnite/pronos/service/draft/SwapSoloService.java` (created — 7 deps, +DraftSwapAuditRepositoryPort)
- `src/main/java/com/fortnite/pronos/controller/SwapSoloController.java` (created)
- `src/test/java/com/fortnite/pronos/service/draft/SwapSoloServiceTest.java` (created)
- `src/test/java/com/fortnite/pronos/controller/SwapSoloControllerTest.java` (created)

**Story 5.3 foundations créées en avance (dans le scope de cette story):**
- `src/main/java/com/fortnite/pronos/domain/draft/model/DraftSwapAuditEntry.java` (created)
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftSwapAuditRepositoryPort.java` (created)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftSwapAuditRepositoryAdapter.java` (created)
- `src/main/java/com/fortnite/pronos/model/DraftSwapAuditEntity.java` (created)
- `src/main/java/com/fortnite/pronos/repository/DraftSwapAuditJpaRepository.java` (created)
- `src/main/java/com/fortnite/pronos/service/draft/DraftAuditService.java` (created — Story 5.3 query)
- `src/main/java/com/fortnite/pronos/dto/DraftAuditEntryResponse.java` (created)
- `src/main/resources/db/migration/V39__add_draft_swap_audits.sql` (created)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**M1 — DOCUMENTED**: Fichiers Story 5.3 créés en avance non listés dans le File List initial. File List mis à jour pour les inclure.

**M2 — FIXED**: Aucun test de sécurité pour `SwapSoloController`. Créé `SecurityConfigSwapSoloAuthorizationTest` (1 test : unauthenticatedCannotSwapSolo → 401/403).

**M3 — FIXED**: NPE dans `parseTranche()` si `player.getTranche()` retourne null → NullPointerException → 500 au lieu de 400 INVALID_SWAP. Ajouté null-guard au début de `parseTranche()` dans `SwapSoloService`.

**L1 — FIXED**: `SwapSoloControllerTest` expose bien un `@DisplayName("Swap Solo")` sur la classe imbriquée concernée ; le follow-up review etait obsolete par rapport a l'etat courant du fichier.

**L2 — FIXED**: `requirePlayerIsFree()` utilise maintenant un `HashSet` dans `SwapSoloService`, ce qui ferme l'observation performance O(n) du review low.

**L3 — FIXED**: le Technical Context est resynchronise avec le comportement reel: `InvalidDraftStateException` est documentee en `409 CONFLICT`, plus en `400`.

### Action items

- Aucun action item restant sur cette story apres revalidation runtime du 2026-03-07.
