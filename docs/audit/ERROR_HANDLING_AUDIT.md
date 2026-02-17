# Error Handling Audit Report

**Date**: 2026-02-17
**Ticket**: JIRA-AUDIT-009
**Scope**: Backend (Spring Boot) + Frontend (Angular)

---

## 1. Backend - GlobalExceptionHandler Coverage

### 1.1 Handled Exceptions (14 handlers)

| Exception | HTTP Status | Code | Response Format |
|-----------|------------|------|-----------------|
| `FortnitePronosException` | Dynamic (from ErrorCode) | ErrorCode.name() | ErrorResponse (standard) |
| `AuthenticationException` | 401 | - | ErrorResponse + accessibility hints |
| `UserNotFoundException` | 401 | USER_NOT_FOUND | ErrorResponse |
| `AccessDeniedException` | 403 | - | ErrorResponse |
| `SecurityException` | 403 | - | ErrorResponse |
| `MethodArgumentNotValidException` | 400 | - | ErrorResponse + validationErrors map |
| `ConstraintViolationException` | 400 | - | ErrorResponse |
| `MethodArgumentTypeMismatchException` | 400 | - | ErrorResponse |
| `IllegalArgumentException` | 400 | - | ErrorResponse (generic message) |
| `IllegalStateException` | 409 | - | ErrorResponse + accessibility hint |
| `GameNotFoundException` | 404 | GAME_NOT_FOUND | ErrorResponse |
| `GameFullException` | 400 | GAME_FULL | ErrorResponse |
| `UserAlreadyInGameException` | 409 | USER_ALREADY_IN_GAME | ErrorResponse |
| `InvalidGameStateException` | 409 | INVALID_GAME_STATE | ErrorResponse |
| `InvalidGameRequestException` | 409 | INVALID_GAME_REQUEST | ErrorResponse |
| `NoResourceFoundException` / `NoHandlerFoundException` | 404 | - | ErrorResponse |
| `Exception` (catch-all) | 500 | - | ErrorResponse (with fallback for join) |

### 1.2 UNHANDLED Exceptions (thrown but no dedicated handler)

| Exception | Thrown By | Falls Into | Actual HTTP Status |
|-----------|----------|------------|-------------------|
| **`BusinessException`** | TradingService (16x), ValidationService (7x) | `Exception` catch-all | **500** (should be 400/409) |
| **`UnauthorizedAccessException`** | GameParticipantService, GameDraftService | `Exception` catch-all | **500** (should be 403) |
| **`TeamNotFoundException`** | TeamService | `Exception` catch-all | **500** (should be 404) |
| **`DraftIncompleteException`** | GameDraftService | `Exception` catch-all | **500** (should be 409) |
| **`NotYourTurnException`** | GameDraftService | `Exception` catch-all | **500** (should be 409) |
| **`PlayerAlreadySelectedException`** | GameDraftService | `Exception` catch-all | **500** (should be 409) |
| **`InvalidDraftStateException`** | (not actively thrown) | `Exception` catch-all | 500 |
| **`InvalidSwapException`** | TeamService | `Exception` catch-all | **500** (should be 400) |
| **`RegionLimitExceededException`** | (not actively thrown) | `Exception` catch-all | 500 |
| **`InsufficientParticipantsException`** | (not actively thrown) | `Exception` catch-all | 500 |
| **`FortniteTrackerException`** | FortniteTrackerService | `Exception` catch-all | **500** (acceptable for external API) |
| `EntityNotFoundException` | TeamService | `Exception` catch-all | **500** (should be 404) |
| `BadCredentialsException` | ProductionAuthenticationStrategy | `AuthenticationException` | 401 (OK) |

**CRITICAL**: 6 actively-thrown custom exceptions return 500 instead of proper status codes.

### 1.3 Incorrect HTTP Status Codes

| Issue | Current | Should Be | Rationale |
|-------|---------|-----------|-----------|
| `InvalidGameRequestException` -> 409 | 409 CONFLICT | **400 BAD_REQUEST** | Validation error, not state conflict |
| `GameFullException` -> 400 | 400 BAD_REQUEST | **409 CONFLICT** | Server state prevents action |
| `UserNotFoundException` -> 401 | 401 UNAUTHORIZED | Context-dependent* | When thrown from GameCreationService, it's a 404, not 401 |

*Note: `UserNotFoundException` is dual-use: thrown during login (correct 401) AND during game creation (should be 404). Consider splitting into `AuthUserNotFoundException` vs `UserNotFoundException`.

