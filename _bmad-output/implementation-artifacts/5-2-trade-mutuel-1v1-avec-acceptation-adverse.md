# Story 5.2: Trade mutuel 1v1 avec acceptation adverse

Status: done

## Story

As a participant,
I want proposer un trade 1v1 avec un autre participant,
so that l'échange ne s'exécute qu'après consentement explicite de l'autre partie.

## Acceptance Criteria

1. **Given** un participant authentifié soumet `POST /api/games/{gameId}/draft/trade` avec `{targetParticipantId, playerFromProposerId, playerFromTargetId}` et que `playerFromProposerId` est dans l'équipe du proposant et `playerFromTargetId` dans l'équipe adverse, **When** la proposition est traitée, **Then** une proposition de trade PENDING est créée et la réponse 201 contient les détails de la proposition.

2. **Given** une proposition PENDING existe, **When** le participant adverse (targetParticipant) soumet `POST /api/games/{gameId}/draft/trade/{tradeId}/accept`, **Then** les deux joueurs sont échangés (DraftPicks swappés) et la réponse 200 indique ACCEPTED.

3. **Given** une proposition PENDING existe, **When** le participant adverse soumet `POST /api/games/{gameId}/draft/trade/{tradeId}/reject`, **Then** aucun échange n'est effectué et la réponse 200 indique REJECTED.

4. **Given** `playerFromProposerId` n'est pas dans l'équipe du proposant, **When** la proposition est soumise, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

5. **Given** `playerFromTargetId` n'est pas dans l'équipe du participant adverse, **When** la proposition est soumise, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

6. **Given** un participant tente d'accepter/rejeter une proposition dont il n'est pas la cible, **When** la requête est soumise, **Then** la requête est refusée 400 (`INVALID_SWAP`) avec message explicite.

## Technical Context

### FR Coverage
- **FR-34**: Trade mutuel 1 joueur vs 1 joueur, **sans restriction de région ni de rang**
- **FR-35**: Trade requiert l'acceptation explicite du participant adverse

### Infrastructure déjà en place (READY)

#### DraftPickRepositoryPort (READY — Story 5.1 étendu)
- `existsByDraftIdAndParticipantIdAndPlayerId(UUID, UUID, UUID)` ✓
- `deleteByDraftIdAndPlayerId(UUID, UUID)` ✓
- `save(DraftPick)` ✓
- **MANQUANT** : `Optional<DraftPick> findByDraftIdAndParticipantIdAndPlayerId(UUID, UUID, UUID)`
  → Nécessaire pour récupérer l'entité JPA DraftPick (avec le Player) avant le swap

#### Lookup ports (READY)
- `GameDomainRepositoryPort.findById(UUID)` ✓
- `DraftDomainRepositoryPort.findActiveByGameId(UUID)` ✓
- `DraftRepositoryPort.findById(UUID)` ✓
- `GameParticipantRepositoryPort.findByUserIdAndGameId(UUID, UUID)` ✓

#### Exceptions (READY — DomainExceptionHandler)
- `InvalidSwapException` → 400 `INVALID_SWAP` ✓
- `GameNotFoundException` → 404 ✓
- `InvalidDraftStateException` → 400 ✓

#### Pattern de findDraftEntity (READY — réutiliser depuis SwapSoloService)
```java
private com.fortnite.pronos.model.Draft findDraftEntity(UUID gameId) {
  com.fortnite.pronos.domain.draft.model.Draft domainDraft =
      draftDomainRepository.findActiveByGameId(gameId)
          .orElseThrow(() -> new InvalidDraftStateException("No active draft found for game: " + gameId));
  return draftRepository.findById(domainDraft.getId())
      .orElseThrow(() -> new InvalidDraftStateException("Draft entity not found for game: " + gameId));
}
```

#### Pattern UserResolver (READY — SwapSoloController / SnakeDraftController)
```java
User user = userResolver.resolve(username, httpRequest);
if (user == null) {
  return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

### Gap 1 — DraftPickRepositoryPort : méthode manquante
```java
// Port
Optional<com.fortnite.pronos.model.DraftPick> findByDraftIdAndParticipantIdAndPlayerId(
    UUID draftId, UUID participantId, UUID playerId);

