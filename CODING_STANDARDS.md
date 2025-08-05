# Fortnite Pronos - Coding Standards & Best Practices

## 📋 **Table of Contents**
1. [Overview](#overview)
2. [Java/Spring Boot Standards](#java-spring-boot-standards)
3. [TypeScript/Angular Standards](#typescript-angular-standards)
4. [API Design Guidelines](#api-design-guidelines)
5. [Database Standards](#database-standards)
6. [Security Guidelines](#security-guidelines)
7. [Testing Standards](#testing-standards)
8. [Documentation Requirements](#documentation-requirements)
9. [Code Review Checklist](#code-review-checklist)
10. [Enforcement & Automation](#enforcement-automation)

---

## **Overview**

This document establishes comprehensive coding standards for the Fortnite Pronos application, ensuring:
- **Consistency**: Uniform code style across all team members
- **Maintainability**: Clean, readable code that's easy to modify
- **Security**: Best practices for secure development
- **Performance**: Optimized code for 150+ concurrent users
- **Quality**: High-quality, testable, and reliable code

### **Core Principles**
- **SOLID Principles**: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **DRY (Don't Repeat Yourself)**: Eliminate code duplication
- **KISS (Keep It Simple, Stupid)**: Prefer simple, clear solutions
- **YAGNI (You Aren't Gonna Need It)**: Don't implement features until needed
- **Fail Fast**: Validate early and provide clear error messages

---

## **Java/Spring Boot Standards**

### **Project Structure**
```
src/main/java/com/fortnite/pronos/
├── controller/          # REST API controllers
├── service/            # Business logic services
├── repository/         # Data access layer (Spring Data JPA)
├── model/             # JPA entities
├── dto/               # Data Transfer Objects
├── config/            # Configuration classes
├── exception/         # Custom exceptions
├── core/              # Core domain logic
└── util/              # Utility classes
```

### **Naming Conventions**

#### **Classes**
```java
// ✅ GOOD - Clear, descriptive names
public class GameService { }
public class CreateGameRequest { }
public class GameNotFoundException extends RuntimeException { }

// ❌ BAD - Abbreviations, unclear names
public class GmSvc { }
public class CGR { }
public class GNF extends RuntimeException { }
```

#### **Methods**
```java
// ✅ GOOD - Verbs that clearly describe action
public GameDto createGame(UUID userId, CreateGameRequest request) { }
public void validateGameRules(Game game) { }
public Optional<User> findUserByEmail(String email) { }

// ❌ BAD - Unclear verbs, missing context
public GameDto process(UUID id, Object req) { }
public void check(Game g) { }
public Optional<User> find(String s) { }
```

#### **Constants**
```java
// ✅ GOOD - UPPER_CASE with business meaning
public static final int MAX_PARTICIPANTS_PER_GAME = 20;
public static final String JWT_SECRET_KEY = "jwt.secret";
public static final Duration TOKEN_EXPIRATION_TIME = Duration.ofHours(24);

// ❌ BAD - Lowercase, meaningless names
public static final int max = 20;
public static final String key = "jwt.secret";
public static final Duration time = Duration.ofHours(24);
```

#### **Variables**
```java
// ✅ GOOD - Clear, descriptive camelCase
User currentUser = getCurrentUser();
List<GameDto> availableGames = gameService.getAvailableGames();
UUID gameId = request.getGameId();

// ❌ BAD - Abbreviations, single letters
User u = getCurrentUser();
List<GameDto> games = gameService.getAvailableGames();
UUID id = request.getGameId();
```

### **Service Layer Standards**

#### **Service Method Naming**
```java
// ✅ GOOD - Clear CRUD operations
public GameDto createGame(UUID userId, CreateGameRequest request) { }
public GameDto updateGame(UUID gameId, UpdateGameRequest request) { }
public void deleteGame(UUID gameId) { }
public Optional<GameDto> findGameById(UUID gameId) { }
public List<GameDto> findGamesByUser(UUID userId) { }

// ✅ GOOD - Business operations
public DraftDto startDraft(UUID gameId, UUID userId) { }
public void joinGame(UUID userId, JoinGameRequest request) { }
public void swapPlayers(UUID teamId, SwapPlayersRequest request) { }
```

#### **Service Implementation Pattern**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default to read-only
public class GameService {

    private final GameRepository gameRepository;
    private final ValidationService validationService;
    
    @Transactional // Override for write operations
    public GameDto createGame(UUID userId, CreateGameRequest request) {
        // 1. Validate input
        validationService.validateCreateGameRequest(request);
        
        // 2. Check business rules
        validateUserCanCreateGame(userId);
        
        // 3. Execute business logic
        Game game = buildGameFromRequest(userId, request);
        Game savedGame = gameRepository.save(game);
        
        // 4. Convert and return
        return GameMapper.toDto(savedGame);
    }
    
    private void validateUserCanCreateGame(UUID userId) {
        // Implementation...
    }
    
    private Game buildGameFromRequest(UUID userId, CreateGameRequest request) {
        // Implementation...
    }
}
```

### **Controller Standards**

#### **Controller Structure**
```java
@Tag(name = "Games", description = "Fantasy league game management")
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final ValidationService validationService;

    @Operation(summary = "Create a new fantasy league game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Game created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
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

### **Exception Handling Standards**

#### **Custom Exception Hierarchy**
```java
// Base exception
public abstract class FortnitePronosException extends RuntimeException {
    private final ErrorCode errorCode;
    
    protected FortnitePronosException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// Specific exceptions
public class GameNotFoundException extends FortnitePronosException {
    public GameNotFoundException(UUID gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "Game not found with ID: " + gameId);
    }
}

public class GameFullException extends FortnitePronosException {
    public GameFullException(String gameName) {
        super(ErrorCode.GAME_FULL, "Game '" + gameName + "' is full");
    }
}
```

#### **Global Exception Handler**
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleGameNotFound(GameNotFoundException e) {
        log.warn("Game not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage(), "GAME_NOT_FOUND"));
    }
}
```

### **DTO Standards**

#### **DTO Naming and Structure**
```java
// ✅ GOOD - Clear suffix, immutable design
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new fantasy league game")
public class CreateGameRequest {
    
    @Schema(description = "Game name", example = "Championship League 2025")
    @NotBlank(message = "Game name is required")
    @Size(min = 3, max = 50, message = "Game name must be between 3 and 50 characters")
    private String name;
    
    @Schema(description = "Maximum number of participants", example = "10")
    @Min(value = 2, message = "Minimum 2 participants required")
    @Max(value = 20, message = "Maximum 20 participants allowed")
    private Integer maxParticipants;
    
    @Schema(description = "Game privacy setting", example = "false")
    private Boolean isPrivate = false;
}

// ❌ BAD - Unclear naming, mutable fields
public class GameReq {
    public String n;
    public int max;
    public boolean priv;
}
```

---

## **TypeScript/Angular Standards**

### **Project Structure**
```
frontend/src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── services/           # Application-wide services
│   ├── guards/            # Route guards
│   ├── interceptors/      # HTTP interceptors
│   └── models/            # Core interfaces and types
├── features/              # Feature modules
│   ├── auth/             # Authentication feature
│   ├── games/            # Game management feature
│   └── leaderboard/      # Leaderboard feature
├── shared/               # Shared components and utilities
│   ├── components/       # Reusable UI components
│   ├── directives/       # Custom directives
│   ├── pipes/           # Custom pipes
│   └── services/        # Shared services
└── environments/         # Environment configurations
```

### **TypeScript Configuration**
```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true
  }
}
```

### **Interface and Type Standards**

#### **Interface Naming**
```typescript
// ✅ GOOD - Clear, descriptive interfaces
interface Game {
  id: string;
  name: string;
  status: GameStatus;
  maxParticipants: number;
  currentParticipants: number;
  createdAt: Date;
}

interface CreateGameRequest {
  name: string;
  maxParticipants: number;
  isPrivate: boolean;
}

// ❌ BAD - Unclear, abbreviated names
interface G {
  i: string;
  n: string;
  s: number;
}
```

#### **Service Standards**
```typescript
@Injectable({
  providedIn: 'root'
})
export class GameService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/games`;

  constructor(private http: HttpClient) {}

  createGame(request: CreateGameRequest): Observable<ApiResponse<Game>> {
    return this.http.post<ApiResponse<Game>>(this.apiUrl, request)
      .pipe(
        catchError(this.handleError),
        tap(response => this.logResponse('createGame', response))
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('API Error:', error);
    return throwError(() => new Error(error.message));
  }

  private logResponse(operation: string, response: any): void {
    console.log(`${operation} completed:`, response);
  }
}
```

### **Component Standards**

#### **Component Structure**
```typescript
@Component({
  selector: 'app-game-list',
  templateUrl: './game-list.component.html',
  styleUrls: ['./game-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameListComponent implements OnInit, OnDestroy {
  games$ = this.gameService.getAvailableGames();
  loading = false;
  error: string | null = null;
  
  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadGames();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onJoinGame(gameId: string): void {
    this.gameService.joinGame(gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.router.navigate(['/games', gameId]),
        error: (error) => this.handleError(error)
      });
  }

  private loadGames(): void {
    this.loading = true;
    // Implementation...
  }

  private handleError(error: any): void {
    this.error = error.message;
    this.cdr.markForCheck();
  }
}
```

---

## **API Design Guidelines**

### **RESTful Endpoint Structure**
```
# Resource-based URLs
GET    /api/games                    # Get all games
POST   /api/games                    # Create new game
GET    /api/games/{id}               # Get specific game
PUT    /api/games/{id}               # Update game
DELETE /api/games/{id}               # Delete game

# Nested resources
GET    /api/games/{id}/participants  # Get game participants
POST   /api/games/{id}/join          # Join game
POST   /api/games/{id}/start-draft   # Start draft

# Non-CRUD operations use verbs
POST   /api/games/{id}/start-draft
POST   /api/teams/{id}/swap-players
POST   /api/auth/login
POST   /api/auth/refresh
```

### **HTTP Status Codes**
```
200 OK           - Successful GET, PUT requests
201 Created      - Successful POST requests
204 No Content   - Successful DELETE requests
400 Bad Request  - Invalid request data
401 Unauthorized - Authentication required
403 Forbidden    - Access denied
404 Not Found    - Resource not found
409 Conflict     - Business rule violation
422 Unprocessable Entity - Validation errors
429 Too Many Requests - Rate limiting
500 Internal Server Error - Server errors
```

### **Request/Response Standards**

#### **Request Format**
```json
{
  "name": "Championship League 2025",
  "maxParticipants": 10,
  "isPrivate": false,
  "regionRules": {
    "EU": { "min": 2, "max": 4 },
    "NAE": { "min": 1, "max": 3 }
  }
}
```

#### **Response Format**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Championship League 2025",
    "status": "WAITING_FOR_PLAYERS"
  },
  "message": "Game created successfully",
  "timestamp": "2025-08-03T10:30:00Z"
}
```

#### **Error Response Format**
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

---

## **Database Standards**

### **Table Naming**
```sql
-- ✅ GOOD - Plural nouns, snake_case
CREATE TABLE games (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE game_participants (
    game_id UUID REFERENCES games(id),
    user_id UUID REFERENCES users(id),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (game_id, user_id)
);

-- ❌ BAD - Singular, inconsistent naming
CREATE TABLE game (
    gameId UUID PRIMARY KEY,
    gameName VARCHAR(50) NOT NULL
);
```

### **Column Naming**
```sql
-- ✅ GOOD - snake_case, descriptive names
id UUID PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
max_participants INTEGER NOT NULL,
is_private BOOLEAN NOT NULL DEFAULT FALSE,
current_season INTEGER NOT NULL

-- ❌ BAD - camelCase, abbreviations
gameId UUID PRIMARY KEY,
createDt TIMESTAMP,
maxPart INTEGER,
priv BOOLEAN
```

### **Index Strategy**
```sql
-- Primary queries optimization
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_created_by ON games(created_by);
CREATE INDEX idx_game_participants_user_id ON game_participants(user_id);
CREATE INDEX idx_scores_player_season ON scores(player_id, season);

-- Composite indexes for complex queries
CREATE INDEX idx_games_status_created_at ON games(status, created_at);
CREATE INDEX idx_teams_user_season ON teams(user_id, season);
```

---

## **Security Guidelines**

### **Authentication & Authorization**
```java
// ✅ GOOD - Secure JWT implementation
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey; // From environment variable
    
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// ❌ BAD - Hardcoded secrets
private static final String SECRET = "mySecretKey123";
```

### **Input Validation**
```java
// ✅ GOOD - Comprehensive validation
@Data
public class CreateGameRequest {
    
    @NotBlank(message = "Game name is required")
    @Size(min = 3, max = 50, message = "Game name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-_]+$", message = "Game name contains invalid characters")
    private String name;
    
    @NotNull(message = "Max participants is required")
    @Min(value = 2, message = "Minimum 2 participants required")
    @Max(value = 20, message = "Maximum 20 participants allowed")
    private Integer maxParticipants;
}

// ❌ BAD - No validation
@Data
public class CreateGameRequest {
    private String name;
    private Integer maxParticipants;
}
```

### **SQL Injection Prevention**
```java
// ✅ GOOD - Parameterized queries
@Query("SELECT g FROM Game g WHERE g.name LIKE %:searchTerm% AND g.status = :status")
List<Game> findByNameContainingAndStatus(@Param("searchTerm") String searchTerm, 
                                        @Param("status") GameStatus status);

// ❌ BAD - String concatenation
@Query("SELECT g FROM Game g WHERE g.name LIKE '%" + searchTerm + "%'")
List<Game> findByName(String searchTerm);
```

---

## **Testing Standards**

### **Test Structure**
```java
// ✅ GOOD - Clear test structure using AAA pattern
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private ValidationService validationService;
    
    @InjectMocks
    private GameService gameService;

    @Test
    @DisplayName("Should create game successfully when valid request provided")
    void shouldCreateGameSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        CreateGameRequest request = CreateGameRequest.builder()
                .name("Test Game")
                .maxParticipants(10)
                .build();
        
        Game expectedGame = Game.builder()
                .id(UUID.randomUUID())
                .name("Test Game")
                .maxParticipants(10)
                .createdBy(userId)
                .build();
        
        when(gameRepository.save(any(Game.class))).thenReturn(expectedGame);
        
        // Act
        GameDto result = gameService.createGame(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Game");
        assertThat(result.getMaxParticipants()).isEqualTo(10);
        
        verify(validationService).validateCreateGameRequest(request);
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    @DisplayName("Should throw GameNotFoundException when game does not exist")
    void shouldThrowExceptionWhenGameNotFound() {
        // Arrange
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> gameService.getGameById(gameId))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found with ID: " + gameId);
    }
}
```

### **Integration Test Standards**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class GameControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Test
    @Order(1)
    @DisplayName("Should create game via REST API")
    void shouldCreateGameViaApi() {
        // Arrange
        CreateGameRequest request = CreateGameRequest.builder()
                .name("Integration Test Game")
                .maxParticipants(10)
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidJwtToken());
        HttpEntity<CreateGameRequest> entity = new HttpEntity<>(request, headers);
        
        // Act
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/api/games",
                HttpMethod.POST,
                entity,
                GameDto.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Integration Test Game");
        
        // Verify in database
        Optional<Game> savedGame = gameRepository.findById(response.getBody().getId());
        assertThat(savedGame).isPresent();
    }
}
```

---

## **Documentation Requirements**

### **Code Comments**
```java
// ✅ GOOD - Explains WHY, not WHAT
/**
 * Calculates the fantasy points for a player based on their Fortnite performance.
 * Uses a weighted scoring system where eliminations have higher impact than placement.
 * This encourages aggressive play style in the fantasy league.
 */
public int calculateFantasyPoints(PlayerStats stats) {
    // Weighted formula: eliminations worth 3x placement points
    // This design decision promotes offensive strategies
    return (stats.getEliminations() * 3) + stats.getPlacementPoints();
}

// ❌ BAD - States the obvious
/**
 * This method calculates points
 */
public int calculateFantasyPoints(PlayerStats stats) {
    // Calculate the points
    return (stats.getEliminations() * 3) + stats.getPlacementPoints();
}
```

### **OpenAPI Documentation**
```java
@Operation(
    summary = "Create a new fantasy league game",
    description = """
    Creates a new fantasy league game with specified configuration.
    
    **Business Rules:**
    - Game name must be unique within the user's games
    - Maximum participants enforced by system limits
    - Region distribution rules must be valid
    
    **State Transitions:**
    CREATING -> WAITING_FOR_PLAYERS -> DRAFT_IN_PROGRESS -> ACTIVE -> COMPLETED
    """,
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "201",
        description = "Game created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = GameDto.class)
        )
    )
})
```

---

## **Code Review Checklist**

### **✅ Functionality**
- [ ] Code implements the required functionality correctly
- [ ] All edge cases are handled appropriately
- [ ] Error handling is comprehensive and meaningful
- [ ] Business rules are correctly implemented

### **✅ Code Quality**
- [ ] Code follows SOLID principles
- [ ] Methods have single responsibility
- [ ] Code is DRY (no duplication)
- [ ] Variable and method names are descriptive
- [ ] Code is readable and well-structured

### **✅ Security**
- [ ] Input validation is implemented
- [ ] No hardcoded secrets or credentials
- [ ] Authentication and authorization are properly handled
- [ ] SQL injection prevention measures in place

### **✅ Performance**
- [ ] Database queries are optimized
- [ ] No N+1 query problems
- [ ] Appropriate caching strategies used
- [ ] Memory usage is considered

### **✅ Testing**
- [ ] Unit tests cover business logic
- [ ] Integration tests cover API endpoints
- [ ] Test names clearly describe what is being tested
- [ ] Tests follow AAA pattern (Arrange, Act, Assert)

### **✅ Documentation**
- [ ] OpenAPI documentation is complete and accurate
- [ ] Complex business logic is commented
- [ ] README files are updated if necessary
- [ ] API examples are provided

---

## **Enforcement & Automation**

### **Pre-commit Hooks**
```bash
#!/bin/bash
# .git/hooks/pre-commit

# Run Java formatting
mvn spotless:check

# Run TypeScript linting
npm run lint --prefix frontend

# Run tests
mvn test
npm test --prefix frontend --watch=false

# Check for security issues
mvn org.owasp:dependency-check-maven:check
```

### **Maven Configuration**
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.19.1</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports />
            <formatAnnotations />
        </java>
    </configuration>
</plugin>
```

### **ESLint Configuration**
```json
{
  "extends": [
    "@angular-eslint/recommended",
    "@typescript-eslint/recommended",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "@angular-eslint/component-class-suffix": "error",
    "@angular-eslint/directive-class-suffix": "error",
    "prefer-const": "error",
    "no-var": "error"
  }
}
```

### **SonarQube Quality Gates**
```yaml
sonar.projectKey=fortnite-pronos
sonar.projectName=Fortnite Pronos
sonar.projectVersion=1.0

# Quality Gates
sonar.qualitygate.wait=true
sonar.coverage.exclusions=**/*Test.java,**/*Spec.ts
sonar.cpd.exclusions=**/*Dto.java,**/*Entity.java

# Thresholds
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
sonar.maintainability_rating.maximum=A
sonar.reliability_rating.maximum=A
sonar.security_rating.maximum=A
```

---

## **Conclusion**

These coding standards ensure the Fortnite Pronos application maintains high quality, security, and maintainability. All team members should follow these guidelines, and the automated enforcement tools will help maintain consistency.

**Key Success Metrics:**
- ✅ **Code Quality**: SonarQube rating A on all dimensions
- ✅ **Test Coverage**: Minimum 80% across all modules
- ✅ **Security**: Zero critical vulnerabilities in OWASP dependency check
- ✅ **Performance**: API response times under 200ms for 95th percentile
- ✅ **Documentation**: 100% OpenAPI coverage for all public endpoints

**Review and Updates:**
This document should be reviewed quarterly and updated based on:
- New technology adoptions
- Lessons learned from code reviews
- Performance optimization discoveries
- Security vulnerability research
- Team feedback and suggestions

---

*Last Updated: August 3, 2025*
*Version: 1.0.0*
*Next Review: November 3, 2025*