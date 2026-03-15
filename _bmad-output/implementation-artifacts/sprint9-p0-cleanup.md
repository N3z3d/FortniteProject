# Story sprint9-p0-cleanup: Process Cleanup — D3 checklist, D4 deferred-future zombies, S2 SEC-R2 doc

Status: done

<!-- METADATA
  story_key: sprint9-p0-cleanup
  branch: story/sprint9-p0-cleanup
  sprint: Sprint 9
  Note: Validation is optional. Run validate-create-story for quality check before dev-story.
-->

## Story

As a developer agent working on FortniteProject,
I want to close 3 process debts identified in the Sprint 8 retrospective (D3/D4/S2),
so that the checklist, sprint-status.yaml, and project-context.md accurately reflect the current state of the project and prevent future confusion.

## Acceptance Criteria

1. **D3 — checklist.md sprint parent criterion** : Un nouveau critère `Sprint-N Parent Status` est ajouté dans la section `## 🔚 Final Status Verification` du fichier `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md`. Le critère stipule que `sprint-N: status` dans `sprint-status.yaml` doit être mis à `done` comme **dernière action du sprint** après que toutes les stories sont `done` et **avant** la rétrospective.
2. **D4 — zombies deferred-future résolus** : Les 5 items `deferred-future` liés à l'hébergement/staging/secrets/DB/CI-CD dans `sprint-status.yaml` sont mis à `wont-do` avec un commentaire explicatif. L'item `sprint4-sec-r2-websocket-auth: deferred-future` est mis à `done` (implémenté en Sprint 8 par `sprint8-sec-r2-websocket-auth`).
3. **S2 — §WebSocket Security Pattern documenté** : La section `## §WebSocket Security Pattern` est ajoutée dans `_bmad-output/project-context.md`. Elle documente : (a) la contrainte SockJS (pas de headers HTTP), (b) le pattern query param `?token=<jwt>`, (c) le comportement actuel (`IllegalStateException → STOMP ERROR` côté client, pas HTTP 401 propre), (d) le comportement frontend attendu quand rejeté.
4. 0 régression : aucun code Java/TypeScript modifié → les 2330 tests backend et 2243 frontend restent inchangés.
5. Les fichiers modifiés ne cassent aucune structure YAML/Markdown existante.

## Tasks / Subtasks

