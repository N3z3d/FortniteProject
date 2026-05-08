# Sprint 19 - Git Worktree Hygiene Audit

Date: 2026-05-07
Story: `sprint19-git-worktree-hygiene`
Scope: audit only. No cleanup, no staging, no commit, no deletion.

## Commandes source

```powershell
git status --porcelain=v1 -uall
git status --porcelain=v1 -uall -- src frontend e2e pom.xml Dockerfile docker-compose.local.yml package.json frontend/package.json angular.json frontend/angular.json tsconfig.json frontend/tsconfig.json
git status --porcelain=v1 -uall -- _bmad-output AGENTS.md CLAUDE.md

# Aggregation top-level utilisee pour verifier les compteurs:
git status --porcelain=v1 -uall |
  ForEach-Object {
    $status = $_.Substring(0, 2)
    $path = $_.Substring(3).Trim('"')
    $topLevel = if ($path.Contains('/')) { $path.Split('/')[0] } else { $path }
    [pscustomobject]@{ Status = $status; TopLevel = $topLevel }
  } |
  Group-Object TopLevel, Status |
  Sort-Object Name
```

## Synthese par categorie

| Categorie | Statut | Nombre | Decision |
|---|---:|---:|---|
| Code applicatif et tests | M | 109 | A rattacher a une story avant commit |
| Code applicatif et tests | ?? | 20 | A rattacher a une story avant commit |
| Artefacts BMAD | M | 3 | Commit separable `docs(bmad)` apres verification |
| Artefacts BMAD | ?? | 38 | A confirmer avant commit |
| Outillage agents/IDE | D | 301 | Ne pas committer sans decision explicite |
| Outillage agents/IDE | M | 11 | Audit dedie requis avant commit |
| Outillage agents/IDE | ?? | ~31665 | Ne pas committer en lot applicatif; total massif issu de dossiers generes/caches, a verifier par pathspec avant action |
| Docs agents racine | M | 2 | Commit docs separe si valide |
| Docs agents racine | ?? | 4 | A confirmer |
| Fichiers inconnus/a confirmer | ?? | 6 | Ne pas committer avant decision explicite |

## Synthese par top-level folder

| Top-level | Statuts observes | Nombre | Categorie |
|---|---:|---:|---|
| `.m2` | ?? | 3372 | cache/depot local Maven, hors commit |
| `.claude` | D/?? | 41 / 1433 | outillage agent |
| `.cursor` | D/?? | 41 / 293 | outillage IDE |
| `.codex-home` | D | 45 | outillage agent |
| `.agents` | ?? | 1400 | outillage agent genere |
| `.adal`, `.agent`, `.bob`, `.cline`, `.codebuddy`, `.cortex`, `.factory`, `.firebender`, `.iflow`, `.junie`, `.kiro`, `.kode`, `.neovate`, `.ona`, `.qoder`, `.qwen`, `.trae`, `.zencoder` | ?? | inclus dans le total massif ~31665 | outillage agents/IDE genere; ne pas utiliser cette ligne comme preuve de suppression |
| `_bmad` | D/M/?? | 174 / 11 / 144 | outillage BMAD regenere |
| `_bmad-output` | M/?? | 3 / 38 | artefacts BMAD |
| `src` | M/?? | 60 / 12 | backend applicatif/tests |
| `frontend` | M/?? | 48 / 8 | frontend applicatif/tests |
| `docker-compose.local.yml` | M | 1 | configuration locale, a rattacher |
| `AGENTS.md`, `CLAUDE.md` | M | 2 | docs agents racine |
| `AGENTS_old-*`, `CLAUDE_old-*`, `code scraping.txt` | ?? | 5 | fichiers inconnus/a confirmer |
| `src/**/V49__add_turn_started_at_to_draft_region_cursors.sql` | ?? | 1 | migration applicative a confirmer |

> Note de lecture: ce rapport est un audit d'hygiene, pas un pathspec commit-ready. Les nombres massifs d'outillage/caches servent a bloquer le staging aveugle, pas a autoriser une suppression ou un commit. Les lots executables doivent etre produits dans une story de remediation separee.

## Fichiers applicatifs restants par story probable

