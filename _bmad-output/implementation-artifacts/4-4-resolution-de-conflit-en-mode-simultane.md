# Story 4.4: Résolution de conflit en mode simultané

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participant,
I want que les doublons de sélection soient résolus automatiquement par tirage au sort,
so that le round avance équitablement même quand deux participants choisissent le même joueur.

## Acceptance Criteria

1. **Given** au moins deux participants ont soumis le même joueur, **When** le moteur de conflit exécute le tirage (`resolveConflict(windowId)`), **Then** un seul gagnant reçoit le joueur (ConflictResolutionResponse avec `winnerParticipantId` + `loserParticipantId`), **And** le message WebSocket `CONFLICT_RESOLVED` est diffusé avec `contestedPlayerId`, `winnerParticipantId`, `loserParticipantId`, `hasMoreConflicts`.

2. **Given** une précondition invalide (fenêtre inexistante, fenêtre non en état `RESOLVING`, aucun conflit dans la fenêtre), **When** `POST /{draftId}/resolve-conflict/{windowId}` est appelé, **Then** le système rejette la requête avec 409 CONFLICT et un message explicite ; aucun état invalide n'est persisté.

3. **Given** une fenêtre avec plusieurs doublons distincts, **When** le premier conflit est résolu, **Then** `hasMoreConflicts = true` est retourné, signalant au frontend qu'une boucle de résolution supplémentaire est nécessaire.

## Technical Context

### Infrastructure déjà en place (JIRA-UX-003c + Stories 4.2 & 4.3)

Les éléments suivants sont entièrement implémentés et couverts par des tests verts :

#### Domain
- **`DraftAsyncWindow`** — state machine immutable : `OPEN` → `RESOLVING` → `RESOLVED`
  - `startResolving()` : OPEN → RESOLVING (guard `IllegalStateException` si pas OPEN)
  - `resolve()` : → RESOLVED (guard si déjà RESOLVED)
- **`DraftAsyncSelection`** — value object : `participantId`, `playerId`, `submittedAt`
- **`DraftAsyncWindowStatus`** enum : OPEN, RESOLVING, RESOLVED

