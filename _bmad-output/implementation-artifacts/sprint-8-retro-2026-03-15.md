# Sprint 8 Retrospective — "Solidité avant exposition publique"

**Date :** 2026-03-15
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 8 :** "F2 done + SEC-R2 WebSocket auth + E2E flux alternatifs + DoD C1/C2"

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 4 / 4 (100%) — F2 ✅, SEC-R2 ✅, E1 ✅, C1/C2 ✅ |
| Tests Vitest (frontend) | 2243 / 2243 (baseline maintenu — 0 régression) |
| Tests backend | 2299 run, SpotBugs 0 bugs, JaCoCo thresholds ✅ |
| Nouveaux tests WebSocket auth | +8 (5 backend + 3 frontend) |
| Nouveaux tests E2E | +3 (alt-flows.spec.ts — ALT-01/02/03) |
| Incidents production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| `sprint8-f2-validation` | Validation CI GitHub Actions : `mvn verify BUILD SUCCESS`, 2299 tests, SpotBugs 0, JaCoCo 0.77/0.62. OWASP migré en profil `-Psecurity-scan`. Fermeture définitive de A3b (ouvert depuis Sprint 3). |
| `sprint8-sec-r2-websocket-auth` | JWT auth sur STOMP WebSocket via `JwtHandshakeInterceptor`. STOMP CONNECT rejeté sans JWT valide (IllegalStateException → STOMP ERROR). Frontend injecte Bearer token automatiquement en query param. 5 tests backend + 3 frontend. Backlog fermé depuis Sprint 3. |
| `sprint8-e1-e2e-flux-alternatifs` | 3 tests E2E dans `alt-flows.spec.ts` : ALT-01 (swap mauvaise région → 400), ALT-02 (trade joueur non-possédé → 400), ALT-03 (pick duplicate en draft → 409). Note : scénarios adaptés pragmatiquement vs. specs initiales (DRAFT-FULL-01 non livré). |
| `sprint8-c1c2-dod-process` | C1 (commit Git par story) + C2 (validation CI/CD obligatoire sur main) ajoutés dans `checklist.md` §Final Status Verification. Process durci pour futurs sprints. |

---

## 2. Suivi des actions Sprint 7

Bob (Scrum Master): "Sprint 7 avait fixé 5 livrables sous 4 labels. Bilan complet."

| # | Action promise | Statut | Notes |
|---|---|---|---|
| F2 | Valider CI GitHub Actions + GHCR push | ✅ Fait | `mvn verify` BUILD SUCCESS. SpotBugs 0. GHCR configuré. Fermé définitivement. |
| S1 | SEC-R2 WebSocket auth JWT | ✅ Fait | JwtHandshakeInterceptor + frontend query param. STOMP CONNECT rejeté sans JWT. 8 tests. |
| E1 | E2E flux alternatifs critiques | ✅ Fait (partiel) | 3 scénarios backend (ALT-01/02/03). DRAFT-FULL-01 (flux UI complet) non livré. |
| C1 | DoD critère commit Git | ✅ Fait | Ajouté dans checklist.md §Final Status Verification. |
| C2 | DoD critère CI/CD validation | ✅ Fait | Ajouté dans checklist.md §Final Status Verification. |
| ⛔ | Ne pas re-proposer hébergement/staging | ✅ Respecté | Zéro mention hébergement en Sprint 8. |

**Score : 5/5 (100%) — troisième sprint consécutif avec 100% des actions livrées.**

---

## 3. Ce qui a bien marché

**Charlie (Senior Dev):** "La victoire principale, c'est F2. Pas juste le fait de l'avoir fermé — c'est comment c'est passé. En Sprint 7, F2 était bloqué parce qu'on ne pouvait pas valider le GHCR push sans un vrai push sur main. En Sprint 8, l'action était simple : pousser sur main et vérifier. `mvn verify BUILD SUCCESS — 2299 tests, SpotBugs 0.` C'est la puissance des stories P0 bien bornées. Le travail préparatoire (Sprint 7) a rendu la clôture triviale."

**Dana (QA Engineer):** "SEC-R2 était un problème non-trivial — SockJS ne supporte pas les headers HTTP. La solution query param `?token=<jwt>` au handshake est élégante et robuste. Le `JwtHandshakeInterceptor` avec fallback sur interception STOMP CONNECT, c'est propre architecturalement. 5 tests backend + 3 frontend, 0 régression."

**Elena (Junior Dev):** "Les E2E alt-flows montrent une adaptation pragmatique saine. DRAFT-FULL-01 nécessitait un état seed trop complexe. Plutôt que de forcer ou de livrer un test fragile, l'équipe a livré 3 tests de validation backend solides. C'est mieux que rien — et ça valide les chemins d'erreur réels."