| Story probable | Indices de rattachement | Decision |
|---|---|---|
| `sprint19-feat-invitation-code-advanced` | `GameInvitationCodeRequestHandler`, `GameRepositoryInvitationCodeLockTest`, `GameController`, `GameService`, surfaces `game-detail`, `join-game`, `main-layout`, i18n invitation-code | Commit applicatif dedie invitation-code uniquement apres diff cible |
| `sprint19-contract-create-game-region-rules` | `GameCreationUseCase`, tests create/configure game, payloads create-game, `PlayerRegion` | Commit dedie create-game/regionRules; confirmer les tests qui ne sont que des fixtures auth |
| `sprint19-migrate-canonical-game-participants` | `Game`, `GameDtoMapper`, `GameDraftService`, `GameDraftLookupService`, `GameDraftReadinessService`, tests game/draft/readiness | Commit dedie invariant participants/draft readiness; ne pas melanger avec invitation-code |
| `sprint19-suggest-epic-id-robustness` | `PlayerResolutionResult`, `ResolutionPort`, `FortniteApiPort`, adapters Fortnite API/resolution, `PlayerIdentityPipelineService`, admin pipeline frontend, `pipeline-suggest.spec.ts` | Commit dedie pipeline/suggest; conserver le controle `rg resolveFortniteId` |
| `sprint19-fix-catalogue-demo-banner` | `CatalogueDataStatusDto`, `PlayerCatalogueReadinessService`, `PlayerCatalogueService`, `PlayerCataloguePageComponent`, `ScrapeLogService`, tests catalogue | Commit dedie catalogue demo/readiness |
| `sprint19-ux-draft-waiting-screen` | dossier `draft-waiting-page`, `websocket.service`, `game-home`, `game-detail`, i18n `draft.waiting` | Commit dedie waiting screen/WS redirect |
| `sprint19-fix-draft-button-guard` ou follow-up draft | `draft-status.guard`, `draft-audit.guard`, `simultaneous-mode.guard`, routes draft, tests guards | Commit dedie guards draft; confirmer le perimetre exact |
| `sprint19-fix-leaderboard-regression` | `simple-leaderboard.component.html/scss` | Commit dedie leaderboard si diff encore pertinent |
| `sprint19-fix-resolution-adapter-config` / `sprint19-fix-fortnite-api-key-config` | `ResolutionAdapterConfiguration`, `docker-compose.local.yml`, tests adapter config | Commit configuration pipeline separe; ne pas inclure outillage |
| A confirmer | `V49__add_turn_started_at_to_draft_region_cursors.sql`, `code scraping.txt`, fichiers racine `AGENTS_old-*` / `CLAUDE_old-*` | Ne pas committer avant decision explicite |

Le rattachement ci-dessus est volontairement au niveau story probable. Il ne remplace pas un inventaire fichier par fichier ni des pathspecs de staging. Avant tout commit applicatif, produire ou verifier un pathspec explicite par story, puis comparer chaque fichier a la File List de la story concernee avec une commande de diff consciente du statut Git: `git diff -- <path>` pour les fichiers suivis, `git diff --no-index -- NUL <path>` pour les fichiers `??`.

## Audit suppressions et regenerations d'outillage

| Fichier ou groupe | Raison observee | References | Decision |
|---|---|---|---|
| `.claude/commands/bmad-*.md` | Anciennes commandes supprimees et nouvelles commandes non prefixees generees | `git status --porcelain=v1 -uall`, 41 D + 1433 ?? | Ne pas committer sans validation tooling |
| `.cursor/commands/bmad-*.md` | Meme regeneration que `.claude` cote Cursor | `git status --porcelain=v1 -uall`, 41 D + 293 ?? | Ne pas committer sans validation tooling |
| `.codex-home/prompts/bmad-*.md` et `.codex-home/tmp/*` | Ancien home Codex supprime, contient aussi fichiers temporaires | `git status --porcelain=v1 -uall`, 45 D | Ne pas committer; exclure les temporaires |
| `_bmad/bmm/workflows/**`, `_bmad/bmm/data/**`, `_bmad/bmm/teams/**` | Ancienne arborescence BMAD supprimee ou remplacee | `git status --porcelain=v1 -uall`, 174 D | Rapport tooling dedie avant toute suppression committee |
| `_bmad/**.bak`, `_bmad/bmb`, `_bmad/cis`, `_bmad/gds`, `_bmad/tea`, `_bmad/scripts` | Regeneration/module BMAD non suivie | `git status --porcelain=v1 -uall`, 144 ?? | Commit `chore(tooling)` seulement apres decision |
| `.agents`, `.adal`, `.agent`, `.bob`, `.cline`, `.codebuddy`, `.cortex`, `.factory`, `.firebender`, `.iflow`, `.junie`, `.kiro`, `.kode`, `.neovate`, `.ona`, `.qoder`, `.qwen`, `.trae`, `.zencoder` | Dossiers agents/IDE generes massivement | `git status --porcelain=v1 -uall`, 31665 ?? | Hors commit applicatif; confirmer `.gitignore`/strategie |
| `.m2` | Cache Maven local dans le repo | `git status --porcelain=v1 -uall`, 3372 ?? | Ne jamais committer; confirmer ignore/exclusion |

