# Story {{epic_num}}.{{story_num}}: {{story_title}}

Status: ready-for-dev

<!-- METADATA
  story_key: {{epic_num}}-{{story_num}}-{{story_slug}}
  branch: story/{{epic_num}}-{{story_num}}-{{story_slug}}
  sprint: Sprint {{sprint_num}}
  Note: Validation is optional. Run validate-create-story for quality check before dev-story.
-->

## Story

As a {{role}},
I want {{action}},
so that {{benefit}}.

## Acceptance Criteria

1. [Add acceptance criteria from epics/PRD]

## Tasks / Subtasks

- [ ] Task 1 (AC: #)
  - [ ] Subtask 1.1
- [ ] Task 2 (AC: #)
  - [ ] Subtask 2.1

<!-- SECURITY TASK — MANDATORY if this story creates one or more @RestController classes.
     Remove this block only if the story creates ZERO new controllers.
     Pattern: SecurityConfig<ControllerName>AuthorizationTest using @WebMvcTest + @WithMockUser.
     Must cover: anonymous → 401/403, non-admin authenticated → 403, admin → 200. -->
- [ ] Task N: Security — Authorization tests for new controller(s) (AC: #3 or equivalent)
  - [ ] N.1: Create `SecurityConfig<ControllerName>AuthorizationTest` in `config/` — `@WebMvcTest(controllers = <Controller>.class)`, `@Import({SecurityConfig.class, SecurityTestBeans.class})`
  - [ ] N.2: Test anonymous user → 401/403 on each endpoint
  - [ ] N.3: Test non-admin authenticated user → 403 on each endpoint (`@WithMockUser(roles = {"USER"})`)
  - [ ] N.4: Test admin user → 200 on each endpoint (`@WithMockUser(roles = {"ADMIN"})`)

## Dev Notes

- Relevant architecture patterns and constraints
- Source tree components to touch
- Testing standards summary

### Pre-existing Gaps / Known Issues

<!-- MANDATORY: List any known pre-existing issues, test failures, or tech-debt that
     existed BEFORE this story starts, so the dev agent doesn't get confused or
     accidentally "fix" things outside the scope of this story.
     Format: - [KNOWN] Description (source: file:line or ticket ref)

     ALWAYS check project-context.md §Pre-existing failures for the current backend/frontend baselines.
     Frontend Vitest: use `npm run test:vitest` — do NOT use fakeAsync()+tick() with Vitest.
       Pattern A (sync Observable): replace fakeAsync(()=>{tick(N)}) with async ()=>{}
       Pattern B (real debounceTime): use vi.useFakeTimers() + vi.advanceTimersByTime(N) + vi.useRealTimers()
-->
- [NONE] No pre-existing gaps identified for this story

### Project Structure Notes

- Alignment with unified project structure (paths, modules, naming)
- Detected conflicts or variances (with rationale)

### References

- Cite all technical details with source paths and sections, e.g. [Source: docs/<file>.md#Section]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
