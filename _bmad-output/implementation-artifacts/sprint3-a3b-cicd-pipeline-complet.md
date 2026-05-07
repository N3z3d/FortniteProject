# Story Sprint3-A3b: Pipeline CI/CD complet

Status: done

<!-- METADATA
  story_key: sprint3-a3b-cicd-pipeline-complet
  branch: story/sprint3-a3b-cicd-pipeline-complet
  sprint: Sprint 3
  priority: P1
  created_at: 2026-04-30
  Note: Story de reconciliation CI/CD. Ne pas rouvrir un deploiement staging public sans decision produit explicite.
-->

## Story

As a maintainer preparing FortniteProject for reliable delivery,
I want the GitHub Actions pipeline to fail on real regressions, publish a deployable production Docker image, and expose a guarded staging/deploy path only when prerequisites exist,
so that the project has a trustworthy CI/CD baseline without reintroducing obsolete hosting scope.

## Acceptance Criteria

1. **CI bloquante et observable**
   **Given** a push or pull request triggers `.github/workflows/ci.yml`, **when** backend or frontend validation fails, **then** the workflow fails and publishes readable logs/artifacts. The Vitest step must not use `continue-on-error: true` now that the frontend baseline is documented as green.

2. **Backend and frontend gates preserved**
   **Given** CI runs, **when** the jobs execute, **then** backend runs `mvn spotless:check -B --no-transfer-progress` and `mvn verify -B --no-transfer-progress`, frontend runs `npm ci --silent`, `npm run test:vitest`, and `npm run build`, and build artifacts remain uploaded with short retention.

3. **GHCR publication robust on `main`**
   **Given** CI runs on `refs/heads/main`, **when** backend and frontend jobs are green, **then** Docker builds the `production` target and pushes `latest` plus immutable SHA tags to `ghcr.io/n3z3d/fortniteproject` or the normalized lowercase repository image name.

4. **Registry authentication and permissions are least-privilege**
   **Given** the Docker publish job authenticates to GHCR, **when** it uses `GITHUB_TOKEN`, **then** the job declares only required permissions (`contents: read`, `packages: write`; add `attestations: write` and `id-token: write` only if provenance is implemented) and no secret is hardcoded.

5. **Deploy/staging is guarded, not silently reinvented**
   **Given** Sprint 4/9 decisions mark public hosting/staging as out of scope, **when** this story is implemented, **then** no public staging deploy is added unless a GitHub Environment `staging` and required secrets already exist. If prerequisites are absent, the workflow must produce an explicit skipped summary and the story must not claim a real deploy.

6. **External validation is mandatory before `done`**
   **Given** `.github/workflows/*.yml` is modified, **when** the story is moved toward completion, **then** a push on `main` and a visible green GitHub Actions run are required before `sprint3-a3b-cicd-pipeline-complet` can become `done`.

## Tasks / Subtasks

- [x] Task 1 - Requalifier l'etat reel du workflow existant (AC: #1, #2, #3)
  - [x] 1.1 Lire `.github/workflows/ci.yml` en entier avant edition.
  - [x] 1.2 Documenter dans le Debug Log le drift actuel: Vitest est encore non bloquant (`continue-on-error: true`) alors que `sprint7-f2` indique que la step devait redevenir bloquante.
  - [x] 1.3 Verifier que les jobs backend, frontend et Docker conservent les gates existants avant tout refactor YAML.

