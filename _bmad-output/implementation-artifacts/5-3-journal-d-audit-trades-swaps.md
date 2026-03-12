# Story 5.3: Journal d'audit trades/swaps

Status: done

## Story

As an admin,
I want historiser tous les swaps/trades avec horodatage,
so that les litiges puissent être analysés et arbitrés.

## Acceptance Criteria

1. **Given** un swap solo est exécuté avec succès (SwapSoloService), **When** le swap est persisté, **Then** une entrée d'audit `DraftSwapAuditEntry` est créée et persistée avec `draftId`, `participantId`, `playerOutId`, `playerInId`, `occurredAt`.

2. **Given** une proposition de trade est créée (Story 5.2 — PENDING), **When** l'événement est traité, **Then** la trace est déjà persistée dans `DraftParticipantTrade` (proposedAt ✓) — aucune action supplémentaire requise pour PROPOSED.

3. **Given** un trade est accepté ou rejeté, **When** l'événement est traité, **Then** la trace est dans `DraftParticipantTrade.resolvedAt + status` (ACCEPTED/REJECTED) — aucune action supplémentaire requise pour ACCEPTED/REJECTED.

4. **Given** un draft actif existe pour une partie, **When** `GET /api/games/{gameId}/draft/audit` est appelé par un participant authentifié, **Then** la réponse 200 retourne une liste triée par date décroissante de toutes les entrées d'audit (swaps + trades), chacune avec `type`, `occurredAt`, et les IDs pertinents.

5. **Given** aucun swap ni trade n'a été effectué dans la partie, **When** `GET /api/games/{gameId}/draft/audit` est appelé, **Then** la réponse est une liste vide (200 + `[]`).

6. **Given** l'utilisateur n'est pas authentifié, **When** `GET /api/games/{gameId}/draft/audit` est appelé, **Then** la réponse est 401.

## Technical Context

### FR Coverage
- **FR-36**: Tout swap/trade est enregistré avec horodatage pour traçabilité et contestation

### Analyse de l'existant — ce qui est DÉJÀ persisté

#### Trades (Story 5.2 — DONE)
`DraftParticipantTradeEntity` (`draft_participant_trades`) contient :
- `proposed_at TIMESTAMP NOT NULL` → timestamp de proposition
- `resolved_at TIMESTAMP` → timestamp d'acceptation/rejet
- `status VARCHAR(20)` → PENDING / ACCEPTED / REJECTED / CANCELLED
- Tous les IDs : draftId, proposerParticipantId, targetParticipantId, playerFromProposerId, playerFromTargetId

**Conclusion** : les trades sont DÉJÀ une trace d'audit complète. Il suffit de les exposer via l'endpoint de consultation.

#### Swaps solo (Story 5.1 — GAP)
`SwapSoloService` modifie uniquement les `DraftPick` (delete + save), **sans aucune trace persistée**. C'est le seul gap à combler.

### Gap 1 — Domaine : DraftSwapAuditEntry

```java
// src/main/java/com/fortnite/pronos/domain/draft/model/DraftSwapAuditEntry.java
// OBLIGATOIRE : final, 0 imports JPA/Spring/Hibernate/Lombok (DomainIsolationTest)
public final class DraftSwapAuditEntry {
  private final UUID id;
  private final UUID draftId;
  private final UUID participantId;
  private final UUID playerOutId;
  private final UUID playerInId;
  private final java.time.LocalDateTime occurredAt;

  // Creation constructor (génère id + occurredAt=now)
  public DraftSwapAuditEntry(UUID draftId, UUID participantId, UUID playerOutId, UUID playerInId) { ... }

  // restore() static factory (persistence reconstitution)
  public static DraftSwapAuditEntry restore(UUID id, UUID draftId, UUID participantId,
      UUID playerOutId, UUID playerInId, LocalDateTime occurredAt) { ... }

  // Getters seulement — immuable
}
```

### Gap 2 — Port : DraftSwapAuditRepositoryPort