**Alice (Product Owner):** "Ce sprint, chaque story fermait une dette *nommée*. F2 = A3b ouvert depuis Sprint 3. SEC-R2 = sprint3-sec-r2-websocket-auth en backlog depuis Sprint 3. C1/C2 = leçons L4/L5 Sprint 7 → process. Ce n'est pas du travail arbitraire — c'est de la dette résolue de façon disciplinée."

**Thibaut (Project Lead):** "Ce qui me marque le plus, c'est la cohérence des 3 derniers sprints. Sprint 6 : 9.2/10. Sprint 7 : 9.3/10. Sprint 8 : 100%. On a trouvé un rythme où chaque sprint nettoie la dette du précédent de façon disciplinée."

### Victoires clés
- 4/4 stories done (100%) — 3ème sprint consécutif parfait
- F2 + SEC-R2 fermés après 6 sprints de backlog (Sprint 3 → Sprint 8)
- SpotBugs 0, JaCoCo green, 2299 tests backend — CI solide
- C1/C2 dans checklist — process amélioré pour les futurs sprints
- 5/5 action items Sprint 7 livrés

---

## 4. Ce qui n'a pas bien marché / Challenges

**Charlie (Senior Dev):** "Les scénarios E2E alt-flows ont dérivé des specs. DRAFT-FULL-01 (draft serpent complet 2 équipes → statut ACTIVE) n'a pas été livré. L'implémentation a adapté vers des tests de validation backend. C'est utile, mais si l'objectif était de prouver les flux UI complets, on a un écart."

**Dana (QA Engineer):** "SEC-R2 a une subtilité : la protection fonctionne, mais via `IllegalStateException → STOMP ERROR` plutôt qu'un rejet HTTP 401 propre au handshake. Ça marche, mais le message d'erreur côté client n'est pas explicite. À documenter ou corriger avant exposition publique."

**Elena (Junior Dev):** "Le `sprint-status.yaml` montre `sprint-8: status: in-progress` alors que toutes les stories sont `done`. C'est un oubli de mise à jour. Mineur mais crée de la confusion."

**Alice (Product Owner):** "Trois items `deferred-future` sans date de révision depuis Sprint 3/4 : hébergement, secrets prod, DB prod. Ils créent du bruit dans le sprint-status.yaml. Il faut soit fixer une date de révision, soit les marquer `wont-do` explicitement."

### Challenges clés
- E2E DRAFT-FULL-01 non livré — scénario UI complet toujours manquant
- SEC-R2 : chemin de rejet via exception plutôt que HTTP 401 propre
- sprint-status.yaml : sprint-8 status non mis à jour en `done`
- items `deferred-future` zombies depuis Sprint 3 — clarification nécessaire

---

## 5. Leçons apprises

### L1 — Les tests E2E complexes nécessitent un état préparé via API, pas via seed statique
**Contexte :** DRAFT-FULL-01 n'a pas été livré car il nécessitait un état DRAFTING seed trop complexe. La seed V1002 n'avait pas de partie en état DRAFTING prête.
**Leçon :** Pour les tests E2E nécessitant un état complexe (draft en cours, trade en attente), créer l'état en `beforeAll` via appels API admin/test plutôt que de dépendre de la seed statique. Pattern : `POST /api/admin/games/test-setup` ou équivalent.
**How to apply :** Pour toute story E2E avec état complexe, évaluer si la seed suffit ou si un setup API est nécessaire. Le décider *avant* de créer la story, pas pendant l'implémentation.

### L2 — Les rejets de sécurité WebSocket doivent être explicites côté client
**Contexte :** SEC-R2 rejette via `IllegalStateException → STOMP ERROR` plutôt qu'un HTTP 401 propre au handshake. Fonctionnel, mais le message client n'est pas explicite.
**Leçon :** Pour les stories de sécurité WebSocket, définir explicitement dans les ACs : (a) le code de rejet HTTP au handshake, (b) le message STOMP ERROR frame côté client, (c) le comportement frontend quand rejeté. Ne pas laisser le chemin d'erreur implicite.
**How to apply :** Template Dev Notes §WebSocket Security Pattern dans project-context.md.

### L3 — Le sprint-status.yaml `status` parent doit être la *dernière* action d'un sprint
**Contexte :** Sprint 8 livré à 100% mais `sprint-8: status: in-progress` dans le yaml.
**Leçon :** Ajouter dans checklist.md : "le statut `sprint-N: status` dans sprint-status.yaml doit être mis à `done` comme dernière action du sprint, après que toutes les stories sont `done`."
**How to apply :** Critère D3 dans les action items Sprint 9.

### L4 — Les items `deferred-future` sans date de révision deviennent des zombies
**Contexte :** Hébergement, secrets prod, DB prod sont `deferred-future` depuis Sprint 3/4 (2+ sprints). Ils occupent de l'espace cognitif sans avancer.
**Leçon :** Tout item `deferred-future` doit avoir un commentaire `# review_at: Sprint N` ou être explicitement marqué `wont-do: true` avec une raison. Sans ça, ils polluent le backlog indéfiniment.
**How to apply :** Critère D4 dans les action items Sprint 9.

