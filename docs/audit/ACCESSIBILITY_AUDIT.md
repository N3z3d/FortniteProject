# Accessibility Audit Report

**Date**: 2026-02-17
**Ticket**: JIRA-AUDIT-014
**Scope**: Frontend Angular (critical user flows)

## Method

1. Static template scan for non-semantic clickable elements:
   - `rg --line-number "<(div|span|a)[^>]*\(click\)=" frontend/src/app --glob "*.html"`
2. Static template scan for icon buttons without explicit `aria-label`:
   - `rg --pcre2 --line-number "<button[^>]*mat-icon-button(?![^>]*aria-label)" frontend/src/app --glob "*.html"`

## Initial Findings (Phase 1)

### High - Keyboard inaccessibility risk on non-semantic clickable elements

| File | Issue | Impact |
|------|-------|--------|
| `frontend/src/app/features/dashboard/dashboard.component.html:209` | Click handler on `<div>` card (games) | Mouse-only interaction risk |
| `frontend/src/app/features/dashboard/dashboard.component.html:217` | Click handler on `<div>` card (leaderboard) | Keyboard users may be blocked |
| `frontend/src/app/features/dashboard/dashboard.component.html:225` | Click handler on `<div>` card (teams) | Missing role/tabindex/keydown pattern |
| `frontend/src/app/features/dashboard/dashboard.component.html:233` | Click handler on `<div>` card (trades) | Same issue on another nav path |
| `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.html:174` | Click handler on trade content `<div>` | Detail view not guaranteed keyboard reachable |
| `frontend/src/app/shared/components/home/home.component.html:7` | `<a (click)>` without `href` or button semantics | Screen reader + keyboard ambiguity |
| `frontend/src/app/shared/components/home/home.component.html:8` | `<a (click)>` without `href` or button semantics | Same pattern on primary navigation |
| `frontend/src/app/shared/components/home/home.component.html:210` | Footer `<a (click)>` without `href` | Same pattern in footer quick links |

### Medium - Icon buttons missing explicit accessible name

| File | Issue | Impact |
|------|-------|--------|
| `frontend/src/app/shared/components/main-layout/main-layout.component.html:47` | `mat-icon-button` uses tooltip, no explicit `aria-label` | Accessible name may be inconsistent |
| `frontend/src/app/shared/components/main-layout/main-layout.component.html:50` | Same pattern (settings button) | Same risk |
| `frontend/src/app/shared/components/main-layout/main-layout.component.html:80` | Same pattern (collapse button) | Same risk |
| `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.html:4` | Refresh icon button without explicit label | Discoverability reduced for AT users |
| `frontend/src/app/features/trades/trade-history/trade-history.component.html:163` | View-details icon button without explicit label | Action meaning can be unclear |

## Decision for next lot

1. Prioritize dashboard + trading clickable `<div>` elements:
   - add semantic button behavior (`role="button"`, `tabindex="0"`, keyboard handlers).
2. Normalize icon buttons:
   - ensure explicit `aria-label` on all `mat-icon-button` in critical flows.
3. Add/adjust targeted component specs to assert keyboard handlers and labels.

## Progress - 2026-02-17 (Lot 1 complete)

### Implemented

- Added keyboard semantics on dashboard navigation cards:
  - `frontend/src/app/features/dashboard/dashboard.component.html`
  - `frontend/src/app/features/dashboard/dashboard.component.ts`
- Added keyboard semantics on trading interactive cards/content:
  - `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.html`
  - `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.ts`

### Tests added/updated

- `frontend/src/app/features/dashboard/dashboard.component.spec.ts`
  - verifies nav cards expose `role="button"` + `tabindex="0"`.
  - verifies Enter key routing via `onNavigationCardKeydown`.
- `frontend/src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts`
  - verifies Enter key opens trade details via `onTradeCardKeydown`.
  - verifies unrelated keys are ignored.

### Validation

- Targeted specs green:
  - `npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include="src/app/features/dashboard/dashboard.component.spec.ts" --include="src/app/features/trades/components/trading-dashboard/trading-dashboard.component.spec.ts"`
- Full Karma headless green:
  - `npm --prefix frontend run test:ci -- --watch=false`

## Progress - 2026-02-17 (Lot 2 complete)

### Implemented

- Extended keyboard semantics on notification center:
  - `frontend/src/app/shared/components/notification-center/notification-center.component.html`
  - `frontend/src/app/shared/components/notification-center/notification-center.component.ts`
