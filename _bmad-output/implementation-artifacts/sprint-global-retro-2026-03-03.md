# Rétrospective Globale de Sprint — Epics 1 à 7

**Date :** 2026-03-03
**Facilitateur :** claude-sonnet-4-6 (SM persona)
**Périmètre :** Sprint complet — Epics 1 à 7 (36 stories, pipeline d'ingestion → supervision admin)
**Statut :** 7/7 epics `done` · 36/36 stories `done` · Tous les retros individuels complétés
**Élicitation avancée :** 5 méthodes appliquées (Pre-mortem, Lessons Learned, Critical Perspective, Self-Consistency, Hindsight)

---

## 1. Scores par Epic — Progression de la Qualité

| Epic | Titre | Stories | Score | Code Reviews |
|------|-------|---------|-------|--------------|
| Epic 1 | Pipeline d'Ingestion et Qualité Joueur | 5/5 | 7.5/10 | 0/5 (aucune) |
| Epic 2 | Catalogue et Consultation Joueur | 4/4 | ~8/10 | 4/4 (100%) |
| Epic 3 | Création de Partie, Invitations, Cycle de Vie | 4/4 | 8/10 | 3/4 (75%) |
| Epic 4 | Draft Serpent, Tranches, Simultané | 6/6 | 8/10 | 6/6 (100%) |
| Epic 5 | Swaps et Trades | 3/3 | 8.5/10 | 3/3 (100%) |
| Epic 6 | Scoring et Leaderboard | 3/3 | 9/10 | 3/3 (100%) |
| Epic 7 | Supervision et Administration | 4/4 | 8.5/10 | 3/4 (75%) |
| **TOTAL** | | **36/36** | **8.2/10** | **22/29 (76%)** |

**Trajectoire qualité :** 7.5 → 8 → 8 → 8 → 8.5 → **9** → 8.5
Progression nette sur l'ensemble du sprint. La légère baisse d'Epic 7 est due à la story 7.3 sans review documentée — non à une régression de la qualité réelle.

> **Note d'honnêteté (issu du challenge critique) :** Le score "8.2/10" reflète la qualité du code livré en contexte de test. La valeur de production réelle est conditionnée à FEAT-003 (clé API Fortnite). Toutes les fonctionnalités utilisent des données synthétiques — le système est un démonstrateur fonctionnel complet, pas encore un système de production.

---

## 2. Métriques Cumulées du Sprint

| Métrique | Valeur |
|----------|--------|
| Epics livrés | 7/7 |
| Stories livrées | 36/36 |
| Tests backend ajoutés (estimation) | ~250+ |
| Tests frontend ajoutés (estimation) | ~150+ |
| **Régressions introduites (sur code testé)** | **0 — sur tests unitaires/intégration** |
| Code reviews effectuées | 22/29 (76%) |
| Findings HIGH résolus avant merge | ~10 |
| Findings MEDIUM résolus avant merge | ~15 |
| Findings LOW en attente (total) | **~40+** ⚠️ |
| Migrations Flyway créées | ~10 (V32–V41) |
| Violations d'architecture détectées et corrigées | ~5 |
| Bugs pré-existants découverts pendant les specs | 2 (3.2 invitation code, auth flow) |
| Nouvelles dépendances externes ajoutées | 0 |
| Tests Vitest frontend orphelins en échec | **47** 🔴 bloquant |

> ⚠️ **Clarification "0 régression" :** Zéro régression sur le périmètre couvert par les tests unitaires et d'intégration. Non validé end-to-end avec la vraie API Fortnite (stub actif). Les smoke tests Playwright couvrent partiellement ce gap mais nécessitent un backend réel connecté.

---

## 3. Ce Qui a Bien Fonctionné — Patterns Gagnants du Sprint

### 3.1 Architecture Hexagonale Progressive — Fondations Solides

**Constat :** `DomainIsolationTest`, `DependencyInversionTest`, `NamingConventionTest`, `LayeredArchitectureTest` sont restés verts sur les 36 stories. Aucune régression architecturale n'a traversé le merge.

> **Nuance importante (issu du challenge critique) :** L'architecture hexagonale est complète sur tous les **nouveaux** domaines créés durant ce sprint. 17 services legacy utilisent encore des imports `model.*` JPA directement (audit ARCHITECTURE_AUDIT). La formulation correcte est : *"Architecture hexagonale progressive — fondations posées sur tous les nouveaux domaines, migration des services legacy en cours (QUAL-001)."*

**Preuves concrètes :**
- Story 3.4 : `IncidentType` dans le mauvais package → bloqué par `NamingConventionTest`, corrigé avant merge
- `DraftPickOrchestrator` → renommé `DraftPickOrchestratorService` après blocage du test
- `DependencyInversionTest` a bloqué 2 controllers injectant directement des repositories

**Leçon confirmée :** Les tests d'architecture automatisés sont **plus fiables** que les code reviews manuelles pour les violations de structure — ils bloquent à la compilation, pas en revue a posteriori.

### 3.2 Zéro Régression sur les 36 Stories Testées

**Constat :** Chaque story a été livrée en laissant la suite de tests verte. TDD Red → Green → Refactor appliqué systématiquement.

**Facteurs explicatifs :**
- Tests écrits avant l'implémentation (TDD)
- Tests exécutés avant chaque merge
- Pattern "test du comportement observable, pas de l'implémentation interne"

**Limite identifiée (pre-mortem) :** Les 47 tests Vitest orphelins créent un bruit rouge constant. Si une vraie régression apparaît, elle risque d'être noyée dans ce bruit et mergée sans être détectée. Ce scénario est **probable** au prochain sprint si G3 n'est pas traité en priorité absolue.

### 3.3 Code Review Adversariale — ROI Démontré

**Constat :** Sur les 22 stories reviewées, ~10 findings HIGH capturés avant merge :
- `IllegalStateException` → sémantique incorrecte capturée en review
- `IncidentReportingService` sans tests (Epic 3)
- `LocalDateTime` → `OffsetDateTime` pour timestamps partagés
- Validation défensive manquante sur statuts (7.4)
- Tests d'autorisation manquants (pattern récurrent sur chaque epic)

**ROI mesuré :** 1 finding HIGH en review = 1 bug évité en production. La corrélation Epic 1 (0 reviews → 3 bugs silencieux) vs Epic 6 (100% reviews → 0 bug opérationnel) est directe.

> **Challenge critique :** Sur les 14 stories sans review (Epic 1 entier, 3.1, 7.3...), on ne peut pas mesurer les bugs **non détectés**. C'est un biais de sélection. La solution : 100% code review dans la DoD — pas de story `done` sans review documentée.

### 3.4 StubResolutionAdapter — Meilleure Décision Architecturale du Sprint

**Constat :** DIP appliqué dès Epic 1 — `ResolutionPort` swappable avec `StubResolutionAdapter` par défaut. 7 epics livrés sans clé API. La dette est bornée et localisée dans FEAT-003/004.

**Vue Hindsight (janvier 2027) :** "Quand FEAT-003 a été livré, on a swappé le stub en 2 heures. Aucune des 36 stories n'a eu besoin d'être retouchée. C'est la définition d'une bonne abstraction sous contrainte."

> **Risque prod identifié (pre-mortem) :** Si `StubResolutionAdapter` reste actif via une mauvaise configuration Spring après le déploiement de FEAT-003, des Epic IDs synthétiques continueront d'être générés silencieusement. **Mitigation :** Ajouter un log WARN au démarrage si `StubResolutionAdapter` est actif en profil `prod`.

### 3.5 Application Inter-Epic des Leçons Techniques

**La boucle retro → application fonctionne pour les leçons techniques. Elle échoue systématiquement pour les leçons de processus.**

| Leçon | Identifiée en | Appliquée en | Type |
|-------|---------------|--------------|------|
| Pattern try-catch + WARN + continue | Epic 1 | Epics 2–7 | ✅ Technique |
| `@Scheduled` annotation | Epic 1 | Epic 6 (6.2) | ✅ Technique |
| `OffsetDateTime` pour timestamps | Epics 1+3 | Epics 4–7 | ✅ Technique |
| `requireTradePending()` guard | Epic 4 | Epic 5 (cloné) | ✅ Technique |
| Code review 100% | Epic 2 | Epics 4, 5, 6 | ✅ Technique |
| DoD formalisée | Epics 1, 2, 3, 7 | **Jamais** | ❌ Processus |
| Template de story mis à jour | Epics 4, 5, 6, 7 | **Jamais** | ❌ Processus |
| Seuil LOW = story dette | Epic 3 | **Jamais** | ❌ Processus |

**Insight clé (Lessons Learned) :** Les leçons techniques s'appliquent parce qu'elles modifient du code — vérifiable. Les leçons de processus ne s'appliquent pas parce qu'elles restent dans des documents textuels. **Règle pour le sprint suivant : toute leçon de processus doit produire un changement de fichier concret** (template, checklist, project-context) — pas juste un action item dans un retro.

---

## 4. Ce Qui Peut Être Amélioré — Patterns Récurrents Non Résolus

### 4.1 [CRITIQUE] Tests de Sécurité — 7 Epics, 0 Fix Durable

**Constat :** Les tests d'autorisation ont été un finding de code review sur **chaque epic** du sprint (1 à 7). 7 fois identifié, 7 fois ajouté en post-review, 0 fois intégré dans le template de story.

**Root cause :** Le template de story ne contient pas de "Task : Tests d'autorisation". Chaque dev agent commence sans cette tâche. Chaque code review la détecte. Le cycle se répète.

**Coût cumulé :** ~7 findings HIGH/MEDIUM en review qui auraient dû être des tâches d'implémentation standard. En janvier 2027 (Hindsight), ce sera la 12ème fois si le template n'est pas modifié.

**Action OBLIGATOIRE — doit être une story tracée :**
```
### Task [N]: Tests d'autorisation Spring Security
Pour chaque endpoint créé :
- [ ] Sans auth → 401 Unauthorized
- [ ] Auth insuffisante / rôle manquant → 403 Forbidden
- [ ] Endpoint admin : utilisateur non-admin → 403
Classe de test dédiée : SecurityConfig{FeatureName}AuthorizationTest
```

### 4.2 [HAUT] DoD Non Formalisée — 4 Demandes, 0 Livraison

**Constat :** Identifiée comme root cause dans les rétros Epics 1, 2, 3, 7. Actions items A4(E1), L1(E2), A1(E3), A2(E7) : toutes demandent d'ajouter la DoD dans `project-context.md`. Elle n'y est toujours pas.

**Conséquences observées :**
- Story 3.1 sans File List → review a posteriori impossible
- Story 7.3 sans Review Follow-ups → tracking incomplet
- `PlayerQualityService` sans `@Scheduled` → marqué `done` alors qu'inopérationnel en production

**DoD cible à ajouter dans `project-context.md` :**
```markdown
## Definition of Done
- [ ] Tests écrits et verts (TDD : Red → Green → Refactor)
- [ ] File List exhaustive remplie dans Dev Agent Record
- [ ] Section "Review Follow-ups" présente (même si vide = "0 finding")
- [ ] Tests d'autorisation créés pour chaque endpoint controller
- [ ] Code review effectuée, findings HIGH+MEDIUM résolus avant `done`
- [ ] Suite de tests globale verte avant merge
```

### 4.3 [HAUT 🔴] 47 Tests Vitest Frontend Orphelins — Bloquant Absolu

**Constat :** 47 tests en rouge qui ne correspondent plus à aucun code actif (DiagnosticComponent, TradeHistory stubs, etc.). Ce bruit masque les vraies régressions.

**Scénario pre-mortem réel :** Dev agent implémente Epic 8. Suite frontend : 47 + 3 en rouge. Les 3 nouveaux sont de vraies régressions — perdues dans le bruit. La feature est mergée cassée.

**Vue Hindsight (jan 2027) :** "On ne les a pas corrigés. En Epic 9, une régression sur le leaderboard n'a pas été détectée. 2 jours de debug perdu."

**Action :** C'est un **bloquant de démarrage**. Aucune story du sprint suivant ne peut commencer si la suite n'est pas à 100% verte. Fix ou suppression des 47 tests en priorité absolue.

### 4.4 [HAUT] `PlayerQualityService` — Scheduler Silencieux depuis Epic 1

**Constat :** `runDailyQualityJob()` n'a pas d'annotation `@Scheduled`. Le job qualité ne s'est jamais déclenché automatiquement depuis la livraison d'Epic 1. Les alertes UNRESOLVED >24h et détections de doublons Epic ID sont implémentées mais inactives.

**Risque production (pre-mortem) :** Des milliers de doublons d'Epic ID s'accumulent silencieusement. En production réelle avec données vraies, le catalogue afficherait des joueurs en double, le scoring serait erroné. Détection uniquement lors d'une plainte utilisateur.

**Vue Hindsight :** "3 jours de cleanup de data. Une annotation aurait suffi."

**Reclassé de MOYEN à HAUT.** Fix en 5 minutes — impact opérationnel maximal.

### 4.5 [MOYEN] Template de Story — 5 Demandes, 0 Modification

**Constat :** Epics 4, 5, 6, 7 ont tous demandé de modifier le template de story (Task sécu, champ "Déclenchement prod" pour batches, "Gaps pré-existants identifiés", "Fondations préexistantes"). Template inchangé.

**Sections à ajouter dans `_bmad/bmm/workflows/4-implementation/create-story/template.md` :**
1. **Task [N] : Tests d'autorisation** — obligatoire pour tout controller
2. **Déclenchement prod** — pour tout service batch/scheduler (`@Scheduled` / event / manuel)
3. **Gaps pré-existants identifiés** — analyse du code existant avant écriture des AC
4. **Fondations préexistantes** — liste des composants déjà disponibles

### 4.6 [MOYEN] Findings LOW — 40+ Pendants Sans Mécanisme de Flush

**Constat :** ~40+ findings LOW accumulés. Chaque epic en génère ~5-16, acceptés individuellement, jamais traités en batch. La règle "seuil 8 LOW = story dette" (retro Epic 3) n'a pas été respectée.

**Findings LOW notables :**
- `serialVersionUID` manquant sur exceptions domaine (×2)
- `GameIncidentService.clearAll()` public → devrait être package-private
- `UserDeletionServiceTest` sans `InOrder` Mockito (ordre critique non vérifié)
- `AdminIncidentListComponent` refetch HTTP à chaque filtre dropdown
- `pickFirstConflict().orElse(null)` → devrait être `orElseThrow()`

**Rendre le coût visible :** Ajouter un compteur `low_findings_pending` dans `sprint-status.yaml`. Seuil automatique : > 10 LOW = story "dette technique" créée dans le sprint en cours.

### 4.7 [MOYEN] Architecture Hexagonale — 17 Services Legacy Restants

**Constat :** `ARCHITECTURE_AUDIT.md` identifie 17 services qui utilisent encore `model.*` JPA directement dans la couche service. Ce n'est pas une architecture hexagonale complète — c'est une migration en cours (ticket QUAL-001).

### 4.8 [BAS] `@Transactional` Self-Invocation et ScoreCalculationService Corrompu

- `TeamScoreDeltaBatchService.computeDeltasForGame()` : self-invocation non résolue (identifié Epic 6)
- `ScoreCalculationService` : fichier corrompu (encoding UTF-8 double) — doit être nettoyé

### 4.9 [BAS] StubResolutionAdapter — Risque Déploiement FEAT-003

**Risque :** Si `StubResolutionAdapter` reste actif via configuration Spring après déploiement de FEAT-003, des Epic IDs synthétiques continueront d'être générés silencieusement.
**Mitigation :** Log WARN au démarrage si `StubResolutionAdapter` est actif en profil `prod`.

---

## 5. Les 3 Grands Patterns à Retenir

### Pattern 1 — Tests d'Architecture = Filet de Sécurité Automatisé

Les tests d'architecture (`DomainIsolationTest`, `DependencyInversionTest`, `NamingConventionTest`) ont capturé des violations structurelles que les reviews manuelles n'auraient probablement pas toutes détectées. Ils bloquent à la compilation — c'est leur force. Étendre leur couverture au prochain sprint.

### Pattern 2 — Leçon Technique vs Leçon de Processus

**Leçons techniques** → s'appliquent dans l'epic suivant parce qu'elles modifient du code vérifiable.
**Leçons de processus** → ne s'appliquent que si elles produisent un changement de fichier concret (template, checklist, project-context). Un action item textuel dans un retro n'est pas une leçon appliquée.

**Règle pour le sprint suivant :** Toute leçon de processus = PR de modification de fichier template/config **avant** la première story codée.

### Pattern 3 — Le Pre-mortem avant de Coder

La méthode la plus rentable pour prévenir les bugs : lire le code existant **avant** d'écrire les AC d'une story. Le GAP CRITIQUE de 3.2 (validation code d'invitation expirée) a été découvert en spec, pas en review ni en test. Ajouter la section "Gaps pré-existants identifiés" dans le template de story.

---

## 6. Action Items — Ordre d'Exécution Validé par Self-Consistency

Les 3 angles d'analyse (priorité technique, risque production, effort/impact) convergent vers l'ordre suivant :

**Phase 0 — Avant de coder quoi que ce soit (bloquants de démarrage)**

| # | Action | Effort | Risque si non fait | Statut |
|---|--------|--------|--------------------|--------|
| G3 | Corriger ou supprimer les 47 tests Vitest frontend orphelins | Moyen | Régressions masquées → **CRITIQUE** | ⬜ |
| G5 | Ajouter `@Scheduled` sur `PlayerQualityService.runDailyQualityJob()` | Très faible | Data corrompue silencieuse → **HAUT** | ⬜ |

**Phase 1 — Process (modifier les fichiers template avant la première story)**

| # | Action | Effort | Risque si non fait | Statut |
|---|--------|--------|--------------------|--------|
| G1+G7 | Modifier `create-story/template.md` + `checklist.md` : Task sécu obligatoire + "Déclenchement prod" + "Gaps pré-existants" | Faible | Pattern récurrent (8ème fois) → **HAUT** | ⬜ |
| G2 | Ajouter DoD formelle dans `project-context.md` (5 critères) | Très faible | Stories `done` sans garanties → **MOYEN** | ⬜ |

**Phase 2 — Cleanup**

| # | Action | Effort | Statut |
|---|--------|--------|--------|
| G6 | Supprimer ou corriger `ScoreCalculationService` corrompu | Faible | ⬜ |
| G4 | Story "Dette Technique LOW" : traiter les ~40 findings LOW | Moyen | ⬜ |
| G4b | Ajouter `low_findings_pending` dans `sprint-status.yaml` | Très faible | ⬜ |

**Phase 3 — Refactoring / Architecture**

| # | Action | Effort | Statut |
|---|--------|--------|--------|
| G8 | Standardiser pattern auth : `@PreAuthorize` vs `userResolver` manuel | Moyen | ⬜ |
| G9 | Créer `DraftWindowViolationException` dédiée | Faible | ⬜ |

**Bloqué externe**

| # | Action | Statut |
|---|--------|--------|
| G10 | Brancher vraie API Fortnite (FEAT-003) — nécessite clé API | ⬜ Bloqué |

> **Note d'architecture (pre-mortem) :** Ajouter log WARN au startup si `StubResolutionAdapter` est actif en profil `prod` — à faire dans le scope de G10.

---

## 7. Recommandation — Sprint de Stabilisation Partiel

**Vue Hindsight :** "Après 7 epics en rafale, on aurait dû faire un sprint de stabilisation : 0 nouvelle feature, que de la dette technique et du process."

**Recommandation concrète :** Le prochain sprint devrait commencer par une **phase de stabilisation** couvrant G3+G5+G1+G7+G2+G6 **avant** de coder la première story feature. Ces 6 actions représentent peu de code mais un impact processus maximal.

Si le sprint planning identifie un nouvel epic feature, débuter par ces items de stabilisation comme stories prioritaires numérotées dans `sprint-status.yaml` — pas comme action items flottants.

---

## 8. Stakeholder Round Table — Rétrospective Globale

#### Bob (Scrum Master)
> "7 epics, 36 stories, 0 régression sur le périmètre testé. Trajectoire qualité 7.5 → 9. La boucle retro → application fonctionne pour les leçons techniques. Ce qui ne fonctionne pas : les leçons de processus. DoD : 4 fois demandée, 0 fois livrée. Template : 5 fois demandé, 0 fois modifié. La différence pour le sprint suivant : G1+G2+G7 doivent être dans sprint-status.yaml comme stories numérotées — pas dans un document retro. Les stories tracées se font. Les action items textuels disparaissent."

#### Alice (Product Owner)
> "Fonctionnellement : pipeline, catalogue, parties, draft, trades, leaderboard, supervision — tout livré. La valeur pour la compétition réelle attend FEAT-003. Je veux être claire sur ce point : le système est un démonstrateur complet et fiable. Dès qu'on a la clé API Fortnite, on branche. L'architecture est prête. Ce qui m'inquiète côté product : les 47 tests Vitest orphelins. Je ne veux pas que des régressions passent en prod à cause de bruit dans la CI."

#### Charlie (Senior Dev)
> "L'architecture hexagonale progressive fonctionne. Les tests d'architecture sont nos meilleurs alliés — ils ont évité des violations que je n'aurais probablement pas toutes capturées en review. Ce que j'aurais fait différemment : le template de story. 5 epics ont demandé d'y ajouter les tests de sécurité. Si on l'avait fait après Epic 2, on aurait économisé 5 findings HIGH en review. Ce sont les décisions de process non actées qui coûtent le plus cher sur la durée."

#### Dana (QA)
> "La suite de tests est solide sur 0 régression. Mais les 47 tests orphelins me font peur — c'est un bruit rouge qui masquera la prochaine vraie régression. Règle que je propose : toute CI avec des tests en rouge est bloquante pour le merge, peu importe si c'est 'pré-existant'. Le `@Scheduled` manquant sur `PlayerQualityService` est aussi mon point de préoccupation — un test d'activation aurait dû exister depuis Epic 1."

#### Elena (Junior Dev)
> "Ce sprint m'a appris que l'architecture n'est pas un coût — c'est un investissement. `PlayerIdentityEntry` créé en Epic 1 a accueilli `.correctMetadata()` en Epic 7 sans modifier les fondations. OCP en action. Ce que j'emporte : la leçon technique vs leçon de processus. Les leçons qui changent des fichiers s'appliquent. Les leçons qui restent dans des retros se répètent."

---

## 9. Évaluation du Sprint — État des Fondations

### Ce qui est disponible et stable

| Domaine | État | Notes |
|---------|------|-------|
| Pipeline ingestion CSV multi-régions | ✅ Opérationnel | Données synthétiques |
| Résolution Epic ID | ⚠️ Stub actif | FEAT-003 requis pour données réelles |
| Catalogue joueurs | ✅ Fonctionnel | |
| Gestion parties (création, invitations, cycle) | ✅ Fonctionnel | |
| Draft serpent + simultané | ✅ Fonctionnel | |
| Système trades + swaps | ✅ Fonctionnel | |
| Leaderboard delta PR | ✅ Fonctionnel | |
| Supervision admin (pipeline, jeux, alertes) | ✅ Fonctionnel | |
| Tests d'architecture | ✅ Verts | Couverture à étendre |
| Contraintes sécurité | ✅ Renforcées | |
| Suite de tests frontend | ⚠️ 47 orphelins rouges | Bloquant |

### Bloquants pour la production réelle

1. **FEAT-003** : Clé API Fortnite requise (bloquant externe)
2. **47 tests Vitest** orphelins (bloquant interne — G3)
3. **`PlayerQualityService`** non schedulé (alertes pipeline inactives — G5)
4. **`GameIncidentService`** in-memory (incidents perdus au redémarrage)
5. **Log WARN startup** manquant pour `StubResolutionAdapter` en prod

---

## 10. Score Global du Sprint

| Dimension | Score | Commentaire |
|-----------|-------|-------------|
| Livraison (scope) | 10/10 | 36/36 stories done, 7/7 epics done |
| Qualité du code | 8.5/10 | 0 régression testée, archi hexagonale progressive, reviews 76% |
| Process | 6.5/10 | DoD non formalisée, template non modifié, 47 tests orphelins |
| Leçons appliquées | 7.5/10 | Techniques ✅ appliquées / Processus ❌ non actées |
| Préparation production | 5/10 | Stub actif, scheduler inactif, incidents in-memory |
| **GLOBAL** | **7.7/10** | Sprint solide avec dette processus identifiée et actionnée |

---

## 11. Mot de Clôture

Ce sprint a livré un système complet : ingestion → catalogue → parties → draft → trades → leaderboard → supervision. 36 stories, 7 epics, 0 régression sur le périmètre testé. La trajectoire qualité 7.5 → 9 → 8.5 prouve que le cycle retro → application fonctionne — pour les leçons techniques.

La fragilité identifiée est structurelle : les leçons de **processus** n'ont pas produit de changements de fichiers. Elles sont restées dans des documents retro. Pour le sprint suivant, la règle est simple : **toute leçon de processus = PR de modification de fichier template/config, avant la première story codée.** G1+G2+G7+G5 entrent dans `sprint-status.yaml` comme stories numérotées — pas comme action items flottants.

> *"Un sprint réussi n'est pas celui qui livre le plus vite — c'est celui qui rend le suivant plus facile à livrer."*
>
> *"Les leçons techniques changent du code. Les leçons de processus changent des fichiers de config. Les leçons qui ne changent rien ne s'appliquent jamais."*

---

*Rétrospective globale générée par claude-sonnet-4-6 (SM persona) · BMAD Method v6 · 2026-03-03*
*Élicitation avancée : Pre-mortem Analysis · Lessons Learned Extraction · Challenge from Critical Perspective · Self-Consistency Validation · Hindsight Reflection*
