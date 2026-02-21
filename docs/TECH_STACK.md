# TECH STACK - Fortnite Pronos

Generated: 2026-02-18  
Source files:
- `pom.xml`
- `frontend/package.json`
- `frontend/angular.json`
- `docker-compose.dev.yml`
- `docker-compose.prod.yml`

## 1) Runtime Environment

| Tool | Version | Notes |
|---|---|---|
| Java | 21.0.10 | Runtime backend (Maven output) |
| Maven | 3.9.12 | Build backend |
| Node.js | 22.12.0 | Runtime frontend tooling |
| npm | 10.9.0 | Package manager frontend |
| Angular CLI | 20.0.2 | Frontend CLI |

## 2) Backend - Runtime Dependencies

Spring Boot parent:
- `org.springframework.boot:spring-boot-starter-parent:3.3.0`

Java language level:
- `java.version=21`

### 2.1 Spring Boot Starters

| Dependency | Version | Scope | Role |
|---|---|---|---|
| `spring-boot-starter-web` | via Boot BOM 3.3.0 | compile | REST API (Spring MVC, Jackson, embedded server) |
| `spring-boot-starter-security` | via Boot BOM 3.3.0 | compile | AuthN/AuthZ |
| `spring-boot-starter-data-jpa` | via Boot BOM 3.3.0 | compile | ORM/JPA/Hibernate |
| `spring-boot-starter-validation` | via Boot BOM 3.3.0 | compile | Bean Validation (`jakarta.validation`) |
| `spring-boot-starter-actuator` | via Boot BOM 3.3.0 | compile | Health/metrics/ops endpoints |
| `spring-boot-starter-mail` | via Boot BOM 3.3.0 | compile | Email integration |
| `spring-boot-starter-websocket` | via Boot BOM 3.3.0 | compile | WebSocket/STOMP support |
| `spring-boot-starter-aop` | via Boot BOM 3.3.0 | compile | AOP/interceptors/cross-cutting concerns |
| `spring-boot-starter-data-redis` | via Boot BOM 3.3.0 | compile | Redis client and repositories |
| `spring-boot-starter-cache` | via Boot BOM 3.3.0 | compile | Cache abstraction |

### 2.2 Data, Migration, Security, Observability

| Dependency | Version | Scope | Role |
|---|---|---|---|
| `org.postgresql:postgresql` | 42.7.3 | runtime | PostgreSQL JDBC driver |
| `org.flywaydb:flyway-core` | 10.11.0 | compile | DB schema migrations |
| `org.flywaydb:flyway-database-postgresql` | 10.11.0 | runtime | Flyway Postgres support |
| `io.jsonwebtoken:jjwt-api` | 0.12.5 | compile | JWT API |
| `io.jsonwebtoken:jjwt-impl` | 0.12.5 | runtime | JWT implementation |
| `io.jsonwebtoken:jjwt-jackson` | 0.12.5 | runtime | JWT JSON serialization |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.5.0 | compile | OpenAPI + Swagger UI |
| `io.micrometer:micrometer-registry-prometheus` | via Boot BOM 3.3.0 | compile | Prometheus export |
| `io.micrometer:micrometer-tracing-bridge-brave` | via Boot BOM 3.3.0 | compile | Distributed tracing bridge |
| `io.zipkin.reporter2:zipkin-reporter-brave` | via Boot BOM 3.3.0 | compile | Zipkin reporting |
| `io.micrometer:micrometer-core` | via Boot BOM 3.3.0 | compile | Metrics core API |
| `net.logstash.logback:logstash-logback-encoder` | 7.4 | compile | Structured JSON logs |

### 2.3 Utility Libraries

| Dependency | Version | Scope | Role |
|---|---|---|---|
| `org.projectlombok:lombok` | 1.18.32 | provided | Boilerplate reduction (getters/builders/constructors) |
| `com.opencsv:opencsv` | 5.11.1 | compile | CSV parsing/writing |
| `com.h2database:h2` | via Boot BOM 3.3.0 | compile | In-memory DB (tests/local fallback) |
| `org.springframework.retry:spring-retry` | via Boot BOM 3.3.0 | compile | Retry policies on I/O boundaries |

## 3) Backend - Test Dependencies

| Dependency | Version | Scope | Role |
|---|---|---|---|
| `spring-boot-starter-test` | via Boot BOM 3.3.0 | test | JUnit 5, AssertJ, Mockito, Spring Test |
| `spring-security-test` | via Boot BOM 3.3.0 | test | Security test support |
| `reactor-test` | via Boot BOM 3.3.0 | test | Reactor testing utilities |
| `archunit-junit5` | 1.2.1 | test | Architecture rule tests |

## 4) Backend - Build/Quality Plugins (Maven)

