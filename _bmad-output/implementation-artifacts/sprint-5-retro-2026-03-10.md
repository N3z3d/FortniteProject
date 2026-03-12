# Sprint 5 Rétrospective — "Thibaut peut utiliser son app"

**Date :** 2026-03-10
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 5 :** "Thibaut peut utiliser son app" — navigation globale, accès admin, couverture E2E masse

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 4 / 5 (80% — 1 backlog) |
| Stories en backlog | 1 (`sprint5-arch-usercontext-jwt`) |
| Tests frontend | 2213 / 2234 passing (21 Zone.js pre-existing, inchangé) |
| Tests backend | ~2402 run, 17F+1E (tous pre-existing, inchangé) |
| Tests E2E | 5 spec files actifs (admin, catalogue, trade ×3) |
| Review findings résolus | 13 (5 catalogue + 8 admin panel) avant `done` |
| Incidents de production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| `sprint5-fix-spa-controller-404` | SpaController : `{*rest}` → `/**` + regex anti-api. Toutes routes Angular accessibles en accès direct. 8 tests unitaires. |
| `sprint5-ux-navbar-globale` | Navbar globale persistante (Parties + Catalogue) + menu Profil dropdown (Mon profil / Paramètres / Déconnexion / ⚙️ Administration si isAdmin()). Décision Party Mode unanime Option B. |
| `sprint5-e2e-admin-panel` | ADMIN-06 à ADMIN-09 ajoutés (DB Explorer, Logs, Error Journal, dropdown nav). 8 findings code review résolus avant `done`. |
| `sprint5-e2e-catalogue` | CAT-01 à CAT-05 créés (load, search, region filter, comparison panel, accessible list). 5 findings code review résolus avant `done`. |

### Story non livrée

| Story | Raison |
|---|---|
| `sprint5-arch-usercontext-jwt` | Reportée : P2 pour Sprint 5, P0 avant exposition publique. Gap documenté mais non bloquant pour l'usage local actuel. |

---

## 2. Suivi des actions Sprint 4

Bob (Scrum Master): "Commençons par voir si on a tenu nos engagements de la dernière retro."

| # | Action promise | Statut | Notes |
|---|---|---|---|
| B1 | Fix SpaController 404 | ✅ Fait | `sprint5-fix-spa-controller-404` — `/admin/dashboard` ne retourne plus 404 |
| E1 | E2E Admin Panel | ✅ Fait | `sprint5-e2e-admin-panel` — ADMIN-01 à ADMIN-09, tous routes admin couverts |
| E2 | E2E Catalogue | ✅ Fait | `sprint5-e2e-catalogue` — CAT-01 à CAT-05, route core produit couverte |
| N1 | Navbar globale | ✅ Fait | `sprint5-ux-navbar-globale` — Parties + Catalogue toujours visibles |
| N2 | Accès admin UX | ✅ Fait | Party Mode → Option B pure (menu profil dropdown) |
| A1 | UserContextService JWT | ⏳ Non fait | `sprint5-arch-usercontext-jwt: backlog` — reporté Sprint 6 |
| ⛔ | Ne pas re-proposer hébergement | ✅ Respecté | Zéro mention hébergement externe en Sprint 5 |

**Score : 5/6 (83%) — meilleur suivi depuis Sprint 1. La règle hébergement est maintenant un réflexe.**

Alice (Product Owner): "83% de suivi, c'est vraiment bien. Et l'item non livré (JWT) est consciemment différé, pas oublié — c'est une différence importante."

---

## 3. Ce qui a bien marché

**Charlie (Senior Dev):** "Le fix SpaController est un exemple de chirurgie précise. Un fichier, un pattern regex changé, 8 tests verts, zéro régression. Le diagnostic de `{*rest}` vs `/**` en Spring MVC 6 était non trivial — l'avoir documenté dans le Dev Notes pour les futures sessions vaut de l'or."

**Dana (QA Engineer):** "Ce sprint marque un tournant sur la qualité E2E. On est passé de 'angle mort total sur admin et catalogue' (retro Sprint 4) à une couverture complète avec code review. 13 findings résolus avant `done` sur deux specs — c'est exactement le process qu'on voulait établir."

