# Sprint 7 Retrospective — "Stabilisation qualité tests + démo-able locale"

**Date :** 2026-03-12
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 7 :** "CI vert avec Vitest + docker push GHCR + ≤5 failures qualifiées + app démo-able avec données"

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 3 / 4 (75%) — Z1 ✅, seed-data ✅, D1/D2 ✅, F2 en review |
| Failures Vitest | 0 / 2243 (sprint6 = 38 pre-existing → sprint7 = 0 ✅) |
| CI Vitest step | Ajouté avec `continue-on-error: true` + log artifact + CI Summary |
| Docker GHCR push | Configuré dans ci.yml — validation finale requiert un push sur main |
| Seed data | V1002: 2 parties, 3 équipes, 15 joueurs, 3 score_deltas, 15 rank_snapshots |
| Incidents de production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| `sprint7-z1-fix-zonejs-debounce-failures` | Zone.js failures éradiquées : 38 → 0. Patterns A et B documentés dans `project-context.md`. Suite 2243/2243 verte. Code review fermée. |
| `sprint7-seed-data-demo` | `V1002__seed_demo_games.sql` : 2 parties (FINISHED "Coupe Automne 2025" + CREATING "Tournoi Hiver 2025"), 3 équipes, 15 joueurs Fortnite (Bugha/Aqua/Clix sparkline réels), 3 score_deltas. App navigable avec données sans étapes manuelles. |
| `sprint7-d1d2-micro-process-updates` | D1: critère "Pre-existing Failures Documented" ajouté dans `checklist.md`. D2a: patterns fakeAsync→Vitest (A/B) dans `template.md`. D2b: `project-context.md` — §Conversion fakeAsync→Vitest, §Pre-existing Failures Baseline, baseline 2243/0, DoD Vitest command. |
| `sprint7-f2-ci-vitest-et-docker-push` | `ci.yml` : step Vitest `continue-on-error: true` + artifact log + CI Summary ; job `docker-build-and-push` avec GHCR lowercase normalization + `permissions: packages: write`. **Statut : review** — la validation requiert un push sur `main` pour confirmer le push registry. |

---

## 2. Suivi des actions Sprint 6

Bob (Scrum Master): "Sprint 6 nous avait fixé 5 livrables sous 4 labels. Faisons le bilan."

| # | Action promise | Statut | Notes |
|---|---|---|---|
| Z1 | Résoudre les 38 failures Zone.js debounce | ✅ Fait | 38 → 0 failures. Patterns A et B formalisés. Suite 100% verte. |
| F1 | API Fortnite wiring check | ✅ Déjà fait (Sprint 4) | `sprint4-api-fortnite-wiring-check: done` — FortniteApiAdapter live (curl Bugha = HTTP 200). F1 = zombie dans Sprint 7. |
| F2 | CI/CD docker build + push registry | ✅ Fait (review) | ci.yml complet. Push GHCR configuré. Validation finale = 1 push GitHub. |
| D1 | Critère project-context.md proactive | ✅ Fait | `checklist.md` : critère "Pre-existing Failures Documented" ajouté. |
| D2 | Pattern fakeAsync→Vitest dans template | ✅ Fait | `template.md` + `project-context.md` mis à jour avec patterns A/B + baseline. |
| ⛔ | Ne pas re-proposer hébergement/staging | ✅ Respecté | Zéro mention hébergement externe en Sprint 7. |

**Score : 5/5 (100%) — deuxième sprint consécutif avec 100% des actions livrées.**

Alice (Product Owner): "F1 était un zombie — c'est bien qu'on l'ait détecté en pré-check plutôt qu'en mi-sprint. C'est exactement l'utilité des `sprint7-precheck-*` : valider les hypothèses avant de créer des stories."

---

## 3. Ce qui a bien marché

**Charlie (Senior Dev):** "Le résultat le plus spectaculaire, c'est les 38 → 0 failures Vitest. Ce chiffre traînait depuis le Sprint 2. On avait accepté ces failures comme 'pre-existing' et normalisé leur présence dans les rapports. Maintenant la suite est 100% verte. Le CI Vitest step aura quelque chose de propre à afficher."