| Plugin | Version | Role |
|---|---|---|
| `spring-boot-maven-plugin` | via Boot parent | Packaging/run support |
| `maven-compiler-plugin` | 3.13.0 | Java compilation (source/target 21) |
| `spotless-maven-plugin` | 2.43.0 | Java formatting and import order checks |
| `spotbugs-maven-plugin` | 4.8.3.1 | Static bug analysis |
| `jacoco-maven-plugin` | 0.8.11 | Coverage report/check gates |
| `dependency-check-maven` | 9.0.9 | Dependency vulnerability scanning |
| `maven-surefire-plugin` | 3.2.5 | Unit/integration test execution |
| `springdoc-openapi-maven-plugin` | 1.4 | OpenAPI file generation |

## 5) Frontend - Runtime Dependencies

| Dependency | Version | Role |
|---|---|---|
| `@angular/animations` | ^20.0.0 | Animations |
| `@angular/cdk` | ^20.0.0 | Component Dev Kit |
| `@angular/common` | ^20.0.0 | Angular common APIs |
| `@angular/compiler` | ^20.0.0 | Angular compiler |
| `@angular/core` | ^20.0.0 | Angular core |
| `@angular/forms` | ^20.0.0 | Forms (template/reactive) |
| `@angular/material` | ^20.0.3 | UI components |
| `@angular/platform-browser` | ^20.0.0 | Browser platform |
| `@angular/platform-server` | ^20.0.0 | SSR platform |
| `@angular/router` | ^20.0.0 | Routing |
| `@angular/ssr` | ^20.0.1 | SSR integration |
| `@stomp/stompjs` | ^7.2.1 | STOMP protocol client |
| `chart.js` | ^4.5.0 | Charts |
| `express` | ^5.1.0 | Node server (SSR) |
| `ng2-charts` | ^8.0.0 | Angular wrapper for Chart.js |
| `rxjs` | ~7.8.0 | Reactive programming |
| `sockjs-client` | ^1.6.1 | WebSocket fallback transport |
| `tslib` | ^2.3.0 | TS runtime helpers |
| `zone.js` | ~0.15.0 | Angular async context tracking |

## 6) Frontend - Dev/Test Dependencies

| Dependency | Version | Role |
|---|---|---|
| `@analogjs/vite-plugin-angular` | ^1.17.1 | Vite + Angular integration |
| `@angular/build` | ^20.0.1 | Build system |
| `@angular/cli` | ^20.0.1 | CLI |
| `@angular/compiler-cli` | ^20.0.0 | AOT/JIT compilation tooling |
| `@types/express` | ^5.0.1 | Type definitions |
| `@types/jasmine` | ~5.1.0 | Jasmine typings |
| `@types/node` | ^20.17.19 | Node typings |
| `@types/sockjs-client` | ^1.5.4 | SockJS typings |
| `jasmine-core` | ~5.7.0 | Test framework |
| `karma` | ~6.4.0 | Test runner |
| `karma-chrome-launcher` | ~3.2.0 | Chrome launcher for Karma |
| `karma-coverage` | ~2.2.0 | Coverage instrumentation |
| `karma-jasmine` | ~5.1.0 | Jasmine adapter |
| `karma-jasmine-html-reporter` | ~2.1.0 | HTML test reporter |
| `typescript` | ~5.8.2 | TypeScript compiler |

## 7) Frontend - Build/Test Configuration

| File | Tooling |
|---|---|
| `frontend/angular.json` | Builder `@angular/build:application`, test builder `@angular/build:karma`, SCSS styles, budgets |
| `frontend/karma.conf.js` | Karma execution config |
| `frontend/tsconfig*.json` | TypeScript project configs |

## 8) Infrastructure and Ops

| Component | Version/Image | Source |
|---|---|---|
| Docker Compose spec | 3.8 | `docker-compose.dev.yml`, `docker-compose.prod.yml` |
| PostgreSQL (dev/prod) | `postgres:16-alpine` | Compose services `postgres`, `postgres-primary`, `postgres-readonly` |
| Redis (prod) | `redis:7-alpine` | Compose service `redis` |
| Prometheus (prod) | `prom/prometheus:latest` | Compose service `prometheus` |
| Grafana (prod) | `grafana/grafana:latest` | Compose service `grafana` |
| Backend image build | Multi-stage Dockerfile | `Dockerfile` |

## 9) Architecture/Quality Patterns Present in Repo

- Backend: layered + hexagonal migration in progress (domain/port/adapter packages present).
- Frontend: feature modules + service-oriented flows (query/command patterns on game feature).
- Quality gates/tooling in Maven:
  - format: Spotless
  - bug scan: SpotBugs
  - coverage: JaCoCo
  - dependency security: OWASP Dependency Check
  - architecture rules: ArchUnit tests

## 10) Useful Commands

Backend:
- `mvn test`
- `mvn -DskipTests compile`
- `mvn spotless:check`

Frontend:
- `npm --prefix frontend run build`
- `npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless`
- `node frontend/scripts/i18n-audit.js`