### 1.4 Generic RuntimeException Usage (CRITICAL)

These throw bare `RuntimeException` instead of domain-specific exceptions:

| File | Message | Should Be |
|------|---------|-----------|
| FlexibleAuthenticationService:49 | "Utilisateur non authentifie" | `AuthenticationException` |
| DevelopmentAuthenticationStrategy:51 | "Mot de passe incorrect" | `BadCredentialsException` |
| DevelopmentAuthenticationStrategy:92 | "Token de rafraichissement invalide" | `BadCredentialsException` |
| SpringSecurityAuthenticationStrategy:98 | "Token de rafraichissement invalide" | `BadCredentialsException` |
| ProductionAuthenticationStrategy:113 | "Erreur interne lors de la connexion" | `AuthenticationException` |
| ProductionAuthenticationStrategy:179 | "Erreur lors de la generation du token" | `FortnitePronosException` |
| ProductionAuthenticationStrategy:199 | "Erreur lors de la generation du refresh token" | `FortnitePronosException` |
| UnifiedAuthService:87,107,110 | "Token de rafraichissement invalide" | `BadCredentialsException` |
| JwtService:187 | "Invalid JWT token" | `BadCredentialsException` |
| PlayerLeaderboardService:120,219 | "Erreur lors de la generation du classement" | `FortnitePronosException` |
| TeamLeaderboardService:164 | "Equipe non trouvee" | `TeamNotFoundException` |

**Total: 13 bare RuntimeException throws**

### 1.5 Mixed Language Messages

| Language | Count | Files |
|----------|-------|-------|
| French | ~35 | ValidationService, TeamService, UserContextService, auth services, model validators |
| English | ~55 | GameParticipantService, GameDraftService, domain models, JoinGameUseCase, CreateGameUseCase |
| Mixed (both in same file) | 5 | TeamService, ValidationService, GameCreationService |

---

## 2. Backend - Controller-Level Error Handling

### 2.1 Controllers with Local try-catch (bypass GlobalExceptionHandler)

| Controller | Catch Pattern | Response on Error | Issue |
|-----------|---------------|-------------------|-------|
| **ApiController** | `catch (Exception)` | `404 null body` | Null body, no ErrorResponse format |
| **ApiController** | `catch (Exception)` | `500 "An error occurred: " + msg` | Raw string, not ErrorResponse |
| **ApiController** | `catch (Exception)` | `500 Map.of("error", ...)` | Non-standard response format |
| **LeaderboardController** | `catch (Exception)` x8 | `500 empty body` or `404 empty body` | Empty body, no error info |
| **DraftController** | `catch (Exception)` x3 | `500 Map.of("error", ...)` | Non-standard response format |
| **DraftController** | `catch (IllegalArgumentException)` | `400 Map.of("error", ...)` | OK but non-standard format |
| **DraftController** | `catch (IllegalArgumentException)` | `400 empty body` | No error info at all |
| **TeamController** | `catch (IllegalArgumentException)` | `404 empty body` | Wrong status + empty body |
| **TeamController** | `catch (Exception)` | `500 empty body` | Empty body |
| **AuthController** | `catch (Exception)` | re-throws | OK (delegates to GlobalExceptionHandler) |

**CRITICAL**: 15 catch blocks return non-standard responses (not `ErrorResponse` format), making frontend error parsing unreliable.

### 2.2 Recommendation: Remove local try-catch, let GlobalExceptionHandler handle all errors consistently.

---

## 3. Frontend - Error Handling Patterns

### 3.1 catchError Usage Summary

| Pattern | Count | Assessment |
|---------|-------|------------|
| `catchError` -> rethrow (`throwError`) | ~5 | Good |
| `catchError` -> return empty array `of([])` | ~8 | Silent failure, no user notification |
| `catchError` -> return fallback data | ~6 | Acceptable for non-critical data |
| `catchError` -> log + return empty | ~10 | Silent failure |
| `catchError` -> snackbar + return empty | ~4 | Good - user notified |

### 3.2 Silent Error Swallowing (no user notification)

