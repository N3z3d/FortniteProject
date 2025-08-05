# ADR-003: API Design Standards

**Status:** Accepted  
**Date:** 2025-08-03  
**Deciders:** Development Team  

## Context

The Fortnite Pronos application requires consistent, well-documented REST APIs that:
- Provide clear and intuitive endpoints for frontend consumption
- Support both web and potential mobile clients
- Maintain backward compatibility as the application evolves
- Follow industry standards for REST API design
- Include comprehensive documentation for developer experience

## Decision

We adopt **RESTful API design principles** with **OpenAPI/Swagger documentation** and **standardized response formats**.

### Core Design Principles

1. **Resource-Based URLs:** Use nouns to represent resources
2. **HTTP Methods:** Use appropriate verbs for operations
3. **Consistent Response Format:** Unified structure across all endpoints
4. **Comprehensive Documentation:** OpenAPI 3.0 specification
5. **Error Handling:** Standardized error responses with meaningful messages

## API Design Standards

### URL Structure
```
{base_url}/api/{version}/{resource}/{id}/{sub-resource}

Examples:
GET    /api/games                          # Get all games
POST   /api/games                          # Create new game
GET    /api/games/{id}                     # Get specific game
PUT    /api/games/{id}                     # Update game
DELETE /api/games/{id}                     # Delete game
GET    /api/games/{id}/participants        # Get game participants
POST   /api/games/{id}/join                # Join game (action)
POST   /api/games/{id}/start-draft         # Start draft (action)
```

### HTTP Method Usage
```
GET     - Retrieve data (idempotent, cacheable)
POST    - Create resources or trigger actions
PUT     - Update/replace entire resource (idempotent)
PATCH   - Partial updates (rare usage)
DELETE  - Remove resources (idempotent)
```

### Standardized Response Format
All API responses follow a consistent wrapper format:

```json
{
  "success": boolean,
  "data": any,
  "message": string,
  "timestamp": ISO8601,
  "pagination"?: PaginationInfo,
  "error"?: ErrorDetails
}
```

#### Successful Response Example
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Championship League 2025",
    "status": "WAITING_FOR_PLAYERS",
    "maxParticipants": 10,
    "currentParticipants": 3
  },
  "message": "Game retrieved successfully",
  "timestamp": "2025-08-03T10:30:00Z"
}
```

#### Error Response Example
```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": ["Game name must be between 3 and 50 characters"],
    "fieldErrors": [
      {
        "field": "name",
        "rejectedValue": "AB",
        "message": "Game name must be between 3 and 50 characters"
      }
    ]
  },
  "timestamp": "2025-08-03T10:30:00Z"
}
```

### HTTP Status Code Standards
```
2xx Success
200 OK           - Successful GET, PUT requests
201 Created      - Successful POST requests (resource creation)
204 No Content   - Successful DELETE requests

4xx Client Errors
400 Bad Request  - Invalid request format or data
401 Unauthorized - Authentication required
403 Forbidden    - Access denied (authenticated but not authorized)
404 Not Found    - Resource does not exist
409 Conflict     - Business rule violation (e.g., game full)
422 Unprocessable Entity - Validation errors
429 Too Many Requests - Rate limiting

5xx Server Errors
500 Internal Server Error - Unexpected server errors
503 Service Unavailable   - Temporary service disruption
```

## OpenAPI Documentation Standards

### API Specification Structure
```java
@OpenAPIDefinition(
    info = @Info(
        title = "Fortnite Pronos API",
        version = "1.0.0",
        description = "Fantasy league management API for Fortnite players"
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development"),
        @Server(url = "https://api.fortnite-pronos.com", description = "Production")
    }
)
```

### Endpoint Documentation Requirements
Every endpoint must include:

```java
@Operation(
    summary = "Brief description of the operation",
    description = """
    Detailed description with:
    - Business context
    - Usage examples
    - Prerequisites
    - State changes
    """,
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Success description",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ResponseDto.class),
            examples = @ExampleObject(name = "example", value = "...")
        )
    ),
    @ApiResponse(responseCode = "400", description = "Error description"),
    @ApiResponse(responseCode = "401", description = "Authentication required")
})
```

### DTO Documentation Standards
```java
@Schema(description = "Request to create a new fantasy league game")
public class CreateGameRequest {
    
