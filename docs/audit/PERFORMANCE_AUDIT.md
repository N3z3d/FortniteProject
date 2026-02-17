# Performance Audit Report

**Date**: 2026-02-17
**Ticket**: JIRA-AUDIT-011
**Scope**: Backend (Spring Boot JPA) + Frontend (Angular 20)

---

## 1. Backend - JPA N+1 Query Detection

### 1.1 Confirmed N+1 Patterns (CRITICAL)

All 4 leaderboard services iterate over `team.getPlayers()` inside loops over teams fetched without JOIN FETCH:

| Service | Line | Pattern | Impact |
|---------|------|---------|--------|
| **TeamLeaderboardService** | 54, 119 | `for (TeamPlayer tp : team.getPlayers())` | N+1: 1 query per team |
| **PlayerLeaderboardService** | 55, 144 | `for (TeamPlayer tp : team.getPlayers())` | N+1: 1 query per team |
| **PronostiqueurLeaderboardService** | 61 | `for (TeamPlayer tp : team.getPlayers())` | N+1: 1 query per team |
| **LeaderboardStatsService** | 108, 172 | `for (TeamPlayer tp : team.getPlayers())` | N+1: 1 query per team |

**Root cause**: `TeamRepository.findBySeason()` / `findBySeasonWithFetch()` fetches teams, but `Team.players` is `@OneToMany(mappedBy = "team")` with default LAZY. When iterating, each `team.getPlayers()` triggers a separate SELECT.

**Fix**: Use `@EntityGraph` or `JOIN FETCH` on team queries used by leaderboard services. TeamRepository already has `findBySeasonWithFetch` and `findParticipantTeamsWithFetch` - verify leaderboard services use these.

### 1.2 Eager @ManyToOne Without Explicit FetchType (MAJOR)

JPA default for `@ManyToOne` is **EAGER**. These 7 relations load automatically on every query:

| Entity | Field | Joins |
|--------|-------|-------|
| **DraftPick** | `draft` | Loads Draft every time |
| **DraftPick** | `participant` | Loads GameParticipant every time |
| **DraftPick** | `player` | Loads Player every time |
| **Trade** | `playerOut` | Loads Player every time |
| **Trade** | `playerIn` | Loads Player every time |
| **Trade** | `teamFrom` | Loads Team every time |
| **Trade** | `teamTo` | Loads Team every time |

**Impact**: Loading a list of DraftPicks triggers 3 JOINs. Loading Trades triggers 4 JOINs (plus 2 already-LAZY from/toTeam). This is fine for single-entity fetches but expensive for list queries.

**Fix**: Add `fetch = FetchType.LAZY` to all 7 `@ManyToOne` annotations above. Only fetch related entities when needed via `@EntityGraph` or explicit `JOIN FETCH`.

### 1.3 @ManyToMany Without Explicit FetchType (MAJOR)

JPA default for `@ManyToMany` is **LAZY** (correct). These are fine:

| Entity | Field | Default |
|--------|-------|---------|
| Trade | `offeredPlayers` | LAZY (OK) |
| Trade | `requestedPlayers` | LAZY (OK) |
| GameParticipant | `selectedPlayers` | LAZY (OK) |

### 1.4 @OneToMany Without @BatchSize (MINOR)

All `@OneToMany` collections default to LAZY (correct), but none have `@BatchSize` to optimize batch loading when accessed for multiple entities:

| Entity | Collection | Recommendation |
|--------|-----------|----------------|
| Game | `regionRules` | Add `@BatchSize(size = 20)` |
| Game | `participants` | Add `@BatchSize(size = 20)` |
| Player | `teamPlayers` | Add `@BatchSize(size = 50)` |
| Player | `scores` | Add `@BatchSize(size = 50)` |
| Team | `players` | Add `@BatchSize(size = 20)` - **highest priority** (leaderboard N+1) |
| Team | `outgoingTrades` | Add `@BatchSize(size = 20)` |
| Team | `incomingTrades` | Add `@BatchSize(size = 20)` |

### 1.5 @EntityGraph Usage (GOOD)

Game entity has 2 named entity graphs, used in GameRepository:
- `Game.withBasicDetails`: creator + participants
- `Game.withFullDetails`: creator + participants.user + regionRules + draft

TeamRepository has `findBySeasonWithFetch`, `findParticipantTeamsWithFetch`, `findByGameIdWithFetch` - these use `JOIN FETCH` for team.players.

---

## 2. Backend - Missing @Transactional(readOnly = true)

### 2.1 Services with Class-Level @Transactional (write) but Read-Only Methods

| Service | Class-Level | Read Methods Without readOnly |
|---------|-------------|-------------------------------|
| **DraftService** | `@Transactional` | `getDraft`, `getByGame`, `getDraftPicks` etc. have method-level `readOnly = true` (GOOD) |
| **GameCreationService** | `@Transactional` | All methods are write operations (OK) |
| **GameDraftService** | `@Transactional` | `getDraftStatus`, `getDraftTeams` have `readOnly = true` (GOOD) |
| **GameParticipantService** | `@Transactional` | `getParticipants`, `isUserInGame`, `getParticipantCount` have `readOnly = true` (GOOD) |
| **TradingService** | `@Transactional` | **No read-only methods** - all methods are write (queries go through TradeQueryService) |
| **TeamService** | `@Transactional` per method | All `@Transactional` without `readOnly` but all are write operations (OK) |
| **ScoreService** | Mixed | Lines 129, 151, 166 correctly use `readOnly = true` |

