# ADR-001: Pure Hexagonal Architecture (Ports & Adapters) - Incremental Migration

**Status:** Accepted (Final)
**Date:** 2025-08-03
**Updated:** 2026-01-25
**Deciders:** Development Team
**Strategic Decision:** JIRA-ARCH-009 (Pure hexagonal with incremental migration strategy)

## Context

The Fortnite Pronos application requires a scalable, maintainable architecture that can handle:
- Fantasy league game management
- Real-time draft systems
- Player statistics processing
- User authentication and authorization
- Support for 150+ concurrent users

## Decision

We adopt a hybrid layered and hexagonal architecture using Spring Boot. The layered structure
remains the primary organization, while use cases depend on ports and adapters implement those
ports. This supports incremental migration without a big-bang rewrite.

## Decision Update (2026-01-25): Pure Hexagonal Architecture with Incremental Migration

After strategic review (JIRA-ARCH-009), we **adopt pure hexagonal architecture (Ports & Adapters pattern)** as the target, using **incremental migration strategy** to minimize risk.

### Architecture Target: Monolith + Pure Hexagonal

```
┌─────────────────────────────────────────────────────────────┐
│                    Monolithic Application                    │
│                    (Spring Boot Container)                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌───────────────┐         ┌─────────────────┐            │
│  │  Adapter IN   │────────>│   Application   │            │
│  │  (Web/REST)   │         │   (Use Cases)   │            │
│  └───────────────┘         └────────┬────────┘            │
│         │                           │                       │
│         │                           v                       │
│         │                  ┌─────────────────┐            │
│         └─────────────────>│  Domain Ports   │            │
│                            │   (Interfaces)  │            │
│                            └────────┬────────┘            │
│                                     │                       │
│                                     v                       │
│                            ┌─────────────────┐            │
│                            │  Domain Models  │            │
│                            │ (Pure Entities) │            │
│                            └─────────────────┘            │
│                                     ^                       │
│                                     │                       │
│                            ┌────────┴────────┐            │
│                            │  Adapter OUT    │            │
│                            │ (Persistence)   │            │
│                            │  JPA Entities   │            │
│                            │    + Mappers    │            │
│                            └─────────────────┘            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Why Pure Hexagonal (No Hybrid)

1. **Long-term maintainability**: Clean separation between domain logic and infrastructure
2. **Framework independence**: Domain models have ZERO dependencies (no JPA, no Spring)
3. **Testability**: Domain logic testable without database/framework
4. **Future-proof**: Easy to swap persistence layer or migrate to microservices if needed
5. **DDD alignment**: True domain-driven design with pure domain models

### Critical Success Factor: INCREMENTAL MIGRATION

⚠️ **WARNING**: Big-bang migration WILL break things (tests, mappers, integrations)

**Incremental Strategy** (one domain at a time):
1. **Migrate Domain** (e.g., Game) → Create domain.game.model + adapter.out.persistence.game
2. **Run ALL Tests** → Verify 1275/1275 passing (zero regression)
3. **Validate Application** → Manual smoke tests, verify endpoints work
4. **IF Tests Pass** → Proceed to next domain (Player)
5. **IF Tests Fail** → Fix issues before moving forward
6. **Repeat** for each domain: Player → Team → Draft → Trade

**Migration Order** (impact-based):
1. Game (highest complexity, most dependencies)
2. Player (medium complexity)
3. Team (medium complexity)
4. Draft (draft system, isolated)
5. Trade (lowest complexity)

### Current Migration Status (Transition Phase)

**Phase 1 - Ports & Use Cases** ✅ DONE:
- 12 Repository Ports (domain.port.out)
- 12 Use Case interfaces (application.usecase)
- 8/14 controllers using DIP pattern

**Phase 2 - Domain Models Migration** 🔄 IN PROGRESS:
- [ ] Game domain (JIRA-ARCH-011)
- [ ] Player domain (JIRA-ARCH-012)
- [ ] Team domain (JIRA-ARCH-013)
- [ ] Draft domain (JIRA-ARCH-014)
- [ ] Trade domain (JIRA-ARCH-015)

**Phase 3 - Remaining Controllers** ⏳ PENDING:
- Migrate 6 remaining controllers to use cases after domain models complete

### Architecture Fitness Functions (Pure Hexagonal Rules)

Enforced via `HexagonalArchitectureTest.java`:

1. **Domain purity**: NO framework dependencies (Spring, JPA, Jackson) in domain.model
2. **Dependency direction**: Adapters depend on domain, NEVER reverse
3. **Port contracts**: All persistence through domain.port.out interfaces
4. **Use case orchestration**: Controllers call application.usecase only
5. **Mapper isolation**: Domain ↔ Entity mapping ONLY in adapter.out
6. **Class size**: ≤ 500 lines (CLAUDE.md constraint)

Once migration complete, `shouldFollowOnionArchitecture()` will be enabled and enforced.

```
┌─────────────────────────────────────────┐
│             Presentation Layer           │
│        (Controllers, DTOs, Web)         │
├─────────────────────────────────────────┤
│              Service Layer               │
│       (Business Logic, Validation)      │
├─────────────────────────────────────────┤
│             Repository Layer             │
│         (Data Access, JPA)              │
├─────────────────────────────────────────┤
│              Database Layer              │
│           (PostgreSQL, H2)              │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

