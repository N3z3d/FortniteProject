# Story: sprint19-worktree-remediation - Remise a plat concrete du worktree Git

Status: done

<!-- METADATA
  story_key: sprint19-worktree-remediation
  sprint: Sprint 19
  priority: P1 (process/release hygiene)
  date_created: 2026-05-07
  source: user clarification 2026-05-07 + docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md
-->

## Story

En tant que mainteneur du repo,
je veux remettre le worktree Git a plat en separant les changements applicatifs, les artefacts BMAD et les regenerations d'outillage,
afin que les prochains commits et reviews soient lisibles, auditables et sans embarquer de suppressions ou fichiers generes accidentels.

## Context / Root Cause

La story `sprint19-git-worktree-hygiene` a produit le rapport d'audit, mais elle ne corrige pas encore le probleme principal: le worktree reste trop gros et melange plusieurs natures de changement.

Etat observe au 2026-05-07:

- 109 fichiers applicatifs modifies et 20 applicatifs non suivis;
- 3 artefacts BMAD modifies et 38 artefacts BMAD non suivis;
- 301 suppressions d'outillage agents/IDE suivies par Git;
- 31 665 fichiers agents/IDE non suivis;
- 3 372 fichiers `.m2` non suivis dans le repo;
- plusieurs stories Sprint 19 deja `done` semblent encore porter des changements non commits dans le worktree.

Decision de story:

- cette story est le chantier de correction, pas seulement un audit;
- aucune commande destructive globale (`git clean`, `git reset --hard`, suppression recursive large) n'est autorisee;
- les changements applicatifs doivent etre isoles par story existante avant commit;
- les dossiers agents/IDE et caches doivent etre retires du worktree visible uniquement apres decision explicite;
- les suppressions suivies par Git doivent etre soit restaurees, soit commitees dans un lot tooling dedie, jamais melangees a une story fonctionnelle.

## Acceptance Criteria

1. Le worktree est classe en lots actionnables avec pathspecs explicites: applicatif par story, artefacts BMAD, outillage agents/IDE, caches locaux, fichiers inconnus.
2. Chaque lot applicatif restant est rattache a une story Sprint 19 existante ou a une nouvelle story avant staging.
3. Les suppressions suivies d'outillage (`.claude`, `.cursor`, `.codex-home`, `_bmad`) sont resolues par une decision documentee: restauration ou commit `chore(tooling)` dedie.
4. Les fichiers non suivis generes/caches (`.m2`, `.agents`, dossiers agents/IDE massifs) sont exclus ou supprimes seulement apres validation explicite du groupe concerne.
5. Aucun `git add -A` n'est utilise; tout staging passe par pathspec explicite et verification `git diff --cached --name-status`.
6. Le worktree final ne contient plus de melange applicatif + tooling dans le meme lot de commit propose.
7. `sprint-status.yaml` ne marque aucune story `done` sans reference Git ou decision de non-commit documentee.
8. Un rapport final indique pour chaque groupe: action appliquee, fichiers restants, commande de verification, risque residuel.
9. Les validations minimales sont executees selon la nature des lots:
   - documentation/process: `git diff --check`;
   - backend: `mvn spotless:apply --no-transfer-progress` + tests cibles;
   - frontend: `npm run test:vitest -- <specs cibles>`;
   - E2E uniquement pour les flows touches.
10. Aucune suppression de fichier suivi ou non suivi n'est faite sans trace dans le rapport final et decision explicite.

## Tasks / Subtasks

- [x] Task 1 - Construire les lots executables depuis l'audit (AC: #1, #2, #6)
  - [x] 1.1 Generer des pathspecs explicites par story probable.
  - [x] 1.2 Generer un pathspec separe pour artefacts BMAD.
  - [x] 1.3 Generer un pathspec separe pour outillage agents/IDE suivi par Git.
  - [x] 1.4 Marquer les fichiers inconnus comme `hold` jusqu'a decision.

