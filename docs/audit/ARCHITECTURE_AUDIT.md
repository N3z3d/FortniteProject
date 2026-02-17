# Architecture Conformity Audit Report - JIRA-AUDIT-002

**Date**: 2026-02-15
**Scope**: Backend (hexagonal architecture) + Frontend (CQRS, layered)

## Summary

| Category | Backend | Frontend | Total |
|----------|---------|----------|-------|
| CRITICAL violations | 2 | 5 | 7 |
| MAJOR violations | 10 | 5 | 15 |
| MINOR violations | 4 | 3 | 7 |
| **Total** | **16** | **13** | **29** |

| Layer | Backend Status | Frontend Status |
|-------|---------------|-----------------|
| Domain models | 100% compliant | N/A |
| Adapters | 100% compliant | N/A |
| Controllers | 100% compliant | N/A |
| Services -> Ports | 28% fully migrated | N/A |
| CQRS separation | N/A | 1/4 features compliant |
| Layer boundaries | Clean | 5 violations (core/shared -> features) |

---

## BACKEND AUDIT

### 1. Domain Model Purity - COMPLIANT

All 5 domain packages verified clean:
- `domain/game/model/` - No JPA, Spring, Hibernate, or Lombok annotations
- `domain/player/model/` - Clean
- `domain/team/model/` - Clean
- `domain/draft/model/` - Clean
- `domain/trade/model/` - Clean

### 2. Adapter Layer - COMPLIANT

All 5 adapters correctly implement their ports:
- `GameRepositoryAdapter` implements `GameDomainRepositoryPort`
- `PlayerRepositoryAdapter` implements `PlayerDomainRepositoryPort`
- `TeamRepositoryAdapter` implements `TeamDomainRepositoryPort`
- `DraftRepositoryAdapter` implements `DraftDomainRepositoryPort`
- `TradeRepositoryAdapter` implements `TradeDomainRepositoryPort`

### 3. Controller Layer - COMPLIANT

All controllers delegate to services. No direct repository imports found.

### 4. Service Layer - Port Migration Status

**54 JPA repository imports** across 30 service files.
**43 domain port imports** across 23 service files.

#### A. Fully Migrated (domain ports only) - 6 services

| Service | Domain Ports Used |
|---------|-------------------|
| `GameCreationService` | GameDomainRepositoryPort, UserRepositoryPort |
| `GameParticipantService` | GameDomainRepositoryPort, GameParticipantRepositoryPort, GameRepositoryPort, UserRepositoryPort |
| `GameDraftService` | DraftDomainRepositoryPort, DraftPickRepositoryPort, DraftRepositoryPort, GameDomainRepositoryPort, GameParticipantRepositoryPort, PlayerRepositoryPort |
| `UserService` | UserRepositoryPort |
| `InvitationCodeService` | InvitationCodeRepositoryPort |
| `TradeQueryService` | TradeDomainRepositoryPort |

#### B. Partially Migrated (mixed ports + JPA repos) - 9 services

| Service | Domain Ports | JPA Repos | Severity |
|---------|-------------|-----------|----------|
| `DraftService` | 4 (DraftDomain, Draft, Game, Player) | 3 (Draft, GameParticipant, Player) | **CRITICAL** |
| `TeamService` | 3 (TeamDomain, PlayerDomain, User) | 2 (Team, TeamPlayer) | **MAJOR** |
| `ScoreService` | 2 (Player, User) | 3 (Player, Score, Team) | **MAJOR** |
| `GameDetailService` | 2 (GameParticipant, Game) | 2 (Draft, Score) | **MAJOR** |
| `PlayerService` | 1 (Player) | 2 (Player, Score) | **MAJOR** |
| `GameQueryService` | 2 (GameDomain, User) | 1 (Player) | **MAJOR** |
| `TeamQueryService` | 2 (Player, User) | 2 (Player, Team) | **MAJOR** |
| `TeamInitializationService` | 1 (User) | 2 (Player, Team) | **MAJOR** |
| `TradingService` | 1 (Player) | multiple via wildcard | **CRITICAL** |

