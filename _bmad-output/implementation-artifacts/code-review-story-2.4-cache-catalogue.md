# Code Review — Story 2.4: Cache catalogue et disponibilite pendant draft

**Date:** 2026-02-26
**Reviewer:** claude-sonnet-4-6 (adversarial)
**Story file:** `_bmad-output/implementation-artifacts/2-4-cache-catalogue-et-disponibilite-pendant-draft.md`
**Verdict:** APPROVED (after fixes)

---

## Scope

Files reviewed:
- `src/main/java/com/fortnite/pronos/config/CacheConfig.java`
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java`
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupService.java`
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceCacheTest.java`

---

## Findings

### F-001 — HIGH — Cache effectiveness untested end-to-end [FIXED]

**Location:** `PlayerCatalogueServiceCacheTest.java`

**Problem:** The original test suite only used annotation reflection (`Method.getAnnotation(Cacheable.class)`) to verify that `@Cacheable` was present on the method. This confirms the annotation exists but does NOT verify that Spring AOP actually intercepts calls and caches the result. A misconfigured Spring context (missing `@EnableCaching`, wrong cache manager, AOP proxy issue) would pass the annotation test but fail in production.

**Fix applied:** Created `PlayerCatalogueServiceCacheIntegrationTest.java` — a lightweight Spring slice test using `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration` + `@EnableCaching` + `ConcurrentMapCacheManager`. Verifies via `verify(repository, times(1))` that:
- `findAll()`: repository called once across two service invocations (cache hit on 2nd)
- `findByRegion(EU)`: repository called once across two service invocations
- Different regions cached independently (EU and NAW each called once across 4 invocations)
- `searchByNickname()`: repository called twice across two service invocations (NOT cached)

**Status:** FIXED — 4 new integration tests, all green.

---

### F-002 — MEDIUM — No error handling in warmup() [FIXED]

**Location:** `PlayerCatalogueWarmupService.java:warmup()`

**Problem:** `warmup()` was called from `onApplicationEvent()` with no exception handling. If the database is unavailable at startup (network partition, late DB readiness), the exception would propagate up through `ApplicationListener` invocation, potentially crashing the application context startup. The application should start regardless — users will see cold-start latency instead of a hard failure.

**Fix applied:** Wrapped the entire warmup body in `try { ... } catch (Exception e) { log.warn(...) }`. The exception is logged as WARN (with the message) so ops teams can diagnose cold-start latency without a startup crash.

**Status:** FIXED — new test `swallowsExceptionFromService()` verifies the behaviour.

---

### F-003 — MEDIUM — PlayerRegion.UNKNOWN warmed unnecessarily [FIXED]

**Location:** `PlayerCatalogueWarmupService.java:warmup()`

**Problem:** The warmup loop iterated over ALL `PlayerRegion.values()` including `PlayerRegion.UNKNOWN`. There are no actual players assigned region `UNKNOWN` in normal operation (UNKNOWN is a transient state during ingestion, later upgraded to a real region). Calling `findByRegion(UNKNOWN)` adds an unnecessary DB round-trip on every warmup and pollutes the cache with a likely-empty list entry.

**Fix applied:** Added `if (region == PlayerRegion.UNKNOWN) { continue; }` in the warmup loop. The log message now reports "8 per-region entries" (the 8 known regions: EU, NAW, BR, ASIA, OCE, NAC, ME, NA).

**Status:** FIXED — `callsFindByRegionForEveryKnownRegion()` now asserts `never().findByRegion(UNKNOWN)` and `callsFindByRegionExactlyOncePerKnownRegion()` uses `KNOWN_REGION_COUNT` (8 instead of 9).

---

### F-004 — LOW — isIdempotent test incomplete [NOT FIXED — ACCEPTABLE]

**Location:** `PlayerCatalogueWarmupServiceTest.java:isIdempotent()`

**Problem:** The test only verifies `findAll()` is called twice. It does not verify that `findByRegion()` is also called the expected number of times on the second invocation. This leaves partial coverage of the idempotency claim.

**Decision:** Not fixed. The test is sufficient to establish the contract (warmup can be called N times without side effects). Adding a full `times(KNOWN_REGION_COUNT * 2)` assertion would add noise without new information — the per-region test already covers correct region handling.

---

### F-005 — LOW — Duplicate TTL constant across cache types [NOT FIXED — ACCEPTABLE]

**Location:** `CacheConfig.java` — `CATALOGUE_TTL_HOURS = 24L` alongside existing `TTL_HOURS = 24L` for other caches.

**Problem:** There are now two 24-hour TTL constants. A future developer might change one without changing the other, creating subtle divergence.

**Decision:** Not fixed. The catalogue cache has a semantically distinct TTL motivation (aligned to nightly scraping cycle). Keeping a named constant `CATALOGUE_TTL_HOURS` makes the intent explicit. If a future story changes the scraping cycle to 12h, the developer will naturally update only the catalogue TTL.

---

## Quality Checklist

- [x] SOLID respecte (SRP: WarmupService responsabilite unique — warmup; OCP: nouveau cache sans modifier PlayerCatalogueService logic)
- [x] ≤ 500 lignes par classe / ≤ 50 lignes par methode (WarmupService ~30L, methode warmup ~20L)
- [x] 0 duplication (cache names centralises dans CacheConfig, pas de magic strings)
- [x] Nommage explicite (`PlayerCatalogueWarmupService`, `PlayerCatalogueServiceCacheIntegrationTest`)
- [x] Tests unitaires + integration ajoutés: 17 tests catalogue (6 warmup + 7 annotation + 4 integration), tous verts
- [x] Pas de nouvelle dependance non justifiee
- [x] Architecture hexagonale respectee (WarmupService dans `service/catalogue/`, pas dans `domain/` ni `adapter/`)
- [x] NamingConventionTest: classes dans `..service..` finissent par `Service` ✓
- [x] CouplingTest: WarmupService = 1 dep ✓

## Test Results

```
Tests run: 2031, Failures: 26, Errors: 1, Skipped: 9
```

- +5 nouveaux tests vs baseline pre-story (2026 → 2031)
- 26 failures = pre-existing identiques (hors scope story 2.4)
- 1 error = GameStatisticsServiceTddTest NullPointer pre-existing
- 0 regression