- [x] Task 1: D3 — Ajouter critère `Sprint-N Parent Status` dans checklist.md (AC: #1)
  - [x] 1.1: Lire `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md` in full (le linter peut avoir modifié les fins de ligne)
  - [x] 1.2: Repérer la section `## 🔚 Final Status Verification`, après la ligne `- [ ] **Sprint Status Updated:**`
  - [x] 1.3: Insérer le nouveau critère D3 (voir Dev Notes §Formulation exacte D3)
  - [x] 1.4: Vérifier structure Markdown préservée (indentation, emojis, listes)

- [x] Task 2: D4 — Résoudre les zombies `deferred-future` dans sprint-status.yaml (AC: #2)
  - [x] 2.1: Lire `_bmad-output/implementation-artifacts/sprint-status.yaml` en entier
  - [x] 2.2: Mettre à jour les 5 items `deferred-future` hébergement → `wont-do` (voir Dev Notes §Items à résoudre)
  - [x] 2.3: Mettre `sprint4-sec-r2-websocket-auth: done` (superseded by sprint8-sec-r2-websocket-auth)
  - [x] 2.4: Vérifier que la structure YAML est préservée (commentaires et indentation intacts)

- [x] Task 3: S2 — Documenter §WebSocket Security Pattern dans project-context.md (AC: #3)
  - [x] 3.1: Lire `_bmad-output/project-context.md` en entier
  - [x] 3.2: Identifier l'emplacement optimal pour la nouvelle section (après §Pièges Connus ou après la stack technique)
  - [x] 3.3: Ajouter la section `§WebSocket Security Pattern` (voir Dev Notes §Contenu S2)

- [x] Task 4: Validation finale (AC: #4, #5)
  - [x] 4.1: Confirmer qu'aucun fichier Java/TypeScript n'a été modifié → 0 test à relancer
  - [x] 4.2: Vérifier syntaxe YAML du sprint-status.yaml modifié (pas d'erreur d'indentation)
  - [x] 4.3: Vérifier rendu Markdown du checklist.md modifié (sections cohérentes)

## Dev Notes

### Contexte — 3 items d'action de la retro Sprint 8

Cette story résout exactement les items D3, D4 et S2 listés dans `sprint-8-retro-2026-03-15.md §7 Action Items Sprint 9` :

| Item | Priorité | Action |
|---|---|---|
| D3 | P3 | Ajouter critère sprint-status parent dans checklist.md §Final Status Verification |
| D4 | P3 | Résoudre les zombies deferred-future : hébergement/secrets/DB/staging/CI-CD → wont-do |
| S2 | P2 | Documenter décision SEC-R2 rejet HTTP (exception vs 401) dans project-context.md |

### D3 — Formulation exacte du critère à ajouter

**Emplacement** : `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md`, section `## 🔚 Final Status Verification`, après la ligne `- [ ] **Sprint Status Updated:** Sprint status updated to "review" (when sprint tracking is used)`.

**Texte exact à insérer** :
```markdown
- [ ] **Sprint-N Parent Status :** Si c'est la dernière story du sprint, vérifier que `sprint-N: status` dans `sprint-status.yaml` est mis à `done` après que toutes les stories sont `done` et **avant** la rétrospective. Un sprint 100% livré avec `status: in-progress` = oubli de mise à jour (cf. Sprint 8 retro L3).
```

**Référence** : sprint-8-retro-2026-03-15.md §L3 — "Le sprint-status.yaml `status` parent doit être la *dernière* action d'un sprint".

### D4 — Items à résoudre dans sprint-status.yaml

Ces 6 items ont le statut `deferred-future` sans date de révision. La décision a été prise le 2026-03-08 en retro Sprint 4 : **hébergement = report long terme, Docker local = cible principale**.

| Clé YAML | Action | Raison |
|---|---|---|
| `sprint4-decision-hebergement` | `wont-do` | Décision prise 2026-03-08 : Docker local = cible opérationnelle. Staging public hors scope. |
| `sprint4-config-secrets-prod` | `wont-do` | Dépend de la décision hébergement → wont-do par cascade. |
| `sprint4-db-prod-provisioning` | `wont-do` | Dépend de la décision hébergement → wont-do par cascade. |
| `sprint4-a10-staging-deployment` | `wont-do` | Dépend de la décision hébergement → wont-do par cascade. |
| `sprint4-a3b-cicd-pipeline-complet` | `wont-do` | CI tests déjà opérationnel via A3a (ci.yml existant). Deploy staging hors scope. |
| `sprint4-sec-r2-websocket-auth` | `done` | ✅ Implémenté en Sprint 8 (`sprint8-sec-r2-websocket-auth`). JWT handshake + `JwtHandshakeInterceptor`. |

**Format YAML de mise à jour** — remplacer `deferred-future` par `wont-do` avec commentaire :
```yaml
sprint4-decision-hebergement: wont-do  # 2026-03-08 retro Sprint4: Docker local = cible principale. Staging public hors scope actuel.
sprint4-config-secrets-prod: wont-do  # cascade: dépend décision hébergement (wont-do ci-dessus)
sprint4-db-prod-provisioning: wont-do  # cascade: dépend décision hébergement (wont-do ci-dessus)
sprint4-a10-staging-deployment: wont-do  # cascade: dépend décision hébergement (wont-do ci-dessus)
sprint4-a3b-cicd-pipeline-complet: wont-do  # CI tests opérationnel via A3a. Deploy staging hors scope.
sprint4-sec-r2-websocket-auth: done  # ✅ Implémenté Sprint 8 (JwtHandshakeInterceptor + query param token)
```

**Note critique** : NE PAS utiliser l'outil Write complet sur sprint-status.yaml — utiliser Edit pour chaque ligne ciblée. Le fichier est long (~650 lignes) et contient des commentaires importants à préserver.

### S2 — Contenu de la section §WebSocket Security Pattern

**Emplacement dans project-context.md** : À ajouter dans une section appropriée (chercher `## §` ou `## Pièges` ou fin du fichier).

**Contenu exact** :

```markdown
## §WebSocket Security Pattern (Sprint 8 — SEC-R2)

### Contexte

SockJS ne supporte pas les **headers HTTP personnalisés** au handshake. L'approche standard `Authorization: Bearer <token>` est impossible. La seule solution viable est le **query param** au moment de la création de la connexion SockJS.

### Pattern de référence (implémenté Sprint 8)

**Backend — `JwtHandshakeInterceptor`** :
```java
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ...) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtUtil.validateToken(token)) {
                attributes.put("username", jwtUtil.extractUsername(token));
                return true;
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }
}
```

**Frontend — `WebSocketService`** :
```typescript
const token = this.authService.getToken();
const sockjs = new SockJS(`${environment.wsUrl}?token=${encodeURIComponent(token ?? '')}`);
```

### Comportement actuel du chemin de rejet

⚠️ **Décision documentée** : Le rejet d'une connexion sans JWT valide passe par `IllegalStateException → STOMP ERROR frame` côté client, et **non** un HTTP 401 propre au handshake.

- **Pourquoi** : `JwtHandshakeInterceptor.beforeHandshake()` retourne `false` + `response.setStatusCode(UNAUTHORIZED)`, mais SockJS intercepte et encapsule la réponse, ce qui entraîne un STOMP ERROR au lieu d'un HTTP 401 visible.
- **Impact** : Le message d'erreur côté frontend (`WebSocketService`) n'est pas explicite — l'erreur de connexion doit être traitée dans le callback `onStompError`.
- **Décision** : Comportement conservé tel quel (fonctionnel). Une correction HTTP 401 propre est possible mais non prioritaire (sprint4-a10 wont-do).

### Pour les futures stories WebSocket

Lors de la création d'une story de sécurité WebSocket, définir explicitement dans les ACs :
1. Le code de rejet (HTTP au handshake OU STOMP ERROR frame)
2. Le message côté client et son handling dans `WebSocketService`
3. Si `onStompError` doit déclencher une déconnexion / notification utilisateur
```

### Règles critiques

- **Lire avant d'éditer** : Le linter Windows peut avoir modifié les fins de ligne → toujours relire avant Edit
- **Utiliser Edit (pas Write)** sur les fichiers longs : checklist.md (~85 lignes) et sprint-status.yaml (~650 lignes)
- **0 test à créer** : Aucun code applicatif modifié — ce sont des fichiers de processus/doc
- **Préserver la structure** : YAML doit rester valide, Markdown doit rester cohérent

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend: ~15 failures pre-existing (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, PlayerServiceTddTest 1, ScoreServiceTddTest 3, GameStatisticsServiceTddTest 1 error) — non impactés par cette story
- [KNOWN] Frontend: 2243/2243 passing — non impacté
- [NONE] Cette story ne modifie aucun code applicatif — 0 risque de régression

### Project Structure Notes

```
_bmad/bmm/workflows/4-implementation/dev-story/
└── checklist.md                  ← MODIFIÉ (D3: ajout critère Sprint-N Parent Status)

_bmad-output/implementation-artifacts/
└── sprint-status.yaml            ← MODIFIÉ (D4: 5 wont-do + 1 done)

_bmad-output/
└── project-context.md            ← MODIFIÉ (S2: ajout §WebSocket Security Pattern)
```

- Aucun fichier Java, TypeScript, SQL, ou SCSS
- Aucune migration Flyway
- Aucun test à créer ou modifier
- Aucun i18n

### References

- [Source: _bmad-output/implementation-artifacts/sprint-8-retro-2026-03-15.md §7 — Action Items D3/D4/S2]
- [Source: _bmad-output/implementation-artifacts/sprint8-c1c2-dod-process.md — pattern modifier checklist.md]
- [Source: _bmad/bmm/workflows/4-implementation/dev-story/checklist.md — fichier cible D3]
- [Source: _bmad-output/implementation-artifacts/sprint-status.yaml sprint-4 section — items deferred-future à résoudre]
- [Source: _bmad-output/implementation-artifacts/sprint8-sec-r2-websocket-auth.md — comportement implémenté SEC-R2]
- [Source: sprint-8-retro-2026-03-15.md §L2/L3/L4 — leçons apprises]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- **D3 (checklist.md)**: Critère `Sprint-N Parent Status` ajouté dans `## 🔚 Final Status Verification` après la ligne `Sprint Status Updated`. Formulation exacte conforme au Dev Notes. Structure Markdown préservée.
- **D4 (sprint-status.yaml)**: 6 edits ciblés — 5 items `deferred-future` hébergement/staging/secrets/DB/CI-CD → `wont-do` avec commentaire; `sprint4-sec-r2-websocket-auth: deferred-future` → `done` (implémenté Sprint 8). Aucun autre contenu du fichier modifié.
- **S2 (project-context.md)**: Section `## §WebSocket Security Pattern (Sprint 8 — SEC-R2)` ajoutée avant `## Usage Guidelines`. Contient: contexte SockJS (pas de headers), pattern query param + code Java/TypeScript, comportement de rejet (STOMP ERROR vs HTTP 401), et guidance pour futures stories WebSocket.
- **0 fichier Java/TypeScript modifié** — 0 régression possible, 0 test à relancer.

### File List

- `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md` — modifié (D3: Sprint-N Parent Status)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — modifié (D4: 5 wont-do + sprint4-sec-r2 done)
- `_bmad-output/project-context.md` — modifié (S2: §WebSocket Security Pattern)
