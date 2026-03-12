# Story 7.2: Résolution manuelle et correction fiche joueur

Status: done

## Story

As an admin,
I want to correct a player's metadata (username and/or region) in the pipeline,
so that the player catalogue quality remains exploitable without waiting for a full rerun.

## Acceptance Criteria

1. **Given** a player entry exists (any status), **When** the admin sends `PATCH /api/admin/players/{playerId}/metadata` with `{newUsername?, newRegion?}`, **Then** the corrected fields are persisted, the `correctedBy` and `correctedAt` audit fields are set, and the full updated `PlayerIdentityEntryResponse` is returned (200 OK).

2. **Given** a player entry does not exist, **When** the PATCH endpoint is called, **Then** the server responds 404 with a `PlayerIdentityNotFoundException` message.

3. **Given** both `newUsername` and `newRegion` are null/omitted, **When** the PATCH is sent, **Then** the call is accepted (200) but no metadata changes (noop — idempotent).

4. **Given** the admin views the `/admin/pipeline` page, **When** they click "Corriger" on any row (unresolved or resolved), **Then** a dialog opens with the current username and region pre-filled, allowing the admin to edit and confirm.

5. **Given** the dialog is confirmed with new values, **When** the service call succeeds, **Then** the data is refreshed and a success snackbar is shown.

6. **Given** a non-admin user calls the endpoint, **Then** the server responds 403 Forbidden (existing `@PreAuthorize("hasRole('ADMIN')")` covers this).

## Technical Context

### FR Coverage

- **FR-42**: L'admin peut résoudre manuellement l'Epic Account ID d'un joueur UNRESOLVED en 1 clic — **ALREADY DONE** (resolve endpoint exists in `AdminPlayerPipelineController.resolve()`). This story does NOT re-implement it.
- **FR-43**: L'admin peut corriger les données d'un joueur (pseudo, région principale) — **NEW** (this story)
- **FR-44**: L'admin peut override la région principale d'un joueur — **NEW** (this story, covered by newRegion field)

### What Is ALREADY THERE

#### Backend
- `PlayerIdentityEntry` domain model — `final class`, has `restore()` static factory (12 params, `@SuppressWarnings("java:S107")`), `resolve()`, `reject()` domain methods. Fields: `playerUsername` (final), `playerRegion` (final), `epicId`, `status`, `confidenceScore`, `resolvedBy`, `resolvedAt`, `rejectedAt`, `rejectionReason`, `createdAt`.
- `PlayerIdentityEntity` (@Entity, Lombok @Builder @Getter @Setter) — mirrors domain + JPA columns. Path: `adapter/out/persistence/player/identity/PlayerIdentityEntity.java`
- `PlayerIdentityEntityMapper` — `toDomain(entity)` calls `PlayerIdentityEntry.restore(12 params)`, `toEntity(domain)` uses builder.
- `PlayerIdentityPipelineService` (4 deps: identityRepository, epicIdValidator, confidenceScoreService, messagingTemplate) — `getUnresolved()`, `getResolved()`, `getCount()`, `resolve()`, `reject()`, `getRegionalStats()`. The `toResponse()` private method maps domain → `PlayerIdentityEntryResponse`.
- `AdminPlayerPipelineController` at `/api/admin/players` — 6 endpoints (GET unresolved/resolved/count/regional, POST resolve/reject).
- `PlayerIdentityEntryResponse` record — 12 fields: `id, playerId, playerUsername, playerRegion, epicId, status, confidenceScore, resolvedBy, resolvedAt, rejectedAt, rejectionReason, createdAt`.
- `PlayerIdentityRepositoryPort` — `findByPlayerId(UUID)` returns `Optional<PlayerIdentityEntry>`, `save(entry)` returns `PlayerIdentityEntry`.
- Latest DB migration: **V40**. Next must be **V41**.
- `PlayerRegion` enum in `domain/game/model/`: `UNKNOWN, EU, NAW, BR, ASIA, OCE, NAC, ME, NA`.