**Assessment**: Good practices overall. Most read-only service methods already have `@Transactional(readOnly = true)`.

---

## 3. Backend - Missing Database Indexes

### 3.1 No @Index Annotations Found

No entity has `@Table(indexes = {...})`. All indexes rely on:
- Primary keys (auto-indexed)
- Foreign key constraints (auto-indexed in some DBs, not all)
- Any Flyway/Liquibase migrations (not checked)

### 3.2 Columns Frequently Used in WHERE Clauses Without Explicit Index

| Table | Column(s) | Used In | Priority |
|-------|----------|---------|----------|
| `games` | `status` | `findByStatus`, `findActiveGames`, `findByStatusNot` | HIGH |
| `games` | `creator_id` | `findByCreator`, `findByCreatorId`, `countByCreator` | HIGH |
| `games` | `deleted_at` | All soft-delete filtered queries | HIGH |
| `games` | `season` | `findByCurrentSeason`, `findGamesBySeason` | MEDIUM |
| `games` | `invitation_code` | `findByInvitationCode` (unique lookup) | HIGH |
| `game_participants` | `game_id, user_id` | `findByGameAndUser`, `existsByGameAndUser` | HIGH (composite) |
| `teams` | `season` | `findBySeason`, `findActiveTeams` | HIGH |
| `teams` | `game_id` | `findByGameId` | HIGH |
| `scores` | `player_id, season` | `findByPlayerIdAndSeason` | HIGH (composite) |
| `trades` | `game_id` | `findByGameId` | MEDIUM |
| `trades` | `status` | `findByGameIdAndStatus` | MEDIUM |

**Note**: H2 (dev) creates indexes on foreign keys automatically. PostgreSQL (prod) does NOT auto-index FK columns. Explicit indexes are recommended for prod.

---

## 4. Backend - Pagination

### 4.1 Unbounded List Queries (MAJOR)

Several repository methods return `List<Entity>` without pagination:

| Repository | Method | Risk |
|-----------|--------|------|
| **GameRepository** | `findAllWithFetch()` | Loads ALL games with JOINs |
| **GameRepository** | `findByCreator()` | Unbounded if creator has many games |
| **PlayerRepository** | `findAll()` (JpaRepository default) | Loads ALL players |
| **ScoreRepository** | `findBySeason()` | All scores for a season |
| **TeamRepository** | `findBySeason()` | All teams for a season |
| **GameParticipantRepository** | 30+ `List<>` methods | Most are game-scoped (bounded by game size, OK) |

**Note**: `GameRepository.findAllGames(Pagination)` exists and uses pagination. But `findAllWithFetch()` does not.

### 4.2 Recommendation

- Add `Pageable` parameter to `findBySeason` on Score/Team repos
- Ensure all "find all" style queries go through paginated endpoints
- Current game scope limits most queries naturally (max ~20 participants/game)

---

## 5. Frontend - Routing and Lazy Loading

### 5.1 Lazy Loading Status (GOOD)

All feature routes use `loadComponent` or `loadChildren`:
- `login` -> `loadComponent` (lazy)
- `diagnostic` -> `loadComponent` (lazy)
- `games` -> `loadChildren` (lazy module)
- `auth` -> `loadChildren` (lazy module)
- `leaderboard` -> `loadChildren` (lazy module)
- `teams` -> `loadChildren` (lazy module)
- `trades` -> `loadChildren` (lazy module)
- `dashboard` -> `loadChildren` (lazy module)
- `draft` -> `loadComponent` (lazy)
- `profile` -> `loadComponent` (lazy)
- `settings` -> `loadComponent` (lazy)
- `admin` -> `loadChildren` (lazy routes)
- `not-found` -> `loadComponent` (lazy)

