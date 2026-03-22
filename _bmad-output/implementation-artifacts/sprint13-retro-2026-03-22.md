# Sprint 13 — Rétrospective
**Date**: 2026-03-22
**Facilitateur**: Bob (SM)
**Participants**: Alice (PO), Charlie (Dev), Dana (QA), Thibaut (utilisateur)

---

## 1. Vue d'ensemble du sprint

| Métrique | Valeur |
|----------|--------|
| Stories planifiées | 6 |
| Stories livrées | 6 (100%) |
| Stories en backlog | 0 |
| Tests frontend (baseline entrée) | 2255/2255 |
| Tests frontend (fin sprint) | 2255+ / 0 failures |
| Score global estimé | 8.5/10 |

### Stories livrées
- `sprint13-fix-534-tests` — Stabilisation tests (534 → 0 failures) ✅
- `sprint13-e2e-navigateur-draft` — E2E navigation navigateur ✅
- `sprint13-fix-draft-critiques` — Fix bugs draft critiques (BUG-01..06) ✅
- `sprint13-fix-ux-visuel` — Fix UX/visual bugs (BUG-07..13) ✅
- `sprint13-structured-logging` — Logging structuré (CorrelationId, MDC) ✅
- `sprint13-docs-joueurs` — Documentation pipeline joueurs ✅

---

## 2. Ce qui s'est bien passé (Keep)

- **534 → 0 failures** : La stabilisation complète des tests en début de sprint a tout débloqué. C'est la décision la plus impactante du sprint — Charlie.
- **Logging structuré** : Le MDC + CorrelationId est propre, bien testé, et suit les standards production. Prêt pour un déploiement réel — Charlie.
- **BUG-12 admin dashboard résilient** : Le remplacement de `forkJoin` par des subscriptions indépendantes avec `catchError` par section est une amélioration architecturale solide — Dana.
- **E2E navigateur** : Les specs Playwright pour la navigation draft fonctionnent et documentent le comportement attendu — Dana.
- **Suivi des action items Sprint 12** : Les 5 items A1..A5 ont tous été traités — Alice.

---

## 3. Ce qui pourrait être amélioré (Improve)

### I1 — Documentation : créée = maintenue (ou pas créée)
La documentation `PLAYER_DATA_PIPELINE.md` a été créée mais elle n'est pas liée à un cycle de maintenance. Des docs qui vieillissent mal sont pires que pas de docs. **Règle DoD proposée** : toute doc créée dans un sprint doit être maintenue dans la même PR que le code qu'elle décrit — sinon ne pas la créer.

### I2 — Diagrammes Mermaid : qualité visuelle insuffisante
Le diagramme Mermaid dans `PLAYER_DATA_PIPELINE.md` est fonctionnel mais décevant visuellement par rapport aux exemples vus en ligne. Si un diagramme est créé, il doit être conçu pour le rendu GitHub-first : couleurs, regroupements lisibles, flux clair. Préciser explicitement dans la story ce qu'on attend.

### I3 — BUG-01/02/04 non validés en conditions réelles
Les fixes du draft (WebSocket, CanDeactivate, guard) ont été implémentés et testés en unit/e2e, mais aucune validation en session Docker réelle n'a été effectuée pendant le sprint. Le comportement en conditions réelles peut différer.

---

## 4. Action Items Sprint 14

| ID | Action | Responsable | Priorité |
|----|--------|-------------|----------|
| A1 | Session de test Docker réel — valider BUG-01, BUG-02, BUG-04 en conditions réelles (draft complet à 2 joueurs) | Thibaut + Dev | P0 |
| A2 | DoD §7 — règle : doc créée dans un sprint = maintenue dans la même PR que le code. Si impossible, ne pas créer la doc. | Alice | P1 |
| A3 | Pour toute story demandant un diagramme d'architecture : spécifier le rendu attendu (GitHub-first Mermaid, couleurs, sous-graphes lisibles) et fournir un exemple de référence | Alice | P2 |

---

## 5. Suivi des action items Sprint 12

| ID Sprint 12 | Statut |
|--------------|--------|
| A1 — Stabiliser les 534 tests | ✅ Done (`sprint13-fix-534-tests`) |
| A2 — canDeactivate guard draft | ✅ Done (`sprint12-candeactivate-draft-guard`) |
| A3 — Validation Docker E2E | ✅ Done (`sprint13-e2e-navigateur-draft`) |
| A4 — Logging structuré | ✅ Done (`sprint13-structured-logging`) |
| A5 — Fix bugs visuels/UX | ✅ Done (`sprint13-fix-ux-visuel`) |

---

## 6. Décisions techniques notables

- **`debounceTime(500)` sur `DataSourceIndicator`** : Évite le flash "Initialisation..." lors du chargement. Pattern de test Vitest : `vi.useFakeTimers()` → `detectChanges()` → `subject.next()` → `advanceTimersByTime(500)` → `detectChanges()` → assert.
- **`delay(3000)` sur émissions `false` dans `trackConnectionStatus()`** : Évite le flash "Connexion instable" au montage initial du composant snake-draft.
- **`forkJoin` → subscriptions indépendantes (admin dashboard)** : Chaque section charge et échoue indépendamment. Amélioration de résilience.
- **`CorrelationIdFilter` + `StompMdcInterceptor`** : CorrelationId propagé sur HTTP et WebSocket. Facilite le suivi des requêtes dans les logs.

---

## 7. Note sur la session de test à venir

Sprint 14 commence par une session de test manuel en conditions Docker réelles. Priorités de test :
1. **BUG-01** : Draft WebSocket — pick reçu correctement par les deux joueurs
2. **BUG-02** : Guard CanDeactivate — confirmation avant quitter la page draft
3. **BUG-04** : Timer et tour de jeu — séquence correcte en draft snake à 2 joueurs
4. **BUG-08** : Filtre région catalogue — vérifier que EU/NAW/BR retournent des résultats
5. **BUG-09** : Flash "Connexion instable" — ne doit plus apparaître au chargement

---

*Rétrospective générée le 2026-03-22 via BMAD workflow retrospective.*
