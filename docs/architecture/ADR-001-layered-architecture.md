# ADR-001: Pure Hexagonal Architecture (Ports & Adapters) - Incremental Migration

**Status:** Accepted (Final)
**Date:** 2025-08-03
**Updated:** 2026-02-05
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

We adopt **pure hexagonal architecture (Ports & Adapters)** with **incremental migration**.
Legacy layered structure remains only during transition and will be removed as domains migrate.

## Decision Update (2026-01-25): Pure Hexagonal Architecture with Incremental Migration

After strategic review (JIRA-ARCH-009), we **adopt pure hexagonal architecture (Ports & Adapters pattern)** as the target, using **incremental migration strategy** to minimize risk.

### Architecture Target: Monolith + Pure Hexagonal

```
+------------------------------------------------------------------+
|                        Monolithic Application                     |
|                        (Spring Boot Container)                    |
|                                                                  |
|  +------------+      +---------------+      +--------------+     |
|  | Adapter IN | ---> | Application   | ---> | Domain Ports |     |
|  | (Web/REST) |      | (Use Cases)   |      | (Interfaces) |     |
|  +------------+      +---------------+      +--------------+     |
|                                   |                              |
|                                   v                              |
|                           +---------------+                      |
|                           | Domain Models |                      |
|                           | (Pure Entity) |                      |
|                           +---------------+                      |
|                                   ^                              |
|                                   |                              |
|                           +---------------+                      |
|                           | Adapter OUT   |                      |
|                           | (Persistence) |                      |
|                           +---------------+                      |
+------------------------------------------------------------------+
```

### Why Pure Hexagonal (No Hybrid)

1. **Long-term maintainability**: Clean separation between domain logic and infrastructure
2. **Framework independence**: Domain models have ZERO dependencies (no JPA, no Spring)
3. **Testability**: Domain logic testable without database/framework
4. **Future-proof**: Easy to swap persistence layer or migrate to microservices if needed
5. **DDD alignment**: True domain-driven design with pure domain models

### Critical Success Factor: INCREMENTAL MIGRATION

WARNING: Big-bang migration WILL break things (tests, mappers, integrations)

**Incremental Strategy** (one domain at a time):
1. **Migrate Domain** (e.g., Game) -> Create domain.game.model + adapter.out.persistence.game
2. **Run ALL Tests** -> Verify 1275/1275 passing (zero regression)
3. **Validate Application** -> Manual smoke tests, verify endpoints work
4. **IF Tests Pass** -> Proceed to next domain (Player)
5. **IF Tests Fail** -> Fix issues before moving forward
6. **Repeat** for each domain: Player -> Team -> Draft -> Trade

**Migration Order** (impact-based):
1. Game (highest complexity, most dependencies)
2. Player (medium complexity)
3. Team (medium complexity)
4. Draft (draft system, isolated)
5. Trade (lowest complexity)

### Current Migration Status (Transition Phase)

**Phase 1 - Ports & Use Cases** DONE:
- 12 Repository Ports (domain.port.out)
- 12 Use Case interfaces (application.usecase)
- 8/14 controllers using DIP pattern

**Phase 2 - Domain Models Migration** IN PROGRESS:
- [ ] Game domain (JIRA-ARCH-011)
- [ ] Player domain (JIRA-ARCH-012)
- [ ] Team domain (JIRA-ARCH-013)
- [ ] Draft domain (JIRA-ARCH-014)
- [ ] Trade domain (JIRA-ARCH-015)

**Phase 3 - Remaining Controllers** PENDING:
- Migrate 6 remaining controllers to use cases after domain models complete

### Decision Table (Validated 2026-02-05)

| Subject | Option | Decision | Rationale | Impact | Owner | Date |
| --- | --- | --- | --- | --- | --- | --- |
| Domain/Application/Adapter boundaries | Strict separation | Domain pure + Application orchestration + Adapters for I/O | Align hexagonal | Packages + dependencies | @Codex | 2026-02-05 |
| Port naming | `<UseCase>Port` / `<UseCase>UseCase` | Use suffix "Port" for in/out interfaces | Clear boundaries | Package conventions | @Codex | 2026-02-05 |
| Mapper location | adapters/out/persistence/mapper | Mappers only in adapter layer | Avoid domain deps | Adapter repository | @Codex | 2026-02-05 |
| DTO boundaries | DTOs in adapters + mappers | Domain has no DTO | Keep domain pure | Controllers/adapters | @Codex | 2026-02-05 |
| Error strategy | Domain errors mapped in adapters | Domain errors framework-free + mapping in adapters | DIP compliance | Error mapping | @Codex | 2026-02-05 |


