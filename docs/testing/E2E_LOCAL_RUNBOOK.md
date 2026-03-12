# E2E Local Runbook

## Goal

Keep a deterministic local regression pack that approximates production behavior without requiring public staging.

## Preconditions

- Frontend available on `http://localhost:4200`
- Backend available on `http://localhost:8080`
- Seed data loaded with local users `admin`, `thibaut`, `marcel`, `teddy`
- Docker/local backend healthy before running Playwright

## Critical Pack

The critical rerunnable pack is the full default Playwright command:

```bash
npx playwright test
```

Covered suites:

- `frontend/e2e/smoke.spec.ts`
- `frontend/e2e/auth.spec.ts`
- `frontend/e2e/catalogue.spec.ts`
- `frontend/e2e/game-lifecycle.spec.ts`
- `frontend/e2e/full-game-flow.spec.ts`
- `frontend/e2e/admin.spec.ts`
- `frontend/e2e/draft-flow.spec.ts`
- `frontend/e2e/trade-dashboard.spec.ts`
- `frontend/e2e/trade-swap-flow.spec.ts`

Contract notes:

- `full-game-flow.spec.ts` stops at `create -> generate code -> join -> detail visibility`
- Invitation-code tests must wait for backend persistence before the join step
- E2E helpers must keep suite-specific prefixes such as `E2E-GL-*`, `E2E-FF-*` and `E2E-TS-*`
- Trade-related cleanup must cover local seeded users `admin`, `thibaut`, `marcel`, `teddy`

## Dedicated Draft Suite

Deep draft coverage is isolated in:

```bash
npx playwright test e2e/draft-flow.spec.ts
```

Current status:

- `DRAFT-01` to `DRAFT-03` are active and passing
- Scope:
  - start draft from game detail
  - current picker selects a player on the snake board
  - reload reflects the persisted pick on the board
- Remaining known gap:
  - fine-grained WebSocket sync is still out of scope; the HTTP/runtime contract is the source of truth

## Dedicated Trade/Swap Suite

Trade and swap coverage is isolated in:

```bash
npx playwright test e2e/trade-swap-flow.spec.ts
```

Current status:

- `TS-01` to `TS-04` are active and passing
- Scope:
  - invalid `swap-solo` rejection
  - valid `swap-solo` with persisted roster mutation and audit
  - `trade` `propose -> accept`
  - `trade` `propose -> reject`
- Local rerunability:
  - fixtures `E2E-TS-*` are soft-deleted automatically in Docker PostgreSQL before and after the suite
  - related leftovers from `E2E-FF-*` and `E2E-GL-*` are also cleaned for the seeded trade users before fixture preparation

## Dedicated Trade Dashboard Suite

Trade dashboard UI coverage is isolated in:

```bash
npx playwright test e2e/trade-dashboard.spec.ts
```

Current status:

- `TD-01` to `TD-03` are active and passing
- Scope:
  - legacy route redirection to `/games/:id/trades`
  - accepted trade visible on the real dashboard UI
  - rejected trade visible on the real dashboard UI
- Contract:
  - the page consumes the real game-scoped trade surface and no longer depends on mock fallback as a proof source

## Current Baseline

Validated on 2026-03-11:

- `npx playwright test e2e/admin.spec.ts` -> `9 passed`, `0 failed`
- `npx playwright test e2e/trade-dashboard.spec.ts e2e/trade-swap-flow.spec.ts` -> `7 passed`, `0 failed`
- `npx playwright test e2e/game-lifecycle.spec.ts` -> `5 passed`, `0 failed`
- `npx playwright test e2e/full-game-flow.spec.ts` -> `10 passed`, `0 failed`
- `npx playwright test` -> `54 passed`, `0 failed`
- second rerun without manual DB reset -> `54 passed`, `0 failed`

## Skip Policy

- Critical-pack skips are not accepted unless explicitly documented here
- Dedicated-suite skips are accepted only when the blocking contract is identified and a follow-up story is recommended
- Silent flaky retries are not a substitute for documenting the blocker

## Remaining Coverage Gaps

- Fine-grained realtime sync remains out of scope as long as refresh/runtime stays correct
- Profile/settings and some non-critical navigations are not part of the critical pack

## Recommended Next Story

Epic 8 local E2E hardening is now green on the current pack.

Recommended next BMAD move:

- switch back to the active Sprint 7 backlog (`sprint7-f2-ci-vitest-et-docker-push`, then `sprint7-z1-fix-zonejs-debounce-failures`)
- open a new local E2E story only if a new product gap is discovered outside the current `54/54` pack