```java
// src/main/java/com/fortnite/pronos/domain/port/out/DraftSwapAuditRepositoryPort.java
public interface DraftSwapAuditRepositoryPort {
  DraftSwapAuditEntry save(DraftSwapAuditEntry entry);
  List<DraftSwapAuditEntry> findByDraftId(UUID draftId);
}
```

### Gap 3 — JPA + Adapter : DraftSwapAuditEntity

```java
// src/main/java/com/fortnite/pronos/model/DraftSwapAuditEntity.java
@Entity @Table(name = "draft_swap_audits") @Getter @Setter @NoArgsConstructor
public class DraftSwapAuditEntity {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false) private UUID draftId;
  @Column(nullable = false) private UUID participantId;
  @Column(nullable = false) private UUID playerOutId;
  @Column(nullable = false) private UUID playerInId;
  @Column(nullable = false) private LocalDateTime occurredAt;

  @PrePersist
  protected void onPersist() {
    if (occurredAt == null) occurredAt = LocalDateTime.now();
  }
}

// src/main/java/com/fortnite/pronos/repository/DraftSwapAuditJpaRepository.java
@Repository
public interface DraftSwapAuditJpaRepository extends JpaRepository<DraftSwapAuditEntity, UUID> {
  List<DraftSwapAuditEntity> findByDraftIdOrderByOccurredAtDesc(UUID draftId);
}

// src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftSwapAuditRepositoryAdapter.java
@Component @RequiredArgsConstructor
public class DraftSwapAuditRepositoryAdapter implements DraftSwapAuditRepositoryPort {
  private final DraftSwapAuditJpaRepository jpaRepository;

  @Override
  public DraftSwapAuditEntry save(DraftSwapAuditEntry entry) {
    DraftSwapAuditEntity entity = new DraftSwapAuditEntity();
    entity.setDraftId(entry.getDraftId());
    entity.setParticipantId(entry.getParticipantId());
    entity.setPlayerOutId(entry.getPlayerOutId());
    entity.setPlayerInId(entry.getPlayerInId());
    entity.setOccurredAt(entry.getOccurredAt());
    DraftSwapAuditEntity saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<DraftSwapAuditEntry> findByDraftId(UUID draftId) {
    return jpaRepository.findByDraftIdOrderByOccurredAtDesc(draftId)
        .stream().map(this::toDomain).toList();
  }

  private DraftSwapAuditEntry toDomain(DraftSwapAuditEntity e) {
    return DraftSwapAuditEntry.restore(e.getId(), e.getDraftId(), e.getParticipantId(),
        e.getPlayerOutId(), e.getPlayerInId(), e.getOccurredAt());
  }
}
```

### Gap 4 — Migration V39

```sql
-- src/main/resources/db/migration/V39__add_draft_swap_audits.sql
CREATE TABLE IF NOT EXISTS draft_swap_audits (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL,
  participant_id UUID NOT NULL,
  player_out_id UUID NOT NULL,
  player_in_id UUID NOT NULL,
  occurred_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_draft_swap_audits_draft_id
  ON draft_swap_audits (draft_id);
```

### Gap 5 — Extension DraftParticipantTradeRepositoryPort

Ajouter `findByDraftId(UUID draftId)` → `List<DraftParticipantTrade>` (toutes statuts) pour l'audit.

```java
// Port — ajouter :
List<DraftParticipantTrade> findByDraftId(UUID draftId);

// DraftParticipantTradeJpaRepository — ajouter :
List<DraftParticipantTradeEntity> findByDraftId(UUID draftId);

// DraftParticipantTradeRepositoryAdapter — ajouter :
@Override
public List<DraftParticipantTrade> findByDraftId(UUID draftId) {
  return jpaRepository.findByDraftId(draftId).stream().map(this::toDomain).toList();
}
```

### Gap 6 — Mise à jour SwapSoloService (7e dép = MAX)

Ajouter `DraftSwapAuditRepositoryPort` comme 7e dépendance (CouplingTest : max = 7).

```java
// Après le swap réussi (après draftPickRepository.save(newPick)) :
DraftSwapAuditEntry audit = new DraftSwapAuditEntry(
    draftId, participantId, playerOutId, playerInId);
auditRepository.save(audit);
```

