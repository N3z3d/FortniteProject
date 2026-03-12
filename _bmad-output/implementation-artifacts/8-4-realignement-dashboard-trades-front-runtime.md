# Story 8.4: Realignement dashboard trades front/runtime

Status: done

<!-- METADATA
  story_key: 8-4-realignement-dashboard-trades-front-runtime
  branch: story/8-4-realignement-dashboard-trades-front-runtime
  sprint: Sprint 4
  Note: Story locale de hardening produit. Objectif: remettre la surface frontend `trades/` en coherence avec le runtime reel, supprimer les faux fallback et rendre l UI fiable pour les validations locales.
-->

## Story

As a local user validating trades from the actual UI,
I want the `trades/` dashboard and related routes to consume the real game-scoped runtime contract without mock fallback,
so that the trade UI itself becomes a trustworthy prod-like surface for local testing.

## Acceptance Criteria

1. La surface principale `/games/:id/trades` charge ses donnees via le contrat runtime reel de trade de la game courante (`/api/trades/game/{gameId}`, `/api/trades/game/{gameId}/statistics`) et non via une surface legacy implicite ou detachee du contexte game.
2. Aucun ecran du module `trades/` ne retombe silencieusement sur des donnees mockees si l API echoue; l utilisateur voit un feedback d erreur explicite.
3. La navigation principale du module trades reste coherente avec le contexte game courant; les routes legacy non coherentes sont soit redirigees vers la surface valide, soit documentees comme hors scope remplace.
4. Une couverture Playwright active prouve que la page trades UI affiche bien un trade reel et son statut terminal apres un flow runtime valide.
5. Les changements de service/composant/routes sont couverts par des tests unitaires cibles sur le contexte game, la gestion d erreur et l absence de mock fallback.
6. Les reliquats hors scope apres cette story sont documentes explicitement (multi-trade complexe, synchro realtime fine, eventuelle rationalisation plus large du module trades).

## Tasks / Subtasks

