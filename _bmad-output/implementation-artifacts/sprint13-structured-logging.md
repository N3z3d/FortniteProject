# Story: sprint13-structured-logging — Logs structurés + MDC HTTP/STOMP

Status: review

<!-- METADATA
  story_key: sprint13-structured-logging
  branch: story/sprint13-structured-logging
  sprint: Sprint 13
  ADR: ADR-S13-001 (see sprint-status.yaml comments)
  Prerequisite for: sprint13-fix-draft-critiques (HARD dependency)
-->

## Story

As a developer (Thibaut) debugging production issues,
I want structured backend logs with MDC correlation IDs propagated through HTTP requests and STOMP WebSocket messages,
so that I can trace BUG-06 (state desync) and other draft bugs through the full request lifecycle without needing server console access.

## Acceptance Criteria

1. A `CorrelationIdFilter` (`OncePerRequestFilter`) generates a UUID `correlationId` per HTTP request, sets it in MDC, adds `X-Correlation-ID` to the response header, and clears MDC in a `finally` block.
2. A `StompMdcInterceptor` (`ChannelInterceptor`) sets MDC keys (`correlationId`, `userId`, `destination`, `command`) in `preSend` for ALL STOMP commands (not just CONNECT) and clears them in `afterSendCompletion`.
3. `StompMdcInterceptor` is registered in `WebSocketConfig.configureClientInboundChannel()` **before** `WebSocketAuthInterceptor` so that MDC is active when auth logs fire.
4. The dev console log pattern in `logback-spring.xml` includes `[%X{correlationId:-no-corr}]` so STOMP-originated log lines are visually distinct in local development.
5. STOMP pick events (SEND to `/app/draft/**`) appear in logs with correlationId, userId, and destination visible — enabling BUG-06 (state desync) root cause diagnosis.
6. MDC is cleared via `MDC.remove()` per key (not `MDC.clear()`) to avoid clobbering context set by upstream filters (e.g., `LoggingUtils`).
7. New tests: `CorrelationIdFilterTest` (≥ 4 cases) and `StompMdcInterceptorTest` (≥ 4 cases) — Mockito unit tests, no `@SpringBootTest`.
8. Backend test count does not decrease; no existing test failures beyond pre-existing baseline.

## Tasks / Subtasks