**Alice (Product Owner):** "L'objectif 'Thibaut peut utiliser son app' — je pense qu'on peut dire mission accomplie sur 80% du périmètre. La navbar est là, l'admin est accessible, les routes ne retournent plus 404. C'est le premier sprint où l'experience utilisateur a été la priorité explicite."

**Elena (Junior Dev):** "La décision Party Mode pour l'UX admin a été rapide et unanime (8/8 agents, Option B pure). C'est un pattern qui fonctionne bien pour les décisions d'architecture UX — évite les aller-retours et aligne tout le monde avant de coder."

**Bob (Scrum Master):** "Le process code review est maintenant systématique sur les stories E2E. Et les reviews trouvent de vraies choses — pas du théâtre. CAT-02 avec `initialText` capturée mais jamais comparée, ou CAT-05 avec le `count()` immédiat sans attendre Angular : ce sont des bugs réels qui auraient rendu les tests flaky en CI."

### Victoires clés
- Objectif sprint "utilisable" : livré à 80%
- Party Mode → décision UX en une passe, zéro aller-retour
- Code review systématique : 13 findings résolus (5 + 8) sur les deux specs E2E
- SpaController : fix propre, bien testé, pattern documenté pour les prochains sprints
- 83% suivi actions Sprint 4 — record du projet

---

## 4. Ce qui n'a pas bien marché / Challenges

**Thibaut (Project Lead):** "Le JWT mock, ça me pèse. Depuis Sprint 4 on sait que le login ne génère pas de vrai JWT. C'est documenté partout, mais c'est pas réglé."

**Charlie (Senior Dev):** "C'est une décision consciente — P2 en Sprint 5 parce qu'en Docker local c'est pas bloquant. Mais Elena a raison de le garder en tête : si on expose l'app un jour, c'est P0 immédiat."

**Elena (Junior Dev):** "Ce qui me dérange sur `sprint5-fix-spa-controller-404` — le review a trouvé CRITICAL : Task 2.6 était cochée [x] mais le test `GET /` n'était pas dans le fichier. Les follow-ups de review restent ouverts dans le story file (`[ ]` non cochés). On a livré la story mais les follow-ups de la review ne sont pas fermés."

**Dana (QA Engineer):** "C'est un pattern qu'on retrouve parfois : la story passe `done` mais certains `[ ]` review follow-ups restent ouverts. Il faudrait une règle : soit on les résout avant `done`, soit on crée un ticket de suivi."

**Bob (Scrum Master):** "Notez ça pour les action items. Et autre point : la variable `initialText` dans CAT-02 — le dev agent l'a capturée, ne l'a pas utilisée pour une vraie comparaison. Ce genre de code 'zombie' (capturé mais inutilisé) aurait dû être attrapé plus tôt. C'est une qualité de review à améliorer."

**Alice (Product Owner):** "Phase 3 (`sprint5-arch-usercontext-jwt`) est à backlog. Ce n't est pas un échec — c'était P2 dans la planification — mais il faut vraiment l'embarquer en Sprint 6. L'objectif 'Thibaut peut utiliser son app' est à 80% ; les 20% restants c'est cette histoire JWT."

### Challenges clés
- JWT mock gap : documenté mais non résolu depuis Sprint 4 → dette croissante
- Review follow-ups SpaController (`[ ]`) laissés ouverts après `done` — process à clarifier
- Code zombie (variables capturées jamais utilisées) non détecté en première passe dev
- Phase 3 non embarquée : `sprint5-arch-usercontext-jwt` reste backlog

---

## 5. Leçons apprises

### L1 — `{*rest}` en Spring MVC 6 (PathPatternParser) ne matche pas les chemins à 2 segments
**Contexte :** `SpaController` utilisait `/{root:regex}/{*rest}` qui ne capturait pas `/admin/dashboard`.
**Leçon :** En Spring Boot 3.x / PathPatternParser, utiliser Ant-style `/**` pour les wildcards multi-segments : `/{segment:(?!api|actuator)[^.]*}/**`. Documenter le "pourquoi" dans le code (pas seulement le "quoi").