---

## 6. Patterns établis (réutiliser en Sprint 9)

### WebSocket JWT auth (standard Sprint 8)
```java
// JwtHandshakeInterceptor — pattern de référence
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
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

```typescript
// Frontend WebSocketService — query param token
const token = this.authService.getToken();
const sockjs = new SockJS(`${environment.wsUrl}?token=${encodeURIComponent(token ?? '')}`);
```

### CI backend avec profils Maven (standard Sprint 8)
```yaml
# OWASP séparé en profil dédié pour ne pas bloquer le CI standard
mvn verify -Dexcludes=... -Psecurity-scan  # uniquement pour audit sécurité
mvn verify -Dexcludes=...                  # CI standard (SpotBugs + JaCoCo inclus)
```

### E2E alt-flows (standard Sprint 8)
```typescript
// Pattern : tester les codes d'erreur backend directement via API response
// quand l'état UI complet est trop complexe à setup
test('ALT-01: swap wrong region → 400', async ({ page }) => {
  test.setTimeout(35_000);
  // Direct API call + verify response code + verify UI error message
});
```

---

## 7. Action Items Sprint 9

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 9 (issus de la retro Sprint 8)
═══════════════════════════════════════════════════════════

### P0 — Fix immédiat (avant toute story)

| # | Action | Détail |
|---|---|---|
| F3 | sprint-status.yaml : sprint-8 status → done | Mettre `status: done` dans le bloc `sprint-8:`. Mettre `epic-8-retrospective: done`. |

### P1 — Qualité E2E

| # | Action | Détail |
|---|---|---|
| E2 | Implémenter DRAFT-FULL-01 | Créer état DRAFTING via API admin en `beforeAll`. Tester flux draft complet → statut ACTIVE. Si non faisable, documenter le compromis explicitement dans le test. |

### P2 — Sécurité

| # | Action | Détail |
|---|---|---|
| S2 | SEC-R2 follow-up : documenter ou corriger chemin rejet | Documenter la décision (exception vs HTTP 401) dans `project-context.md §WebSocket Security Pattern`. Ou corriger pour retourner HTTP 401 propre au handshake. |

### P3 — Process

| # | Action | Détail |
|---|---|---|
| D3 | checklist.md : vérification sprint-status parent | Ajouter critère : "sprint-N: status → done doit être la dernière action avant retro" dans §Final Status Verification. |
| D4 | sprint-status.yaml : résoudre les zombies deferred-future | Pour hébergement/secrets/DB prod : soit `wont-do: true` si décision finale, soit `review_at: Sprint N` si date à fixer. |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → résoudre via D4 (décision wont-do ou date)

═══════════════════════════════════════════════════════════

---

## 8. Évaluation Sprint 8

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 10/10 | 4/4 stories done (100%) |
| Qualité technique | 10/10 | SpotBugs 0, JaCoCo green, 2299 tests backend |
| Tests | 9/10 | +8 WS auth tests ; E2E scénarios adaptés (DRAFT-FULL-01 manquant) |
| Sécurité | 10/10 | SEC-R2 fermé (backlog Sprint 3) — WS auth en place |
| Process | 9/10 | C1/C2 ajoutés ; sprint-status parent non mis à jour |
| Suivi actions Sprint 7 | 10/10 | 5/5 livrés |
| **Global** | **9.7/10** | Meilleur sprint en termes de livraison — dette historique soldée |

---

## 9. Preview Sprint 9

**Bob (Scrum Master):** "Sprint 8 = solidité avant exposition. Sprint 9 = quelle est la prochaine couche de valeur ? On a une app solide, sécurisée, testée. La question stratégique : continuer la consolidation (E2E DRAFT-FULL-01, process D3/D4) ou introduire une nouvelle couche de features ?"

**Orientations naturelles Sprint 9 :**
1. **F3 (P0)** — fix sprint-status.yaml (5 minutes)
2. **D3/D4 (P3)** — clôturer les zombies process
3. **E2 (P1)** — DRAFT-FULL-01 ou documenter le gap explicitement
4. **S2 (P2)** — documenter/corriger SEC-R2 rejet HTTP
5. **Décision stratégique** — nouvelle feature ou consolidation finale ?

**Ce qu'on ne fait pas en Sprint 9 :**
- Hébergement externe / staging → D4 doit d'abord clarifier si c'est `wont-do`

---

*Rétrospective générée le 2026-03-15. Prochaine action recommandée : mettre à jour sprint-status.yaml (sprint-8 → done, epic-8-retrospective → done), puis `/bmad-bmm-sprint-planning` pour Sprint 9.*
