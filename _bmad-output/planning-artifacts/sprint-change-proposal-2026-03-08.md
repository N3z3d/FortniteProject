# Sprint Change Proposal - 2026-03-08

## 1. Issue Summary

The sprint priority changed during implementation. Hosting/staging work remains intentionally deferred by product decision, while the immediate delivery risk moved to local prod-like validation and regression confidence.

Evidence collected on 2026-03-08:

- User explicitly deprioritized `Railway` / public hosting in favor of local validation of real app behavior.
- A full Playwright audit previously exposed a weak baseline (`24 passed`, `8 failed`, `5 skipped`).
- After E2E stabilization work completed today, the current local baseline is `35 passed`, `4 skipped`, `0 failed` on `npx playwright test`.
- The remaining skips are concentrated on comparison-panel determinism and draft-depth coverage, not on the current core create/join/admin/auth paths.

This means the sprint is no longer blocked by implementation defects on the critical local flows, but it still lacks a formal BMAD story dedicated to finishing E2E stabilization and converting the remaining skipped flows into an explicit, maintainable contract.

## 2. Impact Analysis

### Epic Impact

- Existing Sprint 4 infra items stay unchanged and deferred/backlog:
  - `sprint4-decision-hebergement`
  - `sprint4-config-secrets-prod`
  - `sprint4-db-prod-provisioning`
  - `sprint4-a10-staging-deployment`
  - `sprint4-a3b-cicd-pipeline-complet`
  - `sprint4-sec-r2-websocket-auth`
- A new local hardening epic is required to reflect the real sprint objective:
  - `Epic 8: Stabilisation locale et regression E2E critique`

### Story Impact

- Completed local stories remain completed:
  - `sprint4-api-fortnite-wiring-check`
  - `sprint4-e2e-seed-data`
  - `sprint4-a6-jpa-legacy-migration-5-services`
- A new story is introduced:
  - `8.1 - Stabilisation locale E2E critique`

### Artifact Conflicts

- `epics.md` must be updated to add the new epic/story.
- `sprint-status.yaml` must be updated to track the new work item.
- A new implementation story file must be created in `_bmad-output/implementation-artifacts/`.
- No PRD requirement change is required at this stage.
- No architecture document change is required at this stage.
- No UX specification change is required at this stage.

### Technical Impact

- Primary impact area is the Playwright regression layer:
  - helper determinism
  - suite isolation
  - invitation-code persistence checks
  - explicit split between core create/join coverage and draft-depth coverage
- Secondary impact area is test documentation and local execution policy.
- No production infra or deployment artifact needs to move right now.

## 3. Recommended Approach

Recommended path: Moderate change via backlog reorganization.

Reasoning:

- The user has explicitly rejected infra/staging as current priority.
- The measured local test state justifies a dedicated stabilization story rather than ad hoc bug fixing.
- The change is larger than a direct edit to one existing story because it introduces new scope and a new sprint objective, but it does not require a fundamental replan of the product.

Recommended execution:

1. Add a new hardening epic and a first E2E stabilization story.
2. Mark the story `ready-for-dev`.
3. Keep infra items in backlog/deferred without reopening the hosting decision.
4. Execute the stabilization story before creating any new infra story.

Risk level: Medium.

Timeline impact:

- Positive for local quality and bug discovery.
- Neutral-to-delayed for staging/public deployment, by explicit product choice.

## 4. Detailed Change Proposals

### Stories / Sprint Tracking

Artifact: `_bmad-output/planning-artifacts/epics.md`

OLD:

- No epic explicitly covered local E2E hardening after the hosting deferral.

NEW:

- Add `Epic 8: Stabilisation locale et regression E2E critique`
- Add `Story 8.1: Stabilisation locale E2E critique`

Rationale:

- The sprint needs a formal BMAD target aligned with the user's current priority.

Artifact: `_bmad-output/implementation-artifacts/sprint-status.yaml`

OLD:

- No dedicated backlog item existed for local E2E stabilization after infra was deferred.

NEW:

- Add:
  - `epic-8: in-progress`
  - `8-1-stabilisation-locale-e2e-critique: ready-for-dev`
  - `epic-8-retrospective: optional`

Rationale:

- The board must reflect the new immediate executable work.

Artifact: `_bmad-output/implementation-artifacts/8-1-stabilisation-locale-e2e-critique.md`

NEW:

- Create a ready-for-dev implementation story with:
  - current baseline
  - concrete acceptance criteria
  - task breakdown
  - developer guardrails
  - references to current E2E files and local seed setup

Rationale:

- The next BMAD move after Correct Course is an executable story, not a vague recommendation.

### PRD / Architecture / UX

No direct document mutation recommended in this change set.

Rationale:

- The change affects sprint execution priority and test hardening, not core product requirements or architecture direction.

## 5. Implementation Handoff

Scope classification: Moderate.

Handoff recipients:

- Scrum / BMAD tracking:
  - validate the sprint redirect formally through the board and epic/story artifacts
- Development:
  - execute story `8.1`
- QA / automation:
  - use story `8.1` as the basis for closing the remaining skipped E2E gaps

Success criteria:

- `8.1` is present in BMAD as `ready-for-dev`
- Sprint board reflects the local stabilization priority
- Full Playwright suite baseline is known and documented
- Remaining skipped tests are explicitly owned by the new story rather than left implicit
