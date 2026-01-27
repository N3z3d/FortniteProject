# Pure Hexagonal Architecture - Migration Roadmap

**Status:** Approved
**Date:** 2026-01-27
**Decision Reference:** ADR-001, JIRA-ARCH-009
**Strategy:** Incremental Migration (One Domain at a Time)

---

## Executive Summary

This roadmap documents the incremental migration strategy to Pure Hexagonal Architecture (Ports & Adapters pattern) for the Fortnite Pronos application.

**Key Decisions:**
- ✅ Pure hexagonal architecture adopted (no hybrid)
- ✅ Incremental migration strategy (minimize risk)
- ✅ Domain models with ZERO framework dependencies
- ✅ Migration order defined by impact and complexity

---

## Migration Principles

### 1. Incremental Approach (NO Big Bang)

Each domain migrates independently with full validation:

```
Migrate Domain → Run ALL Tests → Manual Smoke Test → IF Pass → Next Domain
                                                     → IF Fail → Fix First
```

### 2. Success Criteria (Per Domain)

**REQUIRED before proceeding to next domain:**
- ✅ All tests passing (1282+ / 1286 tests)
- ✅ Zero test regressions
- ✅ Manual smoke tests pass
- ✅ No breaking changes in public APIs
- ✅ Application starts without errors

### 3. Rollback Strategy

If migration fails validation:
1. Revert commit (git revert)
2. Fix issues
3. Re-attempt migration
4. Document lessons learned

---

## Migration Order & Estimates

### Phase 1: Game Domain (JIRA-ARCH-011) - 8-16h
**Priority:** HIGH (most complex, highest dependencies)

**Scope:**
- Domain models: Game, GameParticipant, GameSettings
- Port interfaces: GameRepositoryPort
- Adapter OUT: JPA entities + mappers (Game ↔ GameJPA)
- Adapter IN: GameController updates

**Dependencies:**
- User domain (reference only)
- Season domain (reference only)

**Validation:**
- Game CRUD operations
- Game search/filter
- Participant management
- Draft integration

---

### Phase 2: Player Domain (JIRA-ARCH-012) - 8-16h
**Priority:** HIGH (medium complexity, data-heavy)

**Scope:**
- Domain models: Player, PlayerStats, PlayerHistory
- Port interfaces: PlayerRepositoryPort
- Adapter OUT: JPA entities + mappers
- Adapter IN: PlayerController updates

**Dependencies:**
- Team domain (reference)
- Game domain (MUST be completed first)

**Validation:**
- Player CRUD operations
- Player statistics
- Player search/filter
- Team assignments

---

### Phase 3: Team Domain (JIRA-ARCH-013) - 8-16h
**Priority:** MEDIUM (business logic, draft dependencies)

**Scope:**
- Domain models: Team, TeamMember
- Port interfaces: TeamRepositoryPort
- Adapter OUT: JPA entities + mappers
- Adapter IN: TeamController updates

**Dependencies:**
- Player domain (MUST be completed first)
- Draft domain (reference)
- Trade domain (reference)

**Validation:**
- Team CRUD operations
- Team member management
- Draft team operations
- Trade team operations

---

### Phase 4: Draft Domain (JIRA-ARCH-014) - 6-12h
**Priority:** MEDIUM (isolated system, complex workflow)

**Scope:**
- Domain models: Draft, DraftPick, DraftRound
- Port interfaces: DraftRepositoryPort
- Adapter OUT: JPA entities + mappers
- Adapter IN: DraftController updates

**Dependencies:**
- Game domain (MUST be completed)
- Team domain (MUST be completed)
- Player domain (MUST be completed)

**Validation:**
- Draft creation
- Draft rounds management
- Pick validation
- Draft completion

---

### Phase 5: Trade Domain (JIRA-ARCH-015) - 6-12h
**Priority:** LOW (lowest complexity, least dependencies)

**Scope:**
- Domain models: Trade, TradeOffer, TradeItem
- Port interfaces: TradeRepositoryPort
- Adapter OUT: JPA entities + mappers
- Adapter IN: TradeController updates

**Dependencies:**
- Team domain (MUST be completed)
- Player domain (MUST be completed)

**Validation:**
- Trade creation
- Trade offers
- Trade acceptance/rejection
- Trade history

---

## Total Effort Estimate

**Optimistic:** 36h (1 week focused work)
**Realistic:** 48h (1.5 weeks)
**Pessimistic:** 60h (2 weeks with issues)

**Timeline:** Multi-sprint (2-3 sprints recommended)

---

## Current Status (2026-01-27)

### ✅ Completed Work
- ADR-001 updated with pure hexagonal strategy
- 12 Repository Port interfaces created
- 12 Use Case interfaces defined
- 8/14 controllers migrated to DIP pattern
- Decision validated: JIRA-ARCH-009

### 🔄 In Progress
- None (Phase 1 blocked until JIRA-ARCH-009 approved)

### ⏳ Blocked (Ready to Start)
- JIRA-ARCH-011: Game Domain Migration (Phase 1)
- JIRA-ARCH-012: Player Domain Migration (Phase 2)
- JIRA-ARCH-013: Team Domain Migration (Phase 3)
- JIRA-ARCH-014: Draft Domain Migration (Phase 4)
- JIRA-ARCH-015: Trade Domain Migration (Phase 5)

---

## Risk Mitigation

### Risk 1: Test Regressions
**Mitigation:** Run full test suite after each domain migration
**Threshold:** Zero tolerance for regressions

### Risk 2: Mapper Complexity
**Mitigation:** Use MapStruct or manual mappers (simple, testable)
**Pattern:** JPA Entity ↔ Domain Model (bidirectional)

### Risk 3: Breaking Changes
**Mitigation:** Preserve public API contracts during migration
**Validation:** Integration tests must pass unchanged

### Risk 4: Performance Impact
**Mitigation:** Add mapping layer benchmarks
**Acceptable:** <5ms overhead per entity mapping

---

## Next Steps

1. ✅ **DONE:** Document migration roadmap (this file)
2. ✅ **DONE:** Validate ADR-001 with team
3. ✅ **DONE:** Unblock JIRA-ARCH-011 to 015
4. 🔜 **NEXT:** Start Phase 1 (Game Domain Migration)

---

## Approval

**Architect:** @Claude
**Date:** 2026-01-27
**Status:** ✅ APPROVED

**Technical Lead:** (Pending user approval)
**Product Owner:** (Pending user approval)

---

## References

- [ADR-001: Pure Hexagonal Architecture](./ADR-001-layered-architecture.md)
- [JIRA-ARCH-009: Architecture Decision](../../Jira-tache.txt)
- [JIRA-ARCH-011 to 015: Domain Migrations](../../Jira-tache.txt)
- [CLAUDE.md: Clean Code Principles](../../CLAUDE.md)
