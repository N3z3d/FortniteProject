# Story 8.4 Runtime Validation

Story: `8-4-realignement-dashboard-trades-front-runtime`
Date: `2026-03-09`
Status: `closed`

## Observation

- La surface frontend `trades/` etait encore partiellement legacy au debut de la story: routes detachees du contexte game, faux fallback mock et mapping d identite base uniquement sur des `userId` frontend locaux.
- Le realignement du dashboard a exige un contrat runtime plus complet cote front: `TradingService` doit combiner la surface legacy game-scoped avec les informations de draft trade reflotees par `details`, `participants` et `draft/audit`.
- La preuve E2E a revele deux causes reelles de non-rerunnabilite qui depassaient le seul dashboard:
  - un emitter SSE stale pouvait encore lever pendant un join rejoue et remonter en erreur metier
  - le cleanup trade oubliait les parties `E2E-FF-*` et `E2E-GL-*` creees ou jointes par `marcel`

## Validation

- Frontend unitaire cible:
  - `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/trades/services/trading.service.spec.ts --include src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts --include src/app/features/trades/trade-list/trade-list.component.spec.ts --include src/app/features/trades/components/trade-proposal/trade-proposal.component.spec.ts` -> `95 SUCCESS`
  - `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/trades/services/trading.service.spec.ts --include src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts --include src/app/features/trades/components/trade-details/trade-details.component.spec.ts` -> `90 SUCCESS`
- Backend cible:
  - `mvn -q -Dtest=GameRealtimeEventServiceTest test` -> `OK`
- Suites E2E ciblees:
  - `npx playwright test e2e/trade-dashboard.spec.ts e2e/trade-swap-flow.spec.ts` -> `7 passed`
  - `npx playwright test e2e/game-lifecycle.spec.ts` -> `5 passed`
  - `npx playwright test e2e/full-game-flow.spec.ts` -> `10 passed`
- Regression pack complet:
  - premier `npx playwright test` -> `46 passed`, `0 failed`
  - second `npx playwright test` sans reset manuel -> `46 passed`, `0 failed`
- Environnement local valide pendant la preuve:
  - `fortnite-app-local` : `healthy`
  - `fortnite-postgres-local` : `healthy`
  - `GET /actuator/health` : `UP`
  - `GET http://localhost:4200` : `200`

## BMAD Handling

- La story `8.4` peut passer en `review`: les 6 acceptance criteria sont couverts par code, tests et preuve runtime sur la vraie page `trades/`.
- Le runbook E2E local doit maintenant considerer `trade-dashboard.spec.ts` comme partie du pack critique rerunnable.
- Reliquats volontaires hors scope:
  - cas multi-trades plus complexes que le 1v1 standard
  - synchro realtime fine au niveau UI
  - rationalisation complete des derniers ecrans trades legacy si le module evolue encore
