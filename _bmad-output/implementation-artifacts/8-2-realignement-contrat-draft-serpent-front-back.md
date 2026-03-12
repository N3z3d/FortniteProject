# Story 8.2: Realignement contrat draft serpent front/back

Status: done

<!-- METADATA
  story_key: 8-2-realignement-contrat-draft-serpent-front-back
  branch: story/8-2-realignement-contrat-draft-serpent-front-back
  sprint: Sprint 4
  Note: Story locale de hardening produit. Objectif: reparer le contrat snake draft reel pour remettre en service le flow profond front/back et activer la suite Playwright dediee.
-->

## Story

As a developer validating the deep draft flow locally,
I want the snake draft page to consume the real backend contract and persist picks end to end,
so that the dedicated Playwright suite can cover draft interactions instead of staying in fixme.

## Acceptance Criteria

1. La page snake draft utilise un contrat runtime reel local et ne depend plus du pseudo endpoint legacy `/api/drafts/{gameId}/board-state`.
2. Un pick confirme en mode snake est persiste cote backend et se reflete dans l etat de partie (`selectedPlayers`, `totalPlayers`, progression draft).
3. Les liens UI vers le draft en cours pointent vers la route snake reelle (`/games/:id/draft/snake`) depuis la home et le detail partie.
4. `frontend/e2e/draft-flow.spec.ts` n est plus en `fixme` et valide au minimum:
   - demarrage du draft depuis le detail
   - selection d un joueur par le picker courant
   - rechargement de la page avec reflet du pick dans le board
5. La story documente explicitement ce qui reste hors scope apres realignement (ex: synchro WebSocket fine si encore partielle).

## Tasks / Subtasks

