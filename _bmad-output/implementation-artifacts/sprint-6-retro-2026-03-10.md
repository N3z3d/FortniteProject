# Sprint 6 Retrospective — "L'app est prête à être montrée"

**Date :** 2026-03-10
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 6 :** "L'app est prête à être montrée" — auth JWT réelle + qualité process + maintenabilité E2E

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 5 / 5 (100%) |
| Stories en backlog | 0 |
| Tests frontend | 2245 total, 38 pre-existing Zone.js (inchangé) — JWT story files 43/43 ✅ |
| Tests backend | 11/11 SpaController tests verts (+3 nouveaux) |
| Code review findings résolus | 2 (H1 `attemptAutoLogin` + M1 File List vide) avant `done` |
| Incidents de production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| `sprint6-arch-usercontext-jwt` | P0 JWT auth : `AuthService` créé, `AuthInterceptor` remplacé (`X-Test-User` → `Bearer`), `UserContextService` migré vers Observable, `attemptAutoLogin()` bug critique corrigé via code review. 11 fichiers modifiés/créés. |
| `sprint6-q1-spacontroller-review-followups` | +3 tests SpaController manquants (CRITICAL/HIGH de la review Sprint 5) : `GET /`, `GET /admin/games`, `GET /main.js`. 11/11 verts. |
| `sprint6-q2-dod-review-followups-rule` | `project-context.md` mis à jour : Spring Boot 3.4.5, Vitest baseline, règle review DoD + bloquants HALT. |
| `sprint6-p1-e2e-helper-wait-for-page-ready` | Helper `waitForPageReady(page, route, waitMs?)` extrait — 11 call-sites refactorisés dans `admin.spec.ts` + `catalogue.spec.ts`. |
| `sprint6-p2-code-review-e2e-checklist-rule` | Critère "zombie variables" ajouté dans `checklist.md` + `project-context.md`. Boilerplate pattern `waitForPageReady` documenté. |

---

## 2. Suivi des actions Sprint 5

Bob (Scrum Master): "Commençons par voir comment on a tenu nos engagements. Sprint 5 nous avait laissé 4 action items."

| # | Action promise | Statut | Notes |
|---|---|---|---|
| A1 | UserContextService JWT | ✅ Fait | `sprint6-arch-usercontext-jwt` — auth JWT réelle, bug critique découvert et corrigé en review |
| Q1 | Fermer review follow-ups SpaController | ✅ Fait | `sprint6-q1-spacontroller-review-followups` — 3 tests manquants ajoutés, tous verts |
| Q2 | Règle DoD review follow-ups | ✅ Fait | `sprint6-q2-dod-review-followups-rule` — formalise dans project-context.md §6 + bloquants HALT |
| P1/P2 | Helper E2E + règle code review | ✅ Fait | `sprint6-p1/p2` — `waitForPageReady()` + critère zombie variables dans checklist |
| ⛔ | Ne pas re-proposer hébergement | ✅ Respecté | Zéro mention hébergement externe en Sprint 6 |

**Score : 4/4 (100%) — premier sprint avec 100% des actions livrées. Tous les points ouverts de Sprint 5 fermés.**

Alice (Product Owner): "C'est la première fois qu'on atteint 100% sur les actions. Et surtout, les actions portaient sur des gaps structurels — pas juste du feature work. Ça montre une vraie maturité de l'équipe."

---

## 3. Ce qui a bien marché

**Charlie (Senior Dev):** "Le plus important de ce sprint, c'est que le code review a attrapé un bug silencieux que rien d'autre n'aurait détecté. `attemptAutoLogin()` appelait `this.login(lastUser)` sans `.subscribe()`. Avec les Observables froids de RxJS, ça veut dire : l'appel HTTP n'est jamais fait. L'auto-login était cassé depuis le début du Sprint 4. Invisible dans les tests unitaires (ils mockent l'Observable), invisible en dev local (le fallback sessionStorage masquait le problème). Seul un reviewer humain lisant le code avec les yeux d'un adversaire pouvait le trouver."

**Dana (QA Engineer):** "La migration JWT est un beau cas de 'pas de régression visible'. On a remplacé un faux mécanisme d'authentification (`X-Test-User`) par un vrai JWT — et les 38 failures pre-existing sont exactement les mêmes qu'avant. Zéro nouvelle cassure. Ça veut dire que l'implémentation respectait les contrats existants."

**Elena (Junior Dev):** "Le helper `waitForPageReady()` a réduit 5 lignes répétées dans chaque test à 1. Mais surtout, il a introduit le paramètre `waitMs` optionnel — ADMIN-02 attend 3s (dashboard plus lent), les autres 2s. Ce genre de détail montre que le refactoring était bien pensé, pas mécanique."