**ATTENTION** : SwapSoloService a actuellement 6 deps. Ajout du 7e = limite atteinte. Ne pas ajouter d'autre dépendance dans ce service.

### Gap 7 — DTO de réponse

```java
// src/main/java/com/fortnite/pronos/dto/DraftAuditEntryResponse.java
// type: "SWAP_SOLO" | "TRADE_PROPOSED" | "TRADE_ACCEPTED" | "TRADE_REJECTED"
public record DraftAuditEntryResponse(
    UUID id,
    String type,
    java.time.LocalDateTime occurredAt,
    UUID participantId,           // null pour les trades (proposer ≠ single participant)
    UUID proposerParticipantId,   // null pour les swaps
    UUID targetParticipantId,     // null pour les swaps
    UUID playerFromProposerId,    // null pour les swaps (= playerOutId)
    UUID playerFromTargetId,      // null pour les swaps (= playerInId)
    UUID playerOutId,             // null pour les trades
    UUID playerInId               // null pour les trades
) {}
```

Simplification alternative (recommandée) : utiliser un `Map<String, Object>` ou un record unique à champs communs — voir ci-dessous.

**Design simplifié recommandé** (moins de nulls, plus clair) :
```java
public record DraftAuditEntryResponse(
    UUID id,
    String type,               // "SWAP_SOLO" | "TRADE_PROPOSED" | "TRADE_ACCEPTED" | "TRADE_REJECTED"
    String occurredAt,         // ISO string
    Map<String, String> details // clés/valeurs selon le type
) {}
```

Ou encore plus simple (un record plat avec tous les champs optionnels comme String) :
```java
public record DraftAuditEntryResponse(
    UUID id,
    String type,
    LocalDateTime occurredAt,
    UUID participantId,
    UUID proposerParticipantId,
    UUID targetParticipantId,
    UUID playerOutId,
    UUID playerInId
) {}
// Pour SWAP_SOLO : participantId + playerOutId + playerInId renseignés
// Pour TRADE_*   : proposerParticipantId + targetParticipantId + playerOutId (=fromProposer) + playerInId (=fromTarget) renseignés
```

### Gap 8 — DraftAuditService

```java
// src/main/java/com/fortnite/pronos/service/draft/DraftAuditService.java
@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class DraftAuditService {
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftSwapAuditRepositoryPort swapAuditRepository;
  private final DraftParticipantTradeRepositoryPort tradeRepository;

  public List<DraftAuditEntryResponse> getAuditForGame(UUID gameId) {
    UUID draftId = draftDomainRepository.findActiveByGameId(gameId)
        .orElseThrow(() -> new InvalidDraftStateException("No active draft for game: " + gameId))
        .getId();

    List<DraftAuditEntryResponse> swaps = swapAuditRepository.findByDraftId(draftId)
        .stream().map(this::toSwapResponse).toList();

    List<DraftAuditEntryResponse> trades = tradeRepository.findByDraftId(draftId)
        .stream().flatMap(t -> toTradeResponses(t).stream()).toList();

    return Stream.concat(swaps.stream(), trades.stream())
        .sorted(Comparator.comparing(DraftAuditEntryResponse::occurredAt).reversed())
        .toList();
  }

  private DraftAuditEntryResponse toSwapResponse(DraftSwapAuditEntry e) { ... }
  private List<DraftAuditEntryResponse> toTradeResponses(DraftParticipantTrade t) {
    // PENDING → TRADE_PROPOSED (proposedAt)
    // ACCEPTED → TRADE_PROPOSED + TRADE_ACCEPTED (proposedAt + resolvedAt)
    // REJECTED → TRADE_PROPOSED + TRADE_REJECTED (proposedAt + resolvedAt)
    ...
  }
}
```

**3 dépendances** : bien dans les limites.

### Gap 9 — DraftAuditController

