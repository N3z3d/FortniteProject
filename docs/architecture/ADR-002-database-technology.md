# ADR-002: Database Technology Selection

**Status:** Accepted  
**Date:** 2025-08-03  
**Deciders:** Development Team  

## Context

The Fortnite Pronos application requires a robust database solution to handle:
- Game and user management data
- Real-time draft operations
- Player statistics and scoring
- Leaderboard calculations
- Support for 150+ concurrent users
- ACID compliance for critical operations

## Decision

We adopt **PostgreSQL** as the primary database with **H2** for testing:

### Production & Development: PostgreSQL 15+
- Primary database for all environments except testing
- Handles complex queries with advanced indexing
- Supports JSON data types for flexible schemas
- Excellent performance for read-heavy workloads

### Testing: H2 In-Memory Database
- Fast test execution with in-memory storage
- Compatible SQL dialect with PostgreSQL
- Isolated test environments
- Simplified CI/CD pipeline

## Alternatives Considered

### 1. MySQL/MariaDB
- **Pros:** Wide adoption, good performance, mature ecosystem
- **Cons:** Less advanced features than PostgreSQL, JSON support limitations
- **Rejected:** PostgreSQL's superior query optimization and JSON capabilities

### 2. MongoDB (NoSQL)
- **Pros:** Flexible schema, horizontal scaling, JSON-native
- **Cons:** No ACID guarantees, complex transactions, SQL skills transfer
- **Rejected:** ACID requirements for fantasy league operations

### 3. SQLite
- **Pros:** Simple deployment, serverless, reliable
- **Cons:** Limited concurrency, no advanced features, single-writer
- **Rejected:** Cannot handle 150+ concurrent users

### 4. Oracle Database
- **Pros:** Enterprise features, excellent performance, advanced analytics
- **Cons:** High licensing costs, complex deployment
- **Rejected:** Cost and complexity for project scale

## Database Design Principles

### Schema Design
```sql
-- Clear naming conventions
CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    status game_status NOT NULL DEFAULT 'WAITING_FOR_PLAYERS',
    max_participants INTEGER NOT NULL CHECK (max_participants BETWEEN 2 AND 20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id)
);

-- Performance indexes
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_created_by ON games(created_by);
CREATE INDEX idx_games_status_created_at ON games(status, created_at);
```

### Data Integrity
- Foreign key constraints for referential integrity
- Check constraints for business rules
- NOT NULL constraints for required fields
- Unique constraints for business keys

### Performance Optimization
- Strategic indexing for query patterns
- Partial indexes for filtered queries
- Composite indexes for multi-column searches
- Query optimization with EXPLAIN ANALYZE

## Configuration Strategy

### Environment-Specific Configuration

**Development (application-dev.yml):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fortnite_pronos
    username: fortnite_user
    password: fortnite_pass
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true
```

**Testing (application-test.yml):**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate for each test
  flyway:
    enabled: false  # Use JPA schema generation for tests
```

**Production (application-prod.yml):**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    validate-on-migrate: true
```

## Migration Strategy

### Flyway Database Migrations
- Version-controlled schema changes
- Repeatable migrations for views/functions
- Baseline migrations for existing databases
- Rollback strategy for production deployments

### Migration File Naming Convention
```
V{version}__{description}.sql
V1__clean_schema.sql
V2__create_users_table.sql
V3__add_games_indexes.sql
```

### Example Migration
```sql
-- V6__create_games_tables.sql
CREATE TYPE game_status AS ENUM (
    'WAITING_FOR_PLAYERS',
    'DRAFT_IN_PROGRESS', 
    'ACTIVE',
    'COMPLETED'
);

CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    status game_status NOT NULL DEFAULT 'WAITING_FOR_PLAYERS',
    max_participants INTEGER NOT NULL CHECK (max_participants BETWEEN 2 AND 20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id)
);

-- Performance indexes
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_created_by ON games(created_by);
CREATE INDEX idx_games_status_created_at ON games(status, created_at);
```

## Consequences

### Positive
- **ACID Compliance:** Ensures data consistency for critical operations
- **Performance:** PostgreSQL's advanced query optimizer handles complex leaderboard calculations
- **JSON Support:** Flexible storage for dynamic game configurations
- **Scalability:** Can handle projected user growth with proper indexing
- **Test Speed:** H2 provides fast test execution
- **Developer Experience:** SQL familiarity across team

### Negative
- **Complexity:** Two database systems to maintain
- **Resource Usage:** PostgreSQL requires more memory than simpler alternatives
- **Deployment:** Requires database server management

### Risk Mitigation
- **Backup Strategy:** Automated daily backups with point-in-time recovery
- **Connection Pooling:** HikariCP configuration for optimal connection management
- **Monitoring:** Database performance monitoring with metrics
- **Development Parity:** Use PostgreSQL for development to match production

## Performance Targets

### Query Performance
- Game list queries: < 100ms
- Leaderboard calculations: < 500ms
- Draft operations: < 200ms
- User authentication: < 50ms

### Concurrent Operations
- Support 150+ concurrent read operations
- Support 50+ concurrent write operations
- Transaction isolation for draft operations

### Indexing Strategy
```sql
-- Critical performance indexes
CREATE INDEX idx_scores_player_season ON scores(player_id, season);
CREATE INDEX idx_teams_user_season ON teams(user_id, season);
CREATE INDEX idx_game_participants_game_id ON game_participants(game_id);
CREATE INDEX idx_draft_picks_game_id_pick_order ON draft_picks(game_id, pick_order);
```

## Monitoring and Maintenance

### Database Health Monitoring
- Connection pool metrics
- Query performance tracking
- Disk space monitoring
- Index usage analysis

### Regular Maintenance Tasks
- VACUUM and ANALYZE operations
- Index maintenance and optimization
- Query plan analysis
- Performance bottleneck identification

## Related ADRs
- ADR-001: Layered Architecture (Repository pattern)
- ADR-003: API Design Standards (Response formats)
- ADR-005: Caching Strategy (Database query optimization)