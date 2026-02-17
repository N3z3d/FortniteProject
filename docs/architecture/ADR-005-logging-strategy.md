# ADR-005 - Unified Logging Strategy (Frontend + Backend)

- Status: Accepted
- Date: 2026-02-17
- Decision owners: @Codex
- Related ticket: JIRA-AUDIT-012

## Context

The project uses two logging systems:
- Backend: SLF4J/Logback.
- Frontend: custom `LoggerService`.

Current usage is inconsistent:
- direct `console.*` calls still exist in application services/components.
- severity levels are not always aligned.
- contextual keys are not standardized.

## Decision

Adopt a single logging convention across backend/frontend.

### 1) Severity policy

- `DEBUG`: technical flow details for diagnosis (disabled in production by default).
- `INFO`: important business lifecycle events (start/end of key actions).
- `WARN`: recoverable anomaly, fallback path, invalid external input.
- `ERROR`: operation failure or unexpected exception.

### 2) Message format

Use stable prefix:
- `<ClassOrService>: <action> <result>`

Examples:
- `GameDetailActionsService: startDraft failed`
- `GameDataService: participants payload is not an array`

### 3) Context policy

Always include structured context object when relevant:
- identifiers: `gameId`, `teamId`, `tradeId`, `userId`
- operation metadata: `operation`, `status`, `httpStatus`
- avoid secrets/tokens/passwords in logs

### 4) Technical rules

- Frontend app code must not call `console.*` directly, except inside `LoggerService`.
- Backend must use SLF4J logger (`@Slf4j` or `LoggerFactory`), never `System.out`.
- User-facing error text remains handled by i18n/UI feedback services; logs keep technical context.

### 5) Minimum critical coverage

For critical user actions (create/join/leave/delete/start draft/trade actions):
- success path: `INFO` or `DEBUG` if noisy
- failure path: mandatory `ERROR` with context + root error object

## Consequences

- Better observability and triage speed.
- Reduced noise from unstructured console traces.
- Clear separation between user messaging and technical logging.

## Initial rollout scope (JIRA-AUDIT-012 lot)

- Replace direct `console.*` in critical game action/data services by `LoggerService`.
- Keep behavior unchanged; only logging path is standardized.
- Validate with targeted unit tests and frontend build.