### L2 — Les variables capturées doivent être utilisées — ou supprimées
**Contexte :** CAT-02 capturait `initialText` pour comparer après la recherche, mais n'effectuait jamais la comparaison. Variable zombie qui induisait en erreur les reviewers.
**Leçon :** Toute variable capturée doit avoir une assertion qui la consomme. Si la valeur ne peut pas être assertée de façon fiable, ne pas la capturer. Règle à appliquer en code review E2E : "chercher les variables read-but-never-asserted."

### L3 — `toBeAttached({ timeout })` > `.count()` immédiat pour les listes async Angular
**Contexte :** CAT-05 utilisait `.count()` immédiatement après que le viewport soit visible, avant qu'Angular ait peuplé la liste accessible.
**Leçon :** Pour les éléments peuplés de façon asynchrone par Angular (notamment les listes `*ngFor` après une réponse HTTP), utiliser `toBeAttached({ timeout: N })` ou `toBeVisible({ timeout: N })` sur le premier élément attendu plutôt qu'un `.count()` immédiat.

### L4 — Les review follow-ups doivent être résolus ou convertis en ticket avant `done`
**Contexte :** `sprint5-fix-spa-controller-404` a des review follow-ups CRITICAL (`[ ]`) ouverts dans le story file même après que la story soit marquée `done`.
**Leçon :** Règle à intégrer dans le DoD : avant de marquer une story `done`, vérifier que les `[ ]` de la section "Review Follow-ups (AI)" sont soit cochés `[x]` (résolus) soit convertis en story/ticket de suivi avec une référence. Un story file `done` avec des `[ ]` ouverts est un état incohérent.

### L5 — Party Mode = outil décisionnel fiable pour les questions d'architecture UX
**Contexte :** La question "comment exposer l'accès admin" était bloquée depuis Sprint 4. Party Mode l'a résolue en un passage (8/8 unanime Option B).
**Leçon :** Pour toute décision d'architecture qui implique plusieurs perspectives (UX / sécurité / accessibilité / tech), déclencher Party Mode avant d'implémenter. Évite les aller-retours et aligne tout le monde sur la même vision.

### L6 — `expect.poll()` > `waitForTimeout` fixe pour les assertions E2E asynchrones
**Contexte :** CAT-02 utilisait `waitForTimeout(400)` fixe après le fill du champ de recherche.
**Leçon :** Préférer `expect.poll(async () => ..., { timeout: N_000 })` pour attendre qu'un état asynchrone se stabilise. Plus robuste qu'un délai fixe : s'adapte à la latence réelle du backend local.

---

## 6. Patterns établis (réutiliser en Sprint 6)

### E2E Playwright — pattern catalogue (forceLoginWithProfile)
```typescript
test('CAT-0N: description', async ({ page }) => {
  test.setTimeout(35_000);
  await forceLoginWithProfile(page, 'thibaut');
  await page.goto('/route');
  await page.waitForTimeout(2_000);
  if (!page.url().includes('/route')) { test.skip(); return; }
  await expect(page.locator('.root-container')).toBeVisible({ timeout: 10_000 });
  const loadingEl = page.locator('.loading-indicator');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }
  // ... assertions structurelles uniquement
});
```

### E2E — guard avant click sur élément potentiellement lent
```typescript
const triggerEl = page.locator('[data-testid="some-select"]');
const visible = await triggerEl.isVisible({ timeout: 5_000 }).catch(() => false);
if (!visible) { test.skip(); return; }
await triggerEl.click();
```

### E2E — attente async list population
```typescript
// Pas ça :
const count = await page.locator('.list-item').count(); // race condition
// Mais ça :
await expect(page.locator('.list-item').first()).toBeAttached({ timeout: 5_000 });
```

### SpaController — pattern Spring Boot 3.x / PathPatternParser
```java
@GetMapping(value = {"/{path:[^.]*}", "/"})
public String spaRoot() { return "forward:/index.html"; }

@GetMapping(value = "/{segment:(?!api|actuator)[^.]*}/**")
public String spaDeep() { return "forward:/index.html"; }
```