- [x] Task 1 - Synchroniser le board BMAD et borner le scope 8.2 (AC: #1, #4, #5)
  - [x] 1.1 Enregistrer la story 8.2 dans le board sprint et l epic 8
  - [x] 1.2 Capturer le contrat casse observe en local (`/api/drafts/*`, routes UI, picks non refleches)

- [x] Task 2 - Corriger le backend pick -> roster participant en TDD (AC: #2)
  - [x] 2.1 Ajouter un test rouge sur `GameDraftService` pour exiger la mise a jour du participant apres pick
  - [x] 2.2 Persister le joueur selectionne dans `GameParticipant.selectedPlayers`
  - [x] 2.3 Revalider la remontee `selectedPlayers` via `GET /api/games/{id}/details`

- [x] Task 3 - Realigner le front snake draft sur le contrat runtime reel (AC: #1, #3)
  - [x] 3.1 Ajouter des methodes snake dediees dans `DraftService` sans casser les usages legacy encore existants
  - [x] 3.2 Faire consommer a `SnakeDraftPageComponent` l etat runtime reel et soumettre les picks via HTTP
  - [x] 3.3 Corriger les liens UI home/detail vers `/games/:id/draft/snake`

- [x] Task 4 - Reactiver la couverture E2E draft dediee (AC: #4)
  - [x] 4.1 Promouvoir `frontend/e2e/draft-flow.spec.ts` de `fixme` a couverture active
  - [x] 4.2 Valider les trois scenarios sur Docker local seeded users

- [x] Task 5 - Verification finale et notes de completion (AC: #1, #2, #3, #4, #5)
  - [x] 5.1 Lancer les tests backend/frontend unitaires cibles
  - [x] 5.2 Lancer `npx playwright test e2e/draft-flow.spec.ts`
  - [x] 5.3 Mettre a jour les notes de completion avec le reliquat reel

## Dev Notes

### Current Measured Baseline

- Baseline au debut de la story:
  - `frontend/e2e/draft-flow.spec.ts` = `3 fixme`
  - la page snake appelle encore `DraftService.getDraftBoardState()` sur `/api/drafts/{gameId}/board-state`
  - les picks snake passent par `WebSocketService.publishDraftPick()` alors que le runtime local ne persiste pas ce flow websocket

### Scope Guardrails

- Cette story ne rouvre pas l hebergement/staging ni Railway.
- Cette story cible le snake draft profond local seulement.
- Ne pas elargir au trade/swap E2E dans ce lot.

### Technical Requirements

- Les endpoints runtime reellement utilises localement sont:
  - `GET /api/games/{id}/details`
  - `GET /api/games/{id}/participants`
  - `GET /players/catalogue`
  - `GET /api/games/{id}/draft/snake/turn?region=GLOBAL`
  - `POST /api/games/{id}/draft/snake/initialize`
  - `POST /api/games/{id}/draft/snake/pick`
- Les profils seedes utilises en validation locale sont `thibaut` et `teddy`.
- Le flux UI cible reste:
  - create quick game
  - generate invitation code
  - join with code
  - start draft
  - open snake board

### Testing Requirements

- Toute correction backend doit avoir un test rouge puis vert sur `GameDraftService`.
- Toute correction frontend du snake draft doit avoir au moins un test de service ou composant adapte.
- La story n est pas terminee tant que `frontend/e2e/draft-flow.spec.ts` ne passe pas activement.

### Pre-existing Gaps / Known Issues

- [KNOWN] Le topic WebSocket snake utilise encore un contrat distinct (`/topic/draft/{draftId}/snake`) du listener front legacy.
- [KNOWN] La story vise d abord la fiabilite locale du flow HTTP; la synchro temps reel peut rester partielle si le board reste correct apres refresh/reload.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `frontend/e2e/draft-flow.spec.ts`
- `frontend/src/app/features/draft/services/draft.service.ts`
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts`
- `frontend/src/app/features/game/game-home/game-home.component.html`
- `frontend/src/app/features/game/game-detail/game-detail.component.html`
- `src/main/java/com/fortnite/pronos/service/game/GameDraftService.java`
- `src/main/java/com/fortnite/pronos/service/GameDetailService.java`
- `src/main/java/com/fortnite/pronos/controller/SnakeDraftController.java`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Audit local draft contract captured on 2026-03-08 before implementation
- Runtime validation captured in `_bmad-output/implementation-artifacts/8-2-realignement-contrat-draft-serpent-front-back-runtime-validation-2026-03-08.md`

### Implementation Plan

- Corriger d abord la persistance backend du roster participant apres pick.
- Brancher ensuite le snake draft front sur les endpoints runtime reels deja exposes.
- Reactiver enfin la suite Playwright dediee et prouver le flow create/join/start/pick/reload.

### Completion Notes List

- Le backend persiste maintenant le joueur choisi dans `GameParticipant.selectedPlayers` et sauvegarde aussi le participant apres `selectPlayer`, ce qui remet en coherence le roster retourne par `GET /api/games/{id}/details`.
- Le test rouge `GameDraftServiceTddTest` verrouille explicitement la mise a jour du roster participant apres un pick snake.
- `DraftService` expose des methodes snake runtime dediees qui consomment `details`, `participants`, `catalogue`, `snake/turn` et `snake/pick`, sans repasser par le pseudo endpoint legacy `/api/drafts/{gameId}/board-state`.
- Le contrat runtime reel du tour courant a ete clarifie: `snake/turn` retourne un `userId` et non un `participantId`; le front mappe donc `userId -> username -> participantId` via `/api/games/{id}/participants`.
- `SnakeDraftPageComponent` soumet maintenant ses picks via HTTP, recharge le board runtime apres confirmation et calcule le tour courant de facon robuste sur le username.
- Les liens UI de la home partie et du detail partie pointent maintenant vers `/games/:id/draft/snake`.
- `frontend/e2e/draft-flow.spec.ts` n est plus en `fixme` et valide `start draft`, `pick courant` et `reload board` en Docker local sur des comptes seedes.
- La review a ferme trois ecarts restants:
  - le board recupere maintenant `draftInfo.totalRounds` depuis `GET /api/games/{id}/details`, ce qui remet une progression draft coherente au lieu de la deduire du round courant
  - le mapping du picker courant tolere maintenant les ecarts de casse entre `/participants` et `/details`
  - l autopick snake est desormais bloque hors tour courant et pendant un pick deja en cours, ce qui evite les requetes concurrentes ou non autorisees
- Validation finale reussie:
  - `mvn "-Djacoco.skip=true" -q "-Dtest=GameDraftServiceTddTest,GameDetailServiceTest" test`
  - `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/draft/services/draft.service.snake.spec.ts --include src/app/features/draft/components/snake-draft-page/snake-draft-page.component.spec.ts`
  - `npx playwright test e2e/draft-flow.spec.ts`
- Reliquat volontaire hors scope apres realignement:
  - synchro WebSocket fine du snake draft toujours partielle
  - couverture E2E trade/swap toujours a traiter dans une story suivante

### File List

- `_bmad-output/implementation-artifacts/8-2-realignement-contrat-draft-serpent-front-back.md`
- `_bmad-output/implementation-artifacts/8-2-realignement-contrat-draft-serpent-front-back-runtime-validation-2026-03-08.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/planning-artifacts/epics.md`
- `frontend/e2e/draft-flow.spec.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.spec.ts`
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts`
- `frontend/src/app/features/draft/services/draft.service.snake.spec.ts`
- `frontend/src/app/features/draft/services/draft.service.ts`
- `frontend/src/app/features/game/game-detail/game-detail.component.html`
- `frontend/src/app/features/game/game-home/game-home.component.html`
- `src/main/java/com/fortnite/pronos/dto/GameDetailDto.java`
- `src/main/java/com/fortnite/pronos/service/GameDetailService.java`
- `src/main/java/com/fortnite/pronos/service/game/GameDraftService.java`
- `src/test/java/com/fortnite/pronos/service/GameDetailServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameDraftServiceTddTest.java`

### Change Log

- 2026-03-08 - Story created to repair the snake draft front/back contract and reactivate dedicated E2E coverage.
- 2026-03-08 - Backend roster persistence, frontend snake runtime mapping, dedicated draft E2E activation, and BMAD proof artifacts completed; story moved to review.
- 2026-03-08 - Senior review fixes tightened snake autopick guards, restored totalRounds in the runtime contract, hardened participant mapping against username case drift, and approved the story.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-08

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. HIGH - `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts`
   - Le timer pouvait declencher un autopick hors tour courant, et un second submit pouvait partir pendant qu un pick etait deja en cours. Le composant bloque maintenant `expired` et `confirmPick` tant que l utilisateur n est pas le picker courant ou qu un submit est deja pending.
2. HIGH - `frontend/src/app/features/draft/services/draft.service.ts`
   - Le board snake ne remontait jamais `totalRounds` et calculait donc une progression fausse apres le premier round. Le contrat runtime recupere maintenant `draftInfo.totalRounds` via `GET /api/games/{id}/details` et l injecte dans le resume et la progression du board.
3. MEDIUM - `frontend/src/app/features/draft/services/draft.service.ts`
   - Le mapping du participant courant dependait d une egalite stricte sur le username entre `/participants` et `/details`, ce qui cassait le tour courant sur des jeux historiques avec casse differente. Le mapping est maintenant insensible a la casse et couvert par test.

### Validation

- `mvn "-Djacoco.skip=true" -q "-Dtest=GameDraftServiceTddTest,GameDetailServiceTest" test`
- `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/draft/services/draft.service.snake.spec.ts --include src/app/features/draft/components/snake-draft-page/snake-draft-page.component.spec.ts`
- `npx playwright test e2e/draft-flow.spec.ts`

### Result Summary

- Story `8.2` now satisfies its 5 acceptance criteria with runtime proof.
- Dedicated draft suite baseline remains `3 passed`, `0 failed`.
- Remaining intentional gap: fine WebSocket sync and future E2E `trade/swap`.
