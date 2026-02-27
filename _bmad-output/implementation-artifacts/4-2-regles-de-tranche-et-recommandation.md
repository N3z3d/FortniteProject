# Story 4.2: Règles de tranche et recommandation

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participant,
I want des picks valides selon le plancher de tranche et un bouton recommander,
so that je prenne des décisions conformes rapidement.

## Acceptance Criteria

1. **Given** un draft serpent est actif avec `tranchesEnabled=true`, **When** un participant appelle `POST /api/games/{gameId}/draft/snake/pick` avec un joueur dont le rang est meilleur que le plancher autorisé (`parseTrancheFloor(player.tranche) < requiredFloor`), **Then** le backend répond 400 BAD_REQUEST avec un message explicite ("Tranche violation: player rank better than allowed floor").

2. **Given** un draft serpent est actif avec `tranchesEnabled=true`, **When** un participant soumet un joueur dont `parseTrancheFloor(player.tranche) >= requiredFloor`, **Then** le pick est accepté (200 OK), le curseur avance, et le pick est persisté normalement.

3. **Given** une partie avec `tranchesEnabled=false`, **When** un participant soumet n'importe quel pick, **Then** aucune validation de tranche n'est effectuée et le pick est accepté sans restriction.

4. **Given** un draft serpent actif avec `tranchesEnabled=true`, **When** un utilisateur appelle `GET /api/games/{gameId}/draft/snake/recommend?region=GLOBAL`, **Then** le système retourne 200 + `PlayerRecommendResponse` du meilleur joueur disponible conforme (non encore pické, `parseTrancheFloor(tranche) >= requiredFloor`, trié par plancher croissant — le plus proche du plancher requis).

5. **Given** aucun joueur conforme n'est disponible (tous pickés ou tous trop bons), **When** `GET /api/games/{gameId}/draft/snake/recommend` est appelé, **Then** le système retourne 404 NOT FOUND.

6. **Given** un draft simultané est actif avec `tranchesEnabled=true`, **When** un participant appelle `POST /api/draft/simultaneous/submit`, **Then** la même validation de tranche s'applique (`DraftTrancheService.validatePick()`) et un pick hors plancher retourne 400.

## Tasks / Subtasks

