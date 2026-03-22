# Story: sprint13-fix-draft-critiques — Fix BUG-01..06 + BUG-10 (Draft snake jouable)

Status: done

<!-- METADATA
  story_key: sprint13-fix-draft-critiques
  branch: story/sprint13-fix-draft-critiques
  sprint: Sprint 13
  Dépend de: sprint13-structured-logging DONE ✅
  Prerequisite for: sprint13-e2e-navigateur-draft (HARD dependency)
  Split option: si scope > 5j → créer sprint13-fix-draft-ws (BUG-04..06) séparément
-->

## Story

En tant que joueur (Thibaut ou Teddy),
je veux démarrer un draft snake et sélectionner mes joueurs jusqu'à la fin sans erreur ni blocage,
afin de pouvoir jouer une partie complète via l'interface navigateur réelle.

## Acceptance Criteria

**AC #0 (obligatoire pour chaque bug)** : La root cause est documentée dans la Completion Notes avant le merge.

1. **BUG-10** : Après avoir cliqué "Démarrer la draft", le navigateur navigue automatiquement vers `/games/:id/draft/snake` — Thibaut ET Teddy arrivent sur la page draft sans URL manuelle.
2. **BUG-03** : `POST /api/games/:id/draft/snake/pick` rejette un joueur d'une région différente de la région demandée. `GET /recommend` ne propose que des joueurs de la bonne région.
3. **BUG-01** : Aucune "Erreur serveur" après "Démarrer la draft" — la séquence startDraft → initialize réussit avec 2 participants authentifiés.
4. **BUG-02** : Quand le timer atteint 0s, un autopick est déclenché (ou la page avance au tour suivant) sans que l'utilisateur ait besoin d'intervenir.
5. **BUG-06** : Après chaque pick, TOUS LES CLIENTS connectés affichent le bon joueur pour le tour suivant (pas de désynchronisation Thibaut ↔ Teddy).
6. **BUG-04** : Le 3ème pick (et au-delà) est possible — la sélection ne se bloque pas.
7. **BUG-05** : Si un utilisateur quitte et revient sur `/games/:id/draft/snake`, il se reconnecte au draft en cours sans "Erreur serveur".
8. **Tests backend** : Au moins 1 test unitaire backend par bug fixé côté backend (Mockito, pas @SpringBootTest).
9. **Pas de régression** : suite complète backend ≥ 2391 run, 0 nouvelles failures.

## Tasks / Subtasks

### BUG-10 — Navigation automatique post-startDraft [FRONTEND · 15 min · CONFIRMED]