// DraftPickRepository @Query
@Query("SELECT dp FROM DraftPick dp WHERE dp.draft.id = :draftId"
    + " AND dp.participant.id = :participantId AND dp.player.id = :playerId")
Optional<DraftPick> findByDraftIdAndParticipantIdAndPlayerId(
    @Param("draftId") UUID draftId,
    @Param("participantId") UUID participantId,
    @Param("playerId") UUID playerId);
```

### Gap 2 — Nouveau modèle domaine : DraftParticipantTrade

```java
// src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTradeStatus.java
public enum DraftParticipantTradeStatus {
  PENDING, ACCEPTED, REJECTED, CANCELLED;
  public boolean isTerminal() {
    return this == ACCEPTED || this == REJECTED || this == CANCELLED;
  }
}

// src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTrade.java
// final class, no JPA/Spring/Lombok (DomainIsolationTest)
public final class DraftParticipantTrade {
  UUID id, draftId, proposerParticipantId, targetParticipantId;
  UUID playerFromProposerId, playerFromTargetId;
  DraftParticipantTradeStatus status;
  LocalDateTime proposedAt;
  LocalDateTime resolvedAt; // nullable

  // Creation constructor
  public DraftParticipantTrade(UUID draftId, UUID proposerParticipantId, UUID targetParticipantId,
      UUID playerFromProposerId, UUID playerFromTargetId) { ... }

  // restore() static factory (persistence reconstitution)
  public static DraftParticipantTrade restore(...) { ... }

  // Business method
  public DraftParticipantTrade accept() { ... }  // → ACCEPTED + resolvedAt=now
  public DraftParticipantTrade reject() { ... }  // → REJECTED + resolvedAt=now
}
```

### Gap 3 — Port + adaptateur JPA

```
// Port
src/main/java/com/fortnite/pronos/domain/port/out/DraftParticipantTradeRepositoryPort.java
  DraftParticipantTrade save(DraftParticipantTrade)
  Optional<DraftParticipantTrade> findById(UUID)
  List<DraftParticipantTrade> findPendingByDraftId(UUID)

// JPA Entity (modèle legacy, pas domaine)
src/main/java/com/fortnite/pronos/model/DraftParticipantTradeEntity.java
  UUID id, draftId, proposerParticipantId, targetParticipantId
  UUID playerFromProposerId, playerFromTargetId
  String status  // @Enumerated STRING
  LocalDateTime proposedAt, resolvedAt

// JPA Repository
src/main/java/com/fortnite/pronos/repository/DraftParticipantTradeJpaRepository.java
  extends JpaRepository<DraftParticipantTradeEntity, UUID>
  List<DraftParticipantTradeEntity> findByDraftIdAndStatus(UUID draftId, String status)

// Adapter
src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftParticipantTradeRepositoryAdapter.java
  implements DraftParticipantTradeRepositoryPort

// Flyway V38 migration
src/main/resources/db/migration/V38__add_draft_participant_trades.sql
  CREATE TABLE draft_participant_trades (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    proposer_participant_id UUID NOT NULL,
    target_participant_id UUID NOT NULL,
    player_from_proposer_id UUID NOT NULL,
    player_from_target_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    proposed_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP
  );
```

### Gap 4 — Service DraftParticipantTradeService (6 deps)
```java
@Slf4j @Service @RequiredArgsConstructor @Transactional
public class DraftParticipantTradeService {
  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final DraftParticipantTradeRepositoryPort tradeRepository;

  // proposeTrade: valide playerFromProposerId dans équipe proposer, playerFromTargetId dans équipe target
  // Crée trade PENDING, retourne DraftTradeProposalResponse
  public DraftTradeProposalResponse proposeTrade(UUID gameId, UUID proposerUserId,
      UUID targetParticipantId, UUID playerFromProposerId, UUID playerFromTargetId)

  // acceptTrade: valide caller = targetParticipant, swap picks, mark ACCEPTED
  public DraftTradeProposalResponse acceptTrade(UUID gameId, UUID callerUserId, UUID tradeId)