- [x] Task 1: Audit existing logging infrastructure (AC: #1, #2, #4)
  - [x] 1.1: Read `logback-spring.xml` — note that `%X{correlationId:-no-correlation}` is already in `FILE_LOG_PATTERN` and prod `CONSOLE_LOG_PATTERN` but NOT in dev `CONSOLE_LOG_PATTERN`
  - [x] 1.2: Read `LoggingUtils.java` — note MDC keys already used: `traceId`, `userId`, `action`, `duration`, `status`; do NOT use `MDC.clear()` to avoid wiping these
  - [x] 1.3: Read `LoggingInterceptor.java` — confirm it does NOT set `correlationId` in MDC (it doesn't)
  - [x] 1.4: Read `WebSocketAuthInterceptor.java` — confirm it only handles CONNECT frames and does NOT set MDC for other STOMP commands (confirmed: only handles CONNECT, returns early for all others)

- [x] Task 2: Update dev CONSOLE_LOG_PATTERN in `logback-spring.xml` (AC: #4)
  - [x] 2.1: In the `<springProfile name="!prod">` block, change `CONSOLE_LOG_PATTERN` to include `[%X{correlationId:-}]` between thread and logger. New pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%15.15t] [%X{correlationId:-}] %-40.40logger{39} : %m%n`

- [x] Task 3: Create `CorrelationIdFilter` (AC: #1, #6)
  - [x] 3.1: Create `src/main/java/com/fortnite/pronos/config/CorrelationIdFilter.java`
    - Package: `com.fortnite.pronos.config` (same as `RateLimitingFilter`)
    - Extends: `OncePerRequestFilter` (Spring)
    - Annotations: `@Component`, `@Order(1)`
    - Logic: read `X-Correlation-ID` header if present (supports distributed tracing), else generate short UUID (16 chars)
    - `MDC.put("correlationId", correlationId)` + `MDC.put("requestId", requestId)`
    - Response: `response.setHeader("X-Correlation-ID", correlationId)`
    - `filterChain.doFilter(request, response)` inside try, `MDC.remove()` per key in finally
    - Order: `@Order(1)` to run before `RateLimitingFilter` and `JwtAuthenticationFilter`

- [x] Task 4: Create `StompMdcInterceptor` (AC: #2, #3, #5, #6)
  - [x] 4.1: Create `src/main/java/com/fortnite/pronos/config/StompMdcInterceptor.java`
    - Package: `com.fortnite.pronos.config`
    - Implements: `ChannelInterceptor` (Spring Messaging)
    - Annotations: `@Component`, `@Slf4j`
    - `preSend()`: Sets MDC (correlationId, stompCommand, stompDestination, stompUser), logs SEND commands at DEBUG, never returns null
    - `afterSendCompletion()`: Removes all 4 MDC keys via `MDC.remove()`

- [x] Task 5: Register `StompMdcInterceptor` in `WebSocketConfig` (AC: #3)
  - [x] 5.1: Injected `StompMdcInterceptor stompMdcInterceptor` in `WebSocketConfig` via `@RequiredArgsConstructor`
  - [x] 5.2: Changed `registration.interceptors(webSocketAuthInterceptor)` to `registration.interceptors(stompMdcInterceptor, webSocketAuthInterceptor)` — MDC set before auth logs

- [x] Task 6: Write tests (AC: #7, #8)
  - [x] 6.1: Created `src/test/java/com/fortnite/pronos/config/CorrelationIdFilterTest.java` — 4 Mockito unit tests: header missing/present/chain-throws/chain-always-called
  - [x] 6.2: Created `src/test/java/com/fortnite/pronos/config/StompMdcInterceptorTest.java` — 4 Mockito unit tests: SEND→MDC set / null accessor→pass-through / afterSendCompletion→MDC cleared / preSend never null
  - [x] 6.3: Run `mvn spotless:apply && mvn test` — **2391 run, 0 failures, 0 errors** (was 2383 — 8 new tests added)

## Dev Notes

### Existing Infrastructure — DO NOT REINVENT

**`logback-spring.xml`** (`src/main/resources/logback-spring.xml`):
- Already has `%X{correlationId:-no-correlation}` in `FILE_LOG_PATTERN` and prod `CONSOLE_LOG_PATTERN`
- JSON_FILE appender uses `net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder` with `<mdc/>` provider — once MDC is populated, JSON logs automatically include all MDC keys
- Dev `CONSOLE_LOG_PATTERN` does NOT include correlationId — **Task 2 adds it**
- Do NOT remove or replace logback-spring.xml — Spring Boot 3.4 native `logging.structured.format.*` is incompatible with existing logback-spring.xml (custom logback config takes full precedence)

**`LoggingUtils.java`** (`src/main/java/com/fortnite/pronos/util/LoggingUtils.java`):
- Already uses MDC keys: `traceId`, `userId`, `action`, `duration`, `status`
- **NEVER call `MDC.clear()`** in the new filter/interceptor — use `MDC.remove()` per key to avoid wiping LoggingUtils context

**`RateLimitingFilter.java`** (`src/main/java/com/fortnite/pronos/config/RateLimitingFilter.java`):
- **USE AS REFERENCE** for `OncePerRequestFilter` pattern in this project (same package, same annotation style)
- `CorrelationIdFilter` follows the same structure: `@Component`, `@Order(N)`, extends `OncePerRequestFilter`

**`WebSocketAuthInterceptor.java`** (`src/main/java/com/fortnite/pronos/config/WebSocketAuthInterceptor.java`):
- Only intercepts STOMP `CONNECT` command (line 46: `if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand()))` → early return)
- Does NOT set any MDC for non-CONNECT messages
- **StompMdcInterceptor** must handle ALL commands (SEND, SUBSCRIBE, CONNECT, etc.)
- Both interceptors are `@Component` — `WebSocketConfig` injects via constructor

**`LoggingInterceptor.java`** (`src/main/java/com/fortnite/pronos/config/LoggingInterceptor.java`):
- `HandlerInterceptor` (HTTP only, not STOMP)
- Does NOT set correlationId in MDC — the new `CorrelationIdFilter` fills this gap
- LoggingInterceptor logs will automatically pick up MDC values after Task 3 is done

**`WebSocketConfig.java`** (`src/main/java/com/fortnite/pronos/config/WebSocketConfig.java`):
- `configureClientInboundChannel()` currently: `registration.interceptors(webSocketAuthInterceptor)`
- Change to: `registration.interceptors(stompMdcInterceptor, webSocketAuthInterceptor)`
- Order matters: MDC before auth so auth logs include correlationId

**`logstash-logback-encoder`**: Already in `pom.xml` (line 196). Do NOT add it again.

### BUG-06 Diagnosis Path

BUG-06: State desync — Teddy sees "à Thibaut de jouer" after his own pick.

After this story, the debug sequence for BUG-06 will be:
1. Reproduce the bug (Thibaut + Teddy pick session)
2. In logs, search for `stompCommand=SEND stompDestination=/app/draft/*/pick` — find all pick events
3. Each pick log line will show correlationId + userId — confirm which user sent which pick
4. Find the STOMP broadcast lines in SnakeDraftController/SnakeDraftService that send PICK_PROMPT
5. Correlate pick receipt vs. broadcast — identify the desync point

### Architecture / Constraints

- Package: `com.fortnite.pronos.config` — all filter/interceptor/config classes live here
- Max 7 injected deps per @Service (CouplingTest) — `CorrelationIdFilter` and `StompMdcInterceptor` have 0 injected service deps, so no concern
- Spotless: **Always run `mvn spotless:apply` before `mvn test`** (Windows CRLF vs LF issue)
- DomainIsolationTest: Only affects `domain..` packages — `config/` classes are safe
- `@WebMvcTest` tests: `CorrelationIdFilter` is a servlet filter — test it as a Mockito unit test (not `@WebMvcTest`), mocking `HttpServletRequest`, `HttpServletResponse`, `FilterChain`
- Spring Security filter chain: `CorrelationIdFilter` runs at servlet level, BEFORE Spring Security. Use `@Order(1)` or register in `SecurityConfig.filterChain()` via `http.addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)` if order matters — prefer `@Order(1)` for simplicity

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend: 2383 tests run (Sprint 12 baseline), ~16 pre-existing failures (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, GameStatisticsServiceTddTest 1 NPE error, PlayerServiceTddTest 1, PlayerServiceTest 1, ScoreServiceTddTest 3). Do NOT fix these — they are pre-existing.
- [KNOWN] Frontend: 2254 tests run, 534 pre-existing `fakeAsync(async () => {})` Zone.js failures (tracked as `sprint13-fix-534-tests`). Do NOT fix these — they are in scope of the parallel story.
- [KNOWN] `logback-spring.xml` references `DistributedTracingConfig` and `DatabaseMonitoringConfig` loggers — these classes may or may not exist. If they don't exist, logback silently ignores them. Do NOT remove these logger declarations.
- [KNOWN] `logback-spring.xml` uses `LoggingEventCompositeJsonEncoder` in prod JSON_FILE appender — this requires `net.logstash.logback:logstash-logback-encoder` at runtime. It's in pom.xml, so this is fine.
- [KNOWN] Spring Boot 3.4 `logging.structured.format.*` is NOT compatible with existing `logback-spring.xml` — do NOT attempt to use it. The logback-spring.xml takes full precedence.

### Project Structure Notes

New files:
- `src/main/java/com/fortnite/pronos/config/CorrelationIdFilter.java`
- `src/main/java/com/fortnite/pronos/config/StompMdcInterceptor.java`
- `src/test/java/com/fortnite/pronos/config/CorrelationIdFilterTest.java`
- `src/test/java/com/fortnite/pronos/config/StompMdcInterceptorTest.java`

Modified files:
- `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java` — inject + register StompMdcInterceptor
- `src/main/resources/logback-spring.xml` — dev CONSOLE_LOG_PATTERN update (1 line)

No new @RestController → No `SecurityConfig*AuthorizationTest` required for this story.

### References

- [Source: `_bmad-output/implementation-artifacts/sprint-status.yaml`, sprint-13 section] — ADR-S13-001 + DoD + dependency chain
- [Source: `_bmad-output/implementation-artifacts/sprint12-retro-2026-03-20.md`, §4, L4, §7 A1] — BUG-06 context, structured logging rationale
- [Source: `src/main/resources/logback-spring.xml`] — existing JSON appender, MDC keys in patterns
- [Source: `src/main/java/com/fortnite/pronos/config/WebSocketAuthInterceptor.java`] — existing ChannelInterceptor pattern (CONNECT-only)
- [Source: `src/main/java/com/fortnite/pronos/config/RateLimitingFilter.java`] — reference OncePerRequestFilter pattern
- [Source: `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java`] — registration point for new interceptor

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

N/A — No debugging required; all 8 new tests passed on first run.

### Completion Notes List

- ✅ Task 1: Audit confirmed all gaps — logback patterns reference correlationId but nothing set it; no MDC.clear() risk identified from LoggingUtils (uses `traceId/userId/action/duration/status` keys).
- ✅ Task 2: `logback-spring.xml` dev CONSOLE_LOG_PATTERN now includes `[%X{correlationId:-}]` for local visibility.
- ✅ Task 3: `CorrelationIdFilter` created — `@Order(1)`, reads/generates 16-char UUID, sets MDC(correlationId+requestId), echoes in response header, removes via MDC.remove() in finally.
- ✅ Task 4: `StompMdcInterceptor` created — intercepts ALL STOMP commands (not just CONNECT), sets MDC(correlationId+stompCommand+stompDestination+stompUser), logs SEND events at DEBUG for BUG-06 diagnosis, never returns null, clears in afterSendCompletion.
- ✅ Task 5: `WebSocketConfig` updated — StompMdcInterceptor injected and registered BEFORE WebSocketAuthInterceptor so MDC is active when auth logs fire.
- ✅ Task 6: 8 new Mockito unit tests (4+4), all passing. Fixed pre-existing `WebSocketConfigTest` compilation error (constructor now takes 2 args). Full suite: 2391 run, 0 failures.
- DoD met: STOMP pick events (SEND commands to /app/draft/*) now visible in logs with user+destination+correlationId, enabling BUG-06 root cause analysis.

### File List

- `src/main/java/com/fortnite/pronos/config/CorrelationIdFilter.java` [NEW]
- `src/main/java/com/fortnite/pronos/config/StompMdcInterceptor.java` [NEW]
- `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java` [MODIFIED]
- `src/main/resources/logback-spring.xml` [MODIFIED]
- `src/test/java/com/fortnite/pronos/config/CorrelationIdFilterTest.java` [NEW]
- `src/test/java/com/fortnite/pronos/config/StompMdcInterceptorTest.java` [NEW]
- `src/test/java/com/fortnite/pronos/config/WebSocketConfigTest.java` [MODIFIED — fixed constructor to pass StompMdcInterceptor]
