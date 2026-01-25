# Clean Code Violations Report

Date: 2026-01-25

## Summary

- **Backend (Java):** 0 classes over 500 lines ✅
- **Frontend (TypeScript):** 12 files over 500 lines ❌

## CRITICAL Violations (>1000 lines)

### 1. translation.service.ts - 5353 lines ⚠️ URGENT
**Location:** `frontend/src/app/core/services/translation.service.ts`
**Current:** 5353 lines
**Limit:** 500 lines
**Violation:** 10.7x over limit

**Root Cause:** All translations are hardcoded inside the service
**Impact:** Unmaintainable, violates SRP, impossible to test properly

**Recommended Fix:**
1. Extract translations to separate JSON files per language (en.json, fr.json, es.json, pt.json)
2. Keep only translation logic in service (<200 lines)
3. Load translations from JSON files dynamically

**Priority:** P0 - Must be fixed immediately

---

## HIGH Violations (600-700 lines)

### 2. trade-proposal.component.ts - 627 lines
**Location:** `frontend/src/app/features/trades/components/trade-proposal/trade-proposal.component.ts`
**Violation:** 1.25x over limit

### 3. trade-details.component.ts - 623 lines
**Location:** `frontend/src/app/features/trades/components/trade-details/trade-details.component.ts`
**Violation:** 1.25x over limit

### 4. trade-history.component.ts - 611 lines
**Location:** `frontend/src/app/features/trades/trade-history/trade-history.component.ts`
**Violation:** 1.22x over limit

**Recommended Fix for Trade Components:**
- Extract reusable trade logic to TradeService
- Create smaller sub-components for sections
- Move validation logic to separate validators
- Extract display logic to presentation components

---

## MEDIUM Violations (500-600 lines)

### 5. game.service.ts - 594 lines
**Location:** `frontend/src/app/features/game/services/game.service.ts`
**Violation:** 1.19x over limit

**Fix:** Split into GameQueryService + GameCommandService (CQRS pattern)

### 6. accessible-error-handler.component.ts - 590 lines
**Location:** `frontend/src/app/shared/components/accessible-error-handler/accessible-error-handler.component.ts`
**Violation:** 1.18x over limit

**Fix:** Extract error handling strategies to separate classes

### 7. draft.component.ts - 584 lines
**Location:** `frontend/src/app/features/draft/draft.component.ts`
**Violation:** 1.17x over limit

**Fix:** Split into DraftOrchestrator + smaller components for UI sections

### 8. premium-interactions.service.ts - 550 lines
**Location:** `frontend/src/app/shared/services/premium-interactions.service.ts`
**Violation:** 1.10x over limit

**Fix:** Split by animation type (ripple, glow, pulse, etc.) into separate classes

### 9. trade-detail.component.ts - 543 lines
**Location:** `frontend/src/app/features/trades/trade-detail/trade-detail.component.ts`
**Violation:** 1.09x over limit

### 10. game-detail.component.ts - 530 lines
**Location:** `frontend/src/app/features/game/game-detail/game-detail.component.ts`
**Violation:** 1.06x over limit

### 11. team-detail.component.ts - 517 lines
**Location:** `frontend/src/app/features/teams/team-detail/team-detail.component.ts`
**Violation:** 1.03x over limit

### 12. dashboard.component.ts - 501 lines
**Location:** `frontend/src/app/features/dashboard/dashboard.component.ts`
**Violation:** 1.002x over limit (barely over)

---

## Recommended Refactoring Order

1. **IMMEDIATE:** translation.service.ts (extract to JSON files)
2. **HIGH:** Trade components (extract shared logic)
3. **MEDIUM:** game.service.ts (apply CQRS)
4. **MEDIUM:** draft.component.ts (componentize)
5. **MEDIUM:** Detail components (extract sub-components)

## Estimated Effort

- Translation service extraction: 2-4h
- Trade components refactoring: 4-6h
- Service splitting (CQRS): 2-3h each
- Component splitting: 1-2h each

**Total:** 20-35h for all violations

---

## Methods Over 50 Lines

⚠️ **TO BE ANALYZED** - Requires detailed method analysis with AST parser