Ce tableau est un rapport de risque par groupe. Il ne valide aucune suppression suivie. Pour appliquer une decision de suppression/restauration, produire un rapport dedie au format `fichier | raison | references | decision`, par exemple avec:

```powershell
git status --porcelain=v1 -uall |
  Where-Object { $_ -match '^ D ' } |
  ForEach-Object { $_.Substring(3) }
```

## Lots proposes

1. `docs(bmad): audit sprint19 git worktree hygiene`
   - `docs/audit/SPRINT19_GIT_WORKTREE_HYGIENE.md`
   - `_bmad-output/implementation-artifacts/sprint19-git-worktree-hygiene.md`
   - `_bmad-output/implementation-artifacts/sprint-status.yaml`

2. Lots applicatifs par story existante
   - Stager uniquement les fichiers listés dans la story concernee.
   - Ajouter le hash de commit dans la story avant de passer `done`.
   - Ne jamais inclure `.claude`, `.cursor`, `.codex-home`, `.agents`, `_bmad` regenere dans ces lots.

3. Lot tooling separe, seulement apres decision
   - Type recommande: `chore(tooling): regenerate bmad agent commands`
   - Precondition: rapport de suppression valide et liste explicite des suppressions acceptees.

4. Fichiers a confirmer
   - `code scraping.txt`
   - `AGENTS_old-20260210.md`, `AGENTS_old-20260410.md`
   - `CLAUDE_old-20260210.md`, `CLAUDE_old-20260410.md`
   - `V49__add_turn_started_at_to_draft_region_cursors.sql`

## Procedure BMAD sans `git add -A`

1. Creer une branche par story: `git switch -c story/<story-key>` ou `git switch -c fix/<bug-key>`.
2. Verifier l'etat complet: `git status --porcelain=v1 -uall`.
3. Lire la story et sa `File List`; si un fichier n'y figure pas, ne pas le stager sans justification.
4. Revoir le diff avant staging avec une commande status-aware:
   - fichier suivi: `git diff -- <path>`;
   - fichier non suivi: `git diff --no-index -- NUL <path>`.
5. Stager par pathspec explicite. Pour un fichier unique: `git add -- <path>`. Pour un fichier pathspec contenant des chemins avec espaces: `git add --pathspec-from-file=<pathspec-file>`.
6. Verifier le staged set: `git diff --cached --name-status` puis `git diff --cached --stat`.
7. Verifier les espaces: `git diff --cached --check`.
8. Executer les validations minimales de la story:
   - backend: `mvn spotless:apply --no-transfer-progress` comme etape de formatage potentiellement mutante, puis `git diff --check` et tests cibles;
   - frontend: `npm run test:vitest -- <specs cibles>` depuis `frontend/`;
   - E2E seulement si la story touche un flux critique ou une spec Playwright.
9. Committer avec le type et le scope de la story: `fix(draft): ...`, `feat(game): ...`, `docs(bmad): ...`, `chore(tooling): ...`.
10. Pousser la branche: `git push -u origin HEAD`.
11. Mettre a jour la story avec le hash ou la reference PR/commit.

## Regle sprint-status

Une story ne passe pas en `done` dans `sprint-status.yaml` sans:

- commit hash, PR, ou reference Git equivalente dans la story;
- code review BMAD terminee si la story touche du code;
- aucun finding HIGH/MEDIUM ouvert;
- File List complete.

Avant ces conditions, les statuts autorises sont `in-progress` ou `review`.

Politique de transition Sprint 19: les stories deja marquees `done` avant ce rapport sont conservees comme etat d'acceptation fonctionnelle historique, mais elles restent bloquees pour release/push tant que leur lot Git n'est pas isole, revu et reference. Toute nouvelle transition vers `done` apres ce rapport doit respecter la regle ci-dessus.