- [x] Task 2 - Rendre les gates CI bloquantes et lisibles (AC: #1, #2)
  - [x] 2.1 Supprimer `continue-on-error: true` du step Vitest et ajuster le summary pour refléter un vrai rouge.
  - [x] 2.2 Garder l'upload du log Vitest avec `if: always()` pour faciliter le diagnostic.
  - [x] 2.3 Ne pas masquer `ng lint` ou le remplacer par un faux vert; si le projet n'a pas de lint configure, documenter explicitement le choix ou creer une commande verifiable.

- [x] Task 3 - Fiabiliser le build/push Docker GHCR (AC: #3, #4)
  - [x] 3.1 Conserver la normalisation lowercase de l'image GHCR.
  - [x] 3.2 Preferer les actions Docker officielles (`docker/login-action`, `docker/metadata-action`, `docker/build-push-action`, `docker/setup-buildx-action`) ou justifier le maintien du Docker CLI direct si le diff doit rester minimal.
  - [x] 3.3 Si des actions externes sont ajoutees, pin les versions/SHA selon la politique repo et ajouter un commentaire sur le choix.
  - [x] 3.4 Ajouter un cache Buildx (`cache-from/to: type=gha`) seulement si le job migre vers `docker/build-push-action`; ne pas bricoler un cache manuel fragile.

- [x] Task 4 - Cadrer le deploy/staging sans rouvrir l'hebergement (AC: #5)
  - [x] 4.1 Verifier s'il existe un environnement GitHub `staging` et des secrets de deploiement deja configures.
  - [x] 4.2 Si les prerequis existent, ajouter un job deploy manuel ou protege qui deploye l'image publiee et valide `/actuator/health`.
  - [x] 4.3 Si les prerequis n'existent pas, ajouter uniquement un resume explicite "staging deploy skipped: hosting decision absent" et ne pas inventer Railway/Supabase/SSH credentials.

- [x] Task 5 - Validations locales et distantes (AC: #1, #2, #3, #6)
  - [x] 5.1 Lancer `mvn verify -B --no-transfer-progress`.
  - [x] 5.2 Lancer `npm run test:vitest` depuis `frontend/`.
  - [x] 5.3 Lancer `npm run build` depuis `frontend/`.
  - [x] 5.4 Lancer `docker build --target production -t fortnite-ci-local .`.
  - [x] 5.5 Fournir la preuve du run GitHub Actions vert sur la branche de stabilisation.
  - [x] 5.6 Avant passage `done`, merger/pousser sur `main` et verifier le run `main` avec Docker/GHCR non-skipped.

### Review Findings

- [x] [Review][Patch] Vitest summary can report passed when Vitest was skipped [.github/workflows/ci.yml:132]
- [x] [Review][Patch] External GitHub validation is checked off before proof exists [_bmad-output/implementation-artifacts/sprint3-a3b-cicd-pipeline-complet.md:68]

## Dev Notes

### Current State

- `sprint-status.yaml` contient encore `sprint3-a3b-cicd-pipeline-complet: backlog`, mais les artefacts ulterieurs indiquent que le sujet a ete partiellement traite:
  - `sprint7-f2-ci-vitest-et-docker-push: done` confirme le step Vitest visible et le push GHCR.
  - `sprint8-f2-validation: done` annonce la fermeture d'A3b via validation CI/GHCR.
  - `sprint9-p0-cleanup` marque `sprint4-a3b-cicd-pipeline-complet` en `wont-do` car le deploy staging public est hors scope.
- Le fichier actuel `.github/workflows/ci.yml` conserve cependant `continue-on-error: true` sur Vitest. C'est une incoherence a corriger: un rouge frontend ne doit plus rester faux vert.
- Le job Docker actuel utilise le CLI (`docker login`, `docker build`, `docker push`) et normalise correctement `ghcr.io/${{ github.repository }}` en lowercase. Ce point doit etre preserve si le job est refactore.

### Architecture and Project Constraints

- Architecture par defaut: layered/hexagonal pour le code applicatif, mais cette story ne doit pas toucher le domaine Java/Angular sauf necessite de validation CI.
- Fichiers UPDATE probables:
  - `.github/workflows/ci.yml` - workflow CI principal; lire completement avant edition.
  - Optionnel: `README.md` ou documentation CI si le repo en possede deja une section pertinente.
- Fichiers a ne pas modifier sans plan dedie:
  - Schemas DB publics, migrations Flyway, contrats API, code metier backend/frontend.
  - `Dockerfile` et `docker-compose.local.yml` sauf si le build CI prouve une casse directement liee a cette story.
- Aucune nouvelle dependance applicative Maven/npm n'est attendue.
- Les actions GitHub externes ne sont acceptables que si elles reduisent un risque concret du workflow (auth GHCR, tagging, cache Buildx, provenance) et restent pinnees/versionnees.

### Testing Requirements

- Pas de test unitaire artificiel pour le fichier YAML seul. Les verifications obligatoires sont les commandes de build/test et le run GitHub Actions reel.
- TDD adapte infra:
  - Red: constater et consigner le faux vert actuel (`continue-on-error: true`) comme echec d'acceptance criteria.
  - Green: supprimer le faux vert et obtenir un workflow localement coherent.
  - Refactor: simplifier le job Docker sans modifier le comportement attendu.
- Si du code Java/TypeScript est touche, ajouter/mettre a jour les tests unitaires correspondants avec cas nominal, au moins 3 edge cases et erreurs attendues.
- Toute modification de `.github/workflows/*.yml` declenche la regle Sprint 8 C2: validation par push `main` + CI vert visible avant `done`.

### Latest Technical Information

- GitHub recommande pour GHCR l'authentification avec `GITHUB_TOKEN`, `registry: ghcr.io`, et des permissions explicites `packages: write` sur le job de publication. Source: https://docs.github.com/en/actions/how-tos/use-cases-and-examples/publishing-packages/publishing-docker-images
- Quand une section `permissions` est specifiee dans GitHub Actions, les permissions non listees sont mises a `none`; declarer seulement ce qui est necessaire. Source: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- Les docs Docker recommandent les actions officielles Docker pour build/push, login, metadata et Buildx; elles donnent aussi le support du cache GitHub Actions via `docker/build-push-action`. Source: https://docs.docker.com/build/ci/github-actions/
- Depuis la transition GitHub cache API v2, utiliser les versions recentes de Buildx/BuildKit; avec `docker/build-push-action` sur runners GitHub, ces parametres sont geres automatiquement. Source: https://docs.docker.com/build/cache/backends/gha/

### Pre-existing Gaps / Known Issues

- [KNOWN] Staging public: explicitement hors scope depuis Sprint 4/9; ne pas le re-proposer sans decision produit.
- [KNOWN] Le push GHCR reel et l'apparition du package ne sont pas prouvables localement; preuve GitHub Actions/GHCR requise.
- [KNOWN] Le worktree peut contenir de nombreux changements non lies; ne pas les revert ni les inclure dans cette story.
- [KNOWN] `python3` n'est pas utilisable dans cet environnement Windows pour les scripts BMAD; cela n'impacte pas l'implementation CI/CD.

### Project Structure Notes

```
.github/
└── workflows/
    └── ci.yml                  <- UPDATE probable

Dockerfile                      <- lire si le build Docker echoue; eviter modification hors cause directe
docker-compose.local.yml        <- reference local prod-like; ne pas transformer en deploy staging
pom.xml                         <- gates Maven/Spotless/JaCoCo deja configures
frontend/package.json           <- scripts Vitest/build existants
_bmad-output/implementation-artifacts/
└── sprint-status.yaml          <- statut a mettre a jour seulement apres workflow de review/done
```

### References

- [Source: `_bmad-output/implementation-artifacts/sprint-status.yaml` - Sprint 3 A3b backlog, Sprint 4 staging `wont-do`, Sprint 7/8 CI/GHCR done]
- [Source: `_bmad-output/implementation-artifacts/sprint3-a3a-cicd-pipeline-minimal.md` - pipeline minimal initial]
- [Source: `_bmad-output/implementation-artifacts/sprint7-f2-ci-vitest-et-docker-push.md` - Vitest visible, GHCR, drift `continue-on-error`]
- [Source: `_bmad-output/implementation-artifacts/sprint8-f2-validation.md` - validation CI/GHCR de fermeture A3b]
- [Source: `_bmad-output/implementation-artifacts/sprint8-c1c2-dod-process.md` - validation CI/CD obligatoire avant `done`]
- [Source: `_bmad-output/implementation-artifacts/sprint9-p0-cleanup.md` - staging public hors scope]
- [Source: `.github/workflows/ci.yml` - etat actuel du workflow]
- [Source: `Dockerfile` et `docker-compose.local.yml` - build production et stack local prod-like]
- [Source: `_bmad-output/project-context.md` - stack, DoD, contraintes de tests et CI]

### Project Context Reference

- Lire `_bmad-output/project-context.md` avant implementation. Ses regles DoD, tests, limites de taille, dependances et validation CI/CD s'appliquent a cette story.
- Cette story est un cadrage d'implementation, pas une autorisation a modifier des zones sensibles hors `.github/workflows/ci.yml` sans plan dedie.

### Story Completion Status

Ultimate context engine analysis completed - comprehensive developer guide created.

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-04-30: RED infra confirme: `.github/workflows/ci.yml` contient `continue-on-error: true` sur Vitest, donc AC1 echoue avant correction.
- 2026-04-30: Lecture complete de `.github/workflows/ci.yml`; jobs backend, frontend et Docker identifies, gates Maven/Vitest/build/Docker conserves.
- 2026-04-30: `continue-on-error: true` supprime du step Vitest; l'upload `frontend-vitest-log` reste en `if: always()`.
- 2026-04-30: `ng lint || true` remplace par un check Node explicite: le repo n'a pas de target Angular lint, donc Vitest + build production restent les gates frontend verifiables.
- 2026-04-30: Docker CLI direct conserve pour minimiser le diff; normalisation lowercase GHCR et permissions `contents: read` / `packages: write` preservees.
- 2026-04-30: Aucun environnement/secrets `staging` verifiable localement; aucun deploy public ajoute, resume GitHub Step Summary explicite en skipped.
- 2026-04-30: Validations: check absence `continue-on-error` OK; check lint config OK; `mvn verify -B --no-transfer-progress` OK hors sandbox; `npm run test:vitest` OK; `npm run build` OK; `docker build --target production -t fortnite-ci-local .` OK.
- 2026-05-05: Code review BMad executee; 2 patches appliques: summary Vitest robuste quand le step n'a pas tourne, et preuve GitHub Actions `main` remise en attente avant `done`.
- 2026-05-05: Validations post-review: parse YAML `.github/workflows/ci.yml` OK; check lint config OK; `npm run test:vitest` OK; `npm run build` OK; `mvn verify -B --no-transfer-progress` OK; `docker build --target production -t fortnite-ci-local .` OK apres redemarrage Docker Desktop.
- 2026-05-07: Branche `story/sprint19-bmad-stabilization` poussee et rendue verte en GitHub Actions apres corrections de stabilite tests Spring/H2/SecurityContext.
- 2026-05-07: Run GitHub Actions vert `25480698230` sur `d2dff1a`: backend success, frontend success, Docker/GHCR skipped attendu hors `main`.
- 2026-05-07: Validations locales complementaires: `mvn verify -B --no-transfer-progress`, sequence ciblee security/DB, sequence ciblee Linux via Docker.
- 2026-05-07: Fast-forward `main` confirme et pousse sur `ea5ed89`; run GitHub Actions `25484065848` vert avec backend success, frontend success, Docker build + push GHCR success.

### Completion Notes List

- CI frontend maintenant bloquante sur Vitest: un test rouge echoue le job au lieu de rester en faux vert.
- Le log Vitest reste publie en artifact avec `if: always()` pour garder le diagnostic.
- Le lint Angular n'est plus masque par `|| true`; l'absence de target lint est verifiee explicitement et echouera si une target apparait sans cablage CI.
- Le job Docker GHCR conserve le push `latest` + SHA avec image lowercase et permissions minimales.
- Le staging/deploy reste volontairement skipped avec un resume explicite; aucun hebergement ni secret n'a ete invente.
- La story est passee en `done` apres validation `main`: le run GitHub Actions `25484065848` prouve backend, frontend et Docker/GHCR verts.
- Les findings de code review ont ete traites; aucun item HIGH/MEDIUM ouvert ne reste dans la section Review Findings.
- Les echecs CI backend observes apres push ont ete corriges sans elargir le scope staging: isolation H2, attachement explicite de Spring Security dans le test MVC, nettoyage du `SecurityContextHolder`.

### File List

- `.github/workflows/ci.yml`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/sprint3-a3b-cicd-pipeline-complet.md`
- `src/test/resources/application-test.yml`
- `src/test/java/com/fortnite/pronos/debug/ApplicationPortTest.java`
- `src/test/java/com/fortnite/pronos/debug/ApplicationStartupDiagnosticTest.java`
- `src/test/java/com/fortnite/pronos/integration/DatabaseIntegrationTest.java`
- `src/test/java/com/fortnite/pronos/config/SecurityConfigSimultaneousDraftAuthorizationTest.java`
- `src/test/java/com/fortnite/pronos/config/WebSocketAuthInterceptorTest.java`

### Change Log

- 2026-04-30: CI Vitest rendu bloquant, lint Angular remplace par check explicite, resume staging skipped ajoute, validations locales terminees.
- 2026-05-05: Review follow-ups appliques: summary Vitest `not run` si skipped, task 5.5 reouverte jusqu'a preuve CI `main`.
- 2026-05-07: CI de branche rendue verte (`25480698230`) via stabilisation des tests backend; passage `done` garde le gate `main`/GHCR.
- 2026-05-07: `main` fast-forward sur `ea5ed89`; CI `main` verte (`25484065848`) avec Docker/GHCR success; story fermee.

### Decisions Confirmed for `done`

- Aucune cible staging publique n'est inventee dans cette story; la partie deploy reste explicitement skipped hors decision produit/hebergement.
- Decision prise: garder le Docker CLI direct pour minimiser le diff tant que les AC sont satisfaits; migrer vers les actions Docker officielles seulement dans une story dediee.
- Action risquee `main` confirmee par l'utilisateur et executee: run `25484065848` vert avec Docker/GHCR non-skipped.
