# Story 4.6: Opérations admin de roster — assign, retrait, ajout

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an admin,
I want gérer manuellement le roster de n'importe quelle partie (assigner, retirer, ajouter un joueur),
so that je puisse corriger toute incohérence de roster sans contrainte de règles de tranche.

## Acceptance Criteria

1. **Given** l'admin est authentifié avec le rôle ADMIN, **When** il envoie `POST /api/admin/games/{gameId}/roster` avec `{participantUserId, playerId, reason}`, **Then** le joueur est assigné à l'équipe du participant dans ce draft (DraftPick créé) sans validation de tranche ; le joueur étant déjà pické dans ce draft déclenche une 409 avec code `PLAYER_ALREADY_SELECTED` ; **And** un non-admin reçoit 403.

2. **Given** l'admin est authentifié avec le rôle ADMIN, **When** il envoie `DELETE /api/admin/games/{gameId}/roster/{playerId}`, **Then** le DraftPick correspondant est supprimé ; si le joueur n'est pas dans ce draft, la requête renvoie 404 ; **And** un non-admin reçoit 403.

3. **Given** une précondition invalide (game inexistante, draft inexistant, participant inexistant, joueur inexistant), **When** la requête est effectuée, **Then** le système rejette avec un code HTTP explicite et un message clair ; aucun état invalide n'est persisté.

## Technical Context

### Infrastructure déjà en place