#### C. Not Migrated (JPA repos only) - 7 services

| Service | JPA Repos | Priority |
|---------|-----------|----------|
| `ScoreCalculationService` | 3 (Score, TeamPlayer, Team) | MAJOR |
| `AdminDashboardService` | 3 (Game, Trade, User) | MINOR |
| `GameService` | 1 (Team) | MAJOR |
| `TradeResponseMapper` | 2 (Player, Team) | MINOR |
| `LeaderboardStatsService` | 1 (JPA) | MINOR |
| `TeamLeaderboardService` | 1 (JPA) | MINOR |
| `PlayerLeaderboardService` | 1 (JPA) | MINOR |

#### D. Bootstrap/Auth (acceptable exceptions) - 14 services

Seed services, auth strategies, CSV loaders, and ingestion services are acceptable JPA users:
- `H2SeedService`, `FakeGameSeedService`, `ReferenceGameSeedService`
- `CsvDataLoaderService`, `CsvBootstrapService`
- `PlayerSeedService`, `TeamSeedService`, `UserSeedService`, `GameSeedService`, `ReferenceUserSeedService`
- `CustomUserDetailsService`, `UnifiedAuthService`
- `SpringSecurityAuthenticationStrategy`, `DevelopmentAuthenticationStrategy`
- `PrIngestionService`

### 5. Missing Domain Ports

These JPA repositories are used by business services but have no corresponding domain port:

| JPA Repository | Used By | Domain Port Needed |
|----------------|---------|-------------------|
| `ScoreRepository` | ScoreService, ScoreCalculationService, PlayerService, GameDetailService | `ScoreDomainRepositoryPort` |
| `TeamPlayerRepository` | TeamService, ScoreCalculationService | `TeamPlayerDomainRepositoryPort` (or extend TeamDomainRepositoryPort) |
| `GameParticipantRepository` | DraftService | Already exists as `GameParticipantRepositoryPort` - DraftService not using it |

---

## FRONTEND AUDIT

### 1. CQRS Compliance

| Feature | Query Service | Command Service | Status |
|---------|--------------|-----------------|--------|
| **Game** | `GameQueryService` | `GameCommandService` | **COMPLIANT** (but duplicate `GameDataService`) |
| **Draft** | - | - | **CRITICAL** - Monolithic `DraftService` (288 lines, 9 queries + 7 commands mixed) |
| **Trades** | - | - | **CRITICAL** - Monolithic `TradingService` (461 lines, 7 queries + 5 commands + 8 state mgmt mixed) |
| **Teams** | - | - | **MAJOR** - Monolithic `TeamService` (4 reads + 4 writes mixed) |

### 2. Layer Boundary Violations

#### CRITICAL: Core -> Features (dependency inversion)

5 files in `core/` import from `features/`:

| Core File | Feature Import | Severity |
|-----------|---------------|----------|
| `core/services/user-games.store.ts` | `features/game/services/game.service` | **CRITICAL** |
| `core/services/user-games.store.ts` | `features/game/models/game.interface` | **CRITICAL** |
| `core/services/game-selection.service.ts` | `features/game/models/game.interface` | **CRITICAL** |
| `core/services/mock-data.service.ts` | `features/game/models/game.interface` | MINOR |
| `core/data/mock-game-data.ts` | `features/game/models/game.interface` | MINOR |

**Root cause**: `Game` interface is defined in `features/game/models/` but is used project-wide. It should live in `core/models/` or `shared/models/`.

#### CRITICAL: Shared -> Features

| Shared File | Feature Import | Severity |
|-------------|---------------|----------|
| `shared/components/main-layout/main-layout.component.ts` | `features/game/services/game.service` | **CRITICAL** |
| `shared/components/main-layout/main-layout.component.ts` | `features/game/models/game.interface` | **CRITICAL** |

### 3. Over-Coupled Components