  // rejectTrade: valide caller = targetParticipant, mark REJECTED
  public DraftTradeProposalResponse rejectTrade(UUID gameId, UUID callerUserId, UUID tradeId)
}
```

### Gap 5 — DTOs
```java
// src/main/java/com/fortnite/pronos/dto/DraftTradeProposalRequest.java
public record DraftTradeProposalRequest(
    UUID targetParticipantId, UUID playerFromProposerId, UUID playerFromTargetId) {}

// src/main/java/com/fortnite/pronos/dto/DraftTradeProposalResponse.java
public record DraftTradeProposalResponse(
    UUID tradeId, UUID draftId,
    UUID proposerParticipantId, UUID targetParticipantId,
    UUID playerFromProposerId, UUID playerFromTargetId,
    String status) {}
```

### Gap 6 — Controller
```java
@RestController @RequestMapping("/api/games/{gameId}/draft/trade")
public class DraftParticipantTradeController {
  // POST /api/games/{gameId}/draft/trade → 201 + DraftTradeProposalResponse
  // POST /api/games/{gameId}/draft/trade/{tradeId}/accept → 200
  // POST /api/games/{gameId}/draft/trade/{tradeId}/reject → 200
}
```

### Contraintes architecture
- `CouplingTest` : 6 dépendances dans `DraftParticipantTradeService` (max 7) ✓
- `NamingConventionTest` : `DraftParticipantTradeService` ✓, `DraftParticipantTradeController` ✓
- `DomainIsolationTest` : `DraftParticipantTrade` → `final`, 0 imports JPA/Spring/Hibernate/Lombok
- Spotless : `mvn spotless:apply` avant `mvn test`

### Pattern du swap (acceptTrade)
```java
// 1. Load picks (réutilise entity avec Player déjà chargé)
DraftPick proposerPick = findPickOrThrow(draftId, proposerParticipantId, playerFromProposerId);
DraftPick targetPick   = findPickOrThrow(draftId, targetParticipantId, playerFromTargetId);

// 2. Delete both picks
draftPickRepository.deleteByDraftIdAndPlayerId(draftId, playerFromProposerId);
draftPickRepository.deleteByDraftIdAndPlayerId(draftId, playerFromTargetId);

// 3. Cross-assign (player from one goes to the other's participant)
draftPickRepository.save(new DraftPick(legacyDraft, proposerParticipantEntity, targetPick.getPlayer(), 0, 0));
draftPickRepository.save(new DraftPick(legacyDraft, targetParticipantEntity, proposerPick.getPlayer(), 0, 0));