- [x] Task 2 - Stabiliser les caches et fichiers non suivis massifs (AC: #4, #8, #10)
  - [x] 2.1 Proposer la decision pour `.m2`: supprimer du repo local ou l'exclure explicitement.
  - [x] 2.2 Proposer la decision pour `.agents` et les dossiers agents/IDE generes.
  - [x] 2.3 Appliquer uniquement les exclusions/suppressions validees.
  - [x] 2.4 Verifier que `git status --porcelain=v1 -uall` ne remonte plus ces groupes si la decision est exclusion.

- [x] Task 3 - Resoudre les suppressions suivies d'outillage (AC: #3, #5, #10)
  - [x] 3.1 Lister toutes les suppressions suivies par Git pour `.claude`, `.cursor`, `.codex-home`, `_bmad`.
  - [x] 3.2 Pour chaque groupe, documenter `restore` ou `commit tooling`.
  - [x] 3.3 Si decision `restore`: restaurer uniquement le groupe valide avec pathspec explicite.
  - [x] 3.4 Si decision `commit tooling`: garder le lot hors commits applicatifs et verifier `git diff --cached --name-status`.

- [x] Task 4 - Isoler les changements applicatifs par story (AC: #2, #5, #6, #9)
  - [x] 4.1 Identifier le lot `sprint19-feat-invitation-code-advanced`.
  - [x] 4.2 Identifier le lot `sprint19-contract-create-game-region-rules`.
  - [x] 4.3 Identifier le lot `sprint19-migrate-canonical-game-participants`.
  - [x] 4.4 Identifier le lot `sprint19-suggest-epic-id-robustness`.
  - [x] 4.5 Identifier le lot `sprint19-fix-catalogue-demo-banner`.
  - [x] 4.6 Identifier le lot `sprint19-ux-draft-waiting-screen`.
  - [x] 4.7 Identifier les lots draft/leaderboard/resolution restants ou les basculer en `hold`.

- [x] Task 5 - Produire le rapport final de remise a plat (AC: #7, #8, #10)
  - [x] 5.1 Creer ou mettre a jour un rapport final sous `docs/audit/`.
  - [x] 5.2 Documenter les actions appliquees et non appliquees.
  - [x] 5.3 Documenter les commandes de verification executees.
  - [x] 5.4 Mettre a jour les File Lists des stories impactees uniquement si elles sont effectivement touchees.
  - [x] 5.5 Mettre cette story en `review` seulement quand le worktree est effectivement reduit et les lots restants sont intentionnels.

### Review Findings

- [x] [Review][Decision] `.gitignore` ignores broad agent/tooling directories without explicit validation - resolved: repo-level ignore now keeps only cache/user-scoped patterns; broad agent/IDE generated-directory exclusions moved to `.git/info/exclude` and documented in the report.
- [x] [Review][Decision] `sprint-status.yaml` keeps Sprint 19 stories as `done` while their file lots remain uncommitted - resolved: AC7 decision documented in the report; done means implementation accepted, but release/push remains blocked until story lots are committed/reviewed separately.
- [x] [Review][Decision] Historical `sprint-status.yaml` rewrites are out of scope or need explicit closure policy - resolved: historical rewrites were reverted; sprint-status diff now only updates date plus the two Sprint 19 process entries.
- [x] [Review][Patch] Validation command does not cover untracked docs/pathspecs [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:61] - fixed with `git diff --no-index --check` validation documented.
- [x] [Review][Patch] Final report lacks per-group action/files/verification/risk table [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:12] - fixed with per-group decision/proof table.
- [x] [Review][Patch] Source hygiene report is referenced but absent from File List/pathspec classification [_bmad-output/implementation-artifacts/sprint19-worktree-remediation.md:99] - fixed by adding it to File List and `remediation-docs.pathspec.txt`.
- [x] [Review][Patch] Root backup files appear in both root-agent-docs and unknown-root-hold pathspecs [docs/audit/worktree-remediation/root-agent-docs.pathspec.txt:2] - fixed by keeping backups only in `unknown-root-hold.pathspec.txt`.
- [x] [Review][Patch] Resolved tooling pathspecs remain actionable despite README reporting zero remaining modifications [docs/audit/worktree-remediation/README.md:12] - fixed by emptying resolved tooling pathspecs.
- [x] [Review][Patch] Story status is `done` but sprint tracking does not reference `sprint19-worktree-remediation` [_bmad-output/implementation-artifacts/sprint19-worktree-remediation.md:3] - fixed by keeping the remediation story in `review` until re-review, then syncing `done` after all patches were resolved.
- [x] [Review][Patch] Untracked files in story pathspecs can be reviewed with an empty `git diff -- <path>` [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:77] - fixed by documenting status-aware tracked/untracked diff commands.
- [x] [Review][Patch] `.gitignore` is modified but missing from remediation pathspecs and documented `git diff --check` coverage [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:70] - fixed by adding `.gitignore` to `remediation-docs.pathspec.txt` and verification commands.
- [x] [Review][Patch] BMAD artifacts pathspec mixes current remediation files with historical review/story artifacts [docs/audit/worktree-remediation/bmad-artifacts.pathspec.txt:1] - fixed by keeping only current process artifacts in `bmad-artifacts.pathspec.txt` and moving historical artifacts to `bmad-artifacts-historical-hold.pathspec.txt`.
- [x] [Review][Patch] Application unknown hold files lack per-group remaining decision details [docs/audit/worktree-remediation/application-unknown-hold.pathspec.txt:1] - fixed by adding a per-group application hold decision table to the remediation log.
- [x] [Review][Patch] Local `.git/info/exclude` proof is not reproducible from the reviewed diff [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:18] - fixed by adding `local-exclude-patterns.snapshot.txt` and documenting that the proof is local unless the snapshot is applied.
- [x] [Review][Patch] `.m2/` is both repo-ignored and still listed in generated-directory hold [docs/audit/worktree-remediation/tooling-untracked-generated-directories.hold.txt:18] - fixed by removing `.m2/` from generated-directory hold because it is a repo-level cache ignore.
- [x] [Review][Patch] Staging example does not guard paths containing spaces [docs/audit/worktree-remediation/unknown-root-hold.pathspec.txt:5] - fixed by recommending `git add --pathspec-from-file` and quoted single-path commands.
- [x] [Review][Patch] Remediation verification claims lacked versioned proof [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:18] - fixed by adding `review-verification.snapshot.txt`, marking local counts as local, and documenting CRLF warnings.
- [x] [Review][Patch] Remediation staging pathspec was ambiguous/non-disjoint [docs/audit/worktree-remediation/README.md:58] - fixed by adding `remediation-commit.pathspec.txt` and separating remediation docs from BMAD sidecar artifacts.
- [x] [Review][Patch] Modified `sprint19-fix-draft-button-guard.md` was hidden in historical hold [docs/audit/worktree-remediation/bmad-artifacts-historical-hold.pathspec.txt] - fixed by moving it to `bmad-artifacts-modified-hold.pathspec.txt` with a separate commit/revert decision.
- [x] [Review][Patch] Cache `.m2/` lacked an explicit pathspec lot [docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md:16] - fixed by adding `cache-local-ignored.pathspec.txt`.
- [x] [Review][Patch] BMAD prompt was documented as an invalid PowerShell command [docs/audit/worktree-remediation/README.md:21] - fixed by labeling it as a BMAD chat prompt, not a PowerShell command.
- [x] [Review][Patch] Remediation story can be `done` without its own Git reference [_bmad-output/implementation-artifacts/sprint19-worktree-remediation.md:3] - fixed by returning the story and sprint-status entry to `review` until a Git reference exists.
- [x] [Review][Patch] Untracked files are not covered by the diff comparison guard [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:145] - fixed by requiring post-staging `git status --porcelain=v1 -uall` and explicit `??` review.
- [x] [Review][Patch] Commit-reference procedure is ordered ambiguously [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:151] - fixed by documenting commit -> second docs commit or PR reference -> push sequencing.
- [x] [Review][Patch] Hold-overlap verification can false-negative on non-`.pathspec.txt` hold lists [docs/audit/worktree-remediation/review-verification.snapshot.txt:110] - fixed by checking both `*hold*.pathspec.txt` and `*.hold.txt` with exact and directory-prefix matching.
- [x] [Review][Patch] Deferred AC7 evidence uses a fragile stale line reference [_bmad-output/implementation-artifacts/deferred-work.md:5] - fixed by replacing the line-number reference with stable section/file references.
- [x] [Review][Patch] Deletion-report command parses non-`-z` porcelain output too loosely [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:98] - fixed by documenting a `git diff --name-status -z --diff-filter=D` based report.
- [x] [Review][Patch] Hold-list staging wording can be read as excluding the hold-list file itself [docs/audit/worktree-remediation/README.md:83] - fixed by clarifying that the hold-list file is documentation staged by the remediation aggregate, while its listed content is not staged.
- [x] [Review][Decision] AC7 remains deferred for historical `done` stories - resolved: historical `done` statuses are kept for accepted work, but each remaining uncommitted Sprint 19 lot now has an explicit temporary non-commit/release-block decision in `SPRINT19_WORKTREE_REMEDIATION_LOG.md`.
- [x] [Review][Patch] PR reference flow still marks `done` before the initial push/PR can exist [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:164] - fixed by documenting commit -> push/open PR -> document reference -> push docs/status -> mark `done`.
- [x] [Review][Patch] Post-staging guard only classifies `??` and unstaged `M` statuses [docs/audit/worktree-remediation/README.md:73] - fixed by requiring every non-clean porcelain entry and preserving full `XY` status.
- [x] [Review][Patch] Hold-overlap verification does not normalize Git pathspec directory semantics [docs/audit/worktree-remediation/review-verification.snapshot.txt:120] - fixed with normalized slash/case comparison and exact plus directory-prefix matching.
- [x] [Review][Patch] No verification proves every remaining visible status entry is classified [docs/audit/worktree-remediation/review-verification.snapshot.txt:120] - fixed by adding `NO_UNCLASSIFIED_STATUS_PATHS` verification against all story/remediation/hold pathspec files.
- [x] [Review][Patch] Sprint-status verification can pass on stale or commented text [docs/audit/worktree-remediation/review-verification.snapshot.txt:75] - fixed with an anchored unique-match check for `sprint19-worktree-remediation: review`.
- [x] [Review][Patch] Controlled deletion report example omits required decision fields [docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md:97] - fixed by emitting `Fichier | Raison | References | Decision` placeholders before any deletion/restoration action.
- [x] [Review][Patch] Status classification proof drops Git `XY` state and parses non-`-z` porcelain unsafely [docs/audit/worktree-remediation/review-verification.snapshot.txt:160] - fixed by parsing `git status --porcelain=v1 -z -uall`, preserving `XY`, and failing unsafe statuses before classification.
- [x] [Review][Patch] Status-aware diff example can miss staged-only, deleted, renamed, copied, or typechanged paths [docs/audit/worktree-remediation/README.md:56] - fixed with a full `XY` status-aware snippet and matching hygiene procedure updates.
- [x] [Review][Patch] Story status proof does not require a unique canonical `Status: review` line [docs/audit/worktree-remediation/review-verification.snapshot.txt:76] - fixed by requiring exactly one story status line, exactly one `Status: review`, and line 3.
- [x] [Review][Patch] Hold overlap proof only checks one directory-containment direction [docs/audit/worktree-remediation/review-verification.snapshot.txt:137] - fixed by checking exact match plus both directory-containment directions.

## Dev Notes

### Developer Context

Cette story doit corriger le probleme operationnel, pas seulement produire une analyse. L'audit source est:

- `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`

Le danger principal est de faire une grosse action globale qui melange ou detruit des changements utiles. Le bon chemin est de reduire le worktree par lots petits, explicites, et verifiables.

### Current Anchors

- `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`
  - contient la synthese par categorie, top-level folder, rattachement probable aux stories, audit des suppressions et procedure sans `git add -A`.
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
  - source des statuts Sprint 19 et des stories existantes.
- `_bmad-output/implementation-artifacts/sprint19-*.md`
  - stories sources a utiliser pour rattacher les fichiers applicatifs.
- `AGENTS.md`
  - impose la clarification avant action, suppression controlee et changements chirurgicaux.

### Architecture / Process Compliance

- Aucun refactor applicatif opportuniste.
- Aucune dependance externe.
- Pas de suppression recursive globale.
- Pas de `git clean`.
- Pas de `git reset --hard`.
- Pas de `git add -A`.
- Utiliser uniquement des pathspecs explicites.
- Toute suppression doit avoir un rapport: fichier/groupe, raison, references, decision.

### Implementation Guidance

Approche recommandee:

1. Generer des fichiers de travail non ambigus sous `docs/audit/worktree-remediation/`, par exemple:
   - `story-sprint19-feat-invitation-code-advanced.pathspec.txt`
   - `story-sprint19-suggest-epic-id-robustness.pathspec.txt`
   - `tooling-tracked-deletions.pathspec.txt`
   - `unknown-hold.pathspec.txt`
2. Ne pas stager automatiquement; commencer par produire les listes et les verifier.
3. Pour les groupes non suivis massifs, preferer une decision explicite avant action:
   - suppression locale si cache/generation sans valeur;
   - `.gitignore` repo si pattern partage par tous;
   - `.git/info/exclude` si exclusion strictement locale.
4. Pour les suppressions suivies, preferer la restauration si aucune decision tooling ne justifie la suppression.
5. Pour les commits applicatifs, stager par story et verifier:
   - `git diff --cached --name-status`;
   - `git diff --cached --check`;
   - tests cibles de la story.

### Testing Requirements

Tests/validations requis:

- `git status --porcelain=v1 -uall` avant/apres chaque lot.
- `git diff --check` sur chaque diff documentation/process.
- `git diff --cached --check` avant tout commit propose.
- Tests backend/frontend cibles seulement quand un lot applicatif est stage ou modifie.
- Aucun test artificiel pour les rapports/pathspecs.

### Pre-existing Gaps / Known Issues

- Le worktree contient deja beaucoup de changements applicatifs qui semblent appartenir a des stories `done`; ne pas les revert sans decision.
- `sprint-status.yaml` porte des modifications hors scope anterieures; ne pas les reformater ni les "corriger" opportunistement.
- `.agents` est utilise par l'environnement Codex local; ne pas le supprimer sans validation.
- `.m2` est tres probablement un cache Maven local dans le repo, mais la suppression reste une action de filesystem a valider.
- Les dossiers agents/IDE massifs peuvent etre issus d'une regeneration voulue; les exclure du commit applicatif ne suffit pas a decider leur avenir.

### References

- `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`
- `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`
- `_bmad-output/implementation-artifacts/deferred-work.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/project-context.md`
- `AGENTS.md`

## Dev Agent Record

### Debug Log References

- 2026-05-07: Story creee apres clarification utilisateur: l'objectif attendu est la correction concrete du worktree, pas seulement l'audit.
- 2026-05-07: Story passee en `in-progress` pour demarrer le premier lot non destructif.
- 2026-05-07: Pathspecs generes depuis `git status --porcelain=v1 -uall`, sans staging, suppression ni restauration.
- 2026-05-07: Exclusion ciblee des caches/generations agents appliquee dans `.gitignore`.
- 2026-05-07: Les 301 suppressions suivies d'outillage ont ete restaurees depuis `HEAD`.
- 2026-05-07: Faux statuts `M` d'outillage resolus par refresh d'index; aucun diff staged restant.

### Completion Notes List

- Task 1 completee: les changements sont maintenant decoupes en pathspecs par story probable, artefacts BMAD, outillage suivi, outillage genere et fichiers inconnus en `hold`.
- Les fichiers multi-stories sont volontairement isoles dans `shared-touchpoints-needing-hunk-review.pathspec.txt` pour eviter un staging aveugle.
- Les caches et fichiers user-scoped (`.m2`, backups `_bmad/**/*.bak`, configs `*.user.toml`) sont exclus via `.gitignore`; les dossiers agents/IDE generes restent exclus localement via `.git/info/exclude` pour ne pas masquer du tooling projet versionnable.
- Les suppressions suivies d'outillage ont ete restaurees depuis `HEAD`, decision la plus fiable pour eviter un commit tooling massif non valide.
- Les pathspecs ont ete regeneres apres remediation; `tooling-tracked-deletions.pathspec.txt` et `tooling-tracked-modified.pathspec.txt` sont maintenant vides.
- Le worktree visible est passe d'environ 31k+ entrees a 191 entrees intentionnelles apres follow-up de review: changements applicatifs par story, artefacts BMAD, docs de remediation, 6 fichiers `_bmad` tooling en hold et fichiers racine a confirmer.
- Reference Git du lot remediation: `772e5da` (`docs(bmad): document sprint19 worktree remediation`).
- La story est passee en `done` apres commit du lot remediation et documentation de la reference Git.
- Rapport final cree: `docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md`.

### File List

- `docs/audit/worktree-remediation/README.md`
- `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`
- `docs/audit/worktree-remediation/application-unknown-hold.pathspec.txt`
- `docs/audit/worktree-remediation/bmad-artifacts.pathspec.txt`
- `docs/audit/worktree-remediation/bmad-artifacts-historical-hold.pathspec.txt`
- `docs/audit/worktree-remediation/bmad-artifacts-modified-hold.pathspec.txt`
- `docs/audit/worktree-remediation/local-exclude-patterns.snapshot.txt`
- `docs/audit/worktree-remediation/cache-local-ignored.pathspec.txt`
- `docs/audit/worktree-remediation/remediation-commit.pathspec.txt`
- `docs/audit/worktree-remediation/remediation-docs.pathspec.txt`
- `docs/audit/worktree-remediation/review-verification.snapshot.txt`
- `docs/audit/worktree-remediation/root-agent-docs.pathspec.txt`
- `docs/audit/worktree-remediation/shared-touchpoints-needing-hunk-review.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-contract-create-game-region-rules.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-draft-guards.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-feat-invitation-code-advanced.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-fix-catalogue-demo-banner.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-fix-leaderboard-regression.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-migrate-canonical-game-participants.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-suggest-epic-id-robustness.pathspec.txt`
- `docs/audit/worktree-remediation/story-sprint19-ux-draft-waiting-screen.pathspec.txt`
- `docs/audit/worktree-remediation/tooling-tracked-deletions.pathspec.txt`
- `docs/audit/worktree-remediation/tooling-tracked-modified.pathspec.txt`
- `docs/audit/worktree-remediation/tooling-tracked-modified-restore.pathspec.txt`
- `docs/audit/worktree-remediation/tooling-index-refresh.pathspec.txt`
- `docs/audit/worktree-remediation/tooling-untracked-generated-directories.hold.txt`
- `docs/audit/worktree-remediation/unknown-root-hold.pathspec.txt`
- `docs/audit/worktree-remediation/bmad-tooling-untracked-hold.pathspec.txt`
- `docs/audit/SPRINT19_WORKTREE_REMEDIATION_LOG.md`
- `.gitignore`
- `_bmad-output/implementation-artifacts/deferred-work.md`
- `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`
- `_bmad-output/implementation-artifacts/sprint19-worktree-remediation.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-05-07: Story creee pour passer de l'audit worktree a la correction concrete par lots.
- 2026-05-07: Story demarree en `in-progress`; Task 1 completee avec generation des pathspecs non destructifs.
- 2026-05-07: Remediation appliquee: ignores cibles, restauration des suppressions suivies, refresh index, regeneration des pathspecs et rapport final; story passee en `review`.
- 2026-05-07: Code review remediation appliquee: exclusions repo reduites aux caches/config user, exclusions agents/IDE deplacees en `.git/info/exclude`, sprint-status historique remis hors diff, rapport renforce, validations non suivies executees.
- 2026-05-08: Follow-up code review applique: story remise en `review`, sprint status synchronise, pathspecs BMAD separes, commandes de diff/staging renforcees; re-review BMAD requise avant `done`.
- 2026-05-08: Re-review appliquee: preuves versionnees, pathspec de commit unique, BMAD hold corrige, cache `.m2` explicite, compteurs/validation alignes; story synchronisee en `done`.
- 2026-05-09: Follow-up hygiene/remediation applique: `deferred-work.md` classe dans le lot BMAD, compteurs visibles actualises a 191, pathspec de commit porte a 34 entrees.
- 2026-05-09: Code review follow-up applique: story et sprint-status remis en `review`, controles untracked/hold renforces, procedure commit/reference Git clarifiee.
- 2026-05-09: Code review remediation appliquee: decisions AC7 par lot ajoutees, procedure PR/push corrigee, controles status/hold/classification renforces; story conservee en `review` jusqu'a reference Git/PR.
- 2026-05-09: Code review patches appliquees: parsing status `-z` avec conservation `XY`, diff status-aware renforce, preuve `Status: review` canonique et overlap hold bidirectionnel.
- 2026-05-10: Lot remediation commite (`772e5da`), reference Git documentee, story et sprint-status passes en `done`.
