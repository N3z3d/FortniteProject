## Runtime Validation - Story 8.5

Date: 2026-03-10
Story: `8-5-couverture-e2e-multi-trade-realtime`
Scope: validation locale Docker du module `trades/` sur scenarios multi-trade et refresh/realtime HTTP.

### Runtime State

- `http://localhost:8080/actuator/health` -> `200 UP`
- `http://localhost:4200` -> `200`

### Executed Validation

- `npx playwright test e2e/trade-dashboard.spec.ts` -> `3 passed`
- `npx playwright test e2e/trade-multi-flow.spec.ts` -> `3 passed`

### Real Fixes Validated

- Le publish realtime SSE ne casse plus `joinGame` quand un emitter stale echoue.
- Les suites `trade-dashboard` et `trade-multi-flow` sont rerunnables sans reset manuel.
- Le helper de login seed local reutilise maintenant une session JWT deja obtenue pour eviter les faux negatifs lies au rate limit backend pendant les suites trade.

### Out of Scope / Remaining Gap

- Le pack Playwright complet n est pas encore totalement vert.
- Au rerun complet du 2026-03-10, `46 passed`, `7 failed`, `1 did not run`.
- Les echecs restants sont principalement sur les helpers d auth legacy encore bases sur le clic profil/UI plutot que sur la session seedee partagee.
- Ce reliquat doit etre traite dans une story dediee de stabilisation auth E2E, pas dans `8.5`.