#### Frontend
- `AdminPipelineTableComponent` — pure emitter: `@Output() resolved`, `@Output() rejected`. Columns: unresolved=[username, region, epicId, actions], resolved=[username, region, epicId, confidenceScore, resolvedBy, status]. Injects NO services (pure presentational).
- `AdminPipelinePageComponent` — injects PipelineService, MatSnackBar, CDR, MatDialog NOT yet injected.
- `PipelineService` — 6 HTTP methods (getUnresolved, getResolved, getCount, resolvePlayer, rejectPlayer, getRegionalStatus).
- `PlayerIdentityEntry` interface: `id, playerId, playerUsername, playerRegion, epicId, status, confidenceScore, resolvedBy, resolvedAt, rejectedAt, rejectionReason, createdAt`.

### Architecture Guardrails

- **CouplingTest** (backend): `PlayerIdentityPipelineService` stays at 4 deps — no new injected dep needed for `correctMetadata()` (uses same `identityRepository`).
- **NamingConventionTest**: `@Service` classes must end with `Service` suffix. The dialog is a `@Component` — no issue.
- **DomainIsolationTest**: `PlayerIdentityEntry` is in `domain/player/identity/model/` — no JPA/Spring deps allowed.
- **DependencyInversionTest**: Controller must NOT inject repositories directly.
- **Spotless**: Run `mvn spotless:apply` before `mvn test`.
- **SOLID/SRP**: `correctMetadata()` domain method is a single responsibility. Keep `PlayerIdentityEntry` ≤ 500 lines, methods ≤ 50 lines.
- **Angular pure component pattern**: `AdminPipelineTableComponent` stays service-free — add `@Output() correctRequested = new EventEmitter<PlayerIdentityEntry>()` and let the PAGE open the dialog.
- **MAT_DIALOG_DATA pattern**: Dialog receives `{ entry: PlayerIdentityEntry }` via injection token, returns `{ newUsername: string | null, newRegion: string | null } | undefined` (undefined = cancelled).
- **Test baseline**: Backend 2206 run (19 failures + 1 error pre-existing). Frontend 2086/2086.

### Gap Analysis

| Component | Gap |
|---|---|
| `PlayerIdentityEntry` | No correctedUsername/Region/By/At fields |
| `PlayerIdentityEntity` | No corrected_* columns |
| `PlayerIdentityEntityMapper` | Missing new fields in toDomain/toEntity |
| `PlayerIdentityEntryResponse` | Missing 4 correction fields |
| `PlayerIdentityPipelineService` | No correctMetadata() method |
| `AdminPlayerPipelineController` | No PATCH /{playerId}/metadata endpoint |
| DB migration | No V41 migration |
| `CorrectMetadataRequest` | Does not exist |
| `PlayerIdentityEntry` (frontend) | Missing 4 correction fields |
| `PipelineService` (frontend) | No correctMetadata() method |
| `AdminPipelineTableComponent` | No "Corriger" button / no correctRequested output |
| `AdminPipelinePageComponent` | No dialog open + no onCorrected handler |
| `CorrectMetadataDialogComponent` | Does not exist |

## Tasks / Subtasks

