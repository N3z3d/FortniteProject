# Story 8.3: Couverture E2E locale trade/swap

Status: done

<!-- METADATA
  story_key: 8-3-couverture-e2e-locale-trade-swap
  branch: story/8-3-couverture-e2e-locale-trade-swap
  sprint: Sprint 4
  Note: Story locale de hardening produit. Objectif: couvrir en E2E les flows trade/swap reels, en realignant au besoin la surface front sur les endpoints runtime existants.
-->

## Story

As a developer validating roster mutations locally,
I want deterministic Playwright coverage for swap solo and mutual trade flows on the real runtime contract,
so that the last major local gameplay area without E2E safety net is covered before any infra/staging work.

## Acceptance Criteria

1. Une suite Playwright dediee `frontend/e2e/trade-swap-flow.spec.ts` est active et n est ni `fixme` ni `skip`.
2. La suite couvre au minimum un flow `swap-solo` heureux de bout en bout, avec validation de la mutation roster et de la trace d audit.
3. La suite couvre au minimum un flow `trade` 1v1 propose puis accepte, avec validation de l echange persiste et de la trace d audit.
4. La suite couvre au minimum un flow `trade` 1v1 propose puis rejete, avec validation d absence de mutation roster et statut terminal explicite.
5. Les helpers E2E permettent de preparer un draft local rerunnable pour ces scenarios sans reset manuel de base.
6. La story documente explicitement le reliquat hors scope apres la couverture trade/swap (ex: dashboard trades legacy encore non aligne, cas multi-trades plus complexes, realtime WebSocket).

## Tasks / Subtasks

