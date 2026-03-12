# Story 4.3: Soumission simultanée sans conflit

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participant,
I want soumettre mon choix en mode simultané,
so that le round avance automatiquement quand il n'y a pas de doublon.

## Acceptance Criteria

1. **Given** un round simultané est ouvert, **When** tous les participants soumettent un joueur distinct, **Then** chaque joueur est attribué à son auteur et le slot suivant est ouvert pour tous (message WebSocket `ALL_RESOLVED` avec la liste des sélections, fenêtre passée à `RESOLVED`).

2. **Given** une précondition invalide (fenêtre fermée, double soumission, fenêtre inexistante), **When** la même action est soumise, **Then** le système rejette la requête avec un code HTTP explicite (409 CONFLICT) et un message d'erreur clair ; aucun état invalide n'est persisté.

## Technical Context

### Infrastructure déjà en place (JIRA-UX-003c + Story 4.2)

Les éléments suivants sont déjà implémentés et couverts par des tests :

- **`DraftAsyncWindow`** / **`DraftAsyncSelection`** — modèles domaine `final`, factory `restore()`, transitions d'état `startResolving()` / `resolve()` avec guards `IllegalStateException`
- **`DraftAsyncRepositoryPort`** — 7 méthodes, adaptateur JPA complet (entity + JpaRepo + mapper + repositoryAdapter), migration V33
- **`DraftSimultaneousService`** — 3 dépendances (`DraftAsyncRepositoryPort`, `SimpMessagingTemplate`, `Random`) :
  - `openWindow()` — persiste une fenêtre OPEN
  - `submit()` — enregistre la sélection, diffuse le compteur, déclenche `resolveWindow()` quand `count == totalExpected`
  - `resolveWindow()` — détecte l'absence de conflit → `broadcastAllResolved()` + fenêtre `RESOLVED` **(AC#1 ✓)**
  - `requireOpenWindow()` / `requireNotAlreadySubmitted()` — guards qui lèvent `IllegalStateException`
  - `getStatus()` — retourne le compteur courant
  - `resolveConflict()` — coin flip (Story 4.4)