- [ ] Task 1: Créer `InvalidTrancheViolationException.java` (AC: #1, #6)
  - [ ] `src/main/java/com/fortnite/pronos/exception/InvalidTrancheViolationException.java`
  - [ ] Étend `RuntimeException` (même pattern que `NotYourTurnException`) — **pas** `BusinessException`
  - [ ] Constructeur `(String message)` uniquement

- [ ] Task 2: Créer `PlayerRecommendResponse.java` DTO (AC: #4)
  - [ ] `src/main/java/com/fortnite/pronos/dto/PlayerRecommendResponse.java`
  - [ ] Record: `id (UUID)`, `nickname (String)`, `region (String)`, `tranche (String)`, `trancheFloor (int)`
  - [ ] Méthode factory statique `from(Player player)` : `trancheFloor = parseFloor(player.getTranche())`
  - [ ] `parseFloor(String tranche)` privée : `tranche.split("-")[0]` → `Integer.parseInt(...)`, cas null/blank → 1

- [ ] Task 3: Créer `DraftTrancheService.java` (AC: #1, #2, #3, #4, #5, #6)
  - [ ] `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java`
  - [ ] **5 dépendances** (≤ 7 requis par CouplingTest) : `GameDomainRepositoryPort`, `DraftDomainRepositoryPort`, `PlayerDomainRepositoryPort`, `DraftPickRepositoryPort`, `DraftPickOrchestratorService`
  - [ ] `validatePick(UUID gameId, String region, UUID playerId)` → void :
    - Get game → si `!game.isTranchesEnabled()` → return immédiatement (no-op)
    - Get active draft → `findActiveDraftOrThrow(gameId)`
    - Get current turn → `orchestratorService.getCurrentTurn(draft.getId(), region)` → `SnakeTurn`
    - Compute slot : `slot = (turn.round() - 1) * game.getMaxParticipants() + turn.pickNumber()`
    - Compute requiredFloor : `(slot - 1) * game.getTrancheSize() + 1`
    - Get player → `playerRepository.findById(playerId).orElseThrow(GameNotFoundException)`
    - Parse player floor : `parseTrancheFloor(player.getTranche())`
    - Si `playerFloor < requiredFloor` → throw `InvalidTrancheViolationException("Tranche violation: player rank " + playerFloor + " better than allowed floor " + requiredFloor)`
  - [ ] `recommendPlayer(UUID gameId, String region)` → `Optional<PlayerRecommendResponse>` :
    - Get game → si `!game.isTranchesEnabled()` → return `Optional.empty()`
    - Get active draft → orElse empty
    - Get current turn → orElse empty
    - Compute slot et requiredFloor (même logique que validatePick)
    - Get all active players → `playerRepository.findActivePlayers()`
    - Filter already-picked : `!draftPickRepository.existsByDraftAndPlayer(draft, player)`  ← NOTE: vérifier si `DraftPickRepositoryPort` prend bien des domain objects (Draft domain + Player domain)
    - Filter tranche : `parseTrancheFloor(player.getTranche()) >= requiredFloor`
    - Sort by trancheFloor ascending (meilleur conforme en premier)
    - Return first → `PlayerRecommendResponse.from(player)` ou empty
  - [ ] Méthode privée `parseTrancheFloor(String tranche)` :
    ```
    if (tranche == null || tranche.isBlank()) return 1;
    return Integer.parseInt(tranche.split("-")[0]);
    ```
  - [ ] Constantes : `GLOBAL_REGION = "GLOBAL"` (ou réutiliser celle de SnakeDraftService si visible)

- [ ] Task 4: Mettre à jour `DomainExceptionHandler.java` (AC: #1, #6)
  - [ ] Ajouter import `InvalidTrancheViolationException`
  - [ ] Nouveau handler : `@ExceptionHandler(InvalidTrancheViolationException.class)` → `ResponseEntity<ApiResponse<Void>>` avec status 400 BAD_REQUEST
  - [ ] Pattern identique aux handlers `NotYourTurnException` / `BusinessException` existants

- [ ] Task 5: Mettre à jour `SnakeDraftController.java` (AC: #1, #2, #4, #5)
  - [ ] Ajouter `DraftTrancheService` comme 4ème dépendance du constructeur
  - [ ] `processPick()` : appeler `draftTrancheService.validatePick(gameId, request.getRegion(), request.getPlayerId())` **avant** `gameDraftService.selectPlayer()` (après `snakeDraftService.validateAndAdvance()`)
  - [ ] Nouveau endpoint : `GET /api/games/{gameId}/draft/snake/recommend?region=GLOBAL`
    - Pas d'auth requise (lecture seule, cohérent avec `getCurrentTurn`)
    - Appelle `draftTrancheService.recommendPlayer(gameId, region)`
    - Si présent → 200 + `ApiResponse<PlayerRecommendResponse>`
    - Si empty → 404

- [ ] Task 6: Mettre à jour `DraftSimultaneousController.java` (AC: #6)
  - [ ] Ajouter `DraftTrancheService` comme dépendance
  - [ ] Dans `submitSelection()` (POST /api/draft/simultaneous/submit) : appeler `draftTrancheService.validatePick(gameId, region, request.getPlayerId())` avant la soumission effective
  - [ ] Vérifier la région utilisée par le draft simultané (probablement `"GLOBAL"` ou celle dans `SimultaneousSubmitRequest`)

- [ ] Task 7: Tests TDD `DraftTrancheServiceTest.java` (AC: #1, #2, #3, #4, #5)
  - [ ] `src/test/java/com/fortnite/pronos/service/draft/DraftTrancheServiceTest.java`
  - [ ] `@ExtendWith(MockitoExtension.class)`, 3 groupes `@Nested`
  - [ ] **ValidatePick** (5 tests) :
    - `tranchesDisabled_skipsValidation` — `tranchesEnabled=false` → no exception
    - `noActiveDraft_throwsInvalidDraftState` — `findActiveByGameId` retourne empty → InvalidDraftStateException
    - `playerTrancheRespected_noException` — floor=11, player tranche "11-20" → floor=11 ≥ 11 → OK
    - `playerTooGood_throwsInvalidTrancheViolation` — floor=11, player tranche "1-5" → floor=1 < 11 → exception
    - `trancheInfini_alwaysValid` — "31-infini" → floor=31, si requis=11 → 31 ≥ 11 → OK
  - [ ] **RecommendPlayer** (4 tests) :
    - `tranchesDisabled_returnsEmpty` — disabled → empty
    - `noEligiblePlayers_returnsEmpty` — tous pickés ou tous trop bons → empty
    - `returnsBestConformingPlayer` — 3 joueurs disponibles dont 2 conformes → retourne celui avec le plus petit floor ≥ requis
    - `noCursor_returnsEmpty` — `getCurrentTurn` retourne empty → empty
  - [ ] **ParseTrancheFloor** (3 tests, méthode privée testée via comportement) :
    - `"1-5"` → floor=1 (via validatePick : slot=1, trancheSize=5, reqFloor=1, joueur "1-5" accepté)
    - `"31-infini"` → floor=31 (parseable)
    - `null/blank` → floor=1 (traité comme premier slot)

- [ ] Task 8: Mettre à jour `SnakeDraftControllerTest.java` (AC: #1, #4)
  - [ ] Ajouter `@Mock DraftTrancheService draftTrancheService` + mettre à jour constructeur du contrôleur dans `setUp()`
  - [ ] **Nouveau test** `processPick_whenTrancheViolation_returns400` : `doThrow(InvalidTrancheViolationException.class).when(draftTrancheService).validatePick(...)` → vérifier comportement (le contrôleur ne gère pas l'exception → propagée vers handler) — alternativement vérifier que `validatePick` est appelé avant `selectPlayer`
  - [ ] **Nouveau test** `recommend_whenPlayerAvailable_returns200` : `when(draftTrancheService.recommendPlayer(gameId, "GLOBAL")).thenReturn(Optional.of(buildRecommendResponse()))` → status 200
  - [ ] **Nouveau test** `recommend_whenNoPlayer_returns404` : `when(...).thenReturn(Optional.empty())` → status 404

- [ ] Task 9: Mettre à jour `DraftSimultaneousControllerTest.java` (AC: #6)
  - [ ] Ajouter mock `DraftTrancheService`
  - [ ] **Nouveau test** `submit_whenTrancheViolation_validatePickCalled` : vérifier que `draftTrancheService.validatePick()` est appelé dans `submitSelection()`

## Dev Notes

### Contraintes architecturales

- **CouplingTest** (`servicesShouldNotHaveMoreThanSevenDependencies`) : `DraftTrancheService` a 5 dépendances — OK. `GameDraftService` est à 7 — **ne pas le modifier**. `SnakeDraftService` est à 6 — ne pas y ajouter DraftTrancheService (passer par le controller).
- **NamingConventionTest** : `@Service` dans `..service..` doit finir par `Service` → `DraftTrancheService` (pas `DraftTrancheValidator` comme dans architecture.md). `PlayerRecommendResponse` dans `..dto..` respecte le suffixe `Response`.
- **DomainIsolationTest** : `InvalidTrancheViolationException` est dans `..exception..` (pas dans `..domain..`) — aucune annotation Spring/JPA/Lombok.
- **DependencyInversionTest** : `SnakeDraftController` ne doit pas dépendre de repositories directement — `DraftTrancheService` (un service) est une dépendance valide.
- **LayeredArchitectureTest** : Le controller peut dépendre du domaine (types domain). `DraftTrancheService` est dans `service/draft/` — couche service valide.
- **Spotless** : Toujours exécuter `mvn spotless:apply` avant `mvn test`.

### Logique de validation de tranche

```
Game.tranchesEnabled = true/false
Game.trancheSize = 10 (configurable)
Player.tranche = "1-5" | "6-10" | "11-20" | ... | "31-infini"

parseTrancheFloor("1-5")      → 1
parseTrancheFloor("6-10")     → 6
parseTrancheFloor("31-infini") → 31
parseTrancheFloor(null/blank)  → 1

slot = (turn.round() - 1) * game.getMaxParticipants() + turn.pickNumber()
// Exemple : round=1, pick=3, maxParticipants=10 → slot=3
// Exemple : round=2, pick=1, maxParticipants=10 → slot=11

requiredFloor = (slot - 1) * game.getTrancheSize() + 1
// slot=1  → floor = (1-1)*10+1 = 1   (tous les joueurs valides)
// slot=2  → floor = (2-1)*10+1 = 11  (tranche ≥ 11)
// slot=3  → floor = (3-1)*10+1 = 21  (tranche ≥ 21)

VALID   : parseTrancheFloor(player.tranche) >= requiredFloor
INVALID : parseTrancheFloor(player.tranche) < requiredFloor → InvalidTrancheViolationException
```

### Logique de recommandation

```
1. game.isTranchesEnabled() == false → return Optional.empty()
2. draft = findActiveByGameId(gameId) → orElse empty
3. turn = orchestratorService.getCurrentTurn(draft.getId(), region) → orElse empty
4. slot + requiredFloor (même formule)
5. players = playerRepository.findActivePlayers()
6. Filtrer : !draftPickRepository.existsByDraftAndPlayer(draft, player)
7. Filtrer : parseTrancheFloor(player.getTranche()) >= requiredFloor
8. Trier par parseTrancheFloor(tranche) ASC (le plus proche du plancher en premier)
9. Return first as PlayerRecommendResponse.from(player)
```

### Attention : `DraftPickRepositoryPort.existsByDraftAndPlayer`

Le port prend `Draft draft` (domain `com.fortnite.pronos.domain.draft.model.Draft`) et `Player player` (domain `com.fortnite.pronos.domain.player.model.Player`). Utiliser :
- `DraftDomainRepositoryPort.findActiveByGameId()` → domain `Draft`
- `PlayerDomainRepositoryPort.findById()` / `findActivePlayers()` → domain `Player`

**Ne pas** utiliser les entités JPA legacy (`com.fortnite.pronos.model.Draft`, `com.fortnite.pronos.model.Player`).

### DraftSimultaneousController — point d'injection

Le `DraftSimultaneousController` se trouve à `src/main/java/com/fortnite/pronos/controller/DraftSimultaneousController.java`. Identifier la méthode de soumission (probablement `submitSelection()` ou `submit()` avec `POST /api/draft/simultaneous/submit` ou `POST /api/draft/simultaneous/{draftId}/submit`). Injecter `DraftTrancheService` et appeler `validatePick(gameId, region, playerId)` avant de déléguer à `DraftSimultaneousService`.

Vérifier le `region` passé dans `SimultaneousSubmitRequest` — si absent, utiliser `"GLOBAL"`.

### Exception handler existant (pattern à suivre)

```java
// Dans DomainExceptionHandler.java :
@ExceptionHandler(NotYourTurnException.class)
public ResponseEntity<ApiResponse<Void>> handleNotYourTurn(NotYourTurnException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(ex.getMessage()));
}

// À ajouter pour InvalidTrancheViolationException :
@ExceptionHandler(InvalidTrancheViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleInvalidTranche(InvalidTrancheViolationException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
}
```

### Projet Structure Notes

```
src/main/java/com/fortnite/pronos/
  exception/
    InvalidTrancheViolationException.java   — NEW
  dto/
    PlayerRecommendResponse.java            — NEW (record)
  service/draft/
    DraftTrancheService.java                — NEW
  controller/
    SnakeDraftController.java               — MODIFIED (+DraftTrancheService dep, +recommend endpoint)
    DraftSimultaneousController.java        — MODIFIED (+DraftTrancheService dep, validate in submit)
  exception/
    DomainExceptionHandler.java             — MODIFIED (+InvalidTrancheViolationException handler)

src/test/java/com/fortnite/pronos/
  service/draft/
    DraftTrancheServiceTest.java            — NEW
  controller/
    SnakeDraftControllerTest.java           — MODIFIED (+mock + 3 tests)
    DraftSimultaneousControllerTest.java    — MODIFIED (+mock + 1 test)
```

### References

- [Source: epics.md#Story 4.2] FR-22, FR-23, FR-24 — règles de tranche, plancher, recommandation
- [Source: architecture.md#Draft Engine] `DraftTrancheValidator` renamed to `DraftTrancheService` (NamingConventionTest)
- [Source: domain/game/model/Game.java] `getTrancheSize()`, `isTranchesEnabled()`, `getMaxParticipants()`
- [Source: domain/player/model/Player.java] `getTranche()` → `String` format "1-5", "6-10", "31-infini"
- [Source: domain/port/out/PlayerDomainRepositoryPort.java] `findActivePlayers()`, `findById(UUID id)`
- [Source: domain/port/out/DraftPickRepositoryPort.java] `existsByDraftAndPlayer(Draft, Player)` — domain types
- [Source: domain/port/out/DraftDomainRepositoryPort.java] `findActiveByGameId(UUID gameId)` → `Optional<Draft>`
- [Source: service/draft/DraftPickOrchestratorService.java] `getCurrentTurn(UUID draftId, String region)` → `Optional<SnakeTurn>`
- [Source: domain/draft/model/SnakeTurn.java] `round()`, `pickNumber()`, `participantId()`
- [Source: exception/NotYourTurnException.java] Patron pour `InvalidTrancheViolationException`
- [Source: exception/DomainExceptionHandler.java] Patron pour nouveau handler → 400 BAD_REQUEST
- [Source: controller/SnakeDraftController.java] 3 dépendances actuelles → ajouter DraftTrancheService en 4ème
- [Source: controller/DraftSimultaneousController.java] Trouver méthode submit + injecter DraftTrancheService
- [Source: MEMORY.md#CouplingTest] Max 7 deps par @Service — GameDraftService déjà à 7 (ne pas toucher)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
