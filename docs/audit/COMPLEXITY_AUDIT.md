# Complexity Audit (JIRA-AUDIT-015)

- Date: 2026-02-17 22:02
- Owner: @Codex
- Scope: `src/main/java` + `frontend/src/app` (hors `*.spec.ts`)
- Methodology: static scan (method length, class length, approximate cyclomatic complexity by control-flow keyword count).

## Summary

- Java methods scanned: 1336
- TypeScript methods scanned: 910
- Java classes scanned: 304
- TypeScript classes scanned: 132
- Methods > 50 lines: 24
- Methods with complexity > 10: 15
- Classes > 500 lines: 1

## Top 10 Methods (Global)

| Rank | Lang | Method | Complexity | Lines | Location |
|---|---|---:|---:|---:|---|
| 1 | java | `validateTeamComposition` | 22 | 70 | `src/main/java/com/fortnite/pronos/service/ValidationService.java:139` |
| 2 | java | `getStatusCode` | 22 | 44 | `src/main/java/com/fortnite/pronos/core/error/ErrorCode.java:153` |
| 3 | ts | `loadDashboardData` | 21 | 94 | `frontend/src/app/features/dashboard/dashboard.component.ts:227` |
| 4 | java | `fetchFromPrimaryTables` | 18 | 72 | `src/main/java/com/fortnite/pronos/service/seed/SupabaseSeedDataProviderService.java:74` |
| 5 | java | `saveCsvRows` | 15 | 62 | `src/main/java/com/fortnite/pronos/service/CsvDataLoaderService.java:165` |
| 6 | java | `resolve` | 15 | 47 | `src/main/java/com/fortnite/pronos/service/UserResolver.java:42` |
| 7 | ts | `extractBackendMessage` | 15 | 35 | `frontend/src/app/features/game/services/game-query.service.ts:210` |
| 8 | ts | `resolveErrorMessage` | 15 | 25 | `frontend/src/app/features/draft/services/draft.service.ts:269` |
| 9 | ts | `resolveErrorMessage` | 15 | 24 | `frontend/src/app/features/game/services/game-command.service.ts:254` |
| 10 | ts | `mapTeamsData` | 13 | 17 | `frontend/src/app/features/teams/teams-list/teams-list.component.ts:135` |

## Top 10 Java Methods

| Rank | Method | Complexity | Lines | Location |
|---|---:|---:|---:|---|
| 1 | `validateTeamComposition` | 22 | 70 | `src/main/java/com/fortnite/pronos/service/ValidationService.java:139` |
| 2 | `getStatusCode` | 22 | 44 | `src/main/java/com/fortnite/pronos/core/error/ErrorCode.java:153` |
| 3 | `fetchFromPrimaryTables` | 18 | 72 | `src/main/java/com/fortnite/pronos/service/seed/SupabaseSeedDataProviderService.java:74` |
| 4 | `saveCsvRows` | 15 | 62 | `src/main/java/com/fortnite/pronos/service/CsvDataLoaderService.java:165` |
| 5 | `resolve` | 15 | 47 | `src/main/java/com/fortnite/pronos/service/UserResolver.java:42` |
| 6 | `sanitize` | 11 | 34 | `src/main/java/com/fortnite/pronos/service/util/TeamRosterSanitizer.java:28` |
| 7 | `createTestGame` | 10 | 62 | `src/main/java/com/fortnite/pronos/service/H2SeedService.java:121` |
| 8 | `validateRegionRules` | 10 | 47 | `src/main/java/com/fortnite/pronos/service/ValidationService.java:84` |
| 9 | `mapPlayers` | 10 | 29 | `src/main/java/com/fortnite/pronos/service/seed/SupabaseSeedDataProviderService.java:198` |
| 10 | `validateUser` | 10 | 12 | `src/main/java/com/fortnite/pronos/service/seed/UserSeedService.java:72` |

## Top 10 TypeScript Methods

| Rank | Method | Complexity | Lines | Location |
|---|---:|---:|---:|---|
| 1 | `loadDashboardData` | 21 | 94 | `frontend/src/app/features/dashboard/dashboard.component.ts:227` |
| 2 | `extractBackendMessage` | 15 | 35 | `frontend/src/app/features/game/services/game-query.service.ts:210` |
| 3 | `resolveErrorMessage` | 15 | 25 | `frontend/src/app/features/draft/services/draft.service.ts:269` |
| 4 | `resolveErrorMessage` | 15 | 24 | `frontend/src/app/features/game/services/game-command.service.ts:254` |
| 5 | `mapTeamsData` | 13 | 17 | `frontend/src/app/features/teams/teams-list/teams-list.component.ts:135` |
| 6 | `extractRawMessage` | 12 | 29 | `frontend/src/app/core/services/ui-error-feedback.service.ts:108` |
| 7 | `getFocusAnnouncement` | 12 | 26 | `frontend/src/app/shared/services/focus-management.service.ts:257` |
| 8 | `isGameHost` | 12 | 24 | `frontend/src/app/features/game/services/game-query.service.ts:112` |
| 9 | `mapTradeOffersToTrades` | 11 | 22 | `frontend/src/app/features/trades/trade-list/trade-list.component.ts:144` |
| 10 | `setupFilteredTrades` | 10 | 58 | `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts:174` |