- **`DraftSimultaneousController`** — POST `/{draftId}/open-window`, POST `/{draftId}/submit` (avec `DraftTrancheService.validatePickByDraftId()`), GET `/{draftId}/status`, POST `/{draftId}/resolve-conflict/{windowId}`
- **`DraftSimultaneousServiceTest`** — 14 tests existants dont `shouldBroadcastAllResolvedWhenNoConflict`
- **`DraftSimultaneousControllerTest`** — 4 tests existants (submit OK, tranche violation, getStatus 200, getStatus 404)
- **`DraftTrancheService.validatePickByDraftId()`** — validation plancher de tranche intégrée dans `submit` **(AC#2 partiel ✓)**

### Gap identifié pour AC#2

`requireOpenWindow()` et `requireNotAlreadySubmitted()` lèvent des `IllegalStateException` qui ne sont pas gérées par `DomainExceptionHandler` → HTTP 500 au lieu de 409 CONFLICT.

## Tasks / Subtasks

- [x] Task 1: Modèles domaine `DraftAsyncWindow` / `DraftAsyncSelection` (AC: #1, #2)
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftAsyncWindow.java`
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftAsyncSelection.java`
  - [x] `src/main/java/com/fortnite/pronos/domain/draft/model/DraftAsyncWindowStatus.java`

- [x] Task 2: Port + adaptateur JPA (AC: #1, #2)
  - [x] `src/main/java/com/fortnite/pronos/domain/port/out/DraftAsyncRepositoryPort.java`
  - [x] Adaptateur JPA complet (Entity + JpaRepo + Mapper + RepositoryAdapter)
  - [x] Migration V33

- [x] Task 3: `DraftSimultaneousService` — logique métier no-conflict (AC: #1)
  - [x] `submit()` déclenche `resolveWindow()` à `count == totalExpected`
  - [x] `resolveWindow()` — no conflict → `broadcastAllResolved()` + fenêtre `RESOLVED`
  - [x] `requireOpenWindow()` / `requireNotAlreadySubmitted()` — guards lèvent `IllegalStateException`

- [x] Task 4: `DraftSimultaneousController` — endpoints REST (AC: #1, #2)
  - [x] POST `/{draftId}/submit` avec `DraftTrancheService.validatePickByDraftId()` avant submit
  - [x] GET `/{draftId}/status`

- [x] Task 5: Mapper `IllegalStateException` → 409 CONFLICT dans `DomainExceptionHandler` (AC: #2)
  - [x] `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java`
  - [x] `@ExceptionHandler(IllegalStateException.class)` → 409 CONFLICT avec message
  - [x] 2 nouveaux tests dans `DomainExceptionHandlerTest` (window not OPEN + already submitted)

- [x] Task 6: Tests unitaires `DraftSimultaneousServiceTest` (AC: #1, #2)
  - [x] `shouldBroadcastAllResolvedWhenNoConflict` — vérifie `RESOLVED` + 2 appels WS
  - [x] `shouldThrowWhenWindowNotFound`
  - [x] `shouldThrowWhenAlreadySubmitted`
  - [x] `shouldThrowWhenWindowNotOpen` (couvert par windowStateTransitions)

- [x] Task 7: Tests unitaires `DraftSimultaneousControllerTest` (AC: #2)
  - [x] `whenPickValid_submitsAndReturns200`
  - [x] `whenTrancheViolation_validatePickCalledBeforeSubmit`
  - [x] `whenStatusFound_returns200`
  - [x] `whenNotFound_returns404`

## Dev Notes

### Architecture
- Hexagonale : service → port → adaptateur JPA. Aucune dépendance JPA dans le service.
- `DraftSimultaneousService` : 3 dépendances (@Service, CouplingTest ≤ 7 ✓).
- WebSocket topic : `/topic/draft/{draftId}/simultaneous`.
- Message types : `SUBMISSION_COUNT` | `ALL_RESOLVED` | `CONFLICT_RESOLVED`.

### Patterns clés
- `DraftAsyncWindow.resolve()` retourne une nouvelle instance immutable (pattern value-object).
- `Random` injecté pour determinism en tests (pattern test seam).
- `@Transactional` sur `submit()` — atomicité persist + broadcast.

### Task 5 — implémentation attendue
```java
@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
  return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ApiResponse.error(ex.getMessage()));
}
```

## Story Progress Notes

### Agent Model Used
claude-sonnet-4-6

### File List

- `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java` — MODIFIED (+IllegalStateException handler → 409)
- `src/test/java/com/fortnite/pronos/config/DomainExceptionHandlerTest.java` — MODIFIED (+2 tests: handleIllegalStateExceptionReturnsConflict, handleIllegalStateExceptionAlreadySubmittedReturnsConflict)
- `src/test/java/com/fortnite/pronos/config/SecurityConfigSimultaneousDraftAuthorizationTest.java` — NEW (M2 fix, 4 tests)

### Completion Notes
- Tasks 1–4 + 6–7 : already implemented and tested (JIRA-UX-003c + Story 4.2)
- Task 5 : `@ExceptionHandler(IllegalStateException.class)` → 409 CONFLICT ajouté dans `DomainExceptionHandler`
- 2 nouveaux tests ajoutés dans `DomainExceptionHandlerTest` (total 13 tests, 0 failure)
- 2130 tests backend total (19 failures + 24 errors = tous pré-existants, 0 régression)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**M2 — FIXED**: Créé `SecurityConfigSimultaneousDraftAuthorizationTest` (4 tests): unauthenticated bloqué sur open-window, submit, status, resolve-conflict.

### Action items

- [ ] **[AI-Review][Medium][M1]** : `@ExceptionHandler(IllegalStateException.class)` dans `DomainExceptionHandler` est trop large — intercepte toutes les `IllegalStateException` de l'application (Spring internals, Hibernate, etc.) et les retourne comme `409 DRAFT_WINDOW_VIOLATION`. Créer `DraftWindowViolationException extends RuntimeException` et remplacer les `throw new IllegalStateException(...)` dans `DraftSimultaneousService.requireOpenWindow()` et `requireNotAlreadySubmitted()`.
- [ ] **[AI-Review][Low][L1]** : `DomainExceptionHandlerTest` — aucun `@DisplayName` sur la classe ou les méthodes de test, inconsistant avec la convention du projet.
- [ ] **[AI-Review][Low][L2]** : `DraftSimultaneousService.groupByPlayer()` utilise `new java.util.ArrayList<>()` en nom pleinement qualifié (ligne 194) au lieu d'un import. Style mineur non détecté par Spotless.
- [ ] **[AI-Review][Low][L3]** : `resolveWindow()` (chemin conflit, ligne 152) : valeur de retour de `pickFirstConflict()` ignorée silencieusement. Le broadcast se produit en interne via side-effect, mais ignorer un retour non-void est un code smell.
- [ ] **[AI-Review][Low][L4]** : Section "Dev Agent Record" de la story utilise `Story Progress Notes` au lieu de la structure standard `File List` + `Completion Notes` des autres stories.