**Dana (QA Engineer):** "La formalisation des Patterns A et B dans `project-context.md` est exactement ce qu'il fallait. Le problème Zone.js n'était pas technique — c'était documentaire. Les agents qui généraient des tests avec `fakeAsync+tick()` le faisaient parce que c'est le pattern Karma standard. Maintenant le fichier de référence les oriente vers les patterns corrects dès le premier sprint."

**Elena (Junior Dev):** "Les pré-checks sont une innovation Sprint 7 qui a bien fonctionné. `sprint7-precheck-f1-verification` a évité qu'on passe 2 jours sur un wiring check déjà fait en Sprint 4. Le pattern 'vérifier les hypothèses avant de créer les stories' est précieux."

**Bob (Scrum Master):** "La seed data est sous-estimée en termes d'impact UX. Avant, `docker-compose up` donnait une app vide — les flux de test requis des étapes manuelles (créer une partie, ajouter des joueurs, faire un draft...). Maintenant en 2 minutes on a 2 parties, 15 joueurs avec sparklines, et un leaderboard avec des scores réalistes. C'est la différence entre une démo potentiellement embarrassante et une démo fluide."

**Thibaut (Project Lead):** "Ce qui me plaît le plus, c'est la cohérence des livrables. Trois stories P1/P2 en support du P0 (CI Vitest). Z1 = tests verts → CI a quelque chose de propre. Seed = app navigable → CI a quelque chose à tester. D1/D2 = documentation → les futurs tests seront écrits correctement dès le départ. Tout converge vers le même objectif."

**Alice (Product Owner):** "Le commit `6a64bd4` (434 fichiers) était un nettoyage de 4 sprints de dette git. Ce n'était pas prévu dans le sprint mais c'était bloquant pour le CI. Le fait qu'on l'ait fait dans le bon ordre — `.gitignore` d'abord, puis commit sélectif — est un signe de maturité."

### Victoires clés
- Zone.js failures : 38 → 0 — dernier signal rouge visible dans les métriques
- Patterns A/B formalisés : les futurs agents écriront des tests Vitest-compatible dès le premier essai
- Pré-checks Sprint 7 : zombie F1 détecté avant de créer une story inutile
- Seed data V1002 : app démo-able sans étapes manuelles en Docker local
- 100% des actions Sprint 6 livrées — deuxième sprint consécutif à 100%
- GHCR push configuré : un push sur `main` pour valider F2

---

## 4. Ce qui n'a pas bien marché / Challenges

**Charlie (Senior Dev):** "F2 reste en 'review' — c'est le seul livrable non-`done`. Le code est complet et correct, mais on ne peut pas marquer `done` sans avoir vu le CI s'exécuter sur GitHub avec un vrai push. C'est une dépendance externe (GitHub Actions + GHCR) qu'on ne contrôle pas en dev local."

**Dana (QA Engineer):** "La migration fakeAsync→Vitest a été faite en Sprint 3 (21 failures restantes) puis Sprint 7 (0 failures). Mais entre les deux, il y avait des patterns mixtes dans la codebase — certains fichiers en Pattern A, d'autres gardant fakeAsync. Le résultat Z1 est propre, mais la durée (2+ sprints) montre qu'on aurait dû avoir les Patterns A/B documentés dès le début de la migration."

**Elena (Junior Dev):** "Le commit de 434 fichiers est techniquement correct mais c'est un indicateur de process — on aurait dû committer de façon incrémentale au fil des sprints. Le fait que 4 sprints de travail n'étaient pas commités crée un risque (perte de travail si corruption locale) et rend l'historique git moins lisible."

**Bob (Scrum Master):** "Le statut de F2 illustre une limite de notre setup : on valide les stories en local mais on ne peut pas toujours prouver qu'elles fonctionnent en CI sans un push réel sur GitHub. Il nous faut peut-être une règle 'les stories CI doivent être validées sur la branche principale, pas juste localement' dans le DoD."