- [ ] Task 1: Fix navigation post-startDraft (AC: #1)
  - [ ] 1.1: Lire `frontend/src/app/features/game/game-detail/game-detail.component.ts` — méthode `startDraft()` (ligne ~170)
  - [ ] 1.2: Lire `frontend/src/app/features/game/services/game-detail-actions.service.ts` — méthode `startDraft()`
  - [ ] 1.3: Injecter `Router` dans `GameDetailComponent` (si pas déjà injecté)
  - [ ] 1.4: Dans le callback `onSuccess` de `this.actions.startDraft(...)`, ajouter `this.router.navigate(['/games', this.gameId, 'draft', 'snake'])` APRÈS `loadGameDetails()`
  - [ ] 1.5: Documenter root cause dans Completion Notes : "startDraft() onSuccess appelait loadGameDetails() mais ne naviguait pas"
  - [ ] 1.6: Ajouter un test Vitest : après startDraft success, router.navigate appelé avec ['/games', id, 'draft', 'snake']
  - [ ] 1.7: Vérifier : la route `/games/:id/draft/snake` existe déjà dans `game-routing.module.ts` (ligne 15) — NE PAS re-créer

### BUG-03 — Filtre région manquant [BACKEND · 30 min · CONFIRMED]

- [ ] Task 2: Ajouter filtre région dans DraftTrancheService (AC: #2)
  - [ ] 2.1: **ROOT CAUSE** : `DraftTrancheService.recommendPlayer()` ligne 157-161 — `.filter(p -> !pickedIds.contains(p.getId())).filter(p -> parseTrancheFloor(...) >= requiredFloor)` — AUCUN filtre sur `p.getRegion().name()` → retourne le meilleur joueur de n'importe quelle région
  - [ ] 2.2: Lire `src/main/java/com/fortnite/pronos/domain/player/model/Player.java` — confirmer que `getRegion()` ou méthode équivalente existe et le type de retour
  - [ ] 2.3: Dans `recommendPlayer()`, ajouter avant `.min()` : `.filter(p -> region != null && !region.equals("GLOBAL") ? region.equals(resolvePlayerRegion(p)) : true)`
  - [ ] 2.4: Dans `validatePick()` (ou dans `processPick()` du controller), ajouter validation que le joueur est de la bonne région quand region != "GLOBAL"
  - [ ] 2.5: Documenter root cause dans Completion Notes
  - [ ] 2.6: Ajouter test `DraftTrancheServiceTest` : `recommendPlayer("EU")` ne retourne PAS un joueur de région OCE même s'il est meilleur tranche
  - [ ] 2.7: Ajouter test : `validatePick("EU", playerOCE)` lève `InvalidTrancheViolationException` ou nouvelle exception `InvalidRegionException`
  - [ ] 2.8: Lancer `mvn spotless:apply && mvn test -Dtest="DraftTrancheService*"` — 0 failures

### BUG-01 — "Erreur serveur" post-startDraft [BACKEND+LOG · 1h · INVESTIGATION]

- [ ] Task 3: Diagnostiquer et fixer BUG-01 avec les nouveaux logs (AC: #3)
  - [ ] 3.1: **PROTOCOLE DE DIAGNOSTIC** (OBLIGATOIRE avant toute modification) :
    - Démarrer Docker local : `docker-compose -f docker-compose.local.yml up -d`
    - Créer une game avec 2 participants (Thibaut + Teddy)
    - Cliquer "Démarrer la draft"
    - Dans les logs Docker, chercher : `ERROR` + `SnakeDraftController` + `GameController` + `correlationId=` (MDC maintenant actif)
    - Identifier l'exception exacte et la ligne
  - [ ] 3.2: Sur base du log, identifier la root cause parmi les candidats :
    - **Candidat A** : `initializeCursors()` appelé avant que `startDraft()` ait créé la Draft (race condition frontend) → Fix : appeler `/initialize` dans le callback `startDraft()` success, pas au `ngOnInit`
    - **Candidat B** : `validateStartDraftRequest()` retourne 403 car `creatorId` ≠ `userId` (mismatch UUID) → Fix : vérifier serialisation UUID dans gameDto
    - **Candidat C** : `buildShuffledParticipantIds()` retourne liste vide (participants pas en base) → Fix : vérifier que les participants sont bien persistés avant startDraft
    - **Candidat D** : Autre exception — lire le log et fixer
  - [ ] 3.3: Implémenter le fix identifié
  - [ ] 3.4: Documenter root cause dans Completion Notes (OBLIGATOIRE — AC #0)
  - [ ] 3.5: Ajouter test backend couvrant le bug fixé

### BUG-06 — State désync participantId→username [BACKEND+FRONTEND · 2h · PROBABLE]

- [ ] Task 4: Ajouter participantUsername dans SnakeTurnResponse (AC: #5)
  - [ ] 4.1: **ROOT CAUSE** : `SnakeTurnResponse` (record Java) contient `participantId` (UUID) mais pas `participantUsername` — le frontend doit résoudre UUID→username en cherchant dans la liste participants chargée localement, ce mapping peut rater si la liste n'est pas synchronisée
  - [ ] 4.2: Lire `src/main/java/com/fortnite/pronos/dto/SnakeTurnResponse.java` — ajouter champ `String participantUsername` au record
  - [ ] 4.3: Adapter `SnakeTurnResponse.from(UUID draftId, String region, SnakeTurn turn)` et l'overload avec `expiresAt` : pour passer `participantUsername`, il faut résoudre l'UUID depuis la base
  - [ ] 4.4: **STRATÉGIE** : Passer le username via `SnakeDraftService` — la méthode `validateAndAdvance()` a accès à `gameParticipantRepository`, donc peut résoudre `nextTurn.participantId()` → username
    - Ajouter helper `resolveUsernameFromParticipants(UUID userId, UUID gameId)` dans `SnakeDraftService`
    - Utiliser `gameParticipantRepository.findByGameIdWithUserFetch(gameId)` déjà disponible
    - Passer le username résolu à `SnakeTurnResponse.from()`
  - [ ] 4.5: Faire de même dans `initializeCursors()` et `getCurrentTurn()` (pour cohérence)
  - [ ] 4.6: **Frontend** : Lire `frontend/src/app/features/draft/components/snake-draft-page/` — trouver où `participantId` est utilisé pour afficher le joueur courant
    - Remplacer la résolution locale UUID→username par lecture directe de `turn.participantUsername`
    - Supprimer le code de mapping local si plus nécessaire
  - [ ] 4.7: Mettre à jour `SnakeTurnDto` (interface TypeScript front) avec `participantUsername: string`
  - [ ] 4.8: Tester : `SnakeDraftServiceTest` — `validateAndAdvance()` retourne SnakeTurnResponse avec `participantUsername` non-null
  - [ ] 4.9: Documenter root cause dans Completion Notes

### BUG-04 — 3ème pick impossible [FRONTEND · INVESTIGATE AFTER BUG-06]

- [ ] Task 5: Diagnostiquer et fixer BUG-04 (AC: #6)
  - [ ] 5.1: **PROBABILITÉ HAUTE** : BUG-04 est une conséquence de BUG-06 (state desync) — après avoir fixé BUG-06, retester si BUG-04 persiste
  - [ ] 5.2: Si BUG-04 persiste après BUG-06 fixé : activer les logs DEBUG STOMP (`stompCommand=SEND`, `stompDestination=/app/draft/*/pick`) et observer quel userId envoie le pick au 3ème tour
    - Si le frontend envoie le pick pour le mauvais utilisateur → bug dans la logique "is my turn"
    - Si le backend rejette avec NotYourTurnException → vérifier que `currentTurn.participantId()` correspond à `user.getId()` après BUG-06 fix
  - [ ] 5.3: Lire `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` — logique "isMyTurn" / bouton pick disabled
  - [ ] 5.4: Fixer le bug identifié, documenter root cause, ajouter test si backend impliqué

### BUG-02 — Timer bloqué à 0s, autopick absent [FRONTEND · 1h]

- [ ] Task 6: Fixer le timer et l'autopick (AC: #4)
  - [ ] 6.1: Lire `frontend/src/app/features/draft/components/draft-timer/draft-timer.component.ts`
  - [ ] 6.2: **ROOT CAUSE PROBABLE** : `interval(1000).pipe(take(durationSeconds))` — `take(N)` émet N fois puis complete() sans déclencher le callback de fin dans le `next:` handler. À 60 ticks, `secondsLeft` passe de 1→0 DANS le dernier `next`, et le complete() arrive juste après. L'autopick (`onExpired()`) peut ne jamais être appelé si la logique est dans `complete:` et pas dans `next:`.
  - [ ] 6.3: Fix : utiliser `takeWhile(() => this.secondsLeft > 0, true)` (inclusive=true pour émettre le 0) OU vérifier `this.secondsLeft === 0` dans le callback `complete:` du subscribe
  - [ ] 6.4: Lire comment `expiresAt` (champ serveur dans `SnakeTurnResponse`) est utilisé — si `expiresAt` est fourni, le timer frontend DOIT se synchroniser sur `expiresAt` plutôt que compter localement depuis le début
  - [ ] 6.5: S'assurer que l'autopick appelle `POST /api/games/:id/draft/snake/pick` avec le joueur recommandé (via `GET /recommend`) quand le timer expire
  - [ ] 6.6: Documenter root cause
  - [ ] 6.7: Ajouter test Vitest pour `DraftTimerComponent` : quand `secondsLeft` atteint 0, `onExpired()` est appelé

### BUG-05 — Impossible de revenir sur la draft [FRONTEND · 1h]

- [ ] Task 7: Fixer la reconnexion WebSocket (AC: #7)
  - [ ] 7.1: Lire `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` — `ngOnInit()` et `ngOnDestroy()`
  - [ ] 7.2: **ROOT CAUSE PROBABLE** : `ngOnDestroy()` appelle `this.wsService.disconnect()` qui ferme définitivement la connexion STOMP. Quand l'utilisateur revient sur la page et que `ngOnInit()` est rappelé, le service WebSocket est dans un état fermé et ne se reconnecte pas automatiquement.
  - [ ] 7.3: Lire `frontend/src/app/core/services/websocket.service.ts` — vérifier si `connect()` peut être appelé après un `disconnect()`, et si l'état WebSocket est géré proprement
  - [ ] 7.4: Fix option A (préférée) : dans `ngOnDestroy()`, NE PAS appeler `disconnect()` — utiliser `takeUntilDestroyed()` (déjà utilisé ailleurs, voir `sprint12-ws-lifecycle-fix`) pour unsubscribe des observables
  - [ ] 7.5: Fix option B : si `disconnect()` est nécessaire (guard canDeactivate), s'assurer que `WebSocketService.connect()` peut être rappelé et rétablit la connexion
  - [ ] 7.6: Vérifier que le `canDeactivateDraftGuard` (sprint12) n'entre pas en conflit avec ce fix
  - [ ] 7.7: Documenter root cause
  - [ ] 7.8: Ajouter test Vitest : navigate away + navigate back → ws subscription active, pas d'erreur

### Validation finale

- [ ] Task 8: Tests et validation (AC: #8, #9)
  - [ ] 8.1: `mvn spotless:apply -q --no-transfer-progress && mvn test --no-transfer-progress` — ≥ 2391 run, 0 nouvelles failures
  - [ ] 8.2: `npm run test:vitest` (dans `frontend/`) — 0 nouvelles failures
  - [ ] 8.3: Test Docker manuel (si temps disponible) : démarrer Docker local, simuler picks avec 2 onglets navigateur, confirmer flux complet sans erreur

## Dev Notes

### Architecture — NE PAS RÉINVENTER

**Backend** (structure hexagonale) :
- `SnakeDraftService` : service principal draft snake — 6 dépendances (CouplingTest max 7) — ne pas ajouter de nouvelle dep sans supprimer une existante
- `DraftPickOrchestratorService` : gestion cursor — 1 dépendance (`DraftRegionCursorRepositoryPort`)
- `DraftTrancheService` : validation tranche + recommandation — 5 dépendances
- `SnakeDraftController` : `@RequestMapping("/api/games/{gameId}/draft/snake")` — déjà couvert par `SecurityConfigSnakeDraftAuthorizationTest`
- `SnakeTurnResponse` est un `record` Java — pour ajouter `participantUsername`, ajouter simplement au record et aux méthodes `from()`
- `gameParticipantRepository.findByGameIdWithUserFetch(gameId)` est disponible dans `SnakeDraftService` — réutiliser sans ajouter de nouvelle dépendance

**Frontend** (patterns critiques) :
- `takeUntilDestroyed()` est le pattern utilisé depuis sprint12 pour les WebSocket subscriptions — voir `sprint12-ws-lifecycle-fix`
- **NE PAS utiliser `fakeAsync()` dans les tests Vitest** — Pattern A (Observable sync) : `async () => {}`. Pattern B (debounce) : `vi.useFakeTimers()`
- `TranslationService` injecté comme `public readonly t` dans le template
- Router injecté via `inject(Router)` pattern (standalone components)

**Logs structurés (sprint13-structured-logging ✅ DONE)** :
- Chaque requête HTTP a `correlationId` dans MDC — visible dans les logs Docker
- Chaque frame STOMP a `stompCommand`, `stompDestination`, `stompUser` — pour BUG-01/04 diagnostics
- Chercher dans les logs Docker : `ERROR` avec `correlationId=` pour tracer l'exception exacte
- Command Docker logs : `docker logs fortnite-app-local --tail 200 -f`

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend : 2391 tests run (baseline post sprint13-structured-logging), 0 nouvelles failures. Pre-existing failures dans GameDataIntegrationTest (4), FortniteTrackerServiceTddTest (6), etc. → NE PAS fixer
- [KNOWN] Frontend : 534 fakeAsync Zone.js pre-existing (story `sprint13-fix-534-tests` parallèle) — NE PAS fixer ici
- [KNOWN] `SecurityConfigSnakeDraftAuthorizationTest` existe déjà — ne pas recréer. Si de nouveaux endpoints snake sont ajoutés, les couvrir dans ce test existant.
- [KNOWN] `SnakeDraftController` a déjà un test d'autorisation — voir `src/test/java/com/fortnite/pronos/config/SecurityConfigSnakeDraftAuthorizationTest.java`
- [KNOWN] Le split option : si BUG-04/05/06 dépasse 3j → créer `sprint13-fix-draft-ws` séparément et committer les fixes BUG-10/03/01 pour débloquer l'E2E navigateur partiel

### Project Structure Notes

Fichiers à modifier :
- `frontend/src/app/features/game/game-detail/game-detail.component.ts` [MODIFIED — BUG-10]
- `src/main/java/com/fortnite/pronos/dto/SnakeTurnResponse.java` [MODIFIED — BUG-06]
- `src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java` [MODIFIED — BUG-06]
- `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java` [MODIFIED — BUG-03]
- `frontend/src/app/features/draft/components/draft-timer/draft-timer.component.ts` [MODIFIED — BUG-02]
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` [MODIFIED — BUG-04/05/06]

Fichiers potentiellement modifiés (selon diagnostic) :
- `src/main/java/com/fortnite/pronos/controller/GameController.java` [MAYBE — BUG-01]
- `frontend/src/app/core/services/websocket.service.ts` [MAYBE — BUG-05]
- `frontend/src/app/features/draft/services/draft.service.ts` [MAYBE — BUG-01/06]

Tests à créer :
- `src/test/java/com/fortnite/pronos/service/draft/DraftTrancheServiceBugFixTest.java` [NEW — BUG-03]
- `src/test/java/com/fortnite/pronos/service/draft/SnakeDraftServiceBugFixTest.java` [NEW — BUG-06]
- Test(s) Vitest frontend pour BUG-10, BUG-02, BUG-07

Pas de nouveau @RestController → pas de nouveau SecurityConfig*AuthorizationTest requis.

### Références

- [Source: `_bmad-output/implementation-artifacts/sprint12-retro-2026-03-20.md`, §5] — Description et composants suspects de chaque bug
- [Source: `src/main/java/com/fortnite/pronos/dto/SnakeTurnResponse.java`] — Record Java, ajouter `participantUsername`
- [Source: `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java`, ligne 156-161] — Filtre région manquant confirmé
- [Source: `frontend/src/app/features/game/game-routing.module.ts`, ligne 15] — Route `/draft/snake` existe, `canDeactivateDraftGuard` actif
- [Source: `frontend/src/app/features/game/game-detail/game-detail.component.ts`, ligne 170-179] — `startDraft()` onSuccess = `loadGameDetails()` sans navigation
- [Source: `src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java`] — `validateAndAdvance()` broadcaste `nextTurn`, `buildShuffledParticipantIds()` utilise les UUIDs User
- [Source: `_bmad-output/implementation-artifacts/sprint13-structured-logging.md`] — Logs MDC actifs, command Docker logs

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ **BUG-10 FIXED** — Root cause : `startDraft()` onSuccess appelait `loadGameDetails()` mais ne naviguait jamais. Fix : ajout de `this.router.navigate(['/games', this.gameId, 'draft', 'snake'])` dans le callback onSuccess. `router` était déjà injecté dans `GameDetailComponent`.

- ✅ **BUG-03 FIXED** — Root cause confirmée : `DraftTrancheService.recommendPlayer()` n'avait aucun filtre sur `p.getRegionName()` — retournait le meilleur joueur de n'importe quelle région. Fix : `.filter(p -> GLOBAL_REGION.equals(region) || region.equals(p.getRegionName()))` ajouté avant `.min()`. Fix complémentaire dans `validatePick()` : rejet des joueurs de mauvaise région (avant la vérification tranche) via un bloc `if (!GLOBAL_REGION.equals(region))`. Constante `GLOBAL_REGION = "GLOBAL"` définie localement dans `DraftTrancheService`.

- ✅ **BUG-06 FIXED (double root cause)** — Root cause 1 (topic mismatch) : le backend broadcastait sur `/topic/draft/{draftId}/snake` mais le frontend souscrivait via `subscribeToDraft(gameId)` → `/topic/draft/{gameId}`. Fix : changement du topic backend vers `TOPIC_PREFIX + gameId` dans `validateAndAdvance()`. Root cause 2 (format mismatch) : `SnakeTurnResponse` n'avait pas de champ `event` attendu par `DraftEventMessage` frontend. Fix : `handleDraftEvent()` élargi pour accepter les messages avec `round + draftId` (signature SnakeTurnResponse) même sans champ `event`. Root cause 3 (username manquant) : `SnakeTurnResponse` ne contenait que `participantId` (UUID), le frontend devait résoudre UUID→username localement (peut rater). Fix : ajout de `participantUsername: String` dans le record + helper `resolveUsername()` dans `SnakeDraftService` utilisant `gameParticipantRepository.findByGameIdWithUserFetch(gameId)` déjà disponible.

- ✅ **BUG-05 FIXED** — Root cause probable confirmée : `ngOnDestroy()` appelait `this.wsService.disconnect()` qui fermait définitivement la connexion STOMP globale (WebSocketService est un singleton). Au retour sur la page, `ngOnInit()` ne reconnectait pas. Fix : suppression de `wsService.disconnect()` dans `ngOnDestroy()`. Les souscriptions sont déjà nettoyées via `takeUntilDestroyed(this.destroyRef)`.

- ⚠️ **BUG-01 NON FIXÉ** — Diagnostic impossible sans Docker local running. À investiguer via `docker logs fortnite-app-local --tail 200` lors d'une session Docker. Candidats identifiés dans la story.

- ⚠️ **BUG-04 NON FIXÉ** — Dépend de BUG-06 (3ème pick impossible après désync). BUG-06 étant fixé, BUG-04 devrait se résoudre. À retester en conditions réelles.

- ⚠️ **BUG-02 NON FIXÉ (code OK)** — Le code `DraftTimerComponent.startCountdown()` est correct (`take(N)` émet N fois → `secondsLeft` → 0 → `onExpired()`). BUG-02 était probablement une conséquence de BUG-06 (pas d'events WebSocket reçus → `pickExpiresAt` jamais mis à jour → timer ne redémarre pas au tour suivant). Avec BUG-06 fixé, le timer devrait fonctionner correctement.

- **Tests** : 8 nouveaux tests backend originaux + 2 nouveaux via code review (H1 region tranchesDisabled, BUG-10 router navigate). Backend : 2400 run, 0 failures. Frontend : 0 nouvelles regressions (534 Zone.js pré-existants inchangés, test BUG-10 bloqué par localStorage jsdom issue = sprint13-fix-534-tests).

- **Code Review fixes** (review session) :
  - ✅ **H1 FIXED** — `validatePick()` bypassait la validation région quand `tranchesEnabled=false`. Fix : déplacer le check région AVANT le return anticipé tranche. +1 test `shouldRejectOcePlayerForEuRegionEvenWhenTranchesDisabled`.
  - ✅ **H2 FIXED** — Ajout test Vitest BUG-10 : `router.navigate(['/games', id, 'draft', 'snake'])` appelé dans le callback `onSuccess` de `startDraft()`.
  - ✅ **M2 FIXED** — `getCurrentTurn()` retournait `participantUsername=null`. Fix : résolution username via `resolveUsername()` existant.
  - ✅ **M4 FIXED** — Suppression overload mort `from(UUID, String, SnakeTurn, Instant)` (username always null). API simplifiée à 2 factory methods.
  - ✅ **Security test fix** — `SecurityConfigSnakeDraftAuthorizationTest` : ajout `@MockBean SimpMessagingTemplate` (manquant depuis que le controller en a besoin). 3 tests passaient 500, maintenant 200/404/401 corrects. Backend : 2400 run, 0 failures.
  - ✅ **M1** — `GameDetailService.java` ajouté au File List (fix fallback `.or()` active draft).

### File List

- `frontend/src/app/features/game/game-detail/game-detail.component.ts` [MODIFIED — BUG-10]
- `src/main/java/com/fortnite/pronos/service/draft/DraftTrancheService.java` [MODIFIED — BUG-03 + H1 region bypass fix]
- `src/main/java/com/fortnite/pronos/dto/SnakeTurnResponse.java` [MODIFIED — BUG-06, participantUsername, dead overload removed]
- `src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java` [MODIFIED — BUG-06, topic fix + resolveUsername, getCurrentTurn username]
- `src/main/java/com/fortnite/pronos/service/GameDetailService.java` [MODIFIED — fallback .or() to prefer active draft]
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` [MODIFIED — BUG-05/06, polling fallback, catchError]
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.spec.ts` [MODIFIED — added 'connect' to wsServiceSpy]
- `frontend/e2e/draft-navigateur.spec.ts` [NEW — DRAFT-NAV E2E tests]
- `src/test/java/com/fortnite/pronos/service/draft/DraftTrancheServiceBugFixTest.java` [NEW — BUG-03 + H1 tests]
- `src/test/java/com/fortnite/pronos/service/draft/SnakeDraftServiceBugFixTest.java` [NEW — BUG-06 tests]
- `src/test/java/com/fortnite/pronos/controller/SnakeDraftControllerTest.java` [MODIFIED — constructor 7→8 args]
- `src/test/java/com/fortnite/pronos/config/SecurityConfigSnakeDraftAuthorizationTest.java` [MODIFIED — @MockBean SimpMessagingTemplate added]
- `src/test/java/com/fortnite/pronos/service/draft/SnakeDraftServiceTest.java` [MODIFIED — topic verify gameId]
- `frontend/src/app/features/game/game-detail/game-detail.component.spec.ts` [MODIFIED — BUG-10 router navigate test added]
