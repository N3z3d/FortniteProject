# Fortnite Pronos

## Purpose
Backend + frontend for a Fortnite fantasy league app with drafts, stats, and games.

## Project map (backend)
- `controller/`: HTTP endpoints, request validation, response mapping. No business logic here.
- `dto/`: API request/response contracts.
- `service/`: legacy orchestration and business logic (being migrated).
- `core/domain/`: pure domain rules and value objects (no Spring/JPA).
- `core/usecase/`: use case interfaces (preferred place for new orchestration).
- `repository/`: data access (Spring Data/JPA).
- `model/`: JPA entities.
- `config/`: Spring configuration.
- `exception/`: error handling and API exceptions.
- `util/`: cross-cutting utilities.

## Where to add logic
1) New HTTP endpoint: add controller + DTOs, delegate to a service/use case.
2) New business rule: add to `core/domain/` or a domain service; keep it framework-free.
3) New orchestration: prefer `core/usecase/` (interfaces) + a service/impl that wires ports.
4) Persistence changes: update `model/` and `repository/` (keep queries here).
5) Cross-cutting concerns: `exception/`, `util/`, or `config/`.

## Naming conventions
- Controllers: `*Controller`
- Services: `*Service`
- Repositories: `*Repository`
- Use cases: `*UseCase` (interfaces)
- DTOs: `*Dto`, `*Request`, `*Response`

## Contribution notes
- Prefer TDD for new/changed code; keep tests deterministic.
- Keep classes <= 500 lines and methods <= 50 lines.
- Avoid adding dependencies unless justified.

## Key docs
- `docs/architecture/ADR-001-layered-architecture.md`
- `docs/adr/001-target-architecture.md`
- `docs/architecture/environments.md`
- `AGENTS.md`
- `CLAUDE.md`
