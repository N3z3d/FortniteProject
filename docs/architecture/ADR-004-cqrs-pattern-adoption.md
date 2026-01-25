# ADR-004: CQRS Pattern Adoption for Specific Use Cases

**Status:** Accepted
**Date:** 2026-01-24
**Deciders:** Development Team
**Supersedes:** Partial revision of ADR-001 (CQRS rejection)

## Context

As the Fortnite Pronos application evolved, certain controllers began exhibiting distinct read and write patterns that justified architectural refinement. Specifically, the ScoreController manages:

- **Read Operations:** Fetching leaderboard rankings, team scores, player statistics, historical data
- **Write Operations:** Updating scores after matches, recalculating rankings, processing game results

Initial analysis showed:
- 80% of ScoreController traffic is read-heavy (GET requests)
- Write operations require complex validation and side effects (rank recalculation)
- Read and write concerns were becoming entangled in a single service layer
- Performance optimization opportunities for read paths were limited by coupling

While ADR-001 initially rejected CQRS as premature optimization, practical experience demonstrated specific controllers would benefit from this pattern.

## Decision

We adopt **CQRS (Command Query Responsibility Segregation)** for controllers with distinct read/write patterns, starting with `ScoreController`.

### Implementation Pattern

```
┌─────────────────────────────────────────────────────┐
│                  ScoreController                     │
├──────────────────────┬──────────────────────────────┤
│                      │                              │
│  ScoreQueryUseCase   │   ScoreCommandUseCase        │
│  (Read Operations)   │   (Write Operations)         │
│                      │                              │
│  - getLeaderboard()  │   - updateScore()            │
│  - getTeamScore()    │   - recalculateRankings()    │
│  - getPlayerStats()  │   - processGameResult()      │
│                      │                              │
└──────────────────────┴──────────────────────────────┘
           │                        │
           ↓                        ↓
    Read-optimized            Write-focused
    Services/Repos            Services/Repos
```

### Key Principles

1. **Use Case Interfaces:** Controllers depend on `QueryUseCase` and `CommandUseCase` interfaces (Dependency Inversion Principle)
2. **Clear Separation:** Read logic never mixes with write logic
3. **Different Optimizations:** Query paths can use caching, projections; Command paths focus on consistency
4. **Selective Application:** CQRS is applied only where read/write patterns diverge significantly

### Example: ScoreController

```java
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {
    private final ScoreQueryUseCase scoreQueryUseCase;
    private final ScoreCommandUseCase scoreCommandUseCase;

    @GetMapping("/leaderboard/{gameId}")
    public ResponseEntity<List<ScoreDto>> getLeaderboard(@PathVariable UUID gameId) {
        return ResponseEntity.ok(scoreQueryUseCase.getLeaderboard(gameId));
    }

    @PutMapping("/{scoreId}")
    public ResponseEntity<Void> updateScore(
            @PathVariable UUID scoreId,
            @RequestBody UpdateScoreRequest request) {
        scoreCommandUseCase.updateScore(scoreId, request);
        return ResponseEntity.noContent().build();
    }
}
```

## Alternatives Considered

### 1. Keep Traditional Layered Architecture
- **Pros:** Simpler, consistent with rest of codebase
- **Cons:** Growing complexity in ScoreService, read/write coupling
- **Rejected:** Technical debt was accumulating

### 2. Full CQRS with Event Sourcing
- **Pros:** Complete separation, audit trail, time-travel queries
- **Cons:** Massive complexity, eventual consistency challenges
- **Rejected:** Over-engineering for current requirements

### 3. Separate Read/Write Services (without interfaces)
- **Pros:** Some separation, less boilerplate
- **Cons:** Violates Dependency Inversion Principle, harder to test
- **Rejected:** Doesn't follow SOLID principles

## Consequences

### Positive
- **Clear Intent:** Query vs Command operations are explicit in code
- **Testability:** Easy to mock QueryUseCase/CommandUseCase in tests
- **Optimization:** Read paths can be cached without affecting writes
- **SOLID Compliance:** Follows Dependency Inversion Principle (DIP)
- **Scalability:** Can independently scale read/write infrastructure if needed

### Negative
- **Increased Boilerplate:** More interfaces and classes per controller
- **Architectural Inconsistency:** Only some controllers use CQRS
- **Learning Curve:** Team needs to understand when to apply CQRS vs layered architecture

### Mitigation Strategies
- **Clear Guidelines:** Document criteria for when to use CQRS (see below)
- **Code Templates:** Provide scaffolding for new CQRS implementations
- **Gradual Migration:** Only refactor controllers when read/write separation is clearly beneficial

## When to Apply CQRS

Apply CQRS pattern when a controller exhibits **3 or more** of these characteristics:

1. **Read-Heavy Traffic:** >70% GET requests
2. **Complex Writes:** Write operations have significant side effects or validation
3. **Different Data Models:** Reads need aggregated/denormalized data, writes need normalized entities
4. **Performance Requirements:** Read and write paths have different performance needs
5. **Growing Complexity:** Single service class exceeds 300 lines or has >10 methods

**Do NOT apply CQRS for:**
- Simple CRUD controllers
- Low-traffic endpoints
- Controllers with balanced read/write operations

## Current Implementations

### Controllers Using CQRS
- ✅ `ScoreController`: Separated query/command operations (2026-01-24)

### Controllers Evaluated and Kept Layered
- ✅ `LeaderboardController`: Multi-service pattern already optimal (ADR documented reasoning)

### Candidates for Future CQRS Adoption
- ⏳ `GameController`: Read/write patterns diverging (pending JIRA-ARCH-009 decision)
- ⏳ `DraftController`: High complexity but stable (monitor)

## Compliance

This ADR is enforced through:
- Code review checklist (verify CQRS applied appropriately)
- Architecture fitness functions (detect bloated services)
- Regular refactoring reviews (quarterly)

## Related ADRs
- ADR-001: Layered Architecture (parent architecture decision)
- ADR-003: API Design Standards (controller conventions)

## Revision History
- 2026-01-24: Initial adoption for ScoreController
- Future: Will be revised after JIRA-ARCH-009 strategic decision