#### Patterns admin existants (READY)
- **`AdminDashboardController`** : `@RequestMapping("/api/admin")`, `@RequiredArgsConstructor`, `@Slf4j` ✓
- **`AdminPlayerPipelineController`** : `@RequestMapping("/api/admin/players")`, pattern identique ✓
- Spring Security filtre par rôle ADMIN au niveau SecurityConfig (pas d'annotation `@PreAuthorize` dans les controllers existants — filtre URL)

#### DraftPick et repositories (READY)
- **`DraftPick`** : `new DraftPick(draft, participant, player, round, pickNumber)` (legacy JPA model) ✓
- **`DraftPickRepositoryPort`** :
  - `save(DraftPick)` ✓
  - `findPickedPlayerIdsByDraftId(UUID draftId)` ✓ (exclusivité)
  - `findByDraft(Draft draft)` ✓
  - **MANQUANT** : `deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId)` — à ajouter
- **`DraftPickRepository`** extends `JpaRepository<DraftPick, UUID>` → hérite `deleteById(UUID)` mais port n'expose pas de delete

#### Participant et joueur lookup (READY)
- **`GameParticipantRepositoryPort.findByUserIdAndGameId(UUID userId, UUID gameId)`** → `Optional<GameParticipant>` ✓
- **`PlayerDomainRepositoryPort.findById(UUID playerId)`** → `Optional<domain.Player>` (conversion legacy nécessaire comme dans `GameDraftService.findPlayerOrThrow()`) ✓

#### Draft lookup (READY)
- Pattern `findActiveDraftEntity(gameId)` dans `GameDraftService` :
  ```java
  draftDomainRepository.findActiveByGameId(gameId) → domainDraft.getId()
  draftRepository.findById(id) → Draft JPA entity
  ```

### Gap identifié pour Story 4.6

**1. DraftPickRepositoryPort** : ne dispose pas de méthode de suppression.
**Fix cible** : ajouter dans `DraftPickRepositoryPort` :
```java
void deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId);
```
Et dans `DraftPickRepository` :
```java
@Modifying
@Query("DELETE FROM DraftPick dp WHERE dp.draft.id = :draftId AND dp.player.id = :playerId")
void deleteByDraftIdAndPlayerId(@Param("draftId") UUID draftId, @Param("playerId") UUID playerId);
```

**2. `AdminDraftRosterService`** : service inexistant.
**Fix cible** : créer `src/main/java/com/fortnite/pronos/service/admin/AdminDraftRosterService.java`
- Méthodes : `assignPlayer(UUID gameId, UUID participantUserId, UUID playerId)` + `removePlayer(UUID gameId, UUID playerId)`
- Bypass tranche : aucun appel à `DraftTrancheService`
- Check exclusivité pour `assignPlayer` : `findPickedPlayerIdsByDraftId(draftId).contains(playerId)`
- 6 dépendances : `GameDomainRepositoryPort`, `DraftDomainRepositoryPort`, `DraftRepositoryPort`, `DraftPickRepositoryPort`, `PlayerDomainRepositoryPort`, `GameParticipantRepositoryPort` (≤ 7 ✓)

**3. `AdminDraftRosterController`** : controller inexistant.
**Fix cible** : créer `src/main/java/com/fortnite/pronos/controller/AdminDraftRosterController.java`
- `POST /api/admin/games/{gameId}/roster` → `assignPlayer`
- `DELETE /api/admin/games/{gameId}/roster/{playerId}` → `removePlayer`

## Tasks / Subtasks

- [x] Task 1: Étendre `DraftPickRepositoryPort` + `DraftPickRepository` (AC: #2, #3)
  - [x] Ajouter `void deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId)` dans `DraftPickRepositoryPort`
  - [x] Ajouter implémentation `@Modifying @Query` dans `DraftPickRepository`

- [x] Task 2: Créer `AdminDraftRosterService` (AC: #1, #2, #3)
  - [x] `src/main/java/com/fortnite/pronos/service/admin/AdminDraftRosterService.java`
  - [x] `assignPlayer(UUID gameId, UUID participantUserId, UUID playerId)` → `DraftPickDto`
  - [x] `removePlayer(UUID gameId, UUID playerId)` → void
  - [x] Check exclusivité dans `assignPlayer` → `PlayerAlreadySelectedException` si déjà pické
  - [x] Check player exists in draft before remove → `GameNotFoundException` avec message explicite si absent

- [x] Task 3: Créer `AdminDraftRosterController` (AC: #1, #2)
  - [x] `src/main/java/com/fortnite/pronos/controller/AdminDraftRosterController.java`
  - [x] `POST /api/admin/games/{gameId}/roster` → 200 + DraftPickDto
  - [x] `DELETE /api/admin/games/{gameId}/roster/{playerId}` → 204

- [x] Task 4: Tests unitaires `AdminDraftRosterServiceTest` (AC: #1, #2, #3)
  - [x] `src/test/java/com/fortnite/pronos/service/admin/AdminDraftRosterServiceTest.java`
  - [x] `assignPlayer_whenValid_createsDraftPick`
  - [x] `assignPlayer_whenPlayerAlreadyPicked_throwsPlayerAlreadySelectedException`
  - [x] `assignPlayer_whenGameNotFound_throwsGameNotFoundException`
  - [x] `assignPlayer_whenParticipantNotFound_throwsIllegalArgumentException`
  - [x] `removePlayer_whenValid_deletesFromDraft`
  - [x] `removePlayer_whenPlayerNotInDraft_throwsGameNotFoundException`

- [x] Task 5: Tests unitaires `AdminDraftRosterControllerTest` (AC: #1, #2)
  - [x] `src/test/java/com/fortnite/pronos/controller/AdminDraftRosterControllerTest.java`
  - [x] `assignPlayer_whenValid_returns200`
  - [x] `removePlayer_whenValid_returns204`

## Dev Notes

### Implémentation exacte

#### DraftPickRepositoryPort (ajout méthode delete)
```java
/** Supprime le pick d'un joueur dans un draft. No-op si absent. */
void deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId);
```

#### DraftPickRepository (implémentation @Modifying)
```java
@Modifying
@Query("DELETE FROM DraftPick dp WHERE dp.draft.id = :draftId AND dp.player.id = :playerId")
void deleteByDraftIdAndPlayerId(@Param("draftId") UUID draftId, @Param("playerId") UUID playerId);
```

#### AdminDraftRosterService.java
```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminDraftRosterService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;

  public DraftPickDto assignPlayer(UUID gameId, UUID participantUserId, UUID playerId) {
    gameDomainRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    com.fortnite.pronos.model.GameParticipant participant =
        gameParticipantRepository.findByUserIdAndGameId(participantUserId, gameId)
            .orElseThrow(() -> new IllegalArgumentException("Participant not found"));
    com.fortnite.pronos.model.Player player = findLegacyPlayer(playerId);

    requirePlayerNotAlreadyPicked(draft.getId(), playerId);

    com.fortnite.pronos.model.DraftPick pick =
        new com.fortnite.pronos.model.DraftPick(draft, participant, player, 0, 0);
    com.fortnite.pronos.model.DraftPick saved = draftPickRepository.save(pick);

    log.info("Admin assigned player {} to participant {} in game {}", playerId, participantUserId, gameId);
    return DraftPickDto.fromDraftPick(saved);
  }

  public void removePlayer(UUID gameId, UUID playerId) {
    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    List<UUID> pickedIds = draftPickRepository.findPickedPlayerIdsByDraftId(draft.getId());
    if (!pickedIds.contains(playerId)) {
      throw new GameNotFoundException("Player not found in draft: " + playerId);
    }
    draftPickRepository.deleteByDraftIdAndPlayerId(draft.getId(), playerId);
    log.info("Admin removed player {} from game {}", playerId, gameId);
  }

  private com.fortnite.pronos.model.Draft findDraftEntity(UUID gameId) {
    com.fortnite.pronos.domain.draft.model.Draft domainDraft =
        draftDomainRepository.findActiveByGameId(gameId)
            .orElseThrow(() -> new InvalidDraftStateException("No active draft for game: " + gameId));
    return draftRepository.findById(domainDraft.getId())
        .orElseThrow(() -> new InvalidDraftStateException("Draft entity not found"));
  }

  private com.fortnite.pronos.model.Player findLegacyPlayer(UUID playerId) {
    return playerRepository.findById(playerId)
        .map(p -> {
          com.fortnite.pronos.model.Player legacy = new com.fortnite.pronos.model.Player();
          legacy.setId(p.getId());
          legacy.setNickname(p.getName());
          return legacy;
        })
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  private void requirePlayerNotAlreadyPicked(UUID draftId, UUID playerId) {
    List<UUID> pickedIds = draftPickRepository.findPickedPlayerIdsByDraftId(draftId);
    if (pickedIds.contains(playerId)) {
      throw new PlayerAlreadySelectedException("Player is already selected in this draft");
    }
  }
}
```

#### AdminDraftRosterController.java
```java
@Slf4j
@RestController
@Validated
@RequestMapping("/api/admin/games/{gameId}/roster")
@RequiredArgsConstructor
public class AdminDraftRosterController {

  private final AdminDraftRosterService rosterService;

  @PostMapping
  public ResponseEntity<DraftPickDto> assignPlayer(
      @PathVariable UUID gameId, @RequestBody @Valid AdminRosterAssignRequest request) {
    DraftPickDto result = rosterService.assignPlayer(gameId, request.participantUserId(), request.playerId());
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{playerId}")
  public ResponseEntity<Void> removePlayer(@PathVariable UUID gameId, @PathVariable UUID playerId) {
    rosterService.removePlayer(gameId, playerId);
    return ResponseEntity.noContent().build();
  }
}
```

#### DTOs record
```java
// AdminRosterAssignRequest.java
public record AdminRosterAssignRequest(UUID participantUserId, UUID playerId) {}
```

### Pattern toLegacyPlayer (depuis GameDraftService)
```java
// GameDraftService.java (référence)
private com.fortnite.pronos.model.Player toLegacyPlayer(com.fortnite.pronos.domain.player.model.Player p) {
  // setId, setNickname — adapter les champs disponibles
}
```
Reproduire ce pattern dans `AdminDraftRosterService.findLegacyPlayer()`.

### Tests — pattern MockitoExtension
```java
@ExtendWith(MockitoExtension.class)
class AdminDraftRosterServiceTest {
  @Mock GameDomainRepositoryPort gameDomainRepository;
  @Mock DraftDomainRepositoryPort draftDomainRepository;
  @Mock DraftRepositoryPort draftRepository;
  @Mock DraftPickRepositoryPort draftPickRepository;
  @Mock PlayerDomainRepositoryPort playerRepository;
  @Mock GameParticipantRepositoryPort gameParticipantRepository;

  private AdminDraftRosterService service;
  private final UUID GAME_ID = UUID.randomUUID();
  private final UUID DRAFT_ID = UUID.randomUUID();
  private final UUID PLAYER_ID = UUID.randomUUID();
  private final UUID PARTICIPANT_USER_ID = UUID.randomUUID();

  @BeforeEach void setUp() {
    service = new AdminDraftRosterService(
        gameDomainRepository, draftDomainRepository, draftRepository,
        draftPickRepository, playerRepository, gameParticipantRepository);
  }
}
```

### Contraintes architecture
- `CouplingTest` : 6 dépendances dans `AdminDraftRosterService` (max 7) ✓
- `NamingConventionTest` : classe suffixée `Service` ✓ + `Controller` ✓
- `DomainIsolationTest` : `AdminDraftRosterService` dans `service.admin` (pas `domain`) ✓
- Spotless : `mvn spotless:apply` avant `mvn test`
- Linter Windows : relire les fichiers avant édition si délai

### Import manquants à anticiper
```java
// AdminDraftRosterService imports
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import java.util.List;
import java.util.UUID;
```

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

### Completion Notes List
- Task 1: `deleteByDraftIdAndPlayerId()` ajouté dans `DraftPickRepositoryPort` + `@Modifying @Query` dans `DraftPickRepository`
- Task 2: `AdminDraftRosterService` créé (6 deps ≤ 7) — `assignPlayer()` bypass tranche + exclusivité, `removePlayer()` avec check
- Task 3: `AdminDraftRosterController` créé (`POST` → 200, `DELETE` → 204) + `AdminRosterAssignRequest` record
- Task 4: 7 tests service (5 AssignPlayer + 2 RemovePlayer) — 7/7 verts
- Task 5: 2 tests controller — 2/2 verts
- 2145 tests backend total, 0 nouvelle régression (pré-existants inchangés)

### File List
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftPickRepositoryPort.java` (modifié — +deleteByDraftIdAndPlayerId)
- `src/main/java/com/fortnite/pronos/repository/DraftPickRepository.java` (modifié — +@Modifying @Query)
- `src/main/java/com/fortnite/pronos/service/admin/AdminDraftRosterService.java` (créé)
- `src/main/java/com/fortnite/pronos/controller/AdminDraftRosterController.java` (créé)
- `src/main/java/com/fortnite/pronos/dto/admin/AdminRosterAssignRequest.java` (créé)
- `src/test/java/com/fortnite/pronos/service/admin/AdminDraftRosterServiceTest.java` (créé — 7 tests)
- `src/test/java/com/fortnite/pronos/controller/AdminDraftRosterControllerTest.java` (créé — 2 tests)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**M1 — FIXED**: `InvalidDraftStateException` non mappée → 500. Ajouté `@ExceptionHandler(InvalidDraftStateException.class)` → 409 CONFLICT / `INVALID_DRAFT_STATE` dans `DomainExceptionHandler`. +1 test `handleInvalidDraftStateExceptionReturnsConflict` dans `DomainExceptionHandlerTest` (total 14 tests).

**M2 — FIXED**: `IllegalArgumentException` pour "Participant not found" et "Player not found" → 400 + message générique "Invalid request parameter". Remplacé par `GameNotFoundException` → 404 avec message explicite. Test `assignPlayer_whenParticipantNotFound_throwsIllegalArgumentException` → `throwsGameNotFoundException` mis à jour.

**M3 — FIXED**: Aucun test de sécurité pour `AdminDraftRosterController`. Créé `SecurityConfigAdminDraftRosterAuthorizationTest` (2 tests : unauthenticatedCannotAssignPlayer + unauthenticatedCannotRemovePlayer).

### Action items

- [ ] **[AI-Review][Low][L1]** : `removePlayer()` lance `GameNotFoundException("Player not found in draft for game: ...")` — type sémantiquement trompeur (le joueur n'est pas dans le draft, pas le jeu). HTTP 404 est correct (AC#2 ✓). Envisager un type dédié `PlayerNotInDraftException` dans une future itération.
- [ ] **[AI-Review][Low][L2]** : `requirePlayerNotAlreadyPicked()` (ligne 132) utilise `List.contains()` — O(n). Harmoniser avec `HashSet` comme suggéré en 4.5 L1 : `new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draftId)).contains(playerId)`.
- [ ] **[AI-Review][Low][L3]** : `AdminDraftRosterControllerTest` — classes imbriquées `AssignPlayer` et `RemovePlayer` n'ont pas de `@DisplayName`. Inconsistant avec la convention du projet.
