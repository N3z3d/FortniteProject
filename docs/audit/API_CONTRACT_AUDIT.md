# API Contract Audit Report - JIRA-AUDIT-008

**Date**: 2026-02-15
**Scope**: All REST endpoints (backend Spring controllers vs frontend Angular HTTP calls)

## Summary

| Category | Count |
|----------|-------|
| Backend endpoints | 89 |
| Frontend HTTP calls | 91 |
| Aligned (OK) | 46 |
| Path mismatches | 2 |
| Frontend calls with no backend endpoint | 17 |
| Backend endpoints with no frontend caller | 24 |
| Duplicate frontend calls (same endpoint, different services) | 8 |

## Critical Mismatches (will cause 404/errors)

### 1. joinGame path mismatch
- **Frontend**: `POST /api/games/{gameId}/join` (`GameCommandService.joinGame()`)
- **Backend**: `POST /api/games/join` (`GameController.joinGame()`)
- **Impact**: Frontend includes `gameId` in the URL path, but backend expects it only in the request body. The path `/api/games/{uuid}/join` does not match any `@PostMapping` in the controller.
- **Severity**: CRITICAL - join game likely broken
- **Fix**: Change frontend to `POST /api/games/join`

### 2. GameCommandService draft endpoints use wrong base path
- **Frontend**: `POST /api/games/{id}/draft/*` (6 endpoints in GameCommandService)
- **Backend**: `POST /api/drafts/{id}/*` (DraftController)
- **Impact**: All draft operations from GameCommandService route to wrong controller path
- **Affected endpoints**:
  - `POST /api/games/{id}/draft/finish` -> should be `POST /api/drafts/{id}/finish`
  - `POST /api/games/{id}/draft/initialize` -> no backend equivalent
  - `POST /api/games/{id}/draft/select` -> should be `POST /api/drafts/{id}/select-player`
  - `POST /api/games/{id}/draft/pause` -> no backend equivalent
  - `POST /api/games/{id}/draft/resume` -> no backend equivalent
  - `POST /api/games/{id}/draft/cancel` -> no backend equivalent
- **Note**: The frontend `DraftService` uses the correct `/api/drafts/` path. These GameCommandService methods may be dead code or duplicates.

## Frontend Endpoints with No Backend Equivalent

### GameCommandService
| Frontend Endpoint | Notes |
|---|---|
| `POST /api/games/{id}/invitation-code` | Generate invitation code - no backend endpoint |
| `POST /api/games/{id}/archive` | Archive game - no backend endpoint |

### GameQueryService
| Frontend Endpoint | Notes |
|---|---|
| `GET /api/games/validate-code/{code}` | Validate invitation code - no backend endpoint |
| `GET /api/games/{id}/draft/state` | Draft state - should use DraftService |
| `GET /api/games/{id}/draft/history` | Draft history - should use DraftService |
| `GET /api/games/{id}/draft/statistics` | Draft statistics - should use DraftService |
| `GET /api/games/{id}/can-join` | Can join check - no backend endpoint |

### AuthSwitchService
| Frontend Endpoint | Notes |
|---|---|
| `POST /api/auth/switch` | Switch user - no backend endpoint |
| `GET /api/auth/available-users` | List users - no backend endpoint |
| `POST /api/auth/notify-switch` | Notify switch - no backend endpoint |
| `POST /api/auth/validate-session` | Validate session - no backend endpoint |
| `GET /api/auth/status` | Auth status - no backend endpoint |
| **Note**: AuthSwitchService has error handling that catches failures silently. These are likely dev-mode features not yet implemented on backend. |

### TradingService
| Frontend Endpoint | Notes |
|---|---|
| `GET /api/trades/teams?gameId={id}` | Get teams for trade - no backend endpoint (backend has TeamController) |
| `GET /api/trades/history?gameId={id}` | Trade history - backend has `/api/trades/team/{teamId}/history` (different path) |

### LeaderboardService (12 endpoints with no backend match)
| Frontend Endpoint | Notes |
|---|---|
| `GET /api/leaderboard/teams/{teamId}` | Team details - not in LeaderboardController |
| `POST /api/leaderboard/teams/{teamId}/trade` | Execute trade via leaderboard - not in backend |
| `GET /api/leaderboard/teams/{teamId}/movements` | Team movements - not in backend |
| `POST /api/leaderboard/players/{playerId}/points` | Update points - not in backend |
| `GET /api/leaderboard/players/{playerId}` | Player from pool - not in backend |
| `GET /api/leaderboard/players/search` | Search players - not in backend |
| `GET /api/leaderboard/rankings/{region}` | Region rankings - not in backend |
| `PATCH /api/leaderboard/players/{playerId}/availability` | Update availability - not in backend |
| `GET /api/leaderboard/players/{playerId}/stats` | Player stats - not in backend |
| **Note**: These LeaderboardService methods were identified as dead code in AUDIT-005. They are never called from any component. |