// 4. Update trade status
DraftParticipantTrade accepted = trade.accept();
tradeRepository.save(accepted);
```

## Tasks / Subtasks

- [x] Task 1: Étendre `DraftPickRepositoryPort` + `DraftPickRepository` (AC: #2, #4, #5)
  - [x] Ajouter `Optional<DraftPick> findByDraftIdAndParticipantIdAndPlayerId(UUID, UUID, UUID)` dans port + repo (@Query)

- [x] Task 2: Créer domaine `DraftParticipantTradeStatus` + `DraftParticipantTrade` (AC: #1–6)
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTradeStatus.java`
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTrade.java` (final, restore(), accept(), reject())

- [x] Task 3: Port + adaptateur JPA (AC: #1–6)
  - [x] `DraftParticipantTradeRepositoryPort.java` (save, findById, findPendingByDraftId)
  - [x] `DraftParticipantTradeEntity.java` (JPA @Entity, @Table("draft_participant_trades"))
  - [x] `DraftParticipantTradeJpaRepository.java` (extends JpaRepository)
  - [x] `DraftParticipantTradeRepositoryAdapter.java` (implements port, in adapter/out/persistence/draft/)
  - [x] `V38__add_draft_participant_trades.sql` (CREATE TABLE draft_participant_trades)

- [x] Task 4: Créer DTOs (AC: #1–3)
  - [x] `src/main/java/com/fortnite/pronos/dto/DraftTradeProposalRequest.java`
  - [x] `src/main/java/com/fortnite/pronos/dto/DraftTradeProposalResponse.java`

- [x] Task 5: Créer `DraftParticipantTradeService` (AC: #1–6)
  - [x] `src/main/java/com/fortnite/pronos/service/draft/DraftParticipantTradeService.java`
  - [x] `proposeTrade(gameId, proposerUserId, targetParticipantId, playerFromProposerId, playerFromTargetId)` → DraftTradeProposalResponse (201)
  - [x] `acceptTrade(gameId, callerUserId, tradeId)` → DraftTradeProposalResponse (200)
  - [x] `rejectTrade(gameId, callerUserId, tradeId)` → DraftTradeProposalResponse (200)
  - [x] Validation : playerFromProposer dans équipe proposer → InvalidSwapException
  - [x] Validation : playerFromTarget dans équipe target → InvalidSwapException
  - [x] Validation : caller est bien targetParticipant pour accept/reject → InvalidSwapException

- [x] Task 6: Créer `DraftParticipantTradeController` (AC: #1–3)
  - [x] `src/main/java/com/fortnite/pronos/controller/DraftParticipantTradeController.java`
  - [x] `POST /api/games/{gameId}/draft/trade` → 201 + DraftTradeProposalResponse
  - [x] `POST /api/games/{gameId}/draft/trade/{tradeId}/accept` → 200
  - [x] `POST /api/games/{gameId}/draft/trade/{tradeId}/reject` → 200

- [x] Task 7: Tests unitaires `DraftParticipantTradeServiceTest` (AC: #1–6)
  - [x] `src/test/java/com/fortnite/pronos/service/draft/DraftParticipantTradeServiceTest.java`
  - [x] `proposeTrade_whenValid_createsPendingTrade`
  - [x] `proposeTrade_whenProposerPlayerNotInTeam_throwsInvalidSwapException`
  - [x] `proposeTrade_whenTargetPlayerNotInTeam_throwsInvalidSwapException`
  - [x] `acceptTrade_whenValid_swapsPicksAndAccepts`
  - [x] `acceptTrade_whenCallerNotTarget_throwsInvalidSwapException`
  - [x] `rejectTrade_whenValid_rejectsTrade`
  - [x] `rejectTrade_whenCallerNotTarget_throwsInvalidSwapException`

- [x] Task 8: Tests unitaires `DraftParticipantTradeControllerTest` (AC: #1–3)
  - [x] `src/test/java/com/fortnite/pronos/controller/DraftParticipantTradeControllerTest.java`
  - [x] `proposeTrade_whenAuthenticated_returns201`
  - [x] `acceptTrade_whenAuthenticated_returns200`
  - [x] `rejectTrade_whenAuthenticated_returns200`

## Dev Notes

### Attention piège : GameParticipantRepositoryPort.findByUserIdAndGameId vs findById
- Pour obtenir le `targetParticipant` (entité JPA), utiliser `gameParticipantRepository.findById(targetParticipantId)` si disponible, ou `findByUserIdAndGameId` si l'on connaît userId
- Pour `proposeTrade`, le `proposerParticipant` est résolu depuis `proposerUserId` via `findByUserIdAndGameId(proposerUserId, gameId)`
- Pour `targetParticipant`, on a son `UUID` directement (passé dans la requête comme `targetParticipantId`), pas son userId
- Vérifier si `GameParticipantRepositoryPort` expose un `findById(UUID participantId)` — sinon ajouter la méthode

### Vérifier GameParticipantRepositoryPort
Vérifier les méthodes disponibles dans `GameParticipantRepositoryPort` avant d'implémenter. Si `findById(UUID participantId)` n'existe pas, chercher un autre moyen d'identifier la targetParticipant entity, ou ajouter la méthode.

### DomainIsolationTest : DraftParticipantTrade doit être final
La classe domaine `DraftParticipantTrade` doit être `final` et ne pas importer de classes JPA/Spring/Hibernate/Lombok. Utiliser `java.time.LocalDateTime`, `java.util.UUID` seulement.

### Pattern DraftPick constructor
```java
new DraftPick(draft, participant, player, round, pickNumber)
// draft = com.fortnite.pronos.model.Draft (JPA)
// participant = com.fortnite.pronos.model.GameParticipant (JPA)
// player = com.fortnite.pronos.model.Player (JPA)
// round = 0, pickNumber = 0 (pour swap/trade)
```

### Naming convention
- Entity JPA : `DraftParticipantTradeEntity` (pour éviter conflit avec domaine `DraftParticipantTrade`)
- JPA Repo : `DraftParticipantTradeJpaRepository` (pour éviter conflit avec port adapter)
- Adapter : `DraftParticipantTradeRepositoryAdapter` (dans `adapter/out/persistence/draft/`)

### Sans restriction de région ni de rang (FR-34)
Contrairement au swap solo (FR-32/33), il n'y a **aucune vérification de région ou de tranche** dans le trade mutuel. Les deux participants échangent librement.

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- 20/20 tests green after review fixes (11 service + 6 controller + 3 security). Full backend suite clean.
- `requireTradePending()` guard added in `acceptTrade()` and `rejectTrade()` — non-PENDING trade now returns 400 INVALID_SWAP instead of propagating `IllegalStateException` as DRAFT_WINDOW_VIOLATION (misleading error code).
- Security test added: 3 endpoints validated unauthenticated → 401/403.
- Runtime follow-up 2026-03-07 closed: fixed Hibernate 6.6 persistence mismatch on pre-assigned trade UUIDs, revalidated `POST /draft/trade` in `201`, `POST /accept` in `200`, and confirmed swapped `draft_picks` in local PostgreSQL.

### File List

- `src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTradeStatus.java` (created)
- `src/main/java/com/fortnite/pronos/domain/draft/model/DraftParticipantTrade.java` (created — final, restore(), accept(), reject())
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftParticipantTradeRepositoryPort.java` (created)
- `src/main/java/com/fortnite/pronos/model/DraftParticipantTradeEntity.java` (created — JPA entity)
- `src/main/java/com/fortnite/pronos/repository/DraftParticipantTradeJpaRepository.java` (created)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftParticipantTradeRepositoryAdapter.java` (created)
- `src/main/resources/db/migration/V38__add_draft_participant_trades.sql` (created)
- `src/main/java/com/fortnite/pronos/dto/DraftTradeProposalRequest.java` (created)
- `src/main/java/com/fortnite/pronos/dto/DraftTradeProposalResponse.java` (created)
- `src/main/java/com/fortnite/pronos/service/draft/DraftParticipantTradeService.java` (created — 6 deps, +requireTradePending guard added in review)
- `src/main/java/com/fortnite/pronos/controller/DraftParticipantTradeController.java` (created)
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftPickRepositoryPort.java` (modified: +findByDraftIdAndParticipantIdAndPlayerId)
- `src/main/java/com/fortnite/pronos/repository/DraftPickRepository.java` (modified: +@Query implementation)
- `src/test/java/com/fortnite/pronos/service/draft/DraftParticipantTradeServiceTest.java` (created — 11 tests in 3 nested classes)
- `src/test/java/com/fortnite/pronos/controller/DraftParticipantTradeControllerTest.java` (created — 6 tests in 3 nested classes)
- `src/test/java/com/fortnite/pronos/config/SecurityConfigDraftParticipantTradeAuthorizationTest.java` (created in review — 3 tests)
- `src/test/java/com/fortnite/pronos/repository/DraftParticipantTradeJpaRepositoryTest.java` (created — guards assigned UUID persistence under Hibernate 6.6)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**H1 — FIXED**: No security test for `DraftParticipantTradeController`. Created `SecurityConfigDraftParticipantTradeAuthorizationTest` (3 tests: unauthenticatedCannotProposeTrade/AcceptTrade/RejectTrade → 401/403).

**M1 — FIXED**: `trade.accept()`/`trade.reject()` threw `IllegalStateException` on non-PENDING trade → caught by `DomainExceptionHandler` as `DRAFT_WINDOW_VIOLATION` 409 (semantically wrong for a trade state error). Added `requireTradePending()` guard in `acceptTrade()` and `rejectTrade()` throwing `InvalidSwapException` → 400 INVALID_SWAP. Added 2 new tests in service test.

**C1 — DOCUMENTED**: Empty `File List` and `Completion Notes List` in Dev Agent Record. Populated above.

### Action items

- [x] **[AI-Review][Low][L1]** : `DraftParticipantTradeControllerTest` — class-level `@DisplayName("DraftParticipantTradeController")` added on 2026-03-07. Nested classes were already aligned.
