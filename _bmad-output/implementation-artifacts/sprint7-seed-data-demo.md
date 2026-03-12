# Story Sprint7 Seed Data Demo

Status: ready-for-dev

<!-- METADATA
  story_key: sprint7-seed-data-demo
  branch: story/sprint7-seed-data-demo
  sprint: Sprint 7
  priority: P1
  Note: Story locale produit. Objectif: rendre `docker compose` demo-able sans preparation manuelle, avec des donnees realistes et deterministes.
-->

## Story

As a maintainer preparing a local demo,
I want the local stack to start with realistic seeded data,
so that the app is immediately navigable and credible without manual setup steps.

## Acceptance Criteria

1. Un seed SQL ou mecanisme equivalent prepare automatiquement au moins `3` equipes et `15` joueurs Fortnite exploitables localement.
2. Deux parties existent apres demarrage local:
   - une partie terminee avec scores consultables
   - une partie en cours avec draft partiellement rempli
3. Le seed est integre au demarrage local (`docker-compose.local.yml` ou mecanisme equivalent) sans etapes manuelles supplementaires.
4. Les parcours de consultation principaux sont demo-ables juste apres demarrage:
   - home / dashboard
   - catalogue
   - detail de partie
   - trades ou leaderboard selon les donnees seedes
5. Les donnees seedees sont deterministes, documentees, et ne cassent pas les tests locaux existants.

## Tasks / Subtasks

- [ ] Task 1 - Cartographier les hooks de seed locaux existants (AC: #3, #5)
  - [ ] 1.1 Identifier les scripts SQL/Flyway/de seed deja presents
  - [ ] 1.2 Verifier comment `docker-compose.local.yml` initialise actuellement la base

- [ ] Task 2 - Concevoir un dataset demo realiste et stable (AC: #1, #2, #5)
  - [ ] 2.1 Definir les joueurs, equipes, parties et scores minimums
  - [ ] 2.2 Verifier la coherence avec les contraintes metier actuelles

- [ ] Task 3 - Integrer le seed au demarrage local (AC: #1, #2, #3)
  - [ ] 3.1 Ajouter ou brancher le seed dans le compose local
  - [ ] 3.2 Eviter toute etape manuelle post-demarrage

- [ ] Task 4 - Valider les parcours demo (AC: #4, #5)
  - [ ] 4.1 Verifier que l app est navigable avec les donnees seedees
  - [ ] 4.2 Documenter les comptes/jeux/pages de demo utiles

## Dev Notes

### Scope Guardrails

- Cette story ne rouvre pas staging, Railway ou l hebergement public.
- Cette story ne doit pas casser les seeds E2E deja en place.
- Cette story vise un environnement local demo-able, pas un jeu de donnees de production.

### References

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `docker-compose.local.yml`
- `src/main/resources/db/seed/`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`

### Pre-existing Gaps / Known Issues

- [KNOWN] Les comptes seedes locaux et les datasets E2E existent deja partiellement, mais ils ne garantissent pas encore une demo locale riche des parcours metier.
- [KNOWN] La story `sprint7-f2-ci-vitest-et-docker-push` reste bloquee sur la preuve GHCR distante; elle ne doit pas bloquer cette story locale.
