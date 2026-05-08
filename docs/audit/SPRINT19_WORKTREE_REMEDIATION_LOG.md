# Sprint 19 - Worktree Remediation Log

Date: 2026-05-08
Story: `sprint19-worktree-remediation`

## Objectif

Reduire concretement le worktree Git trop volumineux et melange, sans casser l'application et sans committer de suppressions ou generations d'outillage non validees.

## Decisions appliquees et preuves

Decision source: le 2026-05-07, Thibaut a delegue le choix technique a Codex avec l'objectif explicite de privilegier la fiabilite, la maintenabilite et le bon fonctionnement de l'application. Les exclusions larges d'outillage restent donc locales au worktree quand elles peuvent masquer une surface versionnable.

| Groupe | Decision / validation | Action appliquee | Fichiers restants / pathspec | Verification | Risque residuel / prochaine action |
|---|---|---|---|---|---|
| Cache Maven `.m2/` | Exclusion repo validee: cache local, jamais applicatif | `.m2/` conserve dans `.gitignore` | `cache-local-ignored.pathspec.txt` | `git check-ignore -v .m2/repository/foo` -> `.gitignore` | Aucun risque applicatif connu |
| Backups/config user `_bmad` | Exclusion repo validee: fichiers generes ou personnels | `_bmad/**/*.bak`, `_bmad/config.user.toml`, `_bmad/custom/*.user.toml` conserves dans `.gitignore` | Patterns directs dans `.gitignore` | `git check-ignore -v _bmad/generated.bak _bmad/config.user.toml` -> `.gitignore` | Aucun commit attendu pour ces fichiers |
| Dossiers agents/IDE generes | Exclusion locale seulement: ces dossiers peuvent devenir du tooling projet si adoptes | Patterns larges de `.gitignore` deplaces dans `.git/info/exclude` | `tooling-untracked-generated-directories.hold.txt`, `local-exclude-patterns.snapshot.txt` | `review-verification.snapshot.txt`; `git check-ignore -v .agents/... _bmad/gds/... .claude/commands/...` -> `.git/info/exclude` | Compteurs locaux non reproductibles sans appliquer `local-exclude-patterns.snapshot.txt` |
| Suppressions suivies `.claude`, `.cursor`, `.codex-home`, `_bmad` | Restauration validee: aucune decision de suppression tooling n'existait | Restauration depuis `HEAD` par pathspec explicite | `tooling-tracked-deletions.pathspec.txt` vide | `review-verification.snapshot.txt`; `git status --porcelain=v1 -uall -- _bmad .claude .cursor .codex-home` -> aucune suppression suivie | Aucun |
| Faux `M` d'outillage suivi | Refresh d'index valide car blobs identiques a `HEAD` | Refresh cible; aucun diff staged conserve | `tooling-tracked-modified.pathspec.txt`, `tooling-tracked-modified-restore.pathspec.txt`, `tooling-index-refresh.pathspec.txt` vides | `review-verification.snapshot.txt`; `git diff --cached --name-status` -> vide | Aucun pathspec historique encore actionnable |
| Outillage BMAD non suivi | Hold, pas de commit applicatif | Aucun staging | `bmad-tooling-untracked-hold.pathspec.txt` | `git status --porcelain=v1 -uall -- _bmad` -> 6 entrees `??` | Story tooling dediee si adoption du nouveau systeme `_bmad` |
| Artefacts BMAD et docs remediation | Classer, puis review avant staging | Pathspecs separes; aggregate commit-ready ajoute | `remediation-docs.pathspec.txt`, `bmad-artifacts.pathspec.txt`, `remediation-commit.pathspec.txt`, `bmad-artifacts-historical-hold.pathspec.txt` | `remediation-docs.pathspec.txt` et `bmad-artifacts.pathspec.txt` sont disjoints; staging recommande via `remediation-commit.pathspec.txt` | Ne pas melanger ces artefacts avec un commit applicatif |
| Artefact BMAD modifie hors remediation | Hold explicite, pas historique | Aucun staging | `bmad-artifacts-modified-hold.pathspec.txt` | `git status --porcelain=v1 -- _bmad-output/implementation-artifacts/sprint19-fix-draft-button-guard.md` -> `M` | Commit/revert separe avec la story draft guard |
| Lots applicatifs par story Sprint 19 | Decision AC7: les statuts `done` existants signifient "implementation acceptee"; aucun Git ref n'est pretendu. Les lots non commits bloquent tout push/release tant qu'ils ne sont pas revus et commits separement | Aucun changement applicatif ni reclassification massive de statuts | `story-sprint19-*.pathspec.txt` | `git status --porcelain=v1 -uall` -> 129 entrees applicatives/tests + 14 shared touchpoints | Commit/review par story avant merge; release interdite tant que ces lots restent dans le worktree |
| Shared touchpoints multi-stories | Hold hunk-level | Aucun staging aveugle | `shared-touchpoints-needing-hunk-review.pathspec.txt` | Pathspec separe de 14 fichiers | Staging interactif/hunk review requis |
| Docs agents racine | Separer docs actives et backups | `root-agent-docs.pathspec.txt` limite a `AGENTS.md`/`CLAUDE.md`; backups gardes en hold | `root-agent-docs.pathspec.txt`, `unknown-root-hold.pathspec.txt` | Absence de doublon backups entre ces deux pathspecs | Decision humaine future pour `AGENTS_old-*`, `CLAUDE_old-*`, `code scraping.txt` |