| File | Line | Pattern | Impact |
|------|------|---------|--------|
| leaderboard.service.ts | 184-188 | `catchError` -> `of([])` | Leaderboard silently empty |
| leaderboard.service.ts | 400-403 | `catchError` -> `of([])` | Player leaderboard silently empty |
| dashboard-data.service.ts | 99 | `catchError` -> `of([])` | Dashboard games silently empty |
| dashboard-data.service.ts | 146 | `catchError` -> `of([])` | Dashboard data silently empty |
| leaderboard.repository.ts | 133-148 | `catchError` -> `of([])` x4 | All leaderboard queries silent |
| dashboard.repository.ts | 195 | `catchError` -> `of([])` | Dashboard repository silent |
| games.resolver.ts | 19,27 | returns `of([])` on error | Route resolver hides error |

### 3.3 Error Messages via Snackbar

| File | Message Source | i18n? |
|------|--------------|-------|
| game-detail-actions.service.ts | Translation keys | Yes |
| game-command.service.ts | Translation keys | Yes |
| join-game.component.ts | Translation keys | Yes |
| main-layout.component.ts | Translation keys | Yes |
| create-game.component.ts | Mixed (some hardcoded) | Partial |

### 3.4 console.error/warn Usage

- **56 occurrences** across 19 files
- Most are in services (game-data, game-detail-actions, mappers)
- Many `console.error` without corresponding user notification

### 3.5 UiErrorFeedbackService

A dedicated error feedback service exists at `core/services/ui-error-feedback.service.ts`. It provides:
- `showError(message)` - snackbar with error styling
- `showErrorWithRetry(message, retryCallback)` - snackbar with retry action
- Uses i18n via `TranslationService`

**Issue**: This service is underused. Most error handlers use raw `console.error` instead.

---

## 4. Summary of Issues by Severity

### P0 - CRITICAL (6 items)

1. **`BusinessException` not handled** - 16 TradingService throws return 500 instead of 400/409
2. **`UnauthorizedAccessException` not handled** - Returns 500 instead of 403
3. **`TeamNotFoundException` not handled** - Returns 500 instead of 404
4. **Draft exceptions not handled** - `DraftIncompleteException`, `NotYourTurnException`, `PlayerAlreadySelectedException` all return 500
5. **`InvalidSwapException` not handled** - Returns 500 instead of 400
6. **13 bare RuntimeException throws** - No semantic error type, all return 500

### P1 - MAJOR (5 items)

7. **15 controller catch blocks** return non-standard response format (not `ErrorResponse`)
8. **`InvalidGameRequestException` uses 409** instead of 400 (validation error)
9. **`GameFullException` uses 400** instead of 409 (state conflict)
10. **Mixed French/English messages** across 5 files
11. **8+ frontend silent error swallows** - `catchError` -> `of([])` without user notification

### P2 - MINOR (4 items)

12. **`UserNotFoundException` dual-use** - 401 for both login and game creation contexts
13. **`UiErrorFeedbackService` underused** - Most frontend errors use `console.error` only
14. **LeaderboardController empty error bodies** - 8 catch blocks return empty response
15. **GlobalExceptionHandler encoding corruption** - Mojibake in French comments/strings

---

## 5. Recommended Fix Plan

### Phase 1: Add missing GlobalExceptionHandler handlers (P0)
- Add handlers for: `BusinessException` (400), `UnauthorizedAccessException` (403), `TeamNotFoundException` (404), `DraftIncompleteException` (409), `NotYourTurnException` (409), `PlayerAlreadySelectedException` (409), `InvalidSwapException` (400)
- Fix: `InvalidGameRequestException` -> 400, `GameFullException` -> 409
- **Effort**: 2h, **Impact**: 6 exception types fixed

### Phase 2: Remove controller local try-catch (P1)
- Remove catch blocks from LeaderboardController, ApiController, DraftController, TeamController
- Let GlobalExceptionHandler handle all errors consistently
- **Effort**: 2h, **Impact**: 15 catch blocks standardized

### Phase 3: Replace bare RuntimeException (P0)
- Replace 13 `RuntimeException` throws with domain-specific exceptions
- **Effort**: 1h, **Impact**: Proper error codes for all auth/token errors

### Phase 4: Frontend silent error notification (P1)
- Replace `catchError` -> `of([])` with `UiErrorFeedbackService.showError()` + `of([])`
- Centralize error display through `UiErrorFeedbackService`
- **Effort**: 2h, **Impact**: 8+ silent failures now visible to users

### Phase 5: Harmonize error messages (P1)
- Choose English for all backend error messages (consistent with domain layer)
- Use i18n keys in frontend for all user-facing messages
- **Effort**: 2h, **Impact**: Consistent language across entire stack
