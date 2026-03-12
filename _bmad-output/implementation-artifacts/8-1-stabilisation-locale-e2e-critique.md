# Story 8.1: Stabilisation locale E2E critique

Status: done

<!-- METADATA
  story_key: 8-1-stabilisation-locale-e2e-critique
  branch: story/8-1-stabilisation-locale-e2e-critique
  sprint: Sprint 4
  Note: Story de hardening local. Objectif: transformer la stabilisation Playwright en backlog BMAD executable et fermer les skips critiques restants.
-->

## Story

As a developer validating the app locally,
I want a deterministic Playwright regression pack for the critical user flows,
so that I can trust local prod-like behavior before any public staging effort.

## Acceptance Criteria

1. `npx playwright test` passe localement sans echec, y compris sur un rerun consecutif, sans reset manuel de la base.
2. Les helpers E2E utilisent une isolation par prefixe de suite et des garde-fous suffisants pour eviter les collisions de donnees historiques entre `smoke`, `game-lifecycle` et `full-flow`.
3. `CAT-04` n est plus skippe et valide de facon deterministe le contrat reel du panneau de comparaison catalogue.
4. Le flux create/join par code reste dans le pack critique et attend explicitement la persistance backend du code d invitation avant l etape de join.
5. Les etapes draft profondes (`FULL-FLOW-06/07/08`) sont soit stabilisees dans une suite dediee, soit officiellement sorties du pack critique avec runbook et preconditions documentes.
6. Les zones encore non couvertes par le pack critique apres la story (notamment trade/swap si toujours hors scope) sont listees explicitement dans les notes de completion et la prochaine story recommandee.

## Tasks / Subtasks

