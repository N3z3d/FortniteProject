# Story 8.3 Runtime Validation

Story: `8-3-couverture-e2e-locale-trade-swap`
Date: `2026-03-08`
Status: `closed`

## Observation

- La couverture Playwright `trade/swap` etait absente au debut de la story.
- Le module frontend `trades/` restait encore partiellement legacy et ne pouvait pas servir de base fiable pour la preuve locale.
- Le runtime a revele un ecart backend reel: `GET /api/games/{id}/details` ne refletait pas les mutations roster produites par `swap-solo` et `trade`, alors que l audit et les endpoints mutation repondaient correctement.
- La rerunnabilite locale exigeait aussi un cleanup automatise des fixtures `E2E-TS-*`, car les parties demarrees ne peuvent pas etre supprimees par le flow applicatif standard.

## Validation

- Backend cible:
  - `mvn -q -Dtest=GameDetailServiceTest test` -> `OK`
  - Le test de service verrouille que `GameDetailService` lit les `DraftPick` comme source de verite lorsqu un draft existe.
- Suite E2E dediee:
  - `npx playwright test e2e/trade-swap-flow.spec.ts` -> `4 passed`
  - `TS-01`: `swap-solo` invalide retourne `400 INVALID_SWAP` sans mutation roster
  - `TS-02`: `swap-solo` valide persiste le roster et apparait dans l audit
  - `TS-03`: `trade` `propose -> accept` echange effectivement les rosters et apparait dans l audit
  - `TS-04`: `trade` `propose -> reject` laisse les rosters intacts et apparait dans l audit
- Regression pack impacte:
  - `npx playwright test e2e/full-game-flow.spec.ts` -> `10 passed`
  - `npx playwright test` -> `43 passed`, `0 failed`
- Environnement local valide pendant la preuve:
  - `fortnite-app-local` : `healthy`
  - `fortnite-postgres-local` : `healthy`
  - `GET /actuator/health` : `UP`

## BMAD Handling

- La story `8.3` peut passer en `review`: les 6 acceptance criteria sont couverts par code, helper rerunnable, suite Playwright active et preuve runtime.
- Le cleanup automatise des fixtures `E2E-TS-*` en PostgreSQL Docker supprime le besoin de reset manuel de base pour ce lot.
- Reliquats volontaires hors scope:
  - dashboard frontend `trades/` toujours legacy
  - cas multi-trades non standards non couverts
  - synchro WebSocket realtime des trades/drafts non couverte ici
