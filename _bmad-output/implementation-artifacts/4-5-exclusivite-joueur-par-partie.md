# Story 4.5: Exclusivité joueur par partie

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participant,
I want garantir qu'un joueur n'appartient qu'à une équipe dans ma partie,
so that il n'y ait aucune incohérence de roster intra-game.

## Acceptance Criteria

1. **Given** un joueur est déjà assigné dans une partie (draft serpent ou simultané), **When** un autre pick vise ce même joueur dans la même partie, **Then** la tentative est refusée (400 ou 409) avec un message explicite ("Player is already selected in this draft") ; **And** ce même joueur reste sélectionnable dans une autre partie distincte.

2. **Given** une précondition invalide (joueur déjà pické, draft inexistant), **When** la soumission est effectuée, **Then** le système rejette la requête avec un code HTTP explicite et un message clair ; aucun état invalide n'est persisté.

## Technical Context

### Infrastructure déjà en place

#### Exception + handler (READY)
- **`PlayerAlreadySelectedException`** : `src/main/java/com/fortnite/pronos/exception/PlayerAlreadySelectedException.java`
  - Constructeur `(String message)` et `(String playerName, String teamName)`
  - **`DomainExceptionHandler`** mappe `PlayerAlreadySelectedException` → **409 CONFLICT**, code `PLAYER_ALREADY_SELECTED` ✓

#### Repository (READY)
- **`DraftPickRepositoryPort`** :
  - `boolean existsByDraftAndPlayer(Draft draft, Player player)` — legacy JPA models
  - `List<UUID> findPickedPlayerIdsByDraftId(UUID draftId)` — **UUID-only** (ajouté Story 4.2, safe à utiliser)
- **`DraftPickRepository`** : implémentation JPA complète avec `@Query`

#### Snake Draft — exclusivité déjà enforced (READY, aucun gap)
- **`GameDraftService.validatePlayerSelection()`** → `isPlayerAlreadySelected()` → `draftPickRepository.existsByDraftAndPlayer()`
- Path complet : `SnakeDraftController.processPick()` → `gameDraftService.selectPlayer()` → check ✓
- **Aucune modification nécessaire pour le serpent.**

#### DraftTrancheService — contexte de modification (READY pour extension)
- **5 dépendances** actuelles : `GameDomainRepositoryPort`, `DraftDomainRepositoryPort`, `PlayerDomainRepositoryPort`, `DraftPickRepositoryPort`, `DraftPickOrchestratorService` ✓ (CouplingTest ≤ 7)
- `validatePickByDraftId(UUID draftId, String region, UUID playerId)` :
  1. Résout le `Draft` via `draftDomainRepositoryPort.findById(draftId)`
  2. Appelle `validatePick(draft.getGameId(), region, playerId)` (tranche)
- **Actuellement** : ne vérifie PAS l'exclusivité inter-rounds
- `DraftPickRepositoryPort` déjà injecté → pas de nouvelle dépendance nécessaire

### Gap identifié pour Story 4.5

**Mode simultané uniquement** : `DraftSimultaneousController.submit()` appelle `draftTrancheService.validatePickByDraftId()` puis `simultaneousService.submit()`. Aucun des deux ne vérifie si le joueur est déjà pické dans un round précédent du même draft.

**Fix cible** : ajouter dans `DraftTrancheService.validatePickByDraftId()` :
```java
List<UUID> pickedIds = draftPickRepository.findPickedPlayerIdsByDraftId(draftId);
if (pickedIds.contains(playerId)) {
  throw new PlayerAlreadySelectedException("Player is already selected in this draft");
}
```
Placé **avant** `validatePick()` (exclusivité toujours enforced, indépendant de `tranchesEnabled`).

## Tasks / Subtasks

