# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context
- **Fortnite Pronos App**: Fantasy league application for Fortnite players
- **Architecture**: Spring Boot backend + Angular frontend
- **Languages**: Java 21, TypeScript/Angular 20
- **Database**: PostgreSQL for development/production, H2 for testing only
- **Build Tools**: Maven, npm/Angular CLI

## Project Structure
```
/src/main/java/com/fortnite/pronos/    # Spring Boot backend
├── controller/                       # REST API controllers
├── service/                         # Business logic services
├── model/                          # JPA entities
├── repository/                     # Data access layer
├── dto/                           # Data transfer objects
├── config/                        # Configuration classes
└── exception/                     # Custom exceptions

/frontend/                         # Angular application
├── src/app/                      # Angular components and services
├── features/                     # Feature modules
├── shared/                       # Shared components
└── core/                        # Core services and guards

/src/main/resources/              # Configuration and migrations
├── application.yml               # Main configuration
├── application-test.yml          # Test configuration
└── db/migration/                # Flyway migrations
```

## ⚠️ ÉTAT CRITIQUE DU PROJET - CORRECTIONS NÉCESSAIRES

### Problèmes Identifiés par les Agents
1. **🔴 ERREURS DE COMPILATION**: javax → jakarta migration requise
2. **🔴 CRASHS JVM**: Configuration mémoire insuffisante (voir hs_err_pid*.log)
3. **🔴 SÉCURITÉ JWT**: Clé secrète non configurée, authentification compromise
4. **🟡 PERFORMANCE**: Optimisations requises pour 147+ joueurs simultanés

### Actions Critiques Avant Lancement
- **PHASE 1A**: Corriger imports javax→jakarta dans JwtService.java et WebConfig.java
- **PHASE 1B**: Configurer JVM avec -Xms2g -Xmx4g pour éviter les crashs
- **PHASE 2**: Sécuriser JWT avec variable d'environnement JWT_SECRET
- **PHASE 3**: Tester compilation et résoudre erreurs restantes

## Development Commands

### Backend (Spring Boot + Maven)
- **Install dependencies**: `mvn dependency:resolve`
- **Build and test**: `mvn clean verify`
- **Run development**: `mvn spring-boot:run -Dspring.profiles.active=dev` (PostgreSQL, port 8080)
- **Run tests only**: `mvn test` (H2 in-memory)
- **H2 Console (tests only)**: Available during test execution

### ⚡ Configuration JVM Recommandée (OBLIGATOIRE)
Ajouter ces options aux scripts PowerShell:
```powershell
-Xms2g -Xmx4g -XX:HeapBaseMinAddress=4g -XX:MaxDirectMemorySize=1g -XX:+UseG1GC
```

### Frontend (Angular)
- **Install dependencies**: `npm ci --prefix frontend`
- **Run development**: `npm start --prefix frontend` (port 4200)
- **Build production**: `npm run build --prefix frontend`
- **Run tests**: `npm test --prefix frontend`
- **Lint check**: `npm run lint --prefix frontend` (if available)

### Full Application
- **Complete startup**: Use `./start-app.ps1` or `./lancer-app.ps1`
- **Frontend URL**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **Health check**: http://localhost:8080/actuator/health

## Database Configuration

### Local Development Setup (PostgreSQL)
**Prerequisites**: Install PostgreSQL locally and create database:
```sql
CREATE DATABASE fortnite_pronos;
CREATE USER fortnite_user WITH PASSWORD 'fortnite_pass';
GRANT ALL PRIVILEGES ON DATABASE fortnite_pronos TO fortnite_user;
```

**Configuration**: Create `src/main/resources/application-dev.yml`:
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/fortnite_pronos
    username: fortnite_user
    password: fortnite_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Test Environment (H2 In-Memory)
Tests use H2 for isolation and speed:
- **Database**: H2 in-memory (`jdbc:h2:mem:testdb`)
- **Configuration**: `src/test/resources/application-test.yml`
- **Flyway**: Disabled (uses `ddl-auto: create-drop`)

### Production Configuration
Same as development but with production database credentials.

## API Configuration

### Angular Environment Setup
Configure API endpoints in environment files:
- **Development**: `frontend/src/environments/environment.ts`
- **Production**: `frontend/src/environments/environment.prod.ts`

