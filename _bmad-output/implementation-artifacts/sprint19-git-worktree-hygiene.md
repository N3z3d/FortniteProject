# Story: sprint19-git-worktree-hygiene

Status: backlog

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

- [ ] Task 1 - Cartographier le worktree actuel
  - [ ] 1.1 Generer une synthese par top-level folder et statut Git.
  - [ ] 1.2 Lister les fichiers applicatifs restants par story probable.
  - [ ] 1.3 Lister les fichiers d'outillage generes/supprimes separement.

- [ ] Task 2 - Definir les decisions de commit
  - [ ] 2.1 Proposer les lots applicatifs restants.
  - [ ] 2.2 Identifier les lots qui doivent rester hors commit.
  - [ ] 2.3 Marquer les fichiers inconnus comme a confirmer.

- [ ] Task 3 - Documenter la procedure
  - [ ] 3.1 Ecrire une procedure "pas de git add -A" pour les sessions BMAD.
  - [ ] 3.2 Documenter le nommage de branches par story.
  - [ ] 3.3 Documenter les validations minimales avant push.

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

### File List

- `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-04-30: Story backlog creee pour traiter separement l'hygiene Git/worktree detectee en review Sprint 19.