- [x] Task 1: Infrastructure existante — exception + handler + repository (AC: #1, #2)
  - [x] `PlayerAlreadySelectedException` → 409 CONFLICT dans `DomainExceptionHandler`
  - [x] `DraftPickRepositoryPort.findPickedPlayerIdsByDraftId(UUID)` disponible
  - [x] Snake draft déjà protégé via `GameDraftService.isPlayerAlreadySelected()`

- [x] Task 2: Ajouter check exclusivité dans `DraftTrancheService.validatePickByDraftId()` (AC: #1, #2)
  - [x] `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java`
  - [x] Méthode privée `requirePlayerNotAlreadyPickedInDraft(UUID draftId, UUID playerId)`
  - [x] Appel avant `validatePick()` dans `validatePickByDraftId()`
  - [x] Lance `PlayerAlreadySelectedException("Player is already selected in this draft")`
  - [x] Utilise `draftPickRepository.findPickedPlayerIdsByDraftId(draftId)` (pas les legacy models)

- [x] Task 3: Tests unitaires `DraftTrancheServiceTest` — exclusivité simultané (AC: #1, #2)
  - [x] `src/test/java/com/fortnite/pronos/service/draft/DraftTrancheServiceTest.java`
  - [x] `playerAlreadyPickedInDraft_throws` — PlayerAlreadySelectedException levée
  - [x] `playerNotPicked_passesThrough` — joueur absent de la liste → no-op
  - [x] `playerPickedInDifferentDraft_allowed` — liste vide pour ce draftId → autorisé

- [x] Task 4: Test intégration controller `DraftSimultaneousControllerTest` (AC: #1)
  - [x] `src/test/java/com/fortnite/pronos/controller/DraftSimultaneousControllerTest.java`
  - [x] `whenPlayerAlreadyPickedInDraft_validatePickThrows` — PlayerAlreadySelectedException propagée, simultaneousService non appelé

## Dev Notes

### Emplacement exact du fix

```java
// DraftTrancheService.java — validatePickByDraftId()
public void validatePickByDraftId(UUID draftId, String region, UUID playerId) {
  Draft draft =
      draftDomainRepositoryPort
          .findById(draftId)
          .orElseThrow(() -> new GameNotFoundException("Draft not found: " + draftId));

  requirePlayerNotAlreadyPickedInDraft(draftId, playerId);  // ← AJOUTER ICI

  validatePick(draft.getGameId(), region, playerId);
}

private void requirePlayerNotAlreadyPickedInDraft(UUID draftId, UUID playerId) {
  List<UUID> pickedIds = draftPickRepository.findPickedPlayerIdsByDraftId(draftId);
  if (pickedIds.contains(playerId)) {
    throw new PlayerAlreadySelectedException("Player is already selected in this draft");
  }
}
```

### Pattern des tests DraftTrancheServiceTest existants
```java
// Référence : ValidatePickByDraftId existant (2 tests)
@Nested @DisplayName("ValidatePickByDraftId")
class ValidatePickByDraftId {
  @Test void shouldResolveGameIdViaDraftAndDelegate() { ... }
  @Test void shouldThrowWhenDraftNotFound() { ... }
  // À AJOUTER : whenPlayerAlreadyPickedInDraft_throws
}
```

Mock nécessaire pour le nouveau test :
```java
when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
    .thenReturn(List.of(PLAYER_ID));
// → expect PlayerAlreadySelectedException
```

### Import à ajouter dans DraftTrancheService
```java
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import java.util.List;  // déjà présent si findPickedPlayerIdsByDraftId utilisé
```

### Contraintes architecture
- Pas de nouvelle dépendance (`DraftPickRepositoryPort` déjà injecté dans `DraftTrancheService`)
- CouplingTest : 5 dépendances actuelles, aucune ajoutée ✓
- Spotless : `mvn spotless:apply` avant `mvn test`
- Linter Windows : relire fichier avant édition si délai

### Scoping d'exclusivité
- Exclusivité **par draft** (game instance), pas globale
- Même joueur sélectionnable dans Game A et Game B simultanément ✓
- `findPickedPlayerIdsByDraftId(UUID draftId)` scope correct

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

### Completion Notes List
- Tasks 1 : infrastructure déjà en place (PlayerAlreadySelectedException, DraftPickRepositoryPort, snake draft)
- Task 2 : `requirePlayerNotAlreadyPickedInDraft()` ajouté dans `DraftTrancheService.validatePickByDraftId()`
- Task 3 : 3 nouveaux tests `ValidatePickByDraftId` — 15/15 verts (DraftTrancheServiceTest)
- Task 4 : 1 nouveau test `Submit` — 7/7 verts (DraftSimultaneousControllerTest)
- 2136 tests backend total, 0 nouvelle régression (5 erreurs AdminDatabaseServiceTest = FEAT-005l pré-existant)

### File List
- `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java` (modifié — +import +méthode privée +appel)
- `src/test/java/com/fortnite/pronos/service/draft/DraftTrancheServiceTest.java` (modifié — +3 tests ValidatePickByDraftId)
- `src/test/java/com/fortnite/pronos/controller/DraftSimultaneousControllerTest.java` (modifié — +1 test Submit)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

Aucun fix automatique — aucun problème HIGH ou MEDIUM. 3 LOW ajoutés en action items.

### Action items

- [ ] **[AI-Review][Low][L1]** : `requirePlayerNotAlreadyPickedInDraft()` (ligne 119) utilise `List.contains()` — O(n). `recommendPlayer()` (ligne 153-154) convertit correctement en `HashSet` pour la même opération. Harmoniser : `new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draftId)).contains(playerId)`.
- [ ] **[AI-Review][Low][L2]** : `validatePickByDraftId()` effectue deux lookups de draft distincts quand `tranchesEnabled=true` : `findById(draftId)` puis `findActiveByGameId(gameId)` dans `validatePick()`. Envisager de passer le draft résolu en paramètre pour éviter le double aller-retour DB.
- [ ] **[AI-Review][Low][L3]** : Test `resolveGameIdFromDraft_delegates` (ligne 180-191) ne stubbe pas `draftPickRepository.findPickedPlayerIdsByDraftId` — repose implicitement sur le comportement Mockito par défaut (liste vide). Ajouter `when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of())` pour rendre l'intention explicite.