## Duplicate Frontend Calls (same endpoint, different services)

| Endpoint | Service 1 | Service 2 |
|---|---|---|
| `GET /api/games/{id}` | GameDataService | GameQueryService |
| `GET /api/games/my-games` | GameDataService | GameQueryService |
| `GET /api/games/{id}/participants` | GameDataService | GameQueryService |
| `GET /api/leaderboard?season=...` | DashboardDataService | LeaderboardService |
| `GET /api/leaderboard/stats?season=...` | DashboardDataService | LeaderboardService |
| `GET /api/leaderboard/distribution/regions` | DashboardDataService | LeaderboardService |
| `GET /api/leaderboard?season=...` | HttpDashboardRepository | HttpLeaderboardRepository |
| `GET /api/leaderboard/stats?season=...` | HttpDashboardRepository | HttpLeaderboardRepository |

**Note**: The duplicate calls between `GameDataService` and `GameQueryService` may be intentional (CQRS separation). The dashboard/leaderboard duplicates suggest consolidation opportunity.

## Backend Endpoints with No Frontend Caller

### ApiController (`/api`)
- `GET /api/teams` - legacy endpoint
- `GET /api/teams/{teamId}` - legacy endpoint
- `GET /api/players` - legacy endpoint
- `GET /api/players/{id}` - legacy endpoint
- `GET /api/trade-form-data` - legacy endpoint

### PlayerController (`/players`)
- `GET /players/all` - legacy paginated
- `GET /players/region/{region}`
- `GET /players/tranche/{tranche}`
- `GET /players/search`
- `GET /players/active`
- `GET /players/stats`

### ScoreController (`/scores`)
- All 7 endpoints (no frontend score management UI)

### LeaderboardController
- `GET /api/leaderboard/season/{season}` - alternate to `?season=` param
- `GET /api/leaderboard/team/{teamId}` - specific team ranking
- `GET /api/leaderboard/distribution/tranches` - tranche distribution
- `GET /api/leaderboard/debug/*` - debug endpoints

### UserController
- `GET /api/users` - all users
- `GET /api/users/{id}` - user by ID

### HomeController
- `GET /` - home
- `GET /api` - api info

## Dead Code in Frontend Services (calling non-existent backend endpoints)

### GameCommandService — 6 dead draft methods
Methods never called from any component (DraftComponent uses DraftService instead):
- `finishDraft()` — `POST /api/games/{id}/draft/finish` (wrong path)
- `initializeDraft()` — `POST /api/games/{id}/draft/initialize` (wrong path)
- `makePlayerSelection()` — `POST /api/games/{id}/draft/select` (wrong path)
- `pauseDraft()` — `POST /api/games/{id}/draft/pause` (wrong path)
- `resumeDraft()` — `POST /api/games/{id}/draft/resume` (wrong path)
- `cancelDraft()` — `POST /api/games/{id}/draft/cancel` (wrong path)

Plus `generateInvitationCode()` — `POST /api/games/{id}/invitation-code` (no backend endpoint, never called)

### GameQueryService — 5 dead stub methods
Methods never called from any component:
- `validateInvitationCode()` — `GET /api/games/validate-code/{code}` (no backend)
- `getDraftState()` — `GET /api/games/{id}/draft/state` (no backend)
- `getDraftHistory()` — `GET /api/games/{id}/draft/history` (no backend)
- `getDraftStatistics()` — `GET /api/games/{id}/draft/statistics` (no backend)
- `canJoinGame()` — `GET /api/games/{id}/can-join` (no backend)

### GameService — 12 dead delegate methods
All delegates to the dead GameCommandService/GameQueryService methods above.

## Recommended Actions (Priority Order)

### P0 - Fix immediately
1. **~~Fix joinGame path~~**: ~~Change `GameCommandService.joinGame()` from `POST /api/games/{gameId}/join` to `POST /api/games/join`~~ **FIXED** (2026-02-15)

### P1 - Fix soon
2. **Audit GameCommandService draft methods**: Determine if they are dead code (DraftService handles drafts correctly). If dead, remove them. If used, fix paths to `/api/drafts/`.
3. **Fix trade history path**: Align `TradingService.getTradeHistory()` with backend `/api/trades/team/{teamId}/history`

### P2 - Improve
4. **Remove dead LeaderboardService methods**: 12 methods with no backend and no callers (already flagged in AUDIT-005)
5. **Consolidate duplicate HTTP calls**: GameDataService vs GameQueryService, DashboardDataService vs HttpDashboardRepository
6. **Implement or remove stub endpoints**: AuthSwitchService endpoints, `archiveGame`, `generateInvitationCode`, `validateCode`, `canJoin`

### P3 - Consider
7. **Remove unused backend endpoints**: ApiController legacy endpoints, PlayerController unused endpoints
8. **Add OpenAPI/Swagger** for contract enforcement
