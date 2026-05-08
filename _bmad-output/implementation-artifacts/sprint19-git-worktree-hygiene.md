# Story: sprint19-git-worktree-hygiene

Status: review

<!-- METADATA
  story_key: sprint19-git-worktree-hygiene
  sprint: Sprint 19
  priority: P2 (process hardening after Sprint 19 review)
  date_created: 2026-04-30
  source: code review of sprint19-migrate-canonical-game-participants
-->

## Story

En tant que mainteneur du repo,
je veux une hygiene Git/BMAD explicite pour separer code applicatif, artefacts de suivi et regenerations d'outillage,
afin que les reviews, commits et pushes restent auditables par story sans embarquer de suppressions ou fichiers generes non valides.

## Context / Root Cause

Le worktree Sprint 19 contient simultanement:

- du code applicatif lie a plusieurs stories;
- des artefacts BMAD legitimes;
- une regeneration massive de dossiers agents/IDE (`.claude`, `.cursor`, `_bmad`, `.agents`, etc.);
- des suppressions suivies par Git dans les anciennes arborescences d'outillage.

Ce melange rend dangereux un `git add -A` et complique les reviews BMAD, car une story fonctionnelle peut embarquer des changements d'outillage sans decision explicite.

## Acceptance Criteria

1. Un inventaire Git classe le worktree par categories: code applicatif, tests, artefacts BMAD, outillage agents/IDE, fichiers inconnus.
2. Les changements applicatifs restants sont rattaches a des stories existantes ou a des stories nouvelles avant commit.
3. Les regenerations/suppressions `.claude`, `.cursor`, `.codex-home`, `_bmad` et dossiers agents sont auditees dans un rapport dedie avant tout commit.
4. Aucune suppression d'outillage n'est commitee sans decision explicite documentee.
5. Une procedure courte de branche/commit/push par story est documentee pour les prochaines sessions BMAD.
6. Le sprint status ne marque pas une story `done` sans commit ou reference Git correspondante.

## Tasks / Subtasks

- [x] Task 1 - Cartographier le worktree actuel
  - [x] 1.1 Generer une synthese par top-level folder et statut Git.
  - [x] 1.2 Lister les fichiers applicatifs restants par story probable.
  - [x] 1.3 Lister les fichiers d'outillage generes/supprimes separement.

- [x] Task 2 - Definir les decisions de commit
  - [x] 2.1 Proposer les lots applicatifs restants.
  - [x] 2.2 Identifier les lots qui doivent rester hors commit.
  - [x] 2.3 Marquer les fichiers inconnus comme a confirmer.

- [x] Task 3 - Documenter la procedure
  - [x] 3.1 Ecrire une procedure "pas de git add -A" pour les sessions BMAD.
  - [x] 3.2 Documenter le nommage de branches par story.
  - [x] 3.3 Documenter les validations minimales avant push.

### Review Findings

- [x] [Review][Decision] Decider le sort de `sprint19-worktree-remediation: done` absent du lot de revue - Resolu: statut retire du lot hygiene; la remediation devra etre revue dans son propre lot avec story/reference Git.
- [x] [Review][Decision] Definir la politique pour les stories Sprint 19 deja `done` mais encore sales - Resolu: politique documentee dans le rapport; les `done` historiques sont conserves comme acceptation fonctionnelle, mais bloques release/push jusqu'a isolation, revue et reference Git.
- [x] [Review][Patch] Inventaire des fichiers inconnus contradictoire [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:28] - Corrige: categorie inconnus/a confirmer alignee a 6 et migration V49 listee separement.
- [x] [Review][Patch] Totaux outillage agents/IDE non reconciliables avec le detail top-level [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:25] - Corrige: total marque approximatif et ligne groupee clarifiee comme non utilisable pour suppression.
- [x] [Review][Patch] Rattachement des 109 fichiers modifies et 20 non suivis non verifiable sans inventaire fichier/pathspec [./_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md:82] - Corrige: rapport clarifie le niveau story probable et exige des pathspecs explicites avant commit applicatif.
- [x] [Review][Patch] Audit des suppressions d'outillage trop groupe pour une decision de suppression controlee [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:67] - Corrige: rapport precise qu'il ne valide aucune suppression et donne la commande de rapport fichier par fichier.
- [x] [Review][Patch] Commande d'aggregation top-level non documentee/reproductible [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:10] - Corrige: commande PowerShell d'aggregation ajoutee.
- [x] [Review][Patch] `spotless:apply` presente comme validation pure alors qu'il peut modifier le worktree [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:106] - Corrige: `spotless:apply` decrit comme etape potentiellement mutante, suivie de `git diff --check` et tests.

## Dev Notes

- Ne pas nettoyer le worktree avec `git clean`, `git reset --hard` ou suppression recursive.
- Ne pas commiter les dossiers agents/IDE avant rapport et decision.
- Preferer des commits par story ou par type d'artefact (`feat`, `fix`, `docs(bmad)`, `chore(tooling)`).
- Cette story est process/outillage; elle ne doit pas absorber de correction fonctionnelle.

## Testing Requirements

- Verification minimale:
  - `git status --porcelain=v1` groupe par top-level folder;
  - `git diff --cached --check` avant chaque commit;
  - tests cibles uniquement si un lot applicatif est modifie.

## Dev Agent Record

### Debug Log References

- 2026-05-07: Workflow `bmad-dev-story` active; `python3` indisponible, customisation resolue manuellement depuis `customize.toml`.
- 2026-05-07: Story cible decouverte via `sprint-status.yaml`: premiere entree Sprint 19 `ready-for-dev`, `sprint19-git-worktree-hygiene`.
- 2026-05-07: Inventaire realise avec `git status --porcelain=v1 -uall`, plus vues ciblees `src/frontend/e2e` et `_bmad-output/AGENTS/CLAUDE`.
- 2026-05-07: Rapport d'audit cree sans staging, sans cleanup, sans suppression et sans modification de code applicatif.

### Completion Notes List

- Cartographie produite par categorie et par top-level folder dans `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`.
- Les 109 fichiers applicatifs modifies et 20 applicatifs non suivis sont rattaches au niveau story probable ou marques `a confirmer`; des pathspecs explicites restent requis avant tout commit applicatif.
- Les suppressions/regenerations `.claude`, `.cursor`, `.codex-home`, `_bmad`, `.agents`, `.m2` et dossiers agents/IDE sont separees du scope applicatif avec decision `ne pas committer sans validation tooling`.
- Une procedure BMAD explicite interdit `git add -A`, impose le staging par pathspec, le controle `git diff --cached --check`, les validations ciblees et une branche par story.
- La regle sprint-status est documentee: aucune story `done` sans hash/PR/reference Git, code review requise pour le code et File List complete.
- Code review appliquee: statut remediation retire du lot hygiene, politique Sprint 19 release-block documentee, compteurs/commandes d'audit clarifies; la story reste en `review` jusqu'a commit/reference Git.

### File List

- `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`

### Change Log

- 2026-04-30: Story backlog creee pour traiter separement l'hygiene Git/worktree detectee en review Sprint 19.
- 2026-05-07: Story demarree en `in-progress` via `bmad-dev-story`.
- 2026-05-07: Rapport d'audit Git/worktree cree; tasks 1 a 3 completees; story passee en `review`.
- 2026-05-07: Code review hygiene appliquee; story maintenue en `review` tant que le commit/reference Git n'existe pas.