**Presentation Layer:**
- REST API controllers
- Input validation via Bean Validation
- DTO mapping and serialization
- HTTP status code management
- OpenAPI documentation

**Service Layer:**
- Business logic implementation
- Transaction management
- Domain rule enforcement
- Cross-cutting concerns (caching, security)
- Integration with external services

**Repository Layer:**
- Data persistence operations
- Query optimization
- Database abstraction
- Entity relationship management

### Hexagonal Additions (Hybrid)
- Use cases live in `core/usecase` and depend on ports in `domain/port/*`.
- Ports define persistence contracts; repositories implement those ports.
- Controllers call use cases (or services) and do not depend on repositories directly.

## Alternatives Considered

### 1. Hybrid Architecture (Layered + Partial Hexagonal) - REJECTED (2026-01-25)
- **Pros:** Lower migration effort, familiar patterns, incremental DIP adoption
- **Cons:**
  - Inconsistent architecture (some controllers use cases, others services)
  - Domain models coupled to JPA (framework dependency)
  - Cannot enable `shouldFollowOnionArchitecture()` test (violations)
  - Long-term technical debt (architectural inconsistency)
- **Decision:** **REJECTED** after JIRA-ARCH-009 strategic review
- **Rationale:** While hybrid reduces short-term effort, it creates permanent architectural inconsistency. Pure hexagonal with **incremental migration** achieves clean architecture with controlled risk.

### 2. Microservices Architecture
- **Pros:** Independent scaling, technology diversity
- **Cons:** Distributed system complexity, network latency
- **Rejected:** Premature optimization for current user base

### 3. CQRS (Command Query Responsibility Segregation)
- **Pros:** Read/write optimization, scalability
- **Cons:** Added complexity, eventual consistency
- **Rejected:** Current read/write patterns don't justify complexity

## Consequences

### Positive
- **Clear Separation:** Each layer has well-defined responsibilities
- **Testability:** Easy to unit test services independently
- **Maintainability:** Changes in one layer don't affect others
- **Spring Integration:** Leverages Spring Boot's dependency injection
- **Team Productivity:** Familiar pattern for most developers

### Negative
- **Potential Over-Engineering:** Some simple operations may be verbose
- **Performance:** Additional abstraction layers may introduce latency
- **Coupling:** Services may become tightly coupled to entities

### Mitigation Strategies
- Use DTOs to decouple API contracts from entities
- Implement caching at service layer for performance
- Regular architecture reviews to prevent layer violations

## Implementation Guidelines

### Controller Layer
```java
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<GameDto>> createGame(
        @Valid @RequestBody CreateGameRequest request) {
        // Handle HTTP concerns only
    }
}
```

### Service Layer
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;
    
    @Transactional
    public GameDto createGame(UUID userId, CreateGameRequest request) {
        // Business logic and validation
    }
}
```

### Repository Layer
```java
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    @Query("SELECT g FROM Game g WHERE g.status = :status")
    List<Game> findByStatus(@Param("status") GameStatus status);
}
```

## Compliance

This ADR is enforced through:
- Package structure conventions
- Code review checklist
- Architecture fitness functions (hybrid rules)
- Dependency direction rules (no upward dependencies)

## Related ADRs
- ADR-002: Database Technology Selection
- ADR-003: API Design Standards
- ADR-004: CQRS Pattern Adoption (selective application within hybrid architecture)
