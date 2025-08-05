# ADR-001: Layered Architecture with Spring Boot

**Status:** Accepted  
**Date:** 2025-08-03  
**Deciders:** Development Team  

## Context

The Fortnite Pronos application requires a scalable, maintainable architecture that can handle:
- Fantasy league game management
- Real-time draft systems
- Player statistics processing
- User authentication and authorization
- Support for 150+ concurrent users

## Decision

We adopt a layered architecture using Spring Boot with clear separation of concerns:

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

## Alternatives Considered

### 1. Hexagonal Architecture (Ports and Adapters)
- **Pros:** Better testability, domain isolation
- **Cons:** Increased complexity, longer development time
- **Rejected:** Overkill for current application size

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
- Architecture fitness functions
- Dependency direction rules (no upward dependencies)

## Related ADRs
- ADR-002: Database Technology Selection
- ADR-003: API Design Standards
- ADR-004: Authentication Strategy