## Etat apres remediations

Commande:

```powershell
git status --porcelain=v1 -uall | Measure-Object
```

Resultat: 214 entrees visibles apres le follow-up de re-review.

Repartition observee:

| Groupe | Statut | Nombre | Decision |
|---|---:|---:|---|
| `src` | M | 60 | Lot applicatif par story |
| `frontend` | M | 48 | Lot applicatif par story |
| `_bmad-output` | ?? | 39 | Artefacts BMAD a rattacher ou hold |
| `docs` | ?? | 29 | Rapports/pathspecs de cette remediation |
| `src` | ?? | 12 | Nouveaux fichiers applicatifs par story |
| `frontend` | ?? | 8 | Nouveaux fichiers applicatifs par story |
| `_bmad` | ?? | 6 | Hold tooling/config BMAD |
| `_bmad-output` | M | 3 | Artefacts BMAD modifies |
| `.gitignore` | M | 1 | Remediation worktree |
| racine divers | M/?? | 8 | Docs agents ou hold |

## Decisions restantes

| Groupe | Decision recommandee |
|---|---|
| `_bmad/_config/skill-manifest.csv` | Hold tooling; ne pas melanger au code applicatif |
| `_bmad/config.toml` | Hold tooling; probablement config installer/team, a valider avant commit |
| `_bmad/custom/.gitignore`, `_bmad/custom/config.toml` | Probablement commit tooling propre si adoption du nouveau systeme de customisation |
| `_bmad/scripts/resolve_config.py`, `_bmad/scripts/resolve_customization.py` | Probablement commit tooling propre, car les skills les referencent |
| `_bmad-output/implementation-artifacts/sprint19-fix-draft-button-guard.md` | Hold BMAD modifie; commit ou revert separe avec la story draft guard |
| `AGENTS_old-*`, `CLAUDE_old-*`, `code scraping.txt` | Hold racine; ne pas committer sans decision humaine ou story dediee |

### Hold applicatif a reclasser avant tout commit

Les 34 fichiers de `application-unknown-hold.pathspec.txt` restent volontairement hors lots applicatifs tant qu'une owner story et une validation cible ne sont pas confirmees.