**Only `MainLayoutComponent` is eagerly loaded** (correct - it's the shell component).

### 5.2 Bundle Budget (OK)

```
Initial bundle: max 2MB (warning at 1.5MB)
Any component style: max 80kB (warning at 60kB)
Per-bundle: max 1.5MB (warning at 1MB)
```

### 5.3 Dependencies Assessment

| Dependency | Size Impact | Assessment |
|-----------|-------------|------------|
| `chart.js` (~200kB) | MEDIUM | Used for dashboard charts. Tree-shakeable via `ng2-charts` |
| `@stomp/stompjs` + `sockjs-client` | SMALL (~30kB) | WebSocket support |
| `@angular/material` + `@angular/cdk` | LARGE (~300kB tree-shaken) | Required for UI |
| No `moment.js` | - | GOOD - no heavy date library |
| No `lodash` | - | GOOD - no heavy utility library |

---

## 6. Frontend - Change Detection

### 6.1 Components Using OnPush (5/40 = 12.5%)

| Component | OnPush |
|-----------|--------|
| SimpleLeaderboardComponent | Yes |
| DashboardComponent | Yes |
| TradingDashboardComponent | Yes |
| TradeProposalComponent | Yes |
| TradeDetailsComponent | Yes |

### 6.2 Components Missing OnPush (MAJOR - 35 components)

All other standalone components use the default `ChangeDetectionStrategy.Default`. High-impact candidates for OnPush:
- **MainLayoutComponent** (shell, renders on every change)
- **GameHomeComponent** (list view with multiple items)
- **GameDetailComponent** (complex view)
- **DraftComponent** (real-time updates via WebSocket)
- **TeamsListComponent**, **TeamEditComponent** (list/form views)
- **ProfileComponent**, **SettingsComponent** (simple forms)

### 6.3 trackBy Usage (GOOD)

30 files use `trackBy` with `@for` or `*ngFor`. This appears consistent across list-rendering components.

---

## 7. Frontend - Subscription Management

### 7.1 Subscription Stats

| Pattern | Count | Assessment |
|---------|-------|------------|
| `.subscribe()` calls | 166 across 30 files | HIGH |
| `takeUntilDestroyed` / `takeUntil` / `DestroyRef` | 44 across 11 files | Partial coverage |

### 7.2 High-Risk Files (many subscribes, few lifecycle guards)

| File | `.subscribe()` | Guards | Risk |
|------|---------------|--------|------|
| **TradingDashboardComponent** | 11 | 12 `takeUntilDestroyed` | LOW (well-guarded) |
| **MainLayoutComponent** | 9 | 7 guards | LOW |
| **DraftComponent** | ~7 | 7 guards | LOW |
| **TradeProposalComponent** | ~4 | 4 guards | LOW |

**Assessment**: The main components properly use `takeUntilDestroyed`. Most `.subscribe()` counts are in spec files (test subscriptions don't leak).

### 7.3 shareReplay Usage (LOW)

Only 2 files use `shareReplay`:
- `dashboard-data.service.ts`
- `trading.service.ts`

**Recommendation**: Add `shareReplay(1)` to shared observables in services that are subscribed by multiple components simultaneously (e.g., `game-data.service.ts` game detail observable).

---

## 8. Summary by Severity

### P0 - CRITICAL (2 items)

1. **Leaderboard N+1 queries**: 4 services iterate `team.getPlayers()` in loops, generating 1 extra SQL query per team. With 50 teams, that's 51 queries instead of 1-2.
2. **7 implicit EAGER @ManyToOne**: DraftPick (3) and Trade (4) entities eager-load related entities on every query, adding unnecessary JOINs to list operations.

### P1 - MAJOR (4 items)

3. **No database indexes**: Production PostgreSQL has no explicit indexes beyond PKs. Frequent WHERE columns (`status`, `creator_id`, `deleted_at`, `season`, `invitation_code`) need indexes.
4. **35/40 components use Default change detection**: Only 5 components use OnPush. Adding OnPush to the remaining 35 would reduce unnecessary re-renders.
5. **Unbounded list queries**: `findAllWithFetch()`, `findBySeason()` on Score/Team return full result sets without pagination.
6. **No @BatchSize on collections**: All `@OneToMany` collections lack batch-size hints, causing individual SELECTs per entity when accessed.

### P2 - MINOR (3 items)

7. **shareReplay underused**: Only 2 services use `shareReplay`. Shared observables should cache results.
8. **chart.js bundle impact**: ~200kB for dashboard charts. Could lazy-load chart component.
9. **Subscription leak risk low**: Most components properly guard subscriptions with `takeUntilDestroyed`.

---

## 9. Recommended Fix Plan

### Phase 1: Fix Leaderboard N+1 (P0, ~2h)
- Add `@BatchSize(size = 20)` to `Team.players`
- OR ensure leaderboard services use `findBySeasonWithFetch()` (JOIN FETCH on team.players)
- Verify with SQL logging (`spring.jpa.show-sql=true`)

### Phase 2: Explicit LAZY on @ManyToOne (P0, ~1h)
- Add `fetch = FetchType.LAZY` to all 7 bare `@ManyToOne` in DraftPick and Trade
- Verify no `LazyInitializationException` in existing tests
- Add `@EntityGraph` to repository queries that need the relations

### Phase 3: Add Database Indexes (P1, ~1h)
- Create Flyway migration adding indexes on: `games(status)`, `games(creator_id)`, `games(deleted_at)`, `games(invitation_code)`, `game_participants(game_id, user_id)`, `teams(season)`, `teams(game_id)`, `scores(player_id, season)`

### Phase 4: OnPush Change Detection (P1, ~3h)
- Add `ChangeDetectionStrategy.OnPush` to remaining 35 components
- Add `ChangeDetectorRef.markForCheck()` where needed
- High priority: MainLayoutComponent, GameHomeComponent, GameDetailComponent

### Phase 5: @BatchSize on Collections (P1, ~30min)
- Add `@BatchSize(size = 20)` to all `@OneToMany` collections
- Reduces N+1 to N/20+1 queries as a safety net
