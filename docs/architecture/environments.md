# Environments and Profiles

## Scope
This document inventories runtime profiles/environments and data sources, and
proposes a minimal target set aligned with the JIRA request.

## Project map (modules and dependencies)
- backend: Spring Boot 3.3, JPA, Security, Flyway, Postgres, JWT.
- frontend: Angular 20, Angular Material, RxJS, Chart.js.
- ops/docs: `docs/`, `k8s/`, Docker/Docker Compose, `start-backend-dev.ps1`.

## Current profiles (backend)
- default: `application.yml` (spring.profiles.active defaults to `dev`).
- dev: `application-dev.yml` (Postgres local, Flyway enabled, debug logs).
- h2: `application-h2.yml` (H2 in-memory, no Postgres required, seed 4 users).
- prod: `application-prod.yml` (Postgres via env vars, stricter settings).
- test: `src/test/resources/application-test.yml` (H2 in-memory, ddl create-drop, Flyway disabled).

## Quick start commands

### Option 1: H2 (no Docker required)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Users disponibles: thibaut, teddy, marcel, sarah

### Option 2: Postgres local (Docker)
```bash
docker compose -f docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Variables: DB_HOST=localhost, DB_PORT=5432, DB_NAME=fortnite_pronos, DB_USERNAME=fortnite_user, DB_PASSWORD=fortnite_pass

## Current data sources and seed paths
- CSV data: `src/main/resources/data/fortnite_data.csv` (148 players; Thibaut 50 / Teddy 49 / Marcel 49).
- Reference seed: `ReferenceGameSeedService` (idempotent, dev only).
- Legacy seed: `DataInitializationService` (disabled unless `fortnite.seed.legacy-enabled=true`).
- Optional fake seed: `FakeGameSeedService` (disabled by default).
- Flyway seeds: `src/main/resources/db/migration-seed` and H2 variants in
  `src/main/resources/db/migration-h2` (not referenced by Flyway locations today).
- Frontend fallback: `frontend/src/app/core/data/mock-game-data.ts`.
- Seed provider selection: `SeedDataProviderSelector` (property `fortnite.data.provider`).

## Target profile set (minimal)
- h2: local dev without Docker (H2 in-memory, 4 test users).
- dev: development + seed + manual tests (Postgres via docker-compose.dev.yml).
- prod: official data (Postgres via env vars).
- test: automated tests only (H2 in-memory, no seed).

## Cross-profile persistence (target)
- Dev data should persist across restarts and dev runs.
- Seed should be idempotent and only reset on explicit opt-in.
- Prod remains isolated via separate DB credentials (or dedicated schema if needed).

## Seed controls
- `fortnite.seed.enabled`: master switch (dev true, prod false).
- `fortnite.seed.legacy-enabled`: enable legacy CSV seed path.
- `fortnite.seed.fake-game-enabled`: optional fake game seed (dev only).
- `fortnite.seed.mode`: `reset` clears games via ReferenceGameSeedService.
- `fortnite.seed.reset`: legacy reset switch (DataInitializationService only).

## DataProvider decision
- Location: backend (service/repository layer).
- Selection: `fortnite.data.provider` (default: `csv`).
- Supabase plan: see `docs/db/supabase-mapping.md`.

## Known duplication / cleanup candidates
- Multiple seed paths (CSV + Flyway seed + FakeGameSeedService).

## Open questions
- Clarify "149 players" vs current CSV count (148).
- Confirm whether "cross-profile" means dev-like profiles only (not user accounts).