- [x] Task 1 - Synchroniser le board BMAD et figer le scope 8.4 (AC: #1, #3, #6)
  - [x] 1.1 Enregistrer la story 8.4 dans l epic 8 et le sprint board
  - [x] 1.2 Capturer les divergences reelles du module `trades/` par rapport au runtime valide par 5.x et 8.3

- [x] Task 2 - Realigner le service et les routes principales `trades/` sur le contexte game reel (AC: #1, #3)
  - [x] 2.1 Confirmer la surface UI principale a conserver (`/games/:id/trades`)
  - [x] 2.2 Adapter `TradingService` et les composants prioritaires pour charger les trades/stats de la game courante sans ambiguite de contexte
  - [x] 2.3 Rediriger ou borner les routes legacy detachees si elles ne sont plus une surface valide

- [x] Task 3 - Supprimer les faux signaux legacy cote UI (AC: #2, #5)
  - [x] 3.1 Retirer les fallback mock qui masquent les erreurs API reelles
  - [x] 3.2 Afficher un feedback d erreur explicite et testable
  - [x] 3.3 Ajouter ou mettre a jour les tests unitaires de service/composant necessaires

- [x] Task 4 - Reconnecter la preuve E2E a la vraie page trades (AC: #4, #5)
  - [x] 4.1 Ajouter ou etendre une suite Playwright pour visiter la page trades apres un flow trade reel
  - [x] 4.2 Verifier que le statut visible cote UI correspond bien au statut runtime persiste
  - [x] 4.3 Garantir la rerunnabilite locale sans reset manuel

- [x] Task 5 - Verification finale et documentation (AC: #1, #2, #3, #4, #5, #6)
  - [x] 5.1 Lancer les tests unitaires cibles frontend/backend si contrat touche
  - [x] 5.2 Lancer la suite Playwright ciblee sur la page trades
  - [x] 5.3 Mettre a jour le runbook E2E local et documenter le reliquat reel

## Dev Notes

### Current Measured Baseline

- Le runbook local liste encore le dashboard frontend `trades/` comme surface partiellement legacy et non source de verite.
- `TradingService` cible bien `/api/trades`, mais le module garde des usages legacy, des routes detachees et des hypothese UI historiques.
- `TradeListComponent` retombe encore sur des donnees mockees si l API echoue, ce qui masque les bugs runtime.
- `TradingDashboardComponent` charge deja la game par route parente, mais le module dans son ensemble n est pas encore clairement borne autour de cette surface.

### Scope Guardrails

- Cette story ne rouvre pas l hebergement, Railway ni le staging.
- Cette story ne rouvre pas les flows metier trade/swap backend deja prouves par 5.x et 8.3 sauf si un bug UI revele un vrai ecart de contrat.
- Cette story ne couvre pas encore les cas multi-trades complexes ni une refonte complete du realtime.

### Technical Requirements

- Les endpoints backend de reference deja disponibles sont:
  - `GET /api/trades/game/{gameId}`
  - `GET /api/trades/game/{gameId}/statistics`
  - `GET /api/trades/{tradeId}`
  - `PUT /api/trades/{tradeId}/accept`
  - `PUT /api/trades/{tradeId}/reject`
  - `PUT /api/trades/{tradeId}/cancel`
- Les preuves runtime deja existantes restent:
  - `5-2-trade-mutuel-1v1-avec-acceptation-adverse`
  - `5-3-journal-d-audit-trades-swaps`
  - `8-3-couverture-e2e-locale-trade-swap`
- Les fichiers front a auditer en priorite sont:
  - `frontend/src/app/features/trades/services/trading.service.ts`
  - `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`
  - `frontend/src/app/features/trades/trade-list/trade-list.component.ts`
  - `frontend/src/app/features/trades/trades-routing.module.ts`

### Testing Requirements

- Toute suppression de fallback mock doit etre couverte par un test unitaire explicite.
- Toute redirection ou rationalisation de routes doit etre couverte par au moins un test de composant ou de routing adapte.
- La story n est pas terminee tant qu une preuve Playwright passe par la vraie page `trades/`.

### Pre-existing Gaps / Known Issues

- [KNOWN] Le module `trades/` melange encore surface dashboard runtime et ecrans legacy plus anciens.
- [KNOWN] La synchro realtime WebSocket des trades peut rester partielle tant que la page reste correcte apres refresh.
- [KNOWN] Les cas de multi-trade et de contre-propositions avancees ne sont pas cibles dans ce lot.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/5-2-trade-mutuel-1v1-avec-acceptation-adverse.md`
- `_bmad-output/implementation-artifacts/5-3-journal-d-audit-trades-swaps.md`
- `_bmad-output/implementation-artifacts/8-3-couverture-e2e-locale-trade-swap.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/src/app/features/trades/services/trading.service.ts`
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`
- `frontend/src/app/features/trades/trade-list/trade-list.component.ts`
- `frontend/src/app/features/trades/trades-routing.module.ts`
- `src/main/java/com/fortnite/pronos/controller/TradeController.java`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story opened on 2026-03-09 after closure of 8.3 and review of the remaining local gap listed in `docs/testing/E2E_LOCAL_RUNBOOK.md`
- Runtime validation captured in `_bmad-output/implementation-artifacts/8-4-realignement-dashboard-trades-front-runtime-validation-2026-03-09.md`

### Implementation Plan

- Identifier la surface trades UI encore utile et supprimer les faux chemins qui brouillent le contexte game.
- Realigner le service et les composants prioritaires sur le contrat runtime reel des trades de game.
- Enlever les fallback mock silencieux qui rendent l app artificiellement saine en local.
- Ajouter une preuve E2E qui passe par la vraie page trades UI au lieu des seuls helpers runtime.

### Completion Notes List

- `TradingService` consomme maintenant le contexte runtime reel de la game courante et reconstruit aussi les trades issus du draft via `details`, `participants` et `draft/audit`, ce qui remet le dashboard `trades/` en coherence avec les preuves 5.x et 8.3.
- `TradingDashboardComponent` et `TradeDetailsComponent` ne dependent plus uniquement des `userId` frontend locaux; ils reconnaissent aussi l utilisateur courant par username, ce qui aligne enfin les profils seedes front (`1/2/3/4`) avec les UUID backend reels.
- `TradeListComponent` ne masque plus les erreurs API par un fallback mock silencieux; le module expose un feedback d erreur explicite et les routes legacy sont bornees autour de `/games/:id/trades`.
- `frontend/e2e/trade-dashboard.spec.ts` prouve maintenant la vraie page UI `trades/` sur des trades `ACCEPTED` et `REJECTED`, en complement de `trade-swap-flow.spec.ts`.
- La story a revele deux causes reelles de non-rerunnabilite hors dashboard pur:
  - un emitter SSE stale pouvait encore lever pendant les joins rejoues; `GameRealtimeEventService` ignore maintenant aussi les echecs de `complete()` et purge correctement ces emitters morts
  - le cleanup des fixtures trades oubliait les parties creees par `marcel` dans `FULL-FLOW` et `GAME-LIFECYCLE`; `cleanupTradeFixtureUsers()` couvre maintenant `admin`, `thibaut`, `teddy` et `marcel`
- Validation finale reussie:
  - `mvn -q -Dtest=GameRealtimeEventServiceTest test`
  - `npx playwright test e2e/trade-dashboard.spec.ts e2e/trade-swap-flow.spec.ts`
  - `npx playwright test e2e/game-lifecycle.spec.ts`
  - `npx playwright test` -> `46 passed`, `0 failed`
  - second rerun `npx playwright test` -> `46 passed`, `0 failed`
- La review 8.4 a ferme deux ecarts restants:
  - le dashboard recharge maintenant ses donnees quand le `gameId` de route change, au lieu de rester fige sur le premier contexte charge
  - `TradingService` ne convertit plus une panne du endpoint principal trades/statistics en etat vide silencieux; l erreur remonte bien au composant et aux feedbacks UI
- Reliquats volontaires hors scope apres 8.4:
  - cas multi-trades plus complexes que le 1v1 standard
  - synchro realtime fine au niveau UI tant que le refresh/runtime reste la source de verite
  - rationalisation plus large du module `trades/` si l on veut supprimer totalement les surfaces legacy restantes

### File List

- `_bmad-output/implementation-artifacts/8-4-realignement-dashboard-trades-front-runtime.md`
- `_bmad-output/implementation-artifacts/8-4-realignement-dashboard-trades-front-runtime-validation-2026-03-09.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/planning-artifacts/epics.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/trade-dashboard.spec.ts`
- `frontend/e2e/trade-swap-flow.spec.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/e2e/helpers/trade-swap-helpers.ts`
- `frontend/src/app/features/trades/services/trading.service.ts`
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.html`
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts`
- `frontend/src/app/features/trades/components/trade-details/trade-details.component.ts`
- `frontend/src/app/features/trades/components/trade-details/trade-details.component.spec.ts`
- `frontend/src/app/features/trades/components/trade-proposal/trade-proposal.component.ts`
- `frontend/src/app/features/trades/components/trade-proposal/trade-proposal.component.spec.ts`
- `frontend/src/app/features/trades/trade-list/trade-list.component.ts`
- `frontend/src/app/features/trades/trade-list/trade-list.component.html`
- `frontend/src/app/features/trades/trade-list/trade-list.component.spec.ts`
- `frontend/src/app/features/trades/trades-routing.module.ts`
- `src/main/java/com/fortnite/pronos/service/GameDetailService.java`
- `src/main/java/com/fortnite/pronos/service/GameRealtimeEventService.java`
- `src/test/java/com/fortnite/pronos/service/GameDetailServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/GameRealtimeEventServiceTest.java`

### Change Log

- 2026-03-09 - Story created to realign the frontend trades dashboard on the real runtime contract and remove legacy mock fallback behavior.
- 2026-03-09 - Runtime trades dashboard realigned on game-scoped data, explicit error handling enforced, dedicated `trade-dashboard` Playwright proof added, rerun blockers on stale SSE emitters and incomplete fixture cleanup removed, and story moved to review.
- 2026-03-09 - Senior review fixes restored route-param driven reloads, removed silent primary-endpoint error swallowing in `TradingService`, and approved the story.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-09

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. HIGH - `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`
   - Le composant ne reactualisait plus le contexte trade si l utilisateur changeait de partie sans recrer la vue. La souscription au contexte route recharge maintenant les donnees quand `gameId` change.
2. HIGH - `frontend/src/app/features/trades/services/trading.service.ts`
   - `getTrades()` et `getTradingStats()` convertissaient une panne du endpoint principal en etat vide/zero silencieux, ce qui remettait un faux vert de type “aucune donnee”. Les erreurs remontent maintenant au composant et a `errorSubject`.

### Validation

- `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/trades/services/trading.service.spec.ts --include src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts`
- `npx playwright test e2e/trade-dashboard.spec.ts`
- `npx playwright test`

### Result Summary

- Story `8.4` satisfait ses 6 acceptance criteria avec preuve runtime et sans faux vide silencieux sur le endpoint principal.
- Le dashboard `trades/` reste rerunnable sur le runtime local actuel.
- Le prochain reliquat produit local concerne surtout `multi-trade/realtime`, pas l infra.