- [x] Task 1 - Synchroniser le board BMAD et figer le scope E2E trade/swap (AC: #1, #5, #6)
  - [x] 1.1 Enregistrer la story 8.3 dans l epic 8 et le sprint board
  - [x] 1.2 Capturer les ecarts reels entre la surface front trades actuelle et les endpoints runtime draft trade/swap

- [x] Task 2 - Preparer un helper local rerunnable pour un draft trade-ready (AC: #5)
  - [x] 2.1 Etendre les helpers Playwright pour creer une game, joindre un second participant, demarrer le draft et garantir un roster initial stable
  - [x] 2.2 S assurer que le cleanup n exige pas de reset manuel de la base

- [x] Task 3 - Activer la couverture E2E swap-solo (AC: #1, #2)
  - [x] 3.1 Ajouter un scenario happy path `swap-solo`
  - [x] 3.2 Ajouter au moins un scenario invalide avec rejection explicite
  - [x] 3.3 Verifier le reflet du swap dans l audit draft

- [x] Task 4 - Activer la couverture E2E trade mutuel 1v1 (AC: #1, #3, #4)
  - [x] 4.1 Ajouter un scenario `propose -> accept`
  - [x] 4.2 Ajouter un scenario `propose -> reject`
  - [x] 4.3 Verifier le reflet des statuts et des mutations/non-mutations cote audit

- [x] Task 5 - Verification finale et documentation (AC: #1, #2, #3, #4, #5, #6)
  - [x] 5.1 Lancer `npx playwright test e2e/trade-swap-flow.spec.ts`
  - [x] 5.2 Mettre a jour le runbook E2E local avec la commande et le reliquat restant
  - [x] 5.3 Mettre a jour les notes de completion avec les zones encore hors scope

## Dev Notes

### Current Measured Baseline

- A ce stade, la couverture Playwright trade/swap est absente.
- `docs/testing/E2E_LOCAL_RUNBOOK.md` liste explicitement trade/swap comme gap restant.
- Les stories backend/runtime de reference sont deja fermees:
  - `5-1-swap-solo-vers-joueur-libre-valide`
  - `5-2-trade-mutuel-1v1-avec-acceptation-adverse`
  - `5-3-journal-d-audit-trades-swaps`

### Scope Guardrails

- Cette story ne rouvre pas l hebergement/staging ni Railway.
- Cette story ne rouvre pas le draft serpent profond, deja couvert par `8.2`.
- Cette story cible la couverture E2E locale des flows trade/swap reels seulement.

### Technical Requirements

- Les endpoints runtime backend deja disponibles sont:
  - `POST /api/games/{gameId}/draft/swap-solo`
  - `POST /api/games/{gameId}/draft/trade`
  - `POST /api/games/{gameId}/draft/trade/{tradeId}/accept`
  - `POST /api/games/{gameId}/draft/trade/{tradeId}/reject`
  - `GET /api/games/{gameId}/draft/audit`
- Les profils seedes prioritaires restent `thibaut` et `teddy`.
- La surface front trades semble encore partiellement legacy (`frontend/src/app/features/trades/services/trading.service.ts`) et devra etre confirmee ou minimalement realignee sur les endpoints runtime ci-dessus.

### Testing Requirements

- La story n est pas terminee tant qu une suite Playwright dediee trade/swap ne passe pas activement.
- Toute realignement de contrat frontend devra avoir au moins un test unitaire/service adapte.
- Les validations E2E doivent prouver la persistance via UI et/ou audit runtime, pas seulement un `200`.

### Pre-existing Gaps / Known Issues

- [KNOWN] Le module frontend `trades/` expose encore des routes et services pouvant diverger du contrat runtime draft trade/swap.
- [KNOWN] La synchro realtime WebSocket trades n est pas couverte par cette story.
- [KNOWN] Les cas complexes hors 1v1 standard ne sont pas inclus dans ce lot.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/5-1-swap-solo-vers-joueur-libre-valide.md`
- `_bmad-output/implementation-artifacts/5-2-trade-mutuel-1v1-avec-acceptation-adverse.md`
- `_bmad-output/implementation-artifacts/5-3-journal-d-audit-trades-swaps.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/src/app/features/trades/services/trading.service.ts`
- `frontend/src/app/features/draft/services/draft-audit.service.ts`
- `src/main/java/com/fortnite/pronos/controller/SwapSoloController.java`
- `src/main/java/com/fortnite/pronos/controller/DraftParticipantTradeController.java`
- `src/main/java/com/fortnite/pronos/controller/DraftAuditController.java`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story opened from epic 8 after code-review closure of 8.2 on 2026-03-08
- Runtime validation captured in `_bmad-output/implementation-artifacts/8-3-couverture-e2e-locale-trade-swap-runtime-validation-2026-03-08.md`

### Implementation Plan

- Preparer un helper Playwright `trade-ready` base sur le contrat runtime reel plutot que sur le dashboard legacy `trades/`.
- Garantir la rerunnabilite locale avec un cleanup automatique des fixtures `E2E-TS-*` en base Docker locale.
- Corriger tout ecart runtime qui empeche la preuve de persistance roster/audit.
- Valider ensuite la suite dediee `trade-swap-flow.spec.ts`, puis rerunner la suite Playwright complete.

### Completion Notes List

- `frontend/e2e/trade-swap-flow.spec.ts` est maintenant active et couvre:
  - `swap-solo` invalide avec rejet explicite `400 INVALID_SWAP`
  - `swap-solo` heureux avec mutation roster persistante et audit
  - `trade` 1v1 `propose -> accept` avec echange roster persistant et audit
  - `trade` 1v1 `propose -> reject` avec roster intact et audit terminal
- Le helper `frontend/e2e/helpers/trade-swap-helpers.ts` cree une partie `trade-ready` via le runtime reel (`create -> regenerate code -> join -> start draft -> seed picks`) sans passer par le module frontend `trades/` encore legacy.
- La rerunnabilite locale de `8.3` ne depend plus d un reset manuel de base: les fixtures `E2E-TS-*` sont soft-deletees automatiquement dans PostgreSQL Docker avant et apres la suite.
- La story a revele un ecart runtime reel cote backend: `GET /api/games/{id}/details` ne reflechait pas les mutations roster issues de `swap/trade` parce qu il lisait encore prioritairement `GameParticipant.selectedPlayerIds`.
- `GameDetailService` prend maintenant les `DraftPick` comme source de verite quand un draft existe, ce qui remet `selectedPlayers`, `totalPlayers` et la distribution par region en coherence apres `swap/trade`.
- Le test `GameDetailServiceTest` verrouille explicitement ce contrat avec un roster participant stale et des `DraftPick` mis a jour.
- Le helper partage `joinWithInvitationCode()` est aussi devenu rerunnable sur le flow reel en soumettant le formulaire via clavier et en acceptant la redirection vers `/games/{id}/dashboard`, ce qui a ferme le reliquat `FULL-FLOW-05`.
- Validation finale reussie:
  - `mvn -q -Dtest=GameDetailServiceTest test`
  - `npx playwright test e2e/trade-swap-flow.spec.ts`
  - `npx playwright test e2e/full-game-flow.spec.ts`
  - `npx playwright test` -> `43 passed`, `0 failed`
- Reliquats volontaires hors scope apres 8.3:
  - le module frontend `trades/` reste partiellement legacy et non encore realigne sur les endpoints draft trade/swap
  - les cas multi-trades plus complexes que le 1v1 standard ne sont pas couverts
  - la synchro WebSocket realtime des trades/drafts reste hors preuve dans ce lot

### File List

- `_bmad-output/implementation-artifacts/8-3-couverture-e2e-locale-trade-swap.md`
- `_bmad-output/implementation-artifacts/8-3-couverture-e2e-locale-trade-swap-runtime-validation-2026-03-08.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/planning-artifacts/epics.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/trade-swap-flow.spec.ts`
- `frontend/e2e/helpers/trade-swap-helpers.ts`
- `frontend/e2e/helpers/local-db-helpers.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `src/main/java/com/fortnite/pronos/service/GameDetailService.java`
- `src/main/java/com/fortnite/pronos/service/GameRealtimeEventService.java`
- `src/test/java/com/fortnite/pronos/service/GameDetailServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/GameRealtimeEventServiceTest.java`

### Change Log

- 2026-03-08 - Story created to cover local trade/swap E2E flows and realign the minimal front/runtime contract if needed.
- 2026-03-08 - Runtime trade/swap suite activated, local rerun helper added, shared join-code helper hardened, backend game detail contract fixed for post swap/trade roster reflection, and story moved to review.
- 2026-03-09 - Senior review fixes hardened join-code failure reporting, enforced draft fixture fail-fast semantics, generalized local DB cleanup fallback for rerunnable suites, fixed stale SSE emitter handling on join-by-code, and approved the story.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-09

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. HIGH - `frontend/e2e/helpers/app-helpers.ts`
   - `joinWithInvitationCode()` pouvait retourner un faux succes quand la navigation n avait pas eu lieu mais qu aucun message d erreur explicite n etait rendu. Le helper echoue maintenant explicitement dans ce cas, ce qui supprime un faux vert silencieux.
2. MEDIUM - `frontend/e2e/helpers/trade-swap-helpers.ts`
   - `prepareTradeReadyGame()` retournait `draftId` vide quand le draft n etait pas encore visible dans `GET /details`, ce qui masquait un fixture incomplet. Le helper fail-fast maintenant si le draft n est pas materialise.
3. HIGH - `frontend/e2e/helpers/local-db-helpers.ts`, `frontend/e2e/helpers/app-helpers.ts`
   - Le cleanup generique ne savait pas fermer les parties devenues non supprimables via l API, ce qui cassait les reruns complets sur quota de parties actives. Un fallback de soft-delete PostgreSQL local est maintenant partage et reutilise par les suites legacy.
4. HIGH - `src/main/java/com/fortnite/pronos/service/GameRealtimeEventService.java`
   - Un emitter SSE stale pouvait lever `IllegalStateException` pendant `join-with-code`, remonter en `409 DRAFT_WINDOW_VIOLATION` et faire tomber la suite `trade/swap`. Le service purge maintenant aussi ces emitters morts, avec garde-fou dans `GameRealtimeEventServiceTest`.

### Validation

- `mvn -q -Dtest=GameRealtimeEventServiceTest test`
- `npx playwright test e2e/game-lifecycle.spec.ts`
- `npx playwright test e2e/trade-swap-flow.spec.ts`
- `npx playwright test`

### Result Summary

- Full suite Playwright revalidee: `43 passed`, `0 failed`
- Les flows `trade/swap`, `draft`, `full-game-flow` et `game-lifecycle` sont rerunnables sur le runtime local actuel
- Reliquats intentionnels inchanges: module frontend `trades/` legacy, cas multi-trades plus complexes, synchro realtime WebSocket fine hors preuve de ce lot
