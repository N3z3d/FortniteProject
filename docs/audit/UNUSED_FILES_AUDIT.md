# Unused Files Audit Report - JIRA-AUDIT-001

**Date**: 2026-02-16
**Scope**: Backend (Java) + Frontend (TypeScript)
**Method**: Cross-reference all imports/references for every source file

## Summary

| Category | Backend | Frontend | Total |
|----------|---------|----------|-------|
| **(C) Orphan** - no references anywhere | 6 | 4 | 10 |
| **(B) Test-only** - referenced only by spec files | 1 | 0 | 1 |
| **Total candidates for deletion** | **7** | **4** | **11** |

---

## Backend Orphan Files

### Category C - Orphan (delete recommended)

| # | File | Lines | Evidence | Recommendation |
|---|------|-------|----------|----------------|
| 1 | `src/main/java/.../adapter/out/AdapterOutMarker.java` | ~5 | No imports found anywhere. Marker class for package scanning. | **Delete** - unused marker |
| 2 | `src/main/java/.../dto/DraftStatus.java` | ~30 | Not imported by any main source file. Has `fromDraft()` factory never called. Domain `DraftStatus` enum exists separately. | **Delete** - superseded by domain model |
| 3 | `src/main/java/.../dto/leaderboard/LeaderboardEntryDto.java` | ~20 | Not imported anywhere. Superseded by `LeaderboardEntryDTO` and `PlayerLeaderboardEntryDTO`. | **Delete** - replaced |
| 4 | `src/main/java/.../repository/PlayerAliasRepository.java` | ~10 | Not imported/autowired anywhere. JPA repo for unused entity. | **Delete** - dead repository |
| 5 | `src/main/java/.../model/PlayerAlias.java` | ~30 | Only referenced by `PlayerAliasRepository`. Since the repo is orphan, this entity is also orphan. | **Delete** - dead entity |

### Category B - Test-only (review recommended)

| # | File | Lines | Evidence | Recommendation |
|---|------|-------|----------|----------------|
| 6 | `src/main/java/.../dto/DraftSelectionRequest.java` | ~15 | Only referenced by `DraftSelectionRequestTest.java`. Never used in any controller or service. | **Delete** both DTO + test |

### Associated Test Files to Delete

| # | Test File | Tests Class That... |
|---|-----------|---------------------|
| 7 | `src/test/java/.../dto/DraftSelectionRequestTest.java` | Tests orphan `DraftSelectionRequest` |

---

## Frontend Orphan Files

### Category C - Orphan (delete recommended)

| # | File | Evidence | Recommendation |
|---|------|----------|----------------|
| 1 | `frontend/src/app/core/core.module.ts` | `CoreModule` not imported by any file. Project uses standalone components. | **Delete** - legacy NgModule |
| 2 | `frontend/src/app/features/draft/components/player-selection/player-selection.component.ts` | `PlayerSelectionComponent` not imported or routed anywhere. Only referenced by own `.spec.ts`. | **Delete** + delete spec |
| 3 | `frontend/src/app/shared/components/home/home.component.ts` | `HomeComponent` (shared) not imported or routed anywhere. Only referenced by own `.spec.ts`. Game feature uses `GameHomeComponent` instead. | **Delete** + delete spec |
| 4 | `frontend/src/app/features/teams/team-list/team-list.ts` | `TeamList` (singular) not imported anywhere. The active version is `teams-list/teams-list.component.ts` (plural). Only referenced by own `.spec.ts` and `.navigation.spec.ts`. | **Delete** + delete specs |

### Associated Frontend Test Files to Delete

| # | Test File | Tests Component That... |
|---|-----------|------------------------|
| 5 | `frontend/src/app/features/draft/components/player-selection/player-selection.component.spec.ts` | Tests orphan PlayerSelectionComponent |
| 6 | `frontend/src/app/shared/components/home/home.component.spec.ts` | Tests orphan HomeComponent |
| 7 | `frontend/src/app/features/teams/team-list/team-list.spec.ts` | Tests orphan team-list |
| 8 | `frontend/src/app/features/teams/team-list/team-list.navigation.spec.ts` | Tests orphan team-list |

---

## Files Investigated but Confirmed Active

These files were flagged as suspects but are confirmed in use:

| File | Referenced By |
|------|--------------|
| `*.module.ts` files (auth, dashboard, game, leaderboard, teams, trades) | `app.routes.ts` via `loadChildren` |
| `fortnite-player.interface.ts` | `game-home.component.ts` (via routing module) |
| `trade-timeline.service.ts` | `trade-details.component.ts` |
| `player-stats.service.ts` | `home.component.ts` (shared) - but home is orphan too |
| `dashboard-formatting.service.ts` | `dashboard.component.ts` |
| `mock-data.service.ts` | `dashboard-data.service.ts` |
| `websocket.service.ts` | `trade-list.component.ts` (via routing module) |
| `data-source-indicator.ts` | `main-layout.component.ts` |
| `route.animations.ts` | `main-layout.component.ts` |
| `confirm-dialog.component.ts` | `game-detail-actions.service.ts`, `draft.component.ts` |
| `games.resolver.ts` | `app.routes.ts` |
| `data-source.strategy.ts` | `dashboard.facade.ts`, `leaderboard.repository.ts` |

**Note**: `player-stats.service.ts` is only used by `shared/home/home.component.ts` which is itself orphan. If HomeComponent is deleted, PlayerStatsService becomes orphan too. However, it may have future utility, so it's left as ACTIVE for now.

---

## Deletion Plan

### Step 1 - Backend (7 files)

```
DELETE src/main/java/.../adapter/out/AdapterOutMarker.java
DELETE src/main/java/.../dto/DraftStatus.java
DELETE src/main/java/.../dto/DraftSelectionRequest.java
DELETE src/main/java/.../dto/leaderboard/LeaderboardEntryDto.java
DELETE src/main/java/.../repository/PlayerAliasRepository.java
DELETE src/main/java/.../model/PlayerAlias.java
DELETE src/test/java/.../dto/DraftSelectionRequestTest.java
```

### Step 2 - Frontend (8 files)

```
DELETE frontend/src/app/core/core.module.ts
DELETE frontend/src/app/features/draft/components/player-selection/player-selection.component.ts
DELETE frontend/src/app/features/draft/components/player-selection/player-selection.component.spec.ts
DELETE frontend/src/app/shared/components/home/home.component.ts
DELETE frontend/src/app/shared/components/home/home.component.spec.ts
DELETE frontend/src/app/features/teams/team-list/team-list.ts
DELETE frontend/src/app/features/teams/team-list/team-list.spec.ts
DELETE frontend/src/app/features/teams/team-list/team-list.navigation.spec.ts
```

### Step 3 - Verify

- Run `mvn spotless:apply && mvn test` (backend)
- Run `npx ng test --watch=false --browsers=ChromeHeadless` (frontend)
- Confirm 0 regressions

---

## Checklist

- [x] All backend Java files scanned for cross-references
- [x] All frontend TypeScript files scanned for import references
- [x] Routing configurations checked (app.routes.ts, *-routing.module.ts)
- [x] Spring auto-detection considered (@Service, @Entity, @Component, etc.)
- [x] Angular lazy loading considered (loadComponent, loadChildren)
- [x] Each orphan verified with precise grep searches
- [x] Associated test files identified for deletion
- [x] Deletion plan prioritized and verified