```java
// src/main/java/com/fortnite/pronos/controller/DraftAuditController.java
@Slf4j @RestController @RequestMapping("/api/games/{gameId}/draft/audit") @RequiredArgsConstructor
public class DraftAuditController {
  private final DraftAuditService auditService;
  private final UserResolver userResolver;

  // GET /api/games/{gameId}/draft/audit → 200 + List<DraftAuditEntryResponse>
  @GetMapping
  public ResponseEntity<List<DraftAuditEntryResponse>> getAudit(
      @PathVariable UUID gameId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User user = userResolver.resolve(username, httpRequest);
    if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(auditService.getAuditForGame(gameId));
  }
}
```

### Contraintes architecture

- **CouplingTest** :
  - `SwapSoloService` : 6 deps → 7 après ajout `DraftSwapAuditRepositoryPort` (MAX = 7) ✓
  - `DraftAuditService` : 3 deps ✓
  - `DraftAuditController` : 2 deps ✓
- **NamingConventionTest** : `DraftAuditService` ✓, `DraftAuditController` ✓
- **DomainIsolationTest** : `DraftSwapAuditEntry` → `final`, 0 imports JPA/Spring/Hibernate/Lombok ✓
- **DependencyInversionTest** : `DraftAuditController` ne dépend pas de repositories directement ✓
- **Spotless** : `mvn spotless:apply` avant `mvn test`
- **Spring `@Autowired` sur SwapSoloService** : Si 2 constructeurs (prod + test), annoter le constructeur prod avec `@Autowired`

### Pattern de confirmation : `@Transactional(readOnly = true)` sur DraftAuditService

L'audit est une lecture seule → `@Transactional(readOnly = true)` sur le service / méthode `getAuditForGame`.

### Précédent SwapSoloService : Injection du 7e dep

Le constructeur de `SwapSoloService` est annoté `@RequiredArgsConstructor` (Lombok). Ajouter le 7e champ `private final DraftSwapAuditRepositoryPort auditRepository;` — Lombok génère automatiquement le bon constructeur. Pas de `@Autowired` nécessaire si un seul constructeur.

**ATTENTION test SwapSoloService** : Le test crée `SwapSoloService` via `new SwapSoloService(...)` dans `@BeforeEach`. Ajouter le 7e paramètre `auditRepository` mock. Ajouter également `when(auditRepository.save(any())).thenReturn(...)` pour le test du chemin nominal.

### Piège : toTradeResponses pour un trade PENDING

Un trade PENDING génère seulement 1 entrée `TRADE_PROPOSED` (resolvedAt = null). Vérifier null avant de créer l'entrée `TRADE_ACCEPTED/REJECTED` :
```java
List<DraftAuditEntryResponse> entries = new ArrayList<>();
entries.add(new DraftAuditEntryResponse(t.getId(), "TRADE_PROPOSED", t.getProposedAt(), ...));
if (t.getResolvedAt() != null) {
  String type = t.getStatus() == DraftParticipantTradeStatus.ACCEPTED ? "TRADE_ACCEPTED" : "TRADE_REJECTED";
  entries.add(new DraftAuditEntryResponse(t.getId(), type, t.getResolvedAt(), ...));
}
return entries;
```

## Tasks / Subtasks

- [x] Task 1: Créer domaine `DraftSwapAuditEntry` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftSwapAuditEntry.java` (final, restore(), getters only)

- [x] Task 2: Port + JPA adapter pour swap audit (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/domain/port/out/DraftSwapAuditRepositoryPort.java` (save, findByDraftId)
  - [x] `src/main/java/com/fortnite/pronos/model/DraftSwapAuditEntity.java` (@Entity @Table("draft_swap_audits"))
  - [x] `src/main/java/com/fortnite/pronos/repository/DraftSwapAuditJpaRepository.java`
  - [x] `src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftSwapAuditRepositoryAdapter.java`
  - [x] `src/main/resources/db/migration/V39__add_draft_swap_audits.sql`