**Thibaut (Project Lead):** "La normalisation des 38 failures depuis Sprint 2 a eu un coût. On les a acceptées comme 'acceptable tech debt' pendant 5 sprints. La réalité : elles généraient du bruit dans chaque rapport de test, elles démotivaient les agents qui voyaient 38 rouges au départ, et elles masquaient potentiellement de nouvelles regressions. La leçon : ne jamais normaliser des failures — les documenter comme 'known issues à résoudre dans N sprints' avec une date limite."

### Challenges clés
- F2 non-`done` : dépendance GitHub Actions non validable en local
- Historique git : 4 sprints non commités → risque et lisibilité réduite
- Normalisation des failures : 38 Zone.js acceptées pendant 5 sprints → coût invisible sur la qualité perçue

---

## 5. Leçons apprises

### L1 — Les "pre-existing failures" ne doivent jamais être normalisées durablement
**Contexte :** Les 38 failures Zone.js ont été acceptées depuis Sprint 2, documentées comme "pre-existing", et corrigées seulement en Sprint 7 (5 sprints plus tard). Pendant tout ce temps, elles généraient du bruit dans les rapports, masquaient potentiellement de nouvelles regressions, et démotivaient les agents démarrant avec 38 rouges.
**Leçon :** Toute failure pre-existing doit avoir une date limite explicite dans le ticket qui la documente. Format recommandé : `[KNOWN] Failure XYZ — résoudre avant Sprint N au plus tard`. Sans date limite, les "pre-existing" deviennent permanentes.

### L2 — Les pré-checks de sprint évitent les stories zombies
**Contexte :** F1 (API Fortnite wiring check) était dans les action items Sprint 7 issus du Sprint 6. Un pré-check a confirmé qu'il avait été livré en Sprint 4. Sans le pré-check, on aurait créé une story, planifié du temps, et découvert en mid-sprint que c'était déjà fait.
**Leçon :** Systématiser les `sprint-X-precheck-*` avant toute story dont la nécessité est basée sur un état présumé (backlog depuis N sprints). Le pré-check coûte 5 minutes et peut économiser une journée.

### L3 — Les patterns de migration (fakeAsync→Vitest) doivent être documentés au moment de la migration, pas après
**Contexte :** La migration Karma→Vitest a commencé Sprint 3. Les Patterns A et B ont été formalisés en Sprint 7 (4 sprints plus tard). Pendant ces 4 sprints, les agents continuaient d'écrire `fakeAsync+tick()` parce que c'est le pattern qu'ils connaissaient, générant de nouvelles failures.
**Leçon :** Tout changement de stack testing doit produire immédiatement (dans le même sprint) : (1) le pattern de migration documenté dans `project-context.md`, (2) au moins un exemple de "avant/après" copié depuis un vrai fichier migré.

### L4 — Les stories CI/CD nécessitent une validation sur la branche principale (pas seulement locale)
**Contexte :** F2 (`sprint7-f2-ci-vitest-et-docker-push`) est complet et correct localement mais ne peut pas passer en `done` sans un push réel sur `main` pour prouver que le registry push fonctionne avec `GITHUB_TOKEN`.
**Leçon :** Ajouter au DoD un critère spécifique aux stories CI/CD : "toute story modifiant `ci.yml` ou un workflow GitHub Actions doit être validée par un push sur la branche principale avec CI vert visible dans GitHub Actions UI". Cette validation ne peut pas être remplacée par un test local.

### L5 — La commit discipline : committer à chaque story `done`, pas à la fin du sprint
**Contexte :** Le commit `6a64bd4` incluait 434 fichiers de 4 sprints de travail. C'était nécessaire pour débloquer le CI, mais c'est un anti-pattern : historique illisible, risque de perte de travail en cas de corruption locale, impossible de bisect en cas de régression.
**Leçon :** Règle à ajouter au DoD : "chaque story en `done` doit correspondre à au moins 1 commit Git avant de passer à la story suivante." Le format `type(scope): description` est déjà la convention — l'appliquer systématiquement à chaque story.

