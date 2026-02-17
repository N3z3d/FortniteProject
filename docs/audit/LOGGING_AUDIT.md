# Logging Audit - Baseline (JIRA-AUDIT-012)

Date: 2026-02-17
Owner: @Codex

## Scan Summary

Command baseline:
- `rg -n "console\\.(log|warn|error|info|debug)" frontend/src/app`
- `rg -n "System\\.out|printStackTrace\\(" src/main/java`

Results:
- Frontend direct `console.*`: 71 occurrences.
- Backend `System.out/printStackTrace`: 0 occurrences.
- Frontend `LoggerService` references: 158.
- Backend `@Slf4j/LoggerFactory` references: 95.

## Major Inconsistencies

Priority 1:
- `frontend/src/app/features/game/services/game-detail-actions.service.ts`
- `frontend/src/app/features/game/services/game-data.service.ts`

Priority 2:
- `frontend/src/app/core/services/websocket.service.ts`
- `frontend/src/app/features/game/game-detail/game-detail.component.ts`
- `frontend/src/app/features/game/mappers/game-api.mapper.ts`

Priority 3:
- Remaining components/services with isolated `console.*` calls.

## Decision

- Logging convention is now defined in `docs/architecture/ADR-005-logging-strategy.md`.
- First remediation lot targets Priority 1 files to align critical game flows.

## Execution Progress

Lot 1 completed:
- `frontend/src/app/features/game/services/game-detail-actions.service.ts`
- `frontend/src/app/features/game/services/game-data.service.ts`

Lot 2 completed:
- `frontend/src/app/core/services/websocket.service.ts`
- `frontend/src/app/features/game/game-detail/game-detail.component.ts`
- `frontend/src/app/features/game/mappers/game-api.mapper.ts`

Updated scan:
- Frontend direct `console.*`: 43 occurrences (from 71 baseline).

Lot 3 completed:
- `frontend/src/app/features/game/create-game/create-game.component.ts`
- `frontend/src/app/features/game/create-game/game-creation-wizard.component.ts`
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`

Updated scan:
- Frontend direct `console.*`: 40 occurrences (from 43 after lot 2).

Lot 4 completed:
- `frontend/src/app/features/trades/components/trade-proposal/trade-proposal.component.ts`
- `frontend/src/app/features/leaderboard/simple-leaderboard.component.ts`
- `frontend/src/app/features/leaderboard/mappers/leaderboard-api.mapper.ts`
- `frontend/src/app/features/dashboard/mappers/stats-api.mapper.ts`
- `frontend/src/app/core/services/theme.service.ts`
- `frontend/src/app/core/services/translation.service.ts`

Updated scan:
- Frontend direct `console.*`: 23 occurrences (from 40 after lot 3).
- Frontend production code `console.*` outside specs: 4 occurrences, all centralized in `LoggerService`.

Lot 5 completed:
- `frontend/src/app/features/dashboard/services/dashboard-data.service.spec.ts`
- `frontend/src/app/features/dashboard/services/dashboard-data-integration.spec.ts`
- Removed console traces in dashboard tests and injected `LoggerService` spies for clean output.

Updated scan:
- Frontend direct `console.*`: 5 occurrences.
- Remaining occurrences are expected and centralized in:
  - `frontend/src/app/core/services/logger.service.ts` (4)
  - `frontend/src/app/core/services/logger.service.spec.ts` (1 assertion)

Lot 6 completed:
- Backend critical game endpoints aligned to ADR message format/context in:
  - `src/main/java/com/fortnite/pronos/controller/GameController.java`
- Added explicit success/failure/unauthorized/forbidden logs on:
  - create/join/join-with-code/leave/start-draft/delete/regenerate-code/rename.
- Validation executed:
  - `mvn "-Dtest=GameControllerSimpleTest" test`
  - `mvn -DskipTests compile`

Lot 7 completed:
- Backend critical trade endpoints aligned to ADR message format/context in:
  - `src/main/java/com/fortnite/pronos/controller/TradeController.java`
- Added explicit request/success logs on:
  - propose/accept/reject/cancel/counter/history/pending/statistics/get-trade/get-game-trades.
- Validation executed:
  - `mvn "-Dtest=TradeControllerTest" test`
  - `mvn -DskipTests compile`

Lot 8 completed:
- Backend critical draft endpoints aligned to ADR message format/context in:
  - `src/main/java/com/fortnite/pronos/controller/DraftController.java`
- Added explicit request/success/warn/failure logs on:
  - getDraftInfo/nextPick/selectPlayer/nextParticipant/order/isTurn/next/complete/available-players/handle-timeouts/finish
- Added guard for missing select-player request body:
  - `@RequestBody(required = false)` + controlled `400 Request body is required`
- Validation executed:
  - `mvn "-Dtest=DraftWorkflowIntegrationTest" test`
  - `mvn "-Dtest=GameControllerSimpleTest,TradeControllerTest,DraftWorkflowIntegrationTest" test`
  - `mvn -DskipTests compile`