| Component | Injected Dependencies | Threshold | Over By |
|-----------|----------------------|-----------|---------|
| `main-layout.component.ts` | 13 | 5 | +160% |
| `draft.component.ts` | 11 | 5 | +120% |

### 4. Direct HTTP Calls in Components

| Component | Issue | Severity |
|-----------|-------|----------|
| `dashboard.component.ts` | Injects `HttpClient` directly (line 6, 125) | **CRITICAL** |

### 5. Duplicate Services

| Endpoint | GameQueryService | GameDataService |
|----------|-----------------|-----------------|
| `GET /games/{id}` | `getGameById()` | `getGameById()` |
| `GET /games/{id}/participants` | `getGameParticipants()` | `getGameParticipants()` |
| `GET /games/my-games` | `getUserGames()` | `getUserGames()` |

**Impact**: DRY violation, maintenance burden, inconsistent behavior risk.

### 6. Inconsistent Patterns

- **Repository pattern**: Used for Dashboard and Leaderboard only, not for Game/Draft/Trades/Teams
- **Facade pattern**: DashboardFacade exists but Dashboard component bypasses it with direct HTTP
- **Service naming**: `game-data.service` vs `game-query.service` (ambiguous roles)

---

## VIOLATION SUMMARY TABLE

### Backend

| # | File | Violation | Severity |
|---|------|-----------|----------|
| B1 | `TradingService` | Wildcard JPA import, multiple direct JPA calls, unsafe type casts | CRITICAL |
| B2 | `DraftService` | 3 JPA repos alongside 4 domain ports (inconsistent) | CRITICAL |
| B3 | `TeamService` | 2 JPA repos alongside 3 domain ports | MAJOR |
| B4 | `ScoreService` | 3 JPA repos alongside 2 domain ports | MAJOR |
| B5 | `GameDetailService` | 2 JPA repos alongside 2 domain ports | MAJOR |
| B6 | `PlayerService` | 2 JPA repos alongside 1 domain port | MAJOR |
| B7 | `GameQueryService` | 1 JPA repo alongside 2 domain ports | MAJOR |
| B8 | `TeamQueryService` | 2 JPA repos alongside 2 domain ports | MAJOR |
| B9 | `TeamInitializationService` | 2 JPA repos alongside 1 domain port | MAJOR |
| B10 | `ScoreCalculationService` | 3 JPA repos, 0 domain ports | MAJOR |
| B11 | `GameService` | 1 JPA repo (TeamRepository) | MAJOR |
| B12 | No `ScoreDomainRepositoryPort` | Missing port for Score domain | MAJOR |
| B13 | `AdminDashboardService` | 3 JPA repos (acceptable - admin) | MINOR |
| B14 | `TradeResponseMapper` | 2 JPA repos (in service layer) | MINOR |
| B15 | Leaderboard services | 4 services with JPA repos, no ports | MINOR |
| B16 | `GameService.getGameTeams()` | TeamRepository call should use port | MINOR |

### Frontend

| # | File | Violation | Severity |
|---|------|-----------|----------|
| F1 | `DraftService` | Monolithic: 9 queries + 7 commands + 6 helpers | CRITICAL |
| F2 | `TradingService` | Monolithic: 461 lines, 7 queries + 5 commands + 8 state mgmt | CRITICAL |
| F3 | `user-games.store.ts` | Core imports GameService from features | CRITICAL |
| F4 | `game-selection.service.ts` | Core imports Game model from features | CRITICAL |
| F5 | `main-layout.component.ts` | Shared imports GameService + Game from features | CRITICAL |
| F6 | `dashboard.component.ts` | Direct HttpClient injection | MAJOR |
| F7 | `TeamService` | No CQRS split (4 reads + 4 writes) | MAJOR |
| F8 | `GameQueryService` + `GameDataService` | Duplicate methods | MAJOR |
| F9 | `main-layout.component.ts` | 13 injected dependencies | MAJOR |
| F10 | `draft.component.ts` | 11 injected dependencies | MAJOR |
| F11 | `mock-data.service.ts` | Core imports Game from features | MINOR |
| F12 | `mock-game-data.ts` | Core data imports Game from features | MINOR |
| F13 | Inconsistent repository/facade patterns | Only Dashboard uses them | MINOR |

