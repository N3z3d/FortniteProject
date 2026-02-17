# Clean Code / CLAUDE.md Conformity Audit - JIRA-AUDIT-003

**Date**: 2026-02-15
**Scope**: All source files (backend 263 Java + frontend 140 TypeScript)
**Rules**: CLAUDE.md - max 500 lines/class, max 50 lines/method, SOLID, DRY, naming

## Summary

| Metric | Backend | Frontend | Total |
|--------|---------|----------|-------|
| Files scanned | 263 | 140 | 403 |
| Files > 500 lines | 1 | 1 | 2 |
| Methods > 50 lines | 25 | 12 | 37 |
| Files 450-500 lines (at risk) | 4 | 6 | 10 |

---

## 1. Files Exceeding 500 Lines

### Backend (1 file)

| File | Lines | Over by |
|------|-------|---------|
| `config/GlobalExceptionHandler.java` | 525 | 25 |

### Frontend (1 file)

| File | Lines | Over by |
|------|-------|---------|
| `features/auth/login/login.component.ts` | 602 | 102 |

---

## 2. Methods Exceeding 50 Lines

### Backend (25 methods)

**Category A: Repository @Query annotations (not refactorable logic)**
These are long SQL strings in `@Query` annotations. The method body itself is 1 line but the annotation inflates the count. These are **acceptable exceptions** to the 50-line rule.

| File | Method | Lines | Notes |
|------|--------|-------|-------|
| GameRepository | findByCreator | 249 | @Query SQL |
| GameParticipantRepository | findByGame | 185 | @Query SQL |
| GameRegionRuleRepository | findByGame | 114 | @Query SQL |
| TeamRepository | findByOwnerAndSeason | 95 | @Query SQL |
| ScoreRepository | findByPlayerAndSeason | 74 | @Query SQL |
| GameRepositoryPort | findByCreator | 68 | @Query SQL |
| TradeRepository | findByTeamId | 65 | @Query SQL |

**Category B: Service logic (refactorable)**

| File | Method | Lines | Recommendation |
|------|--------|-------|----------------|
| PlayerLeaderboardService | getPlayerLeaderboardByGame | 97 | Extract sub-queries + mapping into helpers |
| PlayerLeaderboardService | getPlayerLeaderboard | 87 | Extract common leaderboard logic |
| FortniteTrackerService | getPlayerStats | 86 | Extract HTTP call, parsing, mapping |
| SupabaseSeedDataProviderService | fetchFromPrimaryTables | 72 | Extract per-table fetch methods |
| ValidationService | validateTeamComposition | 70 | Extract validation steps |
| PronostiqueurLeaderboardService | getPronostiqueurLeaderboard | 67 | Extract aggregation logic |
| MockDataGeneratorService | loadMockDataFromCsv | 64 | Extract CSV parsing + entity creation |
| CsvDataLoaderService | saveCsvRows | 62 | Extract row-by-row processing |
| H2SeedService | createTestGame | 62 | Extract entity creation helpers |
| GameSeedService | createTestGames | 62 | Extract game setup helpers |
| LeaderboardStatsService | getLeaderboardStatsByGame | 61 | Extract stats computation |
| TeamLeaderboardService | getLeaderboard | 61 | Extract team scoring logic |
| TeamLeaderboardService | getLeaderboardByGame | 61 | Extract per-game logic |
| MockDataGeneratorService | parseCsvLine | 59 | Extract field parsing |
| GameNotificationService | GameNotification | 53 | Inner class, extract to own file |

**Category C: Configuration (acceptable exceptions)**

| File | Method | Lines | Notes |
|------|--------|-------|-------|
| SecurityConfig | filterChain | 75 | Spring Security chain config |
| CacheConfig | redisCacheManager | 71 | Cache config definitions |
| TradeNotificationService | TradeNotification | 70 | Inner class (record/DTO) |

### Frontend (12 methods)