### Migration Checklist

- Create pure domain models (no framework dependencies)
- Define ports in domain.port.in/out
- Implement adapters in adapter.in/out + mappers
- Update use cases/controllers to depend on ports only
- Replace entity usage with domain models in application
- Run unit tests + architecture tests + smoke tests
- Update ADR status and migration tracking

### Migration Template (Per Domain)

**Scope:** <DomainName>
**Ports:** <List in/out ports>
**Adapters:** <List in/out adapters + mappers>
**Use cases updated:** <List>
**Tests:** <List unit/integration/architecture>
**Exit criteria:** All tests pass, no framework deps in domain

### Architecture Fitness Functions (Pure Hexagonal Rules)

Enforced via `HexagonalArchitectureTest.java`:

1. **Domain purity**: NO framework dependencies (Spring, JPA, Jackson) in domain.model
2. **Dependency direction**: Adapters depend on domain, NEVER reverse
3. **Port contracts**: All persistence through domain.port.out interfaces
4. **Use case orchestration**: Controllers call application.usecase only
5. **Mapper isolation**: Domain <-> Entity mapping ONLY in adapter.out
6. **Class size**: <= 500 lines (CLAUDE.md constraint)

Once migration complete, `shouldFollowOnionArchitecture()` will be enabled and enforced.


### Hexagonal Responsibilities

**Domain (core/domain):**
- Pure business rules and value objects
- No framework dependencies (Spring/JPA/HTTP)
- Emits domain errors (framework-free)

**Application (core/usecase + application):**
- Orchestrates use cases
- Defines input/output ports (interfaces)
- Transactions and policies at the use-case boundary

**Adapters In (controller, messaging, UI boundary):**
- Input validation and mapping to use-case commands/queries
- HTTP/status/serialization concerns only
- Map domain errors to API responses

**Adapters Out (persistence, external services):**
- Implement ports for DB/external systems
- Map domain <-> entities/DTOs
- Own framework-specific dependencies

## Alternatives Considered

### 1. Hybrid Architecture (Layered + Partial Hexagonal) - REJECTED (2026-01-25)
- **Pros:** Lower migration effort, familiar patterns, incremental DIP adoption

**Cons:**
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
- Clear boundaries between domain, application, and adapters
- Domain logic testable without frameworks or database
- Infrastructure can be swapped without touching domain
- Incremental migration reduces risk

### Negative
- More mapping/boilerplate in adapters
- Requires discipline in package boundaries
- Temporary complexity during migration

### Mitigation Strategies
- Keep mappers inside adapters only
- Keep use cases small and focused
- Enforce boundaries with architecture tests and reviews

## Implementation Guidelines

### Use Case Interface
```java
public interface CreateGameUseCase {
    GameId handle(CreateGameCommand command);
}
```

### Adapter In (Controller)
```java
@RestController
@RequestMapping("/api/games")
public class GameController {
    private final CreateGameUseCase createGameUseCase;

    @PostMapping
    public ResponseEntity<GameDto> create(@RequestBody CreateGameRequest request) {
        var id = createGameUseCase.handle(CreateGameCommand.from(request));
        return ResponseEntity.ok(new GameDto(id));
    }
}
```

### Adapter Out (Persistence)
```java
@Repository
public class GameRepositoryAdapter implements GameRepositoryPort {
    // map Domain <-> Entity here only
}
```

## Compliance

This ADR is enforced through:
- Architecture tests (HexagonalArchitectureTest)
- Dependency direction rules (adapters depend on domain only)
- Port naming + mapper isolation conventions
- Code review checklist and clean code limits

## Related ADRs
- ADR-002: Database Technology Selection
- ADR-003: API Design Standards
- ADR-004: CQRS Pattern Adoption (selective application within hybrid architecture)