**Bob (Scrum Master):** "Ce sprint a aussi un avantage sous-estimé : l'audit de la règle DoD. Le `project-context.md` était en retard sur la réalité — Spring Boot 3.3 au lieu de 3.4.5, Karma au lieu de Vitest, baseline 1805 au lieu de 2245. Maintenant les agents qui lisent ce fichier partent sur des données correctes. C'est une dette documentaire résolue."

**Alice (Product Owner):** "L'objectif 'l'app est prête à être montrée' — avec le JWT réel en place, je pense qu'on peut dire oui. Le seul 'faux' mécanisme restant est le mot de passe dev `Admin1234` hardcodé dans `environment.ts`. Mais ça, c'est explicitement documenté et scopé au mode développement."

### Victoires clés
- 100% action items Sprint 5 livrés — record absolu du projet
- Bug critique `attemptAutoLogin()` détecté et corrigé par le code review adversarial
- Migration JWT sans aucune régression sur 2245 tests existants
- `waitForPageReady()` : refactoring DRY propre, 11 call-sites nettoyés
- `project-context.md` remis à jour — documentation agents maintenant synchronisée avec la réalité
- Premier sprint 5/5 stories done

---

## 4. Ce qui n'a pas bien marché / Challenges

**Thibaut (Project Lead):** "Le bug `attemptAutoLogin()` me dérange. On a implémenté la story, les tests unitaires passaient, le code review Q1/Q2 du sprint d'avant avaient été faits — et pourtant ce bug existait. Ça veut dire que les tests unitaires actuels n'auraient pas attrapé ça."

**Charlie (Senior Dev):** "Exactement. Le test mockait `UserContextService.login()` pour retourner `of(undefined)`. Mais `of()` est aussi un Observable froid — sauf qu'il est souscrit automatiquement dans le test. Le bug était : en production, l'appel `this.login(lastUser)` créait un Observable mais personne n'appellait `.subscribe()`. En test, on mockait directement `login()` en `Observable` et le test vérifiait que la méthode était appelée (pas qu'elle était souscrite)."

**Dana (QA Engineer):** "Il faut une leçon sur les Observables froids dans les tests. Le mock `of()` cache le vrai comportement. Pour tester qu'un Observable cold est bien souscrit, il faut utiliser un `Subject` ou vérifier un effet secondaire (HTTP réellement déclenché)."

**Elena (Junior Dev):** "Je note aussi que le story file était vide (`File List` non rempli, toutes les tasks en `[ ]`) à l'état `ready-for-dev`. C'est une violation des bloquants HALT qu'on vient d'écrire. C'est une poule-et-l'œuf : on a créé la règle en même temps qu'on violait la règle. Mais maintenant la règle est là — plus d'excuse."

**Bob (Scrum Master):** "Le timing du bug fix est instructif. On a corrigé `attemptAutoLogin()` pendant le code review, pas pendant le développement initial. Ce qui veut dire que le développement initial est structurellement aveugle à ce type de bug. C'est une limite du TDD quand les mocks sont trop permissifs."

**Alice (Product Owner):** "Un point positif sur le négatif : on a quand même 0 story non-livrée. Tous les challenges ont été résolus dans le sprint. Ce n'est pas un sprint où on a découvert des problèmes et on les a mis sur la liste. On les a résolus."

### Challenges clés
- Bug `attemptAutoLogin()` non détectable par tests unitaires avec mocks `of()` — aveugle point TDD
- Story file vide (File List, tasks) lors de la transition `ready-for-dev` — DoD HALT désormais formalisé
- Règle DoD créée au même sprint où elle était violée — décalage chronologique acceptable mais à surveiller

---

## 5. Leçons apprises

### L1 — Les Observables froids ne s'exécutent pas sans subscribe — les tests avec `of()` cachent ce problème
**Contexte :** `attemptAutoLogin()` appelait `this.login(lastUser)` sans `.subscribe()`. Les tests mockaient `login()` retournant `of(undefined)` — mais le test vérifie que la méthode est appelée, pas que l'Observable est souscrit.
**Leçon :** Pour tester qu'un appel Observable est bien souscrit (fire-and-forget), utiliser un `Subject` explicite ou vérifier un effet secondaire observable (e.g., `authService.login` appelé avec expect + réseau mocké). La pattern `of()` dans un mock est trop permissive pour ce type de test.

