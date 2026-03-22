# Story sprint13-fix-ux-visuel: Fix UX/Visual Bugs BUG-07..13

Status: done

<!-- METADATA
  story_key: sprint13-fix-ux-visuel
  branch: story/sprint13-fix-ux-visuel
  sprint: Sprint 13
  Note: BUG-13 must be fixed FIRST (visible to all users — git log before touching)
-->

## Story

As a Fortnite Pronos user (Thibaut or Teddy),
I want the app to display information correctly and not show confusing banners or broken UX,
so that browsing, using the catalogue, and viewing the leaderboard feel polished and reliable.

## Acceptance Criteria

### BUG-13 — Visual regression on game leaderboard page (FIX FIRST)
1. The game leaderboard page (`/games/:id/leaderboard`) uses the gaming theme (dark background, gaming colors, `Rajdhani` font for title, rank medal colors) — visually consistent with the rest of the app
2. Screenshots before/after included in completion notes
3. `git log -- frontend/src/app/features/leaderboard/game-leaderboard-page/` checked before any changes

### BUG-07 — "Catalogue" button visible in unexpected context
4. After investigation: if the Catalogue button is intentionally in global nav (sprint12-nav-cleanup decision), document WHY the user found it unexpected and add an i18n-friendly tooltip or context label. If it should be hidden on certain routes, add a route-based condition.

### BUG-08 — Region filter → "Aucun joueur trouvé"
5. Selecting a region in the catalogue dropdown returns correct results for all valid regions (EU, NAW, BR, ASIA, OCE, NAC, ME)
6. When search term + region filter active simultaneously: region filter is applied correctly
7. Backend 400 errors (invalid region) do NOT silently return empty list — a user-visible error or fallback message is shown

### BUG-09 — After page refresh: "initialisation" + "connexion instable" shown
8. On page refresh of the snake-draft page, the "Connexion instable" banner does NOT flash immediately (only appears after genuine connection loss, with a minimum delay of 3s after initial mount)
9. The "Initialisation..." text from `DataSourceIndicator` is not shown on pages that do not use the data-source-strategy (draft pages)

### BUG-11 — Draft type not displayed
10. The game-detail page shows the draft type (SNAKE / SIMULTANEOUS) in the draft section when `game.status === 'DRAFTING'`
11. The "Rejoindre le draft" button navigates to the correct route (`/draft/snake` or `/draft/simultaneous`) based on the actual `draftMode` field
12. Frontend `Game` interface has optional `draftMode?: 'SNAKE' | 'SIMULTANEOUS'` field

### BUG-12 — Admin panel many visible errors
13. If one API call in the admin dashboard fails, the remaining sections still render (partial success)
14. Each admin dashboard section shows its own loading/error state independently

## Tasks / Subtasks