    @Schema(
        description = "Unique name for the fantasy league game",
        example = "Championship League 2025",
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Game name is required")
    @Size(min = 3, max = 50)
    private String name;
    
    @Schema(
        description = "Maximum number of participants allowed in the game",
        example = "10",
        minimum = "2",
        maximum = "20"
    )
    @NotNull
    @Min(2) @Max(20)
    private Integer maxParticipants;
}
```

## Request/Response Patterns

### Pagination Standard
For endpoints returning lists:
```json
{
  "success": true,
  "data": [...],
  "message": "Games retrieved successfully",
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2025-08-03T10:30:00Z"
}
```

### Query Parameters Standard
```
GET /api/games?page=0&size=20&sort=createdAt,desc&status=ACTIVE&search=championship

Parameters:
- page: Zero-based page number (default: 0)
- size: Number of items per page (default: 20, max: 100)
- sort: Sort criteria as property,direction (default: createdAt,desc)
- {filter}: Resource-specific filters
```

### Date/Time Format
- **ISO 8601 format:** `2025-08-03T10:30:00Z`
- **Timezone:** UTC for all API communications
- **Precision:** Seconds (no milliseconds in API responses)

## Authentication & Security

### JWT Bearer Token Authentication
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Security Requirements in OpenAPI
```java
@SecurityRequirement(name = "bearerAuth")
```

### Rate Limiting Headers
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1609459200
```

## Error Handling Standards

### Error Code Categories
```java
public enum ErrorCode {
    // Validation errors (4xx)
    VALIDATION_ERROR("VALIDATION_ERROR"),
    INVALID_REQUEST_FORMAT("INVALID_REQUEST_FORMAT"),
    
    // Authentication/Authorization (401/403)
    UNAUTHORIZED("UNAUTHORIZED"),
    ACCESS_DENIED("ACCESS_DENIED"),
    INVALID_TOKEN("INVALID_TOKEN"),
    
    // Business logic errors (409/422)
    GAME_NOT_FOUND("GAME_NOT_FOUND"),
    GAME_FULL("GAME_FULL"),
    INVALID_GAME_STATE("INVALID_GAME_STATE"),
    
    // System errors (5xx)
    INTERNAL_ERROR("INTERNAL_ERROR"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE")
}
```

### Error Response Structure
```java
@Schema(description = "Error details")
public class ErrorDetails {
    @Schema(description = "Machine-readable error code")
    private String code;
    
    @Schema(description = "Human-readable error message")
    private String message;
    
    @Schema(description = "Additional error context")
    private List<String> details;
    
    @Schema(description = "Field-specific validation errors")
    private List<FieldError> fieldErrors;
}
```

## Versioning Strategy

### URL Versioning (Current Approach)
```
/api/v1/games     # Version 1
/api/v2/games     # Version 2 (future)
```

### Backward Compatibility Rules
- No breaking changes within the same major version
- Deprecation warnings for 2 versions before removal
- Additive changes (new fields) are non-breaking
- Optional parameters can be added

### Breaking Change Examples
```
❌ Breaking Changes:
- Removing fields from responses
- Changing field types or formats
- Making optional parameters required
- Changing URL structure

✅ Non-Breaking Changes:
- Adding new optional fields
- Adding new endpoints
- Adding new optional parameters
- Expanding enum values
```

## Implementation Guidelines

### Controller Implementation Pattern
```java
@Tag(name = "Games", description = "Fantasy league game management")
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Create a new fantasy league game")
    @PostMapping
    public ResponseEntity<ApiResponse<GameDto>> createGame(
        @Valid @RequestBody CreateGameRequest request) {
        
        try {
            GameDto game = gameService.createGame(getCurrentUserId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(game, "Game created successfully"));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }
}
```

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getFieldErrors().stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR", 
                                       null, fieldErrors));
    }
}
```

## API Testing Standards

### Contract Testing
- OpenAPI specification drives contract tests
- Ensure request/response schemas match documentation
- Validate example data in documentation

### Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameControllerIntegrationTest {

    @Test
    void shouldCreateGameWithValidRequest() {
        CreateGameRequest request = CreateGameRequest.builder()
                .name("Test Game")
                .maxParticipants(10)
                .build();

        ResponseEntity<ApiResponse<GameDto>> response = testRestTemplate
                .postForEntity("/api/games", request, 
                              new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getName()).isEqualTo("Test Game");
    }
}
```

## Consequences

### Positive
- **Consistency:** Uniform API experience across all endpoints
- **Developer Experience:** Comprehensive documentation reduces integration time
- **Maintainability:** Standardized patterns make code easier to understand
- **Testability:** Clear contracts enable robust testing strategies
- **Evolvability:** Versioning strategy supports backward compatibility

### Negative
- **Overhead:** Additional wrapper structures increase response size
- **Complexity:** More comprehensive documentation requires more maintenance
- **Performance:** Additional abstraction layers may introduce latency

### Mitigation Strategies
- Use compression for API responses to offset wrapper overhead
- Automate documentation generation from code annotations
- Monitor API performance and optimize critical endpoints

## Compliance and Monitoring

### API Documentation Compliance
- 100% OpenAPI coverage for all public endpoints
- Regular documentation audits during code reviews
- Automated testing of example data accuracy

### Performance Monitoring
- API response time tracking
- Error rate monitoring by endpoint
- Rate limiting effectiveness measurement

## Related ADRs
- ADR-001: Layered Architecture (Controller responsibilities)
- ADR-002: Database Technology (Data modeling for APIs)
- ADR-004: Authentication Strategy (Security implementation)