- [x] Task 3: Étendre `DraftParticipantTradeRepositoryPort` (AC: #4)
  - [x] Ajouter `List<DraftParticipantTrade> findByDraftId(UUID draftId)` dans port
  - [x] Ajouter `List<DraftParticipantTradeEntity> findByDraftId(UUID draftId)` dans `DraftParticipantTradeJpaRepository`
  - [x] Implémenter dans `DraftParticipantTradeRepositoryAdapter`

- [x] Task 4: Mettre à jour `SwapSoloService` (AC: #1)
  - [x] Ajouter `DraftSwapAuditRepositoryPort auditRepository` comme 7e dépendance
  - [x] Appeler `auditRepository.save(new DraftSwapAuditEntry(draftId, participantId, playerOutId, playerInId))` après le swap réussi

- [x] Task 5: Créer DTO `DraftAuditEntryResponse` (AC: #4)
  - [x] `src/main/java/com/fortnite/pronos/dto/DraftAuditEntryResponse.java` (record)

- [x] Task 6: Créer `DraftAuditService` (AC: #4, #5)
  - [x] `src/main/java/com/fortnite/pronos/service/draft/DraftAuditService.java`
  - [x] `getAuditForGame(UUID gameId)` : merge swaps + trades, tri par date décroissante
  - [x] `toSwapResponse(DraftSwapAuditEntry)` → type "SWAP_SOLO"
  - [x] `toTradeResponses(DraftParticipantTrade)` → 1 ou 2 entrées selon status

- [x] Task 7: Créer `DraftAuditController` (AC: #4, #5, #6)
  - [x] `src/main/java/com/fortnite/pronos/controller/DraftAuditController.java`
  - [x] `GET /api/games/{gameId}/draft/audit` → 200 + liste, 401 si non authentifié

- [x] Task 8: Tests unitaires `DraftAuditServiceTest` (AC: #4, #5)
  - [x] `src/test/java/com/fortnite/pronos/service/draft/DraftAuditServiceTest.java`
  - [x] `whenSwapsAndTradesExist_returnsMergedSortedList`
  - [x] `whenNoDraftActive_throwsInvalidDraftStateException`
  - [x] `whenNoEventsExist_returnsEmptyList`
  - [x] `whenTradeIsPending_returnsOnlyProposedEntry`
  - [x] `whenTradeIsAccepted_returnsProposedAndAcceptedEntries`
  - [x] `whenTradeIsRejected_returnsProposedAndRejectedEntries`

- [x] Task 9: Tests unitaires `DraftAuditControllerTest` (AC: #4, #6)
  - [x] `src/test/java/com/fortnite/pronos/controller/DraftAuditControllerTest.java`
  - [x] `whenAuthenticated_returns200WithAuditList`
  - [x] `whenNotAuthenticated_returns401`

- [x] Task 10: Mettre à jour `SwapSoloServiceTest` (AC: #1)
  - [x] Ajouter `@Mock DraftSwapAuditRepositoryPort auditRepository`
  - [x] Ajouter le 7e paramètre dans `new SwapSoloService(...)` dans `@BeforeEach`
  - [x] Ajouter `when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0))` dans le path nominal
  - [x] Vérifier `verify(auditRepository).save(any(DraftSwapAuditEntry.class))` dans `whenValid_swapsPlayers`

## Dev Notes

### Architecture : 0 nouvelle violation

`DraftAuditController` → `DraftAuditService` → ports. Pas de dépendance directe controller → repository (`DependencyInversionTest`).

### Ordre d'implémentation recommandé

1. Domaine + port + JPA adapter (Tasks 1–2)
2. Extension trade port (Task 3)
3. SwapSoloService + test update (Tasks 4, 10)
4. DTO + Service + Controller (Tasks 5–7)
5. Tests service + controller (Tasks 8–9)

### Test de tri : vérifier l'ordre par date décroissante

Dans `DraftAuditServiceTest.whenSwapsAndTradesExist_returnsMergedSortedList`, construire des entrées avec des timestamps distincts et vérifier que la liste résultante est triée par `occurredAt` décroissant.

### Pas de WebSocket pour l'audit

L'audit est une lecture synchrone (`GET`). Pas de notification WebSocket pour cette story.

### Référence SwapSoloService : localisation du participantId

Dans `SwapSoloService.executeSoloSwap(gameId, userId, playerOutId, playerInId)`, le `participantId` est récupéré comme `proposerParticipant.getId()` (entité JPA GameParticipant). Utiliser cette valeur pour l'audit.

### Référence draftId dans SwapSoloService

`draftId = legacyDraft.getId()` où `legacyDraft` est le résultat de `findDraftEntity(gameId)`. Ce `draftId` existe déjà dans le service avant le swap — le passer directement au constructeur `DraftSwapAuditEntry`.

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

N/A — no blocking issues encountered.

### Completion Notes List

- **DraftSwapAuditEntry**: `final` domain class with creation constructor (auto-generates UUID + occurredAt=now) and `restore()` static factory. Zero JPA/Spring/Lombok imports — DomainIsolationTest compliant.
- **V39 migration**: `draft_swap_audits` table + `idx_draft_swap_audits_draft_id` index.
- **SwapSoloService**: reached MAX 7 deps after adding `DraftSwapAuditRepositoryPort`. CouplingTest limit respected.
- **DraftParticipantTradeRepositoryPort extended**: added `findByDraftId(UUID)` for all-status trade retrieval (vs existing `findPendingByDraftId`).
- **DraftAuditService**: 3 deps, `@Transactional(readOnly = true)`. Merges swaps + trades into a single stream sorted by `occurredAt` descending. PENDING trades → 1 entry; ACCEPTED/REJECTED → 2 entries (TRADE_PROPOSED + resolution).
- **Test results**: 2179 total, 17 new tests all green (7 DraftAuditServiceTest + 2 DraftAuditControllerTest + 8 SwapSoloServiceTest with 1 new audit verify). 19 failures + 1 error all pre-existing — 0 regressions.
- **Runtime follow-up 2026-03-07**: `GET /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/audit?user=thibaut` revalidated in `200`, with `SWAP_SOLO`, `TRADE_PROPOSED` and `TRADE_ACCEPTED` entries sorted newest-first.

### File List

**New files:**
- `src/main/java/com/fortnite/pronos/domain/draft/model/DraftSwapAuditEntry.java`
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftSwapAuditRepositoryPort.java`
- `src/main/java/com/fortnite/pronos/model/DraftSwapAuditEntity.java`
- `src/main/java/com/fortnite/pronos/repository/DraftSwapAuditJpaRepository.java`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftSwapAuditRepositoryAdapter.java`
- `src/main/resources/db/migration/V39__add_draft_swap_audits.sql`
- `src/main/java/com/fortnite/pronos/dto/DraftAuditEntryResponse.java`
- `src/main/java/com/fortnite/pronos/service/draft/DraftAuditService.java`
- `src/main/java/com/fortnite/pronos/controller/DraftAuditController.java`
- `src/test/java/com/fortnite/pronos/service/draft/DraftAuditServiceTest.java`
- `src/test/java/com/fortnite/pronos/controller/DraftAuditControllerTest.java`

**Modified files:**
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftParticipantTradeRepositoryPort.java` (added `findByDraftId`)
- `src/main/java/com/fortnite/pronos/repository/DraftParticipantTradeJpaRepository.java` (added `findByDraftId`)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/draft/DraftParticipantTradeRepositoryAdapter.java` (implemented `findByDraftId`)
- `src/main/java/com/fortnite/pronos/service/draft/SwapSoloService.java` (added 7th dep + audit save)
- `src/test/java/com/fortnite/pronos/service/draft/SwapSoloServiceTest.java` (added auditRepository mock + verify)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**H1 — FIXED**: No security test for `DraftAuditController`. Created `SecurityConfigDraftAuditAuthorizationTest` (1 test: unauthenticatedCannotGetAudit → 401/403).

### Action items

- [x] **[AI-Review][Low][L1]** : `DraftAuditControllerTest` — class-level `@DisplayName("DraftAuditController")` added on 2026-03-07. Nested class `GetAudit` was already covered.
- [x] **[AI-Review][Low][L2]** : `whenSwapsAndTradesExist_returnsMergedListSortedByDateDesc` — unused local `newest` removed on 2026-03-07.