- [ ] Task 1: BUG-13 — Restyle game-leaderboard-page (AC: #1, #2, #3)
  - [ ] 1.1: `git log -- frontend/src/app/features/leaderboard/game-leaderboard-page/` before changes
  - [ ] 1.2: Update `game-leaderboard-page.component.scss` with gaming theme: dark card background, `Rajdhani` title font, rank medal colors (gold/silver/bronze), positive/negative delta colors from CSS vars, `--gaming-primary` header row
  - [ ] 1.3: Reference `simple-leaderboard.component.scss` (the premium leaderboard) for style inspiration but do NOT import `@import '../../shared/styles/mixins'` — use direct CSS variables (`var(--gaming-primary, ...)` with fallbacks)
  - [ ] 1.4: Add screenshot notes to completion section (before/after)
  - [ ] 1.5: Vitest unit test: render 3 entries, verify `rank-cell` has `gold` class for rank 1

- [ ] Task 2: BUG-07 — Investigate and fix Catalogue context (AC: #4)
  - [ ] 2.1: Read `main-layout.component.html` (lines 22-35) and check which `<nav>` block the Catalogue button is in
  - [ ] 2.2: Check `sprint12-nav-cleanup` story decision: Catalogue in global-nav was intentional. If the button is only in `global-navigation` (no condition), document as "intended" and add a clear label/icon that distinguishes it from game-specific nav items — no code change needed if intentional
  - [ ] 2.3: If Catalogue appears as a duplicate somewhere (both global-nav AND context-nav), remove the duplicate

- [ ] Task 3: BUG-08 — Fix region filter in catalogue (AC: #5, #6, #7)
  - [ ] 3.1: In `PlayerCatalogueService.loadCataloguePlayers()`, when `params.search` AND `params.region` are both set, pass `region` as an additional query param to the search endpoint. If the search endpoint `/catalogue/search` doesn't support `region`, fall back to client-side filter (already done in `filterPlayers`) — verify this path works
  - [ ] 3.2: Replace `catchError(() => of([]))` with `catchError((err) => { log the error; return of([]); })` AND surface the error to the component via a separate `error$` Subject or error flag returned alongside the empty array (use `{ players: [], error: true }` tuple or split Observable)
  - [ ] 3.3: In `PlayerCataloguePageComponent`, when `getPlayers()` errors (not just returns []), show a user-visible error message instead of "Aucun joueur trouvé"
  - [ ] 3.4: Add Vitest test: when backend returns 400 (bad region), component shows error state not empty list

- [ ] Task 4: BUG-09 — Fix false "Connexion instable" + "Initialisation" banners (AC: #8, #9)
  - [ ] 4.1: In `SnakeDraftPageComponent`, replace `wsConnected = true` initial state with a `wsNeverConnected = true` guard flag. Only show the WS banner when `!wsConnected && !wsNeverConnected`. Set `wsNeverConnected = false` after first truthy emission from `isConnected$`
  - [ ] 4.2: Alternatively (simpler): in `trackConnectionStatus()`, pipe `isConnected$` through `skip(1)` OR add a 3s `debounceTime(3000)` before updating `wsConnected = false`, so the initial false-to-true transition doesn't flash the banner
  - [ ] 4.3: For the `DataSourceIndicator` "Initialisation..." — check if the component should be hidden in the draft layout context. If `DataSourceStrategy` resolves to DATABASE quickly, the flash is acceptable. If not, add a minimum display time of 500ms (only show if INITIALIZING state lasts > 500ms) via `debounceTime(500)` filter in the component
  - [ ] 4.4: Vitest test: `SnakeDraftPageComponent` — `wsConnected` banner is NOT shown when `isConnected$` starts false then emits true within 1s

- [ ] Task 5: BUG-11 — Show draft type in game-detail (AC: #10, #11, #12)
  - [ ] 5.1: Add `draftMode?: 'SNAKE' | 'SIMULTANEOUS'` to `Game` interface in `frontend/src/app/features/game/models/game.interface.ts`
  - [ ] 5.2: In `game-detail.component.html`, in the `draft-section`, add a `<span>` or badge showing the draft type label (`t.t('games.detail.draftType.snake')` or `t.t('games.detail.draftType.simultaneous')`)
  - [ ] 5.3: Update the "Rejoindre le draft" button `[routerLink]` to use `game.draftMode === 'SIMULTANEOUS' ? ['...', 'draft', 'simultaneous'] : ['...', 'draft', 'snake']`; default to snake when `draftMode` is undefined (backward compat)
  - [ ] 5.4: Add i18n keys to all 4 translation files for `games.detail.draftType.snake` and `games.detail.draftType.simultaneous`
  - [ ] 5.5: Vitest test in `game-detail.component.spec.ts`: when `game.draftMode === 'SIMULTANEOUS'`, button navigates to `/draft/simultaneous`

- [x] Task 6: BUG-12 — Admin dashboard resilient loading (AC: #13, #14)
  - [x] 6.1: In `admin-dashboard.component.ts`, replace single `forkJoin({...})` with individual subscriptions using `catchError(() => of(null))` per call, so partial failures don't block the whole dashboard
  - [x] 6.2: Add per-section error flags (e.g., `healthError = false`, `alertsError = false`) and update each section in the template to show a small "Erreur de chargement" inline instead of blocking the whole page
  - [x] 6.3: Verify that `getAlerts()` response mapping `r.data` handles undefined gracefully (add `?? []` fallback)
  - [x] 6.4: Vitest test: when one API call fails, other sections still render

## Dev Notes

### BUG Order — IMPORTANT
Fix BUG-13 FIRST as instructed in sprint-status.yaml: `# BUG-13 EN PREMIER : régression visuelle classement (visible tous joueurs)`

### Key File Locations

| Bug | File to modify |
|-----|----------------|
| BUG-13 | `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.scss` |
| BUG-07 | `frontend/src/app/shared/components/main-layout/main-layout.component.html` (read-only, probably no change) |
| BUG-08 | `frontend/src/app/features/catalogue/services/player-catalogue.service.ts` + `player-catalogue-page.component.ts` |
| BUG-09 | `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` + `data-source-indicator/data-source-indicator.ts` |
| BUG-11 | `frontend/src/app/features/game/models/game.interface.ts` + `game-detail.component.html` + `game-detail.component.ts` |
| BUG-12 | `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.ts` + `.html` |

### BUG-13 — Context
The `GameLeaderboardPageComponent` (`/games/:id/leaderboard`) has minimal SCSS:
- Uses `var(--gaming-gray-light, #888)` fallback colors — acceptable
- Uses plain `border-collapse: collapse` table — no gaming theme
- Reference: `simple-leaderboard.component.scss` has the premium gaming styles (gradient bg, particle animation, gold rank colors)
- The fix should add gaming card styling WITHOUT `@import '../../shared/styles/mixins'` (use direct CSS vars with fallbacks instead)

### BUG-07 — Context
Sprint12-nav-cleanup intentionally added Catalogue to global nav: `Logo | Catalogue | 👤 Profil`. Check if Catalogue is ALSO in game-detail context-nav (duplication). If no duplication, the "bug" is just a UX surprise — document and close without code change.

### BUG-08 — Root Cause
Backend `PlayerController.getCatalogue()` calls `PlayerRegion.valueOf(region.toUpperCase())` which throws `IllegalArgumentException` for unknown values. Frontend `catchError(() => of([]))` silently returns empty. Valid regions: `EU, NAW, BR, ASIA, OCE, NAC, ME`. No `NAE`, no `GLOBAL`. If the catalogue dropdown somehow shows `GLOBAL` (from draft context leaking), selecting it triggers 400 → empty list.

### BUG-09 — Root Cause
`WebSocketService.connectionStatus$` is a `BehaviorSubject(false)` (see `websocket.service.ts:62`). On page load, `trackConnectionStatus()` subscribes and immediately receives `false` → sets `wsConnected = false` → banner shows. The `wsConnected = true` initial field value is immediately overwritten.

Fix via `debounceTime(3000)` on false emissions only:
```typescript
// Only debounce false values — true should apply immediately
private trackConnectionStatus(): void {
  this.wsService.isConnected$.pipe(
    switchMap(connected => connected
      ? of(connected)
      : of(connected).pipe(delay(3000))
    ),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(connected => { this.wsConnected = connected; });
}
```
Or simpler: use `skip(1)` to skip the first BehaviorSubject emission on mount.

### BUG-11 — Context
Backend `GameDto.draftMode` field exists (type `DraftMode` enum: `SNAKE | SIMULTANEOUS`). It IS serialized in API responses. Frontend `Game` interface does not have the field — so `game.draftMode` is always `undefined`. Game-detail hardcodes `[routerLink]="['/games', game.id, 'draft', 'snake']"`.

### BUG-12 — Context
`forkJoin` fails entirely when ANY of 6 calls fails. Use `forkJoin({ ..., alerts: adminService.getAlerts().pipe(catchError(() => of([]))) })` pattern instead. Each section gets its data independently.

### CSS Variables Reference (available globally)
```css
--gaming-primary: #6c5ce7
--gaming-accent: #fdcb6e
--gaming-success: #2ecc71
--gaming-error: #e74c3c
--gaming-gray-light: #888
--gradient-dark: linear-gradient(...)
```

### Angular / Testing Patterns
- **DO NOT** use `fakeAsync()` + `tick()` with Vitest — use `async/await` with `of()` sync observables (Pattern A) or `vi.useFakeTimers()` (Pattern B for debounceTime)
- **DO NOT** use `@import 'mixins'` in SCSS — use CSS variables directly with fallbacks: `var(--gaming-primary, #6c5ce7)`
- **Test run**: `cd frontend && npm run test:vitest`
- **Baseline**: 2255/2255 passing — 0 failures (post sprint13-fix-534-tests)

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend 400 for unknown regions (GLOBAL, NAE) is intentional — frontend should handle it gracefully
- [KNOWN] `DataSourceStrategy` INITIALIZING state is expected on every page load — the flash is brief (< 1s when backend responds) — only fix if it persists on draft pages for > 1s

### Project Structure Notes

All frontend: `frontend/src/app/features/`
- Leaderboard: `features/leaderboard/game-leaderboard-page/`
- Catalogue: `features/catalogue/pages/player-catalogue-page/` + `features/catalogue/services/`
- Draft: `features/draft/components/snake-draft-page/`
- Admin: `features/admin/admin-dashboard/`
- Game: `features/game/game-detail/` + `features/game/models/game.interface.ts`
- Main layout: `shared/components/main-layout/`

### References

- Sprint 12 retro bugs: `_bmad-output/implementation-artifacts/sprint12-retro-2026-03-20.md` §5
- Sprint status: `_bmad-output/implementation-artifacts/sprint-status.yaml` lines 927-938
- Backend PlayerRegion enum: `src/main/java/com/fortnite/pronos/domain/game/model/PlayerRegion.java`
- Backend GameDto with draftMode: `src/main/java/com/fortnite/pronos/dto/GameDto.java:68`
- WebSocketService BehaviorSubject(false): `frontend/src/app/core/services/websocket.service.ts:62`
- PlayerCatalogueService catchError: `frontend/src/app/features/catalogue/services/player-catalogue.service.ts:35`
- Game leaderboard (premium ref): `frontend/src/app/features/leaderboard/simple-leaderboard.component.scss`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List

- `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.scss` (BUG-13)
- `frontend/src/app/features/catalogue/services/player-catalogue.service.ts` (BUG-08)
- `frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.ts` (BUG-08)
- `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts` (BUG-09)
- `frontend/src/app/features/game/models/game.interface.ts` (BUG-11)
- `frontend/src/app/features/game/game-detail/game-detail.component.html` (BUG-11)
- `frontend/src/app/features/game/game-detail/game-detail.component.ts` (BUG-11)
- `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.ts` (BUG-12)
- `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.html` (BUG-12)
- `frontend/src/assets/i18n/fr.json` (BUG-11 i18n keys)
- `frontend/src/assets/i18n/en.json` (BUG-11 i18n keys)
- Potentially: `frontend/src/app/shared/components/main-layout/main-layout.component.html` (BUG-07)
- Potentially: `frontend/src/app/shared/components/data-source-indicator/data-source-indicator.ts` (BUG-09)