| File | Method | Lines | Recommendation |
|------|--------|-------|----------------|
| dashboard.component.ts | loadDashboardData | 94 | Extract data loading into sub-methods |
| dashboard-chart.service.ts | createRegionChart | 70 | Extract chart config builder |
| dashboard-chart.service.ts | createPointsChart | 70 | Extract chart config builder |
| leaderboard-api.mapper.ts | calculateGlobalStats | 68 | Extract stat computation steps |
| dashboard-data.service.ts | getDashboardData | 63 | Extract per-section data fetching |
| stats-api.mapper.ts | mapLeaderboardToTeamStats | 60 | Extract mapping steps |
| trading-dashboard.component.ts | setupFilteredTrades | 58 | Extract filter logic |
| dashboard.component.ts | ngOnInit | 56 | Extract subscription setup |
| notification.service.ts | show | 55 | Extract config building |
| game-creation-wizard.component.ts | getGameTemplates | 54 | Extract template definitions to constant |
| stats-api.mapper.ts | mapApiStatsToDisplayStats | 53 | Extract computation helpers |
| button-effects.service.ts | initMagneticButton | 52 | Extract event handlers |

---

## 3. At-Risk Files (450-500 lines)

These files are close to the 500-line limit and should be proactively refactored.

### Backend
| File | Lines | % of limit |
|------|-------|------------|
| Game.java (model) | 468 | 94% |
| GameController.java | 460 | 92% |
| DraftService.java | 450 | 90% |

### Frontend
| File | Lines | % of limit |
|------|-------|------------|
| trade-form.component.ts | 490 | 98% |
| dashboard.component.ts | 488 | 98% |
| trade-proposal.component.ts | 485 | 97% |
| trading-dashboard.component.ts | 481 | 96% |
| main-layout.component.ts | 476 | 95% |
| leaderboard.service.ts | 465 | 93% |
| trading.service.ts | 461 | 92% |

---

## 4. Refactoring Recommendations (Priority Order)

### P1 - Must fix (over limit)

| # | Target | Lines | Action |
|---|--------|-------|--------|
| 1 | `login.component.ts` | 602 | Extract form validation, OAuth logic, password reset into sub-services/components |
| 2 | `GlobalExceptionHandler.java` | 525 | Group handlers by domain (game, trade, auth, system) into separate handler classes |

### P2 - Should fix (methods > 50 lines, core business logic)

| # | Target | Method | Lines | Action |
|---|--------|--------|-------|--------|
| 3 | PlayerLeaderboardService | getPlayerLeaderboardByGame | 97 | Extract query + mapping |
| 4 | PlayerLeaderboardService | getPlayerLeaderboard | 87 | Share common logic with #3 |
| 5 | dashboard.component.ts | loadDashboardData | 94 | Extract sub-loaders |
| 6 | dashboard-chart.service.ts | createRegionChart/createPointsChart | 70 | Extract shared chart config builder |
| 7 | ValidationService | validateTeamComposition | 70 | Extract per-rule validators |
| 8 | PronostiqueurLeaderboardService | getPronostiqueurLeaderboard | 67 | Extract aggregation |

### P3 - Consider (at-risk files, seed/config methods)

| # | Target | Action |
|---|--------|--------|
| 9 | trade-form.component.ts (490) | Extract form sections into sub-components |
| 10 | trading-dashboard.component.ts (481) | Extract filter/sort logic into service |
| 11 | leaderboard.service.ts (465) | Remove 12 dead methods (identified in AUDIT-005/008) |
| 12 | Seed services methods (62-72 lines) | Acceptable for dev-only code |
| 13 | SecurityConfig.filterChain (75 lines) | Acceptable for Spring Security DSL |

---

## 5. Checklist Quality

- [x] CLAUDE.md rules scanned: max 500 lines/class, max 50 lines/method
- [x] All 403 source files analyzed (263 Java + 140 TypeScript)
- [x] Violations classified by severity (P1/P2/P3)
- [x] Repository @Query annotations identified as acceptable exceptions
- [x] Configuration/seed code identified as lower priority
- [x] At-risk files (>90% of limit) flagged for proactive refactoring
- [x] Recommendations provided with concrete actions