- [x] Task 1 - Finaliser la fiabilite du helper Playwright partage (AC: #1, #2, #4)
  - [x] 1.1 Verifier que `frontend/e2e/helpers/app-helpers.ts` couvre proprement cleanup, login profil, create quick game, invitation code et join code
  - [x] 1.2 Conserver une isolation par prefixe de suite (`E2E-GL-*`, `E2E-FF-*`, etc.) et eviter toute dependance a un utilisateur deja pollue
  - [x] 1.3 Verifier la rerunabilite par deux executions consecutives de `npx playwright test`

- [x] Task 2 - Fermer `CAT-04` avec un contrat deterministic (AC: #3)
  - [x] 2.1 Auditer `frontend/e2e/catalogue.spec.ts` et le rendu reel du composant catalogue/comparison panel
  - [x] 2.2 Corriger la spec ou le contrat UI pour que la comparaison de deux joueurs soit testable sans skip
  - [x] 2.3 Relancer la spec catalogue complete et verifier `CAT-01` a `CAT-04`

- [x] Task 3 - Sortir clairement le draft approfondi du pack critique ou le stabiliser (AC: #5)
  - [x] 3.1 Decider si `FULL-FLOW-06/07/08` restent dans `full-game-flow.spec.ts` ou migrent dans une suite dediee `draft-flow.spec.ts`
  - [x] 3.2 Si une suite dediee est creee, documenter les preconditions seed/users/commande de lancement
  - [x] 3.3 S assurer qu aucun test du pack critique ne laisse un jeu indeletable qui casse les reruns

- [x] Task 4 - Documenter la matrice de regression locale (AC: #1, #5, #6)
  - [x] 4.1 Ajouter ou mettre a jour un document de runbook E2E local avec commandes, suites critiques, suites dediees et politique de skip
  - [x] 4.2 Enregistrer le baseline final (nombre de tests pass/skipped) et les zones encore hors couverture

- [x] Task 5 - Verification finale (AC: #1, #2, #3, #4, #5, #6)
  - [x] 5.1 Lancer `npx playwright test`
  - [x] 5.2 Lancer un deuxieme rerun sans reset manuel
  - [x] 5.3 Mettre a jour la section `Completion Notes List` avec le resultat exact et les reliquats eventuels

## Dev Notes

### Current Measured Baseline

- Baseline mesuree le 2026-03-08 apres stabilisation initiale:
  - `npx playwright test` -> `35 passed`, `4 skipped`, `0 failed`
- Skips restants au moment de creer cette story:
  - `CAT-04`
  - `FULL-FLOW-06`
  - `FULL-FLOW-07`
  - `FULL-FLOW-08`

### Scope Guardrails

- Cette story traite la stabilite et la clarte du pack E2E, pas l hebergement/staging.
- Ne pas rouvrir `sprint4-decision-hebergement` ni les stories infra dans ce lot.
- Ne pas elargir implicitement le scope a toute la roadmap QA. Les flows non critiques encore absents (ex: trade/swap) doivent etre documentes, pas absorbes sans arbitrage.

### Technical Requirements

- Le helper partage doit rester dans `frontend/e2e/helpers/app-helpers.ts`.
- Les suites critiques actuelles sont:
  - `frontend/e2e/smoke.spec.ts`
  - `frontend/e2e/auth.spec.ts`
  - `frontend/e2e/catalogue.spec.ts`
  - `frontend/e2e/game-lifecycle.spec.ts`
  - `frontend/e2e/full-game-flow.spec.ts`
- Les utilisateurs seedes disponibles sont `admin`, `thibaut`, `marcel`, `teddy`.
- Le seed local est deja fourni par `src/main/resources/db/seed/V1001__seed_e2e_users_and_players.sql`.
- Le join par code navigue cote UI via `frontend/src/app/features/game/join-game/join-game.component.ts`.
- Le quick-create passe par `frontend/src/app/features/game/create-game/create-game.component.html`.
- Le detail game expose les actions `generate code`, `delete`, `leave`, `start draft` via `frontend/src/app/features/game/game-detail/game-detail.component.html`.

### Architecture Compliance

- Pas de nouvelle dependance externe sans justification.
- Preferer des helpers Playwright simples et reutilisables plutot que dupliquer la logique de login/create/join dans chaque spec.
- Garder les specs focalisees sur le contrat utilisateur reel; si le contrat a change, mettre a jour le test plutot que conserver une hypothese obsolete.

### Testing Requirements

- Toute modification de spec doit etre revalidee au minimum sur:
  - la spec modifiee
  - le pack critique cible
  - la suite complete `npx playwright test`
- La story n est done que si le rerun sans reset manuel ne casse pas.

### Pre-existing Gaps / Known Issues

- [KNOWN] Au demarrage de la story, `CAT-04`, `FULL-FLOW-06`, `FULL-FLOW-07` et `FULL-FLOW-08` sont skippes dans la baseline Playwright du 2026-03-08.
- [KNOWN] La couverture E2E de trade/swap n est pas encore presente dans Playwright; elle devra faire l objet d une story suivante si elle reste hors scope ici.
- [KNOWN] Des jeux E2E historiques peuvent encore exister en base locale; les helpers actuels tolerent ces reliquats si leur suppression n est plus legalement possible via l API.

### Project Structure Notes

- Dossier principal E2E: `frontend/e2e/`
- Config Playwright: `frontend/playwright.config.ts`
- Les changements attendus sont majoritairement frontend/tests; une evolution backend n est acceptable que si elle est strictement necessaire a la determinisme des tests locaux.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/e2e/catalogue.spec.ts`
- `frontend/e2e/game-lifecycle.spec.ts`
- `frontend/e2e/full-game-flow.spec.ts`
- `frontend/e2e/smoke.spec.ts`
- `frontend/e2e/auth.spec.ts`
- `frontend/playwright.config.ts`
- `frontend/src/app/features/game/join-game/join-game.component.ts`
- `frontend/src/app/features/game/create-game/create-game.component.html`
- `frontend/src/app/features/game/game-detail/game-detail.component.html`
- `src/main/resources/db/seed/V1001__seed_e2e_users_and_players.sql`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Playwright full suite baseline captured on 2026-03-08

### Implementation Plan

- Realigner le service catalogue sur les endpoints backend reels pour sortir le catalogue de l etat vide silencieux.
- Rendre `CAT-04` deterministe sur le DOM reel du panneau de comparaison.
- Sortir le draft profond du pack critique dans une suite dediee documentee tant que le contrat draft front/back reste casse.
- Prouver la rerunabilite avec deux executions completes consecutives de Playwright sans reset manuel.

### Completion Notes List

- Le helper partage conserve l isolation par prefixe de suite (`E2E-GL-*`, `E2E-FF-*`) et la persistance explicite du code d invitation avant le join.
- `frontend/src/app/features/catalogue/services/player-catalogue.service.ts` utilise maintenant les endpoints reels `/players/catalogue` et `/players/catalogue/search`, avec mapping local vers `AvailablePlayer` et filtrage local des flags `available` / `tranche`.
- `CAT-03` valide maintenant un catalogue peuple en local au lieu d accepter silencieusement un etat vide, ce qui verrouille le contrat reel des cartes joueur seedees.
- `CAT-04` est desormais actif et deterministe sur le DOM reel du catalogue; `npx playwright test e2e/catalogue.spec.ts` est passe a `4 passed`.
- `frontend/e2e/admin.spec.ts` reutilise desormais le helper partage `loginAsAdmin`, ce qui retire la duplication du login admin et aligne le setup sur les autres suites critiques.
- Le draft profond a ete officiellement sorti du pack critique vers `frontend/e2e/draft-flow.spec.ts` et documente dans `docs/testing/E2E_LOCAL_RUNBOOK.md`.
- Blocage identifie pour la suite draft dediee: dette de contrat draft front/back en local (`/api/draft*` partiel, `404/500` sur une partie de la surface legacy), a traiter dans la story recommandee `8-2-realignement-contrat-draft-serpent-front-back`.
- Le contrat `currentSeason` du catalogue est desormais stabilise via le DTO backend et le mapping frontend, sans dependance a l horloge locale du navigateur.
- Baseline final valide: `npx playwright test` -> `36 passed`, `3 skipped`, `0 failed`.
- Rerun sans reset manuel revalide: `npx playwright test` -> `36 passed`, `3 skipped`, `0 failed`.
- Reliquat hors couverture du pack critique: trade/swap E2E, draft serpent profond, quelques navigations non critiques profil/settings.
- Un warning de cleanup peut encore apparaitre pour des jeux historiques indeletables (`delete=409`, `leave=403`), mais il ne casse plus les reruns ni la suite critique.

### File List

- `_bmad-output/implementation-artifacts/8-1-stabilisation-locale-e2e-critique.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/admin.spec.ts`
- `frontend/e2e/catalogue.spec.ts`
- `frontend/e2e/draft-flow.spec.ts`
- `frontend/e2e/full-game-flow.spec.ts`
- `frontend/e2e/game-lifecycle.spec.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/src/app/features/catalogue/services/player-catalogue.service.spec.ts`
- `frontend/src/app/features/catalogue/services/player-catalogue.service.ts`
- `src/main/java/com/fortnite/pronos/dto/player/CataloguePlayerDto.java`
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceTest.java`

### Change Log

- 2026-03-08 - Stabilized the local Playwright critical pack, activated `CAT-04`, moved deep draft coverage to a dedicated documented suite, and validated two consecutive reruns with `36 passed`, `3 skipped`, `0 failed`.
- 2026-03-08 - Senior review fixes tightened `CAT-03`, aligned admin login setup on the shared helper, stabilized the catalogue season contract, and hardened invitation-code reruns before approving the story.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-08

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. MEDIUM - `frontend/e2e/catalogue.spec.ts`
   - `CAT-03` accepted an empty catalogue state and did not prove the seeded player-card contract in the local environment. The spec now requires at least one accessible result and validates the rendered card structure.
2. MEDIUM - `frontend/e2e/admin.spec.ts`
   - The suite duplicated its own admin login helper instead of reusing the hardened shared helper, which risks future drift between admin and critical-pack setups. The spec now uses `loginAsAdmin`.
3. MEDIUM - `frontend/src/app/features/catalogue/services/player-catalogue.service.ts`
   - The catalogue state synthesized `currentSeason` from the browser clock instead of a stable API-backed contract. The backend DTO now exposes `currentSeason`, and the frontend mapping only falls back to the local deterministic season when the field is absent.
4. HIGH - `frontend/e2e/helpers/app-helpers.ts`
   - The invitation-code flow was not rerunnable on consecutive full-suite executions because join/create steps could race persisted backend state. The helper now resolves or polls the persisted backend invitation code before attempting join, which restored deterministic reruns.

### Validation

- `mvn -q -Dtest=\"PlayerCatalogueServiceTest,PlayerCatalogueSearchTest\" test`
- `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/catalogue/services/player-catalogue.service.spec.ts`
- `npx playwright test e2e/catalogue.spec.ts e2e/admin.spec.ts`
- `npx playwright test`
- second rerun `npx playwright test`

### Result Summary

- Final full suite baseline: `36 passed`, `3 skipped`, `0 failed`
- Second consecutive rerun without manual reset: `36 passed`, `3 skipped`, `0 failed`
- Remaining intentional gap: deep draft flow isolated in `frontend/e2e/draft-flow.spec.ts`, recommended next story `8-2-realignement-contrat-draft-serpent-front-back`