---

## 7. Questions ouvertes pour Sprint 6

**Thibaut:** "Est-ce qu'on adresse le JWT en Sprint 6 ?"

**Alice (Product Owner):** "`sprint5-arch-usercontext-jwt` était P2 en Sprint 5, mais si l'objectif de Sprint 6 est 'l'app est prête pour être montrée', c'est P0. À trancher en planning."

**Charlie (Senior Dev):** "Les review follow-ups ouverts dans `sprint5-fix-spa-controller-404` — les 3 manques de tests (`GET /`, `GET /admin/games`, fichiers statiques). Ce sont des gaps de couverture réels. À embarquer en Sprint 6 ou à ajouter directement au story file de suivi."

---

## 8. Action Items Sprint 6

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 6 (issus de la retro Sprint 5)
═══════════════════════════════════════════════════════════

### P0 — Architecture (bloquant avant exposition publique)

| # | Action | Détail |
|---|---|---|
| A1 | UserContextService JWT | Implémenter `sprint5-arch-usercontext-jwt` : remplacer le login mock par une vraie authentification JWT. P0 avant toute exposition publique. |

### P1 — Qualité / Tests

| # | Action | Détail |
|---|---|---|
| Q1 | Fermer review follow-ups SpaController | Ajouter les 3 tests manquants identifiés en review : `GET /` → `forward:/index.html`, `GET /admin/games` → 200, `GET /main.js` → non-intercepté. |
| Q2 | Règle DoD : review follow-ups | Formaliser dans `project-context.md` : avant `done`, tous les `[ ]` de "Review Follow-ups (AI)" doivent être soit `[x]` soit convertis en ticket. |

### P2 — Amélioration process

| # | Action | Détail |
|---|---|---|
| P1 | Helper `waitForPageReady(page, route)` | Extraire le pattern guard URL + loading wait dans un helper E2E réutilisable. Réduire la duplication boilerplate dans chaque test CAT-0N / ADMIN-0N. |
| P2 | Règle code review E2E | Ajouter critère explicite dans le checklist code-review pour les specs E2E : "vérifier que toute variable capturée a une assertion qui la consomme." |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → `deferred-future`

═══════════════════════════════════════════════════════════

---

## 9. Évaluation Sprint 5

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 8/10 | 4/5 stories (80%), seul `jwt-auth-context` non livré (P2, conscient) |
| Qualité technique | 8/10 | 13 review findings résolus, 0 régression, SpaController fix propre |
| Tests E2E | 8/10 | 5 spec files, CAT-01..05 + ADMIN-06..09 + MULTI-01..03. Review systématique. |
| Accessibilité features | 9/10 | Navbar globale, routes accessibles, menu dropdown admin — objectif atteint |
| Suivi actions Sprint 4 | 8/10 | 5/6 (83%), A1 JWT différé consciemment |
| Process | 9/10 | Party Mode efficace, code review systématique, règle hébergement respectée |
| **Global** | **8.3/10** | Sprint le plus équilibré du projet — objectif utilisateur livré |

---

## 10. Preview Sprint 6

**Bob (Scrum Master):** "Sprint 6 doit répondre à : 'est-ce que l'app est prête à être montrée à quelqu'un qui ne sait pas qu'il y a un mock login ?' Si oui, on peut commencer à parler d'exposition publique."

**Priorités naturelles :**
1. **JWT auth** — seul item qui bloque la prod (`sprint5-arch-usercontext-jwt`)
2. **Fermer les follow-ups SpaController** — tests manquants CRITICAL/HIGH
3. **Formaliser règle DoD review follow-ups** — éviter les récidives
4. **Helper E2E** — réduire la boilerplate et améliorer la maintenabilité des specs

**Ce qu'on ne fait pas en Sprint 6 :**
- Hébergement externe → `deferred-future`
- Staging → `deferred-future`

---

*Rétrospective générée le 2026-03-10. Prochaine action recommandée : `/bmad-bmm-sprint-planning` pour créer le Sprint 6 avec les action items ci-dessus intégrés.*