- Added explicit `aria-label` on critical `mat-icon-button` in notification center.
- Added safe toast rendering through `Renderer2` in notification center to avoid HTML injection via notification message payload.
- Extended keyboard semantics on admin error journal rows and explicit `aria-label` on detail action:
  - `frontend/src/app/features/admin/error-journal/error-journal.component.html`
  - `frontend/src/app/features/admin/error-journal/error-journal.component.ts`

### Tests added/updated

- `frontend/src/app/shared/components/notification-center/notification-center.component.spec.ts`
  - verifies notification toggle button accessible name.
  - verifies Enter key marks a notification as read.
  - verifies non-action key is ignored.
  - verifies notification row has `role="button"` and `tabindex="0"`.
  - verifies malicious toast payload is rendered as plain text.
- `frontend/src/app/features/admin/error-journal/error-journal.component.spec.ts`
  - verifies Enter key opens detail dialog.
  - verifies non-action key is ignored.
  - verifies error rows are keyboard focusable.
  - verifies detail icon button has explicit `aria-label`.

### Validation

- Targeted specs green:
  - `npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include="src/app/shared/components/notification-center/notification-center.component.spec.ts" --include="src/app/features/admin/error-journal/error-journal.component.spec.ts"`
- Extended lot-2 safety run green:
  - `npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include="src/app/shared/components/notification-center/notification-center.component.spec.ts" --include="src/app/features/admin/error-journal/error-journal.component.spec.ts" --include="src/app/shared/components/main-layout/main-layout.component.spec.ts"`

## Progress - 2026-02-17 (Lot 3 complete)

### Implemented

- Normalized explicit accessible names on critical icon-only controls in main layout:
  - `frontend/src/app/shared/components/main-layout/main-layout.component.html`
  - profile / settings buttons
  - sidebar collapse / create game buttons
- Added keyboard semantics on game cards/items:
  - `frontend/src/app/features/game/game-home/game-home.component.html`
  - `frontend/src/app/features/game/game-home/game-home.component.ts`
  - `frontend/src/app/shared/components/main-layout/main-layout.component.html`
  - `frontend/src/app/shared/components/main-layout/main-layout.component.ts`
  - both now expose `role="button"`, `tabindex="0"`, Enter/Space handlers, explicit `aria-label`

### Tests added/updated

- `frontend/src/app/shared/components/main-layout/main-layout.component.spec.ts`
  - verifies explicit `aria-label` on profile/settings icon buttons.
  - verifies explicit `aria-label` on sidebar icon controls.
- `frontend/src/app/features/game/game-home/game-home.component.spec.ts`
  - verifies game cards are keyboard focusable.
  - verifies Enter key triggers card selection.

### Validation

- Targeted lot-3 run green:
  - `npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include="src/app/shared/components/main-layout/main-layout.component.spec.ts" --include="src/app/features/game/game-home/game-home.component.spec.ts" --include="src/app/shared/components/notification-center/notification-center.component.spec.ts" --include="src/app/features/admin/error-journal/error-journal.component.spec.ts"`
  - result: `TOTAL: 136 SUCCESS`

## Manual validation protocol (NVDA/VoiceOver)

1. Dashboard cards:
   - Tab through cards.
   - Press Enter and Space on each card.
   - Expected: navigation triggers once, focus remains visible.
2. Trading dashboard cards:
   - Tab to a trade card.
   - Press Enter and Space.
   - Expected: trade details open from keyboard.
2bis. Games cards/items:
   - Games home page: Tab to a game card, Enter/Space selects the game.
   - Left sidebar: Tab to a game item, Enter/Space opens game context.
3. Notification center:
   - Open with keyboard.
   - Navigate notifications with Tab.
   - Press Enter/Space on an item.
   - Expected: item marked as read, close/delete actions announced with proper names.
4. Error journal:
   - Tab to a row.
   - Press Enter/Space.
   - Expected: detail dialog opens from keyboard.
5. Main layout icon controls:
   - Tab to profile/settings/collapse/create-game icon buttons.
   - Screen reader must announce explicit names (not only "button").

## Manual validation result - 2026-02-17

- Manual tester feedback: "C'est parfait !" after guided validation.
- Keyboard navigation checks validated:
  - dashboard cards
  - trading cards
  - games cards/items (home + sidebar)
  - notification center items
  - error journal rows
  - main-layout icon controls with explicit labels
- No blocking accessibility issue reported on the audited scope.

## Residual scope

- Contrast audit remains a separate optional pass (no blocker for current ticket scope).