Example configuration:
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080'
};
```

### Key API Endpoints
- **Authentication**: `/auth/*`
- **Games**: `/api/games/*`
- **Teams**: `/api/teams/*`
- **Players**: `/api/players/*`
- **Leaderboard**: `/api/leaderboard/*`
- **Draft**: `/api/draft/*`

## Development Workflow: Explorer → Plan → Coder → Commit

### Phase 1: EXPLORER (READ-ONLY)
**IMPÉRATIF**: Commencer TOUJOURS par lire sans écrire de code
- Read all relevant files to understand current implementation
- List ALL files that will be impacted by changes
- Understand existing patterns, conventions, and architecture
- Check related tests and documentation
- Identify potential integration points and dependencies

### Phase 2: PLAN (THINK PHASES)
Use thinking phases to increase solution quality:

**THINK**: Basic analysis and approach
- Define clear, specific objectives
- Identify exact files to modify
- Outline high-level approach

**THINK HARD**: Deeper analysis
- Consider edge cases and error scenarios
- Analyze impact on existing functionality
- Plan test strategy (unit + integration)
- Assess backwards compatibility

**THINK HARDER**: Advanced considerations
- Performance implications
- Security considerations
- Database migration needs
- User experience impact

**ULTRATHINK**: Maximum quality analysis
- Alternative approaches comparison
- Long-term maintainability
- Scalability considerations
- Complete risk assessment
- Detailed implementation steps

### Phase 3: CODER (IMPLEMENTATION)
- Follow TDD-assisted approach (see below)
- Make small, atomic changes
- Follow existing code patterns strictly
- NO new dependencies without explicit approval
- Ensure backward compatibility unless specified

### Phase 4: COMMIT
- Run full test suite: `mvn clean verify`
- Run frontend tests: `npm test --prefix frontend`
- Verify application startup and functionality
- Commit with clear, descriptive messages
- Consider pull request if significant changes

## TDD-Assisted Development (OBLIGATOIRE)

### TDD Workflow: Red → Green → Refactor
**IMPÉRATIF**: Écrire les tests d'abord, les faire échouer, committer, puis coder

#### Step 1: RED (Write Failing Tests)
```bash
# 1. Write failing tests for new functionality
# 2. Verify tests fail for the right reasons
mvn test
# 3. Commit failing tests
git add -A && git commit -m "test: add failing tests for [feature]"
```

#### Step 2: GREEN (Make Tests Pass)
```bash
# 1. Write MINIMAL code to make tests pass
# 2. DO NOT modify tests during this phase
# 3. Focus only on making tests green
mvn test
# 4. Commit working implementation
git add -A && git commit -m "feat: implement [feature] to satisfy tests"
```

#### Step 3: REFACTOR (Improve Code Quality)
```bash
# 1. Refactor both production and test code
# 2. Ensure tests still pass after each refactor
# 3. Commit refactoring separately
git add -A && git commit -m "refactor: improve [aspect] implementation"
```

### Testing Requirements
- **MANDATORY**: Write tests BEFORE implementing functionality
- **MANDATORY**: Maintain compatibility of public APIs unless explicitly requested otherwise
- Use `@ActiveProfiles("test")` annotation for JUnit tests
- Mock external APIs and services in tests
- Never modify tests during GREEN phase

## Quality & Security Requirements

### Security Guidelines  
- **NEVER**: Expose secrets, API keys, or credentials in code
- **NEVER**: Hardcode credentials or make real network calls in tests
- Use proper authentication patterns (JWT is configured)
- Validate all user inputs and API parameters

### Performance Considerations
- Avoid N+1 database queries
- Add database indexes when necessary
- Keep logging at appropriate levels (minimal noise)
- Cache expensive operations when appropriate

## Architecture Patterns

### Backend Patterns
- **Controllers**: Handle HTTP requests, delegate to services
- **Services**: Contain business logic, transaction management
- **Repositories**: Data access layer using Spring Data JPA
- **DTOs**: Data transfer objects for API communication
- **Entities**: JPA entities with proper relationships

### Frontend Patterns
- **Feature Modules**: Organize related components together
- **Shared Components**: Reusable UI components in `/shared`
- **Core Services**: Application-wide services in `/core`
- **Reactive Forms**: Use Angular reactive forms for user input
- **HTTP Interceptors**: Handle authentication and error responses

### Testing Patterns
- **Unit Tests**: Test individual methods and components
- **Integration Tests**: Test complete workflows and API endpoints
- **TDD Approach**: Write failing tests first, then implement features
- **Test Data Builders**: Use builder pattern for test data setup

## Key Domain Concepts

### Core Entities
- **Game**: Fantasy league game with multiple participants
- **User**: Application users (admin/participant roles)
- **Team**: User's fantasy team within a game
- **Player**: Real Fortnite players
- **Draft**: Team selection process
- **Score**: Player performance data
- **Leaderboard**: Ranking system

### Business Rules
- Games have maximum participant limits
- Teams must follow region distribution rules
- Draft order is automatically managed
- Scores are calculated from external Fortnite data
- Users can trade players within games