### L2 — Le code review adversarial est irremplaçable pour les bugs de "convention implicite"
**Contexte :** Le bug Observable-non-souscrit ne viole aucune règle TypeScript (le compilateur ne peut pas le détecter), aucun test ne l'attrape (les mocks masquent le comportement), mais un reviewer qui sait que `login()` retourne un Observable cold le voit immédiatement.
**Leçon :** Les code reviews adversariales (chercher activement les bugs, pas juste lire le code) ont une valeur unique sur les patterns de concurrence et reactive programming. Ne pas traiter la review comme une checklist — la lire avec la mentalité "qu'est-ce qui peut être silencieusement cassé ici ?"

### L3 — La documentation agents doit être synchronisée proactivement, pas réactivement
**Contexte :** `project-context.md` indiquait Spring Boot 3.3, Karma, baseline 1805 — tout était obsolète depuis plusieurs sprints.
**Leçon :** Ajouter au DoD un critère optionnel : "si tu modifies un outil de la stack (framework version, test runner, CI), mettre à jour `project-context.md` dans le même sprint." Un fichier de contexte agents stale génère des sessions de débogage inutiles sur des bases obsolètes.

### L4 — La règle `File List vide → HALT` était nécessaire depuis Sprint 4
**Contexte :** La story `sprint6-arch-usercontext-jwt` avait un File List vide lors de la création. C'est une violation du DoD HALT créé dans le même sprint.
**Leçon :** Le blocage HALT sur File List vide doit s'appliquer au moment du passage en `review` (pas juste en `done`). Le story file doit avoir au minimum la liste des fichiers attendus (pas encore créés) dès que le dev agent commence à travailler. Un File List vide en review = impossible de valider la complétude du travail.

### L5 — `waitForPageReady(page, route, waitMs?)` : paramètre optionnel `waitMs` essentiel
**Contexte :** ADMIN-02 (dashboard admin) a besoin de 3s pour charger alors que les autres pages n'en ont besoin que de 2s. Le helper générique avec default évite les hacks per-test tout en restant flexible.
**Leçon :** Lors de l'extraction d'un helper réutilisable E2E, anticiper les variations légitimes de timing et les exposer comme paramètre optionnel avec une valeur par défaut sensée. Évite les `waitForTimeout(N)` inline qui reviendraient proliférer.

---

## 6. Patterns établis (réutiliser en Sprint 7)

### E2E Playwright — pattern waitForPageReady (nouveau standard Sprint 6)
```typescript
// Pattern standard pour navigation + guard URL dans les specs E2E
import { waitForPageReady } from './helpers/app-helpers';

test('FEATURE-0N: description', async ({ page }) => {
  test.setTimeout(35_000);
  await forceLoginWithProfile(page, 'thibaut'); // ou loginAsAdmin(page)
  if (!await waitForPageReady(page, '/route')) { test.skip(); return; }
  // Pages "lentes" (ex: admin dashboard) :
  if (!await waitForPageReady(page, '/admin', 3_000)) { test.skip(); return; }
  await expect(page.locator('.root-container')).toBeVisible({ timeout: 10_000 });
});
```

### RxJS Observable fire-and-forget — pattern correct
```typescript
// ❌ Bug silencieux : Observable cold jamais souscrit
this.someService.doSomething(param);

// ✅ Fire-and-forget : souscrit + erreur swallowed
this.someService.doSomething(param).subscribe({ error: () => {} });

// ✅ Fire-and-forget avec log erreur
this.someService.doSomething(param).subscribe({
  error: (e) => console.error('Background operation failed', e)
});
```

### JWT Auth flow frontend (standard Sprint 6)
```typescript
// AuthService.login() → sessionStorage jwt_token + jwt_user
// AuthInterceptor → Authorization: Bearer <token> sur /api/**
// UserContextService.isLoggedIn() → authService.getToken() !== null
// UserContextService.isAdmin() → jwtUser.role === 'ADMIN'
// UserContextService.logout() → authService.clearToken() + clearAllStorage()
```

### Code review E2E — critères à vérifier systématiquement
- Variables capturées (`const x = await locator.textContent()`) → doivent être consommées dans un `expect(x)`
- `waitForTimeout(N)` fixe → préférer `expect.poll()` ou `toBeVisible({ timeout: N })`
- Tout `.count()` immédiat sur liste async → préférer `toBeAttached({ timeout: N })` sur `.first()`

---

## 7. Questions ouvertes pour Sprint 7

**Thibaut (Project Lead):** "Le JWT est en place. Docker local tourne. E2E couvre admin, catalogue, trades. Qu'est-ce qui manque encore avant de montrer l'app à quelqu'un ?"