## Threshold Violations

### Methods > 50 lines (Top 20)

| Lang | Method | Lines | Complexity | Location |
|---|---:|---:|---:|---|
| java | `getPlayerLeaderboardByGame` | 97 | 9 | `src/main/java/com/fortnite/pronos/service/leaderboard/PlayerLeaderboardService.java:125` |
| ts | `loadDashboardData` | 94 | 21 | `frontend/src/app/features/dashboard/dashboard.component.ts:227` |
| java | `getPlayerLeaderboard` | 87 | 8 | `src/main/java/com/fortnite/pronos/service/leaderboard/PlayerLeaderboardService.java:36` |
| java | `filterChain` | 75 | 3 | `src/main/java/com/fortnite/pronos/config/SecurityConfig.java:48` |
| java | `fetchFromPrimaryTables` | 72 | 18 | `src/main/java/com/fortnite/pronos/service/seed/SupabaseSeedDataProviderService.java:74` |
| java | `redisCacheManager` | 71 | 1 | `src/main/java/com/fortnite/pronos/config/CacheConfig.java:40` |
| java | `validateTeamComposition` | 70 | 22 | `src/main/java/com/fortnite/pronos/service/ValidationService.java:139` |
| ts | `createPointsChart` | 70 | 8 | `frontend/src/app/features/dashboard/services/dashboard-chart.service.ts:115` |
| ts | `createRegionChart` | 70 | 6 | `frontend/src/app/features/dashboard/services/dashboard-chart.service.ts:41` |
| java | `getPronostiqueurLeaderboard` | 67 | 8 | `src/main/java/com/fortnite/pronos/service/leaderboard/PronostiqueurLeaderboardService.java:34` |
| java | `loadMockDataFromCsv` | 64 | 9 | `src/main/java/com/fortnite/pronos/service/MockDataGeneratorService.java:34` |
| ts | `getDashboardData` | 63 | 7 | `frontend/src/app/features/dashboard/services/dashboard-data.service.ts:156` |
| java | `saveCsvRows` | 62 | 15 | `src/main/java/com/fortnite/pronos/service/CsvDataLoaderService.java:165` |
| java | `createTestGame` | 62 | 10 | `src/main/java/com/fortnite/pronos/service/H2SeedService.java:121` |
| java | `createTestGames` | 62 | 4 | `src/main/java/com/fortnite/pronos/service/seed/GameSeedService.java:78` |
| java | `getLeaderboardStatsByGame` | 61 | 8 | `src/main/java/com/fortnite/pronos/service/leaderboard/LeaderboardStatsService.java:89` |
| java | `getLeaderboardByGame` | 61 | 6 | `src/main/java/com/fortnite/pronos/service/leaderboard/TeamLeaderboardService.java:97` |
| java | `getLeaderboard` | 61 | 5 | `src/main/java/com/fortnite/pronos/service/leaderboard/TeamLeaderboardService.java:34` |
| java | `parseCsvLine` | 59 | 6 | `src/main/java/com/fortnite/pronos/service/MockDataGeneratorService.java:103` |
| ts | `setupFilteredTrades` | 58 | 10 | `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts:174` |

### Classes > 500 lines

| Lang | Class | Lines | Location |
|---|---:|---:|---|
| java | `GameController` | 526 | `src/main/java/com/fortnite/pronos/controller/GameController.java:55` |

## Refactoring Recommendations (Prioritized)

1. Extract large controller/service methods into focused private methods (validation, mapping, orchestration).
2. Split high-complexity TypeScript components into UI state service + presenter/mapper utilities.
3. Reduce conditional branching via strategy objects for status/error mapping flows.
4. Add targeted tests before each refactor on high-risk hotspots (top 10 above).

## Execution Log

- 2026-02-17: `JIRA-AUDIT-016` completed.
- Refactor applied on `src/main/java/com/fortnite/pronos/service/ValidationService.java` (`validateTeamComposition` split into focused helpers).
- Additional coverage added in `src/test/java/com/fortnite/pronos/service/ValidationServiceTeamCompositionTddTest.java` for unsupported rule types, unknown regions, and empty team roster.