---

## 6. Patterns établis (réutiliser en Sprint 8)

### Conversion fakeAsync → Vitest (standard Sprint 7)

```typescript
// Pattern A — Observable synchrone (spy retourne of(value))
// AVANT (incompatible Vitest)
it('should load', fakeAsync(() => {
  component.ngOnInit();
  tick(300);
  fixture.detectChanges();
  expect(component.data).toBeTruthy();
}));

// APRÈS
it('should load', async () => {
  component.ngOnInit();
  fixture.detectChanges();
  expect(component.data).toBeTruthy();
});

// Pattern B — debounceTime réel
// AVANT
it('should debounce', fakeAsync(() => {
  component.searchControl.setValue('test');
  tick(300);
  expect(service.search).toHaveBeenCalled();
}));

// APRÈS
it('should debounce', async () => {
  vi.useFakeTimers();
  component.searchControl.setValue('test');
  vi.advanceTimersByTime(300);
  fixture.detectChanges();
  expect(service.search).toHaveBeenCalled();
  vi.useRealTimers();
});
// vi est global (globals: true dans vitest.config.mts) — pas besoin d'import
```

### Pré-checks avant création de stories (standard Sprint 7)
```yaml
# Pattern : créer des sprint-X-precheck-* avant les stories dépendant d'un état présumé
sprint8-precheck-<feature>: done  # Valider hypothesis avant story creation
```

### CI GHCR push (standard Sprint 7)
```yaml
# Image name normalization obligatoire (GHCR = lowercase)
- name: Normalize GHCR image name
  run: echo "IMAGE_NAME=$(echo 'ghcr.io/${{ github.repository }}' | tr '[:upper:]' '[:lower:]')" >> "$GITHUB_ENV"

# Permissions requises au niveau du job
permissions:
  contents: read
  packages: write
```

### Seed data Docker (standard Sprint 7)
```sql
-- Pattern : ON CONFLICT DO NOTHING pour idempotence
-- Utiliser gen_random_uuid() pour les IDs team_players (sans UUID fixe)
-- Migrations V1001 (users+players) + V1002 (games+teams+scores+snapshots)
-- Flyway auto-execute au démarrage de l'app
```

---

## 7. Questions ouvertes pour Sprint 8

**Thibaut (Project Lead):** "Sprint 7 a nettoyé le dernier signal rouge visible. Maintenant les métriques sont propres : 2243/2243 Vitest, CI configuré, seed data en place. Qu'est-ce qui reste comme friction dans l'usage quotidien de l'app ?"

**Charlie (Senior Dev):** "La validation de F2 est la première urgence. Techniquement c'est 1 push sur main. Mais une fois que ça tourne, on peut considérer que le CI/CD est complet — c'était l'objectif de A3b depuis Sprint 3. Ça mérite d'être marqué `done` officiellement."

**Dana (QA Engineer):** "Ce qui me frappe maintenant qu'on a 0 failures, c'est que la couverture de tests E2E est encore limitée aux flux heureux. On n'a pas d'E2E pour : draft complet 2 équipes (FULL-FLOW), trade refusé, swap invalide. Ces scénarios sont couverts en unitaire mais pas en E2E intégré."

**Elena (Junior Dev):** "Il y a aussi `sprint3-sec-r2-websocket-auth` en backlog depuis Sprint 3. On l'a déféré parce qu'il n'était nécessaire qu'avant exposition publique. Maintenant que JWT est en place et que le CI est quasi-complet — si on veut montrer l'app à quelqu'un, les WebSockets non-authentifiés sont une faille visible."

**Bob (Scrum Master):** "Ce qu'on peut faire pour Sprint 8 : valider F2 (1 push), puis choisir entre : (A) WebSocket auth (SEC-R2, nécessaire avant exposition), (B) E2E couverture flux alternatifs, ou (C) nouveau feature work. La question de Thibaut sur la friction quotidienne oriente vers C si les flux principaux fonctionnent."