- [x] Task 1: Domain model extension (AC: #1, #3)
  - [x] Add 4 nullable fields to `PlayerIdentityEntry`: `correctedUsername`, `correctedRegion`, `correctedBy`, `correctedAt`
  - [x] Add domain method `correctMetadata(String newUsername, String newRegion, String correctedBy)`
  - [x] Update `restore()` static factory to accept and set the 4 new fields (16 params total — keep `@SuppressWarnings("java:S107")`)
  - [x] Add getters for the 4 new fields

- [x] Task 2: JPA layer (AC: #1)
  - [x] Add 4 nullable columns to `PlayerIdentityEntity`: `corrected_username`, `corrected_region`, `corrected_by`, `corrected_at`
  - [x] Update `PlayerIdentityEntityMapper.toDomain()` to pass 4 new fields to `restore()`
  - [x] Update `PlayerIdentityEntityMapper.toEntity()` to map 4 new fields in builder
  - [x] Create `V41__add_player_identity_metadata_corrections.sql` (ALTER TABLE player_identity_pipeline ADD COLUMN × 4)

- [x] Task 3: DTO + Service + Controller (AC: #1, #2, #3)
  - [x] Add 4 fields to `PlayerIdentityEntryResponse` record: `correctedUsername`, `correctedRegion`, `correctedBy`, `correctedAt`
  - [x] Update `toResponse()` private method in `PlayerIdentityPipelineService` to include new fields
  - [x] Create `CorrectMetadataRequest` record: `@NotNull UUID playerId`, `@Size(max=50) String newUsername` (nullable), `String newRegion` (nullable)
  - [x] Add `correctMetadata(UUID playerId, String newUsername, String newRegion, String correctedBy)` to `PlayerIdentityPipelineService`
  - [x] Add `PATCH /{playerId}/metadata` endpoint to `AdminPlayerPipelineController`
  - [x] Add 3 service tests + 2 controller tests

- [x] Task 4: Frontend model + service (AC: #4, #5)
  - [x] Add 4 fields to `PlayerIdentityEntry` interface in `admin.models.ts`
  - [x] Add `CorrectMetadataRequest` interface to `admin.models.ts`
  - [x] Add `correctMetadata(playerId: string, body: CorrectMetadataRequest)` to `PipelineService`
  - [x] Create `pipeline.service.spec.ts` with 2 tests for `correctMetadata`

- [x] Task 5: CorrectMetadataDialogComponent (AC: #4)
  - [x] Create `admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.ts` (standalone, injects MAT_DIALOG_DATA)
  - [x] Dialog receives `CorrectMetadataDialogData { entry: PlayerIdentityEntry }`, returns `CorrectMetadataRequest | undefined`
  - [x] FormGroup with `newUsername` (pre-filled with correctedUsername, maxLength 50) and `newRegion`
  - [x] Cancel/Submit buttons with form validation
  - [x] Spec file with 9 tests (init, submit, cancel, validation)

- [x] Task 6: Table + Page component wiring (AC: #4, #5)
  - [x] Add `@Output() correctRequested = new EventEmitter<PlayerIdentityEntry>()` to `AdminPipelineTableComponent`
  - [x] Add "Corriger" button (mat-icon-button, `edit` icon, class `btn-correct`) to both modes
  - [x] `onCorrect(entry)` emits `correctRequested`
  - [x] Add `'actions'` column to `resolvedColumns`
  - [x] In `AdminPipelinePageComponent`: inject `MatDialog`, add `onCorrectRequested(entry)` handler
  - [x] Opens `CorrectMetadataDialogComponent`, after close calls `pipelineService.correctMetadata()`
  - [x] On success: snackbar "✏ Fiche joueur corrigée", `loadData()`
  - [x] Added `MatDialogModule` to component imports
  - [x] 4 tests in `admin-pipeline-page.component.spec.ts` for correct metadata flow

## Dev Notes

### Backend Implementation Details

#### `PlayerIdentityEntry` domain method
```java
public void correctMetadata(String newUsername, String newRegion, String correctedBy) {
  if (newUsername != null) this.correctedUsername = newUsername;
  if (newRegion != null) this.correctedRegion = newRegion;
  this.correctedBy = correctedBy;
  this.correctedAt = LocalDateTime.now();
}
```

#### `restore()` updated signature (16 params)
```java
@SuppressWarnings("java:S107")
public static PlayerIdentityEntry restore(
    UUID id, UUID playerId, String playerUsername, String playerRegion,
    String epicId, IdentityStatus status, int confidenceScore,
    String resolvedBy, LocalDateTime resolvedAt,
    LocalDateTime rejectedAt, String rejectionReason, LocalDateTime createdAt,
    String correctedUsername, String correctedRegion,
    String correctedBy, LocalDateTime correctedAt) { ... }
```

#### `CorrectMetadataRequest` DTO
```java
// src/main/java/com/fortnite/pronos/dto/admin/CorrectMetadataRequest.java
public record CorrectMetadataRequest(
    @NotNull UUID playerId,
    @Size(max = 50) String newUsername,    // nullable = no change
    String newRegion                        // nullable = no change
) {}
```

#### `PlayerIdentityEntryResponse` updated (add 4 fields)
```java
public record PlayerIdentityEntryResponse(
    UUID id, UUID playerId,
    String playerUsername, String playerRegion,
    String epicId, String status, int confidenceScore,
    String resolvedBy, LocalDateTime resolvedAt,
    LocalDateTime rejectedAt, String rejectionReason, LocalDateTime createdAt,
    String correctedUsername, String correctedRegion,
    String correctedBy, LocalDateTime correctedAt) {}
```
**CRITICAL**: Any test creating `PlayerIdentityEntryResponse` directly must be updated to pass 16 args (add 4 nulls at end). Check all test files for `new PlayerIdentityEntryResponse(`.

#### `correctMetadata()` in service
```java
public PlayerIdentityEntryResponse correctMetadata(
    UUID playerId, String newUsername, String newRegion, String correctedBy) {
  PlayerIdentityEntry entry = identityRepository.findByPlayerId(playerId)
      .orElseThrow(() -> new PlayerIdentityNotFoundException(playerId));
  entry.correctMetadata(newUsername, newRegion, correctedBy);
  PlayerIdentityEntry saved = identityRepository.save(entry);
  return toResponse(saved);
}
```

#### Controller endpoint
```java
@PatchMapping("/{playerId}/metadata")
public ResponseEntity<PlayerIdentityEntryResponse> correctMetadata(
    @PathVariable UUID playerId,
    @Valid @RequestBody CorrectMetadataRequest request,
    Principal principal) {
  String correctedBy = principal != null ? principal.getName() : "admin";
  return ResponseEntity.ok(
      pipelineService.correctMetadata(playerId, request.newUsername(), request.newRegion(), correctedBy));
}
```

#### DB Migration V41
```sql
-- V41__add_player_identity_metadata_corrections.sql
ALTER TABLE player_identity_pipeline
  ADD COLUMN corrected_username VARCHAR(255),
  ADD COLUMN corrected_region VARCHAR(20),
  ADD COLUMN corrected_by VARCHAR(255),
  ADD COLUMN corrected_at TIMESTAMP;
```

#### `toResponse()` update in service (add 4 fields)
```java
private PlayerIdentityEntryResponse toResponse(PlayerIdentityEntry e) {
  return new PlayerIdentityEntryResponse(
      e.getId(), e.getPlayerId(),
      e.getPlayerUsername(), e.getPlayerRegion(),
      e.getEpicId(), e.getStatus().name(), e.getConfidenceScore(),
      e.getResolvedBy(), e.getResolvedAt(),
      e.getRejectedAt(), e.getRejectionReason(), e.getCreatedAt(),
      e.getCorrectedUsername(), e.getCorrectedRegion(),
      e.getCorrectedBy(), e.getCorrectedAt());
}
```

### Frontend Implementation Details

#### `PlayerIdentityEntry` interface update
```typescript
export interface PlayerIdentityEntry {
  // existing fields...
  correctedUsername?: string | null;
  correctedRegion?: string | null;
  correctedBy?: string | null;
  correctedAt?: string | null;
}
```

#### `CorrectMetadataRequest` interface
```typescript
export interface CorrectMetadataRequest {
  playerId: string;
  newUsername?: string | null;
  newRegion?: string | null;
}
```

#### `CorrectionDialogData` and `CorrectionDialogResult` — define in dialog component file
```typescript
export interface CorrectionDialogData {
  entry: PlayerIdentityEntry;
}
export interface CorrectionDialogResult {
  newUsername: string | null;
  newRegion: string | null;
}
```

#### Dialog component pattern
```typescript
// MAT_DIALOG_DATA injection token
readonly data = inject<CorrectionDialogData>(MAT_DIALOG_DATA);
readonly dialogRef = inject(MatDialogRef<CorrectMetadataDialogComponent>);
readonly form = new FormGroup({
  username: new FormControl<string>(
    this.data.entry.correctedUsername ?? this.data.entry.playerUsername),
  region: new FormControl<string>(
    this.data.entry.correctedRegion ?? this.data.entry.playerRegion)
});
readonly REGIONS = ['EU', 'NAW', 'BR', 'ASIA', 'OCE', 'NAC', 'ME', 'NA', 'UNKNOWN'];
confirm(): void {
  const result: CorrectionDialogResult = {
    newUsername: this.form.value.username ?? null,
    newRegion: this.form.value.region ?? null
  };
  this.dialogRef.close(result);
}
cancel(): void { this.dialogRef.close(); }
```

#### Page component — onCorrectRequested
```typescript
// Inject MatDialog in AdminPipelinePageComponent
private readonly dialog = inject(MatDialog);

onCorrectRequested(entry: PlayerIdentityEntry): void {
  const ref = this.dialog.open(CorrectMetadataDialogComponent, {
    data: { entry } satisfies CorrectionDialogData,
    width: '400px'
  });
  ref.afterClosed()
    .pipe(takeUntil(this.destroy$))
    .subscribe((result: CorrectionDialogResult | undefined) => {
      if (!result) return;
      this.pipelineService
        .correctMetadata({ playerId: entry.playerId, newUsername: result.newUsername, newRegion: result.newRegion })
        .pipe(takeUntil(this.destroy$))
        .subscribe(updated => {
          if (updated) {
            this.snackBar.open('✓ Métadonnées corrigées', 'Fermer', { duration: 3000 });
            this.loadData();
          } else {
            this.snackBar.open('Erreur lors de la correction', 'Fermer', { duration: 4000 });
          }
        });
    });
}
```

#### PipelineService — correctMetadata()
```typescript
correctMetadata(body: CorrectMetadataRequest): Observable<PlayerIdentityEntry | null> {
  return this.http
    .patch<PlayerIdentityEntry>(`${this.baseUrl}/${body.playerId}/metadata`, body)
    .pipe(catchError(() => of(null)));
}
```

### Testing Notes

#### Backend: watch out for `PlayerIdentityEntryResponse` constructor calls in tests
- Search: `new PlayerIdentityEntryResponse(` — must update to pass 4 null extra args.
- Pattern to grep: `grep -rn "PlayerIdentityEntryResponse(" src/test/`

#### Backend: `PlayerIdentityEntry.restore()` calls in tests
- 12-param `restore()` becomes 16-param. All test usage must be updated.
- Pattern: `PlayerIdentityEntry.restore(` → add 4 null args at end.

#### Frontend: Dialog testing pattern
```typescript
// Use MatDialogRef spy for unit testing dialog component
const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
// Provide MAT_DIALOG_DATA with test entry
providers: [
  { provide: MAT_DIALOG_DATA, useValue: { entry: TEST_ENTRY } },
  { provide: MatDialogRef, useValue: dialogRefSpy }
]
```

#### Frontend: Page test with dialog
```typescript
// Mock MatDialog to return controlled afterClosed observable
const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
const dialogRefSpy = { afterClosed: () => of({ newUsername: 'new_name', newRegion: 'EU' }) };
dialogSpy.open.and.returnValue(dialogRefSpy);
```

### Project Structure Notes

**Backend files to modify/create:**
- `domain/player/identity/model/PlayerIdentityEntry.java` (modify)
- `adapter/out/persistence/player/identity/PlayerIdentityEntity.java` (modify)
- `adapter/out/persistence/player/identity/PlayerIdentityEntityMapper.java` (modify)
- `src/main/resources/db/migration/V41__add_player_identity_metadata_corrections.sql` (create)
- `dto/admin/PlayerIdentityEntryResponse.java` (modify — add 4 fields)
- `dto/admin/CorrectMetadataRequest.java` (create)
- `service/admin/PlayerIdentityPipelineService.java` (modify — add correctMetadata, update toResponse)
- `controller/AdminPlayerPipelineController.java` (modify — add PATCH endpoint)

**Backend test files to modify:**
- `src/test/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineServiceTest.java` (add 3 tests + fix restore() calls)
- `src/test/java/com/fortnite/pronos/controller/AdminPlayerPipelineControllerTest.java` (add 2 tests + fix PlayerIdentityEntryResponse calls)
- Any other test using `PlayerIdentityEntry.restore()` or `new PlayerIdentityEntryResponse(` — grep first

**Frontend files to modify/create:**
- `features/admin/models/admin.models.ts` (modify — add 4 optional fields + CorrectMetadataRequest)
- `features/admin/services/pipeline.service.ts` (modify — add correctMetadata)
- `features/admin/services/pipeline.service.spec.ts` (modify — add 1 test)
- `features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.ts` (modify — add correctRequested output)
- `features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.html` (modify — add Corriger button)
- `features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.spec.ts` (modify — add test for correctRequested)
- `features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.ts` (create)
- `features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.html` (create)
- `features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.scss` (create)
- `features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.spec.ts` (create)
- `features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` (modify — inject MatDialog, add onCorrectRequested)
- `features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.html` (modify — bind correctRequested)
- `features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts` (modify — add 3 tests)

### References

- `PlayerIdentityEntry` domain: `domain/player/identity/model/PlayerIdentityEntry.java`
- `PlayerIdentityEntity`: `adapter/out/persistence/player/identity/PlayerIdentityEntity.java`
- `PlayerIdentityEntityMapper`: `adapter/out/persistence/player/identity/PlayerIdentityEntityMapper.java`
- `PlayerIdentityPipelineService`: `service/admin/PlayerIdentityPipelineService.java`
- `AdminPlayerPipelineController`: `controller/AdminPlayerPipelineController.java`
- `PlayerIdentityEntryResponse`: `dto/admin/PlayerIdentityEntryResponse.java`
- `PlayerRegion` enum: `domain/game/model/PlayerRegion.java` (values: UNKNOWN, EU, NAW, BR, ASIA, OCE, NAC, ME, NA)
- Latest migration: `V40__add_team_score_deltas.sql` → next is V41
- `AdminPipelineTableComponent`: `features/admin/pipeline/admin-pipeline-table/`
- `AdminPipelinePageComponent`: `features/admin/pipeline/admin-pipeline-page/`
- `PipelineService`: `features/admin/services/pipeline.service.ts`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None — clean run.

### Completion Notes List

- Backend: +5 tests (3 service + 2 controller). 2211 total, 19 failures + 1 error all pre-existing.
- Frontend: +18 tests (2 service + 9 dialog + 3 table + 4 page). 2104/2104 SUCCESS.
- V41 migration adds 4 nullable columns to `player_identity_pipeline`.
- `CorrectMetadataRequest` frontend interface sends `newUsername`/`newRegion` only (playerId is in URL path).
- Dialog pre-fills form from `entry.correctedUsername`/`correctedRegion` (null when no prior correction).
- Runtime follow-up (2026-03-07): revalidation locale sur vraies entrees `UNRESOLVED` issues de l'ingestion PR pour `PATCH /api/admin/players/{playerId}/metadata` et pour le flow admin FR-42 `POST /api/admin/players/resolve` / `POST /api/admin/players/reject` ; les reponses et traces admin sont conformes. Voir `7-2-resolution-manuelle-et-correction-fiche-joueur-runtime-validation-2026-03-07.md`.

### File List

**Backend:**
- `src/main/java/com/fortnite/pronos/domain/player/identity/model/PlayerIdentityEntry.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityEntity.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityEntityMapper.java` (modified)
- `src/main/resources/db/migration/V41__add_player_identity_metadata_corrections.sql` (created)
- `src/main/java/com/fortnite/pronos/dto/admin/PlayerIdentityEntryResponse.java` (modified — 16 fields)
- `src/main/java/com/fortnite/pronos/dto/admin/CorrectMetadataRequest.java` (created)
- `src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java` (modified)
- `src/main/java/com/fortnite/pronos/controller/AdminPlayerPipelineController.java` (modified)
- `src/test/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineServiceTest.java` (modified)
- `src/test/java/com/fortnite/pronos/controller/AdminPlayerPipelineControllerTest.java` (modified)

**Frontend:**
- `frontend/src/app/features/admin/models/admin.models.ts` (modified)
- `frontend/src/app/features/admin/services/pipeline.service.ts` (modified)
- `frontend/src/app/features/admin/services/pipeline.service.spec.ts` (created)
- `frontend/src/app/features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.ts` (created)
- `frontend/src/app/features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.html` (created)
- `frontend/src/app/features/admin/pipeline/correct-metadata-dialog/correct-metadata-dialog.component.spec.ts` (created)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.html` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-table/admin-pipeline-table.component.spec.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.html` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts` (modified)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**M1 — FIXED**: Dialog form initialized with `null` instead of `playerUsername`/`playerRegion` when no prior correction exists. AC #4 requires "a dialog opens with the current username and region pre-filled". Fixed by changing default form values from `correctedUsername ?? null` to `correctedUsername ?? playerUsername ?? null` (and same for region). Updated the spec test from "initializes form with null values when no existing correction" to "initializes form with player username and region when no existing correction". All 2133 frontend tests passing.

### Action items

- [ ] **[AI-Review][Low][L1]**: `CorrectMetadataDialogComponent.submit()` returns empty object `{}` when both fields are cleared (correct for AC #3), but when pre-filled values are unchanged and submitted, it will trigger an API PATCH that sets correctedUsername/correctedRegion to the same values (unnecessary DB write + audit fields updated). Consider comparing form values to current entry values before including in result. Low priority since the backend handles it idempotently.