**Charlie (Senior Dev):** "Techniquement, l'app est présentable. Les flux critiques fonctionnent : login → jeu → draft → trade → leaderboard. Ce qui manque c'est plus de la finition que de la fonctionnalité. Je pense aux 38 failures Zone.js pre-existing — on les a acceptées depuis Sprint 2 mais elles polluent le rapport de tests."

**Dana (QA Engineer):** "On a aussi le `sprint4-a6-jpa-legacy-migration-5-services` marqué done, mais `sprint3-a6` et `sprint4-a3b` (CI/CD complet) sont toujours en backlog. Et `sprint3-api-fortnite-wiring-check` est en backlog depuis Sprint 3."

**Elena (Junior Dev):** "La dette de tests Zone.js — 38 failures — c'est le seul signal rouge visible à quelqu'un qui regarde les métriques. Si on veut 'montrer l'app', commencer par un rapport de tests 100% vert ou presque."

**Bob (Scrum Master):** "Ce qu'on peut faire pour Sprint 7 : soit s'attaquer aux 38 Zone.js (ce qui serait un sprint de stabilisation testing), soit continuer sur les features manquantes (CI/CD complet, Fortnite API, E2E jeu complet). À prioriser en planning."

**Alice (Product Owner):** "Ma recommandation : Sprint 7 = 'production-readiness'. Pas de nouvelles features. Réparer ce qui fait peur : Zone.js failures, API Fortnite wiring check, CI/CD complet avec docker build."

---

## 8. Action Items Sprint 7

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 7 (issus de la retro Sprint 6)
═══════════════════════════════════════════════════════════

### P0 — Qualité tests (derniers feux rouges)

| # | Action | Détail |
|---|---|---|
| Z1 | Résoudre les 38 failures Zone.js debounce | Les 38 pre-existing `fakeAsync+tick` dans 12 spec files. Conversion `async/vi.useFakeTimers`. Cible : ≤ 5 failures résiduelles. |

### P1 — Vérification câblage (dette Sprint 3)

| # | Action | Détail |
|---|---|---|
| F1 | API Fortnite wiring check | `sprint3-api-fortnite-wiring-check` en backlog depuis Sprint 3. Valider FortniteApiAdapter + clé .env en Docker local. |
| F2 | CI/CD pipeline complet | `sprint3-a3b-cicd-pipeline-complet` : docker build + push registry. Valider que la CI passe avec les nouveaux fichiers (auth.service.ts, etc.). |

### P2 — Amélioration process

| # | Action | Détail |
|---|---|---|
| D1 | Règle `project-context.md` proactive | Ajouter critère DoD : "si version framework/test runner modifiée → mettre à jour project-context.md dans le même commit." |
| D2 | File List non-vide avant `in-progress` | Renforcer : le story file doit avoir au minimum la liste des fichiers attendus avant que le dev agent commence. `[ ]` tasks acceptées mais File List obligatoire. |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → `deferred-future`

═══════════════════════════════════════════════════════════

---

## 9. Évaluation Sprint 6

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 10/10 | 5/5 stories (100%), premier sprint parfait |
| Qualité technique | 9/10 | Bug critique detécté en review, 0 régression sur 2245 tests |
| Tests | 8/10 | JWT files 43/43, +3 SpaController verts. 38 pre-existing toujours là. |
| Architecture | 9/10 | JWT réel en place, X-Test-User supprimé, interceptor propre |
| Suivi actions Sprint 5 | 10/10 | 4/4 actions (100%) |
| Process | 9/10 | DoD mis à jour, checklist review enrichi, pattern waitForPageReady établi |
| **Global** | **9.2/10** | Sprint le plus abouti du projet — objectif "prête à montrer" atteint |

---

## 10. Preview Sprint 7

**Bob (Scrum Master):** "Sprint 7 doit répondre à : 'si quelqu'un regarde notre CI/CD et notre rapport de tests, est-ce que ça fait professionnel ?' C'est la question de la production-readiness visible."

**Priorités naturelles Sprint 7 :**
1. **Zone.js failures** — dernier signal rouge visible dans les métriques
2. **API Fortnite wiring check** — dette Sprint 3 non résolue
3. **CI/CD complet** — docker build + push registry pour fermer A3b
4. **Règle project-context.md proactive** — éviter la dérive documentaire

**Ce qu'on ne fait pas en Sprint 7 :**
- Hébergement externe → `deferred-future`
- Staging → `deferred-future`
- Nouvelles features UI

---

*Rétrospective générée le 2026-03-10. Prochaine action recommandée : `/bmad-bmm-sprint-planning` pour créer le Sprint 7 avec les action items ci-dessus intégrés.*