---

## RECOMMENDED ACTION PLAN

### Phase 1 - CRITICAL fixes (Week 1-2)

| # | Action | Violations Fixed | Effort |
|---|--------|-----------------|--------|
| 1 | **Move `Game` interface to `core/models/`** | F3, F4, F5, F11, F12 | 2h |
| 2 | **Split `DraftService` -> DraftQueryService + DraftCommandService** (frontend) | F1 | 4h |
| 3 | **Split `TradingService` -> TradeQueryService + TradeCommandService** (frontend) | F2 | 4h |
| 4 | **Create `MainLayoutFacade`** to reduce coupling + remove feature imports | F5, F9 | 3h |
| 5 | **TradingService** (backend): Replace JPA repos with domain ports | B1 | 4h |
| 6 | **DraftService** (backend): Complete migration to domain ports | B2 | 3h |

### Phase 2 - MAJOR fixes (Week 2-3)

| # | Action | Violations Fixed | Effort |
|---|--------|-----------------|--------|
| 7 | Create `ScoreDomainRepositoryPort` + adapter | B12 | 4h |
| 8 | Migrate `TeamService` (backend) to full domain ports | B3 | 3h |
| 9 | Migrate `ScoreService` to domain ports | B4 | 3h |
| 10 | Migrate `PlayerService` to domain ports | B6 | 2h |
| 11 | Split `TeamService` (frontend) -> Query + Command | F7 | 3h |
| 12 | Merge `GameDataService` into `GameQueryService` | F8 | 2h |
| 13 | Replace `HttpClient` in `DashboardComponent` with service | F6 | 1h |
| 14 | Reduce `draft.component.ts` injections via facade | F10 | 2h |

### Phase 3 - Cleanup (Week 3-4)

| # | Action | Violations Fixed | Effort |
|---|--------|-----------------|--------|
| 15 | Migrate `GameDetailService`, `GameQueryService` (backend) | B5, B7 | 3h |
| 16 | Migrate `TeamQueryService`, `TeamInitializationService` (backend) | B8, B9 | 3h |
| 17 | Migrate `ScoreCalculationService`, `GameService` (backend) | B10, B11 | 3h |
| 18 | Standardize service naming conventions (frontend) | F13 | 2h |
| 19 | Migrate leaderboard services to domain ports (backend) | B15 | 4h |

---

## METRICS

### Backend
- **Total service files**: ~36 core business + ~14 bootstrap/auth = ~50
- **Fully migrated to domain ports**: 6 services (17%)
- **Partially migrated**: 9 services (25%)
- **Not migrated**: 7 services (19%)
- **Bootstrap/Auth (acceptable)**: 14 services (39%)
- **Domain purity**: 100%
- **Controller isolation**: 100%
- **Adapter compliance**: 100%

### Frontend
- **CQRS compliant features**: 1/4 (Game only, 25%)
- **Layer boundary violations**: 7 files (core->features: 5, shared->features: 2)
- **Over-coupled components**: 2 (MainLayout 13 deps, Draft 11 deps)
- **Direct HTTP in components**: 1 (Dashboard)

---

## CHECKLIST

- [x] All backend service files scanned for JPA vs port imports
- [x] Domain model purity verified (5 domains)
- [x] Adapter compliance verified (5 adapters)
- [x] Controller isolation verified
- [x] Frontend CQRS compliance audited (4 features)
- [x] Frontend layer boundaries checked (core, shared, features)
- [x] Frontend component coupling analyzed
- [x] All violations classified by severity (CRITICAL/MAJOR/MINOR)
- [x] Prioritized action plan produced with effort estimates
- [x] No files modified (audit only)