**Alice (Product Owner):** "Ma recommandation : Sprint 8 = valider F2 en priorité absolue (c'est un clic), puis focus sur la solidité — soit SEC-R2 WebSocket auth si on veut montrer l'app à l'extérieur, soit couverture E2E des flux alternatifs si on veut plus de confiance en test. Pas de nouvelle feature avant que le sol soit solide."

---

## 8. Action Items Sprint 8

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 8 (issus de la retro Sprint 7)
═══════════════════════════════════════════════════════════

### P0 — Fermer F2 (1 push, priorité immédiate)

| # | Action | Détail |
|---|---|---|
| F2 | Valider `sprint7-f2-ci-vitest-et-docker-push` | Push sur `main` → vérifier CI GitHub Actions : step Vitest visible + job docker-build-and-push vers GHCR. Passer en `done` si vert. |

### P1 — Sécurité WebSocket (avant exposition publique)

| # | Action | Détail |
|---|---|---|
| S1 | SEC-R2 WebSocket auth | `sprint3-sec-r2-websocket-auth` en backlog depuis Sprint 3. JWT token au handshake ou premier message. Obligatoire avant toute exposition publique de l'app. |

### P2 — Qualité tests (dette E2E)

| # | Action | Détail |
|---|---|---|
| E1 | E2E flux alternatifs critiques | Couvrir : trade refusé, swap invalide, draft complet 2 équipes (FULL-FLOW). Actuellement uniquement en tests unitaires. |

### P3 — Process

| # | Action | Détail |
|---|---|---|
| C1 | DoD critère commit Git | Formaliser : "chaque story `done` = au moins 1 commit Git avant story suivante". Ajouter dans `checklist.md`. |
| C2 | DoD critère CI/CD | Formaliser : "story modifiant `ci.yml` → validation via push sur branche principale + CI vert visible". Ajouter dans `checklist.md`. |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → `deferred-future`

═══════════════════════════════════════════════════════════

---

## 9. Évaluation Sprint 7

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 8/10 | 3/4 stories `done` (75%). F2 = `review` en attente validation CI réelle. |
| Qualité technique | 10/10 | 2243/2243 Vitest (0 failures). GHCR push configuré. Seed data idempotente. |
| Tests | 10/10 | Zone.js 38→0. Patterns A/B documentés. Baseline 100% propre. |
| Architecture | 9/10 | CI unifié Vitest+GHCR. Commit 434 fichiers (nettoyage dette, mais anti-pattern process) |
| Suivi actions Sprint 6 | 10/10 | 5/5 livrables (F1 zombie détecté, Z1/F2/D1/D2 livrés) |
| Process | 9/10 | Pré-checks innovants, L1-L5 documentées, C1/C2 à formaliser Sprint 8 |
| **Global** | **9.3/10** | Sprint le plus propre techniquement — objectif "0 signal rouge" atteint |

---

## 10. Preview Sprint 8

**Bob (Scrum Master):** "Sprint 8 doit répondre à : 'si quelqu'un essaie l'app depuis l'extérieur, est-ce que c'est solide et sécurisé ?' On a les flux heureux qui marchent. La question est : est-ce qu'on ferme les dernières failles (WebSocket non-auth) ou est-ce qu'on enrichit la couverture tests ?"

**Priorités naturelles Sprint 8 :**
1. **F2 done** — validation GitHub Actions = 1 push, ferme A3b ouvert depuis Sprint 3
2. **SEC-R2 WebSocket auth** — dernier gap sécurité avant exposition publique
3. **E2E flux alternatifs** — trade refusé, swap invalide, draft complet
4. **DoD commits + CI** — formaliser C1/C2 dans checklist pour éviter les anti-patterns Sprint 7

**Ce qu'on ne fait pas en Sprint 8 :**
- Hébergement externe / staging → `deferred-future`
- Nouvelles features UI (pas avant que le sol soit solide)

---

*Rétrospective générée le 2026-03-12. Prochaine action recommandée : push sur `main` pour valider F2, puis `/bmad-bmm-sprint-planning` pour créer Sprint 8 avec les action items ci-dessus.*