#### Service
- **`DraftSimultaneousService.resolveConflict(windowId)`** **(AC#1 ✓)** :
  - Valide fenêtre `RESOLVING` (sinon `IllegalStateException`)
  - Appelle `pickFirstConflict()` → coin flip via `random.nextInt(conflicted.size())`
  - Retourne `ConflictResolutionResponse(windowId, contestedPlayerId, winnerParticipantId, loserParticipantId, hasMoreConflicts)`
  - Broadcast WebSocket `CONFLICT_RESOLVED` sur `/topic/draft/{draftId}/simultaneous`
- **`resolveWindow()`** (interne) : déclenché automatiquement quand `count == totalExpected`
  - Si conflit détecté → `startResolving()` + `pickFirstConflict()` + broadcast
  - Si pas de conflit → `resolve()` + broadcast `ALL_RESOLVED`

#### Controller
- **`POST /{draftId}/resolve-conflict/{windowId}`** — appelle `simultaneousService.resolveConflict(windowId)` **(AC#1 ✓)**
  - Retourne `ResponseEntity<ConflictResolutionResponse>` (200 OK)

#### Exception handling
- **`IllegalStateException` → 409 CONFLICT** dans `DomainExceptionHandler` (code `DRAFT_WINDOW_VIOLATION`) **(AC#2 ✓)**
  - Couvre : window not found, not RESOLVING, no conflicts

#### Tests de service existants (DraftSimultaneousServiceTest — 14 tests verts)
- `shouldPickWinnerDeterministically` — coin flip mocké, winner P2 (index 1), loser P1, hasMoreConflicts=false
- `shouldBroadcastConflictResolution` — vérifie `convertAndSend` sur bon topic
- `shouldThrowWhenWindowNotFound` (resolveConflict) — IllegalStateException "Window not found"
- `shouldThrowWhenNotResolving` — IllegalStateException "not in RESOLVING state"
- `shouldBroadcastConflictWhenSamePlayerChosen` — auto-trigger depuis submit()
- `openWindowCannotBeDoubleResolved` — domain guard RESOLVED → RESOLVED bloqué
- `startResolvingRequiresOpen` — domain guard RESOLVING → RESOLVING bloqué

### Gap identifié pour Story 4.4

**Un seul gap** : aucun test unitaire pour l'endpoint `POST /{draftId}/resolve-conflict/{windowId}` dans `DraftSimultaneousControllerTest`.

## Tasks / Subtasks

- [x] Task 1: Modèle domaine + state machine `DraftAsyncWindow` (AC: #1, #2, #3)
  - [x] `startResolving()` OPEN → RESOLVING avec guard
  - [x] `resolve()` → RESOLVED avec guard
  - [x] `DraftAsyncWindowStatus` (OPEN/RESOLVING/RESOLVED)

- [x] Task 2: `DraftSimultaneousService.resolveConflict()` (AC: #1, #2, #3)
  - [x] Coin flip via `Random.nextInt()` injecté
  - [x] Retourne `ConflictResolutionResponse` avec `hasMoreConflicts`
  - [x] Broadcast WebSocket `CONFLICT_RESOLVED`
  - [x] Guards : window not found → ISE, not RESOLVING → ISE, no conflicts → ISE

- [x] Task 3: `POST /{draftId}/resolve-conflict/{windowId}` controller endpoint (AC: #1, #2)
  - [x] Délègue à `simultaneousService.resolveConflict(windowId)`
  - [x] Retourne 200 + `ConflictResolutionResponse`

- [x] Task 4: Exception handler `IllegalStateException` → 409 (AC: #2)
  - [x] `DomainExceptionHandler.handleIllegalState()` → 409 CONFLICT, code `DRAFT_WINDOW_VIOLATION`

- [x] Task 5: Tests unitaires endpoint controller `resolve-conflict` (AC: #1, #2)
  - [x] `src/test/java/com/fortnite/pronos/controller/DraftSimultaneousControllerTest.java`
  - [x] `whenValid_returns200WithResolution` — 200 + body ConflictResolutionResponse + verify service appelé
  - [x] `whenWindowNotResolving_serviceThrowsIllegalState` — IllegalStateException propagée

- [x] Task 6: Tests unitaires service `resolveConflict` (AC: #1, #2, #3)
  - [x] `shouldPickWinnerDeterministically` — Random mocké, winner/loser corrects
  - [x] `shouldBroadcastConflictResolution` — topic WebSocket vérifié
  - [x] `shouldThrowWhenWindowNotFound` + `shouldThrowWhenNotResolving`
  - [x] `shouldBroadcastConflictWhenSamePlayerChosen` (auto-résolution depuis submit)

## Dev Notes

### Architecture
- Hexagonale : service → port → adaptateur JPA. Aucune dépendance JPA dans le service.
- `DraftSimultaneousService` : 3 dépendances (`DraftAsyncRepositoryPort`, `SimpMessagingTemplate`, `Random`) ≤ 7 CouplingTest ✓
- WebSocket topic : `/topic/draft/{draftId}/simultaneous`
- Message `CONFLICT_RESOLVED` fields : `type`, `windowId`, `contestedPlayerId`, `winnerParticipantId`, `loserParticipantId`, `hasMoreConflicts`

### Patron coin flip
```
int winnerIndex = random.nextInt(conflicted.size());
UUID winner = conflicted.get(winnerIndex).getParticipantId();
UUID loser = conflicted.stream()
    .filter(s -> !s.getParticipantId().equals(winner))
    .findFirst().map(...).orElseThrow();
```
`Random` injecté comme seam de test — `@Bean Random` déclaré dans `PronosApplication`.

### `ConflictResolutionResponse` record
```java
record ConflictResolutionResponse(
    UUID windowId, UUID contestedPlayerId,
    UUID winnerParticipantId, UUID loserParticipantId,
    boolean hasMoreConflicts)
```

### `DraftSimultaneousController` constructor (à jour depuis Story 4.2)
```java
public DraftSimultaneousController(
    DraftSimultaneousService simultaneousService,
    DraftTrancheService draftTrancheService)
```

### Task 5 — implémentation attendue du test controller
```java
@Nested @DisplayName("ResolveConflict")
class ResolveConflict {
  @Test void whenValid_returns200WithResolution() {
    ConflictResolutionResponse resp = new ConflictResolutionResponse(
        WINDOW_ID, PLAYER_A, P1, P2, false);
    when(simultaneousService.resolveConflict(WINDOW_ID)).thenReturn(resp);

    mockMvc.perform(post("/api/draft/simultaneous/{draftId}/resolve-conflict/{windowId}",
            DRAFT_ID, WINDOW_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.winnerParticipantId").value(P1.toString()));
    verify(simultaneousService).resolveConflict(WINDOW_ID);
  }

  @Test void whenWindowNotResolving_throws() {
    doThrow(new IllegalStateException("Window is not in RESOLVING state"))
        .when(simultaneousService).resolveConflict(WINDOW_ID);

    mockMvc.perform(post("/api/draft/simultaneous/{draftId}/resolve-conflict/{windowId}",
            DRAFT_ID, WINDOW_ID))
        .andExpect(status().isConflict());
  }
}
```

### Patterns clés
- `MockMvc` dans `DraftSimultaneousControllerTest` : `@WebMvcTest(DraftSimultaneousController.class)`
- `@MockBean DraftSimultaneousService simultaneousService` + `@MockBean DraftTrancheService draftTrancheService` (requis pour éviter l'échec du contexte Spring)
- Constantes UUID dans la classe de test : `DRAFT_ID`, `WINDOW_ID`, `P1`, `P2`, `PLAYER_A`

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

### Completion Notes List
- Tasks 1–4 + 6 : implémentés et testés (JIRA-UX-003c + Stories 4.2/4.3)
- Task 5 : 2 nouveaux tests controller pour `POST /{draftId}/resolve-conflict/{windowId}`
- 2132 tests backend total, 0 nouvelle régression

### File List
- `src/test/java/com/fortnite/pronos/controller/DraftSimultaneousControllerTest.java` (modifié — +2 tests ResolveConflict)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

Aucun fix automatique — aucun problème HIGH. M1 ajouté en action item.

### Action items

- [ ] **[AI-Review][Medium][M1]** : `DraftSimultaneousService.pickFirstConflict()` utilise `.orElse(null)` (ligne 188) — anti-pattern null-propagation. Le caller `resolveConflict()` null-checke à la ligne 130 avec `if (resolution == null)`. Refactorer pour retourner `Optional<ConflictResolutionResponse>` et utiliser `.orElseThrow(...)` dans le caller. Impact : cohérence avec les conventions Java modernes et lisibilité.
- [ ] **[AI-Review][Low][L1]** : Dev Notes (section Task 5 patterns) décrit une approche `@WebMvcTest` + `mockMvc.perform(...).andExpect(status().isConflict())` mais l'implémentation utilise `@ExtendWith(MockitoExtension.class)` avec `assertThrows(...)`. Le test ne vérifie pas le code HTTP 409 — seul `DomainExceptionHandlerTest` le couvre séparément. Documenter cette stratégie de test intentionnelle ou aligner avec `@WebMvcTest`.
- [ ] **[AI-Review][Low][L2]** : Classe `ResolveConflict` nested et ses méthodes de test n'ont pas de `@DisplayName` — inconsistant avec la convention du projet.