| Groupe | Fichiers | Story cible probable | Decision actuelle | Validation minimale avant sortie du hold | Risque residuel |
|---|---|---|---|---|---|
| Config locale / securite | `docker-compose.local.yml`, `SecurityConfig.java` | `sprint19-fix-resolution-adapter-config` ou story tooling/securite dediee | Hold | Review hunk par hunk + test securite cible si `SecurityConfig` est stage | Peut modifier l'exposition locale ou les regles auth |
| Admin / E2E pipeline | `frontend/playwright.config.ts`, `admin.models.ts`, `dashboard.*` | `sprint19-suggest-epic-id-robustness` ou follow-up admin | Hold | Diff tracked/untracked status-aware + specs Vitest/E2E ciblees | Peut melanger UX admin et config E2E |
| Game detail / notifications | `game-detail-actions.*`, `GameDetailDto`, `GameDetailService`, `GameNotificationService*` | `sprint19-ux-draft-waiting-screen` ou follow-up draft notification | Hold | Specs game detail + tests notification cibles | Surfaces partagees multi-stories |
| Draft region cursor / tranche | `DraftRegionCursor*`, `DraftTrancheService*`, `V49__add_turn_started_at_to_draft_region_cursors.sql` | Story draft/timer dediee | Hold | Tests draft tranche + validation migration Flyway ciblee | Migration DB publique sensible |
| Snapshot / scraping / scoring | `PrSnapshot*`, `PlayerRepository`, `PlayerRecommendResponse`, `FortniteTrackerScrapingAdapter*`, `ConfidenceScoreService*` | Story catalogue/pipeline dediee | Hold | Tests scraping/catalogue/scoring cibles | Peut changer pipeline de donnees |
| Integration backend | `GameControllerAuthenticationTest`, `GameControllerIntegration*`, `GameServiceTest` | Story backend partagee selon hunks | Hold | Revue hunk-level + tests d'integration cibles | Peut masquer regressions inter-stories |

## Verification

Commandes executees:

```powershell
git status --porcelain=v1 -uall
git status --porcelain=v1 -uall -- _bmad .claude .cursor .codex-home
git status --porcelain=v1 -- _bmad-output/implementation-artifacts/sprint19-fix-draft-button-guard.md
git diff --cached --name-status
git diff --check -- .gitignore _bmad-output/implementation-artifacts/sprint-status.yaml _bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md
git diff --no-index --check -- NUL _bmad-output/implementation-artifacts/sprint19-worktree-remediation.md
git diff --no-index --check -- NUL docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md
git diff --no-index --check -- NUL docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md
git diff --no-index --check -- NUL <chaque fichier non suivi de remediation>
git check-ignore -v .m2/repository/foo _bmad/config.user.toml _bmad/generated.bak .agents/skills/bmad-code-review/SKILL.md _bmad/gds/example.txt .claude/commands/example.md
Intersection `remediation-docs.pathspec.txt` / `bmad-artifacts.pathspec.txt` (commande detaillee dans `review-verification.snapshot.txt`)
```

Resultats:

- Aucune suppression suivie d'outillage restante.
- Aucun diff staged restant apres refresh d'index.
- `git diff --check` couvre maintenant les fichiers suivis modifies du lot, dont `.gitignore` et `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`.
- `git diff --no-index --check` ne remonte aucun probleme sur les nouveaux fichiers non suivis de remediation; les sorties locales sont synthetisees dans `review-verification.snapshot.txt`.
- Les warnings `LF will be replaced by CRLF` sont acceptes comme comportement checkout Windows; aucun whitespace error n'a ete rapporte.
- `remediation-docs.pathspec.txt` et `bmad-artifacts.pathspec.txt` ne se recouvrent plus (`NO_OVERLAP`).
- Les exclusions larges agents/IDE pointent vers `.git/info/exclude`, pas `.gitignore`; seules les caches/config user restent dans `.gitignore`. Le fichier `local-exclude-patterns.snapshot.txt` documente les patterns locaux a appliquer pour reproduire ce resultat dans un autre clone.
- Les changements applicatifs restent non stages et separes par pathspecs.
- Les fichiers multi-stories sont isoles dans `shared-touchpoints-needing-hunk-review.pathspec.txt`.

## Risque residuel

Le worktree contient encore des changements applicatifs nombreux. Ils ne sont plus melanges avec les suppressions massives d'outillage, mais ils doivent encore etre traites par commits ou reviews separes par story avant push.

## Prochaine etape BMAD recommandee

Si ce lot est modifie a nouveau, relancer la review avec le prompt BMAD suivant:

```text
$bmad-code-review _bmad-output/implementation-artifacts/sprint19-worktree-remediation.md
```
