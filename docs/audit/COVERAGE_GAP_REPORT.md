# Coverage Gap Report

- Date: 2026-02-15 22:22-22:20 (local)
- Ticket: `JIRA-AUDIT-006`

## Commands
- `mvn test -q` (failed: 1 suite, 4 tests)
- `npm --prefix frontend run test:ci -- --code-coverage` (failed: 21 specs)
- Coverage artifacts parsed: `target/site/jacoco/jacoco.xml`, `frontend/coverage/frontend/**`

## Global Coverage (Lines)
- Backend (JaCoCo): **70.67%** (5554/7859)
- Frontend (Karma HTML root): **80.08%**

## Backend Critical Classes < 60% Lines
| Class | Lines % | Lines Covered/Total |
|---|---:|---:|

## Frontend Critical Files < 60% Lines
| File | Lines % |
|---|---:|
| `app/features/trades/components/trade-proposal/trade-proposal.component.ts` | 30.1% |
| `app/shared/services/browser-navigation.service.ts` | 33.3% |
| `app/features/game/services/game-data.service.ts` | 34.2% |
| `app/features/dashboard/dashboard.component.ts` | 34.9% |
| `app/core/services/user-games.store.ts` | 36.2% |
| `app/features/teams/teams-routing.module.ts` | 40.0% |
| `app/features/dashboard/services/dashboard-chart.service.ts` | 41.0% |
| `app/core/services/websocket.service.ts` | 42.5% |
| `app/shared/components/main-layout/main-layout.component.ts` | 51.6% |
| `app/core/repositories/leaderboard.repository.ts` | 52.1% |
| `app/features/teams/team-list/team-list.ts` | 53.3% |
| `app/features/trades/services/trading.service.ts` | 55.5% |
| `app/features/leaderboard/simple-leaderboard.component.ts` | 58.7% |

## Test Suite Failures Blocking Reliable Baseline
### Backend
- Suite: `GameWorkflowIntegrationTest` (4 failures)
  - `shouldMaintainDataConsistencyDuringMultipleOperations` -> `Status expected:<200> but was:<409>`
  - `shouldStartDraftWithEnoughParticipants` -> `Status expected:<200> but was:<409>`
  - `shouldAllowJoiningExistingGame` -> `Status expected:<200> but was:<409>`
  - `shouldPreventJoiningFullGame` -> `Status expected:<200> but was:<409>`

### Frontend
- Karma failed specs: **21**
  - DashboardDataService - Integration TDD Tests de performance et log analysis devrait logger les performances et identifier les goulots d'tranglement
  - DashboardDataService - Integration TDD Validation de l'intgration complte devrait retourner des donnes cohrentes entre les diffrents endpoints
  - DashboardDataService - Integration TDD Validation des donnes relles devrait grer le cas o le backend retourne encore 12 joueurs (rgression)
  - DashboardDataService - Integration TDD Validation des donnes relles devrait recevoir 147 joueurs depuis l'API backend (pas 12)
  - GameDetailComponent - TDD Tests Date Handling should handle null or undefined dates gracefully
  - GameQueryService - Enhanced Logging (JIRA-4A) Error message mapping maps HTTP 400 to a user-friendly message
  - GameQueryService - Enhanced Logging (JIRA-4A) Error message mapping maps HTTP 401 to a user-friendly message
  - GameQueryService - Enhanced Logging (JIRA-4A) Error message mapping maps HTTP 403 to a user-friendly message
  - GameQueryService - Enhanced Logging (JIRA-4A) Error message mapping maps HTTP 404 to a user-friendly message
  - GameQueryService - Enhanced Logging (JIRA-4A) Error message mapping maps HTTP 500 to a user-friendly message
  - ... 11 autres (voir `frontend/karma-failed-specs.txt`)

## Recommended Next Test Lots
- Stabilize failing suites first (backend integration + frontend karma failures).
- Then add targeted tests for critical low coverage files/services listed above.
